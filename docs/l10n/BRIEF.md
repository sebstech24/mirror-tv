# Telebeam app translation brief

Telebeam is a paid Android TV app: it makes a Google TV / Android TV a receiver for the Mac's built-in Screen Mirroring (AirPlay). Users read it from a sofa with a TV remote, so text should be short, plain and friendly (informal "you" where the language allows it naturally: de "du", fr "vous" is fine — pick what big consumer TV/streaming apps in that language use; nl "je", sv/da/nb/fi informal, ru/uk "вы", pl informal 2nd person or impersonal, cs/sk/sl/hr informal "ty" forms are OK for consumer apps, ja/ko polite).

Source: `/Users/sebastianskoic/Coding/MirrorTV/app/src/main/res/values/strings.xml` (read it fully).

## Output
Write one file per language: `/Users/sebastianskoic/Coding/MirrorTV/app/src/main/res/<folder>/strings.xml`, with `<?xml version="1.0" encoding="utf-8"?>` and `<resources>`. Translate EVERY `<string>`, `<string-array>` and `<plurals>` from the source, same `name`s, same order, except strings marked `translatable="false"` (leave those out entirely). Keep the XML comments out (optional).

## Hard rules
- Keep every placeholder exactly: `%1$s`, `%2$s`, `%d`, `%1$d`, `%%` (a literal percent sign). Same placeholders, same count, per string. You may move them within the sentence.
- Escape apostrophes as `\'` (e.g. French `l\'écran`, Italian `dell\'app`). Escape `&` as `&amp;`. Never use a raw `'` unescaped. Do not use `"` straight double quotes inside text; use the language's typographic quotes (e.g. „…“ de/cs/sk/sl/hr/ro/pl, «…» fr/ru/uk/es/it/el/nb, “…” en/nl/tr/id/vi/hi/th/ko, 「…」 ja/zh-TW, “…” zh-CN, ”…” sv/fi, »…« da, „…” hu/pl/ro, «…» or “…” ar).
- Keep brand names untranslated: Telebeam, Telebeam Unlimited, Mac, iPhone, iPad, Google Play, Google TV, Android TV, AirPlay, Wi‑Fi (non-breaking hyphen as in source; or the local word for Wi‑Fi that Apple/Google use, e.g. de "WLAN", fr "Wi‑Fi"), FFmpeg, UxPlay, jqssun, GPL-3.0, LGPL, ALAC, AAC, AAC-ELD/AAC-LC, H.265, HEVC, mDNS, NsdManager, VSYNC, logcat, FPS, USB, PIN, 4K, 1080p, ProMotion. Keep URLs and `play.google.com/redeem` as is.
- Mac menu names MUST use macOS's own translation (taken from macOS's files) — table below. Use them exactly, inside the quotes the source uses, wherever the source says Control Center, Screen Mirroring, System Settings, Displays, "When connected to TV", "Mirror Entire Screen". The `­` soft hyphens in some of them may be kept or dropped.
- Plurals: give the quantity items the language needs (CLDR): ja, ko, zh, th, vi, id: only `other`. de, nl, sv, da, nb, fi, it, es, pt, el, hu, tr, hi(one/other), fr (one/many/other — `many` can repeat `other`). ru/uk: one/few/many/other. pl: one/few/many/other. cs/sk: one/few/many/other. sl: one/two/few/other. hr: one/few/other. ro: one/few/other. ar: zero/one/two/few/many/other. Each item must contain the same placeholders as the English item (`%d` or `%1$d` + `%2$s`).
- "Free" only ever describes the free trial days (`plan_trial_badge`, `plan_monthly_trial_detail`, `plan_free` = the trial price label). Never call the app itself free. `about_license_note` "free software" means libre/open-source software (GPL), use the standard term (de "freie Software", fr "logiciel libre", es "software libre", etc.).
- Badges `plan_trial_badge` and `plan_lifetime_badge` are short uppercase labels on a TV card (keep them short; uppercase where the script has case).
- TV screens have limited width: prefer the shorter natural phrasing, especially for buttons (btn_*, chip_*, paywall_not_now, plan_*), titles and status lines. Don't abbreviate awkwardly though.
- Technical developer settings (MediaFormat, buffer, codec, jitter…) can stay fairly literal; keep technical terms (codec, buffer, bitrate, jitter, latency) in the form that language's Android/tech UIs use.
- `debug_*` overlay strings: translate the labels (FPS/Bitrate can stay), keep `trim=%1$d drop=%2$d sil=%3$d ur=%4$d xrun=%5$d` keys unchanged.

## Check before finishing (per file)
Run: `python3 docs/l10n/check_strings.py <folder>` — it parses the XML and compares names and placeholders against English. Fix everything it reports until it prints OK.

## macOS menu names (from macOS 27's built-in localizations)
See `docs/l10n/mac_terms.json` (keys: de, fr, es, es_419, it, pt_BR, nl, pl, ru, uk, tr, ja, ko, zh_CN, zh_TW, sv, nb, da, fi, cs, sk, sl, hr, hu, ro, el, ar, hi, id, th, vi).
Greek: macOS's menu label "Κατοπτρ. οθόνης" is abbreviated; in running text write "Κατοπτρισμός οθόνης".
