# Native upstream consolidation — Product Studio delivery

This follow-up restores the normal application catalog by adopting twelve native Argentum definitions and retiring their competing application scripts. It preserves the historical failures and provisional assurance. This is an operational integration and bounded source/behavior review, not independent whole-card qualification.

## Revisions and operational outcome

- Merged starting points: application `a150cdd66bf227f4c88a406b04620e56a7aed275` (PR #60), engine `cf6d6bdf00023208b6bc6a7bc915c75b125966fe` (PR #8). Their normal catalog failure was reproduced before editing.
- Verified executable application: `9304f480e43a59ebece781ece4e5aa2ad2687d6a`; engine: `682d4e034a5b9078a6be3146b50258313c9af2fa`. Report/evidence-only application commits follow this executable revision.
- Fixed upstream snapshot: `1e2783f51bdd0436f7bc45ed6e2fce1c8c13a796`, incorporated through ordinary merge `a4ada00f7aa8c21d73e2ed3bb63ffcb2ebbb8f44`. This includes all six commits after the previously integrated `21bc1908…`; no cherry-picks or history rewriting.
- The normal installed Explorer starts with its production catalog, default eligibility corrections and the launcher’s 1 GiB heap. `/api/v1/health` independently reports the exact verified application/engine pair. JDK 21 remains supported.
- A separate upstream recheck found eleven newer commits through `cb6c710d54233e164f4d66970263a8453a0dbcd8` (M15 cards, activation locks/taxes, combat triggers, library grouping, protection, AI and documentation). They are the next possible integration increment, outside this fixed acceptance baseline. The remote application and app-stable bases did not move during verification.

## Twelve native adoptions

Every row now uses the actual native definition, with its application production script and registration removed. All twelve have verified Oracle ID, exact printing ID, set/collector number, release date, source text, full color identity and Commander legality. New dated source/ruling observations are in `src/test/resources/cards/native-adoption-sources.json`; the original source files, golden definitions and assurance ledger remain unchanged.

| Card | Material comparison and bounded evidence |
| --- | --- |
| Bitterblossom | Same mandatory upkeep life loss and Faerie token. Native differences are display text and token imagery. Native metadata curates three rulings; all five original source rulings remain archived, including the two general Kindred notes. Life loss remains an effect, not payment. |
| Galadhrim Ambush | Serialized behavior matches: attackers determine token quantity; damage prevention retains its Elf exception. Existing source-bound positive/zero-attacker scenarios now use native definitions. |
| Hour of Promise | Native Desert check uses `Permanent.Desert` rather than `Land.Desert`; Desert is a land subtype. The search, two-land maximum, tapped entry, shuffle and three-Desert threshold remain native. Existing threshold/search scenarios execute the adopted definition; token imagery differs. |
| Judith, the Scourge Diva | Serialized behavior matches: other creatures’ power boost and own nontoken death trigger. Source identity now associates the preserved historical evidence with native implementation. |
| Krenko’s Command | Same two Goblin tokens; native token imagery differs. Existing token execution and objective scenarios use native directly. |
| Labyrinth of Skophos | Same mana and four-mana/tap activation, restricted attacking/blocking target and removal from combat. Native description text differs. |
| Might of Murasa | Serialized behavior matches, including kicker quantity and targeting. Existing native-execution scenarios cover base/kicked quantities and illegal selection. |
| Oviya Pashiri, Sage Lifecrafter | Same two paid tap activations and creature-count-derived Construct. Native display text/token imagery and additional format data differ. The Scryfall comparison checks only formats that its source API actually supplies. |
| Planewide Celebration | Native return effect omits an explicit `fromZone`; the graveyard target binding and object-generation validation enforce it. New native tests accept our graveyard permanent, reject hand/opponent graveyard targets, and prevent return after exile in response. With its only target illegal, the complete spell, including untargeted modes, fizzles. Existing repeated-mode/proliferation scenarios remain. |
| Spined Tyrranax | Serialized behavior matches, including optional payment followed by reflexive target selection. Both payment branches execute native. |
| Throne of Makindi | Added native empty color identity: any-color generation does not grant colored Commander identity. Activation descriptions differ; actual costs, charge counter and restricted mana remain. Existing paid-activation scenarios and Assay structural comparison exclude display-only wording explicitly. |
| Tyrite Sanctum | Native `AddCreatureType` uses the existing permanent subtype-modification machinery, matching the retired permanent `AddSubtype`. New native tests retain God plus Human across turns and allow an existing noncreature God to receive an indestructible counter, with Sanctum sacrificed as a cost. |

The bounded inventory covered all 22 application extensions. Twelve were redundant; ten remain and the catalog continues to reject unresolved name, face and Oracle-ID collisions. No additional unrelated definitions were retired and no override or card-name selection mechanism was introduced.

Historical assurance is associated by verified Oracle ID. Changed native definitions do not inherit current fidelity/execution claims: all twelve remain **PROVISIONAL**, with stale historical evidence and current source fidelity/execution unreviewed. Tatterkite remains blocked. Source identity, legal eligibility, structural availability and execution qualification remain separate.

## Other compatibility and evidence changes

**Sunforger:** the native definition now supplies verified RW identity and printing metadata, so the old seven-record correction archive remains frozen while the active selection contains six records. Applying the old correction to this changed definition still fails its original binding. Source review also found a real native ordering defect: the found instant must be cast before shuffling. Reordering the existing continuation fixes it without new engine machinery. Native regressions check that target selection precedes shuffle and that `SpellCastEvent` precedes `LibraryShuffledEvent`.

**Predatory Urge:** the historical M1 failure and its old source-probe meaning are unchanged. Explicit `Self` now legitimately uses departed-object last-known information; that old mutation no longer distinguishes this scope. Fresh M10 changes the outgoing damage source to the recipient while preserving damage quantity. Its passing control and rejected mutation assert the original departed host’s actual damage-event source. This is bounded current evidence, not retrospective erasure or whole-card approval.

**Physical association and costs:** the original global snapshot and baseline remain frozen. A new association artifact reuses the exact 39,049-record snapshot and 32,109-card legal population. The generic SDK projection maps physical OMEN layout to the source’s adventure layout while retaining raw native Omen mechanics; `sdk-physical-v2-omen` versions this adaptation separately from the unchanged matching policy. Wrong-face-cost negative controls remain. Confident associations rise from 16,927 to 21,319, with no previously confident association lost; this does not measure mechanical fidelity. Exact native inventory and projection guards remain active. Ten printed-cost bindings were freshly inspected against source costs, physical identity, ability selection and actual current native trees. Original bindings and stale execution receipts remain untouched. Fresh payment execution retains incomplete/unresolved choices and discriminates the missing-mana and missing-sacrifice controls.

**Historical audit and export:** the built-in audit initially failed on an archived `LandTappedForMana` discriminator removed from the current SDK. Offline comparisons now decode application-owned recorded decisions while retaining original SDK payloads as opaque JSON, including duplicates and before/after evidence. Missing revisions and unknown decision values fail closed. No legacy event is relabeled as a current SDK event or re-executed. A separate default-heap Kenrith CLI export reproduced an OOM while constructing the full JSON string; JSON file exports now stream directly. The final default-heap exports preserve the reviewed report bytes. Full HTTP/CLI parity and source-derived legacy-wire regressions pass. Offline pairwise audits use a larger heap for two complete exports; the normal Explorer and discovery export remain at 1 GiB.

**Windows and runtime:** the fresh checkout honors existing LF attributes for all 213 native snapshot files. No canonicalizing hook or weakened byte comparison was added. Application tests require a 2 GiB heap to compare full HTTP/CLI reports over the expanded catalog; the normal Explorer remains at its verified 1 GiB setting. The preexisting Midnight Reaper browser failure came from expecting the second outcome in a compact first-outcome summary. The corrected browser test verifies damage and draw in the full ability view; it also reaches and verifies both represented Blood Artist outcomes. No product UI or objective was added.

## Verification

| Gate | Final result |
| --- | --- |
| JDK 21 bootstrap, installed distribution and normal Explorer | Passed; actual health reports the executable pair above |
| Full Kotlin application suite | **764 passed**, no failures/errors/skips |
| SDK | **713 passed**; one opt-in SDK tail report writer skipped by default |
| Rules engine | **9,479 passed** |
| All nine era scenario suites | **23,463 passed** |
| Strict native catalog | **493 passed**; normal LF resources, no canonicalization hook |
| Current production catalog gate | Passed; zero confirmed benchmark mismatches and empty self-diff |
| Frontend and Edge checks | Build passed; **8 synthetic UI + 18 live backend browser checks passed** |
| Production discovery | **18 normal reports**: three commanders × six objectives, at the default 1 GiB export heap |
| Historical production audits | **18 built-in before/after audits** read the old SDK payloads without rewriting them |
| Bootstrap/source safeguards | Bash 17 passed; PowerShell 14 passed plus three Bash-only skips; snapshot integrity 3 passed; comparator 5 passed |

Native results bind engine `682d4e0…`; the final application suite and runtime bind application `9304f48…` with that same engine. The last application-only changes repair archived report reading and file export; native engine code and tests remained unchanged. The six new Planewide/Tyrite scenarios and the three Sunforger scenarios are included in the era totals. Retained controls include mandatory tap payment 26, collection counters 17, Spark Elemental source identity 6 and source-zone identity 33; these overlap the totals above.

Full named JUnit results and source/blob hashes are retained in the evidence bundle. Native total: **34,148 passed**, one non-gate report writer skipped. No required gate remains failed or unexecuted. Frozen historical whole-report fingerprints remain different and unchanged.

The first merged-baseline focused reproduction failed on catalog collisions and M1. The first consolidated application run was 746/759 passing; the next was 759/761. Their failed receipts remain separate. An intermediate test compilation failure was corrected before the final clean-pair run. Passing current checks does not rewrite those observations. Frozen historical reports still decode; original source resources and qualification records have no modifications.

## Controlled production comparison

Both sides use normal production catalogs, including their application extensions and reviewed corrections. Before is the working pre-integration application `e0fa95970f546c19b79e32bc323af74367d6c353` / engine `bd26b756b8b4d347f19e92bc77ef020919ebfa48`; the already-merged incompatible baseline cannot produce normal reports. After uses the verified executable pair above. Eighteen reports cover Judith, Krenko and Kenrith across all six objectives, without native-only substitution.

| Commander | Common admitted names / before | After | Newly admitted native names | Previously admitted names lost |
| --- | ---: | ---: | ---: | ---: |
| Judith | 6,111 | 7,728 | 1,617 | 0 |
| Krenko | 3,472 | 4,308 | 836 | 0 |
| Kenrith | 15,955 | 20,143 | 4,188 | 0 |

The common sets are held fixed for comparison of results, actions, facts, prerequisites, unsupported structures, uncertainty, eligibility and assurance. Only literal runtime repository URL SHAs are normalized; native trees, paths and evidence revisions remain visible. There are **zero verdict flips at the same card/action/path/branch key** across all eighteen reports; this deliberately does not hide added or removed paths.

Meaningful differences remain recorded: exploit exposes two alternative paths instead of one, with equivalent death decisions but distinct prerequisites; renamed collection/target pipelines shift paths; simultaneous per-player discard collections leave Liliana of the Veil, Rankle’s Prank and Lore Broker `UNKNOWN` for graveyard arrival because selection ownership is not fully bound. Relive the Past’s simultaneous return with entering characteristics is retained as `UNKNOWN`, rather than inheriting an old decomposed reanimation match. Temporary Lockdown, Pinnacle Starcage and The Hunger Tide Rises use newer enclosing native structures whose unhandled branches remain unsupported. Counter modifier forms change from doubling to scaling and explicit placer forms. Turn-tracking amounts use the newer native amount node; image and display differences are separate. Migrated Planewide’s zone difference is covered by fresh execution. These are observed structural interpretation changes, not claims that every old or new definition is source-correct.

Devout Harpist has the one matched-path mechanical-compatibility change: `MATCH` becomes `UNKNOWN` while its final verdict stays `NO_MATCH`. Upstream corrected its target from a creature to an Aura attached to a creature, as specified by the dated official source snapshot. The restriction is preserved rather than silently inferred away.

The twelve migrated cards keep their historical assurance associations. Common native assurance records do not acquire qualification. Twelve already-excluded banned cards now have explicit native Commander `NO_MATCH` instead of `UNKNOWN`; Sunforger keeps reviewed RW eligibility using its corrected native metadata. No previously admitted name is lost. These changes are separate from the migrated cards' native provenance. The external association artifact does not activate new eligibility corrections. Raw report hashes, common populations, changed paths, UNKNOWNs and section counts are retained in the comparison receipt and generated reports. More represented cards is availability, not proof of improved fidelity.

## Maintenance and delivery

Application production card scripts shrink by **404 lines**, plus **28 registration lines**; extensions fall from 22 to 10. Across application production Kotlin, including the historical audit and streaming export fixes, the consolidation adds 131 and removes 452 lines, a net reduction of 321. This excludes the large upstream import and test/source evidence. Small retained application additions own source association, eligibility activation and historical trust, rather than competing card mechanics. New native metadata contributes roughly three lines per reviewed definition; the Sunforger fix reuses the existing pipeline.

The fixed upstream still lacks the demonstrated mandatory source-tap enforcement under Explicit payment, original source-object sacrifice binding and unified collection counter placement. Those patches remain: recursive mandatory-tap validation plus payment-time protection; resolving Self through the bound object reference with current control and owner destination; and delegation of collection placement through the shared single-object replacement-aware machinery. Their existing native regressions are included in the full run. They follow upstream handlers and are plausible future upstream contributions, along with Sunforger ordering/identity metadata; none has been submitted upstream.

JDK 21/toolchain settings and ClassGraph 4.8/API adaptation remain a small compatibility delta. Current upstream requires JDK 25/ClassGraph 6, so deleting these adjustments would abandon the requested JDK 21 target. Windows already has suitable LF attributes; no additional compatibility layer is retained. CI/manual acceptance and fork stewardship remain explicit. No retained correction was removed merely because upstream introduced a superficially similar feature.

Drafts: [engine #9](https://github.com/jasullivan5/argentum-engine/pull/9) and [application #61](https://github.com/jasullivan5/mtg-deckbuilding-assistant/pull/61). Both remain unmerged.

See [verification receipts and replay commands](evidence/native-consolidation-2026-11-18/README.md), including source preservation, physical reassociation, raw test results, source/definition bindings, runtime health and production comparison hashes.

Review and merge the engine/application changes as a coordinated pair. The engine PR requires this application consolidation; the application PR requires this engine candidate. Neither PR is merged automatically, and neither repository’s old primary checkout or unrelated worktree was overwritten. There is no consequential unresolved implementation choice requiring Product Studio approval; independent qualification and the eleven later upstream commits remain separate future work. Unconsumed server, AI, gym, client gameplay, deprecated mtgish and whole-MTG fidelity are not claimed verified by these application-focused gates.
