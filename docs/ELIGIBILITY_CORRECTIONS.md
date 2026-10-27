# Reviewed Commander eligibility corrections

## Scope and implementation

Phase 2.9 starts at accepted main `7b28c068de23ba9476b9071d4d601cb511064a69`. Argentum remains `ebf5bf2e9fbb80d13a556d75491dd462e10c206f`; the Phase 2.8 matching algorithm is unchanged.

`eligibility/EffectiveEligibility.kt` defines a small reviewed record set, validates it once per Catalog, and resolves effective full color identity and Commander format legality. The packaged [plan](../cli/src/main/resources/eligibility/phase29.json) has seven approved records, the original NativeIdentity projection, canonical native-content fingerprint, expected and approved values, original full-snapshot EnrichmentContext, projected archived/current source records, source URI, date and review basis. It is ordinary versioned application data, not a database, exported enrichment table, live API or mass repair pipeline.

The runtime regenerates Phase 2.8 associations over the entire current native physical catalog against the seven reviewed physical source records. The full-snapshot approval evidence and external-name uniqueness are checked by the independent evidence script against the exact bulk data. The packaged subset does not claim to be a new complete Oracle snapshot: the manifest/context identify its full parent source, and the plan explicitly lists its reviewed source records. It cannot establish mappings for arbitrary new cards. Per-target original identifying fields and non-eligibility native content must remain unchanged; new competing native claims are rejected by the existing matcher. The historical global native fingerprint records the approval context; it is not required to match after a harmless metadata correction. Target validation and global claim checks are regenerated instead.

Approved records require a confident matching Oracle ID and unique target; matching names alone cannot authorize them. Snapshot/revision mismatch, changed identity/printing evidence, changed native content, a different expected value, unsupported values, source disagreement, missing evidence, and duplicate/conflicting records fail before any effective values are exposed. The deterministic failure behavior is to refuse the eligibility operation, not continue with a partially applied plan. Native registry lookup remains available for diagnosis. A new upstream pin always requires review, including apparently harmless updates. Within a reviewed revision, a native value already equal to the approved value is reported as `UPSTREAM_ALREADY_CORRECT`; proposals remain in the plan and do not apply. Another eligibility field may change only under its own approved record. Both missing-field admissions in this phase have explicit archived and independently retrieved current evidence. A coverage command supplying a different Oracle snapshot must re-review affected approvals; it cannot silently reuse them against the new source version.

The fingerprint canonicalizes serialized native objects and excludes eligibility fields, allowing reviewed metadata retirement without allowing changed scripts/costs/types/text/printing data to pass. Each approved field also checks its own expected/current value, the other field, and the saved native identity projection. Corrected full identity must include existing face and land restrictions plus explicit native mana-cost and color-indicator colors. Format legality remains independent: compatible colors cannot legalize a banned/nonlegal card.

## Eligibility consumers and native boundaries

- Catalog owns a lazy resolver; registration, CardDefinition objects, executable scripts and printings remain native.
- Discovery uses effective format legality and full identity for the commander and every physical candidate, then passes the original CardDefinition objects to the unchanged extractor and objective policies.
- Commander inspection and selection use the same resolver. Native commander designation, physical-front checks, validation, chosen-color/pairing guards and mechanical-review status remain separate. JSON and text inspection preserve original corrected values and correction decisions.
- Conventional search's identity and Commander-format projections use the same effective values for reviewed targets. Other formats, actual card colors, types, text, costs, printings and returned native objects remain unchanged. A conventional filter is still an additional filter, not a deck validator.
- Coverage continues to build its external pool from Scryfall and its selected commander's effective identity. It retains original identity association and original metadata disagreement; a separate effective agreement reports remaining discrepancies.

The raw `fullIdentity`, `commanderFormatLegality` and `nativeEligibility` helpers intentionally describe native metadata for diagnostics and validation. Production eligibility consumers obtain effective metadata from Catalog. New consumers must do the same. No Oracle-text effect interpreter, registry rebuild, native metadata mutation or engine-rule changes are introduced.

## Seven bounded approvals

A new seven-card Scryfall collection request is saved in [current-source.json](../reference/phase29/current-source.json); the exact archived records are in [archived-source.json](../reference/phase29/archived-source.json). Current and archived Oracle IDs, full identities and Commander legalities agree. Full Oracle IDs, exact native fingerprints, source links and retrieval timestamps are in the packaged plan and compact verification receipt.

