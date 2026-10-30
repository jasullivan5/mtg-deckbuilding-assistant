# Explorer M1 — live Commander discovery

The Explorer is a local browser interface over the application's existing Kotlin discovery. The first Prototype Lab review did not accept M1 as a usable research experience. The revised experience now provides compact controls, grounded Magic explanations, explicit overlapping counts, mechanic browsing and verified printing/face imagery. It is ready for a fresh Prototype Lab review; acceptance remains the user’s decision. Existing mechanical classifications and CLI evidence remain authoritative. See [repository layout](REPOSITORY_LAYOUT.md) for current versus historical evidence.

## Launch on Windows

From the application checkout:

```powershell
.\scripts\explorer.ps1
```

Install Git, JDK 21, Node 22.12+ and pnpm 11.25.0; set JAVA_HOME and put JDK 21 on PATH. The existing sibling engine must be clean on app-stable with the maintained fork as origin. Setup creates that sibling if absent and preserves existing work as described in [engine workflow](ENGINE_WORKFLOW.md). Initial dependency installation needs network access.

The command installs locked frontend dependencies, type-checks/builds the React UI, builds the Kotlin distribution and starts one foreground JVM. Paths are repository-relative and work in this workspace containing spaces and an ampersand. Open **<http://127.0.0.1:8787>** after the terminal prints **Explorer ready**. Catalog loading can take tens of seconds; the service does not claim readiness while it loads.

Stop with **Ctrl+C** in that terminal. No separate frontend server needs cleanup. An occupied port is reported before the build; choose `-Port 8788` if needed. The command rebuilds saved changes on launch; this launch does not provide hot reload. `pnpm dev` alone is a frontend source server, not the supported live application launch.

Windows PowerShell 7/JDK 21 was executed. Other platforms are not claimed tested for the launch script.

## Try three research questions

1. Leave **Judith, the Scourge Diva** and **Our nontoken creatures die**, then **Run discovery**. In **May help**, find **Acolyte of Aclazotz**. Its text, image and explanation show tapping and sacrificing another creature or artifact. For this objective, choose a nontoken creature; an ordinary noncreature artifact does not satisfy the question. Compare **Accursed Centaur**: its sacrifice is required when it enters, and the source is not excluded. Neither finding certifies every interaction.
2. Select **Needs investigation** and find **Agent of Shauku**. It appeared because its ability sacrifices a land as a cost. An ordinary land is not a creature. A land that is also a nontoken creature could satisfy the death condition, but the application has not established an animation effect or verified that interaction. Expand **Ability evidence and technical details** to inspect the retained land filter and unresolved checks.
3. Select **All findings**, clear the name filter and choose a **Mechanical relationship**, such as **Sacrifice** or **Destruction**. Counts above cover the whole search; the count below the filters describes your current view. A selected card stays highlighted and the **Read selected card** link moves keyboard focus to its inspection. For a useful boundary example, choose **Abilities ruled out**, reset the relationship filter and inspect **Murder**: this objective excludes ordinary friendly-target removal by policy, even though it can mechanically affect your creature. This is not a claim of mechanical impossibility.

Change objectives or commanders to execute a new Kotlin search. Selecting **Murder as the commander** remains blocked; ordinary card eligibility and commander selection are different questions. All three existing objectives remain available.

## Reading findings honestly

The browser's categories count physical finding cards and overlap. The run header separately counts objective pathways and unsupported structure records. Its measured population is the eligible authored native catalog plus application extensions, not every Commander-legal Magic card. Unresolved format/identity metadata is excluded before mechanical extraction and retained in the full eligibility evidence.

MATCH means conditional structural potential. UNKNOWN, a nonmatching pathway, an unsupported record, whole-card translation assurance, historical scenario execution and strategic usefulness remain separate. A card can have multiple different pathway verdicts. No emitted pathway is not a proven mechanical negative.

Readable explanations describe the retained Kotlin operation, affected-object filter, costs and verdict separately from uncertainty. Objective-policy exclusions remain distinct from mechanical incompatibility. The explanation layer does not add acceptance rules. Original action, fact, decision, prerequisite, source, eligibility, structural-support and assurance records remain accessible. Authored native text is not independent Oracle/translation certification. The browser can filter the current findings by recorded mechanical relationships. It is not a comprehensive card catalog.

Only **the latest run** is retained in memory. Starting another discovery invalidates earlier run URLs, including those in other tabs. Expired evidence reports a recovery message; rerun the question. Export complete evidence before replacing a run if you need to retain it. Exports are not imported as live discovery.

## Local HTTP contract — schema version 1

All API paths start with `/api/v1`. JSON bodies reject unknown fields; commander names use exact native lookup. Browser input never becomes a shell command or filesystem path.

