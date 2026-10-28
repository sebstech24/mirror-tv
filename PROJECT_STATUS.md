# Mirror TV — project status

## NAME DECIDED: Telebeam (Sebastian, 29 Oct 2026; "Screenbridge" clashed with an existing TV company)
- App name on the TV "Telebeam"; Play title "Telebeam: Screen Mirroring TV"; package `com.sebstech.telebeam`
  (was com.sebstech.screenbridge; changed before any Play upload). Products: `telebeam_monthly` (base plan
  `monthly`, 7-day trial) and `telebeam_lifetime`. Repo stays sebstech24/mirror-tv. New TV banner (text swapped,
  same art): design/banner.png, design/play-tv-banner-1280x720.png, res/drawable-xhdpi/banner.png.
- Release file: dist/telebeam-1.0.0-104.aab (versionCode 104). minSdk raised 23 -> 24 (Android 7) because Play's
  automatic protection (anti-piracy for paid apps) requires it; drops only ~2016 Android 6 TVs.
- 1.0.1 PREPARED (29 Oct, not uploaded): dist/telebeam-1.0.1-105.aab (versionCode 105). Adds "Redeem code" on the
  paywall and in Settings (opens Play's redeem page / Play Store app with the code; purchases re-checked on return, so a
  redeemed Lifetime or free subscription period unlocks by itself) and Google's In-App Review card (after a session the
  Mac stopped, 10+ min of picture, never the first session, once per version, never on the paywall or while mirroring;
  billing/ReviewPolicy.kt + ui/ReviewPrompt.kt). 15 unit tests. R8 needs -dontwarn for a gms annotation (proguard-rules).
- Older "Screenbridge" mentions below are history.

