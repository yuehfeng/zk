---
paths:
  - "{zk,zul,zkbind,zhtml}/src/main/java/**/*.java"
  - "*/src/main/resources/web/js/**/*.ts"
  - "*/src/main/resources/web/js/**/*.js"
description: ZK server-side output encoding, resource routing, media, and ARIA layering
---

# Security & Accessibility

## Server-side output encoding

ZK delegates encoding to OWASP Java Encoder (`org.owasp.encoder.Encode`); the
trap is matching `forX` to the sink — the wrong one is still XSS. Client-side
sinks (`innerHTML =`, `document.write`, `location.href`) are already rejected by
the SDL + `eslint-plugin-zk` lint rules, so the manual check is the **Java**
side:

| Sink | Right API |
|---|---|
| HTML element body / text | `Encode.forHtmlContent` |
| HTML attribute value | `Encode.forHtmlAttribute` |
| Mixed body + quoted attr | `Encode.forHtml` |
| JS string literal | `Encode.forJavaScript` |
| JS inside `<script>` | `Encode.forJavaScriptBlock` |
| URI query component | `Encode.forUriComponent` |
| URL / `href` / `src` | `Executions.encodeURL(...)` then `forHtmlAttribute` |
| XML attribute / body | `Encode.forXmlAttribute` / `forXmlContent` |

- **`Encodes.encodeURL` does NOT encode anything after `?`** — encode each query
  value with `Encodes.encodeURIComponent` / `addToQueryString`, never
  `encodeURL(base + "?q=" + userInput)`.
- **`org.zkoss.xml.XMLs` is deprecated since 10.1.0** — new code uses
  `Encode.forXml*`.
- **Encoded-by-default vs raw-by-design**: `Label#setValue` is encoded (client
  assigns `/*safe*/ getEncodedText()`); `Html#setContent` is raw by design
  (caller owns XSS); `zhtml.Text` encodes unless `setEncode(false)` — flag any
  `setEncode(false)` reaching user input.

## Resource routing, upload, media

- Keep resource delivery in the existing servlet / extendlet / WPD-WCS / theme
  URI / execution owner; avoid ad-hoc routing for static resources, debug JS,
  source maps, uploads, downloads, or media.
- Check upload/download filename encoding, content type, stream repeatability,
  and buffering thresholds — framework code, caches, retries, and tests may
  consume a media stream more than once.
- Bundled third-party assets need clear lifecycle ownership (load, bind, unbind,
  event bridge, cleanup) and should expose customization through existing ZK
  channels before a component-specific hook.

## Accessibility — classify ARIA before assigning ownership

- **CE renders intrinsic baseline semantics only** — native roles in molds,
  structural `role="none"`, decorative `aria-hidden`, popup relationships, generic
  `msgzul` labels. Leave a CE no-op hook (e.g. an empty `_moldA11yAttrs()`
  returning `''`) for EE to augment.
- **EE `za11y` owns enhanced accessibility** — dynamic ARIA repair, icon tooltip
  enrichment, screen-reader workarounds, and accessibility-only strings. New a11y
  label keys go to `za11y`'s `MZa11y` / `msgza11y*.properties` and ship via its
  `Utils.outLocaleJavaScript()`, never to CE `MZul`/`msgzul`.
- **Dynamic ARIA state belongs to `za11y`, hooked onto the method that owns the
  transition** — not emitted in a mold. `aria-expanded` goes on `open`/`close`,
  `aria-invalid` on `_markError` / `clearErrorMessage` (or a component's own
  `_setInvalid`), `aria-disabled` on `setDisabled` / the widget's edit-state pass.
  Precedent: `za11y/zul/inp-a11y.ts` holds the only `aria-invalid` in either repo,
  and `za11y/zul/db-a11y.ts` flips Datebox's `aria-expanded` — CE `Datebox.ts`
  emits none.
- **Never emit a static state attribute the base does not itself flip.** A
  mold-rendered `aria-expanded="false"` that only `za11y` ever updates is a lie in
  the NO_A11Y build — worse than the attribute being absent. The few existing
  sites that do this are latent bugs, not precedent.
- **`za11y`-only is the right home when the base's silence is honest**: an absent
  attribute claims nothing, and a NO_A11Y user is left exactly where every other CE
  widget leaves them. It is the wrong home when the fix changes focus order,
  keyboard operability, or DOM structure — that is UX, and it must work without
  `za11y`.
- **App-supplied ARIA uses `ca:role` / `ca:aria-*`** client attributes (EL
  allowed), never a bespoke `setAriaLabel`-style component property — one widget
  plays different roles per page. That escape hatch does not cover widgets the
  framework creates itself (a popup's own children): the page has no handle on
  them, so the framework must name them.
- Tests asserting `za11y`-added behavior must guard for the za11y-enabled suite:
  `if (!Boolean.valueOf(getEval("!!window.za11y"))) return;`.
