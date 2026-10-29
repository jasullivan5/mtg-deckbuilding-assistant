# Explorer M1 checkpoint — 2026-10-29

## Resume here

Continue **feature/explorer-m1** in the application checkout. Do not restart the Explorer or restore the old dependency policy. This is an unfinished implementation checkpoint, not a completed/accepted milestone.

- Original preliminary work is preserved at **a35b9b5**.
- **aa0f497** merges application main **b66dd0d** into the same feature branch. Conflicts in .gitignore, cli/build.gradle.kts and gradlew.bat deliberately retain main's corrected versions. Both a35b9b5 and b66dd0d are ancestors.
- Normal engine is the clean sibling **argentum-engine**, branch **app-stable**, currently **e46fab1475ddc6f355e1c8fdc770b12bdc697b0f**. Engine PR #2 and application PR #12 are merged. Do not use the retired nested clone or change the engine.
- No M1 PR has been opened. No M1 acceptance is claimed. No servers are currently running.

Read AGENTS.md, README.md, docs/ARCHITECTURE.md, docs/ENGINE_WORKFLOW.md and docs/EXPLORER_MILESTONES.md. The original assignment in this conversation remains M1 only: one-command Windows launch; live Kotlin inspection/discovery; three existing objectives; usable controls/results/evidence; truthful unsupported/unknown/assurance states; focused HTTP/UI tests and CLI regression; reviewable application PR, no automatic merge.

## Fresh verification already completed

At reconciled source **aa0f497**:

```powershell
.\dev.ps1 test --tests dev.mtg.assistant.ExplorerServerTest :assistant-cli:installDist
```

All **five recovered HTTP tests passed**, including full CLI report/evidence parity for all three objectives, invalid selections, malformed requests, errors, busy/timeout handling. That was the earlier full-report HTTP contract; it is NOT validation of the newer projection below.

After the subsequent frontend changes, these passed:

```text
node node_modules/typescript/bin/tsc --noEmit
node node_modules/vite/bin/vite.js build
```

These are build/type checks, not browser interaction tests.

## Why the preserved foundation is being refined

Measured existing complete reports (generated/handoff/catalog):
- Judith A: 47,802,090 bytes, 3,049 pathways, 8,702 unsupported records, 5,400 finding cards, 487 conditional-match cards.
- Judith B: 48,772,091 bytes, 4,788 finding cards.
- Judith C: 40,112,825 bytes, 4,579 finding cards.

The recovered UI rendered all findings and constructed a complete JSON Blob on every render. Keep its architecture and presentation, but send a compact finding index and load selected-card evidence on demand. Retain complete export; do not prune consequential evidence or reinterpret mechanics. Client-side pages of 40 address the observed thousands of records, with basic verdict/name navigation within the executed findings; this is not M2's comprehensive browser.

## Current source changes (not yet Kotlin-compiled/tested)

- **ExplorerModels.kt**: existing Objective/ResearchRequest/ApiError moved here; RunSummary, CardFinding, CardEvidence and full LiveDiscovery export; StoredRun indexes the unchanged report by card. All original evidence remains in export and selected-card records.
- **ExplorerServer.kt**: retains JDK loopback HTTP server, validation, single exclusive discovery worker and error handling. One latest run retained. New summary/detail/export routes below. Streaming JSON avoids full-report intermediate strings. A deadline prevents publication after timeout; gate stays reserved until work actually ends. Bounded request executor/body sizes/timeouts; static real-path containment.
- **cli/build.gradle.kts**: generates application-build.properties alongside engine metadata, with actual application commit and +dirty when applicable. New property generation is not yet Kotlin/Gradle-validated.
- **Main.kt**: existing explorer dispatch retained; usage line added.
- **explorer/src/main.tsx**: retains original controls/results/inspection design; compact API types, selected-card fetch with cancellation, 40-card pages, basic verdict/name navigation, lazy raw details, native-authored text, complete download URL. Fixes object-URL leak by using server export. Type/build checks passed.
- **explorer/src/style.css**: original style retained plus navigation/pager/authored-text refinements.
- **scripts/explorer.ps1**: still recovered version; needs cleanup. It already calls normal setup/dev wrappers, but has obsolete pinned wording and MTG_APPLICATION_REVISION env logic now superseded by packaged metadata.

