---
name: zk-test-artifacts
description: Use when adding, reviewing, or scoping ZK ticket tests, WebDriver/ZATS pages, config.properties entries, or interaction coverage.
---

# ZK Test Artifacts

Keep ticket tests present, discoverable, and effective. Opening a `*Test.java`
auto-loads `test-writing.md` (naming, trifecta, API, non-vacuous assertions) —
use it as the reference.

## Scope the artifacts

1. Classify the change: bug/regression (`B{verCode}`) or feature (`F{verCode}`).
2. Check the trifecta is complete when it applies — Java test, ZUL page under
   `test2/`, and a `config.properties` entry (Java uses underscores, ZUL/config
   use dashes).
3. Confirm the assertion discriminates (would fail before the fix), not just
   `assertNoJSError()`; an interaction fix needs an interaction test on the
   actual path (keyboard, paste, IME, hover, drag, focusout, close/dismiss).
4. EE/PE components are covered here via WebDriver, never bare module JUnit.

## Pick the verification command

- Single class: `cd zktest && ./gradlew test --tests "…B104_ZK_6047Test" -PmaxParallelForks=1 --console=plain --no-daemon`
- `@ForkJVMTestOnly`-tagged class: `testGroupForkJVMTestOnly` instead of `test`.
- Accessibility: `./gradlew testWCAGOnly`. The suite runs both A11Y and NO_A11Y —
  guard za11y-dependent assertions accordingly.
