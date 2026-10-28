package io.github.jqssun.airplay.billing

/**
 * When Google's In-App Review card may appear (marketing plan, Sebastian approved 28 Oct 2026): after a good mirroring
 * session (the Mac stopped it, 10+ minutes of picture), never in the very first session, at most once per app version.
 * No rewards and no "do you like it?" pre-prompt (Play policy). Pure Kotlin, unit-tested.
 */
object ReviewPolicy {
    const val MIN_SESSION_MS = 10 * 60_000L

    fun goodSession(mirroredMs: Long, stoppedByMac: Boolean) = stoppedByMac && mirroredMs >= MIN_SESSION_MS

    /** [sessionsSoFar] counts the session that just ended. */
    fun shouldAsk(sessionsSoFar: Int, good: Boolean, lastAskedVersion: Long, version: Long) =
        good && sessionsSoFar >= 2 && lastAskedVersion != version
}
