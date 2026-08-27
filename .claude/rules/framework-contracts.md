---
paths:
  - "{zk,zul,zkbind,zhtml}/src/main/java/**/*.java"
  - "*/src/main/resources/web/js/**/*.ts"
  - "*/src/main/resources/web/js/**/*.js"
  - "zul/src/main/resources/metainfo/xml/*.xsd"
  - "*/src/main/resources/web/WEB-INF/tld/**/*.tld"
  - "*/src/main/resources/web/**/*.dsp"
description: ZK server/client pairing, render, event, XSD/TLD, API, i18n, and CE/EE contracts
---

# Framework Contracts

The invariants a linter cannot check and that most often cause regressions.
Auto-loads when you touch a component's Java, widget/mold, or XSD.

## Component ↔ Widget Pairing

Every UI element is a pair — a Java component (server state, validation,
`smartUpdate`, `service`) and a TypeScript widget (DOM, interaction, setters,
`fire`). A half-pair is a bug no compile step catches:

- A Java `smartUpdate("x", …)` or new public `setX(...)` needs a matching TS
  `setX()` that actually re-runs the consumer reading it (not just stores a
  field); `rerender()` when the mold owns that structure.
- A TS `fire("onX", …)` or new `addClientEvent(…, "onX", …)` needs a matching
  Java `service()` branch.
- A new component also needs, in sync: the `lang.xml` (CE) / `lang-addon.xml`
  (EE) `<component>` block, the widget `.ts` + its `index.ts` export + folder
  `zk.wpd`, and the mold `.js` at the `mold-uri` — or the ZUML parser / client
  never sees it.

## Render vs smartUpdate vs response — and value coherence

- `renderProperties(ContentRenderer)` renders initial state; render only
  non-defaults (`render(renderer, name, boolean)` already skips `false`).
- `smartUpdate` pushes persistent component state.
- `response(AuResponse)` carries one-shot side effects (focus, select text,
  scroll, error display) — not state.
- **Initial render and later `smartUpdate` must send the same effective value.**
  If full render sends a derived/coerced value (an encoded href, a
  `DeferredValue`), the update path must send that same effective value or the
  client must derive it identically. A coerced-render / raw-update mismatch makes
  the runtime view differ from a reload — the single most recurring bug class.
- Don't push state the other side already holds or can derive unambiguously.
- **Normalize/validate at the shared layer, not in a late `renderProperties`
  throw.** Runtime `smartUpdate`/data-bound updates do not re-run full render, so
  cross-field validation or fallback deferred to a `renderProperties` hard-throw
  is bypassed at runtime — put it where both the initial-render and update paths
  pass through.

## Event Contracts

An event name that has a typed event class must be posted as that class, never a
raw `Event` with a hand-built data map (a raw `Event` breaks the conventional
listener signatures and EL `event.value`).

| Event | Typed class | Accessors / factory |
|---|---|---|
| `onSelect` | `SelectEvent` | `getReference()`, `getSelectedItems()`; `SelectEvent.getSelectEvent(request, handler)` |
| `onChanging` / `onChange` | `InputEvent` | `getValue()` (new String), `getPreviousValue()` (typed old) |
| `onError` | `ErrorEvent` (extends `InputEvent`) | `getValue()`, `getMessage()` (`null` = cleared); `ErrorEvent.getErrorEvent(request, oldValue)` |

- Names with no typed class (`onClose`, `onOK`, `onCancel`) stay a plain `Event`.
- Validate then rebuild the payload from the factory — never pass
  `request.getData()` straight through (attacker-controlled extra keys). Use the
  `Events.*` constant, not a re-declared `"onChanging"` string.
- `onError` carries one bad value even on a multi-value component; pass `null`
  when there is no single prior scalar.
- Client `fire(...)` opts: `{toServer: true}` forces the AU request;
  `{rtags: {...}}` dedupes a pending event; `{ignorable: true}` lets the queue
  drop a stale high-frequency event.
- A commit event (`onChange`) must serialize the value the user actually sees; a
  per-field error must not leave a stale value that a sibling edit then commits;
  a debounced auto-apply timer must tolerate duplicate same-target events.

## Client / Server Validation Split

The server `Constraint` is authoritative and must throw regardless of what the
client did. The client's job is a different one: **never offer a choice the server
will reject.** Express the restriction as control state — a disabled cell or
control plus an inert click (`SimpleConstraint` → `Calendar._fixConstraint` →
`Renderer.disabled` → `z-calendar-disabled`, and the click path short-circuits on
that class) — not as a rejected commit. A choice the user can reach that comes back
as an errorbox and a snap-back is a bug, not validation. Hover and preview
affordances honour the same predicate as the click, or they advertise something
unpickable.

When a component narrows a constraint it received (e.g. to a window around a
partial selection), intersect with the original bounds and emit **one** token:
`_fixConstraint` lets a later `between` overwrite the earlier `_beg`/`_end`, and
`setConstraint` rerenders, which strips any classes the owner painted — reapply
them on the next tick.

## XSD & Language Metadata

- A public attribute setter added/removed/renamed on a `zul` component must be
  mirrored in `zul/src/main/resources/metainfo/xml/zul.xsd` (right
  `xs:complexType`, matching `type`), or user ZUL loses validation + IDE
  autocomplete.
