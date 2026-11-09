# Development instructions

Implement the explicitly approved assignment. The user owns goals, priorities, consequential decisions and acceptance. Product Studio is the user and ChatGPT examining direction and evidence; Codex Build implements bounded work and delivers reviewable PRs. Hands-on review is optional when useful; no separate Prototype Lab role or approval ceremony is required. Do not expand scope or merge without authorization.

## Living authority

Live Git and the running application own continuity. Start at [README](README.md#current-state-and-authority) for capabilities and documentation ownership. [Objective direction](docs/OBJECTIVE_MODEL.md) records product decisions and hypotheses; [architecture](docs/ARCHITECTURE.md) describes implementation; [limitations](docs/KNOWN_LIMITATIONS.md) records boundaries. The Primer explains these to users. Retired ChatGPT Project attachments and old Project Instructions are not active dependencies, including where older sibling-fork guidance mentions them. Historical reports retain their original scope and are not new assignments.

## Repositories, environment and Git safety

- This application owns discovery, Commander eligibility, assurance, research diagnostics and the React/local API boundary. [Our maintained Argentum fork](https://github.com/jasullivan5/argentum-engine) owns native definitions, SDK semantics, rules execution and native regressions. Engine feature/fix PRs target `app-stable`; fork `main` tracks upstream. No upstream PR, wholesale import or generic fork-sync into `app-stable` without explicit direction.
- Normal builds use the clean sibling `../argentum-engine` on our fork's `app-stable`. Preserve origin/clean-source safeguards and record the actual engine SHA. There is no separate dependency-pin adoption. Engine acceptance requires native regressions and complete application compatibility against the exact clean candidate; preserve historical evidence and concerns. See [engine workflow](docs/ENGINE_WORKFLOW.md).
- Use a feature/fix branch in the primary checkout (application base `main`, engine base `app-stable`). Worktrees need practical simultaneous isolation, including protecting an active stable checkout. Never reset, clean, force-push or delete unrelated work/history. Before normal worktree removal without force, check modifications, untracked/unpushed work, active processes and consumers; preserve ambiguous checkouts and branches.
- Preserve Kotlin/JDK 21/Gradle, CLI/headless operation and the independent React/TypeScript/Vite interface. Frontend presentation must not reinterpret Oracle rules or implement mechanical acceptance. New objectives, roles/composition algorithms, deck evaluation, hosted infrastructure and unrelated upgrades need assignment. Conceptual direction is not implementation authorization.
- Use `dev` / `dev.ps1`, not root `gradlew test`. Windows launch: `.\scripts\explorer.ps1`; setup: `.\scripts\bootstrap.ps1` or `bash scripts/bootstrap.sh`; focused tests: `.\dev.ps1 test --tests '<class>'`; full application: `.\dev.ps1 test`; Bash gates: `bash scripts/verify.sh fast` / `full` / `catalog`. See [README](README.md) and [environment](docs/CODEX_ENVIRONMENT.md).

## Mechanical correctness and evidence

- Use `Catalog.eligibility` effective metadata for format legality and full color identity. Raw native helpers are diagnostics. Metadata corrections need reviewed physical/source/snapshot/implementation bindings; preserve original metadata and definitions. Identity association is not eligibility approval or mechanical certification; version changed association rules.
- Separate availability, native representability, source fidelity, engine execution, structural possibility, objective-policy relevance and contextual value. MATCH is conditional potential. Preserve UNKNOWN and unsupported structures, along with consequential cost/effect, owner/controller/source/object/zone/target/quantity/timing/branch relationships.
- Extend the existing walker, reusable facts and predicates; keep objective policies separate. Death retains its legacy projection; reanimation, tokens and graveyard state use shared facts. Prefer generic engine corrections to production card-name exceptions. No new Oracle parser or ranking ontology without assignment.
- Use source-first requirements and adversarial execution evidence at the appropriate scope. Never rewrite frozen expectations to pass tests. Author/agent tests are bounded evidence; independent qualification requires separate source- and revision-bound approval through the existing gate. Preserve source/test hashes, provisional status, historical failures and concern history. Merges and rereading do not refresh evidence or resolve concerns.
- Run proportionate checks; report passed, failed and unexecuted results with exact revisions. Shared extraction, eligibility or objective-policy changes require full application tests and controlled unchanged-pool comparisons. Sample agreement is not whole-catalog recall. See [authoring](docs/COMMANDER_AUTHORING.md) and [assurance](docs/TRANSLATION_RELIABILITY.md).

## Visualization fidelity

- Measurements identify population, units, commander/objective/filters, revisions, exclusions, unknowns and observation mode. Cards, faces, pathways, scenarios and tests are different units; overlapping categories cannot be summed. No emitted pathway is not proven NO_MATCH. No invented confidence percentages or universal strength scores.
- Distinguish source expectations, recorded execution, fresh native scenarios and illustrative examples. Never depict fictional states as observed or promote one passed scenario to whole-card certification. Provide readable explanations with access to exact evidence.
- Imagery needs verified physical-card/printing/face identity, attribution and an explicit fallback; never substitute approximate art by name. Engine-client asset reuse needs coupling, provenance and licensing review.

## Delivery and organization

Deliver a small reviewable PR, behavior and limits, actual commands/results and provenance. For user-facing work, include a straightforward launch, useful real-card tasks and actual screenshots when helpful. Implementation completion, merge, user acceptance and independent qualification are distinct. Do not add decorative UI or demand hands-on review for every change.

Use existing behavior folders and root source sets in [repository layout](docs/REPOSITORY_LAYOUT.md). Do not introduce assignment/Phase/Batch source or test trees. Keep runtime metadata in `src/main/resources`, consumed evidence/fixtures in `src/test/resources`, current outputs in ignored `generated/`, and obsolete bulk output in the movable ignored archive. Preserve frozen contents, hashes and qualification when relocating evidence. Builds/tests must not depend on the export. Ignore caches, dependencies, credentials and machine settings; keep real Git history and compact reviewed evidence.
