# Explorer M1 — live Commander discovery

M1 is a local browser interface over the application's existing Kotlin discovery. It is ready for Prototype Lab only after the checks recorded in the PR; user acceptance remains separate. The preserved foundation at a35b9b5 was reconciled with current main in aa0f497 and extended on feature/explorer-m1.

## Launch on Windows

From the application checkout:

```powershell
.\scripts\explorer.ps1
```

Install Git, JDK 21, Node 22.12+ and pnpm 11.25.0; set JAVA_HOME and put JDK 21 on PATH. The existing sibling engine must be clean on app-stable with the maintained fork as origin. Setup creates that sibling if absent and preserves existing work as described in [engine workflow](ENGINE_WORKFLOW.md). Initial dependency installation needs network access.

The command installs locked frontend dependencies, type-checks/builds the React UI, builds the Kotlin distribution and starts one foreground JVM. Paths are repository-relative and work in this workspace containing spaces and an ampersand. Open **http://127.0.0.1:8787** after the terminal prints **Explorer ready**. Catalog loading can take tens of seconds; the service does not claim readiness while it loads.

Stop with **Ctrl+C** in that terminal. No separate frontend server needs cleanup. An occupied port is reported before the build; choose `-Port 8788` if needed. The command rebuilds saved changes on launch; this M1 launch does not provide hot reload. `pnpm dev` alone is a frontend source server, not the supported live application launch.

Windows PowerShell 7/JDK 21 was executed. Other platforms are not claimed tested for the launch script.

## Try three research questions

1. Leave **Judith, the Scourge Diva**, select **Create creature tokens**, and run discovery. Find **Bitterblossom** within the findings and select it. Inspect the conditional creation pathway, what must be true, the original prerequisites/decisions and its unsupported record. Authored card text is displayed with its provenance caveat; no guessed image is substituted.
2. Change to **Return our creatures** and run again. The browser executes a new Kotlin query, rather than reusing the token report. Alternatively change the commander to **Oviya Pashiri, Sage Lifecrafter** and run token discovery; the eligible identity and findings change. All three existing objective identities are selectable, including **Our nontoken creatures die**.
3. Enter **Murder** and run. Inspection blocks this ordinary noncommander instead of silently selecting Judith. A nonexistent exact name produces missing-definition feedback, which does not establish that a Magic card is nonexistent.

The first useful Prototype Lab question is: **Can you explain why Bitterblossom appeared, what remains conditional, and what its unsupported record means without treating it as a recommendation or a certified translation?**

## Reading findings honestly

The browser's categories count physical finding cards and overlap. The run header separately counts objective pathways and unsupported structure records. Its measured population is the eligible authored native catalog plus application extensions, not every Commander-legal Magic card. Unresolved format/identity metadata is excluded before mechanical extraction and retained in the full eligibility evidence.

MATCH means conditional structural potential. UNKNOWN, a nonmatching pathway, an unsupported record, whole-card translation assurance, historical scenario execution and strategic usefulness remain separate. A card can have multiple different pathway verdicts. No emitted pathway is not a proven mechanical negative.

Readable explanations restate existing Kotlin objectives and verdicts; they do not add mechanical acceptance rules. Original action, fact, decision, prerequisite, source, eligibility, structural-support and assurance records remain accessible. Authored native text is not independent Oracle/translation certification. Imagery and comprehensive card browsing remain deferred.

Only **the latest run** is retained in memory. Starting another discovery invalidates earlier run URLs, including those in other tabs. Expired evidence reports a recovery message; rerun the question. Export complete evidence before replacing a run if you need to retain it. Exports are not imported as live discovery.

## Local HTTP contract — schema version 1

All API paths start with `/api/v1`. JSON bodies reject unknown fields; commander names use exact native lookup. Browser input never becomes a shell command or filesystem path.

