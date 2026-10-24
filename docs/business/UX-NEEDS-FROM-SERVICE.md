# What the TV screens need from the service

Written by the UI helper, 24 Oct 2026. The UI works without these today (it falls back to generic text); each item makes a message more specific.

1. **Sender name** (e.g. "Seb's MacBook Air"), as `AirPlayService.clientName: StateFlow<String?>` (null when unknown / nobody connected).
   The service helper already added `onClientRequest(name, model)`; exposing it as a flow is all that's needed.
   Used for: "Seb's MacBook Air is connecting…" and the 3-second "Mirroring from Seb's MacBook Air" toast. Today: "Your Mac is connecting…" / "Mirroring started".
   UI hook: `MainViewModel.updateFromService()` + `ConnectingContent` / `FullscreenVideo` in `ui/`.
2. **Why the last session ended**, as `AirPlayService.lastDisconnect: StateFlow<DisconnectReason?>` with at least `STOPPED_BY_SENDER` (clean teardown / POST /stop) vs `CONNECTION_LOST` (socket reset, timeout; `onConnectionReset(reason)`).
   Used for: "Mirroring stopped on the Mac" vs "Connection lost — check Wi‑Fi". Today one combined message is shown for 8 s after the connection count drops to 0.
3. **Lowest latency**: the UI shows `Prefs.LOWEST_LATENCY` as a top-level "Lowest latency" switch in Settings. The pref is marked `restart = true`; please make sure flipping it applies without the user having to turn the receiver off and on.
4. **Blank `SERVER_NAME`**: the UI shows `Context.defaultServerName()` when the pref is blank (home screen, TV-name hint in Settings). Keep that function name stable.
