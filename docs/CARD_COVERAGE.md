# Commander-scoped external card coverage

Scryfall supplies the ordinary Commander-legal Oracle identities that may need evaluation. Argentum supplies authored native mechanics. `audit catalog-coverage` compares those layers without changing discovery's three objectives, predicates or policies. It does not translate cards during a query.

Phase 2.8 uses observational identity enrichment by default. See [ORACLE_IDENTITY.md](ORACLE_IDENTITY.md) for the frozen matching rules, before/after results and independent metadata diagnostics. The Phase 2.7 experiment and its original counts below remain historical evidence.

## Obtain an explicit snapshot

JDK 21 and Python 3.11+ are required for this workflow. From PowerShell, use the existing machine environment helper when present (`. .\.env.ps1`). Downloads and reports belong under ignored `generated/`.

```powershell
# Deliberate update to Scryfall's current Oracle Cards version:
python scripts/oracle-snapshot.py download --directory generated/oracle

# Reproduce the archived Phase 2.7 version instead:
python scripts/oracle-snapshot.py download --directory generated/oracle --manifest reference/phase27/snapshot.manifest.json

.\dev.ps1 installDist
```

The download command requests bulk metadata once and the bulk file once. Manifest replay requests only the versioned bulk file, or reuses a checksum-verified local copy without network requests. It uses a descriptive User-Agent and Accept header, validates HTTPS Scryfall source hosts, downloads through a temporary file, and refuses to overwrite a mismatched existing version. Replay enforces the archived size and SHA256 before publishing the downloaded file. The manifest records the source URI, bulk ID, update/retrieval timestamps, format, compression and exact content hash. A local hash proves file integrity, not a server signature.

The current service provides gzipped JSONL. The Kotlin loader also accepts uncompressed JSONL and JSON arrays, with the encoding declared in the adjacent `<snapshot>.manifest.json`. It streams records, verifies the compressed file's size/hash, retains compact Oracle metadata, and builds deterministic ID/alias indexes. It performs no network requests. A truncated file, unsupported manifest or conflicting records for one Oracle ID cannot silently enter the eligible pool.

## Report coverage and inspect a bounded inventory

```powershell
$cli = '.\cli\build\install\mtg-assistant\bin\mtg-assistant.bat'
$snapshot = 'generated/oracle/oracle-cards-20261026210159.jsonl.gz'

& $cli audit catalog-coverage --commander 'Judith, the Scourge Diva' --oracle-cards $snapshot --json --output generated/judith-coverage.json
& $cli audit catalog-missing --report generated/judith-coverage.json --limit 20
& $cli audit catalog-missing --report generated/judith-coverage.json --name Goblin --offset 0 --limit 20 --json

# Optionally evaluate the existing native token query and attach the reviewed sample's status:
& $cli audit catalog-coverage --commander 'Judith, the Scourge Diva' --oracle-cards $snapshot --objective create-creature-tokens --translation-review reference/phase27/translation-review.json --json --output generated/judith-coverage.json
```

`--objective` accepts an existing objective name or `all`. Without it, coverage measures mappings without running mechanical queries. The snapshot is independent of ordinary `discover`; discovery continues to use the pinned authored catalog and its native eligibility rules. Current external metadata never silently changes a native definition or native discovery policy.

`catalog-missing` reads a saved report without constructing the native catalog. It supports case-insensitive name search, offset pagination, a limit of 1–200, and `--status` with one of the six status names below. Its JSON includes the total filtered count, offset, limit and bounded items. `--output` can save either command's result.

## Counts and uncertainty

- `eligible_oracle_cards` is the number of distinct, sufficiently identified Oracle records legal in the snapshot and within the selected native commander's full identity, including CR 903.5d basic-land-type restrictions. The selected commander is resolved through the existing inspection/selection mechanism. Counts include potential ordinary inclusions, not a validated 99-card deck.
- Double-faced, split, Adventure and flip faces remain attached to their one physical Oracle record. Mana colors and reminder text are not substituted for Scryfall's full `color_identity`. Related meld-part metadata is retained without making every face another inclusion.
- Known banned/nonlegal records are excluded. Missing identity/legality/face metadata and conflicting records for one Oracle ID are separately listed as unresolved pool records, outside the confirmed eligible denominator.
- Distinct Oracle IDs can share a face name. These remain distinct eligible cards. A shared-name diagnostic affects name-only native mapping, never eligibility. The real snapshot includes old spell names reused by Adventure faces.
- Companion roles, commander pairs, pre-game color choices and deck-quantity exceptions are annotated as unsupported construction roles. Ordinary inclusion does not grant those roles or modify the selected commander's identity. No singleton, deck-size, companion or outside-game validation is performed.

Every eligible record has exactly one combined status:

| Status | Meaning |
|---|---|
| `NATIVE_AVAILABLE` | A confident identity association (existing ID or guarded physical evidence) with agreeing required native identity/legality metadata. This does not certify all behavior. |
| `NATIVE_METADATA_UNRESOLVED` | A confident association exists, but required native eligibility metadata is missing or disagrees with the snapshot. Absence of the native Oracle-ID field alone no longer forces this status after confident enrichment. |
| `NATIVE_MECHANICALLY_UNRESOLVED` | A reliable native mapping exists, but requested discovery analysis encounters unsupported/partial structures or UNKNOWN pathways. Supported matching pathways can coexist with this status. |
| `NATIVE_DEFINITION_MISSING` | No Oracle-ID or name/face candidate was identified in the pinned native catalog plus extensions. It is not a mechanical `NO_MATCH`. |
| `AMBIGUOUS_MAPPING` | Conflicting claims or genuine multiple physical identities prevent a unique association. |
| `NATIVE_IDENTITY_REVIEW_REQUIRED` | A native physical/face candidate exists but the available identifying evidence is incomplete or inconsistent. |

