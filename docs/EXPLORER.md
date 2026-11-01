# Explorer M1 — Card Discovery

The Explorer is a local browser interface over the application's existing Kotlin discovery. The first Prototype Lab review did not accept M1 as a usable research experience. The revised experience provides complete relevant-ability inspection, scoped clickable evidence, compact controls, mechanic browsing and verified printing/face imagery. It is ready for a fresh Prototype Lab review; acceptance remains the user’s decision. Existing mechanical classifications and CLI evidence remain authoritative. See [repository layout](REPOSITORY_LAYOUT.md) for current versus historical evidence.

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

1. Select **Judith, the Scourge Diva**, choose **My nontoken creatures die**, and **View commander**. Her verified image, mana cost, type, text and black/red identity are visible before searching. The connection explains why causing your nontoken creatures to die can trigger her damage ability. Click **Rules comparison**, **Engine tests** and **Independent review** for short explanations; expand **Technical evidence and recorded interactions** only when you want exact records. The commander has no inapplicable mechanic-search status.
2. **Find cards**, then search the findings for **Attrition**. Its ability excerpt groups black mana, creature sacrifice and target destruction. Read the legal nonblack-target requirement and repeat-activation conditions. The identified cost remains positive while an actual unresolved target-filter interpretation is stated beside the ability; the policy-excluded destruction check is technical evidence. Click the evidence controls: missing comparison/testing records are not failures or an engine-incapability claim.
3. Compare **Acolyte of Aclazotz** with **Agent of Shauku** under **Needs interpretation**. Acolyte's creature/artifact sacrifice choice and life effects remain together. A token or ordinary noncreature artifact does not advance this objective. Agent's land payment surfaced, but the search has not established that the land can also be a nontoken creature when sacrificed. For a real recorded concern, choose **All findings**, find **Tatterkite** and inspect its open historical counter-prohibition concern; this view has not rerun that failure or resolved the ledger.

Objective help is contextual under **About this objective**. Its scope is cards that can be included in the selected commander's deck, using full color identity and format legality rather than a colors-only search. Change an objective or commander to start a new search. Selecting **Murder as the commander** remains blocked; ordinary card eligibility and commander selection are different questions. The three technical objective IDs are unchanged.

## Reading findings honestly

Ordinary browsing prioritizes **Identified mechanics**: cards with at least one accepted contribution stay there even when another instruction is unresolved or excluded. **Needs interpretation** contains cards with UNKNOWN results and no MATCH. **All findings** includes every indexed card; it is not a claim that all those cards contribute. Missing rules review, gameplay testing and independent approval are evidence gaps, not extra mechanical-uncertainty categories.

**Research and diagnostic views** retains overlapping accepted/unresolved/nonqualifying/unread card counts. Those count distinct physical cards and must not be summed. Pathway checks and unsupported instruction records have separate denominators in the optional run-scope table. Scope is the eligible authored catalog and application extensions, not all Commander-legal Magic cards; unresolved eligibility remains excluded and inspectable.

Ability headings are bounded excerpts generated from the corresponding stored cost, trigger and effect, rather than “Triggered ability 1.” They are not guessed associations between Oracle text lines and internal indexes. Unmapped forms retain a truthful descriptive fallback and technical identifiers. The selected-card view groups results only through their exact stored ability/action record. For Attrition, that record itself contains both cost and destruction, so they are displayed as a single activation. Grouping does not assert a guaranteed whole-action outcome. An unrelated negative check is available under **Investigate this ability**, not a prominent card-wide verdict. Other untranslated instructions remain optional interpretation questions; a confirmed historical concern is visibly distinguished from an unresolved question.

The Kotlin formatters describe retained operation/filter/cost/target records; they do not change acceptance, interpret Oracle text as production rules, or certify engine execution. The frontend renders those projections. Original results, actions, facts, source/review data and exclusions remain accessible, including JSON.

Only **the latest run** is retained in memory. Starting another discovery invalidates earlier run URLs, including those in other tabs. Expired evidence reports a recovery message; rerun the question. Export complete evidence before replacing a run if you need to retain it. Exports are not imported as live discovery.

## Local HTTP contract — schema version 1

All API paths start with `/api/v1`. JSON bodies reject unknown fields; commander names use exact native lookup. Browser input never becomes a shell command or filesystem path.

| Method and path | Request | Response |
| --- | --- | --- |
| GET /health | none | ready state, actual engine SHA/mode, packaged application revision |
| GET /objectives | none | three Objective records: exact id, label, explanation |
| POST /commander | {commander} | unchanged CommanderInspection; HTTP 200 also covers blocked selections |
| POST /commander-view | {commander, objective} | original inspection plus verified image/fallback, readable card text, color identity, bounded objective connection and applicable scoped evidence; does not run whole-catalog discovery |
| POST /discover | {commander, objective} | RunSummary: run ID, fresh structural execution mode/time, revisions, scope/counts, limitations and compact card index |
| GET /runs/{id}/cards/{index} | index from that run's summary | CardEvidence: authored text, exact per-card results/actions/facts/unsupported/support/eligibility/assurance readable restatements, whole-ability projections and scoped evidence labels |
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
| Relevant mechanic | The stated part of this ability met the existing objective checks. | A recognized sacrifice cost does not verify its targets, other effects, rules fidelity or execution. UNKNOWN remains an unresolved relationship. |
| Rules comparison | The registered record reports comparison with retained card sources. | Author/source-led review is not independent rules certification. A card-level record does not supply per-ability coverage. Verified artwork is separate. |
| Engine tests | Registered bounded execution outcomes exist. | A scenario filename alone is incomplete evidence. Recorded passes/failures retain their engine revision; a compatible historical pass is not a fresh run. |
| Independent review | The existing exact-bundle qualification gate accepted an explicit independent approval. | Author tests, source comparison and structural matches cannot create this status. Approval scope is its bound requirements and cases. |

