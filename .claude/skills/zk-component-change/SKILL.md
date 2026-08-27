---
name: zk-component-change
description: Use when editing ZK Java components, TypeScript widgets, molds, component metadata, XSD, events, CSS hooks, or component tests.
---

# ZK Component Change

Keep the server component, client widget, metadata, styles, and tests in sync.
Opening any of these files auto-loads the matching `.claude/rules/*.md`
(`framework-contracts`, `java-component`, `typescript-widget`,
`security-accessibility`, `lifecycle-data-build`, `test-writing`) — read the
files and lean on those rules rather than re-deriving.

## Trigger surfaces

- `zul/src/main/java/org/zkoss/zul/**/*.java`
- `*/src/main/resources/web/js/**/*.ts` and mold `**/*.js`
- `zul/src/main/resources/metainfo/**` (lang.xml, XSD)
- `zkmax` / `zkex` / `za11y` component surfaces in `../zkcml`
- `zktest` artifacts validating component behavior

## Workflow

1. Identify every half of the pair: Java component, TS widget, mold, lang
   metadata, XSD, CSS, and tests that own the behavior.
2. Keep initial render and runtime `smartUpdate` sending the same effective
   value.
3. Pair every client `fire(...)` with the correct server `service(...)` typed
   event; every `smartUpdate`/`setX` with a client owner that re-runs its
   consumer.
4. Keep CE and EE separated — CE detects EE by string/reflection only; a rendered
   EE component keeps its `Runtime.init(this)` gate.
5. Add/adjust the test trifecta, then run the `zk-review` gates before finishing.
