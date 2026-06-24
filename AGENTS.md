# AGENTS.md

AI coding agent instructions for the **polyconf-parser** project.

## Obsidian Memory

This project uses an Obsidian vault (`polyconf-parser`) for cross-session memory.
On session start, read project context, preferences, and governance documents:

```bash
obsidian vault="polyconf-parser" read path="preferences.md"
obsidian vault="polyconf-parser" read path="Decisions/2026-06-24 - Philosophy.md"
obsidian vault="polyconf-parser" read path="Decisions/2026-06-24 - Limitations.md"
obsidian vault="polyconf-parser" read path="Decisions/2026-06-24 - Policies.md"
```

### During Session

- All documentation must be managed within Obsidian. Do not leave any documentation within the project.
- Write all documentation within english.

- **Decision made** → create `Decisions/{title}.md`
- **Task completed/created** → update `Tasks/active.md`

### Session End

- Update `Tasks/active.md`, `Tasks/completed.md` and `Decisions/{title}.md`
- Rebuild `_Index.md`

## Git

- Never add `Co-authored-by` trailer to commits.