Each label opens two or three player-facing sentences describing the supported claim and important gap. A nested technical inspector retains exact scopes, recorded claims, limitations, references, hashes, versions and potential next investigations. Ordinary mana payments and legal targets appear as requirements, not research warnings. Missing independent review is neutral, does not block discovery and is not a request for the user to manually approve the catalog. No new independent-review process was added. Five display states distinguish established scoped claims, missing records, incomplete/unresolved interpretation, known problems and outdated evidence. Missing evidence is neutral; suspected issues are not confirmed defects. Historical outcomes remain visible even when their bindings become inapplicable, without being asserted as current passes or failures. Open recorded concerns retain their history; obsolete historical next-action wording is preserved in raw records rather than issued as a new workflow instruction.

Judith's applicable record is author/source-led and provisional, with engine evidence recorded on **4133241856227d1d36269e92aaba633bbaf93f5c**. The existing implementation-compatibility gate can accept that binding on current app-stable, but it does not convert it into newly executed evidence. This inspection does not map individual scenario outcomes to each instruction. Fresh development tests run for this delivery are reported below; they are not silently imported as new card qualifications.

## Commander objective connection

Judith's native death trigger and one-damage/any-target effect are represented together. The existing death-enhancement query accepts a narrower removal-related relationship because that damage can potentially affect a creature; it does not claim every trigger kills a creature, and does not enumerate all death payoffs in Magic. The objective itself is broader: cause your nontoken creatures to die and discover recognized ways to benefit. This distinction is stated in objective help; the exact accepted branch remains in discovery evidence.

The commander presentation uses the existing native walker on the selected card only. A bounded formatter describes the directly represented death trigger and effect without treating the query's removal classification as the definition of the objective. It does not certify rules fidelity or replace objective policies. Unsupported connections receive no invented relationship. Commander imagery uses the same printing/face verifier, bounded provider requests, cache and explicit fallback as result imagery.

## Verified implementation and current limits

Fresh local verification on 2026-10-30 used application **a32b4108e8f3d899b492b57769ab1105641c0868** and the clean sibling engine **03869c08665bf476a8b40dacbfbcd501c54a33bd**, mode **app-stable**. Subsequent documentation-only changes are not another test execution.

- **493 Kotlin tests / 63 suites**, zero failures/errors/skips. Coverage includes full CLI/HTTP report parity, complete Attrition activation/optional-target controls, partial support, missing records, historical and outdated evidence (including retained failures), existing independent-approval gates, commander presentation, image identity/cache/fallback and API boundaries.
- Locked frontend installation and TypeScript/Vite build passed, followed by **6 synthetic interface tests** and **3 real Chrome browser tests**. Real exercises covered all existing objectives, changed/blocked commander selection, complete Attrition, Acolyte's alternatives and life effects, Agent's unresolved land relationship, Judith's historical scoped records, verified loaded artwork, count reconciliation, keyboard interactions and 390px containment. Desktop/mobile screenshots were inspected; exhaustive accessibility and other platforms are not claimed tested.
- The supported one-step Windows launcher was executed at that exact saved revision; health reported the same application and engine SHAs. The temporary validation service is stopped after checking.
- Judith's death search examined **6,111 eligible authored cards** and indexed **5,400 distinct finding cards**: **487** had identified contributions, **351** had unresolved results without an identified contribution. The diagnostic any-UNKNOWN count remains **373**: 22 cards with both stay in ordinary identified browsing. Original overlapping counts, **499/410/2,140 MATCH/UNKNOWN/NO_MATCH pathway checks**, and **8,702 unread instruction records** are preserved.
- The complete nested DiscoveryReport matched the pre-revision Judith death capture in **every field, with no normalization**. No extractor, policy, eligibility, native definition, assurance-ledger or frozen expectation was changed. Qualification and concern history were not promoted or resolved.

Current ignored receipts/screenshots live under `generated/explorer/`: `evidence-verification.json`, `evidence-observation.json`, `kotlin-evidence-verification.log`, `interface-results.json`, `browser-results.json`, `evidence-discovery-report.json`, `attrition-inspection.png`, `agent-investigation.png`, `commander-evidence.png`, `mobile-evidence.png` and `mobile-commander-evidence.png`. The source/ruling snapshots and hashes used by tests are retained under `src/test/resources/cards/`. Earlier fixture runs exposed ambiguous control/hidden-record selectors; those harness assertions were corrected and the current checks reran successfully.

Per-ability review/execution mapping remains an evidence-data gap. Readable formatting is deliberately bounded; complex or untranslated instructions require the retained exact records. A missing registered source or run record is not proof of a defective implementation. Authored coverage is incomplete, only the latest run is retained, and exports remain large. No strategic score, deck simulation, automatic qualification or engine correction was added. M1 remains open for user evaluation.

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
