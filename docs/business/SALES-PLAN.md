# Sales plan: Android TV / Google TV receiver for Mac Screen Mirroring

Written 24 Oct 2026. Builds on PLAY-STORE.md (read that for competitors, Apple trademark rules, TV quality rules and legal risk; not repeated here). Not legal advice. "Estimate" means my guess, not a fact.

## 1. Positioning

Who buys:
- Mac owners with an Android TV / Google TV (Sony, TCL, Hisense, Philips, Nvidia Shield, Chromecast with Google TV) and no Apple TV.
- Typical uses: presenting slides in a meeting room or classroom, watching local video files, a second big monitor for work, family photos on the TV.
- They already tried Screen Mirroring in the Mac menu bar, saw no TV listed, and search Play for a fix.

One-sentence pitch: "Pick your TV in your Mac's Screen Mirroring menu and your screen appears on it. No ads, no subscription."

Why pay when a free GPL version exists (be honest in the listing and README):
- The free source is real and stays free. People who can build it themselves will. Our buyers do not want to.
- What the money buys: a signed, ready-to-install Play build; a TV-first interface (big D-pad friendly screens, a "how to connect from your Mac" guide on the TV itself); tuning for Mac mirroring (frame drops and audio sync, see PLAY-STORE.md section 1); updates when macOS changes; email support; a developer who sells Mac software and answers.
- The free tier must still do the core job: full-quality mirroring, no ads, no watermark, no tracking. Only session length is limited. If free is crippled it will earn 1-star reviews and the GPL fork looks better.

## 2. Pricing and paywall

Google's fee (verified today on [Google's service fees page](https://support.google.com/googleplay/android-developer/answer/112622)):
- US, UK, EEA: since 30 June 2026, one-time purchases cost 20% plus a 5% billing fee on new installs (first $1M a year), so 25% total. Subscriptions 10% plus 5%.
- Australia and Japan: same structure from 30 Sept 2026.
- All other markets: 15% on the first $1M a year, 30% above; subscriptions 15%.
- So the "15%" figure only holds outside US/UK/EEA/AU/JP. His biggest markets (US, Germany, UK) pay 25%. Plan with 25%.

Design:
- Free: full 1080p/4K-quality mirroring, no ads, no watermark, sessions of 20 minutes, then a calm screen "Session ended. Reconnect from your Mac to continue, or unlock unlimited." Reconnecting works, so nobody is locked out. (Time limit beats watermark, which ruins presentations and feels hostile; beats resolution cap, which makes the first impression bad. AirScreen also uses time limits per [PLAY-STORE.md](PLAY-STORE.md), so users understand it.)
- Unlock: one managed, non-consumable in-app product "Unlimited" via Play Billing. Restore is automatic by Google account. Show the price on the TV with a QR/code is NOT needed; Play Billing works on the TV with the remote and Google account.
- Price: $7.99 US. Regional tips: use Play's price templates, round to local price points; roughly $4.99 for India/Brazil/Turkey/SE Asia/Eastern Europe, $6.99 for most of EU/UK, $7.99 to $8.99 for US/CA/AU/CH/Nordics. (Estimate; check Play's suggested equivalents when entering the price.)
- Launch: first 2 weeks $4.99 ("launch price") through Play's promotional pricing for in-app products, then $7.99. Good to give early testers and Reddit threads a reason to act now.
- Subscription: no at launch. No servers, no running cost, and competitors' subscriptions are what people complain about. Revisit only if you add a paid cloud feature. A $1.99 "supporter" optional product can come later.
- Take-home per sale (before your own income tax and VAT handling; Play collects and remits VAT in most countries):

| Sale | Price | Google keeps | You get |
|---|---|---|---|
| US, new install, $7.99 | $7.99 | 25% = $2.00 | about $5.99 |
| Other markets, $7.99 | $7.99 | 15% = $1.20 | about $6.79 |
| US, launch price $4.99 | $4.99 | 25% = $1.25 | about $3.74 |

