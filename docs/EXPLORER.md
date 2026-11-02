# Explorer M1 — Card Discovery

The Explorer is a local browser interface over the application's existing Kotlin discovery. The first Prototype Lab review did not accept M1 as a usable research experience. The revised experience shows card text once, marks reliably associated abilities, adds brief objective-specific notes, and presents compact evidence fields with optional technical records. Mechanic browsing and verified printing/face imagery are preserved. It is ready for a fresh Prototype Lab review; acceptance remains the user’s decision. Existing mechanical classifications and CLI evidence remain authoritative. See [repository layout](REPOSITORY_LAYOUT.md) for current versus historical evidence.

## Launch on Windows

From the application checkout:

```powershell
.\scripts\explorer.ps1
```

Install Git, JDK 21, Node 22.12+ and pnpm 11.25.0; set JAVA_HOME and put JDK 21 on PATH. The existing sibling engine must be clean on app-stable with the maintained fork as origin. Setup creates that sibling if absent and preserves existing work as described in [engine workflow](ENGINE_WORKFLOW.md). Initial dependency installation needs network access.

The command installs locked frontend dependencies, type-checks/builds the React UI, builds the Kotlin distribution and starts one foreground JVM. Paths are repository-relative and work in this workspace containing spaces and an ampersand. Open **<http://127.0.0.1:8787>** after the terminal prints **Explorer ready**. Catalog loading can take tens of seconds; the service does not claim readiness while it loads.

Stop with **Ctrl+C** in that terminal. No separate frontend server needs cleanup. An occupied port is reported before the build; choose `-Port 8788` if needed. The command rebuilds saved changes on launch; this launch does not provide hot reload. `pnpm dev` alone is a frontend source server, not the supported live application launch.

Windows PowerShell 7/JDK 21 was executed. Other platforms are not claimed tested for the launch script.

## Try three inspections

1. Leave **Judith, the Scourge Diva** and **My nontoken creatures die** selected. Open **About commander** and **About objective** without searching. Both begin collapsed. Judith shows labeled properties and one copy of her rules text, with her relevant trigger marked. Click **Rules comparison**: the recorded author is an unnamed AI agent, the current card-review format does not separately capture the comparison reviewer/date, and the bound source is retained Scryfall Oracle data. **Engine tests** identifies the different engine version used by the earlier results.
2. **Find cards**, then use **Card name** to inspect **Accursed Centaur** and **Attrition**. Centaur adds only the nontoken requirement and mandatory-sacrifice note to its printed ability. Attrition retains one complete printed activation plus the important legal nonblack-target requirement and the actual unresolved interpretation. Open **Discovery** for the actual mechanic, why it qualified, the conditional death outcome and assumed game conditions. Full objective-policy decisions and cost/effect projections remain under **Technical investigation**.
3. Compare **Judith** (a response to an earlier death) with **Tragic Slip** (death-enhanced removal). Under **All findings**, inspect **Murder**: ordinary friendly-target removal is deliberately excluded. **Midnight Reaper** has a negative player-damage check, while its draw payoff is outside this objective; **Pitiless Plunderer** has no indexed finding. Switch to **Create creature tokens** to find **Doomed Dissenter**. For an unresolved case, inspect **Agent of Shauku** under **Needs interpretation**; compare it with **Acolyte of Aclazotz** under **Identified mechanics**. Acolyte explains which sacrifice choice advances the objective; Agent retains the unresolved land/creature restriction. For a real historical concern, choose **All findings** and inspect **Tatterkite**. Its open historical issue is distinct from absent reviews/tests. Expand its investigation details for the exact original failure and binding history.

Opening commander information after searching preserves the results. Change a commander/objective to start a new search. **Murder** remains a blocked commander selection. The three technical objective IDs are unchanged. Search population, full-identity eligibility, exclusions and implementation coverage remain under the findings' research details. General evidence methodology lives in the shared **About the evidence checks** disclosure rather than repeating across cards.

## Reading findings honestly

Ordinary browsing prioritizes **Identified mechanics**: cards with at least one accepted contribution stay there even when another instruction is unresolved or excluded. **Needs interpretation** contains cards with UNKNOWN results and no MATCH. **All findings** includes every indexed card; it is not a claim that all those cards contribute. Missing rules review, gameplay testing and independent approval are evidence gaps, not extra mechanical-uncertainty categories.

