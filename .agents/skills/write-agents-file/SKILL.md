---
name: write-agents-file
description: Interactively interview the user to design, write, or update AGENTS.md files for a project, ensuring strict adherence to the progressive disclosure and core section standards (Commands, Boundaries, Structure, Style, Testing, Git, Data).
---

# Write Agents File Skill

This skill guides the agent in interviewing the user, reviewing the codebase, and authoring or updating `AGENTS.md` files (at the root and submodule levels) to ensure they are high-quality, actionable, and strictly adhere to the agent harness standards.

## Usage Guidelines

### 1. Codebase & Context Review (Before Interviewing)
- Explore the codebase to understand the overarching structure (monorepo vs single project, frameworks, languages, databases).
- Identify existing `AGENTS.md` files at the root and in submodules (`.agents/AGENTS.md`).
- Read the existing files to understand the current rules and what might need compacting or updating based on user intent.

### 2. Relentless Requirement Gathering (The Interview Phase)
- Ask the user questions **one at a time** to nail down the specifics for the `AGENTS.md` files. Do not overwhelm them with a wall of questions.
- For each question, provide your recommended answer or technical suggestion based on your codebase review.
- Proactively probe the following dimensions (if not already clear from existing files):
  - **Hierarchy:** Should there be one root file, or are submodule overrides needed (e.g. backend vs frontend vs data)?
  - **Commands:** What are the exact `make`, `npm`, or `pytest` commands required to run, test, and build the project?
  - **Boundaries (Three-tier):** 
    - *Always do* (e.g., specific type enforcements).
    - *Ask first* (e.g., modifying DB schemas, adding dependencies).
    - *Never do* (e.g., commit secrets, PII constraints).
  - **Project Structure:** What are the key directories and their explicit purposes?
  - **Code Style:** What are the non-negotiable style rules? (Gather executable snippets, not prose).
  - **Testing & Git Workflow:** What is the testing framework, coverage threshold, and PR/Branching strategy?
  - **Data Sections (If applicable):** What are the certified tables, prohibited tables, PII columns, and core business rules?

### 3. File Creation & Refinement
- After gathering requirements, draft the `AGENTS.md` file(s).
- Ensure the files follow the strict standards:
  - **Under 150 lines** per file.
  - **No generic prose** (e.g., "write clean code"). Use exact executable commands and code snippets.
  - **Progressive Disclosure:** Submodule `AGENTS.md` files must override and contain only the module-specific context, while the root file contains global orchestrations.
- Write or update the files in their respective `.agents/AGENTS.md` paths.

### 4. Review & Finalize
- Present the generated/updated `AGENTS.md` files to the user for final approval.
- Ask if they would like to open a Pull Request with these changes. If so, guide them through the PR creation (or use `gh pr create` if authorized).
