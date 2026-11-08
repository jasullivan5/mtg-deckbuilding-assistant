# Research Progress Dashboard

The Explorer has three primary destinations: **Discover**, **Progress**, and **Primer**. Discover preserves Commander selection and the four existing objective policies. Progress is available before a discovery run (`http://127.0.0.1:8787/#progress`). Primer explains the pipeline and evidence boundaries. The collapsed **Developer research index** retains secondary investigations and the curated Lab; discovery links to execution are also collapsed. The former multi-part primer walkthrough is retired; its source evidence and shared orientation components remain.

## Launch and guided tasks

```powershell
.\scripts\explorer.ps1
```

This one-step local launcher still builds both layers. Default Progress measures the native inventory and interpretation, extension assurance, and the retained historical comparison. External coverage requires an explicitly selected local snapshot; absence or invalid input is displayed, never converted to zero missing cards.

To reproduce the source population used by the existing identity/eligibility approvals, deliberately replay its retained manifest with the existing downloader (Python 3.11+):

```powershell
python scripts/oracle-snapshot.py download --directory generated/oracle --manifest src/test/resources/progress/reference-snapshot.manifest.json
.\scripts\explorer.ps1 -OracleSnapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
```

The download is 25,135,745 bytes, SHA-256 `081a50ef82c767523f955e914153cf988ff045a72e3765b6aa2382567f9eb53a`, source update `2026-10-26T21:01:59.922+00:00`. The downloader and Kotlin loader verify the manifest. Ordinary dashboard requests do not fetch source data. Another snapshot may invalidate reviewed eligibility associations; an error is visible instead of silently comparing mismatched sources. Image lookup remains the existing separate Scryfall workflow.

1. **Inspect coverage.** Open Progress. Compare native physical definitions with registered names. Read the typed-node distribution, then expand mechanical groups and filter `Pit Raptor`. Inspect the retained native location and unsupported/partial explanation. With the explicit snapshot selected, inspect the Judith-eligible external denominator, missing definitions, uncertain associations and exclusions separately.
2. **Examine compounding gains.** Open the historical comparison drill-down. Choose source-bound sacrifice and filter `Seal of Fire`: an UNKNOWN becomes a justified NO_MATCH. Choose source-card discard and filter `Twinshot Sniper`: a new pathway becomes MATCH. Inspect exact decisions and provenance. The gains are recorded historical observations, not current-engine execution.
3. **Investigate an obstacle.** Expand inherited source fidelity in the obstacle list and follow supporting evidence. Open Tatterkite's assurance record. Its concern remains BLOCKED, while inherited definitions stay outside the application ledger. Compare this scope with the remaining capability overview; qualification alone would not establish contextual deck value.

## Metric contract

| Measurement | Population / counting rule | Limits and records |
| --- | --- | --- |
| Native representation | `physicalCards(Catalog.cards)`, grouped by exact application provenance; registered names reported separately | Existing back/flip aliases and standalone meld results excluded. Not printings, Oracle fidelity or external coverage. Report includes every physical name. |
| External representation | Existing `catalogCoverage` over the explicit snapshot and ordinary Judith black/red eligibility; no objective filter | Exact manifest and engine retained. Confident associations / confirmed eligible external physical identities is the only percentage. Missing, ambiguous/review/conflict, exclusions and unresolved source records remain separate. Diagnostic rows overlap. |
| Mechanical interpretation | Existing `audit.coverage` traversal over every native physical definition, no Commander or objective filter; support from `PathwayExtractor(inspectCardEntries=true)` | Unit is serialized **typed-node occurrences**, including costs, conditions, filters and quantities. It is deliberately not an instruction-completeness estimate. SDK vocabulary not present in catalog, keyword-lowered behavior and dynamic engine execution are outside this denominator. |
| Explicit node support | SUPPORTED occurrence count / all independently encountered typed nodes | Partial and unsupported enclosing structures remain so. OBSERVED_WITHOUT_LOCAL_PATHWAY is unclassified, including outside-objective structures/metadata, never NO_MATCH. A supported local node does not certify the whole action/card. |
| Mechanical facts | Existing emitted facts by kind and retained confidence | Fact confidence is not an objective verdict, source-fidelity rating or probability. Complete constraints and native locations are inspectable. |
| Reusable extractor reach | Observed `nativeType` labels with at least one SUPPORTED occurrence, with supported / observed occurrence counts per label | The audit's serialized discriminator also includes metadata and outside-objective structures, so its distinct-label total is not a count of all mechanic families. No guessed file-name families or claim that all variants work. Parent/child nodes are separate occurrences. No sums of overlapping group card counts. |
| Source review | Existing `Catalog.translationAssurance` and `assessTranslation` gate over physical application extensions | Current evidence, stale evidence and no evidence are disjoint. Fidelity states displayed only for current evidence. All other native definitions are not measured under this ledger; no inherited pass/fail inference. |
| Qualification | Existing gate's QUALIFIED / PROVISIONAL / BLOCKED / WITHDRAWN within extension scopes | No promotion from test counts, merged PRs, images, structural matches or Lab success. A measured zero applies only to this ledger. |
| Engine execution | Not measured at catalog scale | Ledger's historical card-level execution has no scenario-to-ability outcome index. No aggregate scenario pass count is inferred. Exact original evidence remains in drill-down and historical investigations; missing is not failed. |
| Historical gain | Two existing controlled graveyard-state comparisons; exact 6,111-card input and full eligibility equality; shared intermediate digest | Matching cards, pathways, unknowns, nonmatches and unsupported records are separate. No trend to the current catalog is inferred. See retention method below. |
| Obstacles and capability states | Kotlin-owned authored narrative, anchored to audit/ledger counts and current architecture/limitations | Foundational assurance gaps first, then interpretation/representation/execution/context. Order is a research judgment; no numerical severity or overall completion score. Impact categories overlap. |

