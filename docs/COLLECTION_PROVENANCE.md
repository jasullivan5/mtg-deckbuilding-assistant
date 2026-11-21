# Union targets and action-local collection provenance

Application baseline: `58c58deac9da5bda012a71480a4fd129b788422d`. Clean maintained engine: `0878d800edbec56f0c82dac080ae4fec32816b5f` on `app-stable`. No engine changes or upstream integration are part of this increment. The detached `mtg-collection-baseline` build and candidate use this exact same engine.

## Source controls

[Frozen requirements](../src/test/resources/regression/collection-movement-sources.json) retain printing and Oracle identities, retrieved Oracle text and rulings, exact native definition paths/hashes, and expected behavior. These are requirements frozen before production interpretation changed; passing author tests does not independently qualify any whole card.

| Native control | Physical printing | Bounded relationship |
| --- | --- | --- |
| Savior of Ollenbock | VOW 34; `ba77e83b-1846-4c42-bea0-2e304429fbe0` | One optional training-exile target chosen from battlefield or graveyard clauses; independent source-linked owner return |
| Come Back Wrong | DSK 86; `72ee3b45-aa4e-4c5b-a9e6-608bfbd93f8b` | Chosen-target gather → destruction result → nontoken creature selection → battlefield movement; delayed sacrifice retained as unsupported continuation |
| Cryptic Coat | MKM 50; `0c3d7e2c-a104-4757-9984-fb31088f92c4` | Top-one library gather → tracked cloaked arrival → indexed attachment to that resulting permanent |
| Kheru Lich Lord | KTK 182; `e7fbca8d-34a3-4df1-986a-36fca142a758` | Optional upkeep payment → graveyard creature pool → random-one selection → tracked return → immediate keyword consumers |

