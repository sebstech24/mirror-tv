# Mirror TV — project status

Paid, Mac-focused Android TV / Google TV screen-mirroring receiver (Mac's built-in Screen Mirroring → TV).
Fork of jqssun/android-airplay-server (GPL-3.0, based on UxPlay). Public repo: github.com/sebstech24/mirror-tv,
work branch `tv-edition` (main = untouched upstream). Owner: Sebastian.

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
- Paywall (Play Billing one-time unlock, 20-min free sessions) — see docs/business/SALES-PLAN.md.
- USE_FULL_SCREEN_INTENT may need a Play declaration (restricted to calling/alarm apps on Android 14+).
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
