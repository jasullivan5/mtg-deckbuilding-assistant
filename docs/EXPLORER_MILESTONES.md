# Commander Discovery Explorer — Milestones and Development Rhythm

**Status:** Working product plan; not authorization to implement all milestones  
**Home:** `mtg-deckbuilding-assistant/docs/EXPLORER_MILESTONES.md`  
**Ownership:** Product Studio (user + ChatGPT) sets experience goals and accepts direction; Codex Build implements approved increments; Prototype Lab reviews what actually runs.

## 1. Purpose and first journey

Build an engaging research interface for the existing Commander discovery application. The Explorer should help a player investigate **why a card may matter for a chosen objective**, what must be true for its mechanics to contribute, and where the application's knowledge stops. It is a research instrument, not a general card ranking or deck recommendation engine.

The first coherent journey is:

1. Select and inspect a **supported** commander.
2. Choose one of the application's **existing three** discovery objectives.
3. Run **genuine** discovery through the Kotlin application using the adopted Argentum revision.
4. Browse the returned cards and pathways; inspect a card in readable MTG terms.
5. Expand prerequisites, decisions, source identity, translation status, unsupported structures, and evidence.
6. Understand the run's scope, limitations, and revision provenance rather than mistaking a structural match for a verified or valuable inclusion.

The Technology Audit 001 read-only React viewer is a useful design/integration reference, **not** an integrated live application or an accepted Explorer milestone. Reuse selectively where appropriate. M1 now adds a live React UI and local Kotlin adapter, preserving the CLI and diagnostics. See [the runnable delivery](EXPLORER_M1.md); user acceptance remains pending.

## 2. Sequenced experience milestones

The order below reflects learning dependencies, **not** a promise of dates, final specifications, or blanket authorization. Each milestone can be split into small reviewable increments. A milestone is accepted only after the user has tried its meaningful end-to-end experience.

| Milestone | What the user gains | Minimum useful demonstration | Deliberately deferred |
|---|---|---|---|
| **M1 — Live discovery foundation** | Start the local Explorer, choose a supported commander and objective, run actual discovery, and inspect at least one result and one meaningful limitation | Browser → bounded local Kotlin integration → existing discovery logic; parity with CLI on representative input; clear errors/unknowns; straightforward Windows launch | Comprehensive card art, advanced charts, arbitrary objectives, gameplay |
| **M2 — Card-first exploration** | A compelling card grid/list, identity-safe imagery, filtering/navigation, and a readable **why this card appeared** detail view | Verified card/printing/face imagery or honest fallback; accessible controls; explanation and expandable prerequisites/raw evidence; useful result filtering | Deck editing, strategic strength ratings, general dependency graph |
| **M3 — Evidence and visual exploration** | Understand the population behind results and investigate surprising matches, omissions, unsupported mechanics and assurance concerns | At least one useful interactive chart/filter with drill-down to contributing records; scoped counts, exclusions, unknowns and revisions; separate source/translation/execution/discovery claims | Whole-deck evaluation and unsupported confidence percentages |
| **M4 — Scenario inspection (conditional)** | Inspect actual mechanics through recorded scenarios, including decisions, events and expected-vs-observed outcomes | Reproducible, revision-bound **recorded** engine scenario viewer; no fabricated states; clear differentiation from fresh execution | Live configurable sandbox, arbitrary game-state editing, multiplayer |

**Decision point after M3:** Try the Explorer and decide whether scenario inspection is the most useful next research experience. M4 is a candidate, not an automatic phase. The UI should ultimately support deeper strategic questions—dependencies, substitutes and contextual deck value—but those need independent research and authorization rather than being smuggled into these milestones.

### M1 delivery awaiting Prototype Lab

The authorized M1 assignment resumes the preserved `feature/explorer-m1` foundation after reconciliation with the sibling app-stable integration. It delivers the live browser → Kotlin slice, existing objectives, inspectable evidence and Windows launch described in [Explorer M1](EXPLORER_M1.md). Verification remains separate from user acceptance. No M2 or later milestone has begun.
## 3. How increments are proposed, built and accepted

**One approved task at a time.** Product Studio maintains the direction; Codex may sequence ordinary technical subtasks within an approved increment, but must not execute subsequent milestones merely because they are written here. Prefer a single focused PR per meaningful increment; use more than one only when there is a concrete isolation or review benefit. Avoid a sprawling PR that implements multiple future milestones.

For each increment:

