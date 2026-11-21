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

Final revision-bound checks, census and Discover comparisons are retained with the delivery receipt. Bulk JSONL inventories, all 36 Discover reports, logs and installed libraries remain under ignored `generated/collection-movements/`.

The native-only census uses the existing face-aware `LegacyExileInventory.java` against the exact installed baseline. `CollectionProvenanceInventory.java` streams one production extraction per physical card at 1 GiB. `inventory-collection-provenance.py` distinguishes physical roots, authoring face containers, native declarations, actions and producer-consumer records; matching variable text alone never creates a relationship. `compare-collection-provenance.py` permits only the three additive evidence fields and requires all existing report content to remain equal.

## Remaining research boundaries

Dynamic/unbounded collection algebra, complements, correlated target restrictions, unsupported chooser binding, general deferred consumers and arbitrary pile consumers remain incomplete. Structural potential is distinct from actual native execution and source fidelity. Face-down characteristics remain outside the legacy movement predicate's supported subset even when an explicit producer-consumer binding is understood. Source fidelity remains UNKNOWN beyond the bounded fresh scenarios, and no whole-card qualification is issued.

Granted-ability source attribution remains a strong next research candidate: collection continuations already expose exact native binding points, while attribution of abilities granted to resulting objects requires a separate source/context model. More complex collection continuations should be chosen only from the remaining measured native shapes. No follow-on increment is begun here.
