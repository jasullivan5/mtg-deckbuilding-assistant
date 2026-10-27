# Oracle identity enrichment

## Scope and rationale

The accepted Phase 2.7 baseline at `fec9720` has 16,999 physical native definitions. Only the four application extensions carry usable Oracle IDs; the 16,995 upstream definitions omit them. Replaying the exact archived Oracle Cards snapshot reproduces the 3 / 1 / 1 confident scoped associations and all published pool/missing totals. This is an identity-data gap, not evidence that upstream mechanics are absent.

Phase 2.8 adds OracleEnrichment over the existing NativeIndex and validated OracleSnapshot. It is observational: Catalog, native definitions, eligibility enforcement, action extraction and the three queries/policies are unchanged. No database, online query service, Oracle-text effect parser or new objective is introduced.

## Frozen matching rules

`physical-identity-v1` was committed as `e7741e9` before held-out selection. Each global native assignment records its method, evidence, confidence, original ID and candidates. Context binds the algorithm version, exact Argentum revision, Oracle snapshot SHA256 and canonical native identity-field fingerprint. Each invocation regenerates from locally validated inputs; no exported assignment table is imported as a trusted overlay. The context validator rejects stale snapshot/revision/native fields. These hashes identify inputs, not publisher signatures or proof of complete mechanical correctness.

1. Preserve a valid native Oracle ID. Report conflicts with independently consistent physical/printing evidence; never replace it. Malformed IDs and IDs absent from the snapshot require review.
2. An exact Scryfall printing ID present in the snapshot can independently identify the Oracle record. Conflicting physical identity evidence is quarantined. A different representative printing ID is not an Oracle-ID conflict.
3. Without IDs, require a globally unique complete physical name plus consistent native layout, ordered complete face names, cast costs and primary card types. Transform/flip backs do not have separately payable cast costs. Native Tribal/Oracle Kindred are the same primary-type vocabulary. Subtypes and Oracle-text similarity are not unchecked match shortcuts.
4. Resolve both directions globally before any commander filtering. Multiple native claims for one Oracle ID, or ambiguous physical names, stay unresolved. Complete Adventure/split/DFC/flip identities can coexist with a separate spell using one face name; a face alias alone cannot stand for the whole physical card.
5. Keep non-deck objects out of physical-name matching. Meld results remain excluded by the existing physicalCards filter; a meld front is its own physical card. Reprints retain their physical identity despite different representative printing metadata. Unsupported alternate-layout/cost representations remain review candidates, not missing mechanics.

Without a complete snapshot, NativeIndex retains conservative legacy diagnostic behavior; production coverage supplies the full snapshot for global enrichment. The exported artifact is reproducible evidence, not a hand-maintained mapping database. Changing mapping rules requires a new algorithm version and fresh review.

## Independent dimensions

Reports use schema 2. `mapping.association` contains confidence/method/evidence and the associated Oracle ID; `mapping.candidates` retains original native fields. Missing native ID fields remain missing. `mapping.metadata` separately retains native/external legality and full identity, discrepancies and `REVIEW_ONLY_NOT_APPLIED` correction candidates. `derivedNativeIdentity` is the existing SDK/app fallback with the root override removed, useful for diagnosing an incorrect override; it is neither authoritative nor used to admit discovery candidates.

A confident link can attach an existing native objective evaluation even when metadata disagrees, but only if that unchanged native report actually evaluated the card. Otherwise it remains `NOT_EVALUATED_NATIVE_ELIGIBILITY_DISAGREEMENT`. Missing/uncertain identity is never a mechanical nonmatch. Observations retain supported, conditional and partial matched pathways, UNKNOWN pathways, distinct unsupported nodes, mechanical nonmatch pathways and policy exclusions. No accepted pathway is guaranteed resolution, and no absence of accepted pathways is a universal assertion that a card can never satisfy the objective.

Identity categories reconcile independently: confident + review + ambiguous + missing = external eligible records. Metadata, mechanical uncertainty and not-evaluated counts overlap those categories. Six combined CatalogStatus counts also reconcile, with metadata taking precedence over a mechanical-unresolved combined label; objective details still expose both dimensions.

