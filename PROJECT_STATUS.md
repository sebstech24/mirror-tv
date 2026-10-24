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
- NOT verified: real Mac → Android mirroring (emulator NAT can't take the receiver's random ports) —
  needs a real Android TV/phone on the same Wi-Fi.

## Open
- Name DECIDED (25 Oct): app "Screenbridge", Play title "Screenbridge: Screen Mirroring", Play ID com.sebstech.screenbridge (done in code). Sebastian still to trademark-check. App icon completed at `design/icon.png`; Android TV home banner completed at `design/banner.png` (1280×720). Remaining rebrand: Play listing.
- Paywall (Play Billing one-time unlock, 20-min free sessions) — see docs/business/SALES-PLAN.md.
- USE_FULL_SCREEN_INTENT may need a Play declaration (restricted to calling/alarm apps on Android 14+).
- Settings opens scrolled to the middle (focus lands on "Ask for a PIN") — small polish.
- Sebastian: Play developer account type/age (12 testers × 14 days if personal & newer than Nov 2023);
  lawyer check before charging.

## Docs
docs/business/: PLAY-STORE.md (market + rules), IMPROVEMENTS.md (audit), SALES-PLAN.md (price/name/launch),
SERVICE-STATE.md / UX-NEEDS-FROM-SERVICE.md (UI ↔ service state).
