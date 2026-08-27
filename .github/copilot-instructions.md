# ZK Framework — AI Development Guide

Build, test, and workflow mechanics for this repo. Framework coding conventions
are in `AGENTS.md` (which imports this file); Claude Code's deep per-surface
rules are in `.claude/rules/`.

## Project Overview

ZK is an open-source Java web framework. The sibling directory `../zkcml/`
contains enterprise (EE) extensions that depend on this project.

**Modules:** `zk`, `zul`, `zkbind`, `zhtml` (core, TypeScript + Java);
`zcommon`, `zel`, `zweb`, `zweb-dsp`, `zkplus` (shared utilities); `zktest`
(Selenium/WebDriver integration tests); `eslint-plugin-zk` (custom lint rules).

## Version

**Always read the current version dynamically** from `gradle.properties` — never
hardcode. `zk/gradle.properties` (CE) and `zkcml/gradle.properties` (EE) must
stay in sync. The active dev version ends in `-SNAPSHOT`. Version-code mapping
(file naming): `10.0.0`→`100`, `10.4.0`→`104`, `11.0.0`→`110`.

```bash
./gradlew upVer -PchangeVersionTo=X.Y.Z
./gradlew versionCheck -Pcheck.version=X.Y.Z
```

## Tech Stack

Java 11, Gradle 7.6.4, Node 20, TypeScript 5.3.3, ESLint 9 (flat config), Gulp 5,
Webpack 5, Checkstyle 10.18.1.

## Build Commands

```bash
npm run build          # build all TS/JS (gulp)
npm run dev            # watch mode
npm run type-check     # tsc --noEmit
npm run lint -- <path> # ESLint (path arg required; whole-tree `.` exceeds the CI gate)
./gradlew tscheck      # root TS type-check (composite-aware)
./gradlew jscheck      # JS lint, per subproject (:zul:jscheck) — this IS the CI gate
./gradlew jsfix        # manual ESLint autofix entry; don't run eslint --fix directly
./gradlew compileLess  # LESS compile (per subproject)
./gradlew checkstyleMain          # Java style
./gradlew :zk:build               # single-module build
./gradlew publishToMavenLocal     # so zktest picks up local changes
```

## Code Style

ESLint (flat `eslint.config.js` at root, custom `eslint-plugin-zk` + Microsoft
SDL) and Checkstyle (`config/checkstyle/`) are CI-blocking and enforce Java/TS
style, imports, naming, Javadoc/TSDoc presence, and client-side XSS sinks. Fix
all errors before committing; do not disable `eslint-plugin-zk` rules without
review. What lint can't check is in `AGENTS.md` / `.claude/rules/`.

## Testing

**Every bug fix and new feature needs a test.** Do NOT use the IDE's built-in
runner (▶) — it skips resource processing and yields `Language not found` errors.
Use the CLI:

```bash
# single class (from repo root)
cd zktest && ./gradlew test --tests "org.zkoss.zktest.zats.test2.B104_ZK_6047Test" \
  -PmaxParallelForks=1 --console=plain --no-daemon
# a class tagged @ForkJVMTestOnly (needs Docker)
cd zktest && ./gradlew testGroupForkJVMTestOnly --tests "…" -PmaxParallelForks=1 --console=plain --no-daemon
cd zktest && ./gradlew test            # full suite (excludes WCAG / ForkJVMTestOnly)
cd zktest && ./gradlew testWCAGOnly    # accessibility (needs Lighthouse)
```

- Extend `org.zkoss.test.webdriver.WebDriverTestCase`; use `getActions()` (not
  `new Actions(...)`); write drags explicitly (`clickAndHold`/move/`release`).
- Path `zktest/src/test/java/org/zkoss/zktest/zats/test2/`; a ZUL page under
  `zktest/src/main/webapp/test2/` (dashes: `B100-ZK-5529.zul`); register it in
  `zktest/src/main/webapp/test2/config.properties`:
  `B100-ZK-5529.zul=A,M,ComponentName` (field 1 code level A/B/C, field 2 UX
  level H/M/E, field 3+ affected components).
- Naming, assertion quality, and interaction coverage: see
  `.claude/rules/test-writing.md`.

## Source Layout

TS source: `{module}/src/main/resources/web/js/`. Java: `{module}/src/main/java/org/zkoss/`.

## Critical Constraints

- Don't change public Java APIs in `zk`/`zul`/`zkbind` without checking `../zkcml/` impact.
- Any ZUL attribute/element added/removed/changed → update
  `zul/src/main/resources/metainfo/xml/zul.xsd` (and bump its schema timestamp).
- Issue tracker: https://tracker.zkoss.org/projects/ZK

## Workflow for Each Issue

1. Find/create the tracker issue.
2. Write the test first in `zktest/` using the issue id in the filename.
3. Add the ZUL page under `zktest/src/main/webapp/test2/`.
4. Register it in `config.properties`.
5. Implement the fix.
6. If a ZUL attribute/element changed, update `zul.xsd`.
7. `./gradlew :<mod>:jscheck checkstyleMain` — lint the module you touched.
8. Verify the test passes.
9. PR title references the issue id.