Relevant public sources include the [VOW release notes](https://media.wizards.com/2021/downloads/VOW_Release_Notes/EN_MTGVOW_ReleaseNotes_20211021.pdf), [MKM release notes](https://media.wizards.com/2024/downloads/MKM_Release_Notes_82nnDTWVBdD/EN_MTGMKM_ReleaseNotes_20240125.pdf), and the printing-specific Scryfall source/ruling URLs retained in the fixture. Existing exile timing, legacy-helper failures, archived receipts and qualification remain unchanged.

## Shared interpretation

`ActionEvidence.unionTargets` retains one native target requirement, its independent zone/filter clauses, binding, optionality/count, announcement checkpoint, consuming operation paths and existing movement fact IDs. The alternatives are an OR for one selected object. Selection, continuing legality, exile success and future return remain separate prerequisites. Non-controller choosers, dynamic/correlated counts and additional restrictions remain UNKNOWN. `UnionTarget.observeSelection` records current selected generations, their actual zone and owner/controller, and zone-clause indices. Zero optional targets yield no subject; old generations are never recaptured through raw IDs. Its legality field remains UNKNOWN: actual acceptance is established by native transaction tests, and later legality remains a native checkpoint. Existing flat `ObjectScope` uncertainty is preserved for consumers that cannot use clause-specific evidence; no objective admission policy changes.

`ActionEvidence.collectionStages` and `collectionUses` are emitted during the existing effect walk. Frame-local variable bindings point to versioned producer IDs; branches copy the bindings and new actions start empty. The records reference action paths and movement facts rather than copying native effect trees. They distinguish gathered members, selected subsets, positive filters, unresolved complements, aliases, retained original inputs and native moved-result members.

Membership has a potentially empty lower bound; a supported fixed upper bound does not create a member. Selection records separately retain the native selection mode and chooser, so an exactly-one request differs from an up-to-one request even when the actual source pool can be empty. An empty branch and an out-of-range indexed reference cannot establish an object. Reading the original input after movement does not imply that every member moved. A filtered movement retains its original membership and a separate result binding.

The same provenance supports movement, explicit variable readers, bounded indexed movement/attachment/keyword consumers, iteration declarations and collection-dependent guards. Nested or deferred relationships without a demonstrated producer in the current scope remain UNKNOWN. Engine-owned source-linked piles and duration returns are not ordinary action-local collections. Savior's richer whole linked-pile relationship retains its previous UNKNOWN; the new target-clause evidence establishes a useful narrower relationship without removing that uncertainty.

`CollectionStage.observeMovement` reads supplied native event slices and original `ObjectRef` generations. It records native stored IDs separately from observed requested-zone arrivals and currently surviving arrivals. Printed names and raw IDs never advance an object generation. A replacement can redirect movement, so a native moved-result variable is not a guarantee of arrival in the requested zone. The observation helper does not simulate filters, replacement effects or collection membership. Unsupported token/meld and ambiguous identity semantics remain outside the bounded original-card model.

## Native and structural evidence

`CollectionProvenanceTest` protects clause restrictions, optionality, fact references, distinct result bindings, fixed cardinality, source hashes, branch/action isolation, complements and old action decoding. `CollectionMovementScenarioTest` uses real native casts, training, target validation, stack resolution, independent same-name collection contexts and upkeep payment for the four controls. Separate isolated executor scenarios inspect the actual stored subset after a mixed destruction batch and positive filtering; these are explicitly not real-card scenarios.

The first focused attempt encountered stale compiled types while the observation implementation was being added. The next run passed 16 of 17 controls and exposed an application observer that incorrectly required battlefield last-known information for library departure. Native Cryptic Coat movement/attachment was correct. The observer was corrected; 22 subsequent focused controls passed. Additional cardinality guards and full compatibility verification are recorded in the final receipt.

## Verification and retained measurements

Verified implementation: `45d5275208d40a5cba8ec17e149a981644472ad0`, with maintained engine `0878d800edbec56f0c82dac080ae4fec32816b5f` and implementation fingerprint `0b0439c946d9d3f1a60581a8250a298f37bea8ee465ca8e5c64c12b080045b9f`. The delivery commit adds documentation and compact evidence after this tested implementation. [Verification receipt](evidence/collection-provenance-verification.json) · [native observations](evidence/collection-provenance-native-observations.json) · [census](evidence/collection-provenance-inventory.json) · [18 comparisons](evidence/collection-provenance-discover-comparison.json).

| Check | Observed result |
| --- | --- |
| Complete Kotlin suite, JDK 25.0.4.1 | 842 passed; zero failures, errors or skips; 118 classes |
| New bounded evidence | 13 structural tests and 13 native scenario tests: 11 real-card scenarios, two isolated executor scenarios |
| Bootstrap / installed distribution / CLI | Passed; bootstrap guards 17, snapshot helper 3, catalog-comparison guards 5 |
| Current installed catalog gate | Passed. Frozen historical fingerprint is different; it was preserved rather than refreshed |
| Supported historical decoding | Three historical-wire tests pass; installed CLI identical-snapshot diff has zero changes; old action fields default new evidence to empty |
| Frontend / browser | Frontend build and 8 interface checks pass; 18 live Edge checks pass on the pre-optimization implementation, followed by a passing final-revision live smoke check |
| Normal Explorer | Exact clean application/engine SHAs confirmed by health; `MaxHeapSize=1073741824`; API inspection exposes the new evidence for all four controls |
| Final controlled Discover | All 18 pairs preserve every pre-existing report field, including eligibility, facts, links, targets, verdicts, prerequisites, unsupported structures and assurance |

The source cohort exercises both starting zones, optional zero targets, illegal targets, owner/controller distinctions, actual result-member attachment and keyword consumers, failed destruction, filtered subsets, empty libraries and separate same-name collection contexts. A creature selected on the battlefield and subsequently destroyed cannot become a new graveyard selection of the same original card. A separate structural control prevents a reflexive selection from inheriting the root union binding even when its target descriptor is reused. Native execution demonstrated these bounded expectations without an engine correction. Delayed sacrifice and other whole-card behavior remain outside this evidence.

The candidate's 18 complete reports are also byte identical to the pre-optimization reports. The complete integrated interpretation stream has the same SHA-256 before and after replacing repeated growing-fact-list searches with the current instruction's fact range: `4c7b3f76b050e5fce18388cef2e7a23be287ec011978518dfc293f8c222b989d`. Timings came from overlapping local checks and are not a controlled speed benchmark. The change avoids an additional growing-list scan; it adds no movement state or catalog interpretation pass.

Judith's effective population remains 7,728, Krenko's 4,308, and Kenrith's 20,143 for each of their six objectives. The only added report fields are `ActionEvidence.unionTargets`, `collectionStages` and `collectionUses`. Reanimation, intrinsic input supply, attachments, chosen targets, counter interpretation and historical qualification are unchanged. No native rules-engine or era-wide suite rerun is claimed because the engine is unchanged; fresh native execution is exercised in the complete application suite.

Bulk JSONL inventories, the baseline/final 36 reports, 18 additional pre-optimization reports, logs and installed libraries remain under ignored `generated/collection-movements/`. Source guards retain both original byte hashes and canonical LF hashes. One installed-build attempt caught an incorrect zone-key field in the new observer; it was corrected before final verification. The earlier observer failure and interim complete-suite passes of 839 and 840 tests are retained as verification history, not substituted for the final pass.

The native-only census uses the existing face-aware `LegacyExileInventory.java` against the exact installed baseline, supplemented with the ten application-native extension declarations. `CollectionProvenanceInventory.java` streams one production extraction per physical card at 1 GiB. `inventory-collection-provenance.py` distinguishes physical roots, authoring face containers, native declarations, actions and producer-consumer records; matching variable text alone never creates a relationship. `compare-collection-provenance.py` permits only the three additive evidence fields and requires all existing report content to remain equal.

The measured integrated population is **21,699 physical cards**, comprising 21,689 engine definitions and ten application extensions, with **22,487 authored face containers**. Containers include aggregate roots and are not a count of printed card faces. These are catalog observations, not source review or qualification counts.

| Native shape | Physical cards | Declarations | Action/ability containers |
| --- | ---: | ---: | ---: |
| Explicit union filters | 12 | 12 | 12 |
| Union filters spanning distinct zones | 8 | 8 | 8 |
| Gather producers | 3,888 | 4,683 | 4,226 |
| Collection selections | 2,678 | 3,154 | 2,821 |
| Collection filters | 300 | 394 | 309 |
| Collection movements | 3,666 | 5,045 | 3,937 |
| Named moved-result producers | 183 | 198 | 193 |
| Indexed pipeline target declarations | 286 | 370 | 298 |
| Explicit `FromVariable` source readers | 7 | 7 | 7 |
| Collection iterations | 125 | 132 | 126 |
| Collection-dependent conditions | 225 | 242 | 236 |

The walker emits 13,140 collection stages and 8,446 consumer records. Of the consumer records, 3,348 have supported conditional bindings and 5,098 remain UNKNOWN. **63 supported consumers refer specifically to native moved-result producers**, across **47 physical cards, 48 face containers and 52 actions**: ten iterations, twenty conditions, eight indexed consumers, twelve filters, ten selections and three further movements. These categories overlap by card and cannot be summed into a card count. They describe producer-consumer bindings, not guaranteed execution or complete consumer semantics.

Ten union records are emitted: eight supported and two UNKNOWN, including same-zone alternatives. Seven records span zones; five are supported and two remain UNKNOWN for multi-target/dynamic restrictions. The remaining cross-zone candidate uses a scaled target wrapper. A modal local union declaration is also outside the root matcher. All exact paths are retained.

Of 198 native moved-result declarations, 190 receive stage records. Eight declarations on seven cards remain untraversed: granted/static ability bodies, replacement-added triggers or replacement effects, an overload variant, and repeat loops. This is an uninterpreted structure, not a missing definition. Principal UNKNOWN reasons include dynamic/unbounded cardinality, uncertain post-movement input membership, unresolved producers, complements, source-linked piles, extra selection restrictions and deferred consumers. Reason counts are nonexclusive record counts; the retained census supplies exact units and paths. No source-fidelity percentage is inferred from these measurements.

## Remaining research boundaries

Dynamic/unbounded collection algebra, complements, correlated target restrictions, unsupported chooser binding, general deferred consumers and arbitrary pile consumers remain incomplete. Structural potential is distinct from actual native execution and source fidelity. Face-down characteristics remain outside the legacy movement predicate's supported subset even when an explicit producer-consumer binding is understood. Source fidelity remains UNKNOWN beyond the bounded fresh scenarios, and no whole-card qualification is issued.

Granted-ability source attribution is the strongest next Card Integration candidate. Three untraversed moved-result declarations already occur in granted/static or replacement-added ability contexts; understanding the granting source and resulting ability instance would benefit more than this collection subset. More complex collection continuations remain a useful later direction, particularly multi-zone producers and bounded continuations currently blocked by missing provenance. Replacement, copy/meld and redirected-arrival fidelity beyond the fresh controls remain unverified. No follow-on increment is begun here.
