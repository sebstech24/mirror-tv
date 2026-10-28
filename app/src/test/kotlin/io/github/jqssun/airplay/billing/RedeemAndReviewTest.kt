package io.github.jqssun.airplay.billing

import io.github.jqssun.airplay.billing.Entitlement.LIFETIME_ID
import io.github.jqssun.airplay.billing.Entitlement.MONTHLY_ID
import io.github.jqssun.airplay.billing.Entitlement.Owned
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedeemAndReviewTest {
    // a promo code redeemed in the Play Store shows up on the next purchase check like any purchase
    @Test fun redeemedLifetimeUnlocks() {
        val s = Entitlement.evaluate(listOf(Owned(listOf(LIFETIME_ID), purchased = true, pending = false)))
        assertTrue(s.unlocked); assertEquals(LIFETIME_ID, s.plan); assertFalse(s.pending)
    }

    @Test fun redeemedFreeSubscriptionPeriodUnlocks() {
        val s = Entitlement.evaluate(listOf(Owned(listOf(MONTHLY_ID), purchased = true, pending = false)))
        assertTrue(s.unlocked); assertEquals(MONTHLY_ID, s.plan)
    }

    @Test fun lifetimeWinsOverMonthly() {
        val s = Entitlement.evaluate(listOf(
            Owned(listOf(MONTHLY_ID), purchased = true, pending = false),
            Owned(listOf(LIFETIME_ID), purchased = true, pending = false),
        ))
        assertEquals(LIFETIME_ID, s.plan)
    }

    @Test fun pendingOrForeignDoesNotUnlock() {
        val pending = Entitlement.evaluate(listOf(Owned(listOf(LIFETIME_ID), purchased = false, pending = true)))
        assertFalse(pending.unlocked); assertTrue(pending.pending)
        val other = Entitlement.evaluate(listOf(Owned(listOf("some_other_app_item"), purchased = true, pending = false)))
        assertFalse(other.unlocked); assertEquals(null, other.plan)
        assertFalse(Entitlement.evaluate(emptyList()).unlocked)
    }

    @Test fun redeemLink() {
        assertEquals("https://play.google.com/redeem", Entitlement.redeemUri(null))
        assertEquals("https://play.google.com/redeem", Entitlement.redeemUri("  "))
        assertEquals("https://play.google.com/redeem?code=AB12CD34", Entitlement.redeemUri(" ab12-cd34 "))
        // nothing but letters and digits reaches the URL
        assertEquals("https://play.google.com/redeem?code=X1Y", Entitlement.redeemUri("x1&y=<"))
    }

    @Test fun reviewRules() {
        val ten = ReviewPolicy.MIN_SESSION_MS
        assertTrue(ReviewPolicy.goodSession(ten, stoppedByMac = true))
        assertFalse(ReviewPolicy.goodSession(ten - 1, stoppedByMac = true))
        assertFalse(ReviewPolicy.goodSession(3 * ten, stoppedByMac = false)) // connection lost
        // never in the first session
        assertFalse(ReviewPolicy.shouldAsk(1, good = true, lastAskedVersion = -1, version = 105))
        assertTrue(ReviewPolicy.shouldAsk(2, good = true, lastAskedVersion = -1, version = 105))
        // once per version
        assertFalse(ReviewPolicy.shouldAsk(5, good = true, lastAskedVersion = 105, version = 105))
        assertTrue(ReviewPolicy.shouldAsk(5, good = true, lastAskedVersion = 104, version = 105))
        assertFalse(ReviewPolicy.shouldAsk(5, good = false, lastAskedVersion = -1, version = 105))
    }
}
