# MirrorTV: what to improve before charging money

Written 24 Oct 2026 from a read of the fork's code (upstream v0.0.36, `io.github.jqssun.airplay`), all 34 upstream GitHub issues, and web reviews of AirScreen. Nothing was built or tested on a TV for this note. Companion to `PLAY-STORE.md`.

## Where the app stands today (seen from a TV remote)

- **First run:** Android asks for notifications and "nearby devices" right away, with no explanation. The server starts by itself after that. There is no welcome screen and nothing tells you what to do on the Mac.
- **Waiting screen:** a grey box with a cast icon and "Waiting for connection...". Below it, a card with the name ("Android AirPlay" by default), "0 connected" and a **Stop** button. On a TV the remote's focus lands **on Stop**, so pressing OK once switches the receiver off (`ui/MainScreen.kt` ~613, `focusRequester(startFocus)`).
- **Layout:** a phone-style app with three tabs: Overview, **Logs**, Settings. Settings has about 35 switches, many of them engineer words ("MediaFormat.KEY_COLOR_TRANSFER", "Advertise ALAC codec", "Oboe buffer").
- **Mirroring:** goes full screen by itself (setting on). The screen stays awake only while the app is in front and someone is connected (`MainActivity.kt`, `KEEP_SCREEN_ON`).
- **Mac disconnects:** the app quietly goes back to "Waiting for connection...". No message, no reason, no "reconnect" help. Nothing on the Mac side can reconnect automatically either.
- **Background / boot:** "Start at boot" and "Run in background" are on by default (`Prefs.kt`, `BootReceiver.kt`). When a Mac connects while the app is in the background, the app tries to jump to the front, but that needs the "Display over other apps" permission, which most Google TV models have no screen to grant. So on many TVs the Mac connects, but nothing shows until the user opens the app by hand.
- **Branding:** "AirPlay Server" app name, "Android AirPlay" receiver name, "AirPlay Server Running" notification, upstream icon and banner, package `io.github.jqssun.airplay`. No About screen, no licenses screen, no link to source code.

## Real user problems from upstream issues (github.com/jqssun/android-airplay-server/issues)

