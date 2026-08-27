---
paths:
  - "{zk,zul,zkbind,zhtml}/src/main/java/**/*.java"
description: ZK Java component setter, render, clone, serialization, and helper conventions
---

# Java Component Rules

Server-side authoring boilerplate for an `org.zkoss.*` component. Pairing,
render/smartUpdate coherence, events, XSD, and API contracts co-load from
`framework-contracts.md`. Precedent: `zul/.../Button.java`.

## Property storage & setters

- **Optional properties live in a lazy inner `AuxInfo`, not direct fields.** A
  rarely-set property is stored in a serializable/cloneable `private static class
  AuxInfo`; the getter reads `_auxinf != null ? _auxinf.x : DEFAULT`, the setter
  writes `initAuxInfoForXxx().x = …`, and `clone()` deep-copies `_auxinf`. Avoids
  one null field per never-set property on every instance.
- **Setter shape: guard with `Objects.equals`, then `smartUpdate(name, getter())`.**
  Compare old (null-coalesced the same way the getter is) vs new; only on change
  mutate `AuxInfo` and `smartUpdate("prop", getProp())` — push the getter
  result / `DeferredValue`, not the raw arg.
- **Register client events in a `static {}` block** via
  `addClientEvent(X.class, Events.ON_FOCUS, CE_DUPLICATE_IGNORE)`; pair each with
  a `service()` branch.
- **`renderProperties()` calls `super` first and renders only non-defaults.**

## Exceptions & logging

- `UiException` for structural violations (wrong/duplicate child in
  `beforeChildAdded`/`onChildAdded`); `WrongValueException` for bad values / bad
  enums in a setter.
- Logger field is lower-case `log`, used only for real diagnostics — not for
  ordinary input recovery or valid intermediate states, where
  `WrongValueException` / `UiException` / JavaDoc / a documented fallback is the
  right contract.
- Prefer `response(AuResponse)` for state-independent side effects (focus, select
  text, scroll); the `response(key, resp, priority)` overload orders them.

## clone() & serialization of live fields

When a component holds a model, renderer, or registered listener:

- **`writeObject`/`readObject` must be `private`** — the JVM only invokes the
  private magic methods; `protected`/`public` silently disables serialization.
- **Model/renderer fields serialize via `willSerialize(...)` +
  `Serializables.smartWrite(...)` and restore via `didDeserialize(...)`** — not a
  plain `s.writeObject`, which skips the `ComponentSerializationListener`
  callbacks.
- **`clone()` nulls transient listener fields then recreates them**
  (`clone._dataListener = null; clone.initDataListener();`) — else the clone
  shares the original's listener and events fire on the wrong component.

## Foundational helpers (reach for these, don't re-roll)

- `Objects.equals`/`hashCode` (null-safe, array-deep, BigDecimal-aware),
  `Strings.isEmpty`/`isBlank`, `Classes.forNameByThread`/`existsByThread`,
  `Exceptions.getRealCause`.
- Config reads go through `Library.getProperty(key[, def])` (per-deployment scope
  then `System.getProperty`, never throws) — the same mechanism CE uses to
  feature-detect optional EE/integration classes.
- Integration modules (`zkplus`) declare no `<component>` — only
  listeners/resolvers — and detect optional frameworks at runtime via
  `Library.getProperty` + `Classes.existsByThread`.

## Comments & JavaDoc

A comment carries what the code cannot: **the decision taken**, or **the trap the
next editor will fall into** — not the context that led there. Ticket numbers and
design rationale stay in the commit message / Jira (`AGENTS.md`), never in
production code.

- **One line by default; two is the ceiling** — for inline comments and for
  `private`/package-private members. A paragraph that explains mechanism, weighs
  alternatives, or recounts how a bug was found is a commit message, not a comment.
- **State a standing fact, not a working session.** "Skipped when the tag already
  carries one: the parser keeps the first nonce." — not "we found that when the
  page supplies its own nonce, the parser keeps the first of two and drops ours,
  so …".
- **Keep genuine landmine warnings, compressed to the warning itself.**
- **Delete test.** If removing the comment loses nothing a reader can't recover
  from the code in ~5 seconds, delete it.
- **JavaDoc is the behavioural contract** — what it does, what it throws, which
  sibling API to use instead (`{@link}`). Not why it was added, not a regression
  history; no "see spec" / "per ZK-XXXX".
- **`public`/`protected` JavaDoc is exempt from the cap.** Its length is whatever
  the contract requires — default, accepted tokens, side effects,
  `@param`/`@return`/`@exception` when the signature doesn't answer them, and
  `@since MAJOR.MINOR.PATCH` for anything past the module's base. Simple ZK
  accessors genuinely are one line (`/** Sets the value. */`), a getter adding
  `<p>Default: 0 (means no limitation)`. Read and mirror the neighbours —
  `zul/.../Label.java` (accessor pairs with `Default:`), `zul/.../Combobox.java`
  (`@since` + longer contract) — don't invent a style.