### Current intended API v1

- GET /api/v1/health: ready, actual engine SHA/mode, packaged application revision.
- GET /api/v1/objectives: exact three IDs, readable labels/explanations.
- POST /api/v1/commander: {commander}; unchanged CommanderInspection.
- POST /api/v1/discover: {commander, objective}; **RunSummary**, not full LiveDiscovery.
- GET /api/v1/runs/{runId}/cards/{index}: complete CardEvidence for one indexed finding card.
- GET /api/v1/runs/{runId}/report: streamed **LiveDiscovery** envelope with full unchanged DiscoveryReport and run provenance, attachment download.
- New discovery invalidates previous run (410 RUN_EXPIRED for old evidence); only latest report retained.
- Existing 400/403/405/409/413/415/422/500/504 distinctions retained.
- Native MATCH/UNKNOWN/NO_MATCH and unsupported records stay distinct. Summary card indexes include emitted pathways or unsupported records, not every eligible card; all other facts/eligibility remain in full export.

## Immediate next steps, in order

1. **Update ExplorerServerTest.kt to the new response contract.** Its old parity test expects full LiveDiscovery directly from POST discover and will fail. Read RunSummary, fetch its report endpoint, verify underlying full report vs CLI (prefer stream decoding/hashing to avoid extra 40–49 MB strings), and test selected-card evidence completeness, expired IDs/bad indexes, all three objectives and changed commander. Keep the recovered validation/busy/timeout/error tests. Add late-timeout publication exclusion if practical.
2. Run focused Kotlin test/build through dev.ps1. Fix actual compile/runtime errors before further UI work. The current API/model/property additions have not been compiled yet.
3. Reconcile scripts/explorer.ps1 with sibling setup and packaged provenance. Check Node/pnpm/JDK prerequisites and exit codes; one foreground Kotlin process serving built Vite assets; readiness, occupied port and Ctrl+C shutdown. Avoid hardcoded workstation paths. Don't require discovery JSON preparation.
4. Add reproducible lightweight browser tests. Installed workspace runtime has **playwright 1.62.1**, but **@playwright/test is not yet added**. Existing frontend has React 19.2.7, TypeScript 6.0.3, Vite 8.1.0 and pnpm lock; Node 24.22.1/pnpm 11.25.0 available here. Add exact test dependency + lockfile, not node_modules. Test mocked UI states honestly as fixtures, and separately execute real browser → Kotlin flows.
5. Actually launch on Windows from the spaces-and-ampersand workspace; measure readiness and summary/detail sizes/query duration. Exercise Judith + at least two objectives, change to Oviya/Krenko, inspect a real result's prerequisites, and Murder/missing commander feedback. Verify all objective IDs selectable, no silent fallback, loading, errors, empty/unsupported and stale evidence behavior. Inspect actual screenshot/mobile/keyboard flow.
6. Run full application suite/affected checks and preserved CLI. Backend mechanics/policies/native engine were not changed; do not modify frozen records. Avoid rebuilding installDist concurrently with live CLI/catalog checks (earlier migration proved that can replace a JAR under a running process).
7. Update README, architecture, M1 API/launch documentation and milestone review state to **ready for Prototype Lab** only when demonstrated; never accepted automatically. Commit/push feature branch, create/attach focused application PR, no merge.
8. Deliver exact Windows launch, prerequisites/shutdown, 2–3 actual guided exercises, actual source/engine revisions and fresh test/performance evidence. Remain within M1.

## Constraints and known review points

- Source API worker/projection refinements still need validation. Do not claim recovered tests cover those changes.
- Initial compact finding index may still be ~MB; measure, don't invent infrastructure.
- Preserve structural potential vs resolution/translation assurance/strategic value. No imagery association loaded; honest fallback. No rankings/confidence scores.
- Full export is deliberately large. Do not commit generated reports.
- A selected-card fetch can return 410 when another tab starts discovery; show understandable recovery.
- Direct server uses packaged application provenance, so it must be rebuilt after source changes.
- No subagent delegation is authorized. Use primary agent.
- Agent usage was 88% consumed in the five-hour window at checkpoint creation; user requested a durable checkpoint if the limit is reached. This is not a request to discard work or restart.
