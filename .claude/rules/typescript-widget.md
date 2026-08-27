---
paths:
  - "*/src/main/resources/web/js/**/*.ts"
  - "*/src/main/resources/web/js/**/*.js"
description: ZK TypeScript widget and mold authoring patterns (beyond what lint enforces)
---

# TypeScript Widget Rules

Client-side authoring for widgets (`.ts`) and molds (`.js`). Pairing and event
contracts co-load from `framework-contracts.md`; comment length, register, and
doc-comment shape follow `java-component.md` § Comments & JavaDoc (TSDoc in place
of JavaDoc).

`eslint-plugin-zk` + `@microsoft/sdl` + strict `@typescript-eslint` already
reject the syntactic rules (no access modifiers / `_`-prefix non-public,
`this`-returning setters, `undefined` not `null`, native `class`/`instanceof`, no
property-functions, TSDoc shape, `noMixedHtml`, `noLocationHrefAssign`) — CI
catches them, so don't hand-check or re-report them. The patterns below are what
lint does **not** cover.

## Authoring patterns

- **`@zk.WrapClass('module.ClassName')`** registers the widget; extend the
  established base (`zul.Widget`, `zul.mesh.MeshWidget`).
- **`override` is mandatory** when redefining a parent method (`tsconfig` sets
  `noImplicitOverride`) — omitting it fails type-check.
- **Setters take `opts?: Record<string, boolean>` and rerender only on change** —
  compare old vs new and update DOM / `rerender()` only when
  `o !== val || opts?.force`.
- **Module-level `_`-prefixed functions are auto-exposed** — a top-level
  `function _initUpld()` is rewritten to `zk.<pkg>_._initUpld()` by the build
  transform so it survives as a cross-file / `fire()` target; call it by plain
  name. The transform skips `index.ts` and `global.d.ts`.
- **`fire(evt, data?, opts?, timeout?)`** — opts vocabulary is in
  `framework-contracts.md`; 4th arg is a delay in ms.
- **Mold `.js`**: accumulate markup with `out.push(...)`; emit attributes /
  content via `this.domAttrs_()` / `this.domContent_()`, class names via
  `this.$s('cls')`, id via `this.uuid` — never inline-concat attributes or uuid.
  Mark known-safe HTML with a leading `/*safe*/` to satisfy the XSS lint rules.
- **`zk.wpd`**: `<package name>` equals the JS folder path (`zul.inp`); list
  upstream packages in `depends="…"`; template-only widgets are `moldOnly="true"`.
  **`index.ts`**: `export default {};` then `export * from './X'`; annotate an
  API-doc class with trailing `// jsdoc="true"`.
- **Casts**: narrow with `as unknown as T`, never a bare `as T`; forbid bare
  `Function` (use `zk.Callable`); prefer `interface` over `type`; explicit
  function return types.

## Lifecycle & DOM

- Guard DOM work in `bind_()`/`onSize()` with `zk.mounting`/`this.desktop`;
  defer init needing stable DOM with `zk.afterMount(() => …)`; signal
  widget→widget via `zWatch.fire()`/`fireDown()` (no AU round-trip).
- **`unbind_()` must undo what `bind_()` did** — release listeners, timers,
  watchers, and DOM refs. `unbind_` can run after the node is already gone, so
  **capture the node/resource in `bind_` and reuse it in `unbind_`, don't
  re-resolve `$n()`/`$n_()`** (re-resolving in teardown is an NPE-class bug).
  **Parent-managed DOM attributes/classes must survive a child rerender/rebind,
  or be reapplied by the owner** — losing them on rerender is a recurring bug.
- **Every dismiss path shares one guard.** Backdrop click, document-click,
  keyboard `Esc`, and programmatic close must all funnel through the same guard
  flag (e.g. `closeOnOutsideClick`) — one path bypassing it is a recurring
  popup/overlay bug.
- **A `doClick_` override that calls `this.fireX(evt)` must then call
  `super.doClick_(evt, true)` with the literal `true`** — not the forwarded
  `popupOnly` — so the base path doesn't fire the same click again (forwarding
  `popupOnly` is the recurring double-fire bug). An override that only opens or
  closes something and fires nothing forwards `popupOnly` unchanged, as
  `ComboWidget` and `Datebox` do — the literal `true` is for the firing case only.

### Keyboard and focus for a float

- **A float does not take focus when it opens; its keys are forwarded.** The
  owning input keeps focus and its `doKeyDown_` hands the event on
  (`this._pop.doKeyDown_(evt)`), with Alt+↓/↑ as the open/close gesture and Tab as
  the one deliberate hand-off. No ZK float focuses itself on open. A float that
  takes focus without a `doKeyDown_` of its own is keyboard-dead: arrows and Enter
  reach nothing, and the owner's Alt+↑ becomes unreachable.
- **Make the float an `appendChild` child of its owner** so the default
  `doKeyDown_` bubbling carries unhandled keys back up. A `document` keydown
  listener is a last resort, justified only for keys aimed outside the owner.
- **A grid-like float's cursor is one off-screen `tabindex="-1"` anchor**
  (`z-focus-a`), moved by the widget — never a tabindex per cell. `Calendar`'s day
  cells sit outside the Tab order deliberately.
- **A trigger button beside an input is `tabindex="-1" aria-hidden="true"`**
  (ZK-4598); the wrapper carries the semantics. Making it a tab stop means owning
  both Enter and Space activation, which an `<a>` without `href` gets from neither.
- **`role="dialog"` on a ZK float is labelling** (or an axe
  scrollable-region-focusable fix), never a focus trap.

## Input widgets — one value pipeline

DOM display value, committed/coerced value, and server/widget value are one
pipeline. Integrate with inherited `InputWidget` blur/change/onChanging/onError
bookkeeping, not parallel raw DOM listeners. Raw `keydown`/`paste`/`beforeinput`/
`input`/composition handlers that mutate DOM or `preventDefault()` must respect
`readonly`/`disabled` gates, IME snapshots, and `onChanging` ordering — verify
with browser interaction tests, not just unit checks.

## Enum-backed visual modes

An enum visual-mode value must have a positioner: either the CSS owns the layout
(a JS early-return is then safe) or the widget positions it in JS. The token set
must be identical across the TS default, the LESS selectors, Java validation, and
the `zul.xsd` enumeration — multi-word tokens are underscore (`top_right`), never
hyphen.
