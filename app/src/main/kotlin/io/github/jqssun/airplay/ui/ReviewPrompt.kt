package io.github.jqssun.airplay.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import io.github.jqssun.airplay.Prefs
import io.github.jqssun.airplay.billing.ReviewPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Google's In-App Review card, shown by [ReviewPolicy]'s rules. Only appears on a Play-installed build. */
object ReviewPrompt {
    // not in Prefs.all: neither "Reset all settings" nor adb should touch these
    private const val SESSIONS = "review_sessions"
    private const val ASKED = "review_asked_version"
    private val _due = MutableStateFlow(false)
    val due = _due.asStateFlow()

    /** Called by the service when the last sender disconnects. */
    fun onSessionEnded(ctx: Context, mirroredMs: Long, stoppedByMac: Boolean) {
        val p = ctx.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        val n = p.getInt(SESSIONS, 0) + 1
        p.edit { putInt(SESSIONS, n) }
        if (ReviewPolicy.shouldAsk(n, ReviewPolicy.goodSession(mirroredMs, stoppedByMac), p.getLong(ASKED, -1), _version(ctx))) {
            _due.value = true
        }
    }

    /** From a visible screen only (the caller makes sure it isn't the paywall and nothing is mirroring). */
    suspend fun show(activity: Activity) {
        _due.value = false
        activity.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE).edit { putLong(ASKED, _version(activity)) }
        runCatching {
            val manager = ReviewManagerFactory.create(activity)
            manager.launchReview(activity, manager.requestReview())
        }.onFailure { Log.w("ReviewPrompt", "review card not shown", it) }
    }

    @Suppress("DEPRECATION")
    private fun _version(ctx: Context): Long = ctx.packageManager.getPackageInfo(ctx.packageName, 0).let {
        if (android.os.Build.VERSION.SDK_INT >= 28) it.longVersionCode else it.versionCode.toLong()
    }
}
