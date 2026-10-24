package io.github.jqssun.airplay.service

import android.content.SharedPreferences
import io.github.jqssun.airplay.Prefs
import io.github.jqssun.airplay.get
import io.github.jqssun.airplay.observe
import io.github.jqssun.airplay.renderer.AudioConfig
import kotlinx.coroutines.flow.Flow

// lowest latency: adaptive cushion at its lowest percentile, low-latency decoder + output
fun readAudioConfig(p: SharedPreferences, fastest: Boolean = p[Prefs.LOWEST_LATENCY]) = AudioConfig(
    cushionMs = if (fastest || p[Prefs.AUDIO_AUTO_BUFFER]) 0 else p[Prefs.AUDIO_CUSHION_MS],
    percentilePct = Prefs.ADAPTIVE_PERCENTILES[if (fastest) 0 else p[Prefs.AUDIO_ADAPTIVE_STEP]],
    oboeBufferFrames = p[Prefs.OBOE_BUFFER_FRAMES],
    forceSwAlac = p[Prefs.FORCE_SW_ALAC],
    forceSwAac = p[Prefs.FORCE_SW_AAC],
    realtimePriority = p[Prefs.KEY_PRIORITY],
    lowLatency = fastest || p[Prefs.LOW_LATENCY],
    benchmarkLog = p[Prefs.BENCHMARK_LOG],
)

fun SharedPreferences.audioConfigFlow(): Flow<AudioConfig> = observe { readAudioConfig(this) }