The support percentage is conservative **local audit coverage**, not the portion of every Magic rule mechanically understood. A card with one supported operation and several unread structures is never counted as fully interpreted. Objective policy and discovery mechanics are untouched.

## Architecture and inspectability

`server/ResearchProgress.kt` projects the existing catalog, native audit, shared extractor, catalog-coverage audit and assurance gate. It adds no native walker, Oracle parser, objective policy, database or rules execution. React renders the Kotlin projection and filters records; it makes no mechanical decisions. Expensive current measurements are lazy, cached for the server/build, and run through the existing exclusive research worker and deadline. They do not replace the retained Discover run. A restart remeasures; there is no hidden source update.

GET endpoints:

- `/api/v1/progress`: current projection, native physical population, narrower assurance records, obstacles and capability limits.
- `/api/v1/progress/mechanics`: existing full grouped native audit; up to five representative locations per group, explicitly labelled. Group card counts overlap.
- `/api/v1/progress/instructions`: every emitted extractor structural-support record, including partial/unsupported records.
- `/api/v1/progress/facts`: every emitted fact with retained constraints and native location.
- `/api/v1/progress/external`: existing catalog-coverage report with every eligible/missing/mapping record, unresolved pool records, exclusions and manifest, or an explicit unavailable measurement.
- `/api/v1/progress/history`: compact retained comparison including every eligible physical name and every changed verdict. Unavailable packaged baseline returns a distinct 404; no fabricated trend.

All native counts use the packaged actual engine SHA. `EngineBuild.evidenceCompatible` remains the existing narrow implementation-equivalence rule for the extension ledger; it cannot relabel recorded execution as fresh. Historical report sources keep their exact original revisions. Research tools and supporting evidence remain in Git, not permanent Project attachments.

## Historical retention and reproduction

`src/main/resources/research-history.json` retains a bounded projection of the existing reports, not a new execution receipt. `scripts/retain-progress.mjs` requires three explicit input reports, verifies their exact documented SHA-256 values and checks engine, objective, commander, no-filter status, complete ordered eligible population and full eligibility equality. It keys verdict transitions by existing `stateEntry.factId`, preserves negative cases, lost matches, new unknowns and removed pathways, and validates the documented gains. It is not part of any build and needs no historical export folder at runtime.

Historical engine: `03869c08665bf476a8b40dacbfbcd501c54a33bd`. Initial application: `b9c970df8a1e99ec32c505121d19019c54e5e98d`; discard implementation: `8dc1f01df9f62da14f2413a1d89807ca841bcc8f`; sacrifice baseline: `6d46214715713d0a34c296e4419682bc47dfd2fa`; sacrifice implementation: `fa9ba4030356f2eef214d92f4c41b903c196fc7e`. The bootstrap-only sacrifice baseline emits the exact same bytes as the discard implementation's intermediate report.

| Stage | Matching pathways | Matching cards | Unknown pathways | Nonmatch pathways | Unsupported records |
| --- | ---: | ---: | ---: | ---: | ---: |
| Initial | 433 | 412 | 892 | 106 | 8,552 |
| Source discard | 442 | 420 | 892 | 112 | 8,537 |
| Source sacrifice | 535 | 508 | 576 | 335 | 8,537 |