| Issue | What users hit | Status in this fork |
|---|---|---|
| [#54](https://github.com/jqssun/android-airplay-server/issues/54) | **Mac mirroring ends by itself**: (1) after 64 s if "Advertise audio" is off, (2) whenever a Bluetooth beacon probe arrives and "Allow new connections" is on (the default). | Fix (2) is in as patch `0007-keep-session-on-ble-discovery-query.patch`. Fix (1) is just a setting: **remove that switch** (see Remove list). Needs a long Mac test to confirm. |
| [#27](https://github.com/jqssun/android-airplay-server/issues/27) | Mac to Google TV: audio about **1 second late**. | Closed with no stated fix. Must test. |
| [#72](https://github.com/jqssun/android-airplay-server/issues/72) | Image and sound stutter together on a Sony TV; better on 5 GHz. | Open. |
| [#50](https://github.com/jqssun/android-airplay-server/issues/50) | Hisense (MediaTek) TV: mirroring dies in under a second (system security blocks a memory call). | Open. Cheap MediaTek TVs are a big share of Google TV. |
| [#7](https://github.com/jqssun/android-airplay-server/issues/7), [#30](https://github.com/jqssun/android-airplay-server/issues/30) | Black screen with sound only on older Google TVs. | Fixed upstream (keyframe handling). Keep. |
| [#9](https://github.com/jqssun/android-airplay-server/issues/9), [#38](https://github.com/jqssun/android-airplay-server/issues/38) | TV screensaver/sleep during mirroring. | Fixed only while app is in front. |
| [#35](https://github.com/jqssun/android-airplay-server/issues/35) | Remote gets stuck in a slider; can't save the name without a mouse. | Fixed. Shows the settings were built for touch first. |
| [#65](https://github.com/jqssun/android-airplay-server/issues/65) | Can't install upstream on a TV from the Play website. | Upstream's Play listing isn't set up for TV. **Opening for us**: be the one that installs on TV from Play. |
| [#26](https://github.com/jqssun/android-airplay-server/issues/26), [#8](https://github.com/jqssun/android-airplay-server/issues/8) | Wants dark mode / no bright flash. | Use a dark TV-only design. |

## AirScreen (the paid leader): what people complain about

- Ads, and a subscription needed to remove them ([Overclockers forum](https://www.overclockers.co.uk/forums/posts/33523692), [Sony community](https://community.sony.rs/t5/android-tv/kd55a85-bu-if-i-uninstall-airscreen-it-is-no-longer-visible-on-my/td-p/3799918)).
- Lag far behind a real Apple TV on the same Wi-Fi ([Overclockers](https://www.overclockers.co.uk/forums/posts/33523692)).
- Crashes, disconnects, playback glitches, "aggressive monetization" (review summary on [Chrome-Stats](https://chrome-stats.com/d/com.ionitech.airscreen)).
- Own changelog warns some TV models aren't fully supported ([APKMirror](https://apkmirror.com/apk/ionitech/airscreen-airplay-miracast/variant-%7B%22arches_slug%22%3A%5B%22x86_64%22%5D%7D)).
- Subscription prices about $12 to $20 a year (vendor blog, [PigeonCast](https://pigeoncast.com/blogs/is-airscreen-free)).

The pitch that beats both: **"Made for Mac. Pay once. No ads. Turn on the TV and your Mac finds it."**

## Ranked improvements

Effort: S = a day or less, M = a few days, L = a week or more. Risk = chance of breaking something or not working.

### (a) Must-have before a paid launch

1. **TV-first welcome that shows the exact Mac clicks.**
   You notice: a big full-screen guide: "On your Mac: click Control Center (top-right) → Screen Mirroring → pick *Living Room TV*". Shows the TV's name in huge letters, plus "Same Wi-Fi as your Mac: ✓ (network name)". Upstream and AirScreen show a small "waiting" text.
   Effort M, risk low. Files: new `ui/WelcomeScreen.kt`, `ui/MainScreen.kt` (OverviewContent), `res/values/strings.xml`, `res/drawable/` (simple drawn Mac menu-bar picture, not Apple artwork).

2. **Fix the remote focus trap and make the home screen a single TV screen.**
   You notice: OK on the remote never switches the receiver off by accident; no Logs tab; Settings behind one gear button. Effort S, risk low. Files: `ui/MainScreen.kt` (Tab enum, `startFocus`), `ui/TvFocus.kt`.

3. **Rebrand everything.**
   You notice: your name and icon on the TV home row, in the Mac's Screen Mirroring list (default name like "Living Room TV" or the TV's own device name instead of "Android AirPlay"), notification and banner. New package id (required: you can't publish under `io.github.jqssun.airplay`). Effort M, risk medium (package rename touches the JNI glue: `native_bridge.cpp` function names follow the package). Files: `app/build.gradle.kts` (`applicationId`, `namespace`), `res/values/strings.xml`, `res/mipmap*`, `res/drawable/banner*`, `Prefs.kt` (`SERVER_NAME` default), `AndroidManifest.xml` (`SET_SETTING` actions), `cpp/native_bridge.cpp`, `fastlane/metadata/`.

4. **Mac reliability pass (from issue #54 and #27).**
   You notice: a Mac session runs for hours without dropping, and sound matches lips. Do: hard-wire audio advertising on, keep patch 0007, run 2-hour Mac tests on 2 to 3 real TVs (one Google TV, one cheap MediaTek, one Sony/Philips), measure audio delay and fix the default buffer. Effort L, risk medium. Files: `Prefs.kt` (`ADVERTISE_AUDIO`, `AUDIO_*`), `service/AirPlayService.kt`, `renderer/AudioRenderer.kt`, `cpp/audio_engine.cpp`, `cpp/patches/UxPlay/`.

5. **Show up even when the app is in the background (no "Display over other apps" dance).**
   You notice: the TV is on YouTube, you click Screen Mirroring on the Mac, and the TV switches to your Mac. Do: test on Google TV which method works without the overlay permission (full-screen notification intent, or keep the service plus a "Mac wants to mirror, press OK" card); if none work, say so in onboarding. Effort M, risk high (Android restricts starting screens from the background; varies by TV). Files: `service/AirPlayService.kt` (`launchMainActivity`, `shouldLaunchOnConnect`), `ui/SettingsScreen.kt` ~447.

6. **Clear messages when things go wrong.**
   You notice: instead of silently returning to "Waiting", a friendly line: "Your Mac stopped mirroring", "TV and Mac are on different Wi-Fi", "This video is protected (Apple TV app, Netflix) and can't be shown", "Wi-Fi is weak, move to 5 GHz". Effort M, risk low. Files: `service/AirPlayService.kt` (`onConnectionDestroy`, `onConnectionReset`, `_failStart`), `viewmodel/MainViewModel.kt`, `ui/MainScreen.kt`, `strings.xml`.

7. **About + licenses + source link screen (GPL, see below).** Effort S, risk low. Files: new `ui/AboutScreen.kt`, `ui/SettingsScreen.kt`, `res/raw/` (license texts).

8. **Pay-once unlock with a generous free mode.** (Pricing in `PLAY-STORE.md`.) You notice: a 15-minute free session ends with a calm "Unlock forever" card, never mid-sentence ads. Effort M, risk medium (Play Billing, test track). Files: new `billing/`, `app/build.gradle.kts`, `ui/MainScreen.kt`, `service/AirPlayService.kt` (session timer).

9. **Fix the MediaTek crash (#50) or block those TVs honestly.** Effort M to L, risk medium; needs the TV in hand. Files: `renderer/VideoPipeline.kt`, `cpp/native_bridge.cpp`.

10. **Play TV listing done right (#65).** TV distribution opted in, 1920×1080 screenshots, 320×180 banner, 16 KB-aligned native libs, "Works with Mac" in the description only. Effort S to M, risk low. Files: `app/build.gradle.kts`, `fastlane/metadata/`, `res/drawable/banner*`.

### (b) Differentiators worth paying for

11. **"Lowest lag" mode as one big switch** (instead of 6 hidden ones): 60 fps, decoder low-latency, small audio cushion, frame drops allowed. Show a live lag number. You notice: typing and mouse on the TV feel close to an Apple TV. Beats AirScreen's known lag. Effort M, risk medium (some TVs stutter; keep "Smooth" as the safe default). Files: `Prefs.kt` (`LOW_LATENCY`, `MAX_FPS`, `AUDIO_ADAPTIVE_STEP`, `SCHEDULED_OUTPUT_BUFFER_RELEASE`), `renderer/DecoderSelector.kt`, `renderer/VideoRenderer.kt`, `ui/SettingsScreen.kt`.

12. **Always ready: start on TV boot, works after the TV wakes, keeps the TV awake while mirroring even in the background.** Code already has boot start and a wake lock; add screen-keep-awake from the service, re-advertise on Wi-Fi change/wake, and a "Ready" chip in the notification. You notice: never have to open the app. Effort M, risk medium (TV makers kill background apps; test each brand). Files: `BootReceiver.kt`, `service/AirPlayService.kt`, `discovery/NsdServiceManager.kt`, `MainActivity.kt`.

13. **Fit-to-TV picture for Mac screens**: advertise the TV's real 1080p/4K size so the Mac offers "Optimize for TV", handle overscan, and a crop/zoom choice for 16:10 MacBook screens. You notice: no black bars or tiny text. Effort S to M, risk low. Files: `Prefs.kt` (`RESOLUTION`, `OVERSCANNED`), `service/AirPlayService.kt` (`_displaySize`), `ui/VideoSurfaceView.kt`.

14. **Multiple Macs, nicely**: show "Seb's MacBook is mirroring", optional "Ask before a new Mac takes over" with OK/Cancel on the remote, optional PIN for offices. Upstream just drops the old sender. Effort M, risk medium. Files: `service/AirPlayService.kt` (`onConnectionInit`), `Prefs.kt` (`ALLOW_NEW_CONN`, `REQUIRE_PIN`), `ui/MainScreen.kt`.

15. **Presenter extras**: big clock/"Mirroring from…" hidden overlay, quick "pause picture" (freeze frame while you switch windows), screensaver with your name when idle. Effort S each, risk low. Files: `ui/MainScreen.kt`, `renderer/VideoRenderer.kt`.

### (c) Later

- **Control My Mac tie-in**: the TV shows a QR code; scan it with Control My Mac on the phone and the phone becomes a trackpad for the Mac you're mirroring. Big cross-sell, but needs Control My Mac pairing work. Effort L.
- Wired mode marketing (upstream already has "USB only" via USB tethering, `discovery/UsbMdnsAdvertiser.kt`): useful for hotels and offices. Keep hidden until tested with a Mac.
- Languages (German, Spanish, French first), dark/light theme choice.
- iPhone/iPad as a "bonus" on the listing once Mac is rock solid.

## Remove or hide for a simple Mac-focused paid app

- **Logs tab** and log export: move to a hidden "Help → Send diagnostics" (`ui/LogsScreen.kt`).
- **"Advertise AirPlay audio support" switch**: turning it off breaks Mac mirroring after 64 s (#54). Hard-code on.
- **"Allow new connections"** as worded: replace with item 14.
- **Codec/engineer switches**: ALAC, AAC, software decoders, Oboe buffer, operating rate, KEY_COLOR_TRANSFER, KEY_PRIORITY, benchmark log, server port. Keep under a hidden "Advanced" (developer options already exist).
- **Video download button** (`download/VideoDownloader.kt`, `cd_download`): saves AirPlay video streams to the TV. Not a Mac mirroring feature and a copyright/Play-policy risk in a paid app. Remove.
- **Phone-only bits**: touch gestures, rotate, lock controls (`ui/gestures/`, parts of `ui/VideoControls.kt`) if shipping TV-only. Keep if you also sell on phones/tablets.
- **Music-receiver features** (now-playing screen, DACP remote): harmless, keep but don't advertise.
- **Settings broadcast receiver** (`SettingsReceiver.kt`): adb-only (needs DUMP), harmless; keep for testing.

## GPL and codec notes (not legal advice)

- **Whole app is GPL-3.0** (root `LICENSE`, UxPlay, its `playfair` FairPlay code is GPL-3.0 per `cpp/third_party/UxPlay/lib/playfair/LICENSE.md`). Selling is allowed; you must publish the full source of the exact version you ship (including the billing code and build scripts), keep upstream copyright notices, mark your changes, and not add restrictions. The paywall can be removed by anyone who builds it; that's fine.
- **Other parts:** libplist LGPL-2.1, llhttp MIT, OpenSSL (Apache-2.0, `openssl-cmake` MIT), Oboe / AndroidX / Media3 / Hilt Apache-2.0. All compatible with GPL-3.0.
- **FFmpeg is built LGPL, not GPL**: `cpp/cmake/BuildFFmpeg.cmake` uses `--disable-all` and enables only the ALAC and AAC decoders, with no `--enable-gpl` or `--enable-nonfree`. Good. Ship FFmpeg's license text.
- **Nothing in the app tells users any of this today**: no About screen, no license list, no source link. Item 7 fixes it (an "Open source licenses" screen plus "Source code: github.com/sebstech24/mirror-tv").
- **Patents:** video decoding (H.264/H.265) uses the TV's own hardware decoder, whose license the TV maker pays. The software **AAC** decoder in FFmpeg is the one patent question for a paid app: AAC-LC patents are mostly expired, but AAC-ELD (used by Mac mirroring audio) is newer and licensed by Via LA. Lowest-risk move: default to the TV's built-in AAC decoder (`FORCE_SW_AAC` is already off by default) and use software AAC only as a fallback; ask the lawyer from `PLAY-STORE.md` about it. ALAC is royalty-free (Apache-licensed by Apple).
- Trademarks: keep "AirPlay" out of the app name, receiver name and notification (all three use it today).
