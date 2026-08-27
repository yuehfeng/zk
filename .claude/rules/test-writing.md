---
paths:
  - "zktest/src/test/java/**/*Test.java"
  - "zktest/src/main/webapp/test2/*.zul"
  - "zktest/src/main/webapp/test2/**/*.zul"
  - "zktest/src/main/java/**/*.java"
description: ZK WebDriver/ZATS test conventions, trifecta, ZUL page authoring, and API reference
---

# ZK Test Rules (WebDriverTestCase)

## Ticket artifacts (the trifecta)

A ZK ticket with behavior or public-surface impact usually ships three artifacts
together:

- Java WebDriver/ZATS test under `zktest/src/test/java/.../test2/`.
- ZUL page under `zktest/src/main/webapp/test2/`.
- A `config.properties` entry when the page belongs to the test catalog (the
  Check-PR-Files CI action gates required PR files at PR-open). Put the line
  **inside its own version group** — the contiguous `##zats##B110-…` block — not
  appended at EOF; some existing entries sit at EOF and are the mistake, not the
  pattern. The file is **CRLF**: append with `\r\n`. A lone bare-LF line is
  invisible to `file(1)` (still "with CRLF line terminators"); only comparing CR
  and LF byte counts finds it —
  `tr -dc '\r' <f | wc -c` vs `tr -dc '\n' <f | wc -c`.

`verCode` is the **current** dev version: read `version=` from
`gradle.properties` and concatenate major and minor — `11.0.0-SNAPSHOT` → `110`,
`10.4.0` → `104`. Derive it; it moves with the release.

Naming: `B{verCode}` for bug/regression, `F{verCode}` for feature (e.g.
`F104_ZK_6098SpeeddialTest`). Java names use underscores; ZUL/config names use
dashes. Legacy numeric, `Z`-prefixed, and descriptive-suffix names
(`F104_ZK_5409_DeviceMatrixTest`) already in the tree are sanctioned — match the
neighbours, don't force a rigid pattern.

## ZUL page authoring

`<zscript>` is interpreted by **BeanShell 2.0b6**, not compiled by javac
(`zcommon/build.gradle:16`; `zk/…/metainfo/zk/config.xml:24-25` maps language
`Java` to `BSHInterpreter`). No generics, diamond, lambdas, or `var` — across the
tree's 1504 `<zscript>` blocks there are zero of each. It is also not
JavaScript: `'x'` is a char literal, so `onClick="setLabel('x')"` silently
no-ops where `onClick='setLabel("x")'` works. Nothing catches any of this at
build time — BeanShell parses at page load, so the failure arrives as a
`UiException` that reads like a framework bug. `<script>` is the client-side JS
counterpart; don't reach for one when you mean the other.

A composer / ViewModel / model the page needs is a **webapp** class and belongs
in `zktest/src/main/java/org/zkoss/zktest/test2/` (package
`org.zkoss.zktest.test2`) — not beside the test, which sits in a different source
set under the same package with `zats.` inserted
(`org.zkoss.zktest.zats.test2`) and is invisible to the webapp classloader.
Reference it by FQCN, normally `apply=` (383 pages) over `use=` (23):
`B100-ZK-5529.zul:22` `apply="org.zkoss.zktest.test2.B100_ZK_5529Composer"` ↔
`B100_ZK_5529Composer.java` ↔ `B100_ZK_5529Test.java`.

Neither the pages nor these support classes are linted — `eslint.config.js`
scopes ESLint to `**/*.ts`, and `config/checkstyle/suppressions.xml` suppresses
`checks=".*"` for `files="zktest/.*"`. Correctness here rests entirely on review.

## Assertions must discriminate

Prove the behavior: exact value / count / state beats a broad `||` fallback, an
existence-only check, or `assertNoJSError()` when the bug is visible in DOM or
state. A test that passes with or without the fix is vacuous — verify it fails
before the fix. Interaction fixes need interaction tests (keyboard, paste, IME,
hover, drag, focusout, close/dismiss), not just a load-and-assert.

Comments follow `java-component.md` § Comments & JavaDoc — one line saying what
the case pins down, not the debugging story that found it.

## Reading a test run

A green console line is not evidence. Read the JUnit XML under
`zktest/build/test-results/…`, and read the directory that matches the variant
you ran:

- **A11Y** (the default) writes the unmarked `test-results/test/`.
- **NO_A11Y** writes `test-results/no-A11Y/test/` — `build.gradle` redirects
  `reports.junitXml.destination` for that variant. So after running both, the
  default directory still holds the **A11Y** results; reading it believing you
  read NO_A11Y is silent and gives identical-looking numbers. Compare timestamps.
- Only the XML is redirected, **not** the HTML report: `build/reports/tests/test/`
  is overwritten by whichever variant ran last, so the default-path HTML and the
  default-path XML can describe different runs.

Variant selection is a **string** compare — `zktestWithoutA11y == 'false'` means
a11y IS included, and `zktest/gradle.properties` defaults it to `false`. Any other
value (`FALSE`, `0`, a typo) silently selects NO_A11Y, and nothing in the console
says which variant ran. The positive check is
`find build -name 'za11y*.jar' | wc -l` — `0` means the last run had no za11y.

Never judge a run from a `grep`-filtered tail of the gradle output: filtering out
the `PASSED` lines leaves a log that looks like zero tests ran.

## API quick reference

- **Lifecycle**: `connect()` loads the ZUL page (call before `getDriver()`);
  `connect(path)` a specific path; `waitResponse()` after every interaction.
- **Selectors / read-back**: `jq(".z-button")` (jQuery), `.exists()`, `.text()`,
  `.css("prop")`, `.toWidget()`; `widget(jq)`; `getEval(js)` to read a JS value
  back into Java (bare `eval(js)` is void).
- **Interactions**: `click(w)`, `type(w, text)`; `getActions()` for Selenium
  Actions (not `new Actions(driver)`); drag as
  `clickAndHold → moveByOffset → release` (not `dragAndDropBy`).
- **Pitfalls**: a test tagged `@ForkJVMTestOnly` runs via the
  `testGroupForkJVMTestOnly` task; the suite runs both A11Y and NO_A11Y, so guard
  za11y-dependent assertions with
  `if (!Boolean.valueOf(getEval("!!window.za11y"))) return;`.
- EE/PE components are covered here through WebDriver (real container + license),
  never bare module JUnit under `zkex`/`zkmax`.
