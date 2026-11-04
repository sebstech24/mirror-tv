# support@telebeam.app: the app's own address

**4 Nov 2026 (Sebastian: "yes to all"):** Telebeam now has **support@telebeam.app** on iCloud+ Custom Email Domain (telebeam.app, like diskbeam.app). DNS added through Cloudflare Domain Connect: MX mx01/mx02.mail.icloud.com, TXT apple-domain + `v=spf1 include:icloud.com ~all`, CNAME sig1._domainkey (the GitHub Pages A/www records untouched). Tested both ways: Gmail → support@ arrived in iCloud Mail; support@ → Gmail arrived in Primary with DKIM (telebeam.app) and SPF pass. Listings, website and privacy page switched. telebeam@controlmymac.com stays as an alias, so older links still reach him. Brevo: telebeam.app authenticated + branded (mail.telebeam.app) 4 Nov, records added by hand in Cloudflare (brevo-code TXT, brevo1/brevo2._domainkey, _dmarc p=none, mail / r.mail / img.mail CNAMEs, all DNS only); sender id 5 "Sebastian Škoić" support@telebeam.app is active. Sender id 4 (telebeam@controlmymac.com) can stay for old drafts.

## Earlier: telebeam@controlmymac.com

Decided 29 Oct 2026 (Sebastian: "do as you recommend"): no new domain, no separate website for now (website part replaced 2 Nov 2026: Sebastian wants each app to have its own page; the email stays). Telebeam gets **telebeam@controlmymac.com** on the existing controlmymac.com setup. telebeamapp.com is dropped.

How controlmymac.com mail works: iCloud+ Custom Email Domain receives (MX mx01/mx02.mail.icloud.com) and the mail reaches Sebastian's Gmail; Brevo sends, and the domain is already authenticated there (SPF includes icloud.com, Brevo DKIM, DMARC p=none with reports).

Steps:
1. **Add the address in iCloud** (Sebastian; 29 Oct test mail bounced "550 user does not exist"): icloud.com/settings → Custom Email Domain → controlmymac.com → Add an Email Address → `telebeam`.
2. **Test inbound:** mail from Gmail to telebeam@controlmymac.com arrives.
3. **Brevo sender:** "Sebastian Škoić" `telebeam@controlmymac.com` (domain already authenticated, so mail is signed by controlmymac.com).
4. **Test outbound:** a Brevo test from telebeam@ to Gmail shows SPF/DKIM/DMARC pass.
5. **Switch it everywhere:** listing texts (25 `full_description.txt`), Play Console contact email, privacy policy (gh-pages), website pages, newsletter sender, press pitches, directories.
