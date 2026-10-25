# 4K60 / 4K120 / 4K60 HDR — research (25 Oct 2026)

## A. Through the Mac's built-in Screen Mirroring (AirPlay, what Screenbridge uses now)
| Target | Verdict | Why |
|---|---|---|
| 4K60 | Unclear, likely no on Wi-Fi | Receiver can ask for 3840x2160 + H.265 (UxPlay >=1.70, app has the setting), but reports say Macs send ~1080p; Apple senders only switched to H.265/4K over wired Ethernet (UxPlay #233, iPad). Needs a real test: Mac on Ethernet, app at 3840x2160 + H.265, read the live resolution/bitrate overlay. |
| 4K120 | No | Receiver can advertise 120, no evidence any Mac sends >60 fps over AirPlay mirroring. |
| 4K60 HDR | No | Screen mirroring is SDR only; UxPlay has no HDR path (8-bit BT.709); app forces SDR. |
Other: Mac picks bitrate itself (~25 Mbps max per Apple/AirServer), latency ~150 ms (more on busy Wi-Fi), audio AAC-ELD 44.1 kHz (no lossless while mirroring).

## B. Mac does the work (own Mac sender app → TV app as viewer)
| Target | Verdict | Main blocker | Effort |
|---|---|---|---|
| 4K60 SDR, low lag, lossless sound | Possible | TV decoder speed / low-latency support varies per chip — test per TV | Medium |
| 4K60 HDR (10-bit HEVC) | Possible with caveats | HDR screen capture on macOS 26/27 reported unreliable; MacBook Air display isn't HDR; colour bug reports with Apple's 10-bit HEVC encoder | Medium–large |
| 4K120 | Not realistic on a MacBook Air | Air display is fixed 60 Hz; 120 fps needs a private virtual-display API (not App Store safe); encoder speed unproven; TVs rarely let apps output 120 Hz | Large, uncertain |
Expected: ~15–40 ms end-to-end (vs ~150 ms AirPlay), 60–80 Mbps HEVC, uncompressed 48 kHz stereo audio (~1.5 Mbps).
Network: macOS can't run a Wi-Fi hotspot while on Wi-Fi and has no Wi-Fi Direct (Android can't join Apple's P2P), so no "virtual direct network" over the air. Best: Mac (and ideally TV) on Ethernet, or strong 5 GHz Wi-Fi; a USB-C Ethernet adapter + cable is the practical direct link. Many TV Ethernet ports are 100 Mbps (still enough for 4K60).
Comparable products: Moonlight+Sunshine (Sunshine on Mac is weak: no system audio, no confirmed HDR/4K120), Parsec (Mac host up to 4K60).
Licensing: a separate Mac sender app talking over the network can stay closed-source (GPL covers the TV app only).

Sources: see the two research reports summarised in the chat of 25 Oct 2026 (UxPlay README.md:1108/1471/1366/2116, issue FDH2/UxPlay#233, Apple Community 255477555, WWDC22/24 ScreenCaptureKit, moonlight-qt streamingpreferences.cpp, AOSP low-latency + HDR docs).

## C. Measured on Sebastian's MacBook Air M4 (25 Oct 2026, quiet Mac)
4K60 HEVC 8-bit and 10-bit: 60 fps, ~10 ms/frame, nothing dropped. Real screen capture → 4K60 encode: ~58 fps,
~19 ms/frame. 4K120: encoder ceiling ~95 fps → not possible on this Mac. Apple's low-latency rate-control mode
drops frames → use normal real-time mode. Details: ~/Coding/ScreenbridgeMac/PROJECT_STATUS.md (private).
