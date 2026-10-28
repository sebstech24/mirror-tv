# telebeam@controlmymac.com: the app's own address

Decided 29 Oct 2026 (Sebastian: "do as you recommend"): no new domain, no separate website for now. Telebeam gets **telebeam@controlmymac.com** on the existing controlmymac.com setup. telebeamapp.com is dropped.

How controlmymac.com mail works: iCloud+ Custom Email Domain receives (MX mx01/mx02.mail.icloud.com) and the mail reaches Sebastian's Gmail; Brevo sends, and the domain is already authenticated there (SPF includes icloud.com, Brevo DKIM, DMARC p=none with reports).

Steps:
1. **Add the address in iCloud** (Sebastian; 29 Oct test mail bounced "550 user does not exist"): icloud.com/settings → Custom Email Domain → controlmymac.com → Add an Email Address → `telebeam`.
2. **Test inbound:** mail from Gmail to telebeam@controlmymac.com arrives.
3. **Brevo sender:** "Sebastian Škoić" `telebeam@controlmymac.com` (domain already authenticated, so mail is signed by controlmymac.com).
4. **Test outbound:** a Brevo test from telebeam@ to Gmail shows SPF/DKIM/DMARC pass.
5. **Switch it everywhere:** listing texts (25 `full_description.txt`), Play Console contact email, privacy policy (gh-pages), website pages, newsletter sender, press pitches, directories.