**Research and diagnostic views** retains overlapping accepted/unresolved/nonqualifying/unread card counts. Those count distinct physical cards and must not be summed. Pathway checks and unsupported instruction records have separate denominators in the optional run-scope table. Scope is the eligible authored catalog and application extensions, not all Commander-legal Magic cards; unresolved eligibility remains excluded and inspectable.

Card rules appear once beside the image, with labeled mana cost and type. A relevant marker has the accessible name **Relevant to this objective**; unresolved relevance uses a separate marker. Paragraph links require an unambiguous, complete text match to a corresponding stored-action description after whitespace/case/literal self-name normalization. This is display association, not parsing Oracle text for facts or verdicts. Duplicated, changed or back-face text is not assigned a guessed front-text link. Unmapped abilities retain their records and, when needed, a chooser. Acolyte's combined life-effect sentence currently uses this unmarked fallback; its sacrifice discovery and objective-specific choice note remain available.

The default body adds only conditions that bridge printed text to the objective or materially affect interpretation. It omits repeated ability headings, cost/effect lists, generic caveats and card-list explanation paragraphs. Original projections, choices, exclusions and unsupported instructions remain in **Technical investigation**. The visible “Read selected card” link is removed; activating a card with the keyboard focuses its inspector. Selecting a precisely linked printed ability switches the associated evidence.

Only **the latest run** is retained in memory. Starting another discovery invalidates earlier run URLs, including those in other tabs. Expired evidence reports a recovery message; rerun the question. Export complete evidence before replacing a run if you need to retain it. Exports are not imported as live discovery.

## Local HTTP contract — schema version 1

All API paths start with `/api/v1`. JSON bodies reject unknown fields; commander names use exact native lookup. Browser input never becomes a shell command or filesystem path.

| Method and path | Request | Response |
| --- | --- | --- |
| GET /health | none | ready state, actual engine SHA/mode, packaged application revision |
| GET /objectives | none | three Objective records: exact id, label, legacy explanation, concise capability bullets and optional coverage note |
| POST /commander | {commander} | unchanged CommanderInspection; HTTP 200 also covers blocked selections |
| POST /commander-view | {commander, objective} | original inspection plus verified image/fallback, readable card text, color identity, bounded objective connection and applicable scoped evidence; does not run whole-catalog discovery |
| POST /discover | {commander, objective} | RunSummary: run ID, fresh structural execution mode/time, revisions, scope/counts, limitations and compact card index |
| GET /runs/{id}/cards/{index} | index from that run's summary | CardEvidence: authored text, exact per-card results/actions/facts/unsupported/support/eligibility/assurance readable restatements, preserved whole-ability projections, concise objective notes, printed-text links and structured evidence fields |
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

The fixture browser suite tests UI states using explicitly synthetic transport records; it does not claim live mechanics. The live suite executes real browser → Kotlin queries and writes actual screenshots under ignored `generated/explorer/`. Its mobile check covers keyboard access and horizontal containment at 390 px. CI runs the locked frontend build and fixture browser suite; genuine live browser execution is recorded separately in the local verification below.

HTTP parity compares every serialized DiscoveryReport field with fresh CLI output for each objective, using streaming/hashing to avoid extra giant report trees. Selected-card evidence is also compared field-by-field. Boundary tests retain malformed/blocked/error/busy/timeout checks. The existing CLI and frozen expectations are unchanged.

## Evidence labels and scope

These are presentation states over existing discovery and assurance records, not a new qualification framework:

| Label | What a supported claim means | Important boundary |
| --- | --- | --- |
| Discovery | The stated part of this ability met the existing objective checks. | A recognized sacrifice cost does not verify its targets, other effects, rules fidelity or execution. UNKNOWN remains an unresolved relationship. |
| Rules comparison | The registered record reports comparison with retained card sources. | Author/source-led review is not independent rules certification. A card-level record does not supply per-ability coverage. Verified artwork is separate. |
| Engine tests | Registered bounded execution outcomes exist. | A scenario filename alone is incomplete evidence. Recorded passes/failures retain their engine revision; a compatible historical pass is not a fresh run. |
| Independent verification | The existing exact-bundle qualification gate accepted an explicit independent approval. | Author tests, source comparison and structural matches cannot create this status. Approval scope is its bound requirements and cases. |

