package io.github.jqssun.airplay.renderer

import java.io.ByteArrayOutputStream

/**
 * The Mac's H.264 SPS doesn't say "no frame reordering", so decoders (seen on MediaTek TVs) hold every picture
 * until several more arrive. With the Mac's uneven frame rate that is a visible stutter. Rewriting the SPS VUI with
 * max_num_reorder_frames = 0 lets the decoder output each picture at once (Moonlight does the same).
 * Pure Kotlin; returns null when the SPS can't be parsed, so the caller keeps the original.
 */
object H264SpsFixer {

    /** Rewrites every SPS NAL in an Annex-B access unit; returns [au] unchanged when there is none. */
    fun fixAccessUnit(au: ByteArray): ByteArray {
        val starts = _nalStarts(au)
        if (starts.none { (_, p) -> p < au.size && (au[p].toInt() and 0x1F) == 7 }) return au
        val out = ByteArrayOutputStream(au.size + 16)
        out.write(au, 0, starts.first().first)
        for ((i, sp) in starts.withIndex()) {
            val (prefixAt, payloadAt) = sp
            val end = if (i + 1 < starts.size) starts[i + 1].first else au.size
            out.write(au, prefixAt, payloadAt - prefixAt)
            val nal = au.copyOfRange(payloadAt, end)
            val fixed = if (nal.isNotEmpty() && (nal[0].toInt() and 0x1F) == 7) fixSps(nal) ?: nal else nal
            out.write(fixed)
        }
        return out.toByteArray()
    }

    /** [nal] = SPS NAL incl. its 1-byte header, with emulation prevention bytes. */
    fun fixSps(nal: ByteArray): ByteArray? = runCatching {
        val r = BitReader(_unescape(nal, 1))
        val w = BitWriter()
        val profile = r.copy(w, 8)
        r.copy(w, 16) // constraint flags, level
        r.copyUe(w) // seq_parameter_set_id
        if (profile in HIGH_PROFILES) {
            val chroma = r.copyUe(w)
            if (chroma == 3) r.copy(w, 1)
            r.copyUe(w); r.copyUe(w) // bit depths
            r.copy(w, 1) // qpprime_y_zero_transform_bypass
            if (r.copy(w, 1) == 1) { // seq_scaling_matrix_present
                repeat(if (chroma != 3) 8 else 12) { i -> if (r.copy(w, 1) == 1) _copyScalingList(r, w, if (i < 6) 16 else 64) }
            }
        }
        r.copyUe(w) // log2_max_frame_num_minus4
        when (r.copyUe(w)) { // pic_order_cnt_type
            0 -> r.copyUe(w)
            1 -> { r.copy(w, 1); r.copySe(w); r.copySe(w); repeat(r.copyUe(w)) { r.copySe(w) } }
        }
        val refFrames = r.copyUe(w)
        r.copy(w, 1) // gaps_in_frame_num_allowed
        r.copyUe(w); r.copyUe(w) // width, height in mbs
        if (r.copy(w, 1) == 0) r.copy(w, 1) // frame_mbs_only, mb_adaptive_frame_field
        r.copy(w, 1) // direct_8x8_inference
        if (r.copy(w, 1) == 1) repeat(4) { r.copyUe(w) } // frame cropping
        val vui = r.read(1)
        w.write(1, 1)
        if (vui == 1) {
            if (r.copy(w, 1) == 1 && r.copy(w, 8) == 255) r.copy(w, 32) // aspect ratio (+ extended SAR)
            if (r.copy(w, 1) == 1) r.copy(w, 1) // overscan
            if (r.copy(w, 1) == 1) { r.copy(w, 4); if (r.copy(w, 1) == 1) r.copy(w, 24) } // video signal type
            if (r.copy(w, 1) == 1) { r.copyUe(w); r.copyUe(w) } // chroma location
            if (r.copy(w, 1) == 1) { r.copy(w, 32); r.copy(w, 32); r.copy(w, 1) } // timing
            val nalHrd = r.copy(w, 1); if (nalHrd == 1) _copyHrd(r, w)
            val vclHrd = r.copy(w, 1); if (vclHrd == 1) _copyHrd(r, w)
            if (nalHrd == 1 || vclHrd == 1) r.copy(w, 1) // low_delay_hrd
            r.copy(w, 1) // pic_struct_present
            // the old bitstream_restriction (if any) is replaced below
        } else {
            repeat(8) { w.write(0, 1) } // aspect, overscan, signal, chroma loc, timing, nal hrd, vcl hrd, pic_struct
        }
        w.write(1, 1) // bitstream_restriction_flag
        w.write(1, 1) // motion_vectors_over_pic_boundaries
        w.writeUe(2); w.writeUe(1) // max_bytes_per_pic_denom, max_bits_per_mb_denom
        w.writeUe(16); w.writeUe(16) // log2_max_mv_length horizontal, vertical
        w.writeUe(0) // max_num_reorder_frames: output each picture as soon as it's decoded
        w.writeUe(maxOf(refFrames, 1)) // max_dec_frame_buffering
        w.write(1, 1) // rbsp_stop_one_bit
        w.alignZero()
        byteArrayOf(nal[0]) + _escape(w.bytes())
    }.getOrNull()

