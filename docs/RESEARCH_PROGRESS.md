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

Verification receipt and screenshots are recorded after the running Kotlin-backed application is checked. Current measurements and the stability comparison use maintained engine `84fb6147f5b6ae11445f215ce23c3a9bb6a72db1`, not the historical comparison engine. Application baseline is current main `b6e7cc7045a9cc01fbc76779d59c80d40bb2bb2e`. Candidate commands and outcomes are recorded in the delivery receipt below.

## Limits and next decision

Source fidelity across inherited definitions, scenario-to-ability execution coverage, SDK-versus-bespoke reuse, independent objective consumer reuse and reusable verification-pattern breadth remain unmeasured. There is no current-versus-historical trend across unmatched engine/catalog baselines. Default launch intentionally has no external denominator until a source is explicitly selected. Group representatives are samples of native locations, not independent Oracle adjudications. No new scenarios, objectives, integrations or engine changes were added.

The recommended next bounded investigation is a source-first review of one recurring native mechanic family across a small positive/negative/deceptive/held-out cohort, with exact Oracle, definition and engine bindings and an explicit independent review boundary. The inherited fidelity gap applies across the whole native catalog, and wider extraction reach would otherwise amplify an unmeasured source assumption. Choose the family from the dashboard's observed occurrence table. This increment does not start that investigation.