Each tile names a category and a specific result, such as **Creature sacrifice cost identified** or **Affected creature type unresolved**. Clicking it opens concise fields rather than another paragraph restating the tile. Missing records stay neutral and do not acquire invented metadata; their technical inspector still exposes available scope and records. Each category has consistent tooltip/accessibility help, with general methodology in one shared disclosure. Exact scope, original explanations, hashes, versions, failure history and references remain below the fields.

Five presentation states preserve established, absent, incomplete, known-problem and outdated evidence. Historical passes identify a different engine version; stale failures retain the earlier outcome without asserting it as current. Only actual concerns are prominent. Missing independent verification does not block discovery or require the user to review the catalog. No review/qualification pipeline was added.

### Reviewer, source, date and ability-coverage investigation

The existing TranslationEvidence contract is card-definition-bound. It has an `author` and source/test/requirement references, but no structured comparison-reviewer, comparison-date or per-ability review-coverage fields. Its independent-approval record does separately hold a reviewer/date. The current author's identifier `implementing-agent` supports **AI agent**, not a named Codex or human attribution. Because implementation authorship does not establish a separately identified comparison reviewer, the fields preserve that missing identity. Dates in filenames, source release metadata and unrelated execution receipts are not substituted for an absent comparison date.

Some referenced requirements/scenario reports describe individual behaviors. They remain inspectable, but do not provide a structured association between this selected action and an approved rules comparison. **Abilities checked: The card-review format has no ability-by-ability links** describes that card-review contract gap, rather than claiming no individual behavior was ever considered.

Installed builds bundle the existing retained card-source/rulings snapshots. `EvidenceDetails` validates their recorded text hash and card identity before describing Scryfall Oracle data or rulings marked as Wizards sources. Unavailable, mismatched or uncertain metadata gets a retained-file/provider-not-identified fallback. The source field links the exact recorded snapshot, not a newly fetched comparison. No ledger, frozen source, review claim or qualification was changed to add this display metadata.

Judith's applicable record is author/source-led and provisional, with engine evidence recorded on **4133241856227d1d36269e92aaba633bbaf93f5c**. The existing implementation-compatibility gate can accept that binding on current app-stable, but it does not convert it into newly executed evidence. This inspection does not map individual scenario outcomes to each instruction. Fresh development tests run for this delivery are reported below; they are not silently imported as new card qualifications.

## Commander objective connection

Judith's native death trigger and one-damage/any-target effect are represented together. The existing death-enhancement query accepts a narrower removal-related relationship because that damage can potentially affect a creature; it does not claim every trigger kills a creature, and does not enumerate all death payoffs in Magic. The research goal is broader than its present coverage. About objective and search details explicitly list accepted death sources and death-related removal, and state that general death-triggered draw, mana, token and life-change payoffs are not searched independently. See the source-bound [objective investigation](OBJECTIVE_COVERAGE.md), including omissions and policy exclusions.

The commander presentation uses the existing native walker on the selected card only. The directly associated trigger is marked in the displayed rules; a short cause-neutral connection supplies only the additional objective relationship. A bounded formatter describes the directly represented death trigger and effect without treating the query's removal classification as the definition of the objective. It does not certify rules fidelity or replace objective policies. Unsupported connections receive no invented relationship. Commander artwork is 220 px wide on desktop and remains compact on mobile. Commander imagery uses the same printing/face verifier, bounded provider requests, cache and explicit fallback as result imagery.

## Verified implementation and current limits

Fresh local verification on 2026-11-02 used application **1d71212c4a9be1e1b0f782ee0c492b810e51502b** and clean sibling engine **03869c08665bf476a8b40dacbfbcd501c54a33bd**, mode **app-stable**. The later source-printing assertion and documentation do not change production code. The strengthened eight-test source/coverage class was rerun successfully afterward.

