package io.github.jqssun.airplay.billing

/**
 * Who may mirror: an active Monthly subscription (its free trial included) or Lifetime. There is no free tier
 * (Sebastian, 28 Oct 2026). Pure Kotlin, so it is unit-tested without Android or Play.
 */
object Entitlement {
    // Play Console product ids: Monthly $4.99 with a 7-day trial, Lifetime $49.99
    const val MONTHLY_ID = "screenbridge_monthly"
    const val MONTHLY_BASE_PLAN = "monthly"
    const val LIFETIME_ID = "screenbridge_lifetime"

    /** Either an active Monthly subscription or Lifetime unlocks; nothing else does. */
    fun unlocks(boughtProductIds: Collection<String>) = MONTHLY_ID in boughtProductIds || LIFETIME_ID in boughtProductIds

    /** Play's answer wins when Play was reachable (non-null); otherwise the cached answer. */
    fun unlocked(playOwned: Boolean?, cached: Boolean, debugFake: Boolean) = debugFake || (playOwned ?: cached)

    /** Play's free-trial length ("P7D", "P1W") in days; null for anything else. */
    fun trialDays(isoPeriod: String): Int? {
        val m = Regex("P(\\d+)([DW])").matchEntire(isoPeriod) ?: return null
        val n = m.groupValues[1].toInt()
        return if (m.groupValues[2] == "W") n * 7 else n
    }
}
