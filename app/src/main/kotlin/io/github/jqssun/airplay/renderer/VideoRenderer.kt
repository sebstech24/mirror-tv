package io.github.jqssun.airplay.renderer

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import io.github.jqssun.airplay.renderer.DecoderSelector.Companion.videoCaps

class VideoRenderer(ctx: Context) {

    private val lock = Object()
    private val pipeline = VideoPipeline()
    val selector = DecoderSelector(ctx)
    private var avcDecoder: MediaCodecInfo? = null
    private var hevcDecoder: MediaCodecInfo? = null
    private var maxFps = 0
    private var codec: MediaCodec? = null
    private var displaySurface: Surface? = null
    private var currentH265 = false
    private var videoWidth = 0
    private var videoHeight = 0
    private var firstFrameQueued = false
    // direct mode: decoder renders straight to the display surface, used when the gl pipeline
    // fails or never produces a frame on this device. sticky for the process
    private var direct = false
    private var firstQueuedAtNs = 0L
    @Volatile private var gotOutput = false
    // decoded frames are taken out on their own thread the moment they're ready. Draining only when the next
    // frame arrived made each picture wait for its successor: invisible with the iPhone's steady 60 fps, a
    // visible stutter with the Mac, which sends frames unevenly (only when the desktop changes)
    private var outThread: Thread? = null
    @Volatile private var outRunning = false
    // smoothing: hold each decoded picture until its turn on the sender's own clock, plus just enough delay to
    // cover the late frames seen in the last ~3 s (Macs on Wi-Fi bunch frames up about once a second).
    // Always on with "Lowest latency" off; with it on, switches itself on for the session once frames arrive unevenly.
    @Volatile var smoothing = false
    @Volatile private var _autoSmooth = false
    private val _held = ArrayDeque<Pair<Int, Long>>()
    private val _offsets = LongArray(SMOOTH_WINDOW)
    private var _offN = 0
    private var _baseNs = Long.MAX_VALUE
    private var _delayNs = 0L
    private var _unevenStreak = 0
    // timing probe for the benchmark log: when each frame went into the decoder, by presentation time
    private val _probePts = LongArray(64)
    private val _probeAt = LongArray(64)
    private var _probeIdx = 0
    @Volatile private var _outThisSec = 0
    @Volatile private var _waitMaxUs = 0L
    @Volatile private var _waitSumUs = 0L
    private var _lastInNs = 0L
    private var _inGapMaxUs = 0L
    private var _lastInPts = 0L
    @Volatile private var _netJitterMaxUs = 0L
    @Volatile private var _feedWaitMaxUs = 0L
    // keyframe that started this session's codec: replayed to restart it in direct mode
    private var sessionKey: ByteArray? = null
    private var sessionKeyH265 = false

    // stats
    @Volatile var fps = 0; private set
    @Volatile var bitrateBps = 0L; private set
    @Volatile var frameCount = 0L; private set
    @Volatile var codecName = ""; private set
    @Volatile var droppedFrames = 0L; private set
    @Volatile var framePacingJitterUs = 0L; private set

    var enforceSdr = true
    var keyAllowFrameDrop = true
    var scheduledOutputBufferRelease = true
    var benchmarkLog = false
    var benchmarkLogCallback: ((String) -> Unit)? = null
    private var _framesThisSec = 0
    private var _bytesThisSec = 0L
    private var _lastStatReset = 0L
    private val _frameIntervalsNs = LongArray(120)
    private var _frameIntervalIdx = 0
    private var _frameIntervalCount = 0
    private var _lastOutputFrameNs = 0L
    // anchors that map decoder PTS (us) to System.nanoTime() for scheduled rendering
    private var _ptsBaseUs = Long.MIN_VALUE
    private var _wallBaseNs = 0L

    fun setResolution(w: Int, h: Int) = synchronized(lock) {
        if (w == videoWidth && h == videoHeight) return@synchronized
        videoWidth = w
        videoHeight = h
        pipeline.setVideoSize(w, h)
        sessionKey = null
        stopCodec()
    }

