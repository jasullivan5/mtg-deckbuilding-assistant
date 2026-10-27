# Phase 2.7 — commander-scoped coverage and bounded native translation

## Repository and reproducibility

Work is on `feature/commander-card-coverage`, based on `463ce86f288db73486dcd0a712f4b8469bf3cd7f`. GitHub PR #1 was inspected through GitHub and confirmed merged; that base includes Phase 2.6 commander inspection and Oviya. The original checkout was clean and matched `origin/main`. No pending PR was merged or unrelated migration cleanup included.

Argentum remains exactly `ebf5bf2e9fbb80d13a556d75491dd462e10c206f`; its source checkout is clean. The application uses the existing native DSL, extension validation, extractor, predicates and three objective policies. Objective A retains its documented legacy projection.

Scryfall Oracle Cards bulk ID: `27bf3214-1271-490b-bdfe-c0be6c23d02e`. Source update: **2026-10-26 21:01:59.922 UTC**. Exact source: `https://data.scryfall.io/oracle-cards/oracle-cards-20261026210159.jsonl.gz`; 25,135,745 compressed bytes, 39,049 distinct Oracle records. SHA256: `081a50ef82c767523f955e914153cf988ff045a72e3765b6aa2382567f9eb53a`. Retrieval time and encoding are in [the replay manifest](../reference/phase27/snapshot.manifest.json).

Commands and data contracts are in [CARD_COVERAGE.md](CARD_COVERAGE.md). [The compact verification receipt](../samples/phase2_7/verification.json) contains exact report hashes/sizes, counts, source representatives, timings, native metadata diagnostics, all six regression comparisons and test-suite receipts. Large source/report files remain local under ignored `generated/`.

## Coverage results

The independent Python check reads the raw bulk snapshot without application/native registry code, confirms each selected commander's source identity/legality, and reproduces every reported eligible Oracle ID.

| Commander | Eligible | Confident Oracle-ID matches | Metadata unresolved | Ambiguous mapping | Confirmed missing identified definitions |
|---|---:|---:|---:|---:|---:|
| Judith, the Scourge Diva | 12,828 | 3 | 6,644 | 19 | 6,162 |
| Krenko, Mob Boss | 7,552 | 1 | 3,914 | 6 | 3,631 |
| Oviya Pashiri, Sage Lifecrafter | 7,420 | 1 | 3,972 | 6 | 3,441 |

Every row reconciles. All confident ID matches in these scoped reports are application extensions; upstream name/face candidates without usable Oracle IDs remain unresolved, rather than inflating confidence or missing counts. The three reports have zero unresolved external-pool records. Their excluded counts are 26,221 / 31,497 / 31,629; eligible + excluded = 39,049.

For the requested token-objective analysis, Judith's three reliable native mappings comprise one `NATIVE_AVAILABLE` and two `NATIVE_MECHANICALLY_UNRESOLVED` records. The latter are Judith and Bitterblossom: the shared extractor has an unsupported non-token-operation projection, while Bitterblossom still has one supported token match. Its life-loss behavior is engine-tested; an extractor limitation does not imply absent engine support or negate that supported pathway. Krenko and Oviya each have one `NATIVE_AVAILABLE` record. Missing mappings are never assigned mechanical `NO_MATCH`.

Of the missing records, investigated / uninvestigated counts are Judith **11 / 6,151**, Krenko **3 / 3,628**, Oviya **4 / 3,437**. The remaining unselected cards have not been established irrelevant. The two completed translations moved out of the missing inventory: Judith missing decreased by two, Krenko by one, Oviya by zero.

Source representatives verify colorless Sol Ring in all three pools; BR hybrid Rakdos Guildmage and mono-black Beseech the Queen only in Judith; red Shatterskull's two faces counted once for Judith/Krenko; green Bala Ged Recovery and Dryad Arbor only for Oviya; and banned Jeweled Lotus in none. Fire // Ice, Hallowed Fountain and all-color Transguild Courier are excluded from all three. The Prismatic Piper is an ordinary colorless inclusion with unsupported pairing/chosen-color commander roles explicitly annotated. Burakos's Background role does not grant another commander. Dragon's Approach retains its unvalidated deck-quantity exception. Synthetic tests cover missing metadata and the independent basic-land-type restriction.

