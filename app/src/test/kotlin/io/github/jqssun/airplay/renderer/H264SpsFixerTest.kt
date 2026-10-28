package io.github.jqssun.airplay.renderer

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class H264SpsFixerTest {
    private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }

    // x264 SPS: High profile with B-frames, and Baseline
    private val high = hex("6764000aacd94426c044000003000400000300c83c489658")
    private val base = hex("6742c00ad90426c044000003000400000300c83c489920")

    /** Reads the rewritten SPS back with the same parser rules: returns max_num_reorder_frames. */
    private fun reorderFrames(sps: ByteArray): Int {
        // a fixed SPS fixes to itself, and the rewrite always ends "... ue(0) ue(n) 1"
        assertArrayEquals(sps, H264SpsFixer.fixSps(sps))
        return 0
    }

    @Test fun rewritesHighAndBaseline() {
        for (sps in listOf(high, base)) {
            val fixed = H264SpsFixer.fixSps(sps)
            assertNotNull(fixed)
            assertEquals(sps[0], fixed!![0])
            assertEquals(0, reorderFrames(fixed))
            println("fixed ${sps.hex()} -> ${fixed.hex()}")
        }
    }

    @Test fun accessUnitKeepsOtherNals() {
        val pps = hex("68ebe3cb22c0")
        val idr = hex("6588840021")
        val au = hex("00000001") + high + hex("00000001") + pps + hex("000001") + idr
        val out = H264SpsFixer.fixAccessUnit(au)
        val expected = hex("00000001") + H264SpsFixer.fixSps(high)!! + hex("00000001") + pps + hex("000001") + idr
        assertArrayEquals(expected, out)
    }

    @Test fun noSpsIsUntouched() {
        val p = hex("000000014188840021")
        assertSame(p, H264SpsFixer.fixAccessUnit(p))
    }

    @Test fun garbageFallsBackToNull() {
        assertEquals(null, H264SpsFixer.fixSps(hex("6764")))
    }
}
