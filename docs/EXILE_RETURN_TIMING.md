# Native exile and return timing correction

Oblivion Ring and Banisher Priest require different native mechanics. Ring's entry and departure abilities resolve independently: an empty return resolving first does not cancel a later legal exile. Priest establishes a duration only while its original source object remains on the battlefield; once established, the card returns during source departure, before another instruction or player priority. A separate stacked return ability creates an incorrect opportunity to respond.

## Sources and unchanged-engine reproductions

[Frozen correction requirements](../src/test/resources/regression/exile-timing-correction-sources.json) retain canonical printing and Oracle identities, live Oracle text/rulings, baseline native paths/hashes, and the November 6, 2026 Comprehensive Rules reference. The compact cohort is Oblivion Ring (LRW 34), Banisher Priest (M14 7), Fiend Hunter (ISD 15), Ossification (ONE 26), and Mirror Golem (MRD 208). Their roles are independent triggers, duration exile, and a graveyard-linked pile consumer; these are individual controls, not approval of every translation using their helpers. Relevant rules are 113.7a, 117.5, 400.7, 607.2a and 610.3, including 610.3b/c.

The [fresh baseline receipt](evidence/exile-timing-baseline.json) records actual actions on clean engine `4e72523c8832ecee30c9b142cd91c63acdb7af39`. Ring's departure and empty return resolved before its entry trigger; the still-legal target incorrectly stayed on the battlefield. After Priest's established exile, Boomerang resolved and players received priority with the creature still exiled and a return trigger on the stack. An actual Opt cast was accepted above it. The Oracle-required assertions failed in both cases. A separate Control Magic scenario passed, preserving the source visit across a projected control change.