## Measured coverage

All counts below use the unchanged archived snapshot SHA256 `081a50ef82c767523f955e914153cf988ff045a72e3765b6aa2382567f9eb53a` and Argentum `ebf5bf2e9fbb80d13a556d75491dd462e10c206f`.

| Commander | Eligible | Confident before → after | Identity review | Ambiguous | Missing before → after |
|---|---:|---:|---:|---:|---:|
| Judith | 12,828 | 3 → 6,652 | 10 | 1 | 6,162 → 6,165 |
| Krenko | 7,552 | 1 → 3,914 | 5 | 1 | 3,631 → 3,632 |
| Oviya | 7,420 | 1 → 3,971 | 6 | 1 | 3,441 → 3,442 |

Separately, confident associations with native eligibility-metadata discrepancies number 579 / 466 / 445. Token-objective mechanically unresolved counts are 4,328 / 2,407 / 2,785, and not-evaluated counts are 6,753 / 4,104 / 3,893. These are overlapping dimensions, not additional identity categories. The old `confident_oracle_id_matches` count is retained as an alias; actual native-ID links remain 3 / 1 / 1, while additional enrichment links are 6,649 / 3,913 / 3,970.

The small missing increases are corrections of alias-only false candidates: Naktamun Lorespinner // Wheel of Fortune, Sign in Blood and Stensian Sanguinist // Exsanguinate for Judith; Naktamun for Krenko; Yavimaya Bloomsage // Channel for Oviya. Having the other physical card that shares a face name does not supply these definitions.

Globally, 16,958 native definitions have confident associations (four existing IDs and 16,954 guarded physical associations); 37 require review, two are ambiguous and two have no external record identified. Prototype/Omen/other alternate-layout representations and two cast-cost disagreements explain the review group. Pick Your Poison and Red Herring have genuine multiple Oracle identities. A-Orcish Bowmasters and A-The One Ring are not present under those digital-rebalanced names in this supplied snapshot; no `A-` stripping or paper-card substitution is performed. These counts are not a catalog-wide precision estimate or a mechanical verification claim.

## Reviewed evaluation

The compact development corpus has 20 source-led cases: 16 correct confident associations, two appropriately ambiguous physical-name records and two correctly missing definitions. Adversarial unit tests also cover conflicting IDs, one-to-many/many-to-one claims, face-only impostors, missing eligibility fields, unpayable costs, non-deck names, historical names and stale context.

After the rule freeze, six native examples were chosen by lowest SHA256 per layout (normal, split, transform, modal DFC, flip, Adventure), excluding development names and using only native projection fields, never matcher outcomes. One separate Scryfall collection request resolved their canonical set/collector numbers to Oracle IDs; these printing keys were independent evidence not used by the matching algorithm. All six printing IDs agree with archived Oracle records. Two baseline-confirmed missing cards were also held out. The eight held-out decisions pass: six correct confident associations and two correct missing definitions. Neither subset has observed missed or incorrect confident associations. Two development ambiguities still require manual review.

Fixtures, selection seed/hashes, independent-printing provenance, freeze commit and normalized source hash are in reference/phase28. The original CRLF working-tree freeze hash is retained alongside the portable LF Git-blob hash. This is post-freeze source-led evaluation, not external human adjudication or a statistical guarantee over all 16,958 links. The cases are no longer unseen for future algorithm versions.

## Concrete metadata findings

The archived snapshot and one bounded current five-card collection request on 2026-10-27 agree on these identities and color sets. Native fallback colors also recover the missing activation colors. The present native overrides are incorrect, rather than missing fields, alias errors or an observed source-version difference.

| Card | Native override | Snapshot/current identity | Hypothetical consequence of a reviewed correction |
|---|---|---|---|
| Heartwood Shard | Colorless | G | Remove from Judith/Krenko pools; remain allowed by Oviya |
| Hooded Kavu | R | BR | Remove from Krenko; remain allowed by Judith |
| Firescreamer | R | BR | Remove from Krenko; remain allowed by Judith |
| Sunforger | Colorless | RW | Remove from all three current native pools |
| Toxrill, the Corrosive | B | BU | Remove from Judith's pool |

