package io.github.jqssun.airplay.billing

import io.github.jqssun.airplay.billing.SessionLimiter.Companion.FREE_LIMIT_MS
import io.github.jqssun.airplay.billing.SessionLimiter.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLimiterTest {
    private val min = 60_000L
    private fun SessionLimiter.at(t: Long, connected: Boolean = true, mirroring: Boolean = true, unlocked: Boolean = false) =
        tick(t, connected, mirroring, unlocked, FREE_LIMIT_MS)

    @Test fun idleUntilMirroringStarts() {
        val l = SessionLimiter()
        assertEquals(State.Idle, l.at(0, connected = false))
        assertEquals(State.Idle, l.at(5 * min, mirroring = false))
        // clock starts at the first picture, not at connect
        assertEquals(State.Running(20 * min), l.at(10 * min))
    }

    @Test fun warnsAt18AndExpiresAt20() {
        val l = SessionLimiter()
        l.at(0)
        assertTrue(l.at(18 * min - 1) is State.Running)
        assertEquals(State.Warning(2 * min), l.at(18 * min))
        assertEquals(State.Warning(1), l.at(20 * min - 1))
        assertEquals(State.Expired, l.at(20 * min))
    }

    @Test fun mirroringBlipDoesNotResetClock() {
        val l = SessionLimiter()
        l.at(0)
        l.at(10 * min, mirroring = false)
        assertEquals(State.Running(5 * min), l.at(15 * min))
    }

    @Test fun reconnectGivesNewFreeSession() {
        val l = SessionLimiter()
        l.at(0)
        assertEquals(State.Expired, l.at(20 * min))
        assertEquals(State.Idle, l.at(20 * min + 1, connected = false))
        assertEquals(State.Running(20 * min), l.at(21 * min))
    }

    @Test fun expiredIsReportedOnce() {
        val l = SessionLimiter()
        l.at(0)
        assertEquals(State.Expired, l.at(20 * min))
        // still connected for a moment while the session shuts down: a new session starts, no second expiry
        assertTrue(l.at(20 * min + 500) is State.Running)
    }

    @Test fun unlockedHasNoLimit() {
        val l = SessionLimiter()
        assertEquals(State.Idle, l.at(0, unlocked = true))
        assertEquals(State.Idle, l.at(100 * min, unlocked = true))
        // buying mid-session cancels the running clock
        l.at(200 * min)
        assertEquals(State.Idle, l.at(219 * min, unlocked = true))
    }

    @Test fun debugLimitWarnsAtHalf() {
        val l = SessionLimiter()
        val lim = SessionLimiter.DEBUG_LIMIT_MS
        l.tick(0, true, true, false, lim)
        assertEquals(State.Warning(30_000), l.tick(30_000, true, true, false, lim))
        assertEquals(State.Expired, l.tick(60_000, true, true, false, lim))
    }

    @Test fun entitlement() {
        assertTrue(SessionLimiter.unlocked(playOwned = true, cached = false, debugFake = false))
        // Play reachable and says not owned (e.g. refunded): cache is overruled
        assertFalse(SessionLimiter.unlocked(playOwned = false, cached = true, debugFake = false))
        // offline: trust the cache
        assertTrue(SessionLimiter.unlocked(playOwned = null, cached = true, debugFake = false))
        assertFalse(SessionLimiter.unlocked(playOwned = null, cached = false, debugFake = false))
        assertTrue(SessionLimiter.unlocked(playOwned = false, cached = false, debugFake = true))
    }
}
