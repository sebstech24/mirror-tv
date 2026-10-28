package io.github.jqssun.airplay.billing

/**
 * Free-tier clock: a mirroring session may run [limitMs], with a notice shortly before the end.
 * Pure Kotlin (the caller passes the time), so it is unit-tested without Android or Play.
 */
class SessionLimiter {
    sealed interface State {
        data object Idle : State
        data class Running(val remainingMs: Long) : State
        data class Warning(val remainingMs: Long) : State
        // returned once; the caller ends the session and shows the paywall
        data object Expired : State
    }

    private var startedAt = -1L
    private var limitMs = 0L

    /**
     * A session starts once mirroring shows a picture and ends when the sender disconnects;
     * reconnecting starts a new free session. Unlocked = no limit.
     */
    fun tick(now: Long, connected: Boolean, mirroring: Boolean, unlocked: Boolean, limitMs: Long): State {
        if (!connected || unlocked) {
            startedAt = -1
            return State.Idle
        }
        if (startedAt < 0) {
            if (!mirroring) return State.Idle
            startedAt = now
            this.limitMs = limitMs
        }
        val left = this.limitMs - (now - startedAt)
        return when {
            left <= 0 -> { startedAt = -1; State.Expired }
            left <= warnBefore(this.limitMs) -> State.Warning(left)
            else -> State.Running(left)
        }
    }

    companion object {
        const val FREE_LIMIT_MS = 20 * 60_000L
        const val DEBUG_LIMIT_MS = 60_000L
        private const val WARN_BEFORE_MS = 2 * 60_000L

        // 2 minutes before the end; half the session for the 1-minute debug limit
        fun warnBefore(limitMs: Long) = minOf(WARN_BEFORE_MS, limitMs / 2)

        /** Play's answer wins when Play was reachable (non-null); otherwise the cached answer. */
        fun unlocked(playOwned: Boolean?, cached: Boolean, debugFake: Boolean) = debugFake || (playOwned ?: cached)

        // Play Console product ids (Sebastian, 28 Oct 2026: Monthly $4.99 with a 7-day trial, Lifetime $49.99)
        const val MONTHLY_ID = "screenbridge_monthly"
        const val MONTHLY_BASE_PLAN = "monthly"
        const val LIFETIME_ID = "screenbridge_lifetime"

        /** Either an active Monthly subscription or Lifetime unlocks; nothing else does. */
        fun unlocks(boughtProductIds: Collection<String>) = MONTHLY_ID in boughtProductIds || LIFETIME_ID in boughtProductIds

        /** Play's free-trial length ("P7D", "P1W") in days; null for anything else. */
        fun trialDays(isoPeriod: String): Int? {
            val m = Regex("P(\\d+)([DW])").matchEntire(isoPeriod) ?: return null
            val n = m.groupValues[1].toInt()
            return if (m.groupValues[2] == "W") n * 7 else n
        }
    }
}
