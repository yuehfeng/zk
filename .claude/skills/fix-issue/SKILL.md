---
name: fix-issue
description: Use when implementing a ZK ticket or bug fix end to end (ZK-XXXX), from success criteria through the review gates.
---

# Fix Issue

Implement a ZK change with the required checks. For a component change, the
`zk-component-change` skill carries the pairing specifics; this skill is the
surrounding process for any surface.

## Before editing

1. State the concrete success criteria (what observable behavior proves it
   fixed). Investigate history first when the cause is unclear — `git blame` →
   `ZK-XXXX` → CaseFoundry `lookup_issue`.
2. Locate the touched surface and open its files — the matching
   `.claude/rules/*.md` load automatically.
3. Check whether `../zkcml` is affected (public API, EE/CE boundary, za11y,
   shared component behavior).

## Implement

- Keep the diff surgical and match surrounding ZK style; every changed line
  traces to the ticket.
- Preserve component/widget pairs (Java render/smartUpdate, TS setters, events,
  molds, lang metadata, XSD, CSS, tests stay in sync).
- Prefer an existing framework facility over new public surface.
- Add/update the test trifecta when behavior or public surface changes
  (`zk-test-artifacts`).

## Before finishing

1. Run the smallest meaningful compile/test command for the surface.
2. Run the `zk-review` gates against the diff.
3. Summarize changed files, verification run, and any residual risk.
