# Linked original-card movements

The shared action evidence now distinguishes an individual movement from a conditional relationship between movements of the same original card. `ActionEvidence.linkedMovements` is additive, defaults to empty when decoding earlier reports, and is independent of the six Discover policies. The original movement facts, their IDs, warnings and verdicts remain available unchanged.

## Representation and identity

`LinkedMovement` records the originating physical-definition name and face, exact action and effect paths, component fact IDs when available, native effects, source/destination zones, return controller, binding, checkpoints, prerequisites, support and explicit uncertainty. Native source evidence remains on the referenced action; the engine revision is retained on the relationship. Structural support and source fidelity are separate: source fidelity defaults to UNKNOWN.

The existing target movement map now retains the preceding instruction alongside its existing possible-zone scopes. It does not introduce another movement walker. Only adjacent ordered instructions in one resolution with a singular unambiguous native target binding support immediate battlefield → exile → battlefield linkage. Different bindings, alternative target clauses, intervening instructions, optional/deferred/guarded contexts and other zone shapes cannot become supported links. A structural link does not establish a successful first movement, a successful return, or a currently legal target.

The selected original card and its successive game objects are different concepts. `observeResolution` accepts the selected native `ObjectRef` and the event slice of one resolution. It requires a primary departure from that exact generation, an exile object, and a subsequent primary return from that exact exile generation. It retains all three references and whether the result is still current. Same names or the same raw entity ID in another generation do not satisfy this check. Missing movement evidence remains UNKNOWN. An exiled token is explicitly NO_MATCH for an original-card return. This is bounded observation of supplied native events, not an event attribution or replacement simulator.

Original attachments, counters, marked damage, temporary effects, old targeting and source obligations are not carried by the relationship. State cleanup remains Argentum's responsibility. Native scenarios inspect attachment, counter, damage and temporary-stat cleanup on a returned Hill Giant. The same original card persists; its former battlefield object does not.

## Immediate and deferred families

Cloudshift's Exile/PutOntoBattlefieldUnderYourControl and Momentary Blink's Exile/Move(BATTLEFIELD) share the immediate record. Their controller fields remain YOU and OWNER respectively. Actual Control Magic scenarios distinguish ownership from current projected control. Momentary Blink's native flashback action remains its existing separate casting capability.

The deferred matcher recognizes exact root ExileUntilLeaves declarations and a separate self-departure trigger whose native composite gathers FromLinkedExile and moves that collection to the battlefield under owners' control. It associates only one producer and one consumer in the same physical definition/face; ambiguous source-pile producers remain UNKNOWN. The exile effect was not a legacy MOVE_OBJECT fact, so that component retains the native instruction with no invented fact ID. The return references its existing collection movement fact. That fact retains its broader unresolved collection interpretation.

This describes the native declarations, including their independent trigger resolutions. It does not turn a native separate trigger into a duration return, merge trigger checkpoints, or infer that an already-resolved empty return will consume a future exile. The structural relationship requires the original source battlefield visit to remain available at establishment. The executor's source-presence guard alone is not proof of that generation requirement across departure/reentry.

`captureLinkedSource` captures a current native source ObjectRef, battlefield-entry timestamp and controller. `linkedExileNow` delegates to Argentum's existing LinkedExileLookup using that visit's context; it records no application exile ledger. Live and departed source piles remain engine-owned. Another same-name source and a later battlefield visit do not acquire the old visit's observation. An empty pile is only empty at that checkpoint.

## Frozen sources and native discrepancies

[Source requirements](../src/test/resources/regression/linked-movement-sources.json) were committed before production implementation at `6d7c74f`. They bind four exact Oracle identities and representative physical printings from the retained Oracle snapshot, normalized native definition hashes, the merged application baseline and unchanged engine. A representative snapshot printing is distinct from the canonical native definition's printing. [Live Scryfall rulings](../src/test/resources/regression/linked-movement-rulings.json) were retrieved separately with their URLs and UTC timestamps; they corroborate the timing and identity boundaries. New observations do not qualify whole cards or change historical concerns.

| Source | Bounded expected behavior | Native fidelity boundary |
| --- | --- | --- |
| Cloudshift | Exile selected controlled creature; return same nontoken card under ability controller as a new object | Successful primary transitions and entry remain prerequisites |
| Momentary Blink | Same sequence; owner controls the returned permanent | Flashback remains separate; no expansion of casting interpretation |
| Oblivion Ring | Two independent triggers; departure-first returns nothing, then entry still exiles | ExileUntilLeavesExecutor checks source presence and skips that late exile, contradicting the native comment and retained ruling |
| Banisher Priest | No exile if the source has already left; duration return at departure | Native definition uses a separate return trigger, leaving a response window that Oracle's duration return does not create |

The adverse Ring scenario is a source-bound reproduction of the unchanged engine discrepancy. Its test asserts the observed behavior separately from the frozen expectation. Priest's departure-before-exile boundary agrees with its ruling, but the separate return-trigger design remains a fidelity concern. There is no application card-name workaround and no engine correction or upstream integration in this increment.

