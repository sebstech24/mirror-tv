package io.github.jqssun.airplay.billing

/**
 * Who may mirror: an active Monthly subscription (its free trial included) or Lifetime. There is no free tier
 * (Sebastian, 28 Oct 2026). Pure Kotlin, so it is unit-tested without Android or Play.
 */
object Entitlement {
    // Play Console product ids: Monthly $4.99 with a 7-day trial, Lifetime $49.99
    const val MONTHLY_ID = "telebeam_monthly"
    const val MONTHLY_BASE_PLAN = "monthly"
    const val LIFETIME_ID = "telebeam_lifetime"

    /** Either an active Monthly subscription or Lifetime unlocks; nothing else does. */
    fun unlocks(boughtProductIds: Collection<String>) = MONTHLY_ID in boughtProductIds || LIFETIME_ID in boughtProductIds

    /** Play's answer wins when Play was reachable (non-null); otherwise the cached answer. */
    fun unlocked(playOwned: Boolean?, cached: Boolean, debugFake: Boolean) = debugFake || (playOwned ?: cached)

    /** One purchase as Google Play reports it (bought in the app, or redeemed from a promo code in the Play Store). */
    data class Owned(val products: List<String>, val purchased: Boolean, val pending: Boolean)

    /** What the app should show: unlocked, which plan did it (Lifetime wins), and a slow payment still pending. */
    data class State(val unlocked: Boolean, val plan: String?, val pending: Boolean)

    fun evaluate(items: List<Owned>): State {
        val mine = items.filter { o -> o.products.any { it == MONTHLY_ID || it == LIFETIME_ID } }
        val ids = mine.filter { it.purchased }.flatMap { it.products }.toSet()
        val plan = if (LIFETIME_ID in ids) LIFETIME_ID else if (MONTHLY_ID in ids) MONTHLY_ID else null
        return State(unlocks(ids), plan, mine.any { it.pending })
    }

    /** Google Play's redeem page, with the code filled in when the user typed one (spaces and dashes dropped). */
    fun redeemUri(code: String?): String {
        val c = code.orEmpty().filter { it.isLetterOrDigit() }.uppercase()
        return if (c.isEmpty()) REDEEM_URL else "$REDEEM_URL?code=$c"
    }
    const val REDEEM_URL = "https://play.google.com/redeem"

    /** Play's free-trial length ("P7D", "P1W") in days; null for anything else. */
    fun trialDays(isoPeriod: String): Int? {
        val m = Regex("P(\\d+)([DW])").matchEntire(isoPeriod) ?: return null
        val n = m.groupValues[1].toInt()
        return if (m.groupValues[2] == "W") n * 7 else n
    }
}
