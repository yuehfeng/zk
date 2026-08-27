# Review instructions (ZK / ZKCML)

Review-behavior overrides for this repo, injected verbatim as highest priority.
Self-contained on purpose — `@`-imports and referenced files do not load here, so
every rule that must apply is stated inline. Read the current source from disk;
never rely on cached content. In a local Claude Code session the per-surface
rules under `.claude/rules/*.md` also auto-load by path, but do not depend on
them here.

## Severity

🔴 **Important** is a bug that breaks production or a framework-contract
violation — report these first:

- Server/client desync: Java `render`/`smartUpdate` with no valid widget owner, a
  client `fire` with no server handler, or a coerced-render value whose
  `smartUpdate` path ships a different effective value.
- Typed-event contract violation (raw `Event`+map where a typed class exists).
- XSS / wrong encoder for the sink (server side).
- CE compile-time importing or depending on EE.
- Public `zk`/`zul`/`zkbind` API change that breaks `zkcml` call sites.
- Missing companion artifact for new framework surface, or reinventing an
  existing facility (see Surface audit).

🟡 **Nit**: style, naming, local refactor, payload-byte polish, CI-enforced
formatting. Cap at five; summarize the rest as a count.

## Do not report

- Anything CI already enforces: checkstyle, ESLint, `tscheck`, LESS compile.
- Generated/build output: `*/build/**`, `*/out/**`, `*/debug/**`, `*/codegen/**`,
  `*/generated-sources/**`, `*/node_modules/**`, lockfile churn.
- Test-only code that intentionally violates production rules.
- Same-ticket CE/EE release-order reminders when both sides are on the branch.
- The known-correct patterns below.

## Known-correct patterns (do not report)

Recurring project-wide false positives — do not report unless the PR changes the
contract itself:

- `InputEvent.getValue()` is the new value as `String`; `getPreviousValue()` is
  the typed old value. The asymmetry is intended.
- A server setter may throw `WrongValueException` while the TS widget clamps and
  shows `zk.error(...)` — the server stays source of truth.
- Repeated `invalidate()` during child add/remove is coalesced by the UI engine.
- `_`-prefixed top-level TS functions are exposed by the build transform — not
  undefined.
- CE no-op a11y hooks (empty `_moldA11yAttrs()` etc.) are EE `za11y` augment
  points — don't fill or delete them.
- A new i18n key ships English in every locale file until translations arrive;
  "in sync" means present in all locale files + generated constants
  (`MZa11y`/`global.d.ts`), not translated. Flag only a *missing* locale file or a
  server-key ↔ bundle-id mismatch.
- `<xs:group ref="baseGroup">` in a leaf XSD type permits ZK meta-elements
  (`<attribute>`), not visual children — not a contradiction of
  `isChildable()==false`.
- Enter/Space handlers calling `evt.stop()` intentionally suppress the native
  synthetic click — not double activation.
- `render(renderer, name, false)` already skips — no redundant guard.
- `as unknown as T` double casts are lint/type-required; don't reduce to a bare
  `as T`.
- `response(AuResponse)` is correct for state-independent one-shot actions
  (focus, select text, scroll, error) — don't force `smartUpdate`.
- `Html#setContent` is raw by design; `Label#setValue` is encoded. Flag only user
  input reaching a raw sink.
- A per-desktop activation lock makes `getChildren().size()` then `.get(idx)` in
  one `service()`/listener atomic — not a TOCTOU race.
- A TS setter storing `''` where the Java setter normalizes `''`→`null` is not a
  desync when every client sink is falsy-gated.
- TS setter params are typed plain (`setLabel(label: string)`) even where Java can
  `smartUpdate(null)` — eslint `noNull` forbids `null` in type positions; not a
  type mismatch.
- `##zats##` / `##ztl##` prefixes in `test2/config.properties` are active suite
  markers (`RunByTagsSuite`), not disabled entries — removing the prefix drops the
  test.
- EE components (`biglistbox`, `chosenbox`, `daterangebox`, …) in CE `zul.xsd` are
  a tooling/IDE artifact — the boundary rule is about compile/runtime deps, not
  schema documentation. Report only an actual CE→EE dependency or an
  `xs:attribute` set that disagrees with the EE component's setters.
- Every `*Type` inherits `<xs:anyAttribute processContents="lax"/>`, so an EL-only
  Object/Map setter (`setModel`, `setConfig(Map)`) is schema-valid via
  `attr="${…}"` without an `xs:attribute` — not "missing from XSD". Report only a
  *declared* `xs:attribute` that disagrees with a public String setter.