The movement, Aura casting, control change, temporary Giant Growth effect, source bounce and recasting scenarios submit real CastSpell actions and resolve native triggers. Fixtures directly seed creatures, counters and damage, and mark the token control with the native TokenComponent; these setup operations do not demonstrate a token-producing or counter-placing spell. The stale-exile control uses the native zone service to construct an intervening hand/exile movement. It does not claim a particular real card caused that interruption.

## Objective and historical boundaries

Battlefield → exile → battlefield is not direct graveyard reanimation. Exile → battlefield is not graveyard → battlefield. No objective or admission policy was added. Existing SelfReturnLink records remain distinct: they describe intrinsic source-bound creature return routes with their own active zones, costs and limits. New linked movement evidence can inform later identity questions, but does not infer death, repeatability, sacrifice fuel or a loop.

The strict comparison script removes only the new action-level relationship field and compares every other report field exactly. It also verifies referenced action and fact IDs and paths. This protects movement facts, event operations, eligibility, chosen-target and attachment evidence, intrinsic returns, prerequisites and historical assurance, rather than comparing only MATCH totals.

All 18 comparisons passed on the same engine and physical populations. Every existing result row, admission decision, MATCH/NO_MATCH/UNKNOWN verdict, action field, movement fact, source-bound return, target/attachment relationship, triggered operation, unsupported structure, prerequisite and assurance record is exactly preserved. No reanimation candidate was promoted. Relationship inventories are identical across each commander's six objective reports.

| Commander | Eligible physical cards | New relationships | Immediate / deferred / UNKNOWN |
| --- | ---: | ---: | ---: |
| Kenrith | 20,143 | 151 | 19 / 30 / 102 |
| Judith | 7,728 | 4 | 0 / 0 / 4 |
| Krenko | 4,308 | 2 | 0 / 0 / 2 |

The commander pools overlap. Relationships outside a commander's eligible population are distinct from mechanical NO_MATCH decisions. [Comparison receipt](evidence/linked-movement-comparison.json) retains each pair's population digest, exact report hashes, relationship counts and unchanged result counts.

## Verification and inventory

Final measurements and revision-bound receipts are recorded in the compact evidence accompanying this report. Full exports, streaming inventory output and logs remain under ignored `generated/linked-movements`.

The physical inventory driver uses the production extractor one physical definition at a time at one GiB, avoiding registry back-face aliases. Its reducer counts exact native declaration nodes and paths separately from interpreted relationships. Candidate immediate sequences require identical native target references or explicit collection-variable continuity; candidate deferred faces require native linked-exile producers and reads. Those candidates are not proofs of singular identity, timing or source fidelity. A native declaration outside interpretation is distinct from a missing native card definition.

| Catalog measurement | Count |
| --- | ---: |
| Integrated physical cards / faces inspected | 21,699 / 22,487 |
| Candidate physical cards / faces | 383 / 387 |
| Root spell/activation/trigger authoring containers / movement declarations | 515 / 765 |
| Other movement declaration containers with unresolved action attribution | 45 |
| Potential ordered native sequence pairs | 96 |
| Faces containing native source-pile producers and reads | 160 |
| Native linked-pile read declarations | 200 |
| Native MoveUntilSourceLeaves declarations | 25 |
| Supported immediate relationships | 19 |
| Supported deferred relationships | 31 |
| Physical cards with newly supported relationships | 50 |
| UNKNOWN relationships / physical cards | 108 / 101 |
| Candidate faces without a new relationship record | 236 |

[Compact inventory](evidence/linked-movement-inventory.json) retains all supported native paths, all UNKNOWN relationship identities with reason codes, counts, reproducibility hashes and bounded uninterpreted examples. The complete generated inventory retains every candidate declaration's native node. Its 765 movement declarations comprise 332 MoveToZone, 323 MoveCollection, 81 ExileUntilLeaves, 25 MoveUntilSourceLeaves, two ReturnNotedExileTappedWithAuras, one ReturnOneFromLinkedExile and one ExileWithAurasNotingCounters. These are declarations within the candidate population, not all movements in the catalog.

Root authoring containers identify where a declaration occurs, not proof that a nested granted operation executes as that root action. The other 45 containers include Saga chapters, replacement declarations, Class paths and granted abilities, including Deadeye Navigator's two movement declarations. They retain exact native paths rather than invented action/source bindings. The 560 total containers must not be reported as 560 bound actions. These units are separate from the 50-card supported relationship gain.

The 160 source-pile candidate faces include nonreturn consumers such as play permissions. They are not 160 proven deferred returns. The 96 native sequence pairs and 158 emitted relationship records use different units; branch interpretation and unresolved continuations prevent one-to-one correspondence. UNKNOWN reasons overlap: 69 guarded/optional/deferred contexts, 53 nonadjacent continuations, 42 nonsingular or unsupported bindings and 24 source declarations outside the isolated two-trigger subset are the largest groups. Other reasons retain exact controller, source-zone, target, producer/consumer and zone-sequence gaps. The four named controls are all available; missing definitions were not classified as unsupported relationships.