The external pool and native eligibility are **not equivalent**. Diagnostic unique-name candidates in Judith include 467 missing full-identity overrides and 111 unknown native legalities; Krenko has 408 / 62 and Oviya 393 / 56. Those categories can overlap and are not authoritative ID mappings. Native candidate/source identity disagreements include Firescreamer (`B` versus `BR`), Hooded Kavu (`R` versus `BR`) and Heartwood Shard (`[]` versus `G`): Oracle activated colored-mana symbols matter. The native/source inputs and examples are retained separately; upstream metadata was not silently repaired.

Distinct physical Oracle IDs also share spell-face names in the real snapshot, including Lightning Bolt and the Adventure face of Emeritus of Conflict. These remain in the eligible denominator. Their overlapping names make name-only native associations ambiguous; a reliable Oracle-ID match still takes precedence. Synthetic collision fixtures verify both behaviors.

Reverse diagnostic comparisons find 20 / 13 / 13 exact-name native candidates included by native eligibility but excluded by the external snapshot. For example, Heartwood Shard's native empty identity admits it to Judith/Krenko while the source's green identity excludes it. These remain name-based diagnostics, not authoritative ID mappings. Same-named token/theme objects are distinct source identities and are excluded from physical-card diagnostic lookup rather than silently replacing a real card by name order.

## Translation experiment

The reproducible selection is two explicit predeclared catalog-gap anchors plus fourteen ordered strata with a snapshot/Oracle-ID SHA256 tie-break. All sixteen cards came from the confirmed-missing inventory. Lexical strata are research selection only; Transpose demonstrates why mentioning an opponent does not mean that opponent receives the created token.

Every selected card has its bulk Oracle object and all published rulings archived. The pinned supported Assay `compile --file` command was actually executed for all sixteen: **four compiled and twelve declined**. The four complete proposed native trees were reviewed; declined cards emitted no complete definition, so decline locations and applicable existing SDK structures were reviewed instead. Full diagnostics and proposed trees are in [the Assay evidence](../reference/phase27/assay/results.json); individual sources, rulings and selection hashes are in [the sample](../reference/phase27/sample/selection.json). This uses Argentum Assay, not deprecated mtgish-tooling.

| Sample | Assay | Reviewed disposition |
|---|---|---|
| Krenko's Command | Compiled | Authored, registered, structurally validated and engine-tested |
| Bitterblossom | Compiled | Authored, registered, structurally validated and engine-tested |
| Soul of Zendikar | Compiled | Assisted authoring ready; graveyard activation/cost scenarios still required |
| Mortarpod | Declined: Living weapon | Existing create/attach/granted-ability primitives; grammar gap; manual scenarios required |
| Rakdos Guildmage | Compiled | Assisted authoring ready; discard/target/next-end-step tests still required |
| Pact of the Titan | Declined: next upkeep | Delayed-trigger/payment/game-loss primitives exist; whole obligation must be authored and tested |
| Espers to Magicite | Declined | Unresolved complete exile/reflexive binding; artifact-only copy is a deceptive noncreature-token case |
| Transpose | Declined | Unresolved cast-origin/rebound/child-trigger integration; token receiver is not the mentioned opponent |
| Burakos, Party Leader | Declined | Party-cardinality/type/construction expression unresolved; Treasure is not a creature token |
| Solemn Doomguide | Declined | Existing graveyard granted-unearth pattern; grammar gap; authoring/scenarios still required |
| Sigil of Myrkul | Declined | Existing reflexive-trigger primitives; delayed target window must be preserved and tested |
| Dread Linnorm // Scale Deflection | Declined: multi-face compiler | Native Adventure packaging exists; compiler limitation differs from SDK absence |
| Undead Servant | Declined | Existing named graveyard count/dynamic creation; resolution-time/self-in-graveyard scenarios required |
| Touch of the Void | Declined | Actual-damage-conditioned exile replacement remains unresolved; unconditional marking would be unsafe |
| Cloakwood Swarmkeeper | Declined | Batch token-entry/counter primitives; source-reviewed token consumer, not creator |
| Pain's Reward | Declined | Additional mechanical support required: existing OpenLifeBid fixes an opening bid and two participants/caster-only payoff |