- `msgza11y` labels name only text-less icon controls; a single-input / editor
  widget with no icon buttons correctly ships zero `msgza11y` keys (its name comes
  from the page's `ca:aria-*`) — not "missing labels".
- ZK's static-resource ETag is emitted unquoted (framework convention in
  `JspFns`) and compared exactly — not RFC-7232-nonconformant. Report only a
  framework-wide change that fixes some paths but not others.
- A CycloneDX aggregate with `includeTestScope=false` correctly omits test-scoped
  deps (e.g. javassist is `test` scope in `zuti`); a flagship-named aggregate
  `outputName` is intentional. Verify the dep's real scope before flagging an SBOM
  omission.

## Surface audit — first-class (discover, don't assume)

Reinventing a framework facility is a top-severity finding on its own, never a
nit — do not down-rank or cut it for looking cosmetic. Enumerate **every** new
unit the PR introduces — public method/property, ZUL attribute, value pushed via
`render()`/`smartUpdate()`, event, CSS hook, user-facing string — and for each,
before accepting it as novel:

- Find the most generic existing mechanism first. Don't stop at "it calls a real
  API" — prefer a generic facility (`ca:*` attribute pass-through,
  `Library.getProperty`, a shared bundle, a base class, an existing constant
  catalog, a theme token) over a new typed one.
- Prefer the correct owner across the server↔client boundary — flag any value the
  server re-resolves and ships that the client already holds or can derive.
- Cite the precedent as `file:symbol`. If you claim something is genuinely new,
  quote the greps that returned nothing.

## ZK contract checks

Run these before accepting a change; each is a common miss no linter catches:

- **Component ↔ widget pairing.** Every Java `smartUpdate`/`setX` has a client
  owner that re-runs its consumer; every TS `fire`/`addClientEvent` has a Java
  `service()` branch; render and update send the same effective value; a
  `doClick_` override calls `super.doClick_(evt, true)` (literal `true`, not a
  forwarded `popupOnly`) so the click fires once.
- **XSD sync.** A public `zul` attribute setter added/removed/renamed updates
  `zul.xsd` (right complexType, matching type) and bumps the trailing schema
  timestamp; expand inherited attribute groups before claiming an attribute is
  missing. A **new element** must also be referenced in both `anyGroup` and
  `anyGroupSingle` — `grep -c 'xs:element ref="x"' zul.xsd` must be `2`; a
  declared-but-unreferenced type is an orphan no container accepts, independent
  of whether the component takes children.
- **Typed events.** Use `Events.*`/typed classes; commit events serialize the
  value the user displays; a debounced commit tolerates duplicate same-target
  events; never pass `request.getData()` straight through.
- **EE/CE boundary.** CE detects EE by string/reflection only; a rendered
  `zkmax`/`zkex` component calls `org.zkoss.zkex.rt.Runtime.init(this)` first in
  `renderProperties`.
- **Clone/serialization.** A component owning a model/renderer/listener
  deep-copies it in `clone()` and uses private `writeObject`/`readObject`.
- **i18n.** A new key is present in the default bundle + every locale file + any
  generated constant.
- **LESS/CSS.** An enum visual mode has a positioner and one underscore token set
  across TS/LESS/Java/XSD; `font-family` uses the theme LESS variables.
- **Lifecycle.** `unbind_` releases what `bind_` took (capture the node in `bind_`,
  don't re-resolve `$n()` in `unbind_`); parent-managed DOM survives child
  rerender; every dismiss path shares one guard flag.
- **Validation placement.** Normalize/validate at the shared layer, not a late
  `renderProperties` throw that runtime `smartUpdate` bypasses.
- **Public API.** Grep `../zkcml` before changing a signature; prefer a
  deprecated overload; new public/protected Java needs JavaDoc + `@since`, new
  public TS needs TSDoc.
- **Comments.** A new inline comment states a decision or a trap in one line (two
  max), not the session narrative that led there. Public JavaDoc/TSDoc is exempt
  from the cap — judge it against the contract it owes (default, accepted tokens,
  side effects, `@since`) and the neighbouring API, not against its length.
- **Tests.** Java test + ZUL page + `config.properties` entry; assertions
  discriminate (would fail before the fix); interaction fixes have interaction
  tests.

## Evidence

Cite current `file:line`; framework-contract findings cite precedent as
`file:symbol` or `file:line`. Code precedent is binding; a ticket number alone is
not proof. If you claim something is new, include the search terms checked. If a
finding predicts a concrete test failure, run the targeted test before finalizing
it; if you cannot, mark it unverified and lower confidence. Do not cite
generated/build output as precedent.

## Re-review convergence

After the first full review, focus later passes on new changes, unresolved
Important findings, and rules affected by the fix. Suppress new Nits unless the
latest patch introduced them or the user asks for a fresh sweep.
