---
name: zk-review
description: Use for code review of ZK/ZKCML changes — framework-contract bugs, compatibility, security, and missing-artifact checks.
---

# ZK Review

The full review contract — severity, do-not-report, known-correct patterns, the
first-class surface audit, and the ZK contract checks — lives in the repo-root
**`REVIEW.md`** (the same file the managed Code Review injects). Read it first and
follow it.

For a local review:

- Read current source from disk (never cached). Inspect the diff; as you read each
  changed file the matching `.claude/rules/*.md` auto-loads by path — lean on it
  rather than re-deriving.
- Check both `zk` and `zkcml` when public API, EE/CE boundary, za11y, or shared
  component behavior can cross the boundary.
- Report findings inline, ordered by severity (🔴 Important, 🟡 Nit, 🟣
  pre-existing), each with a current `file:line` and a concrete failure scenario.
- A recurring project-wide false positive belongs in `REVIEW.md`'s known-correct
  list, so every future review (managed and local) benefits — add it there rather
  than re-deciding per branch.