Source discard: 9 new matching pathways / 8 newly matching cards, 6 new nonmatch pathways, no lost prior match. Source sacrifice: 93 newly matching pathways / 88 newly matching cards, 223 UNKNOWN-to-NO_MATCH transitions, no lost prior match or removed pathway. Source-disciplined positive, negative, deceptive and held-out cohorts are in [discard](SOURCE_DISCARD_DISCOVERY.md) and [sacrifice](SOURCE_SACRIFICE_DISCOVERY.md). Spark Elemental's historical adverse execution failure remains there; later engine/Lab records do not erase it.

## Verification and visual review

Current measurements and the stability comparison use maintained engine `84fb6147f5b6ae11445f215ce23c3a9bb6a72db1`, not the historical comparison engine. Application baseline is current main `b6e7cc7045a9cc01fbc76779d59c80d40bb2bb2e`. Backend implementation/stability candidate is `3967b05d0acd604ce0496bba4428ed91de7654f8`; final frontend/navigation implementation is `11c50350fe89b9b04df17e23bb79053d40cbc78e`. The later change modifies frontend navigation/presentation and documentation only. The final clean Windows build packages `11c50350fe89b9b04df17e23bb79053d40cbc78e`; subsequent receipt/test-only commits do not change the running implementation.

### Observed current counts

| Measurement | Result | Scope |
| --- | ---: | --- |
| Native physical definitions | 17,017 | 16,995 inherited + 22 application extensions |
| Registered names | 17,373 | Includes faces/aliases, a different unit |
| External reference records | 39,049 | Exact explicit snapshot above |
| Judith-eligible external physical identities | 12,828 | 26,221 excluded; 0 unresolved pool records |
| Confident native associations | 6,658 / 12,828 (51.9%) | 6,649 inherited + 9 extension associations; not fidelity or discovery eligibility |
| Missing native definitions | 6,159 | Within the same external eligible denominator |
| Uncertain associations | 11 | 1 ambiguous + 10 identity-review-required |
| Effective source/native metadata disagreements | 575 | Within confidently associated records; not silently repaired |
| Native typed nodes | 192,992 | Entire native physical population, no Commander filter |
| Explicitly supported nodes | 7,863 (4.1%) | Local typed-node coverage, not all mechanics understood |
| Partially supported nodes | 2,962 | Same typed-node denominator |
| Unsupported nodes | 22,537 | Same typed-node denominator |
| Unsupported or unresolved nodes | 41,991 | Same typed-node denominator |
| Observed without local pathway | 117,639 | Unclassified including metadata/outside-objective structures; not NO_MATCH |
| Native type labels with explicit support | 19 | 1,628 distinct serialized type values observed; this is not an all-mechanic-family denominator |
| Emitted facts | 13,632 | 12,689 MATCH-confidence / 938 UNKNOWN / 5 NO_MATCH-confidence; separate from objective verdicts |
| Extension assurance scopes | 22 | 0 current / 22 stale / 0 without recorded evidence |
| Qualification | 0 qualified / 21 provisional / 1 blocked / 0 withdrawn | Extension gate only; Tatterkite remains blocked |
| Inherited definitions outside assurance process | 16,995 | Catalog fidelity and qualification not measured |

These populations are not interchangeable. In particular, the external eligible population (12,828), confidently associated external population (6,658), and controlled native discovery input (6,111) answer different questions. The external association audit evaluates no objective; its zero objective counters do not mean zero unknown mechanics.

### Commands and outcomes