Collections and storeMovedAs retain existing movement facts and provenance but do not receive a singular original-card relationship in this increment. Arbitrary delayed triggers, other linked-pile consumers, copies, melds, unknown-size collections, source renewal during unresolved exile, and replacement-driven routes remain outside the bounded supported subset. General native source fidelity is unreviewed outside the frozen cohort.

## Revisions, regressions and runtime

The live merged application baseline is `ec6e61e6309e7891b61ec88252dd1dc3f33cb9dc` (PR #62). Frozen source requirements are at `6d7c74f`; final production and test source is `a0521ac6b57815d1c230dc783d79ed2e584b8002`. The unchanged clean maintained engine and live app-stable are `4e72523c8832ecee30c9b142cd91c63acdb7af39`. Temurin java and javac 25.0.4.1 were used. No additional upstream revisions, engine correction, objective or eligibility policy was integrated.

| Check | Final result |
| --- | --- |
| Complete Kotlin application suite | 805 passed; zero failures, errors or skips |
| New structural/source/schema controls | 9 passed |
| New native scenario checks | 16 passed, with the Ring source discrepancy explicitly retained |
| Installed distribution | Passed; exactly the same application JAR as the comparison libraries |
| Current catalog gate | Passed; zero confirmed benchmark mismatches and empty self-diff |
| Six objectives × three commanders | All 18 strict comparisons passed |
| Bootstrap / snapshot / catalog-comparator workflow checks | 17 / 3 / 5 passed |
| Explorer frontend build | Passed |
| Supported historical report decoding | Merged-baseline Krenko token report decoded successfully |
| Normal Explorer | Cold Progress, Kenrith reanimation, four evidence details, retained Progress and final ready health passed at one-GiB heap |

The [verification receipt](evidence/linked-movement-verification.json) retains normalized source/test/diagnostic hashes, exact native object observations, test identities, source discrepancies, catalog checks and runtime provenance. Full exports and the source-bound test logs remain reproducible generated output. Initial focused fixture failures used an illegal Unsummon target for Ring and raw rather than projected Control Magic control; correcting those fixtures did not alter frozen requirements. Interrupted full runs are not counted as passed verification; the final complete run supersedes them.

The installed and comparison application JAR SHA-256 is `024e9d6d40924565dadebe674a0ec4c9c0c467bbf11402410f96fd9a2b80abc5`. Runtime provenance reports `a0521ac...+dirty` while documentation and retained evidence were in progress. Subsequent delivery edits affect diagnostics/documentation only; the retained production/test hashes identify the verified implementation. The normal Explorer and CLI default remain `-Xmx1g -XX:ActiveProcessorCount=4`. The existing Kotlin test heap is two GiB; historical offline audit used its established four-GiB allowance. These are different execution modes.

Historical whole-report fingerprints retain their earlier difference from the pre-consolidation reference; they were not rewritten. Current-schema compatibility is established by a complete old-report decode and the action default/round-trip tests. The older LandTappedForMana SDK-schema incompatibility was not changed or re-investigated. The separate full engine regression suite and browser automation were unexecuted; real Argentum action execution is covered by the application scenarios, and live Explorer was checked through its HTTP interface with freshly built frontend assets.

Reproduce the Kotlin and distribution checks with `./dev.ps1 test --tests 'dev.mtg.assistant.LinkedMovement*'`, `./dev.ps1 test`, and `./dev.ps1 installDist`; run `bash scripts/check-catalog.sh` for the current catalog gate. Export all six objective IDs with the installed CLI for Kenrith, Judith and Krenko on unchanged main and the candidate, then run `scripts/compare-linked-movements.py BASELINE CANDIDATE RECEIPT`. Compile [the streaming inventory driver](evidence/LinkedMovementInventory.java) against the installed libraries, execute it with `-Xmx1g`, and reduce its UTF-8 JSONL output with `scripts/inventory-linked-movements.py RAW OUTPUT`. All candidate native source paths come from the existing compiled-getter DefinitionSources index; no card-name mechanics search supplies a link.

For normal use, launch `./scripts/explorer.ps1`. Kenrith with Reanimate own creatures exposes the four controls as diagnostic findings; their action evidence contains the new links while their reanimation admissions stay unchanged. The verification service used a separate port and was stopped after the checks.

## Next research direction

The strongest next direction is broader linked-zone interpretation coupled to source-fidelity verification: distinguish legacy independent-trigger exile from modern duration returns, including establishment after source renewal. The inventory already exposes 25 native MoveUntilSourceLeaves declarations outside this subset; collection identity through storeMovedAs also directly reuses this increment. Native correction, if needed, requires its own bounded source-led verification and application compatibility. Granted abilities and source attribution remain a strong alternative: Deadeye Navigator demonstrates movement declarations whose ability source is not yet interpreted, while existing attachment evidence already identifies many hosts. Their separate source/controller and availability requirements should remain explicit. No subsequent assignment has begun.
