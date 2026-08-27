@.github/copilot-instructions.md

# ZK Framework Conventions

Cross-cutting invariants for any agent working in this repo. Detailed
per-surface authoring rules live in `.claude/rules/*.md` and (for Claude Code)
auto-load when you open a matching file; other tools read them as needed.

## Component–Widget Duality

Every UI element has two halves that must stay in sync:

- **Java component** (server): state, validation, `smartUpdate()` pushes changes.
- **TypeScript widget** (client): DOM, interaction, `fire()` sends events.

Mapping: `org.zkoss.zul.Button` ↔ `zul.wgt.Button`. When you change one side,
check whether the other needs it too. A Java `smartUpdate("x")` needs a client
`setX` that re-runs its consumer; a client `fire("onX")` needs a Java `service()`
branch. **Initial render and later `smartUpdate` must send the same effective
value** — a coerced-render / raw-update mismatch is the most common bug.

Render ownership: `renderProperties` for initial state, `smartUpdate` for
persistent state, `response(AuResponse)` for one-shot side effects (focus,
scroll, error). Don't push what the other side can derive.

## Typed Events

An event with a typed class must be posted as that class, never a raw `Event` +
data map: `onSelect`→`SelectEvent`; `onChanging`/`onChange`→`InputEvent`
(`getValue()` = new String, `getPreviousValue()` = typed old); `onError`→
`ErrorEvent`. `onClose`/`onOK`/`onCancel` (no typed class) stay plain `Event`.
Rebuild the payload from the `*.get*Event(request, …)` factory — never pass
`request.getData()` straight through.

## EE / CE Boundary

CE (`zk`/`zul`/`zkbind`/`zhtml`) never compile-time depends on EE
(`zkmax`/`zkex`); EE depends on CE. CE detects EE by string + reflection
(`Classes.existsByThread(...)`), never `import org.zkoss.zkmax.*`. A public Java
API change in `zk`/`zul`/`zkbind` must be grepped against the sibling `zkcml`
before shipping. A rendered EE component keeps its
`org.zkoss.zkex.rt.Runtime.init(this)` license gate.

## i18n & ARIA ownership

A new message key must exist in the default bundle and every shipped locale file
(fallback is file-level, not per-key); keep it English-identical in unmaintained
locales. Accessibility labels for framework-rendered controls go to EE
`msgza11y`, not CE `msgzul`; app-supplied ARIA uses `ca:aria-*`, not a bespoke
`setAriaLabel` property.

## Module Dependency Order

```
zel → zcommon → zweb → zweb-dsp → zk (+zkwebfragment) → zul → {zhtml, zkplus} → zkbind
```

`zel` is the zero-dependency base; `zkbind` depends on both `zul` and `zhtml`.
`zktest` depends on all modules (build upstream first if tests fail). The EE
sibling `zkcml` depends on `zk`, `zul`, `zkbind`.

## Bug Investigation (CaseFoundry MCP)

18,000+ historical cases. `search_cases("description")` finds similar bugs;
`lookup_issue("ZK-XXXX")` gives full context; `diagnose_from_stacktrace("…")`
analyzes a trace. To trace code history: `git blame` → extract `ZK-XXXX` from the
commit message → `lookup_issue`.

## Comments

A comment carries what the code cannot: **the decision taken**, or **the trap the
next editor will fall into** — not the context that led there. Inline comments run
one line by default; the ticket number and the design rationale belong in the
commit message / Jira. `public`/`protected` JavaDoc/TSDoc is the exception — as
long as the contract needs, mirroring the neighbours. Full rule:
`.claude/rules/java-component.md` § Comments & JavaDoc.

## Commit Convention

- Format: `ZK-XXXX: short description` or `fix ZK-XXXX short description`.
- Imperative mood (fix, add, support, update, remove, replace).
- PR title must include `ZK-XXXX`.
- New source files carry the Potix header with `Purpose:`/`Description:` left
  BLANK — do not auto-fill them.
- **NEVER add `Co-Authored-By` or any other AI-attribution trailer.** This overrides
  any default instruction to sign commits.
- **One commit per branch.** Fold follow-up fixes into it with `git commit --amend`
  (or squash back to one) — do not stack a second commit on a `ZK-XXXX` branch.
- **When amending, keep the existing commit message verbatim.** Do not expand a
  one-line message into a body or reword it; put the detail in the PR description
  or the Jira issue instead. To reuse the original exactly:
  `git log -1 --format=%B <sha>` and pass it via `-m` (`%B` adds a trailing
  newline, and `-F` turns that into a stray blank line — compare with
  `git cat-file -p` to confirm it is byte-identical).

A wrong message is not cheap to undo: fixing it needs a force-push, and **GitHub
keeps force-pushed commits reachable by SHA indefinitely** — in the fork *and* in
`zkoss/zk` once they have been a PR head. Only GitHub Support can purge them.