Academy Manufactor is a separate real missing-override example; Afterthought Sentry has missing native Commander legality. Both are confidently identified while the missing fields remain unresolved. No correction is applied. A future application-owned metadata overlay is a separate engineering decision requiring source binding, review and eligibility regression tests; it must not imply mechanical completeness.

## Executed verification and reproduction

Windows/JDK21 baseline `dev.ps1 build :assistant-cli:installDist` executed 272 tests with zero failures/errors/skips. The baseline distribution was copied under ignored generated/phase28/baseline-cli before building new code. Focused mapping/coverage checks passed 56 tests before adding the held-out cases. The final full build passed 320 tests, zero failures/errors/skips, including all existing benchmark and native engine scenarios.

```powershell
python scripts/oracle-snapshot.py download --directory generated/oracle --manifest reference/phase27/snapshot.manifest.json
$source = 'generated/oracle/oracle-cards-20261026210159.jsonl.gz'
# Reproduce BEFORE with a preserved/built accepted fec9720 distribution:
python scripts/phase28-verify.py before --snapshot $source --dist generated/phase28/baseline-cli
.\dev.ps1 build :assistant-cli:installDist
python scripts/phase28-verify.py after --snapshot $source
python scripts/phase28-verify.py compare --snapshot $source
python scripts/phase28-review.py
python scripts/test-oracle-snapshot.py
```

Build the accepted baseline distribution before checking out this feature, or use a separate checkout explicitly if reproducing later; do not reset/discard local work to obtain it. The verification runner invokes the installed main class with the launcher's `-Xmx1g -XX:ActiveProcessorCount=4` JVM settings, logs each CLI argument list, and samples Windows process peak working set. It generates three coverage reports and all nine native discoveries per stage. The independent comparison rereads the raw bulk snapshot to validate each external pool, reconciles dimensions and asserts equality of the entire parsed native reports. All nine reports are identical: no native eligible card, action, fact, verdict, unsupported node, support record or explanation changed. The existing benchmark remains 15 matching reviewed cases plus one documented unresolved case, with zero confirmed mismatches. The three offline snapshot replay/integrity tests pass.

Optional export and bounded inventory inspection:

```powershell
$cli = '.\cli\build\install\mtg-assistant\bin\mtg-assistant.bat'
& $cli audit catalog-coverage --commander 'Judith, the Scourge Diva' --oracle-cards $source --objective create-creature-tokens --enrichment-output generated/identities.json --json --output generated/coverage.json
& $cli audit catalog-missing --report generated/coverage.json --status NATIVE_IDENTITY_REVIEW_REQUIRED --limit 20 --json
```

The measured pool/index/matching stage rose from 368–392 ms to 1,167–1,229 ms (about 0.8–0.9 s added). Whole coverage commands were 28.6–33.7 s before and 28.3–32.3 s after; cache/GC/catalog startup variation means those wall-time differences are not an enrichment speedup claim. Sampled peak JVM working sets were approximately 1.70–1.83 GiB before and 1.78–1.94 GiB after, not an isolated allocation-cost measurement. Native catalog loading remains dominant. The optional global artifact is 16,505,374 bytes; enriched report sizes are 33,984,958 / 19,993,224 / 19,890,466 bytes. Retained Phase 2.8 local data, including both sets of nine full discoveries and the preserved baseline distribution, totals about 831 MB; the 25,135,745-byte source bulk is separate. Large files remain ignored. Only compact fixtures and verification receipts are committed. Saved receipts: [controlled comparisons](../samples/phase2_8/verification.json), [mapping review and metadata findings](../samples/phase2_8/mapping-review.json), and [baseline tests](../samples/phase2_8/baseline-tests.json).

The smallest remaining obstacle is native eligibility-data correctness and review of the 37 layout/cost candidates. Identity linkage alone must not resolve those fields, certify mechanics or authorize broadening native discovery pools.