| Method and path | Request | Response |
| --- | --- | --- |
| GET /health | none | ready state, actual engine SHA/mode, packaged application revision |
| GET /objectives | none | three Objective records: exact id, label, explanation |
| POST /commander | {commander} | unchanged CommanderInspection; HTTP 200 also covers blocked selections |
| POST /discover | {commander, objective} | RunSummary: run ID, fresh structural execution mode/time, revisions, scope/counts, limitations and compact card index |
| GET /runs/{id}/cards/{index} | index from that run's summary | CardEvidence: authored text, exact per-card results/actions/facts/unsupported/support/eligibility/assurance and readable restatements |
| GET /runs/{id}/images?indices=0,1 | 1–40 indexes from the current run | Verified printing/face image records or explicit unavailable results |
| GET /runs/{id}/report | none | streamed LiveDiscovery attachment containing the complete unchanged DiscoveryReport and run provenance |

Card indexes belong to a single run; never reuse them for another run. Summary cards include cards with emitted objective pathways or unsupported records. Other cards/facts and every eligibility record remain in the full report. Name/category/relationship navigation and 24-card UI pages do not narrow the Kotlin search population.

The complete TypeScript consumer and serializable Kotlin DTOs are in `web/src/api.ts` and the DTOs under `src/main/kotlin/server/`. Export metadata includes schemaVersion, execution, runId, startedAt, durationMs, applicationRevision, engineMode and report. The nested report's argentumCommit is the actual packaged engine revision. A +dirty application revision explicitly means its commit alone does not identify all uncommitted source.

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
Push-Location web
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

The fixture browser suite tests UI states using explicitly synthetic transport records; it does not claim live mechanics. The live suite executes real browser → Kotlin queries and writes actual screenshots under ignored `generated/explorer/`. Its mobile check covers keyboard access and horizontal containment at 390 px. CI runs the locked frontend build and fixture browser suite; genuine live browser execution is recorded separately in the initial Explorer delivery receipt.

HTTP parity compares every serialized DiscoveryReport field with fresh CLI output for each objective, using streaming/hashing to avoid extra giant report trees. Selected-card evidence is also compared field-by-field. Boundary tests retain malformed/blocked/error/busy/timeout checks. The existing CLI and frozen expectations are unchanged.

## Verified revision and current limits

Fresh local verification on 2026-10-30 used application **7851ac0f988a7518e096b5e0c43430e9db5aecee** and the clean sibling engine **03869c08665bf476a8b40dacbfbcd501c54a33bd**, mode **app-stable**. Subsequent documentation-only changes do not represent another test execution.

- Full Kotlin verification: **475 tests, 61 suites, zero failures/errors/skips**. This includes the existing CLI/HTTP full-report parity tests, source-bound sacrifice explanation assertions, image identity/cache/fallback tests and image API bounds/expired-run checks. Existing Centaur engine-scenario coverage was freshly executed; Agent and Acolyte's described relationships are source/representation observations, not newly certified gameplay scenarios.
- Frontend: frozen dependency installation, TypeScript/build, **5 synthetic interface browser tests** and **3 genuine live browser tests** passed with Node 24.22.2, pnpm 11.25.0 and installed Chrome. The live exercises covered all three objectives, changed commander, rejected selection, the three sacrifice cases, Murder's policy exclusion, actual loaded Acolyte artwork, category reconciliation, 390px containment and keyboard focus. Desktop/mobile screenshots were inspected. Exhaustive accessibility and other workstation platforms are not claimed tested.
- The supported one-step Windows launcher was executed against that exact saved application revision. Its health response reported the same application and engine SHAs. The temporary verification service is stopped after validation.
- Judith's death search examined **6,111 eligible authored cards** and indexed **5,400 physical finding cards**. Overlapping card categories were **487 MATCH**, **373 UNKNOWN**, **1,763 NO_MATCH** and **4,359 with unsupported records**. Separately, there were **499/410/2,140 MATCH/UNKNOWN/NO_MATCH pathway checks** and **8,702 unsupported records**. The **Sacrifice** relationship view contained **890 cards**, including unresolved and nonqualifying checks; it is not a count of verified creature-sacrifice outlets.
- The complete exported report was **47,801,399 bytes**, including run metadata. Its nested DiscoveryReport matched the pre-revision Judith death report in **every field, with no normalization**. The extractor, policies, eligibility, native definitions and frozen expectations were not changed.
- All three investigated cards returned VERIFIED printing/face associations. Acolyte resolved to printing **99009400-ffa4-40af-8305-78295ae605ac**, Oracle ID **324d2aa8-6dea-4b0d-898d-1e52ed187565**. Image metadata records retain original retrieval times and hashes even when served from cache.

