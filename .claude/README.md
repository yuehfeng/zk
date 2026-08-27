# ZK Claude Topology

Project-local control center for Claude Code in the `zk` repository. Everything
committed here is meant to be safe to share with anyone who clones the repo.

## Layout

- `settings.json` — shared permissions + worktree behavior.
- `settings.local.json` — personal overrides; git-ignored, never committed.
- `rules/` — per-surface conventions. Each carries a `paths:` glob and
  **auto-loads only when you open a matching file** (and injects into a subagent
  when it reads a matching file), so they cost no resident context. There is no
  routing table to consult — the glob does the routing.
- `skills/` — task workflows (`zk-component-change`, `zk-review`,
  `zk-security-audit`, `zk-test-artifacts`, `fix-issue`).
- `agents/` — read-only checker subagents (component-architect, code-reviewer,
  security-auditor, test-architect); run them in parallel before "done".

The always-on framework core lives in the root `CLAUDE.md` → `AGENTS.md` →
`.github/copilot-instructions.md` import chain, not here.

## Workspace

Developed as two sibling repos: `zk` (this CE repo) and `zkcml` (EE, at
`../zkcml`). Treat them as one workspace for public API, EE/CE boundary, za11y,
and shared component behavior. Each repo has its own `.claude/`; open Claude Code
in the repo you are editing (`paths:` globs match only within their own repo).

## Not committed

Keep runtime / personal Claude files ignored: `settings.local.json`,
`*.local.json`, `scheduled_tasks.lock`, `worktrees/`, `.cc-writes/`, and local
helper scripts.