The `mapping` field separately retains the association's confidence, method, evidence, native fields and metadata agreement/correction candidates. `confident_native_identity_associations` counts confidently linked definitions, including definitions with metadata disagreements; legacy `confident_oracle_id_matches` is retained as an alias for that count. `existing_native_oracle_id_matches` counts actual native-ID associations, while `enriched_identity_associations` counts the additional links. No IDs or colors are written back into native definitions. Shared face names alone no longer invalidate a complete, independently consistent physical association. Full-name ambiguity or conflicting claims still fail closed.

Identity categories reconcile as confident associations + review candidates + ambiguous mappings + missing definitions = eligible Oracle records. The six combined CatalogStatus counts also reconcile independently. Metadata discrepancies, mechanically unresolved outcomes and not-evaluated counts overlap those identity categories and must not be added to them. Without `--objective`, all records are not yet evaluated. `nativeWithoutExternalMatch` now contains native definitions lacking a confident external association, not every definition with an empty native ID field.

When requested, each objective reports supported matching pathways, UNKNOWN pathways and distinct unresolved native nodes. The existing extractor records an unsupported node in two diagnostic streams; the coverage audit deduplicates those by action/face/path/type. A supported token match alongside unsupported life-loss projection is reported honestly rather than certified as complete whole-card interpretation.

All six combined-status counts sum to eligible records. Eligible + excluded + unresolved pool records sum to distinct snapshot records; collapsed duplicate printings are reported separately. Missing cards with a supplied snapshot-bound translation review are split into investigated and uninvestigated counts. Not selected for research does not mean irrelevant.

The remaining experiment/reproduction recipes document Phase 2.7. Reproduce them with the accepted Phase 2.7 CLI at `fec9720`; its evidence script expects the original mapping categories. For current enriched reports and unchanged native-discovery verification, use the Phase 2.8 recipes in ORACLE_IDENTITY.md.

## Reproduce the bounded experiment

`reference/phase27/sample/selection.json` records two explicit existing-gap anchors followed by fourteen ordered research strata. Within each stratum the script chooses the lowest SHA256 of `snapshotSha256 + ':' + oracleId` among unused confirmed-missing records in the three commander reports. These lexical strata select research material; they never supply a production mechanical interpretation.

The historical pre-translation missing inventory can be reproduced with the coverage-only commit `9772904`, whose native catalog still has only the Phase 2.6 extensions. Use a separate clean checkout/worktree of that commit, follow normal pinned setup and install the CLI, then run the final branch's verification script with `-Repository <that-checkout> -Stage before`. The saved Phase 2.6 discovery reports and pre-translation coverage files belong under that checkout's ignored `generated/phase27/`. Use the archived snapshot. Sampling consumes only confirmed-missing rows; final published eligible/mapping totals use the final branch's shared-face handling. Timings/report hashes vary by run; the selected Oracle IDs and selection hashes must agree.

```powershell
# Paths below assume the original coverage-before files have been reproduced/copied locally.
python scripts/phase27-sample.py select --snapshot $snapshot --reports generated/phase27/coverage-before --output generated/phase27/reproduced-sample
python scripts/phase27-sample.py rulings --sample generated/phase27/reproduced-sample
.\dev.ps1 installDist :oracle-assay:installDist
python scripts/phase27-sample.py assay --sample generated/phase27/reproduced-sample --output generated/phase27/reproduced-assay
```

The rulings command fetches only the sixteen selected cards' rulings, caches them and spaces requests by 150 ms. Oracle information comes from the bulk snapshot, not thousands of individual requests. The Assay command executes the pinned supported `oracle-assay compile --file` path offline and archives stdout, decline/warning diagnostics and timings. Explicit UTF-8 stream properties preserve Windows punctuation. `mtgish-tooling` is not used.

Assay success is guidance, not a verified definition. Review the native structure and rulings, preserve authoritative metadata, author a Kotlin definition, validate/register through `Extensions`, and execute consequential engine scenarios. Only then mark it verified. The same definition is reused by every eligible commander and all mechanically applicable objective queries; future upstream Oracle-ID/name collisions still fail registration.

## Verification and retained evidence

```powershell
python scripts/test-oracle-snapshot.py
.\dev.ps1 test --tests dev.mtg.assistant.CatalogCoverageTest --tests dev.mtg.assistant.Phase27TranslationIntegrationTest --tests dev.mtg.assistant.Phase27TranslationScenarioTest
.\dev.ps1 build :assistant-cli:installDist
.\scripts\phase27-verify.ps1 -Snapshot $snapshot -Stage after
python scripts/phase27-evidence.py --snapshot $snapshot
```

`phase27-verify.ps1` runs three coverage reports, saves each first twenty missing cards, executes all six Judith/Krenko A/B/C discoveries, and invokes the existing `audit diff` for every before/after pair. `-CoverageOnly` refreshes coverage without repeating discovery. `phase27-evidence.py` independently checks external eligibility from the raw snapshot, reconciles counts, compares all preexisting eligibility/actions/facts/results/unsupported/support rows, checks the exact new token matches, and writes compact source representatives, report hashes, timings and test-suite receipts. It fails on unexpected changes.

Large bulk files, full coverage/discovery reports, build outputs and upstream sources remain ignored. Only compact fixtures, the sixteen reviewed cards/rulings, Assay evidence, reviewed status/provenance and the compact verification receipt are committed. See [the Phase 2.7 report](PHASE2_7_REPORT.md) for executed outcomes and limits.