    // doesn't restart codec; decoder renders into pipeline's own persistent surface
    fun setSurface(surface: Surface) = synchronized(lock) {
        displaySurface = surface
        if (!direct) { pipeline.setDisplaySurface(surface); return@synchronized }
        val c = codec ?: return@synchronized
        try { c.setOutputSurface(surface) } catch (e: Exception) { stopCodec() }
    }

    fun clearSurface(surface: Surface) = synchronized(lock) {
        if (displaySurface !== surface) return@synchronized
        displaySurface = null
        // direct: the codec can't outlive its surface; restarts from sessionKey when a new one arrives
        if (direct) stopCodec() else pipeline.setDisplaySurface(null)
    }

    fun selectDecoders(w: Int, h: Int, fps: Int, h265: Boolean?): Boolean = synchronized(lock) {
        avcDecoder = selector.avc()
        hevcDecoder = if (h265 != false) selector.hevc(avcDecoder, w, h, fps, force = h265 == true) else null
        maxFps = fps
        Log.i(TAG, "decoders: avc=${avcDecoder?.name} hevc=${hevcDecoder?.name}")
        hevcDecoder != null
    }

    // unknown limits default to 1080p
    fun maxResolution(): Pair<Int, Int> =
        listOfNotNull(avcDecoder?.let { it to DecoderSelector.AVC }, hevcDecoder?.let { it to DecoderSelector.HEVC })
            .map { (info, mime) ->
                runCatching { info.videoCaps(mime).let { it.supportedWidths.upper to it.supportedHeights.upper } }
                    .getOrDefault(1920 to 1080)
            }
            .reduceOrNull { (w1, h1), (w2, h2) -> minOf(w1, w2) to minOf(h1, h2) } ?: (1920 to 1080)

    // codec per mirror session; pipeline persists across sessions
    fun startSession() = synchronized(lock) { _resetStats(); _autoSmooth = false }

    fun stopSession() = synchronized(lock) { sessionKey = null; stopCodec() }

    private fun _resetStats() {
        fps = 0; bitrateBps = 0; frameCount = 0; codecName = ""
        droppedFrames = 0; framePacingJitterUs = 0
        _framesThisSec = 0; _bytesThisSec = 0
    }

    private fun _updateStats(size: Int) {
        val now = System.currentTimeMillis()
        if (now - _lastStatReset >= 1000) {
            fps = _framesThisSec
            bitrateBps = _bytesThisSec * 8
            framePacingJitterUs = _computeFramePacingJitterUs()
            _framesThisSec = 0
            _bytesThisSec = 0
            _lastStatReset = now
            if (benchmarkLog) _emitBenchmarkLine()
        }
        _framesThisSec++
        _bytesThisSec += size
        frameCount++
    }

    private fun _emitBenchmarkLine() {
        val out = _outThisSec
        val msg = "fps=$fps out=$out drawn=${pipeline.takeDrawn()} skipped=${pipeline.takeSkipped()} " +
            "wait_avg=${if (out > 0) _waitSumUs / out / 1000 else 0}ms wait_max=${_waitMaxUs / 1000}ms " +
            "in_gap_max=${_inGapMaxUs / 1000}ms net_jitter_max=${_netJitterMaxUs / 1000}ms feed_wait_max=${_feedWaitMaxUs / 1000}ms " +
            "smooth=${smoothing || _autoSmooth} smooth_delay=${_delayNs / 1_000_000}ms " +
            "bitrate=${bitrateBps / 1000}kbps " +
            "jitter=${framePacingJitterUs}us frames=$frameCount " +
            "dropped=$droppedFrames codec=$codecName " +
            "res=${videoWidth}x${videoHeight}"
        _outThisSec = 0; _waitMaxUs = 0; _waitSumUs = 0; _inGapMaxUs = 0; _netJitterMaxUs = 0; _feedWaitMaxUs = 0
        Log.i(BENCH_TAG, msg)
        benchmarkLogCallback?.invoke(msg)
    }

