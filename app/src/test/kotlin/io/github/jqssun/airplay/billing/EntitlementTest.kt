package io.github.jqssun.airplay.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementTest {
    @Test fun monthlyUnlocks() = assertTrue(Entitlement.unlocks(setOf(Entitlement.MONTHLY_ID)))

    @Test fun lifetimeUnlocks() = assertTrue(Entitlement.unlocks(setOf(Entitlement.LIFETIME_ID)))

    @Test fun neitherStaysLocked() {
        // no free tier: nothing bought means the paywall and no receiver
        assertFalse(Entitlement.unlocks(emptySet()))
        // the retired one-time product and unknown ids don't unlock
        assertFalse(Entitlement.unlocks(setOf("screenbridge_unlimited")))
        assertFalse(Entitlement.unlocked(playOwned = Entitlement.unlocks(emptySet()), cached = false, debugFake = false))
    }

    @Test fun playAnswerWinsOverCache() {
        assertTrue(Entitlement.unlocked(playOwned = true, cached = false, debugFake = false))
        // Play reachable and says not owned (subscription ended, refund): cache is overruled
        assertFalse(Entitlement.unlocked(playOwned = false, cached = true, debugFake = false))
        // offline: trust the cache, so a paying customer isn't locked out
        assertTrue(Entitlement.unlocked(playOwned = null, cached = true, debugFake = false))
        assertFalse(Entitlement.unlocked(playOwned = null, cached = false, debugFake = false))
        assertTrue(Entitlement.unlocked(playOwned = false, cached = false, debugFake = true))
    }

    @Test fun trialLength() {
        assertEquals(7, Entitlement.trialDays("P7D"))
        assertEquals(7, Entitlement.trialDays("P1W"))
        assertEquals(null, Entitlement.trialDays("P1M"))
    }
}