    private fun _copyScalingList(r: BitReader, w: BitWriter, size: Int) {
        var last = 8; var next = 8
        for (j in 0 until size) {
            if (next != 0) { next = (last + r.copySe(w) + 256) % 256 }
            last = if (next == 0) last else next
        }
    }

    private fun _copyHrd(r: BitReader, w: BitWriter) {
        val cpb = r.copyUe(w)
        r.copy(w, 8) // bit_rate_scale, cpb_size_scale
        repeat(cpb + 1) { r.copyUe(w); r.copyUe(w); r.copy(w, 1) }
        r.copy(w, 20) // four 5-bit length fields
    }

    // (start-code offset, payload offset) for each NAL
    private fun _nalStarts(b: ByteArray): List<Pair<Int, Int>> {
        val out = ArrayList<Pair<Int, Int>>()
        var i = 0
        while (i + 2 < b.size) {
            if (b[i].toInt() == 0 && b[i + 1].toInt() == 0 && b[i + 2].toInt() == 1) {
                val prefix = if (i > 0 && b[i - 1].toInt() == 0) i - 1 else i
                out.add(prefix to i + 3); i += 3
            } else i++
        }
        return out
    }

    private fun _unescape(b: ByteArray, from: Int): ByteArray {
        val out = ByteArrayOutputStream(b.size)
        var zeros = 0
        for (k in from until b.size) {
            val v = b[k].toInt() and 0xFF
            if (zeros >= 2 && v == 3) { zeros = 0; continue }
            out.write(v); zeros = if (v == 0) zeros + 1 else 0
        }
        return out.toByteArray()
    }

    private fun _escape(b: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(b.size + 8)
        var zeros = 0
        for (x in b) {
            val v = x.toInt() and 0xFF
            if (zeros >= 2 && v <= 3) { out.write(3); zeros = 0 }
            out.write(v); zeros = if (v == 0) zeros + 1 else 0
        }
        return out.toByteArray()
    }

    private val HIGH_PROFILES = setOf(100, 110, 122, 244, 44, 83, 86, 118, 128, 138, 139, 134, 135)

    class BitReader(private val b: ByteArray) {
        private var pos = 0
        fun read(n: Int): Int {
            var v = 0
            repeat(n) {
                val bit = if (pos / 8 < b.size) (b[pos / 8].toInt() shr (7 - pos % 8)) and 1 else error("SPS too short")
                v = (v shl 1) or bit; pos++
            }
            return v
        }
        fun copy(w: BitWriter, n: Int): Int = read(n).also { w.write(it, n) }
        fun copyUe(w: BitWriter): Int {
            var zeros = 0
            while (read(1) == 0) { zeros++; check(zeros < 32) }
            val v = (1 shl zeros) - 1 + (if (zeros > 0) read(zeros) else 0)
            w.writeUe(v); return v
        }
        fun copySe(w: BitWriter): Int {
            var zeros = 0
            while (read(1) == 0) { zeros++; check(zeros < 32) }
            val k = (1 shl zeros) - 1 + (if (zeros > 0) read(zeros) else 0)
            w.writeUe(k)
            return if (k % 2 == 1) (k + 1) / 2 else -(k / 2)
        }
    }

    class BitWriter {
        private val out = ByteArrayOutputStream()
        private var cur = 0
        private var n = 0
        fun write(v: Int, bits: Int) {
            for (i in bits - 1 downTo 0) {
                cur = (cur shl 1) or ((v ushr i) and 1); n++
                if (n == 8) { out.write(cur); cur = 0; n = 0 }
            }
        }
        fun writeUe(v: Int) {
            val x = v + 1
            val len = 32 - Integer.numberOfLeadingZeros(x)
            write(0, len - 1); write(x, len)
        }
        fun alignZero() { if (n > 0) write(0, 8 - n) }
        fun bytes(): ByteArray = out.toByteArray()
    }
}