    fun feedFrame(raw: ByteArray, ntpTimeNs: Long, isH265: Boolean) {
        if (benchmarkLog) _probeArrival(System.nanoTime(), ntpTimeNs / 1000)
        // the Mac's SPS lets the decoder reorder frames, so it holds pictures back; tell it not to
        val data = if (!isH265 && _startsWithSps(raw)) H264SpsFixer.fixAccessUnit(raw).also {
            if (benchmarkLog) Log.i(BENCH_TAG, "sps in: ${raw.copyOf(minOf(raw.size, 40)).joinToString("") { b -> "%02x".format(b) }} " +
                "out: ${it.copyOf(minOf(it.size, 44)).joinToString("") { b -> "%02x".format(b) }}")
        } else raw
        synchronized(lock) {
            _updateStats(data.size)
            if (videoWidth == 0 || videoHeight == 0) return

            _checkFallback()
            var replay: ByteArray? = null
            if (codec == null || isH265 != currentH265) {
                // a stale reference frame decodes to corruption, so wait for a keyframe to (re)start
                if (_isKeyframe(data, isH265)) {
                    sessionKey = data; sessionKeyH265 = isH265
                } else {
                    // direct mode restarts mid-session (surface swap, fallback): the sender won't
                    // send another keyframe, so replay the session's one; a glitch beats black
                    replay = sessionKey?.takeIf { direct && sessionKeyH265 == isH265 }
                    if (replay == null) {
                        if (codec != null) stopCodec()
                        return
                    }
                }
                stopCodec()
            }

            try {
                if (codec == null) startCodec(isH265)
                if (codec == null) return
                replay?.let { _feedToCodec(it, ntpTimeNs) }
                _feedToCodec(data, ntpTimeNs)
            } catch (e: Exception) {
                Log.w(TAG, "Codec error, resetting", e)
                stopCodec()
            }
        }
    }

    private fun _feedToCodec(data: ByteArray, ntpTimeNs: Long) {
        val c = codec ?: return
        // dropping a frame desyncs decoder until the next keyframe, but source would only send one on (re)connect
        val retries = if (firstFrameQueued) FEED_RETRIES else FIRST_FEED_RETRIES
        val feedStart = System.nanoTime()
        repeat(retries) {
            val idx = c.dequeueInputBuffer(FEED_WAIT_US)
            if (idx >= 0) {
                val buf = c.getInputBuffer(idx) ?: return
                buf.clear()
                buf.put(data)
                // an access unit delimiter after the picture tells the decoder it is complete; without it the
                // MediaTek decoder waited for the next frame, so the last picture before a pause showed up late
                val aud = !currentH265 && buf.remaining() >= AUD.size
                if (aud) buf.put(AUD)
                c.queueInputBuffer(idx, 0, data.size + if (aud) AUD.size else 0, ntpTimeNs / 1000, 0)
                if (benchmarkLog) _probeIn(ntpTimeNs / 1000)
                _feedWaitMaxUs = maxOf(_feedWaitMaxUs, (System.nanoTime() - feedStart) / 1000)
                if (!firstFrameQueued) firstQueuedAtNs = System.nanoTime()
                firstFrameQueued = true
                return
            }
        }
        droppedFrames++
        Log.w(TAG, "Decoder input queue full; dropping frame. drops=$droppedFrames")
    }

    // the Mac sends SPS/PPS at the start of each keyframe; only those get rewritten
    private fun _startsWithSps(d: ByteArray): Boolean {
        for (i in 0 until minOf(d.size - 3, 4)) {
            if (d[i].toInt() == 0 && d[i + 1].toInt() == 0 && d[i + 2].toInt() == 1) return (d[i + 3].toInt() and 0x1F) == 7
        }
        return false
    }

    private fun _isKeyframe(data: ByteArray, isH265: Boolean): Boolean {
        if (data.size < 5) return false
        var i = 0
        while (i <= data.size - 5) {
            if (data[i] == 0.toByte() && data[i + 1] == 0.toByte() &&
                data[i + 2] == 0.toByte() && data[i + 3] == 1.toByte()) {
                val key = if (isH265) {
                    val type = (data[i + 4].toInt() shr 1) and 0x3F
                    type == 19 || type == 20 || type == 21 || type == 32 || type == 33
                } else {
                    val type = data[i + 4].toInt() and 0x1F
                    type == 5 || type == 7
                }
                if (key) return true
            }
            i++
        }
        return false
    }

