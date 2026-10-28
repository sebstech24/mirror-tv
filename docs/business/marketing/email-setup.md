# support@telebeamapp.com: setup (after Sebastian buys telebeamapp.com)

Same setup as controlmymac.com: iCloud+ receives, Brevo sends, DNS at Cloudflare. Nothing here costs money beyond the domain (about $10–11 a year, Cloudflare at cost).

1. **Buy** telebeamapp.com in Cloudflare (Sebastian; Domain Registration ▸ Register). Cloudflare becomes its DNS automatically.
2. **Receive with iCloud+** (Sebastian, on the Mac: System Settings ▸ Apple Account ▸ iCloud ▸ iCloud Mail ▸ Custom Email Domain ▸ Add a domain you own ▸ telebeamapp.com ▸ address `support`). Apple shows MX, SPF, DKIM (`sig1._domainkey`) and an `apple-domain` TXT record. Cloudflare can add them for you: choose "Cloudflare" when Apple offers to set up DNS automatically, or Claude adds them with the Cloudflare tools after his yes.
3. **Send with Brevo** (Brevo ▸ Senders, domains & dedicated IPs ▸ Domains ▸ Add telebeamapp.com). Brevo shows a `brevo-code` TXT, DKIM CNAMEs and a DMARC suggestion; add them in Cloudflare. Then add sender "Sebastian Škoić" `support@telebeamapp.com` and verify it with the code that arrives in iCloud Mail.
4. **One SPF record only**, combining both: `v=spf1 include:icloud.com include:spf.brevo.com ~all` (Brevo also signs with DKIM, which is what DMARC checks).
5. **DMARC**: `_dmarc` TXT `v=DMARC1; p=none; rua=mailto:rua@dmarc.brevo.com` to start; move to `p=quarantine` after a few weeks of clean reports.
6. **Check**: send a test from Brevo to a Gmail and an Outlook address; both should show "signed by telebeamapp.com", SPF/DKIM/DMARC pass.
7. **Switch it everywhere**: Play Console contact email, listing texts (`support@controlmymac.com` → `support@telebeamapp.com` in all 25 `full_description.txt`), privacy policy, website pages, newsletter, press pitches, AlternativeTo/SaaSHub.

Warm-up: a new domain has no sending reputation, so pitches go out about 10–20 a day at first, plain text, at most 2 links.