Paid, Mac-focused Android TV / Google TV screen-mirroring receiver (Mac's built-in Screen Mirroring → TV).
Fork of jqssun/android-airplay-server (GPL-3.0, based on UxPlay). Public repo: github.com/sebstech24/mirror-tv,
work branch `tv-edition` (main = untouched upstream). Owner: Sebastian.

## PRICING DECIDED (Sebastian, 28 Oct 2026: "make lifetime 50$ and monthly 5$"; later the same day: "Don't give them
the 20-minute counter before the paywall. Give them two options, A monthly with a seven-day free trial Or lifetime instant unlock.")
- NO free tier any more: the free 20-minute sessions are removed (no timer, no "minutes left" notice, no mid-session drop).
- Monthly: $4.99 subscription with a 7-day free trial (introductory offer), Family Sharing OFF.
- Lifetime: $49.99 one-time, unlocks instantly, Family Sharing OFF.
- Replaces the 25 Oct one-time $7.99 / $4.99 launch-price plan in docs/business/SALES-PLAN.md.
- Sebastian asked to publish once ready (28 Oct). Release work: see "Play release" chats.

## RECEIVER FIXES, real-TV night 28 Oct 2026 (Philips TV, MacBook Air; versionCode 102)
Sebastian's report: Mac mirroring stuttered (iPhone smooth); changing a setting dropped the Mac; OK showed faint icons;
Home text ran off screen. Measured with the benchmark log (benchmark_log=true: per second fps/out/drawn, decode wait,
input gaps, arrival jitter vs the Mac's timestamps, smoothing delay). Findings and fixes:
- Decoded pictures were only taken out when the NEXT frame arrived. The Mac sends unevenly, so pictures waited (A/B on
  the TV: wait 133 -> 70 ms avg, worst 554 -> 162 ms, frame-timing spread 50 -> 11 ms). Now a dedicated output thread.
- The GL loop merged "frame available" signals and left frames queued; now it latches every queued frame.
- The Mac's frames arrive bunched about once a second (~100 ms off their own timing; our own waiting was 1-6 ms, so
  it's Mac/Wi-Fi). Adaptive smoothing: hold pictures to the Mac's clock plus the measured spread (max 66 ms = what
  the decoder's ~6 output buffers allow), rises fast, falls slowly; switches itself on per session.
- SPS rewrite (max_num_reorder_frames=0, H264SpsFixer + 4 tests) and an access unit delimiter after each frame, so
  the decoder doesn't hold pictures waiting for more input.
- MediaTek vendor keys "game-mode"/"low-latency-mode" route pictures to a hidden video plane: BLACK SCREEN. Never set.
- Lowest latency (now OFF by default) also enabled decoder frame-drop = small skips; off was smooth to Sebastian's eye.
- 4K: the Mac only sends ~48 uneven fps over H.264 at 4K (0.55 Mbps, decode fine). Auto now = max 1080p; 4K stays an
  explicit choice ("may stutter"). Fps hard-capped at 60 everywhere; HDR never (decoder pinned to SDR BT.709).
- Settings never drop a connected Mac: restart-type settings wait for the disconnect ("takes effect the next time your
  Mac connects"); Lowest latency applies live. "Reset all settings" at the bottom of Settings (keeps purchases).
- OK and Back open one control bar while mirroring (who's mirroring, size, Settings / Picture-in-picture / Hide).
- Home: overscan-safe margins and shrink-to-fit (the TV draws text bold/outlined, which made it overflow).
- The TV itself runs its panel at 100/120 Hz with its own motion processing; the Mac never sends more than 60.
Still open: some residual hitches when the Mac pauses >0.2 s (Mac side); Philips "long-press Home" TV-settings
case not tested yet; In-App Review prompt for 1.0.1 (marketing chat's spec, after submission); app rename pending.

## FIRST PLAY RELEASE BUILT (28 Oct 2026): 1.0.0, versionCode 101
- Signed AAB: `dist/screenbridge-1.0.0-101.aab` (not in git; rebuild with `./gradlew bundleRelease`). 100 was the
  earlier build that still had free sessions; don't upload it.
- Upload key: `~/.secrets/screenbridge-upload.jks` (alias `upload`), password in vault `SCREENBRIDGE_UPLOAD_KEY_PASSWORD`,
  the key file itself backed up as vault `SCREENBRIDGE_UPLOAD_KEY`. `local.properties` points at it (git-ignored).
  Play App Signing holds the real app key.
- Paywall up front: without an active `screenbridge_monthly` (subscription, base plan `monthly`, 7-day trial offer)
  or `screenbridge_lifetime` (one-time) the app opens on the paywall and the receiver is OFF, so the Mac never lists a
  TV it can't use. Buying (or a restore) closes the paywall and starts the receiver at once. A subscription that
  ends turns the receiver off and brings the paywall back. Restore runs on every app start/resume and from
  "Restore purchases". Prices and trial length come from Play. "Not now" / Back leaves the app.
  Logic: billing/Entitlement.kt (5 unit tests), billing/Billing.kt; gate in AirPlayService._startServer.
- Paywall design (docs/screens/paywall-plans.png): pitch on the left, two D-pad cards on the right (Monthly focused
  first), Not now / Restore, renewal terms and the privacy policy link (About is behind the paywall).
- Settings (docs/screens/settings-plan-card.png): plan card on top (Monthly: manage/cancel in Play; Lifetime: thanks),
  "Mirroring" section, 720p/1080p/4K labels (Custom/Portrait/Landscape only under Advanced), "Mac controls TV volume"
  moved out of Advanced, "Show Screenbridge when a Mac connects" no longer needs a permission.
- About shows the privacy policy (https://sebstech24.github.io/mirror-tv/privacy.html), as Play requires.
- Removed for Play: USE_FULL_SCREEN_INTENT, SYSTEM_ALERT_WINDOW, TURN_SCREEN_ON. A blocked "come to front" now shows a
  heads-up notice. Wake-on-connect stays but only works up to Android 13 (14+ needs TURN_SCREEN_ON); the Home tip covers it.
- TV-only on Play now (leanback required=true), touchscreen not required, banner 320x180, target SDK 37,
  64-bit native libs 16 KB aligned, BILLING permission present.
- Debug-only switches (ignored in release, settable only over adb): debug_fake_unlocked, debug_sample_prices
  (shows $4.99 / $49.99 sample prices when Play has none, for layout checks).
- Checked in the emulator (MirrorTV_API34): fresh install opens on the paywall and the receiver logs "Locked" (nothing
  advertised); fake unlock closes the paywall, starts the receiver and advertises _airplay/_raop; re-lock keeps it off;
  Settings plan card. Not checked: a real purchase/trial (needs a Play test-track install with a license tester).

## State (24 Oct 2026)
- Builds on the Mac (`./gradlew assembleDebug`, NDK 27.3.13750724, platform android-37.0; first build ~15 min).
- Runs in the Android TV emulator (AVD `MirrorTV_API34`): new Home screen with Mac steps, Settings, About,
  advertises `_airplay._tcp` + `_raop._tcp`. Screens: docs/screens/.
- Done on `tv-edition`: TV-first home/settings/about (GPL + FFmpeg LGPL notices, Apple trademark note),
  sender name + disconnect reasons, no accidental Stop, download button removed, audio always advertised
  (fixes 64 s Mac drop), re-advertise on network change, lowest-latency mode, MediaTek video fallback,
  full-screen-intent pop-up when a Mac connects in the background.
- REAL-DEVICE TEST PASSED (25 Oct 2026, Philips Google TV TA1, Android 14, MediaTek): Mac (macOS 27) found
  "Screenbridge TV", connected as extended display at 1080p, live video (Photo Booth feedback loop seen on
  camera + TV screencap), AAC-ELD audio stream received (not listened to), "Mirroring stopped on the Mac" card
  shown after stopping. Screens: docs/screens/real-tv-*.
- BUG found: this TV has its own built-in AirPlay (com.mediatek.airplaydaemon) + AirScreen installed; both
  claim the TV's name, so Android blocked our _airplay._tcp registration (NameConflictException, no failure
  callback) and the Mac couldn't see the TV. Worked around by renaming to "Screenbridge TV" over adb.
  Fix needed: default name that can't clash (e.g. "<TV name> · Screenbridge") + detect conflict and retry.
- Market note: many MediaTek/Philips Google TVs ship with AirPlay built in.

## Open
- Name DECIDED (25 Oct): app "Screenbridge", Play title "Screenbridge: Screen Mirroring", Play ID com.sebstech.screenbridge (done in code). Sebastian still to trademark-check. App icon completed at `design/icon.png`; Android TV home banner completed at `design/banner.png` (1280×720). Remaining rebrand: Play listing.
- Sebastian: Play developer account type/age (12 testers × 14 days if personal & newer than Nov 2023);
  lawyer check before charging.

## Docs
docs/business/: PLAY-STORE.md (market + rules), IMPROVEMENTS.md (audit), SALES-PLAN.md (price/name/launch),
SERVICE-STATE.md / UX-NEEDS-FROM-SERVICE.md (UI ↔ service state).

## Night test 27 Oct 2026 (real Philips TV, Mac in front of it; all TV work ended by 04:50)
Results, in the order of the 25 Oct list:
1. Screensaver: NOT a keep-screen-on bug. While mirroring with our app in front the TV stays awake (2.5 min with
   a 1-min timeout; Philips' own burn-in watcher also stops counting). The real bug: if a screensaver is
   ALREADY up when the Mac connects, the picture plays unseen behind it. Two kinds on this TV:
   a) Google's screensaver: Android blocks our "come to front". Fix added: wake the screen on connect
      (wake lock with ACQUIRE_CAUSES_WAKEUP). Verified it wakes straight into the mirrored picture, BUT only
      when the app holds the TURN_SCREEN_ON permission. That is off by default and this TV has no settings page
      for it (granted over adb for the test, then reset). DECIDED (Sebastian, 27 Oct): no permission setup flow;
      the wake code stays in (works where the permission happens to be allowed).
   b) Philips' logo screensaver (OLED burn-in protection, drawn by the TV system over every app, after ~2.3 min
      of a still screen with no Mac connected). It goes away only on a remote button press or when a DIFFERENT
      app comes to the front; its stop signal is system-only. Tried a tiny invisible screen inside our app:
      did not work, removed. DECIDED (Sebastian, 27 Oct): a tip on the TV home screen under the mirror hint,
      "Picture hidden by the screensaver? Press any button on your remote." (checked in the emulator).
2. Paywall: verified on the TV with the 25 Oct build (the paywall is out of tv-edition, kept on paywall-later):
   "1 minute left" notice at 30 s, paywall screen at 60 s (docs/screens/night-paywall-at-60s.png), Mac
   dropped, reconnect worked; debug_fake_unlocked mirrored 2.5 min uncut.
3. Name: fixed and verified. After a clash the native server reports the new name too, so the Mac's display
   tile now says "Living Room TV (2)". The working name is remembered (pref working_name), so the next start
   registers "(2)" in ~1 s instead of a 6-s timeout first. Small leftover: macOS's "+" menu in Displays still
   listed "Living Room TV" while the tile said "(2)".
4. Lag + quality: TV panel is 4K 60 Hz; "Auto" already asks the Mac for 3840x2160. The Mac sent H.264 every
   time even with H.265 offered, so the codec is the Mac's choice. Lag measured with a ms clock on the Mac and
   the MacBook camera on the TV (camera delay included, 6 readings each): 4K default 294 ms, Lowest latency on
   319 ms, 1080p 275 ms. Differences are within the noise, so the defaults stay (Auto/4K, Lowest latency off).
   Not done: absolute lag with the iPhone 240 fps camera.
5. Mirror hint: added to the TV home screen, using the Mac's exact wording ("System Settings → Displays →
   When connected to TV → Mirror Entire Screen"). docs/screens/night-home-hint.png. Spacing tightened so the
   bottom Wi-Fi line still fits with the screensaver tip added (docs/screens/home-screensaver-tip.png).
Also: Settings now opens at the top (docs/screens/night-settings-top.png).
Found: "Mac volume drives TV volume" (sync_client_volume, default on) raised the TV to 6/60 during mirroring
tests. Worth a thought before launch (a surprise for people mirroring at night).
Privacy note: screenshots taken while mirroring show the Mac's screen; keep them out of this public repo.

## PAUSED 25 Oct 2026 (Sebastian is using the TV)
Done since last notes (commit 799fe03): auto-rename when the TV's name is taken (verified on the Philips TV:
"Living Room TV" -> "Living Room TV (2)", Mac sees it); paywall built (20-min free sessions, one-time
`screenbridge_unlimited`, Play Billing 9.1, 8 unit tests pass) — NOT yet verified on the TV.
Next, in order:
1. Screensaver: Sebastian saw the TV go to screensaver during the paywall test — check whether it happened
   while mirroring (keep-screen-on missing) or after the 1-min test session ended (normal on Home).
2. Finish the paywall test on the TV (debug_short_session=true is still set in the debug app on the TV):
   notice at 30 s, paywall at 60 s, Mac drops, can reconnect; then debug_fake_unlocked.
3. Name polish: the Mac lists the receiver by the name the native server reports ("Living Room TV"), not the
   renamed mDNS name "(2)" — set the native name to the renamed one too; remember the last name that worked
   so the next start uses it immediately (no 5-s clash window).
4. Responsiveness + picture quality (Sebastian's request): measure lag with a ms clock on the Mac + camera
   (Photo Booth) view of the TV; compare Lowest latency on/off, 1080p vs 4K, H.264 vs H.265; check TV panel
   resolution; pick the best defaults.
5. "How to mirror instead of extend" hint on the TV (Mac defaults to extended display).
Play Console setup for Sebastian (when publishing): product id `screenbridge_unlimited` ($7.99, one-time),
license testers, internal test track. Note new rule (24 Oct): his app pricing standard is Monthly $4.99 with
7-day trial — ask him whether that overrides the one-time $7.99 plan before setting up Play.