    // switch to direct mode when the gl pipeline died or decoded nothing for a while
    private fun _checkFallback() {
        if (direct || codec == null) return
        val stalled = firstQueuedAtNs != 0L && !gotOutput &&
            System.nanoTime() - firstQueuedAtNs > NO_OUTPUT_FALLBACK_NS
        if (!pipeline.failed && !stalled) return
        Log.w(TAG, "video pipeline ${if (stalled) "produced no frames" else "failed"}; rendering directly")
        _goDirect()
    }

    private fun _goDirect() {
        direct = true
        directForProcess = true
        stopCodec()
        pipeline.release()
    }

    private fun _surfaceForCodec(): Surface? {
        if (!direct) {
            pipeline.start()
            pipeline.setVideoSize(videoWidth, videoHeight)
            pipeline.inputSurface?.let { return it }
            Log.w(TAG, "video pipeline unavailable; rendering directly")
            _goDirect()
        }
        return displaySurface?.takeIf { it.isValid }
    }

    private fun startCodec(h265: Boolean) {
        if (directForProcess) direct = true
        val s = _surfaceForCodec() ?: return
        currentH265 = h265
        val mime = if (h265) DecoderSelector.HEVC else DecoderSelector.AVC
        val info = (if (h265) hevcDecoder else avcDecoder) ?: error("no decoder selected for $mime")

        firstFrameQueued = false
        try {
            _startWithLadder(info, mime, s, h265)
        } catch (e: Exception) {
            // strict hw decoders reject configs beyond their real limits
            val sw = selector.software(mime, videoWidth, videoHeight) ?: throw e
            Log.w(TAG, "Hardware decoder failed, trying software fallback", e)
            _startWithLadder(sw, mime, s, h265)
        }
        Log.i(TAG, "Video codec started: $mime ${videoWidth}x${videoHeight} ($codecName)")
    }

    private fun _startWithLadder(info: MediaCodecInfo, mime: String, s: Surface, h265: Boolean) {
        var tryNum = 0
        while (true) {
            val format = _format(mime, info)
            val more = selector.lowLatencyOptions(format, info, mime, tryNum)
            try {
                _startDecoder(MediaCodec.createByCodecName(info.name), format, s, h265)
                return
            } catch (e: Exception) {
                if (!more) throw e
                Log.w(TAG, "configure try $tryNum failed: $format", e)
                tryNum++
            }
        }
    }

