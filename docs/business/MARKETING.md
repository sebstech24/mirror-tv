# Launch marketing (Telebeam, formerly Screenbridge)

Started 28 Oct 2026, from Sebastian's approved 7-step plan ("Start all of it"). Every batch of posts, emails or public replies needs his yes. **Nothing is public yet:** the app isn't live on Play (release waits for receiver fixes). **Renamed 29 Oct 2026 to Telebeam** (Play title "Telebeam: Screen Mirroring TV", package `com.sebstech.telebeam`), because "Screenbridge" is already used by a connected-TV ad agency (screenbridge.tv); shortlist and checks in `NAME-SHORTLIST.md`. App email (decided 29 Oct): **telebeam@controlmymac.com** on the existing controlmymac.com setup (no new domain or website). Live 29 Oct: added in iCloud, Brevo sender id 4; sending tested (arrived in Gmail's Primary inbox), iCloud accepts incoming mail. Listings and help pages use it.

## Where everything is

| Step | State | Files |
|---|---|---|
| 1. Play listing for search | **Done, 25 languages** (fastlane files). Telebeam 1.0.0 (build 104) submitted for review 29 Oct 08:37 with en-US; managed publishing OFF, so it goes live when Google approves. **The other 24 languages are in the Console as a saved draft** (29 Oct, checked against the files, not sent for review so the first review isn't delayed): after 1.0.0 is live, open Default store listing ▸ Next ▸ Save, then Publishing overview ▸ Send for review. AI labels: icon, feature graphic, TV banner are marked AI-made; screenshots are real | `fastlane/metadata/android/<locale>/` |
| 1. Demo video (Mac → TV in 3 clicks, 22 s) | **Rendered 30 Oct** (hub job #2062, CPU-only because Premiere held the MAIN PC's GPU; 22 s, 1920x1080, checked). Mac copy `design/demo-video/renders/telebeam-demo-3-clicks.mp4`. Next: YouTube upload (his yes), then the link goes in the Play listing | `design/demo-video/` (PC copy: `Y:\Vibe Coding\Telebeam demo video\`, output `renders\telebeam-demo-3-clicks.mp4`) |
| 2. Help pages on controlmymac.com | **Built, not deployed** (branch `screenbridge-pages`, commits 5a5d909, 48e2402, 5e8bd81 (renamed to /telebeam), worktree `Fable stuff/controlmymac-site-screenbridge`). 7 EN + 3 DE pages, homepage spot, sitemap, hreflang | see "Deploy" below |
| 3. In-app review prompt (1.0.1) | **Built** by the release-build chat: Telebeam 1.0.1 (versionCode 105, `dist/telebeam-1.0.1-105.aab`, commit 6f76730), not uploaded yet. Shown after a session the Mac ended normally with 10+ min of picture, never in the first session, once per app version, never on the paywall or while mirroring, no rewards or pre-prompt; only on Play-installed builds. 1.0.1 also adds "Redeem code" (Play promo codes) on the paywall and in Settings, which reviewers need | release-build chat |
| 4. Cross-sell | CMM website spot: in the help-pages branch. Newsletter: **written, not in Brevo yet** (needs his yes). Brevo list "Control My Mac Updates" has **7 subscribers**. "More apps" spot inside the CMM iPhone app: not started (CMM's own code; next CMM version) | `docs/business/marketing/newsletter-telebeam.html` |
| 5. Communities | **Drafts done:** 13 thread replies, 2 self-post drafts, 2 modmails, Show HN | `docs/business/marketing/community-drafts.md` |
| 6. Press | **Drafts done:** 44 targets (34 contacts verified), pitches in en, de, fr, es, it, nl, pl, send plan | `press-targets.csv`, `press-pitches.md` |
| 7. Tracking | **Done:** one UTM-tagged Play link per channel | `docs/business/marketing/tracking-links.md` |
| Directories | **Texts ready** for AlternativeTo, SaaSHub, Product Hunt (Sebastian submits; Claude's browser is blocked on these forms) | `directories.md` |
| Social places | **Plan ready**, waits for his yes | `social-plan.md` |

## Decisions made

- **"AirPlay" in the listing:** not in the title, short description or icon. One nominative mention in the long description ("Your Mac's Screen Mirroring … uses Apple's AirPlay technology; [app] is the receiver on the TV side"), plus the credit line naming jqssun's "AirPlay Receiver" and the Apple trademark notice. Competitors (AirScreen, AeroPlay, jqssun) put AirPlay in their titles; we don't, which is the lower-risk choice.
- **Search findings (Play web search, US + DE, 28 Oct):** "screen mirroring" is crowded with phone-casting apps; phrases with "Mac" ("mirror mac to tv", "mac screen mirroring") have weak competition, so "Mac" sits in the short description and early in the full text. German searchers mostly see English-titled apps, so de-DE keeps "Screen Mirroring" in the title. Not checked: search on a real TV's Play Store (only meaningful once we're live).
- **Wording:** never call the app free (paid only; "free" only in "7-day free trial"); "open source (GPL-3.0)". Only tested on a Philips Google TV: the listing says "made for" other brands, not "works on".
- **Mac menu names** in every language were taken from macOS 27's own built-in translations (ControlCenter.app, DisplaysExt), not guessed.
- **Press (Sebastian, 28 Oct):** pitch everyone relevant, including 9to5Mac and iDownloadBlog; don't hold back outlets with an open CMM conversation; the app gets its own sender address (domain to match the new name).

## Deploy at launch (in this order)

1. Re-render the demo video through the job hub (after the in-app rename, so the TV screen matches); upload it to YouTube (his yes) and put the link in the Play listing.
2. Website: merge `screenbridge-pages` into main in `controlmymac-site`, set `PLAY_LIVE = True` in `scripts/build-telebeam.py`, rebuild, push (a push publishes). Then ping IndexNow with the 12 URLs listed in the branch's commit/deploy note.
3. Newsletter: create the Brevo draft (account "Sebastian Škoić s.p.", list 3, sender id 4 telebeam@controlmymac.com) → test to him → he sends from Brevo.
4. Communities, press and directories, in batches, each with his yes.
5. GitHub: make `tv-edition` the default branch (or merge to main), new README intro and repo description, before Show HN.

## Numbers so far

None yet: the app isn't live. Read them weekly in Play Console ▸ Grow users ▸ Store listing acquisitions (by utm_source), plus Brevo opens/clicks.

| Week | Store visitors | Installs | Trials | Paid | Ratings | Top source |
|---|---|---|---|---|---|---|
| — | — | — | — | — | — | — |