1. **Choose an observable question.** State what a user can do after the change, which real MTG examples demonstrate it, and what misleading/negative/unknown case should be visible. Define the smallest defensible acceptance boundary and explicitly excluded work. A brief is enough; avoid ceremony.
2. **Inspect live Git and implement.** Codex starts from current application `main`, checks `AGENTS.md`, architecture, sibling engine revision, relevant code/tests, local changes and known limits; makes routine technical choices; keeps the existing Kotlin/CLI mechanics authoritative; implements on a feature branch. Fork-only native engine corrections require a separate scoped decision and native/application compatibility checks before app-stable acceptance.
3. **Test at the right layers.** Use focused Kotlin parity/integration tests, frontend component/browser tests, error/unknown cases, and representative live interactions. Preserve exact source and engine revisions, source/data provenance, actual commands, failures and skipped checks. Test claims must not outgrow their evidence.
4. **Deliver something to try.** Provide an easy local Windows setup/launch, **what can I do now?**, two or three guided actions (one should expose a real limitation), and screenshots only when captured from the actual running build. Include the PR and concise known issues. No automatic merges.
5. **Prototype Lab review.** The user runs the app and judges navigation, clarity, usefulness, appearance and surprising results. ChatGPT helps separate UI misunderstandings, projection bugs, mechanical/translation errors, missing source data and strategic disagreement. This is **product acceptance**, not independent translation certification.
6. **Accept, revise, or redirect.** If accepted, the user may merge the PR; update this plan's milestone status and the next concrete question in Git. If not accepted, assign a bounded revision. Only then authorize the next increment. Backend-only corrective investigations are permitted when they are the cheapest adequate way to resolve a concrete obstacle.

**Expected review cadence:** Favor reviewable experiences that can be tried after each meaningful increment. Do not wait for the entire Explorer to be completed before the user sees it. Do not create a new planning exercise after every routine fix; adjust the next increment based on actual experience.

## 4. Quality boundaries that apply to every milestone

- **Mechanics remain in Kotlin and recorded Argentum builds.** Frontend code renders application-owned results; it must not implement a competing Oracle parser, rules engine, Commander legality policy, or mechanical acceptance gate. Maintain CLI/headless operation.
- **Eligibility first.** Commander legality and full color identity precede candidate discovery. Unknown eligibility, unsupported commander configurations, missing native definitions and unsupported actions are distinct conditions; don't silently convert them into `NO_MATCH`.
- **Evidence is layered.** Distinguish identity/source → representability → translation fidelity → engine execution → structural discovery → objective relevance → contextual deck value. `MATCH` is conditional structural possibility, not resolved play, card certification or a deck recommendation.
- **Visual truth is mandatory.** A chart identifies its measured population, unit (distinct cards, faces, pathways, tests, etc.), filters, objective/commander, exclusions, missing/unknown categories and relevant revisions. Meaningful chart categories should link to their contributing records. No pathway emitted does not mean proven nonmatch.
- **Imagery is identity-safe.** Images must correspond to a verified card, printing and face; show a fallback rather than guess. Observe image-provider terms, attribution and caching limits.
- **Execution modes are named accurately.** Imported read-only report, live discovery, historical recorded scenario and fresh live engine scenario are different things. Do not invent gameplay state, progress measurements, coverage percentages or confidence scores.
- **Approachable before exhaustive.** Show readable MTG explanations first, with optional deeper evidence, raw native structures, exact prerequisites and uncertainty. Accessibility, keyboard operation, clear failures and responsive navigation count as research quality.
- **Preserve engine stewardship.** Application and independent engine development repositories remain siblings; the application normally uses the clean sibling engine on app-stable and records its actual revision. Do not merge upstream or alter assurance records to make a UI look successful.
- **Stay local and narrow.** Initially use a loopback-only application-owned integration, bounded requests and clear errors. No accounts, hosted server, databases, deck editor, multiplayer, general sandbox or packaging without a demonstrated need and approval.

## 5. Repository and workstation conventions

Work from the user's existing Windows development workspace containing **both** repositories. Discover actual local paths; do not assume directory names or commit machine-specific absolute paths. Paths may contain spaces and `&`, so quote them correctly. Use ordinary feature branches in the primary checkout; preserve dirty, untracked, local-only and worktree changes. Do not reset/clean or restructure the workspace as a convenience. Keep package lockfiles and reproducible scripts, not `node_modules`, generated large reports or local caches.

Aim for **one practical launch command** from the application repo (or one documented script that orchestrates frontend and backend) once a live increment is delivered. Make failures and required prerequisites legible. Windows execution is essential for the first increment; do not claim other platforms were tested when they were not.

Current code, `AGENTS.md`, `docs/ARCHITECTURE.md`, tests and the recorded engine revision are authoritative for implementation. This document tracks *user-facing direction and review state*, not exact APIs, current test counts or pinned SHAs. Architectural changes with meaningful tradeoffs return to Product Studio for a decision.

## 6. Lightweight status and decision log

Keep this section brief and record demonstrated readiness separately from acceptance; mark acceptance only **after** user review. No dates or completion claims without observed evidence.

| Milestone | Status | Next product decision |
|---|---|---|
| M1 — Live discovery foundation | **Ready for Prototype Lab; acceptance pending** | Try the live M1 delivery and assess conditional explanations, evidence and limitations |
| M2 — Card-first exploration | **Not started** | Reassess priorities after trying M1 |
| M3 — Evidence and visualization | **Not started** | Choose charts/questions from actual Explorer shortcomings |
| M4 — Scenario inspection | **Conditional / not started** | Reconsider after the earlier Explorer experiences |

Update only decisions that alter the work: *accepted experience, observed gap, changed milestone, or next authorized increment*. Keep detailed technical diagnostics, test receipts and PR history in the corresponding repository records instead of duplicating them here.