GPL honesty: the paywall code is public. Anyone may remove it and rebuild; that is allowed and accepted ([GNU GPL FAQ](https://www.gnu.org/licenses/gpl-faq.html)). Do not add DRM, obfuscation, or a license check designed to stop people building from source, and do not add terms forbidding sharing. A normal "has the user bought the product" check is fine. Say plainly in README: "The Play version supports development; you can build it yourself."

## 3. Name and brand

Rule from PLAY-STORE.md: no "AirPlay", no leading "Mac", title max 30 characters.

Checks done: my web search tool is weak. For Screenbridge, Glassline, Beamcast, Mirrorly and BigScreen Mirror it found no Google Play app and no trademark record under those names, but that is "found nothing", not "cleared". Search results only showed generic screen-mirroring apps. Real clearance (Play search on a TV, then free searches in [USPTO](https://tmsearch.uspto.gov) and [EUIPO eSearch](https://euipo.europa.eu/eSearch/)) takes Sebastian about 20 minutes and must be done before the logo is made.

| Name | Pro | Con |
|---|---|---|
| Screenbridge TV | Describes the job (screen to TV), easy to say, no Apple word | Plain words, so maybe taken by something in software class 9; likely weak as a trademark |
| Glassline TV | Distinctive, nothing found | Says nothing about mirroring; needs the description to explain |
| BigScreen Mirror | Very clear | Generic; hard to protect; "BigScreen" is a known VR brand (unverified), risk of confusion |
| Beamcast Receiver | Short, memorable | "cast" is crowded (Chromecast, AirBeamTV); "Receiver" is dull |
| Mirrorly Receiver | Friendly | Sounds like many mirror apps |

Recommendation: **Screenbridge TV** (14 characters). Fallback: **Glassline TV**. Decide after the USPTO/EUIPO/Play search; if both fail, test "Beamcast" and "Castlight TV" the same way.

Package id: `com.sebstech.screenbridgetv` (changed from upstream `io.github.jqssun...` so it is never confused with it; cannot change after release, so pick once).

Listing title (Play, max 30) — **DECIDED by Sebastian 25 Oct 2026: `Screenbridge: Screen Mirroring` (30 chars); app name on the TV: `Screenbridge`; Play ID `com.sebstech.screenbridge`.** Trademark check still to do by Sebastian.

Short description (80 chars max): `Mirror your Mac screen to your TV from Screen Mirroring. No ads, no subscription.` Count it in the Console; if over 80 use PLAY-STORE.md's version: `Show your Mac's Screen Mirroring on your TV. No ads, no subscription.` (68).

Full description draft:
> Screenbridge TV turns your Android TV or Google TV into a wireless screen mirroring receiver. On your Mac, open Control Center, choose Screen Mirroring, pick your TV, and your screen appears on the big screen. It also works with iPhone and iPad Screen Mirroring.
>
> Great for presentations, watching video files, photos, and using your TV as a big second screen.
>
> - Free: full-quality mirroring in 20-minute sessions, no ads
> - One-time unlock: unlimited sessions, no subscription, no account
> - Private: everything stays on your home network; we collect no data
> - Open source (GPL-3.0): source code for this exact version: [link]. Based on the free android-airplay-server by jqssun, UxPlay and FFmpeg.
>
> Good to know: protected video from the Apple TV app and other DRM streaming apps cannot be mirrored (Apple blocks it). Your Mac and TV must be on the same Wi-Fi/network.
>
> Screenbridge TV is independent and not made, sponsored or approved by Apple Inc. Mac, macOS, iPhone, iPad and AirPlay are trademarks of Apple Inc., registered in the U.S. and other countries.

Graphics sizes Play requires ([Google asset help](https://support.google.com/googleplay/android-developer/answer/9866151)): icon 512x512 PNG; feature graphic 1024x500 JPEG/PNG, no alpha; **TV banner 1280x720** (the in-app launcher banner from PLAY-STORE.md is the separate 320x180 file); screenshots 16:9, at least 1920x1080, 1 required for TV, up to 8. The 1280x720 you asked about is the Play banner, not a screenshot.

8 TV screenshot ideas (1920x1080, real captures, add a caption bar in your own font; no Apple logos, no Apple wallpaper; generic desktop):
1. Mac slide deck full-screen on the TV, caption "Present on the big screen"
2. Home screen of the app: TV name and "Ready: open Screen Mirroring on your Mac"
3. Mac menu: the Control Center Screen Mirroring list showing the TV (crop only the menu, with a neutral wallpaper)
4. Video file playing from the Mac, "Watch your own videos"
5. Photo library grid, "Family photos, bigger"
6. Work scene: two document windows, "A bigger second screen"
7. Settings screen: device name, 1080p/4K choice, "Simple settings, all with the remote"
8. Unlock screen: "Free 20-minute sessions. Unlock unlimited once." plus "No ads. No subscription."

## 4. GPL-3.0 compliance checklist (selling a fork)

Sources: [GPL-3.0 text](https://www.gnu.org/licenses/gpl-3.0.html), [GPL FAQ](https://www.gnu.org/licenses/gpl-faq.html).
- [ ] Keep the LICENSE file (GPL-3.0) and every upstream copyright header; add your own line for your changes.
- [ ] In the repo: tag the exact commit of each release (e.g. `v1.0.0`) and link that tag from the Play listing and the in-app About screen ("source for this version").
- [ ] About screen: credit jqssun/android-airplay-server (GPL-3.0), UxPlay and its ancestors (RPiPlay, shairplay), FFmpeg (check which license variant is bundled, LGPL vs GPL, and ship its notice and source offer), plus the Apple trademark line.
- [ ] Include the licenses of every bundled library (GStreamer, OpenSSL/libplist etc. if present); the build's own license list is the check.
- [ ] Change application id, app name, icon, launcher banner and all strings that name upstream. Do not use upstream's icon or screenshots. Say "based on" with a link, not "by".
- [ ] No extra restrictions on users: the Play terms do not forbid sharing the APK from source; do not add a "no redistribution" EULA.
- [ ] Courtesy: email jqssun before launch, say you are selling a branded build, link the fork, and offer fixes upstream (send Mac sync/lag fixes as pull requests where they are not paywall-related).
- [ ] Keep a public fork on a second host (Codeberg) as backup.

## 5. Launch steps on Google Play (in order)

Sebastian himself must do the items marked [S]; everything else Claude can prepare.
1. [S] Open Play Console, Dashboard: check the account type and creation date. Personal and created on or after 14 Nov 2023 means a closed test with at least 12 testers for 14 straight days before production ([Google help](https://support.google.com/googleplay/android-developer/answer/14151465)). Older or organization account: skip. Per his notes, publish under his own name, not AzureFilm.
2. [S] Check or set up the merchant (payments) profile for selling and tax info; needs his identity and bank details, which he enters himself.
3. [S] Name clearance searches (section 3); decide the name. Claude can do the rename in the code after.
4. Claude: code work: new package id, banner/icon, leanback launcher, API 35+, 16 KB-aligned native libs, About screen, paywall with Play Billing, build a signed AAB. [S] Create and keep the upload key safe (Play App Signing).
5. Claude: write privacy policy page (no accounts, no analytics, local network only, Google handles payments). [S] Approve text and host on his site or GitHub Pages.
6. [S] Create the app in Play Console; fill listing (paste from section 3), upload graphics (Claude produces them), set category Tools, contact email.
7. [S] Complete App content: privacy policy URL, Data safety (nothing collected, per PLAY-STORE.md), content rating questionnaire (no violence, no user-generated content: likely "Everyone"), ads: none, target audience: 18+ or general, news: no.
8. [S] Create the in-app product "unlimited" with price per region; set the launch promotion.
9. [S] Android TV: tick the TV form factor, upload the TV banner and a TV screenshot. Google's TV quality review checks D-pad use, banner, no touch required; fix anything it flags.
10. [S] Upload the AAB to a closed test track, invite 15 to 20 testers (friends, Reddit/Discord helpers, his Control My Mac users; they need Google accounts and a TV or Android TV emulator is not enough for the rule, they just opt in). Run 14 days.
11. Real-hardware test while waiting: a real Google TV plus an Apple Silicon Mac (audio sync, dropped frames).
12. [S] Apply for production, wait about a week for review, release. Staged rollout 20% first.
13. Before charging real money: [S] short lawyer opinion on FairPlay/playfair and the name (PLAY-STORE.md section 5).

Can be automated: building and signing the AAB, listing text and graphics, screenshot capture, privacy policy text, updates via Play's API later. Cannot: account verification, tax/bank, tester recruiting, the declarations (he must agree to them himself).

## 6. Marketing (free channels only)

- Reddit, honest and useful, not spammy (read each rule first): r/AndroidTV, r/GoogleTV, r/macapps (self-promo day), r/mac, r/chromecast. Post "I made this because Mac Screen Mirroring does not see Android TVs", with a short demo clip and the free-source link. Answer every comment.
- YouTube Short and a 2-minute demo: Mac menu to TV in 10 seconds, titled around search phrases ("mirror Mac to Android TV without Apple TV").
- Product Hunt launch once the app is live and has some reviews. Also AlternativeTo (list as alternative to AirScreen, AirServer).
- A simple page on his own site (also needed for the privacy policy) with a "How to mirror a Mac to a Google TV" guide for search traffic.
- Cross-promo: link from the Control My Mac website and in-app "Other apps by me" panel, and a mention in its release notes/mailing list. Do not pass any user data between them.
- Later bundle idea: a Control My Mac customer perk (e.g. a free promo code for Screenbridge TV Unlimited, via Play promo codes, which are limited per quarter) or a "Mac to TV" combo page. Not before both products are stable.
- Ask happy users for a Play review in-app after a successful session of over 10 minutes (Play's in-app review API), never earlier.

## 7. Rough numbers (estimates only)

Assumptions: price $7.99, about $6 net per sale (US mix at 25% fee, rest at 15%), one-off unlock, no subscription, no ads. Conversion 1 to 3% of installs is typical for freemium with a limit (estimate; not from a source). Installs unknown: AirScreen has "multi million" ([PLAY-STORE.md](PLAY-STORE.md)); a new niche paid-unlock app from a solo developer with free marketing might reach the low range for year one.

| Scenario | Installs (year 1) | Conversion | Sales | Net revenue |
|---|---|---|---|---|
| Pessimistic | 1,500 | 1% | 15 | about $90 |
| Realistic | 10,000 | 2% | 200 | about $1,200 |
| Good | 40,000 | 3% | 1,200 | about $7,200 |

Costs: Play developer fee ($25 once, if not already paid), a domain/hosting for the policy page (can be free via GitHub Pages), his time. Income tax and VAT on his side not included. Treat this as a side income with a low floor; the main risks are a takedown (trademark, PLAY-STORE.md section 5) and the free GPL app doing the same job.
