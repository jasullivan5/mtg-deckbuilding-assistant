# Explorer — Card Discovery

The Explorer is a local browser interface over the application's existing Kotlin discovery. The first Prototype Lab review did not accept M1 as a usable research experience. The revised experience shows card text once, marks reliably associated abilities, adds brief objective-specific notes, and presents compact evidence fields with optional technical records. Mechanic browsing and verified printing/face imagery are preserved. Product Studio subsequently accepted the bounded M1 and M2 experiences against separate criteria. See the [milestone decisions](EXPLORER_MILESTONES.md#m1-and-m2-acceptance). Existing mechanical classifications and CLI evidence remain authoritative. See [repository layout](REPOSITORY_LAYOUT.md) for current versus historical evidence.

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

1. Leave **Judith, the Scourge Diva** and **My nontoken creatures die** selected. Open **About commander** and **About objective** before searching: labeled properties, verified art and a marked death trigger; three brief search categories. Inspect **Rules comparison** and **Engine tests** to distinguish retained sources, capture gaps and historical execution. Then **Find cards**, choose the **Sacrifice** mechanic, and inspect **Accursed Centaur**, **Viscera Seer** and **Attrition**. Compare required sacrifice on resolution, optional repeated activation and the legal nonblack-target prerequisite. Expand **Discovery** for what was checked.
2. Compare **Akki Scrapchomper** under **Identified mechanics** with **Agent of Shauku** under **Needs interpretation**. Akki names its artifact-or-land cost and accepted artifact-creature route; its land alternative remains unresolved. Agent names its land cost and unresolved overlap, without declaring creature-lands impossible or always requiring animation. Costs/targets are game conditions; the unhandled overlap is the research limitation. Original decisions remain under **Technical investigation**. Primary inclusion policy alternatives are documented in [the boundary investigation](OBJECTIVE_COVERAGE.md#primary-inclusion-decision-still-open).
3. Under the death objective, **Reassembling Skeleton** and **Bloodghast** have no accepted resource-replenishment contribution. Switch to **Return our creatures**: both now have recognized original-card returns, with mana/tapped-entry or landfall in their printed rules. Bloodghast's former false rejection is corrected. This is existing reanimation coverage, not a new recurring-fuel search. A vanilla **Walking Corpse** has no death finding; **Grim Haruspex**'s draw payoff remains outside this query. These omissions do not mean those cards lack strategic relevance. For a retained historical concern, inspect **Tatterkite** under **All findings** and open its investigation details.

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

Judith's native death trigger and one-damage/any-target effect are represented together. The existing death-enhancement query accepts a narrower removal-related relationship because that damage can potentially affect a creature; it does not claim every trigger kills a creature, and does not enumerate all death payoffs in Magic. The research goal is broader than its present coverage. About objective lists accepted death sources and death-related removal. Detailed search scope explains that general death-triggered draw, mana, token and life-change payoffs are not searched independently. See the source-bound [objective investigation](OBJECTIVE_COVERAGE.md), including omissions and policy exclusions.

The commander presentation uses the existing native walker on the selected card only. The directly associated trigger is marked in the displayed rules. The redundant cause-neutral sentence is no longer displayed; its existing API value remains available in commander technical records. A bounded formatter describes the directly represented death trigger and effect without treating the query's removal classification as the definition of the objective. It does not certify rules fidelity or replace objective policies. Unsupported connections receive no invented relationship. Commander artwork is 220 px wide on desktop and remains compact on mobile. Commander imagery uses the same printing/face verifier, bounded provider requests, cache and explicit fallback as result imagery.

## Verified implementation and current limits

Fresh verification on 2026-11-02 used clean sibling engine **03869c08665bf476a8b40dacbfbcd501c54a33bd**, mode **app-stable**. Production extraction was tested at **877f9ee90fea7120f85096de583db7d898e6fd56**. Subsequent commits strengthen source/ability-scope tests and refine presentation; the final running candidate was **f4cbe602e4fdbcffa982841c8cfe7d62d93db40c**. Later documentation commits are not new test executions.

| Check actually run | Application revision | Outcome |
| --- | --- | --- |
| `./dev.ps1 installDist :assistant:test --rerun` | 877f9ee | **516 tests / 66 suites**, zero failures/errors/skips; CLI/API parity, scenarios, eligibility, source/assurance states and regressions |
| `./dev.ps1 installDist :assistant:test --tests dev.mtg.assistant.SacrificeBoundaryTest` | 049be635a2eea6834a3a84b18a05a5d1b133a846 | **7 passed**, strengthened to verify all affected sources and exact movement paths |
| Same focused command plus `--tests dev.mtg.assistant.AbilityInspectionTest` | 6061557829123106efa934f4753e9b93af2c154d | **14 passed**, including the final nonduplicated accepted-route field |
| Locked installation and TypeScript/Vite build through the one-step launcher; `pnpm --dir web test:ui` | Final frontend unchanged across these follow-ups | Build and **8 synthetic interface checks passed** |
| `pnpm --dir web test:live`, installed Chrome, real local Kotlin service | 049be635a2eea6834a3a84b18a05a5d1b133a846 | **6 passed**; existing objectives, blocked/changed commanders, evidence/concern cases, keyboard/390 px and new sacrifice/resource exercises |
| `pnpm --dir web exec playwright test tests/live.spec.ts -g 'sacrifice type boundaries'` | f4cbe60 | **1 passed**, rechecking the final route field and unambiguous noncreature wording |

The initial source-led resource test exposed Bloodghast's false rejection. A new synthetic fixture initially omitted required creature stats; it was completed and the full suite reran successfully. Strengthening the source comparisons also exposed the need to bind individual movement paths on multi-ability cards; that test scope was corrected and passed. These development failures are retained in the local receipts; no frozen expectation was rewritten.

Fresh before/after CLI captures use application **45782ae4e537cdaab0a2e13de22308466409ecb1** versus **877f9ee90fea7120f85096de583db7d898e6fd56**, the same engine, the same **6,111** eligible mechanical inputs and identical eligibility records. Death and token classifications/counts/decisions are unchanged. Twenty shared movement facts have corrected source-zone/related metadata; reanimation changes nine false negatives to MATCH: **95 → 104 cards**, **97 → 106 pathways**. Every changed card/path and preserved negative is recorded in [the controlled comparison](OBJECTIVE_COVERAGE.md#controlled-comparison-and-source-review). This is not an all-fields-unchanged report: raw differences remain inspectable. The final browser report was also compared with the corrected CLI report.

Rendered controls and card/evidence panels were actually read. Judith's image remains 220 px wide on desktop, with her trigger marked once and no repeated cause paragraph. Verified images were allowed to finish loading for Centaur, Seer, Attrition, Akki and Agent before final captures. Akki's final mobile view was checked at 390 px. The review preserves normal costs/targets as conditions, genuine interpretation gaps as such, historical tests as historical, and missing review as neutral. Exhaustive accessibility and non-Windows launch are not claimed tested.

Ignored receipts under `generated/explorer/` include `boundary-kotlin-results.json`, `boundary-kotlin.log`, `boundary-source-validation.log`, `boundary-final-fields-results.json`, `boundary-interface-results.json`, `boundary-live-results.json`, `boundary-final-live-results.json`, `boundary-before-*.json`, `boundary-after-*.json`, `boundary-diff.json`, `boundary-final-reading.json`, `boundary-final-report.json` and `boundary-verification.json`. Current desktop/mobile views use `boundary-final-*.png`. The temporary service is stopped after verification.

Thirty new exact-printing Scryfall snapshots support the bounded comparisons. `cards/sacrifice-boundary-sources.json` and `cards/trigger-zone-sources.json` retain raw hashes, URLs, printing/Oracle identities and native source paths. The latter identifies exact affected ability paths and observed before/after verdicts. These are not new registered whole-card approvals, independent reviews or card-specific gameplay receipts. Existing assurance, qualification, concerns, native definitions and frozen evidence remain unchanged. No separate full native engine suite was rerun; the engine was not modified.

The final M1 implementation candidate was delivered for hands-on Prototype Lab review. Product Studio subsequently accepted M1 and M2 within their documented scope. The primary inclusion boundary for type-dependent sacrifice remains a Product Studio choice before broader policy changes. General death payoffs, distinctive recurring-resource discovery, role filters, composition and scenario viewing remain later research, not new M1 capabilities. [Objective direction v1](OBJECTIVE_MODEL.md) records the adopted decisions and a bounded future experiment. Multiple active trigger zones retain their existing treatment; this correction addresses explicitly single active zones. Only the latest run is retained, catalog coverage is incomplete and images can require network access.

## Sacrifice findings investigation

Source snapshots and rulings were retrieved from Scryfall on 2026-10-30 for PCY #55, LCI #89 and ONS #123. Raw hashes and URLs are retained in `src/test/resources/cards/image-and-explanation-sources.json`. The source set is a bounded investigation, not an independent whole-card qualification.

| Card | Oracle expectation and authored representation | Existing discovery | Presentation consequence |
| --- | --- | --- | --- |
| Agent of Shauku | Mana plus a land sacrifice pays for a targeted power increase. The authored cost retains IsLand. | UNKNOWN: the application does not resolve IsLand as creature-compatible; policy accepts sacrifice costs only after mechanical checks. | Explain the land sacrifice that caused investigation. An ordinary land does not satisfy the creature objective. A land that is also a nontoken creature could do so, but this run establishes no animation or cross-card interaction. |
| Acolyte of Aclazotz | Tap and sacrifice another creature or artifact. The authored filter retains the OR and excludeSelf. | MATCH under retained conditions: a legal choice can be a nontoken creature; a noncreature artifact or token does not meet the objective. | Show the choice, source exclusion and tap/resource restrictions. This is one OR-filtered sacrifice, not two independently payable costs. |
| Accursed Centaur | On entering, sacrifice a creature. The source is not excluded. The retained ruling says it must sacrifice itself if it is the only creature. | MATCH under retained conditions, mandatory triggered effect rather than an activated sacrifice outlet. | Show the enters trigger, required sacrifice and source eligibility; no repeatable activation is inferred. |

Checked against the official [Comprehensive Rules](https://magic.wizards.com/en/rules), revision 2026-09-25: sacrifice is a controller's battlefield-to-graveyard action (701.21a); death requires reaching the graveyard from the battlefield (700.4); effects can retain earlier card types (205.1b). No engine correction or broader filter-satisfiability change is justified by these three findings. That earlier sacrifice investigation preserved the extractor, policies, report verdicts, source bindings and frozen expectations; the separate finalization source-zone correction is recorded above.

## Printing and face imagery

`CardImages` retrieves only requested visible cards, in batches of at most 40, from Scryfall's collection endpoint. A native Scryfall printing ID, an ID embedded in its Scryfall image URL, or exact set/collector identity is required. No name-only lookup is used. Conflicting known printing IDs, set/collector data, declared Oracle ID, physical layout, complete ordered face names, types or relevant mana costs prevent image admission. Unsupported layouts and missing images receive explicit fallbacks. The image association algorithm is `printing-face-image-v1`; it does not change coverage's physical-identity algorithm or eligibility.

Scryfall's [API guidance](https://scryfall.com/docs/api) was checked on 2026-10-30. Requests identify the application and request JSON; collection batches are spaced at least 600ms apart. Metadata is cached for 24 hours under ignored `generated/explorer/image-metadata/`, revalidated against the current definition on every use, and bound by retrieval time and SHA256. Failure responses are not automatically retried; non-200 responses pause further requests (respecting a bounded Retry-After). Images are loaded directly from validated HTTPS Scryfall image URLs, unaltered, with browser caching, lazy loading, source links, artist credit and Wizards attribution. No image bytes are committed or proxied. Offline or conflicting metadata never substitutes approximate art. Image identity verifies the association, not card mechanics or current rules wording.

## Attrition source and activation investigation

The exact authored UDS #52 printing is **e3eb615b-249d-433f-a521-8310e8784b5d**, Oracle ID **a85ee0cf-39ce-49f2-874a-f6f54ca1775f**. Current Scryfall text and the empty returned ruling list were retained on 2026-10-30 with canonical URLs/raw hashes in `cards/image-and-explanation-sources.json`. Source-grounded tests compare the creature sacrifice, black mana payment and nonblack creature target with the actual stored activation; they do not register a full-card rules certification.

The legal-target requirement was checked against the official [Comprehensive Rules](https://magic.wizards.com/en/rules), 2026-09-25 revision: activation follows the target-selection and legality process (602.2b, 601.2c, 601.2e). Required targets are chosen before paying costs. The formatter preserves optional-target distinctions and does not infer an activation opportunity where the mandatory target is absent.

The original report still contains a MATCH for the sacrifice cost and a separate policy-excluded destruction check under the same action ID. The latter also retains unresolved affected-object checks for the nonblack restriction. The inspection names that uncertainty without declaring a rules defect or erasing the identified cost. Its registered source-review, card-specific execution and independent-approval records remain absent; image identity is separately verified. No consequential SDK or rules-engine correction was indicated by this investigation.

## Discovery Coverage — M3.1

From the completed Judith search for **My nontoken creatures die**, open **Investigate Discovery Coverage**. The view compares one reviewed relationship per distinct card in a deliberately selected 12-card cohort. The category buttons partition those 12 cards. Each button shows the same count as you switch filters. Ordinary findings, pathway counts and assurance evidence retain their own populations.

1. **Recognized contribution.** Select Viscera Seer or Accursed Centaur. Compare the source expectation with the native ability, then inspect **Discovery** and **Engine tests**. Seer pays a sacrifice cost to activate. Centaur requires a sacrifice when its entry ability resolves. In either case the creature must reach the graveyard for the death.
2. **Outside current objective coverage.** Select Reassembling Skeleton. It returns tapped from the graveyard and can replenish nontoken creature fuel for a later death. Inspect the retained Oracle text, the native card and the movement fact in its evidence. The live death query emits no pathway for it, though an unread instruction puts it in the findings index. Select Pitiless Plunderer to inspect a card missing from that index altogether. Grim Haruspex offers another comparison where the draw goes unqueried even though a separate morph diagnostic appears in ordinary findings.
3. **Unresolved relationship.** Select Agent of Shauku and compare it with Akki Scrapchomper under **Recognized contribution**. Akki's artifact route can use an inherent artifact creature. Agent's land route stays unresolved in the mechanical query. Dryad Arbor illustrates the printed type overlap and is excluded by Judith's color identity. Murder is a separate control for deliberate objective policy exclusion.

The [versioned comparison](../src/main/resources/discovery-coverage-v1.json) records exact printing and Oracle IDs, source URLs and SHA-256 bindings to the existing snapshots. The [retained investigations](OBJECTIVE_COVERAGE.md) document the rules sources, native representation and decisions. No registered assurance records, engine definitions or objective policies were added. Each card's inspector distinguishes available discovery, rules comparison, engine tests and independent verification. The source review concerns the listed relationship and recorded engine revision. The comparison shows uncertain identity or unexplained absence when the available records cannot support a stronger diagnosis.

A bounded next research question is whether native death triggers can be correlated with draw and token creation from the same ability. Grim Haruspex, Doomed Dissenter and Pitiless Plunderer give three different trigger/effect controls for that study. The Agent/Akki primary-inclusion question remains open.

### M3.1 verification record

Windows, JDK 21, Gradle 9.6.1, Node 24.22.1, pnpm 11.25.0 and Playwright with Microsoft Edge. The application base was `f0b77714e4cab57a812e389c38561d068cb4878e`. Both live builds reported clean Argentum `app-stable` at `03869c08665bf476a8b40dacbfbcd501c54a33bd`.

| Check | Result |
| --- | --- |
| `.\dev.ps1 test` | 519 passed, zero skipped. Includes source snapshot hashes and identities, exact native printing joins, missing-definition and unassessed inputs, and CLI/API report parity for all three objectives. |
| `pnpm build`, then `pnpm test:ui` in web | Production build passed and eight interface tests passed in Edge. |
| `.\scripts\explorer.ps1 -Port 8797`, then `pnpm test:live` with the loopback URL | Seven passed against application `b5e8d34f89cf571f76bc2f27e58e686d54681252` with the corrected tests from `b83db8b2ab54c2e337f77834e38dd2a29391a9a5`. Includes real discovery, changed commander and objective, verified imagery, keyboard navigation and evidence inspection. |
| `.\scripts\explorer.ps1 -Port 8798`, then `pnpm test:live -g 'Discovery Coverage'` | Final production build and coverage journey passed. The health endpoint reported application `3187a1113bc0542359a44a53bf9e24337c6912d1`. The separate port avoids the occupied default. |

The live sample contained four recognized contributions, four gaps in objective coverage, one unresolved relationship, one objective-policy exclusion, one ordinary body and one Commander eligibility exclusion. All 12 definitions were present. Outside the cohort, the live death search used 6,111 eligible authored cards and emitted 499 MATCH, 410 UNKNOWN and 2,140 NO_MATCH pathway checks, matching the recorded baseline. M3.1 changed the comparison and evidence presentation only. The original result export and index are checked against live report records and CLI output.

An initial simultaneous build hit a Gradle file hash cache lock. The first completed Kotlin and browser runs exposed an incorrect assertion about Skeleton's index membership, corrected by testing its unread instruction and Plunderer's absence separately. No new native scenarios or independent qualifications were added. Cross-platform and exhaustive accessibility checks were outside this verification.