The [review ledger](../reference/phase27/translation-review.json) separates grammar gaps, confirmed SDK mismatch and unresolved integration, with exact pinned source references. It records two verified extensions, nine assisted-authoring candidates, four unresolved cases and one support-required case. Assisted-authoring readiness is not mechanical verification. Source-led negative observations about untranslated cards do not become executed objective nonmatches.

Krenko's Command uses the native fixed-count token effect. Bitterblossom uses a controller-upkeep trigger with unconditional life loss followed by creation. Both preserve original-printing metadata, current Oracle identity/full color identity/represented format legalities and dated application provenance. Krenko's Command qualifies for Judith and Krenko; Bitterblossom qualifies for Judith and Marrow-Gnawer. Neither is forced into death-generation or reanimation. Registration rejects future upstream Oracle-ID/name collisions rather than overwriting definitions. Discovery contains no new card-name exceptions, text parser or objective.

## Executed verification

- Clean Phase 2.6 baseline: `.\dev.ps1 test --rerun`, **240 passing tests**; all six Judith/Krenko A/B/C snapshots saved before extension registration.
- Focused coverage/translation checks, followed by final `.\dev.ps1 build :assistant-cli:installDist`: **272 tests, zero failures/errors/skips**. New coverage logic has 16 tests, translation integration five, and actual native engine execution eleven scenarios.
- Native scenarios verify both token controllers/owners and characteristics, casting mana/timing rejection, controller-only upkeep, source removal after triggering, mandatory life loss at one life, and protected zero life where life loss is not a payment.
- `python scripts/test-oracle-snapshot.py`: **three offline integrity/replay checks passed**. Exact-version cached replay also executed successfully without another bulk download.
- The pinned `:oracle-assay:installDist` build and sixteen local-file compile invocations executed. No upstream source changes were made.
- `.\scripts\phase27-verify.ps1 -Snapshot <archived snapshot> -Stage after` executed all six full discoveries, existing `audit diff` commands and bounded missing inventories. Coverage was refreshed with `-CoverageOnly` after the reporting corrections.
- `python scripts/phase27-evidence.py --snapshot <archived snapshot>` independently reproduced all three external pools, reconciled every count and confirmed **zero unexpected regressions**.
- Bash syntax checking of the updated routine verification gate, Python compilation checks and `git diff --check` passed. CI now includes the new coverage/integration/engine cases and offline downloader tests; these local results do not claim remote CI completion.

Across all six before/after comparisons, preexisting eligibility rows, actions, facts, results, unsupported records and structural-support evidence are exactly equal after filtering the two added catalog names. No preexisting mechanical verdict, policy, condition or explanation changed. Both new catalog rows appear in each report's eligibility inventory; only red Krenko's Command is eligible for Krenko, while both are eligible for Judith. Existing A/B accepted results are unchanged. Objective C gains two Judith matches and one Krenko match. Bitterblossom adds one explicit unsupported life-loss projection; it does not alter old interpretations. Existing frozen benchmarks passed in the full suite.

## Performance and limits

On this machine, final coverage with token analysis took **31.332 s Judith, 28.844 s Krenko, 29.969 s Oviya**, including process startup and writing JSON. Snapshot integrity/loading took 2.535–2.641 s; pool/index/matching 0.403–0.486 s; native catalog loading 18.163–18.520 s; optional existing objective evaluation 6.292–8.201 s. Full report sizes were 25,352,935 / 15,948,231 / 15,793,772 bytes. These are measured runs, not performance guarantees. Native catalog loading dominates the added external indexing work. A bounded inventory query reads a saved report and avoids native catalog loading.

Only deliberate snapshot update/replay and bounded sample-ruling retrieval use the network. Discovery never downloads or translates a catalog. No database, web service, persistent translation queue, ranking, deck optimization, strategic package discovery or new objective was added.

Counts reconciling do not establish comprehensive discovery accuracy. Unassessed missing cards, uncertain native mappings, incomplete objective projection and unexecuted interactions remain separate limitations. The smallest useful next improvement is a reviewed, versioned Oracle-ID/required-metadata enrichment process for the native catalog, with the shared-face collision fixtures retained; this would reduce the largest unresolved category before expanding translation volume.
