/* Site kit — language switcher + first-visit redirect (client-side, no deps).
   From controlmymac.com assets/i18n.js (c15ee24). English lives at the root (/, /support, ...),
   every other language under a prefix (/de/, /de/support, ...). The visitor's choice is
   remembered in localStorage + cookie; first-time visitors are sent to the version matching
   their browser language. Choosing a language (incl. English) sticks and stops auto-redirects.
   Kit change: the language list is not hard-coded. It is read from this page's footer
   (<nav class="flangs">, written by build.py), so a page only ever offers, and redirects to,
   the languages it really exists in. CMM's one-off "new languages" migration is left out. */
(function () {
  "use strict";

  var links = document.querySelectorAll("nav.flangs a[data-code]");
  if (!links.length) return;
  var LOCALES = Array.prototype.map.call(links, function (a) {
    return { code: a.getAttribute("data-code"), name: a.textContent, hreflang: a.getAttribute("hreflang"),
             href: a.getAttribute("href"), short: a.getAttribute("data-short") };
  });
  var CODES = LOCALES.map(function (l) { return l.code; });
  var byCode = {};
  LOCALES.forEach(function (l) { byCode[l.code] = l; });
  var KEY = "kit_lang";

  function getStored() {
    try { var v = localStorage.getItem(KEY); if (v) return v; } catch (e) {}
    var m = document.cookie.match(/(?:^|;\s*)kit_lang=([a-z]{2,3}(?:-[a-z0-9]{2,4})?)/);
    return m ? m[1] : null;
  }
  function setStored(v) {
    try { localStorage.setItem(KEY, v); } catch (e) {}
    document.cookie = KEY + "=" + v + "; path=/; max-age=31536000; samesite=lax";
  }

  var cur = document.querySelector("nav.flangs a[aria-current]");
  var current = cur ? cur.getAttribute("data-code") : "en";

  function match(tag) {
    tag = (tag || "").toLowerCase();
    if (CODES.indexOf(tag) > -1) return tag; // exact folder names, e.g. "pt-pt", "es-419"
    var primary = tag.split("-")[0];
    // Chinese: Traditional for the Hant script and for Taiwan, Hong Kong and Macau.
    if (primary === "zh") {
      var want = /-hans\b/.test(tag) ? "zh" : /-(hant|tw|hk|mo)\b/.test(tag) ? "zh-hant" : "zh";
      return CODES.indexOf(want) > -1 ? want : null;
    }
    // Norwegian: Bokmål, Nynorsk and the plain "no" tag all go to /no.
    if (primary === "nb" || primary === "nn") primary = "no";
    return CODES.indexOf(primary) > -1 ? primary : null;
  }
  function detect() {
    var langs = navigator.languages || [navigator.language || "en"];
    for (var i = 0; i < langs.length; i++) { var m = match(langs[i]); if (m) return m; }
    return "en";
  }
  function go(code) {
    location.replace(byCode[code].href + location.search + location.hash);
  }

  // First-visit / remembered redirect: only ever triggered from an English (unprefixed) URL,
  // so a visitor deliberately on /de/… is never bounced away.
  var stored = getStored();
  if (current === "en") {
    if (stored && stored !== "en" && byCode[stored]) { go(stored); return; }
    if (!stored) {
      var want = detect();
      if (want !== "en") { setStored(want); go(want); return; }
    }
  } else {
    setStored(current); // landing directly on a localized page records that preference
  }

  // Build the switcher UI (progressive enhancement; hidden until JS populates it).
  function build() {
    var host = document.querySelector("[data-i18n-switch]");
    if (!host || LOCALES.length < 2) return;
    var here = byCode[current] || LOCALES[0];
    host.hidden = false;
    host.innerHTML = "";

    var btn = document.createElement("button");
    btn.className = "lang-btn";
    btn.type = "button";
    btn.setAttribute("aria-haspopup", "listbox");
    btn.setAttribute("aria-expanded", "false");
    btn.setAttribute("aria-label", host.getAttribute("data-label") || "Choose language");
    btn.innerHTML = '<span aria-hidden="true">🌐</span> <span class="lang-cur"></span>';
    btn.querySelector(".lang-cur").textContent = here.short || here.code.toUpperCase();

    var menu = document.createElement("ul");
    menu.className = "lang-menu";
    menu.setAttribute("role", "listbox");
    menu.hidden = true;

    LOCALES.forEach(function (l) {
      var li = document.createElement("li");
      var a = document.createElement("a");
      a.lang = l.hreflang;
      a.textContent = l.name;
      a.href = l.href;
      if (l.code === current) { a.setAttribute("aria-current", "true"); li.className = "is-active"; }
      a.addEventListener("click", function () { setStored(l.code); });
      li.appendChild(a);
      menu.appendChild(li);
    });

    function close() { menu.hidden = true; btn.setAttribute("aria-expanded", "false"); }
    btn.addEventListener("click", function (e) {
      e.stopPropagation();
      var open = menu.hidden;
      menu.hidden = !open;
      btn.setAttribute("aria-expanded", String(open));
    });
    document.addEventListener("click", function (e) { if (!host.contains(e.target)) close(); });
    document.addEventListener("keydown", function (e) { if (e.key === "Escape") close(); });

    host.appendChild(btn);
    host.appendChild(menu);
  }

  if (document.readyState !== "loading") build();
  else document.addEventListener("DOMContentLoaded", build);
})();