- `./dev.ps1 installDist :assistant:test --rerun`: **509 Kotlin tests / 65 suites**, zero failures/errors/skips. This includes full CLI/API parity across the existing objectives, boundary and image checks, assurance-state controls, scenario regressions and the new death-source/payoff/policy investigation. The focused follow-up used `./dev.ps1 test --tests dev.mtg.assistant.DeathObjectiveCoverageTest` and passed all eight tests.
- The one-step launcher installed locked dependencies and passed TypeScript/Vite build. `pnpm --dir web test:ui`: **8 synthetic interface checks** passed. `pnpm --dir web test:live` against the real service: **5 Chrome checks** passed, covering all existing objectives, changed/blocked commanders, 390 px keyboard/containment behavior, imagery, recorded concerns and the new coverage cases. The first live attempt had four passes and one failure waiting for Tatterkite's provider image; its receipt is retained. The complete rerun passed.
- Actual commander image bounds were 220 × 307 px, top 247 px, in a 1365 × 950 viewport. The entire image was visible. Rendered desktop/mobile commander and representative card views were read, not merely asserted. The commander inspection shows the cause-neutral connection, retained Scryfall Oracle source, unnamed AI author and missing capture fields; historical engine tests remain identified as a different version. Exhaustive accessibility and other platforms are not claimed tested.
- Judith's search still examined **6,111 eligible authored cards**, indexing **5,400 distinct finding cards**: **487** identified, **351** unresolved-only, and **373** with any UNKNOWN (22 overlap identified cards). Original **499/410/2,140 MATCH/UNKNOWN/NO_MATCH checks** and **8,702 unsupported records** remain unchanged.
- The complete nested DiscoveryReport matched the previous actual capture in **every field, with no normalization**. No extractor, objective policy, eligibility, native definition, assurance ledger, frozen source or expected result changed. Qualifications and concern history were not promoted or resolved.

Ignored receipts live under `generated/explorer/`: `coverage-verification.json`, `coverage-kotlin.log`, `coverage-source-validation.log`, `coverage-interface-results.json`, `coverage-live-first-results.json`, `coverage-live-results.json`, `coverage-observation.json`, `coverage-commander-reading.json` and `coverage-report.json`. Current commander imagery is `coverage-commander.png`; the live suite also refreshed `hierarchy-reading-audit.json` and the `hierarchy-*` card/mobile screenshots. The unchanged-report comparison uses `clarity-discovery-report.json` as historical input, not newly executed evidence.

The reading audit preserved printed text once and compact status tiles. Expanded Discovery now distinguishes the recorded mechanic, accepted relationship, conditional death outcome, ability context, assumed game state and unresolved checks. Midnight Reaper's player-damage incompatibility takes precedence over a generic removal-policy explanation. A missing emitted relationship is no longer presented as a blanket negative verdict. Comparison metadata distinguishes fields absent from the current format from missing review records and actual failures. Technical records remain available.

This branch began from updated `main` **a3bb87b** (merged PR #21). No separate full native suite was rerun because engine code is unchanged. Seven exact-printing Scryfall snapshots were retrieved for this bounded source comparison, with hashes/identities in `src/test/resources/cards/objective-coverage-sources.json`. They do not become registered whole-card reviews, fresh card-specific gameplay receipts or independent approvals.

The proposed smallest follow-up is an explicit death-response/payoff contribution, initially correlating death triggers with draw and token creation (including Treasure). It requires approval as new discovery behavior and is **not implemented** here; see [scope and source cases](OBJECTIVE_COVERAGE.md). Life changes, direct mana and broader rewards require their own defined coverage. Per-ability review/execution mapping remains a data gap; only the latest run is retained; authored coverage is incomplete. No strategic score, simulation or automatic qualification was added. M1 remains open for user evaluation.

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

## Attrition source and activation investigation

The exact authored UDS #52 printing is **e3eb615b-249d-433f-a521-8310e8784b5d**, Oracle ID **a85ee0cf-39ce-49f2-874a-f6f54ca1775f**. Current Scryfall text and the empty returned ruling list were retained on 2026-10-30 with canonical URLs/raw hashes in `cards/image-and-explanation-sources.json`. Source-grounded tests compare the creature sacrifice, black mana payment and nonblack creature target with the actual stored activation; they do not register a full-card rules certification.

The legal-target requirement was checked against the official [Comprehensive Rules](https://magic.wizards.com/en/rules), 2026-09-25 revision: activation follows the target-selection and legality process (602.2b, 601.2c, 601.2e). Required targets are chosen before paying costs. The formatter preserves optional-target distinctions and does not infer an activation opportunity where the mandatory target is absent.

The original report still contains a MATCH for the sacrifice cost and a separate policy-excluded destruction check under the same action ID. The latter also retains unresolved affected-object checks for the nonblack restriction. The inspection names that uncertainty without declaring a rules defect or erasing the identified cost. Its registered source-review, card-specific execution and independent-approval records remain absent; image identity is separately verified. No consequential SDK or rules-engine correction was indicated by this investigation.