- `git fetch origin` in both repositories; inspect current remote branches; application feature branch from current `origin/main`; clean engine fast-forward to `origin/app-stable`. No engine implementation edits.
- `node scripts/retain-progress.mjs generated/discard-baseline/stock-own-graveyard.json generated/discard-candidate/stock-own-graveyard.json generated/source-sacrifice-candidate/stock-own-graveyard.json`: passed all exact historical source hashes, population/eligibility equality and expected positive/negative transition checks.
- `python scripts/oracle-snapshot.py download --directory generated/oracle --manifest src/test/resources/progress/reference-snapshot.manifest.json`: exact replay passed size/SHA checks. The initial replay used the identical archived manifest; the retained manifest removes any runtime dependency on that archive.
- `.\dev.ps1 test --tests '*ResearchProgressTest'`: 5 focused tests passed. Includes disjoint node populations, overlapping cards, stale/missing assurance, unavailable external source, unsupported/enclosing structures and exact historical baselines.
- `.\dev.ps1 test ':assistant:installDist'`: **561 tests / 74 suites, 0 failures, 0 errors, 0 skips**. Includes existing mapping/missing/ambiguous/source-revision checks, regression benchmarks, assurance gates, native scenarios and full CLI/HTTP discovery parity.
- `pnpm build`: TypeScript (including Playwright files) and production Vite build passed. Progress and Primer are lazy loaded; the initial bundle is approximately 234 kB before gzip.
- `.\scripts\explorer.ps1 -Port 8797 -OracleSnapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz`: genuine one-step Windows build and foreground Kotlin service reached readiness. Default port 8787 and the no-snapshot launch remain supported.
- With `EXPLORER_URL=http://127.0.0.1:8797` and `EXPLORER_BROWSER_CHANNEL=msedge`, `pnpm exec playwright test`: **27 passed (2.2 minutes)**. Includes all eight transport/interface regressions, nineteen live research/dashboard checks, genuine discovery across all objectives, archived tools, direct Progress/Primer access, keyboard navigation, record drill-downs, unavailable historical baseline, desktop and 390px overflow checks. The first full browser run had 26 passes and one obsolete keyboard assumption (Commander first); the corrected test explicitly verifies Discover → Progress → Primer → Commander before continuing. That initial failed receipt/context remains under ignored `generated/progress/`; it was not relabelled as a pass. The full corrected suite was rerun successfully.
- Build current main in isolated `../mtg-progress-baseline` against the same clean engine; run each of the four CLI objectives on baseline and candidate with Judith, no search filter and JSON output. **All four complete reports are byte-identical**, including their 6,111-card inputs, eligibility, pathways, decisions, unsupported records and evidence. The [compact receipt](../src/test/resources/progress/discovery-stability.json) records exact report hashes and counts; raw outputs stay under ignored `generated/progress/`. Reproduce each with `mtg-assistant discover --commander 'Judith, the Scourge Diva' --objective <id> --json --output <path>`, then compare SHA-256. The isolated baseline was needed for simultaneous source/build separation, not a replacement development checkout.

The initial sandboxed Gradle attempt could not write the shared cache lock; the authorized build outside the restricted sandbox passed. The sandboxed preview did not reach readiness and was stopped; actual Edge checks ran outside that process sandbox. These attempts are not counted as passed verification. Standalone engine-suite execution, non-Windows launch, other browsers, exhaustive accessibility and other external source snapshots were not executed in this increment. Existing engine-backed application scenarios were executed by the full Kotlin suite.

### Visual review

Actual captures from the running Kotlin-backed dashboard, with the explicit snapshot selected: [desktop first screen](images/progress/desktop-first-screen.png), [390px first screen](images/progress/mobile-first-screen.png), [desktop complete page](images/progress/desktop.png), [390px complete page](images/progress/mobile.png). Both widths were inspected. The review moved the developer index below primary content and removed the duplicate return control from Progress/Primer. Headline figures, separate historical scope, uncertainties, obstacle accents and the capability table remain readable without exposing raw diagnostics by default. Tables can scroll inside the narrow viewport; browser checks establish no document-level horizontal overflow. No synthetic image or illustrated execution substitutes for these captures.

## Limits and next decision

Source fidelity across inherited definitions, scenario-to-ability execution coverage, SDK-versus-bespoke reuse, independent objective consumer reuse and reusable verification-pattern breadth remain unmeasured. There is no current-versus-historical trend across unmatched engine/catalog baselines. Default launch intentionally has no external denominator until a source is explicitly selected. Group representatives are samples of native locations, not independent Oracle adjudications. No new scenarios, objectives, integrations or engine changes were added.

The recommended next bounded investigation is a **current source-fidelity audit of the shared source-bound sacrifice representation in inherited definitions**. Use the existing Mogg Fanatic, Seal of Fire, Evolving Wilds and Spark Elemental source-led cases plus held-out Reckless Barbarian/Ball Lightning variants to check source identity, controller versus owner, battlefield departure versus arriving card type, and same-object requirements. Bind findings to current Oracle snapshots, native definitions and engine revision, explicitly separating author/source review from independent approval. Do not infer that existing stale bundles or the rest of the catalog are renewed. This family already demonstrates reusable breadth in the controlled comparison, while inherited source fidelity remains unmeasured. This increment does not start that investigation.