Current local receipts and screenshots are under ignored `generated/explorer/`: `verification.json`, `experience-observation.json`, `report-preservation.json`, `investigation-images.json`, `interface-results.json`, `browser-results.json`, `discovery-overview.png`, `acolyte-discovery.png`, `agent-investigation.png` and `mobile-card-inspection.png`. Source snapshots and their retrieval bindings are retained with the tests. These are fresh execution receipts; they do not promote qualification or resolve historical concerns.

The service still retains only its latest run. Large exports, incomplete authored coverage, unresolved filters/eligibility, unsupported instructions and unverified whole-card translations remain real limits. Explanations deliberately cover recognized operations and a bounded set of costs/filter forms; untranslated restrictions remain explicit in the underlying evidence. No strategic ranking, deck editor, general Oracle parser, animation-combination search or gameplay sandbox was added. No consequential engine decision remains blocked by these three examples; acceptance of the revised research experience is still pending user review.

## Sacrifice findings investigation

Source snapshots and rulings were retrieved from Scryfall on 2026-10-30 for PCY #55, LCI #89 and ONS #123. Raw hashes and URLs are retained in `src/test/resources/cards/image-and-explanation-sources.json`. The source set is a bounded investigation, not an independent whole-card qualification.

| Card | Oracle expectation and authored representation | Existing discovery | Presentation consequence |
| --- | --- | --- | --- |
| Agent of Shauku | Mana plus a land sacrifice pays for a targeted power increase. The authored cost retains IsLand. | UNKNOWN: the application does not resolve IsLand as creature-compatible; policy accepts sacrifice costs only after mechanical checks. | Explain the land sacrifice that caused investigation. An ordinary land does not satisfy the creature objective. A land that is also a nontoken creature could do so, but this run establishes no animation or cross-card interaction. |
| Acolyte of Aclazotz | Tap and sacrifice another creature or artifact. The authored filter retains the OR and excludeSelf. | MATCH under retained conditions: a legal choice can be a nontoken creature; a noncreature artifact or token does not meet the objective. | Show the choice, source exclusion and tap/resource restrictions. This is one OR-filtered sacrifice, not two independently payable costs. |
| Accursed Centaur | On entering, sacrifice a creature. The source is not excluded. The retained ruling says it must sacrifice itself if it is the only creature. | MATCH under retained conditions, mandatory triggered effect rather than an activated sacrifice outlet. | Show the enters trigger, required sacrifice and source eligibility; no repeatable activation is inferred. |

Checked against the official [Comprehensive Rules](https://magic.wizards.com/en/rules), revision 2026-09-25: sacrifice is a controller's battlefield-to-graveyard action (701.21a); death requires reaching the graveyard from the battlefield (700.4); effects can retain earlier card types (205.1b). No engine correction or broader filter-satisfiability change is justified by these three findings. This revision preserves the extractor, policies, report verdicts, source bindings and frozen expectations.

## Printing and face imagery

`CardImages` retrieves only requested visible cards, in batches of at most 40, from Scryfall's collection endpoint. A native Scryfall printing ID, an ID embedded in its Scryfall image URL, or exact set/collector identity is required. No name-only lookup is used. Conflicting known printing IDs, set/collector data, declared Oracle ID, physical layout, complete ordered face names, types or relevant mana costs prevent image admission. Unsupported layouts and missing images receive explicit fallbacks. The image association algorithm is `printing-face-image-v1`; it does not change coverage's physical-identity algorithm or eligibility.

Scryfall's [API guidance](https://scryfall.com/docs/api) was checked on 2026-10-30. Requests identify the application and request JSON; collection batches are spaced at least 600ms apart. Metadata is cached for 24 hours under ignored `generated/explorer/image-metadata/`, revalidated against the current definition on every use, and bound by retrieval time and SHA256. Failure responses are not automatically retried; non-200 responses pause further requests (respecting a bounded Retry-After). Images are loaded directly from validated HTTPS Scryfall image URLs, unaltered, with browser caching, lazy loading, source links, artist credit and Wizards attribution. No image bytes are committed or proxied. Offline or conflicting metadata never substitutes approximate art. Image identity verifies the association, not card mechanics or current rules wording.

## Current evidence-inspection revision checkpoint

Work on `feature/discovery-evidence` starts at cf2b407 after PR #18 was merged. The new assignment is the 2026-10-30 Prototype Lab feedback on understandable discovery and verification. Current work groups actual ability records (including Attrition’s sacrifice, legal nonblack target and destruction), projects existing assurance into scoped clickable labels without promoting qualification, keeps historical executions explicit, and simplifies browsing/commander inspection. Preserve the existing report and frozen records. Before delivery run the new evidence/ability/API tests, adapt and execute frontend and real-browser exercises, inspect desktop/mobile layout, record actual source revisions, and replace this checkpoint with fresh validation. No engine correction is currently required.