| Method and path | Request | Response |
|---|---|---|
| GET /health | none | ready state, actual engine SHA/mode, packaged application revision |
| GET /objectives | none | three Objective records: exact id, label, explanation |
| POST /commander | {commander} | unchanged CommanderInspection; HTTP 200 also covers blocked selections |
| POST /discover | {commander, objective} | RunSummary: run ID, fresh structural execution mode/time, revisions, scope/counts, limitations and compact card index |
| GET /runs/{id}/cards/{index} | index from that run's summary | CardEvidence: authored text, exact per-card results/actions/facts/unsupported/support/eligibility/assurance and readable restatements |
| GET /runs/{id}/report | none | streamed LiveDiscovery attachment containing the complete unchanged DiscoveryReport and run provenance |

Card indexes belong to a single run; never reuse them for another run. Summary cards include cards with emitted objective pathways or unsupported records. Other cards/facts and every eligibility record remain in the full report. Alphabetical name/category navigation and 40-card UI pages do not narrow the Kotlin search population.

The complete TypeScript consumer and serializable Kotlin DTOs are in `explorer/src/main.tsx` and `cli/src/main/kotlin/dev/mtg/assistant/explorer/ExplorerModels.kt`. Export metadata includes schemaVersion, execution, runId, startedAt, durationMs, applicationRevision, engineMode and report. The nested report's argentumCommit is the actual packaged engine revision. A +dirty application revision explicitly means its commit alone does not identify all uncommitted source.

Errors use `{code, message, inspection?}`:
- 400 invalid JSON/name/objective; 403 foreign Origin/Host; 405 unsupported methods on research/evidence routes.
- 409 busy; 410 expired run; 413 body over 4096 bytes; 415 wrong content type.
- 422 blocked discovery selection with its inspection; 500 internal error (details in terminal); 504 execution timeout.
- 404 unknown route or card index. Health/objective requests with an unsupported method are also reported as an unknown route/method.

The service binds IPv4 loopback only. It allows one research worker, with no queued expensive research. Four HTTP workers and a bounded queue handle transport. Research responses have a 120-second limit. A timed-out extraction retains the worker reservation until it actually finishes; late discovery results are not published. Restart the service if it cannot recover. Request/response time limits and the JVM's 1 GB heap also bound resources. Static assets are restricted to the configured build directory, including resolved symlink containment. No permissive CORS is enabled.

## Verification and reproduction

```powershell
.\dev.ps1 test --tests dev.mtg.assistant.ExplorerServerTest
.\dev.ps1 test
.\dev.ps1 installDist
Push-Location explorer
pnpm install --frozen-lockfile
pnpm build
# Installed Chrome on this workstation:
$env:EXPLORER_BROWSER_CHANNEL = 'chrome'
pnpm test:ui
# With the real Explorer running in another terminal:
$env:EXPLORER_URL = 'http://127.0.0.1:8787'
pnpm test:live
Pop-Location
```

The fixture browser suite tests UI states using explicitly synthetic transport records; it does not claim live mechanics. The live suite executes real browser → Kotlin queries and writes actual screenshots under ignored `generated/explorer/`. Its mobile check covers keyboard access and horizontal containment at 390 px. CI runs the locked frontend build and fixture browser suite; genuine live browser execution is recorded separately in the M1 delivery receipt.

HTTP parity compares every serialized DiscoveryReport field with fresh CLI output for each objective, using streaming/hashing to avoid extra giant report trees. Selected-card evidence is also compared field-by-field. Boundary tests retain malformed/blocked/error/busy/timeout checks. The existing CLI and frozen expectations are unchanged.

## Observed limits

The original full Judith reports were 40–49 MB and contained roughly 4,600–5,400 finding cards. The index/detail approach addresses that measured cost. An early warm token query measured 874 KB summary, 11.6 KB Bitterblossom detail and 4.8 seconds native discovery; readable explanations were refined afterward, so use the final receipt for final measurements. Full exports remain large and are deliberately streamed on request.

This is structural research over available authored definitions. It is not exhaustive card coverage, a deck editor, strategic scoring, new objectives, scenario execution UI or independently qualified card translation. Source-first concerns and provisional/blocked assurance remain visible.

