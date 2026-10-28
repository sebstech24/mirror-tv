package io.github.jqssun.airplay.billing

import android.content.Context
import android.os.SystemClock
import io.github.jqssun.airplay.Prefs
import io.github.jqssun.airplay.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Runs the free-session clock for the service and tells the UI when to warn and when to show the paywall. */
object FreeSession {
    private val _limiter = SessionLimiter()
    private val _state = MutableStateFlow<SessionLimiter.State>(SessionLimiter.State.Idle)
    val state = _state.asStateFlow()
    /** Why the paywall is up: the free session ran out, or the plans were opened from Settings. */
    enum class Paywall { EXPIRED, PLANS }
    private val _paywall = MutableStateFlow<Paywall?>(null)
    val paywall = _paywall.asStateFlow()

    fun showPlans() { _paywall.value = Paywall.PLANS }
    fun dismissPaywall() { _paywall.value = null }

    /** Called once by the service; [endSession] must disconnect the sender. */
    fun watch(
        scope: CoroutineScope,
        context: Context,
        connections: StateFlow<Int>,
        mirroring: StateFlow<Boolean>,
        endSession: () -> Unit,
    ) = scope.launch {
        val prefs = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        val debug = Billing.debuggable(context)
        while (isActive) {
            val unlocked = Billing.unlocked.value
            val limit = if (debug && prefs[Prefs.DEBUG_SHORT_SESSION]) SessionLimiter.DEBUG_LIMIT_MS else SessionLimiter.FREE_LIMIT_MS
            val s = _limiter.tick(SystemClock.elapsedRealtime(), connections.value > 0, mirroring.value, unlocked, limit)
            if (s == SessionLimiter.State.Expired) {
                _paywall.value = Paywall.EXPIRED
                endSession()
            } else if (unlocked || (connections.value > 0 && _paywall.value == Paywall.EXPIRED)) {
                // bought, or the Mac reconnected for another free session
                _paywall.value = null
            }
            _state.value = s
            delay(1_000)
        }
    }
}
