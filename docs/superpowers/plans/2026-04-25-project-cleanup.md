# Project Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize the project into a cleaner professional folder structure while keeping behavior and file names as close to the original project as possible.

**Architecture:** Keep the current Java classes unchanged except for relative file paths that now point to a dedicated `data/` directory. Preserve `web/` as the single frontend source, move Java source into `src/`, keep `bin/` for generated `.class` output, and remove duplicate generated or copied files from the project root.

**Tech Stack:** Java, plain text data files, static HTML/CSS/JavaScript

---

### Task 1: Prepare the source and data paths

**Files:**
- Modify: `Main.java`
- Modify: `WebServer.java`

- [ ] Update both Java entry points to load runtime files from `data/`.
- [ ] Keep `web/` as the static frontend root so the existing browser app continues to work.

### Task 2: Reorganize the repository contents

**Files:**
- Create: `src/`
- Create: `data/`
- Move: `Account.java`, `BankDataStore.java`, `Main.java`, `WebServer.java`
- Move: `Users.txt`, `Accounts.txt`, `Transaction.txt`
- Remove: duplicate root frontend files, root `.class` files, stale files inside `bin/`

- [ ] Move Java source into `src/`.
- [ ] Move runtime text files into `data/`.
- [ ] Delete duplicate frontend files from the root because `web/` is the real frontend source.
- [ ] Clear generated output so `bin/` is only used for compilation output.

### Task 3: Refresh project documentation

**Files:**
- Modify: `.gitignore`
- Modify: `README.md`

- [ ] Update ignore rules for generated output and the new `data/` location.
- [ ] Update project structure and run instructions to match the reorganized layout.

### Task 4: Verify the cleanup

**Files:**
- Verify: `src/*.java`
- Verify: `bin/`

- [ ] Compile the project with `javac -d bin src\\*.java`.
- [ ] Confirm the source compiles after the folder changes.