| Card | Actual native value | Approved effective value | Actual impact on the three native pools |
|---|---|---|---|
| Heartwood Shard | Colorless | G | Remove from Judith and Krenko; retain for Oviya |
| Hooded Kavu | R | BR | Remove from Krenko; retain for Judith |
| Firescreamer | **B** | BR | Remains allowed by Judith and excluded by Krenko/Oviya |
| Sunforger | Colorless | RW | Remove from all three |
| Toxrill, the Corrosive | B | BU | Remove from Judith; its own commander selection now reports BU |
| Academy Manufactor | Missing authoritative full identity | Colorless | Newly admitted to all three |
| Afterthought Sentry | Commander legality UNKNOWN | Commander legal | Newly admitted to all three; existing full identity is colorless |

The assignment and Phase 2.8 narrative described Firescreamer's original override as R. Actual pinned source/projection and both saved receipts show B. Its correction is still BR, but it was already outside Krenko's pool. No removal is fabricated. The seven native mechanics remain bounded by the same extractor support and engine definitions; these approvals do not certify complete card behavior.

## Report contract

Coverage schema 3 preserves `mapping.association`, `mapping.candidates` and `mapping.metadata` as original native evidence. `mapping.effectiveMetadata` records original/effective identity and legality, Oracle/native identity, source-backed correction record and APPLIED/already-correct decisions. `mapping.effectiveAgreement` compares the effective values with the report's external snapshot. Original candidate proposals in `mapping.metadata` retain their historical review-only disposition; approval is a distinct explicit record.

The combined CatalogStatus uses effective metadata agreement, while original discrepancies remain counted under `native_metadata_discrepancies`. New `effective_metadata_discrepancies` and `approved_corrections_in_external_pool` counters do not erase history. Metadata, identity and mechanical counts remain overlapping dimensions. Root `eligibilityCorrections` also lists affected cards outside the external eligible pool, so removal cases such as Sunforger remain inspectable. Its before/after field checks hold the selected effective commander's identity fixed; actual historic pool changes are proven by the nine controlled reports.

Identity associations and externally eligible IDs must be unchanged for Judith/Krenko/Oviya. Missing definitions remain missing, and no uncertainty becomes a mechanical NO_MATCH. A corrected field may move a combined status from metadata-unresolved to mechanically-unresolved; the original objective support remains visible.

## Executed verification

The fresh baseline `dev.ps1 test --rerun` passed 320 tests. The final full build passed 356 tests with zero failures/errors/skips; the benchmark retained 15 MATCH and one documented UNRESOLVED case, with zero confirmed mismatches. Three offline snapshot integrity tests passed. The complete source replay checks, benchmark and nine-context controlled comparisons are recorded in [verification.json](../samples/phase2_9/verification.json) and [baseline-tests.json](../samples/phase2_9/baseline-tests.json). These receipts include final counts, exact arguments, source/report hashes, timings and retained-evidence row counts.

Seven real approvals and the synthetic positive/adversarial cases were used during design; no new held-out accuracy claim is made. The unchanged Phase 2.8 held-out identity tests remain part of the full suite. Tests cover corrected and missing metadata, source/ID/target ambiguity, duplicates, conflicting/stale approvals, already-correct upstream values, changed content, illegal formats, multi-face/land/indicator restrictions, other-field drift, no approval, commander CLI selection/inspection, search filters, coverage history and every established CLI objective.

Reproduce from a clean accepted baseline without resetting/discarding local work:

```powershell
$env:JAVA_HOME = 'path-to-jdk21'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
python scripts/oracle-snapshot.py download --directory generated/oracle --manifest reference/phase27/snapshot.manifest.json
$source = 'generated/oracle/oracle-cards-20261026210159.jsonl.gz'
# Before changing from accepted main, execute tests/build and preserve its installed distribution:
.\dev.ps1 test --rerun
.\dev.ps1 installDist
New-Item -ItemType Directory -Force generated/phase29 | Out-Null
Copy-Item cli/build/install/mtg-assistant generated/phase29/baseline-cli -Recurse
# Then use this branch's runner with the preserved baseline CLI:
python scripts/phase29-verify.py before --snapshot $source --dist generated/phase29/baseline-cli
.\dev.ps1 build :assistant-cli:installDist
python scripts/phase29-verify.py after --snapshot $source
python scripts/phase29-evidence.py --snapshot $source
python scripts/test-oracle-snapshot.py
```

The runner executes three full coverage reports, nine full discovery reports and, after correction, all nine existing `audit diff` comparisons. Evidence reconciliation independently recomputes external pools from the raw bulk, checks complete original identity artifacts, validates the seven approvals, asserts the exact allowed removals/admissions, and compares every retained action/fact/result/unsupported/support row. It also checks all uncorrected eligibility rows, native input lists, source contexts and relevant pipeline denominators. Eligibility-driven removed/added mechanical rows are separately counted, not hidden by whole-report normalization.

