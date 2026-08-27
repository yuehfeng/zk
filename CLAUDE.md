@AGENTS.md

## Claude Code in this repo

The framework conventions above (via `@AGENTS.md`) are the always-on core.
**Detailed per-surface rules live in `.claude/rules/*.md` and auto-load by
`paths:` when you open a matching file** — editing a component's `.java` pulls in
`framework-contracts`, `java-component`, `security-accessibility`, and
`lifecycle-data-build`; a widget `.ts`/mold `.js` pulls in `typescript-widget`;
a `*Test.java` pulls in `test-writing`. Subagents get the same rule injected when
they read a matching file, so you don't restate rule content to them.

## Skills (`.claude/skills/`)

- `fix-issue` — implement a ZK ticket/bug fix end to end (any surface).
- `zk-component-change` — keep both halves of a component change in sync.
- `zk-review` — review a diff against the ZK gates, priorities, and
  known-correct patterns.
- `zk-security-audit` — audit output-encoding, resource, and CE/EE surfaces.
- `zk-test-artifacts` — plan/verify the test trifecta and interaction coverage.

## Checker subagents (`.claude/agents/`)

Run these read-only agents in parallel before declaring a change done:
`zk-component-architect`, `zk-code-reviewer`, `zk-security-auditor`,
`zk-test-architect`.

## Test artefact naming & registration

The `B`/`F` prefix of a test page is the **current dev version's code**: read
`version=` from `gradle.properties` and concatenate major and minor —
`11.0.0-SNAPSHOT` → `110`, `10.4.0` → `104`, `10.0.0` → `100`. Derive it every
time; never hardcode it, it moves with the release.

Register the page in `zktest/src/main/webapp/test2/config.properties` **inside its
own version group** — the contiguous `##zats##B110-…` block — not appended at the
end of the file. (Some existing entries sit at EOF; they are the mistake, not the
pattern.)

That file is **CRLF**. Append with `\r\n`. A single bare-LF line is invisible to
`file(1)`, which still reports "ASCII text, with CRLF line terminators"; only
comparing CR and LF byte counts (`tr -dc '\r' | wc -c` vs `tr -dc '\n' | wc -c`)
reveals it.

## Workspace

Developed as two sibling repos: `zk` (this CE repo) and `zkcml` (EE, at
`../zkcml`) — treat them as one workspace for public API, EE/CE boundary, za11y,
and shared component behavior. Each repo has its own `.claude/`; open Claude Code
in the repo you are editing.