    private fun _format(mime: String, info: MediaCodecInfo) = MediaFormat.createVideoFormat(mime, videoWidth, videoHeight).apply {
        setInteger(MediaFormat.KEY_FRAME_RATE, maxFps)
        if (selector.adaptive(info, mime)) {
            setInteger(MediaFormat.KEY_MAX_WIDTH, videoWidth)
            setInteger(MediaFormat.KEY_MAX_HEIGHT, videoHeight)
        }
        setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxOf(videoWidth * videoHeight * 3 / 4, 1024 * 1024))
        if (enforceSdr) {
            setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709)
            setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_LIMITED)
            setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_SDR_VIDEO)
        }
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            setInteger(MediaFormat.KEY_ALLOW_FRAME_DROP, if (keyAllowFrameDrop) 1 else 0)
        }
        // not the MediaTek TV "game-mode"/"low-latency-mode" vendor keys: on a Philips TV they route decoded
        // pictures to the TV's own video plane, behind our surface, so the screen stayed black (28 Oct 2026)
    }

    private fun _startDecoder(c: MediaCodec, format: MediaFormat, surface: Surface, h265: Boolean) {
        try {
            c.configure(format, surface, null, 0)
            c.start()
        } catch (e: Exception) {
            try { c.release() } catch (_: Exception) {}
            throw e
        }
        codec = c
        codecName = (if (h265) "H.265" else "H.264") + " (${c.name})"
        if (benchmarkLog) Log.i(BENCH_TAG, "decoder format: $format")
        _startOutputThread(c)
    }

    private fun _startOutputThread(c: MediaCodec) {
        outRunning = true
        outThread = Thread({
            val info = MediaCodec.BufferInfo()
            while (outRunning) {
                try {
                    val idx = c.dequeueOutputBuffer(info, if (_held.isEmpty()) OUTPUT_WAIT_US else HELD_POLL_US)
                    if (idx >= 0) _onOutput(c, idx, info)
                    _releaseDue(c)
                } catch (e: Exception) { break }
            }
            _held.clear()
        }, "VideoOutput").apply { priority = Thread.MAX_PRIORITY; start() }
    }

    private fun stopCodec() {
        outRunning = false
        outThread?.join()
        outThread = null
        _offN = 0; _baseNs = Long.MAX_VALUE; _delayNs = 0; _unevenStreak = 0
        _frameIntervalIdx = 0
        _frameIntervalCount = 0
        _lastOutputFrameNs = 0L
        _ptsBaseUs = Long.MIN_VALUE
        _wallBaseNs = 0L
        firstQueuedAtNs = 0L
        gotOutput = false
        codec?.let {
            try {
                it.stop()
                it.release()
            } catch (_: Exception) {}
        }
        codec = null
    }

    private fun _onOutput(c: MediaCodec, idx: Int, info: MediaCodec.BufferInfo) {
        val now = System.nanoTime()
        val pts = info.presentationTimeUs * 1000
        _offsets[_offN++ % SMOOTH_WINDOW] = now - pts
        if (_offN % SMOOTH_RECOMPUTE == 0) _recomputeDelay()
        if (!(smoothing || _autoSmooth) || _baseNs == Long.MAX_VALUE) { _releaseOutput(c, idx, info); return }
        gotOutput = true
        _recordOutputFrameTime()
        if (benchmarkLog) _probeOut(info.presentationTimeUs)
        val due = pts + _baseNs + _delayNs
        // more than the decoder can spare held: show the oldest now rather than stall it
        if (due <= now || _held.size >= MAX_HELD) {
            if (_held.size >= MAX_HELD) c.releaseOutputBuffer(_held.removeFirst().first, true)
            if (due <= now) { c.releaseOutputBuffer(idx, true); return }
        }
        _held.addLast(idx to due)
    }

    private fun _releaseDue(c: MediaCodec) {
        val now = System.nanoTime() + HELD_POLL_US * 1000
        while (_held.isNotEmpty() && _held.first().second <= now) c.releaseOutputBuffer(_held.removeFirst().first, true)
    }

    // base = the fastest arrival in the window (tracks clock drift); delay = how much later the slow ones came
    private fun _recomputeDelay() {
        val n = minOf(_offN, SMOOTH_WINDOW)
        val sorted = _offsets.copyOf(n).also { it.sort() }
        _baseNs = sorted[0]
        val spread = sorted[(n * 95 / 100).coerceAtMost(n - 1)] - sorted[0]
        // rise at once (the next late burst is covered), fall slowly: a delay that jumps around is its own judder
        val target = spread.coerceIn(0, MAX_SMOOTH_NS)
        _delayNs = if (target >= _delayNs) target else maxOf(target, _delayNs - SMOOTH_DECAY_NS)
        _unevenStreak = if (spread > UNEVEN_NS) _unevenStreak + 1 else 0
        if (!smoothing && !_autoSmooth && _unevenStreak >= UNEVEN_STREAK) {
            _autoSmooth = true
            Log.i(TAG, "frames arrive unevenly (${spread / 1_000_000} ms spread): smoothing on for this session")
        }
    }

    private fun _releaseOutput(c: MediaCodec, idx: Int, info: MediaCodec.BufferInfo) {
        gotOutput = true
        _recordOutputFrameTime()
        if (benchmarkLog) _probeOut(info.presentationTimeUs)
        if (scheduledOutputBufferRelease) {
            // schedule frame at VSYNC matching its NTP presentation time
            val ptsUs = info.presentationTimeUs
            if (_ptsBaseUs == Long.MIN_VALUE) {
                _ptsBaseUs = ptsUs
                _wallBaseNs = System.nanoTime()
            }
            c.releaseOutputBuffer(idx, _wallBaseNs + (ptsUs - _ptsBaseUs) * 1000L)
        } else {
            c.releaseOutputBuffer(idx, true)
        }
    }

    // arrival from the network, before we wait on the decoder
    private fun _probeArrival(now: Long, ptsUs: Long) {
        if (_lastInNs > 0) {
            val gap = (now - _lastInNs) / 1000
            _inGapMaxUs = maxOf(_inGapMaxUs, gap)
            // >0: arrived later than the Mac's timestamps say (network/encoder bunching); ~0: Mac simply sent nothing
            _netJitterMaxUs = maxOf(_netJitterMaxUs, kotlin.math.abs(gap - (ptsUs - _lastInPts)))
        }
        _lastInNs = now; _lastInPts = ptsUs
    }

    private fun _probeIn(ptsUs: Long) {
        val now = System.nanoTime()
        val i = _probeIdx++ and 63
        _probePts[i] = ptsUs; _probeAt[i] = now
    }

    private fun _probeOut(ptsUs: Long) {
        _outThisSec++
        for (i in 0 until 64) if (_probePts[i] == ptsUs) {
            val w = (System.nanoTime() - _probeAt[i]) / 1000
            _waitSumUs += w; if (w > _waitMaxUs) _waitMaxUs = w
            return
        }
    }

    fun release() = synchronized(lock) {
        stopCodec()
        pipeline.release()
        _resetStats()
    }

    private fun _recordOutputFrameTime() {
        val now = System.nanoTime()
        if (_lastOutputFrameNs > 0) {
            _frameIntervalsNs[_frameIntervalIdx % _frameIntervalsNs.size] = now - _lastOutputFrameNs
            _frameIntervalIdx++
            _frameIntervalCount++
        }
        _lastOutputFrameNs = now
    }

    private fun _computeFramePacingJitterUs(): Long {
        val count = _frameIntervalCount.coerceAtMost(_frameIntervalsNs.size)
        if (count < 2) return 0

        var sum = 0.0
        var sumSq = 0.0
        for (i in 0 until count) {
            val interval = _frameIntervalsNs[i].toDouble()
            sum += interval
            sumSq += interval * interval
        }
        val mean = sum / count
        val variance = (sumSq / count) - (mean * mean)
        return (kotlin.math.sqrt(variance.coerceAtLeast(0.0)) / 1000.0).toLong()
    }

    companion object {
        private const val TAG = "VideoRenderer"
        private const val BENCH_TAG = "BENCHMARK"
        private const val FEED_WAIT_US = 20_000L
        private const val FEED_RETRIES = 10
        private const val FIRST_FEED_RETRIES = 50
        private const val NO_OUTPUT_FALLBACK_NS = 3_000_000_000L
        private const val OUTPUT_WAIT_US = 10_000L
        private const val HELD_POLL_US = 2_000L
        private val AUD = byteArrayOf(0, 0, 0, 1, 0x09, 0xF0.toByte())
        private const val SMOOTH_WINDOW = 180 // ~3 s at 60 fps
        private const val SMOOTH_RECOMPUTE = 30
        // MAX_HELD pictures at 60 fps: asking for more forces early releases, i.e. the bunching again
        private const val MAX_SMOOTH_NS = 66_000_000L
        private const val SMOOTH_DECAY_NS = 2_000_000L // per recompute (~0.5 s): 4 ms a second
        private const val MAX_HELD = 4 // the decoder has ~6 output buffers
        private const val UNEVEN_NS = 34_000_000L // two frames
        private const val UNEVEN_STREAK = 4 // ~2 s of uneven arrival
        // once the gl path fails it fails again on the same device
        @Volatile private var directForProcess = false
    }
}