This supersedes current observations, not history. [PR #63's delivery](LINKED_MOVEMENTS.md), frozen sources/rulings and original verification receipt remain unchanged. The historical source archive used by its hash regression retains the exact four native definitions at engine `4e72523c`; it is test data, not an obsolete-definition compatibility implementation.

## Native correction

Only two card definitions change. Oblivion Ring uses existing `Effects.ExileLinkedToSource`, an ordinary `MoveToZoneEffect(EXILE, linkToSource=true)`, and retains its independent linked-return trigger. Existing `LinkedExileLookup` attributes late exile to the resolving ability's original battlefield visit, including a departed visit. An empty prior return establishes no future obligation. A renewed source has its own pile.

Banisher Priest uses existing `Effects.MoveUntilSourceLeaves(target, EXILE)` and has no departure return trigger. The existing executor validates the source ObjectRef and battlefield visit when establishing the effect. `ZoneReturnService`, invoked by `ZoneTransitionService`, returns the still-current exile object during departure. The native zone event identifies `DURATION_RETURN`. No executor, public SDK type, ledger, delayed trigger, or card-name check is added. The source-presence guard on `ExileUntilLeavesExecutor` remains unchanged for other definitions.

The card keeps its original entity identity through successful zone moves, while each move creates a new ObjectRef generation. Return uses the owner's control and does not preserve the former permanent's Aura, counters, damage, targets or source obligations. Tokens do not acquire an original-card return. A different source, renewed source visit or renewed exile object cannot consume an old association.

## Application interpretation

The existing shared linked-movement model recognizes ordinary source-linked exile plus an exact separate return pipeline. Those movements reference existing movement facts and separate action identities. A root singular `MoveUntilSourceLeaves` declaration instead produces `DURATION_BOUND`, with typed implicit return metadata and no invented continuation action. Current duration lookup reads native `GameState.zoneReturns` by exact source and moved ObjectRefs. Source capture reads projected control, including continuous control effects.

Existing effects, unsupported records and provenance remain available. The duration departure was not previously a movement fact, so the relationship retains its exact native declaration without fabricating a legacy fact. Unsupported anchors, destinations, additional source abilities and collection shapes retain UNKNOWN. No second movement walker or whole-catalog pass is introduced.

Structural support, actual execution, scoped source agreement and whole-card qualification remain separate. Relationship source fidelity stays UNKNOWN in ordinary extraction; the revision-bound scenario receipt supplies bounded new observations. No whole card is independently qualified. Neither family broadens direct graveyard reanimation, intrinsic creature supply or any other Discover admission policy.

## Controlled Discover comparison

All six objectives were compared for Judith, Krenko and Kenrith using clean merged application `d2240321f4b32e176fbbed69f7e2a2ac012aa49a` with engine `4e72523c`, and tested application `1874b558b4481212827454ca3038312f2a72bd3b` with clean candidate engine `0c5be6cd9d9c7594d5cc35e5ff68366a982a014f`. Installed CLI libraries produced both populations at one GiB. The [comparison checker](../scripts/compare-exile-timing.py) normalizes only current native file/repository URL revisions and current relationship engine revisions. Historical assurance is compared before normalization and must remain exact.

All 18 comparisons passed. Eligibility, physical projections, every MATCH result/admission, historical assurance, intrinsic creature returns, and unrelated facts/actions/targets/attachments/prerequisites are unchanged. Only Ring and Priest have changes outside the additive relationship field. Ring gains its ordinary exile movement fact alongside the existing return fact. Priest loses the obsolete departure action and collection-return fact; its implicit duration return is represented without inventing a new legacy fact. Corresponding unsupported/structural-support records change with those declarations.

Seventeen result arrays remain exact after current-source provenance normalization. Kenrith reanimation has one removed Priest UNKNOWN return row and one added Ring NO_MATCH exile row: MATCH remains 232, UNKNOWN changes 528 → 527, and NO_MATCH changes 6,608 → 6,609. No prior UNKNOWN was promoted to reanimation. Unsupported records decrease by three in the affected Kenrith diagnostic scopes; other count fields retain their existing definitions.

| Commander | Eligible physical cards | Relationship records, before → after | Current immediate / independent / duration / UNKNOWN |
| --- | ---: | ---: | --- |
| Judith | 7,728 | 4 → 6 | 0 / 0 / 0 / 6 |
| Krenko | 4,308 | 2 → 3 | 0 / 0 / 0 / 3 |
| Kenrith | 20,143 | 151 → 161 | 19 / 29 / 3 / 110 |

The pools overlap. Each commander's six relationship inventories agree. Priest changes category; existing native duration declarations also gain annotations. Ossification and Sigrid, God-Favored have newly supported structural duration records. Sigrid is not a new source-qualified control. Eight additional Kenrith relationships remain UNKNOWN for collection bindings, nonentry triggers or additional source abilities. Those annotations preserve native provenance and uncertainty; they do not expand collection interpretation or any objective policy. Full exports stay under ignored `generated/exile-timing`; compact receipts retain their hashes and attributed changes.

## Verification and remaining scope

[Verification receipt](evidence/exile-timing-verification.json) and [all 18 comparison receipts](evidence/exile-timing-comparison.json) retain exact revisions, source/test hashes, actual native checkpoints, populations, report hashes and attributed differences. The Kotlin/runtime implementation was tested at `1874b558b4481212827454ca3038312f2a72bd3b`; later delivery changes contain comparison normalization, retained evidence/documentation and removal of a trailing test-file blank line. Runtime sources and probe assertions are unchanged. The engine stayed clean at `0c5be6cd9d9c7594d5cc35e5ff68366a982a014f`. Java and javac are Temurin 25.0.4.1; native/application wrappers use Gradle 9.8.2/9.6.1 respectively. Live application main and engine app-stable remained at the recorded baselines; neither repository was merged or advanced automatically.

| Check | Observed result |
| --- | --- |
| Focused native source/zone/exile controls | 76 passed; included again in the broader suites |
| Full SDK | 713 passed; one opt-in tail-report writer skipped |
| Full rules engine | 9,479 passed; no failures, errors or skips |
| All nine card-era scenario suites | 23,479 passed; no failures, errors or skips |
| Strict native catalog, including snapshots and lint | 493 passed; update mode disabled |
| Complete Kotlin application suite | 811 passed; no failures, errors or skips |
| Bootstrap / Oracle snapshot / catalog comparator checks | 17 / 3 / 5 passed |
| Installed distribution and current catalog gate | Passed; zero confirmed benchmark mismatches and empty self-diff |
| Frontend build / Edge interface / live Edge checks | Passed / 8 passed / 18 passed |
| Normal Explorer | Startup, cold/retained Progress and four exact relationship kinds passed; actual maximum heap 1,073,741,824 bytes |
| Health | Application `1874b558`, clean candidate engine `0c5be6cd`; same installed libraries used for candidate exports |
| Existing AI-reading diagnostic | Ring 2/2 and Priest 1/1 abilities read fully; no AI changes |
| Historical current-SDK report | Typed decode succeeded, current-catalog pin guard correctly rejected reprocessing, and offline self-diff was empty |

The old whole-report fingerprints remain frozen and do not match the current catalog, as before this correction; the current catalog gate passes independently. The older archived `LandTappedForMana` SDK incompatibility remains outside this work. Historical assurance and qualification are not refreshed. The original native console capture stopped during the paused session, while later XML receipts completed. The resumed complete gate confirmed current inputs and finished successfully; the interrupted capture and full receipts remain retained under ignored generated output.

Native tests cover departure-first ordering, actual response checkpoints, same-name independent sources, source renewal, stale exile generations, target legality, tokens and attachment/counter/damage cleanup. Real spells establish exile, bounce, flicker, return and priority behavior. Token/counter/damage setup and intervening hand/exile movements are explicit native fixtures, not demonstrations of spells that produce those states.

Fiend Hunter's ordinary exile/return and optional-decline scenarios protect neighboring independent-trigger execution. Its unchanged guarded helper still warrants a separate departure-first fidelity investigation; ordinary success is not adverse timing qualification. Its optional-target representation also needs separate review of the choice checkpoint. These are retained concerns, not additional corrections in this increment. Ossification protects an existing automatic duration implementation, including return before a subsequent instruction. Mirror Golem protects graveyard exile and later pile-derived protection. Other old-helper declarations remain individually unreviewed; the previous 81/25 declaration counts were inventories, not verified translations.

Both corrections are suitable for an eventual upstream contribution because they compose existing upstream primitives and add reusable scenario invariants. No upstream synchronization or submission occurs here. Broader collection-linked interpretation and granted-ability source attribution remain possible next assignments; this correction does not begin either.