After adding the final observed-source guard, the three coverage reports were refreshed with `python scripts/phase29-verify.py after --coverage-only --snapshot $source`; the nine native reports remain valid because that final guard affects only coverage source validation. The final full suite also executes real CLI discovery. Recheck timings are retained separately.

One extra baseline identity-only coverage invocation was executed in this development run to export its global association artifact after the initial runner capture; the current runner exports it during both before/after stages automatically.

## Bounded discrepancy review and remaining limits

The global confident-association diagnostics contain 705 missing authoritative identities, 48 present identity disagreements, and 353 missing Commander legalities; these overlap. There were no native/external Commander-legality disagreements among those confident associations in the archived input. Commander-scoped counts overlap each other and are not global severity totals.

Eight unapproved representatives are retained in the receipt: Bloodletter Quill and Boros Fury-Shield (additional rules-text colors); Brutal Cathar (multi-face identity disagreement); Abzan Devotee (broader identity disagreement); Aberrant Mind Sorcerer and Accomplished Automaton (missing full-identity overrides); Academic Ascent and Aerid Konstrari (missing legality). They remain uncorrected. Complete native scripts/costs and current authoritative data need individual review before proposing fixes. Missing overrides conservatively exclude cards; an incorrect present value can cause illegal inclusion. The multicolor source identities are not automatically permission to change either field. Multi-face inconsistencies are a subset of identity disagreements, not an extra disjoint total.

The current/archived check found no source-version difference for the seven activated records; unreviewed discrepancies cannot all be attributed to source churn. Independent evidence and review remain necessary. The 37 unresolved identity candidates are still outside correction authorization.

Performance and exact pool/coverage results are in the receipt and the measured-results section below. Runtime is offline and validates the small plan once per Catalog. Whole-command timings include catalog/JVM/GC effects; some initial before/after timings overlap test/build activity, so no isolated wall-time speedup or memory causation is claimed. Large bulk data, full reports, baseline distribution and caches remain ignored; only compact sources, approval records, tests and receipts are committed.

The next bounded correctness target is an individually verified remaining illegal-inclusion discrepancy, such as Bloodletter Quill's off-color activation identity. This phase stops after the seven demonstrated approvals; no automatic expansion is implemented.

## Measured results

| Commander | Native eligible before → after | External eligible (unchanged) | Confident associations (unchanged) | Original discrepancies → effective remaining | Approved cards in external pool | Token mechanically unresolved | Not evaluated |
|---|---:|---:|---:|---:|---:|---:|---:|
| Judith | 6,106 → 6,105 | 12,828 | 6,652 | 579 → 575 | 4 | 4,330 | 6,751 |
| Krenko | 3,467 → 3,466 | 7,552 | 3,914 | 466 → 464 | 2 | 2,409 | 4,102 |
| Oviya | 3,547 → 3,548 | 7,420 | 3,971 | 445 → 442 | 3 | 2,787 | 3,891 |

The original discrepancy counts remain 579 / 466 / 445; they are not overwritten by the effective counts. Identity review remains 10 / 5 / 6, ambiguity 1 / 1 / 1, and genuine missing definitions 6,165 / 3,632 / 3,442. Mechanical uncertainty and not-evaluated columns overlap identity/metadata categories. Newly admitted native cards can add UNKNOWN/unsupported evidence: token mechanical-unresolved counts rose by two per commander, while not-evaluated counts fell by two. No improved mechanical completeness is claimed.

Final resolver validation measured 463–556 ms per catalog. The final coverage-only refresh took 29.8–35.8 s per command, with sampled peak JVM working set 1.74–2.01 GiB. Initial whole-command before/after runs are separately recorded and partly overlap other verification activity; they are not an isolated performance experiment. The packaged plan is 35,633 bytes. Retained local Phase 2.9 generated evidence is approximately 906 MB, including both complete discovery sets and the preserved baseline CLI; the bulk source and prior-phase data are separate. No archive is produced and the large files are ignored.

All nine actual pool changes reconcile: Judith removes Heartwood Shard, Sunforger and Toxrill; Krenko removes Heartwood Shard, Hooded Kavu and Sunforger; Oviya removes Sunforger. Each admits Academy Manufactor and Afterthought Sentry. All other eligibility rows retain their native decisions, and every retained mechanical evidence row is identical. The existing audit diffs were also executed for all nine contexts.
