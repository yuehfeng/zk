---
paths:
  - "{zk,zul,zkbind,zhtml}/src/main/java/**/*.java"
  - "*/src/main/resources/web/js/**/*.ts"
  - "zul/src/main/resources/**/*.less"
description: ZK model-backed data components, date/locale round-trip, LESS/CSS theming, performance
---

# Data, Date, Layout & Performance

Component clone/serialization lives in `java-component.md`; client
attach/detach/input lives in `typescript-widget.md`.

## Model-backed data components

Renderer, paging, selection, open state, fake/ROD rows, dynamic child
add/remove, and detach behavior must stay synchronized across server and client.
A selective/diff update path must preserve the same invariants as a full
render — a partial update that skips an invariant the full render enforces is a
desync.

## Date, timezone & locale numbers

Date/time and number formatting must round-trip between Java and the client with
the same timezone, locale, parser, formatter, and legacy-identifier behavior.
Locale grouping and numeric parsing are part of the contract, not display polish.

## LESS / CSS & theming

- `font-family` references the project LESS variables, never a hardcoded stack:
  `@baseContentFontFamily` for content text, `@baseTitleFontFamily` for
  titles/headings (defined in `zul/.../web/zul/less/_zkvariables.less`). A literal
  `font-family: Arial, …` bypasses theming.
- Prefer existing `zclass`, mold, and theme-hook structure; new hooks must be
  stable and themed.
- An enum visual-mode value must agree on one token set across the TS default,
  LESS selectors, Java validation, and `zul.xsd` (underscore tokens); every value
  needs a positioner (see `typescript-widget.md`).
- Masks / skeletons / overlays must cover the real root-element categories they
  claim (replaced elements, form controls, SVG/media, special roots). A decorative
  pseudo-element does not block interaction — verify pointer-events / focusability
  / disabled state or a real overlay element.
- Repointing a component to a **new** `--zk-*` theme var (instead of the one it used)
  is a **three-repo** change, not a CE-only one. The EE theme LESS lives in
  `github.com/zkoss/zkThemeTemplate` (the `zkcml/zkthemebuilder/template` submodule),
  and each palette override lives in `zkcml/zkthemebuilder/palettes/*_css.less`. A
  palette that overrode only the OLD var silently falls back to the base value once
  the template syncs — e.g. the 10 dark palettes (montana, spaceblack, …) set only
  `--zk-mask-background-color:#151515`, so ZK-6105's move to
  `--zk-mesh-outer-background-color` repaints their grid/listbox/tree outer band light.
  A CE-only `var(--new, var(--old))` fallback does NOT save it (the synced template
  defines `--new`). Require matching updates in zkThemeTemplate **and** every palette
  overriding the old var. `Build-ThemePack` Jenkins auto-pulls template `origin/master`
  and builds all jars with no error on this — gate the ordering with `ZK_THEME_HEAD`.

## Performance & memory

Avoid retained listeners, repeated expensive DOM work, unbounded caches, and
large buffering without an owner. A component or desktop that starts a timer must
cancel it on cleanup. Distinguish a real regression from a harmless
micro-optimization, and test the path that can leak or repeat.
