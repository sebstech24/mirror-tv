# Receiver service: state and settings for the UI

Written by the service helper, 24 Oct 2026. Answers `UX-NEEDS-FROM-SERVICE.md`.

## New state on `AirPlayService` (all `StateFlow`, safe to collect from the UI)

| Flow | Type | Meaning |
|---|---|---|
| `serverName` | `String` | Name the Mac sees in Screen Mirroring (the pref, or the TV's own device name when the pref is blank). `""` until the server has started once. |
| `clientName` | `String?` | Sender's own name, e.g. "Seb's MacBook Air". Arrives with the sender's SETUP, so it can be null for the first moment of "connecting". Null when nobody is connected. |
| `clientModel` | `String?` | Sender's model id, e.g. "Mac14,2" (starts with "Mac" for Macs, "iPhone"/"iPad" otherwise). Null when idle. |
| `lastDisconnect` | `DisconnectReason?` | Why the last session ended. Null until one ends; reset to null when a new session starts from idle. |

`AirPlayService.DisconnectReason` (note the names differ from the UX doc's suggestion):

- `SENDER_STOPPED`: clean end (Mac stopped mirroring, lid closed, slept). The UX doc's `STOPPED_BY_SENDER`.
- `CONNECTION_LOST`: the mirror stream broke (Wi-Fi, sender crash).
- `REPLACED`: another sender took over the TV. Set while the new sender is already connecting, so `connectionCount` stays above 0.
- `NETWORK_CHANGED`: the TV's own network changed during the session.

"Connecting" needs no new flow: `connectionCount > 0 && !mirroringActive && !audioOnly && !videoPlaybackActive`.

`AirPlayService.EXTRA_FROM_CONNECT` (`"from_connect"`, boolean) is on the MainActivity intent when a sender connecting brought the app to the front (direct start or the alert notification). The UI can use it to go straight to the connecting/mirror screen.

## Settings keys

| Key | Pref | Default | Notes |
|---|---|---|---|
| `lowest_latency` | `Prefs.LOWEST_LATENCY` | off | One switch: smallest adaptive audio cushion, low-latency audio decoder + output, frames shown as soon as decoded, frame drops allowed. The video decoder already asks for `KEY_LOW_LATENCY` on every TV. The server restarts by itself ~0.5 s after it flips (this ends a running session, like the other video settings). |
| `boot_auto_start` | `Prefs.BOOT_AUTO_START` | on | Start the receiver when the TV boots. Only works after the app has been opened once (Android rule). |
| `launch_on_connect` | `Prefs.LAUNCH_ON_CONNECT` | on | Bring the mirror screen to the front when a sender connects. |
| `server_name` | `Prefs.SERVER_NAME` | `""` | Blank means "use `Context.defaultServerName()`" (TV's device name, else model). The old default "Android AirPlay" is also treated as blank. |
| `advertise_audio` | `Prefs.ADVERTISE_AUDIO` | on | **Ignored now**: audio is always advertised (turning it off ended Mac sessions after 64 s). Please remove the switch from Settings. |

## Coming to the front from the background

Order the service tries when a sender connects and the app isn't visible:

1. `startActivity` directly. Android allows it only if "Display over other apps" is granted (or the app is already visible). A blocked start fails silently.
2. If the overlay permission isn't granted: a high-priority notification "Seb's MacBook Air wants to show its screen / Select to show it on the TV" with a **full-screen intent**. Where the TV's system UI honours full-screen intents it opens the mirror screen by itself; otherwise it shows as a notification the user can select. It disappears when the mirror screen shows its video surface, when the sender leaves, or after 30 s.

The UI can still offer the "Display over other apps" shortcut as the fallback for TVs where step 2 only shows a notification.

## Strings

The alert notification uses `res/values/service_strings.xml` (`svc_alert_*`), kept apart from `strings.xml`. The persistent notification still uses `notification_title`/`notification_text` from `strings.xml` ("AirPlay Server Running" etc.): those belong to the UI helper's rebrand.
