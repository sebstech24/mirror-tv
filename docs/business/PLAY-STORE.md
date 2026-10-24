# Mac Mirror TV: Google Play research

Written 24 Oct 2026. Plain-language research, not legal advice. Every fact has a source link. "Unverified" means I could not confirm it; check before relying on it. Google Play and Apple pages could not be loaded in full by my tools (Play listings come back empty), so competitor numbers are thin and mostly second-hand.

## 1. Competitors on Android TV

| App | What it does | Price / limits | Source |
|---|---|---|---|
| AirScreen (ionitech) | AirPlay + Google Cast + Miracast + DLNA receiver. "Multi million downloads" since 2016. | Free with a subscription. A rival's blog (biased, unverified) says Basic about $1.49/mo or $11.99/yr, Pro about $2.49/mo or $19.99/yr. Free = ads, standard definition, one device, time-limited sessions. One review says free is capped at 30 min/day Google Cast mirroring and 15 AirPlay videos/day. | [apkpure](https://apkpure.com/airscreen-airplay-cast/com.ionitech.airscreen), [PigeonCast blog](https://pigeoncast.com/blogs/is-airscreen-free), [search summary of a review](https://play.google.com/store/apps/details/AirScreen_AirPlay_Cast?id=com.ionitech.airscreen&hl=en_SG) |
| AirPin (waxrain) | AirPlay + DLNA receiver for TV/box. Says it works with iPhone, iPad and Mac. | Pro subscription needed to share with up to 4 devices (secondhand). Exact price not found. | [mirrorscreen.tv](https://mirrorscreen.tv/reviews/best-airplay-receiver-apps-for-android-tv), [PigeonCast guide](https://pigeoncast.com/blogs/airplay-receiver-app), [AppFollow](https://appfollow.io/android/airpin-lite-airplay-dlna-receiver/com.waxrain.airplayer2?country=us) |
| AirReceiver | Basic AirPlay/DLNA receiver, very plain interface. Paid version adds YouTube and PC connection. 2014 price was about 2.33 EUR (old). | Current price unknown. Guide describing it is old and maintenance is unverified. | [Xataka](https://www.xatakahome.com/televisores/como-usar-airplay-televisor-android-tv-para-enviar-contenido-iphone-ipad), [Born City](https://borncity.com/blog/2014/07/22/airreceiver-pro-als-android-airplay-empfnger-teil-6/) |
| android-airplay-server (jqssun) | Free, GPL-3.0, on Google Play, F-Droid and GitHub. Mirrors from iOS, iPadOS and macOS, H.264/H.265, D-pad support, built on UxPlay. Says "not affiliated with Apple". | Free. This is the closest thing to your idea and it is free, so a paid app must be clearly more polished. | [GitHub](https://github.com/jqssun/android-airplay-server) |
| AriannaCast, PigeonCast, AirDroid Cast TV | Cross-platform casting apps. Some are free; plans vary. | Not verified. | [mirrorscreen.tv](https://mirrorscreen.tv/reviews/best-airplay-receiver-apps-for-android-tv), [PigeonCast](https://pigeoncast.com/blogs/airplay-receiver-app) |

Ratings and exact install counts: I could not read them (Play pages did not load). apkpure shows AirScreen as 9.8 from only 9 reviews, which is not the real Play rating. Please open the Play listings on the TV once to read ratings and the 1-star reviews.

Complaints I could verify (weak evidence, no scraped review data):
- Subscription model, and one reviewer says the account cannot be used on another device. Ads, some that cannot be closed. ([search summary of the Play listing](https://play.google.com/store/apps/details/AirScreen_AirPlay_Cast?id=com.ionitech.airscreen&hl=en_SG))
- Compatibility warnings: Play listing says some phones and TV models are not fully supported. ([same](https://play.google.com/store/apps/details/AirScreen_AirPlay_Cast?id=com.ionitech.airscreen&hl=en_SG))
- Lag: a forum user found AirReceiver and similar Android TV apps "horrendously" slow versus an Apple TV on the same Wi-Fi. ([Overclockers forum](https://www.overclockers.co.uk/forums/posts/33523692))
- UxPlay itself (the engine you will use) documents Mac issues: many dropped video frames on macOS unless sync is relaxed (`-vsync no`), and audio/video sync tuning trade-offs. This is where to beat competitors. ([UxPlay README](https://github.com/FDH2/UxPlay))
- Apple TV app / DRM video cannot be mirrored with UxPlay. Say so in the listing to avoid 1-star reviews. ([UxPlay README](https://github.com/FDH2/UxPlay))

What to beat: no ads, no subscription, low lag, correct audio sync, "just works with Mac Screen Mirroring", clear honesty about DRM video.

## 2. Monetization

Facts first:
- GPL lets you sell copies at any price. It does not let you forbid recipients from sharing or force them to pay you. ([GNU GPL FAQ](https://www.gnu.org/licenses/gpl-faq.html))
- So a paywall in the app is fine as long as the full source is public under GPL-3.0. Anyone can compile the source and skip the paywall. That is normal for GPL apps; your value is the signed, updated, supported Play version.
- Precedent: OsmAnd (GPL) sells a paid version on Google Play while the code stays open. ([OsmAnd discussion](https://www.osnews.com/?p=24225), secondary)
- Caution: the FSF objected to Apple's App Store because Apple's terms added restrictions on users (limit to five devices). Google Play has no such per-device limit, and GPL apps ship there already (the jqssun app above), but I found no FSF statement on Google Play. Treat as low risk, not zero. ([LWN on GNU Go](https://lwn.net/Articles/391423))
- Play Billing fees: I could not parse Google's fee page. In "other markets" it says 15% on the first $1M a year (30% above) and 15% for subscriptions. For US/UK/EEA from 30 June 2026 it lists 10% for subscriptions and 20% for other purchases on new installs, each plus a 5% billing fee. Check the live table before pricing. ([Google](https://support.google.com/googleplay/android-developer/answer/112622))

Recommendation (my opinion):
- Free tier: full quality mirroring, 15-minute sessions (reconnect to continue). No ads. This beats AirScreen's ads, and 15 minutes is enough to prove it works with a Mac.
- One-time unlock "Pro Lifetime": around $7.99 (range $5.99 to $9.99). Competitors charge roughly $12 to $20 per year (unverified), so one payment is a clear selling point.
- Skip subscriptions at launch. A receiver has almost no running costs for you (no servers), and "pay once" matches the open-source story. Add an optional $1.99 "tip/supporter" later if you want.
- Use a managed in-app product (one-time, non-consumable). Remember your standing rule: Family Sharing is not a Play concept, so no issue here.
- Put the source link and "build it yourself for free" in the listing and README. Honest and fits GPL.

## 3. Naming and trademark

What Apple says (Apple's third-party trademark guidelines):
- You may not use an Apple trademark as part of your product or company name, except as the guidelines allow.
- "Mac" may be used only combined with another non-generic word, must not stand out, and must not suggest a false Apple link. "Variations, phonetic equivalent, takeoff" are also banned.
- You may mention Apple marks to say your product is compatible, in phrases like "for", "works with", "compatible with", with the Apple mark less prominent than your name, no Apple logo, no endorsement implied.
- Use the legal credit line in the US, e.g. "Mac, macOS and AirPlay are trademarks of Apple Inc., registered in the U.S. and other countries" and a non-affiliation line.
- All three (AirPlay, Mac, macOS) are on Apple's trademark list. Source: [Apple third-party guidelines](https://www.apple.com/legal/intellectual-property/guidelinesfor3rdparties.html), [Apple trademark list](https://www.apple.com/legal/intellectual-property/trademark/appletmlist.html).
- Apple's design guidelines also say not to put Apple trademarks in app names and to keep references to AirPlay less prominent than your own name. ([Apple HIG, AirPlay editorial](https://developer-mdn.apple.com/design/human-interface-guidelines/technologies/airplay/editorial))
- The official "Works with Apple AirPlay" badge is a certification for licensed products (Denon and Marantz manuals describe it as certified). Do not use it or any Apple logo. ([Denon manual](https://manuals.denon.com/avrx3800h/NA/EN/GFNFSYbxvayfun.php))

Consequence: the working name "Mac Mirror TV" puts "Mac" in the name and "Mac" is the main word. That is against Apple's guideline in my reading. Drop it from the Play title; use "Mac" only in the description.

Google Play rules:
- Title max 30 characters, no emojis, no misleading claims. ([Google Play metadata policy](https://support.google.com/googleplay/android-developer/answer/9898842))
- Trademark infringement (identical or similar mark likely to confuse) can get an app suspended; owners complain through a trademark form. ([Google IP policy](https://support.google.com/googleplay/android-developer/answer/9888072))
- I did not get Google's impersonation policy text. General rule: do not look like Apple or imply Apple made it.

What competitors do: AirScreen's own title is "AirScreen - AirPlay & Cast" (uses the AirPlay word in the title) and AirPin's listing says it works with "iPhone, iPad, and Mac devices" ([Play title via search](https://play.google.com/store/apps/details/AirScreen_AirPlay_Cast?id=com.ionitech.airscreen&hl=en_SG), [mirrorscreen.tv](https://mirrorscreen.tv/reviews/best-airplay-receiver-apps-for-android-tv)). Being tolerated is not the same as being compliant, so I recommend a cleaner title than they use.

Five name ideas (none checked for existing apps or registered trademarks; search Play, USPTO and EUIPO before choosing):
1. Screenbridge TV
2. Mirrorly Receiver
3. BigScreen Mirror
4. Beamcast Receiver
5. Sidecar TV is NOT safe (Apple feature name). Use instead: Glassline TV

Short description (80 chars max on Play):
"Show your Mac's Screen Mirroring on your TV. No ads, no subscription."

Full description draft:
> Turn your Android TV or Google TV into a wireless screen mirroring receiver. Open Screen Mirroring on your Mac (Control Center), pick this TV, and your screen appears on the big screen. Also works with iPhone and iPad Screen Mirroring.
>
> - Works with Mac Screen Mirroring (macOS) and iPhone/iPad
> - Free: full-quality mirroring in 15-minute sessions, no ads
> - One-time Pro unlock: unlimited sessions, no subscription
> - Nothing leaves your home network. We collect no data.
> - Open source (GPL-3.0). Source code: [link]
>
> Not supported: protected video from the Apple TV app and other DRM streaming apps (they show black or are blocked by Apple).
>
> This app is independent and is not made, sponsored or approved by Apple Inc. Mac, macOS, iPhone, iPad and AirPlay are trademarks of Apple Inc., registered in the U.S. and other countries and regions.

Avoid: the Apple logo, Mac/Apple artwork, "AirPlay" in the title, "official", "for Mac" as the first words.

## 4. Google Play requirements for a TV app

TV quality guidelines ([Android TV quality](https://developer.android.com/docs/quality-guidelines/tv-app-quality)):
- Leanback launcher intent (`LEANBACK_LAUNCHER`) so it shows on the TV home.
- Banner 320x180 px with the app name on it; icon at least 160x160.
- Everything usable with a five-way D-pad; do not need a Menu button; Back goes to the TV home.
- Manifest: touchscreen marked not required; minSdk 31 or lower.
- Landscape, no cut-off text at screen edges, full-screen non-transparent background.
- At least one real, unaltered TV screenshot (use 1920x1080), app bundle (AAB) required.
- From 1 Aug 2026: 32- and 64-bit builds and 16 KB page-size compatibility (matters because UxPlay/GStreamer/native libs must be rebuilt for it).

Target API for new apps late 2026: phones must target API 36 from 31 Aug 2026, but Android TV needs only API 34 (can request extension to 1 Nov 2026). Aim for 35 or 36 anyway. ([Google target API page](https://developer.android.com/google/play/requirements/target-sdk))

Data safety form: required for every app, even if you collect nothing; you must also give a privacy policy link. Data that stays on the device is not disclosed; data sent off the device is "collection". Google's page does not say how pure local-network traffic counts. Reasonable answer: "No data collected, no data shared", encryption not applicable, and explain in the policy that mirrored content goes only over the local network from the user's own computer to their TV, and Play Billing is handled by Google. Confirm in the form's wording. ([Data safety help](https://support.google.com/googleplay/android-developer/answer/10787469))
- Make a one-page privacy policy (host on GitHub Pages or his own site). Say: no accounts, no analytics, no ads, no data collected, local network only, Google handles payments.
- Play Billing declaration and the "uses local network / multicast (mDNS)" permissions should be explained in the listing or review notes. Not looked up in detail.

New-account testing rule ([Google help](https://support.google.com/googleplay/android-developer/answer/14151465)):
- Applies to personal developer accounts created on or after 14 Nov 2023 (Android Authority and others say 13 Nov).
- Needs a closed test with at least 12 testers, each opted in for 14 days in a row, then you click "Apply for production" and wait about a week. Opting out resets that person's days. Aim for 15 to 20 testers.
- Organization accounts are said to be exempt (secondary source: [Choicely](https://www.choicely.com/blog/google-play-12-tester-rule)).
- Sebastian's existing account: it matters. If it is a personal account made after Nov 2023, he needs 12 testers for 14 days for this app too (the rule is per account's first production access, but Google asks again for new apps in some cases; I did not confirm). If it is older, or an organization account, he is exempt. Check Play Console, Dashboard. Also, per his memory notes, he publishes under his own name, not AzureFilm; keep it that way.

## 5. Legal risk (not legal advice)

- Mac Mirror TV would contain code derived from reverse-engineered Apple protocols, including playfair (FairPlay key exchange). UxPlay lineage: UxPlay from RPiPlay from AirplayServer, shairplay, playfair. ([UxPlay README](https://github.com/FDH2/UxPlay))
- What I found: no report of any Apple DMCA notice or takedown against UxPlay, RPiPlay or shairport-sync. My search could not prove the negative. Apple's lawyers have filed DMCA notices on GitHub in 2020 and 2021, but I could not read what they targeted, so I cannot link them to AirPlay. ([GitHub DMCA mirror](https://git.randommeaninglesscharacters.com/github/dmca/src/branch/master/2020/08/2020-08-24-Apple.md), blocked by the site)
- Closest history: in 2011 the AirPlay key was extracted and the open ShairPort emulator released; commentators speculated about DMCA problems but no action was found. ([AppleInsider forum](https://forums.appleinsider.com/discussion/122266)) In 2012 Apple removed Airfoil Speakers Touch from the App Store for its AirPlay receiver feature, citing App Store rules, not a lawsuit. ([MacStories](https://www.macstories.net/news/airfoil-speakers-touch-is-back-in-the-app-store-but/), [TidBITS](https://tidbits.com/2012/06/06/airfoil-speakers-touch-back-in-app-store-without-airplay-support/))
- Commercial receivers (AirScreen, AirPin, AirReceiver, AirServer) have been on sale for years. I found no takedown or lawsuit against them, but my search tool was weak, so verify.
- The jqssun app (GPL, UxPlay-based, Google Play, claims Mac support) is a live precedent on Google Play. ([GitHub](https://github.com/jqssun/android-airplay-server))
- Risks that remain: Apple could send a DMCA/copyright or anti-circumvention complaint to GitHub or Google Play; Google could remove the app after a complaint (Play can suspend for IP complaints). A trademark complaint is the easiest for Apple, so naming is the cheapest risk to avoid. Charging money makes you a more visible target than a free hobby project. You do not decrypt Apple TV app DRM video, which helps.
- Mitigations: no Apple names in title or icon, non-affiliation notice, keep source and a clear README, keep a fork of the repo on a second host, do not promise "AirPlay 2 certified", get a lawyer's opinion before you charge real money, and keep the Mac-side behavior to Apple's documented Screen Mirroring (the Mac just sees a normal AirPlay receiver).

## Recommended plan

- Name: pick one from the list after a Play/USPTO search; title under 30 characters, no "Mac" or "AirPlay" in it. Working repo name can stay Mac Mirror TV.
- Price: free with 15-minute sessions, no ads; one-time Pro unlock about $7.99; no subscription at launch.
- License and repo: GPL-3.0, public source link in the listing, README with trademark credit line and non-affiliation notice.
- Launch steps:
  1. Check the Play Console account type and date; if personal and after Nov 2023, start recruiting 15 to 20 closed testers right away (14 days).
  2. Build the AAB with leanback launcher, 320x180 banner, 160x160 icon, touchscreen not required, target API 35 or 36, 16 KB-aligned native libs.
  3. Test on a real Google TV with an Apple Silicon Mac: audio sync and frame drops (the UxPlay issues above).
  4. Write the privacy policy page; fill Data safety (no data collected); add Play Billing one-time product.
  5. Screenshots at 1920x1080 (Mac screen on a TV), feature graphic, listing text from section 3.
  6. Closed test for 14 days, apply for production, release.
  7. Before the paid launch: ask a lawyer about the FairPlay/playfair question and the name.
- Open items I could not verify: real Play ratings and install counts, fee percentages, Google's impersonation text, whether any DMCA ever hit AirPlay receivers.
