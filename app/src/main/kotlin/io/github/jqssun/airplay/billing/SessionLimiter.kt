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
    }
}