- Any edit to `zul.xsd` / `zk.xsd` must bump the trailing build timestamp on the
  root element — `version="MAJOR.MINOR.PATCH.YYYYMMDDHHmm"` — to now, keeping the
  `MAJOR.MINOR.PATCH` prefix aligned with `gradle.properties`. A stale timestamp
  is a frequently-missed defect.
- A **new component element** takes three edits, not one: the `xs:element`
  declaration, its `xs:complexType`, and an `<xs:element ref="…"/>` in **both**
  `anyGroup` and `anyGroupSingle` (alphabetical position). Declared but
  unreferenced is an orphan type — no container accepts it and it validates only
  as a document root, so the ticket's own fixture pages go red in the IDE.
  Verify with `grep -c 'xs:element ref="name"' zul.xsd` → must be `2`, like every
  shipped component.
- `anyGroupSingle` is not a subset of `anyGroup` but the **single-child** model
  (`xs:sequence`, pinned `maxOccurs="1"`) used by `<center>`, the
  `<north>/<south>/<east>/<west>` regions (`layoutRegionType`), and
  `<panelchildren>`. Adding to `anyGroup` alone silently rejects
  `<center><name/></center>`.
- Group membership says what may **contain** the element; what the element may
  itself contain is its own `complexType` content model. A leaf component
  (`isChildable() == false`) still carries `<xs:group ref="baseGroup">` for ZK
  meta-elements and still belongs in both groups — the two are unrelated.

## DSP / JSP EL Functions (`.tld`)

`${z:fn()}` on a `.dsp` page resolves **only** through the taglib's `<function>`
entries — `Taglibs.load()` builds the function map from the TLD by name,
`function-class`, and `function-signature`. There is no auto-discovery, so a new
static helper on `DspFns` / `ZkFns` / `JspFns` does not exist for the page until
it is registered:

- Register in the TLD that matches the page type: `.dsp` →
  `zk/src/main/resources/web/WEB-INF/tld/zk/core.dsp.tld` (uri
  `http://www.zkoss.org/dsp/zk/core`, prefix `z` by convention), `.jsp` →
  `core.jsp.tld`. They are not interchangeable — the JSP variants take explicit
  `ServletContext`/request/response arguments, so the same capability needs a
  different signature and usually a different `function-class` (`JspFns`, not
  `DspFns`).
- The method must be `public static`, and `<function-signature>` must match it
  exactly (fully-qualified return and parameter types).
- **All three failure modes are invisible to the build.** Unregistered → EL
  evaluation fails when the page is served; non-static → logged `Not a static
  method` and silently skipped; mismatched signature → logged `Method not
  found`. None of `compileJava`, `checkstyleMain`, or the lint gates catch any of
  them; the only proof is rendering the page.

## Public API Compatibility

Public Java APIs in `zk`/`zul`/`zkbind` can break EE/CML users. Grep the sibling
`zkcml` for call sites before changing a signature, return type, overload, or
behavior contract; prefer a compatible overload / deprecation over removal.
Every new public/protected component member needs JavaDoc stating default,
accepted tokens, side effects, and `@since MAJOR.MINOR.PATCH`; new public TS
widget members need TSDoc (use `@internal` only for `_`-prefixed members).

## CE / EE Boundary

CE (`zk`/`zul`/`zkbind`/`zhtml`) must never compile-time depend on EE
(`zkmax`/`zkex`/`zkrt`) — EE depends on CE, never the reverse (the standalone-CE
CI build fails on any reverse reference). CE detects an optional EE capability by
string + reflection only (`Classes.existsByThread("org.zkoss.zkex.Version")`),
never `import org.zkoss.zkmax.*`. A new EE component's class lives under
`org.zkoss.zkmax.*` and registers in `zkmax/.../lang-addon.xml`; a rendered EE
component overriding `renderProperties` keeps the
`org.zkoss.zkex.rt.Runtime.init(this)` license gate as the first line of that
render path.

## Messages & ARIA ownership

- A new message key must be physically present in the default bundle
  (`msgzul.properties`) and every shipped locale file — `Messages.get` falls back
  at the file level, not per key, so an existing locale file missing the key
  ships a localized "unknown message code", not the English base. Keep new keys
  English-identical in unmaintained locales (only the `zh` family is maintained);
  an unverified machine translation is worse than honest English.
- App-supplied ARIA uses the `ca:aria-*` client-attribute namespace, never a
  bespoke `setAriaLabel`-style property (one widget plays different roles per
  page). Accessibility labels for framework-rendered controls belong to EE
  `msgza11y`, not CE `msgzul`.

## Surface Audit — earn new surface

Before adding any new unit — method/property, ZUL attribute, event, CSS hook,
message key, or `render()`/`smartUpdate()` value — reach for the most generic
existing facility first: a pass-through namespace/attribute (`ca:*`),
`Library.getProperty` for config, reflection for EE detection, `msgzul`/`msgza11y`
for client-loaded strings, a base class, an existing constant catalog, a theme
token, or existing widget state. Prefer a generic facility over a new typed one,
and keep state on the correct side of the server↔client boundary — never ship a
value the client already holds or can derive. Reinventing a facility — or
re-sending derivable state — is a finding even when it looks cosmetic; record the
precedent you reused, or the grep that found nothing if it is genuinely new.
