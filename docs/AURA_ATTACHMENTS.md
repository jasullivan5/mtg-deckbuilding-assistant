# Aura attachment interpretation

The shared walker records an Aura's enchant restriction separately from its explicit spell target requirements. The relationship belongs to the existing card/face root action and names its exact native path. It preserves a source on the battlefield, the declared host zone, and the native condition for an acceptable host. Supported dependencies refer to that relationship instead of treating the Aura and host as the same object.

ChosenTargets.kt uses the native enchant requirement for an Aura casting announcement. It retains its origin and validates the selected target through Argentum's targeting machinery. Successful selection establishes neither resolution nor an attachment. The current-attachment query instead inspects the native attachment component, checks source and host generations, and asks Argentum's enchant-legality code about the current projected host. It returns the source and host controllers separately. A remembered host is rejected after departure or reassignment.

## Operations and checkpoints

- Constant ModifyStats declarations with the native attached scope describe an ongoing modification at continuous evaluation. The power/toughness values and exact group filter remain available. The effect requires an active Aura and legal current host. It does not assert a death or an actual final power/toughness.
- Attached step triggers can link to existing fixed life gain, life loss or direct player damage evidence. For the supported native StepEvent(Player.You) form, timing consults the host's controller at trigger detection. The Aura still controls the ability. Player.TriggeringPlayer preserves the event's player for later resolution.
- Other static declarations and attached triggers retain their native structure and remain UNKNOWN when their dependencies or operations fall outside the supported forms. Burning Anger's granted tap ability is one such boundary. Its source and damage amount require granted-ability interpretation.

These records are independent of Discover admission. Earlier event-operation confidence and unsupported records retain their scope. The added attachment interpretation can establish a supported dependency while the broader event record still records an unresolved general attachment binding.

## Source controls

The frozen cohort contains exact Oracle/printing identity, retained source text, normalized native file hashes and source snapshot hash. The primary control, Stab Wound, combines a negative continuous modification with a host-controller upkeep trigger. Holy Strength tests a positive continuous modification. Wanderlust uses the same upkeep binding with damage. Burning Anger preserves the granted-ability boundary. [Frozen source controls](../src/test/resources/regression/aura-attachment-sources.json)

The maintained engine supplies casting, resolution, attachments, layers, triggers and state-based actions. The application performs structural interpretation and bounded observations through that implementation. Successful scenarios retain their individual scope and provisional assurance status.

## Remaining boundaries

Concrete observation currently binds front definitions. Player and nonbattlefield hosts, complex enchant filters, changed enchant abilities, all attachment-entry methods, general reattachment, granted abilities, arbitrary continuous layers and historical relationships remain incomplete. A source-relative effect may require last-known information beyond this query. A missing known host cannot make the structural capability impossible.

| Remaining opportunity | Compounding gain | Consequential boundary |
| --- | --- | --- |
| Linked intermediate-zone movement | Connect original-card movement through exile and return to existing reanimation and replenishment evidence | Preserve the physical card link while distinguishing each new game object and delayed permission |
| Other persistent relationships | Reuse source/host identity for Equipment and granted abilities | Establish the granted ability's actual source, controller and availability |
| Broader causal interpretation | Extend supported dependencies to more of the retained damage, tapping, combat and zone-change triggers | Preserve event-time bindings and resolution references without assuming a current attachment |

The preferred next investigation is bounded original-card exile-and-return linkage. It compounds the existing movement families and the object-generation work demonstrated here. Further attachment or granted-ability interpretation remains valuable, but its source attribution and permissions require a separate source-led increment.

## Native observations

Stab Wound, Holy Strength and Wanderlust were cast through actual Argentum actions onto an opponent-controlled Hill Giant. The Aura attached to the selected object. Stab Wound changed that host from 3/3 to 1/1, Holy Strength made it 4/5, and the unrelated Hill Giant stayed 3/3. The appropriate upkeep caused two life loss for Stab Wound or one damage for Wanderlust, while Holy Strength introduced no such trigger.

Control Magic changed the host's controller through native casting and resolution. Stab Wound remained controlled by its original controller, and the upkeep trigger followed the host's new controller. A separate test removed the Aura from the constructed native state after its trigger entered the stack. The trigger retained its original player through resolution.

Unsummon returned an enchanted creature to hand through native execution, after which the Aura went to the graveyard. Recasting the creature produced a new native object reference and no inherited stat modifier or attachment. Another test removed the target before the Aura resolved. The legal Aura cast then failed to attach. A noncreature target was rejected at casting.

The source-removal tests use the engine test driver's direct graveyard movement helper to construct that state. They do not demonstrate a specific destruction spell or its triggers. The host bounce, return, control change, failed resolution, upkeep effects and Aura casting use real native actions.

## Catalog units and boundaries

| Measurement | Count |
| --- | ---: |
| Physical native cards with Aura mechanics on at least one face | 671 |
| Card/face attachment records | 672 |
| Supported enchant restrictions | 570 |
| Unresolved enchant restrictions | 102 |
| Supported continuous stat dependency declarations | 228 |
| Supported attached step-trigger operation declarations | 3 |
| Unresolved continuous stat declarations | 9 |
| Unresolved attached trigger declarations | 92 |
| Other static declarations retained without supported dependency interpretation | 730 |
| Distinct cards with a newly supported dependent operation | 230 |

The initial registry audit selected 672 Aura definitions and emitted 673 records. It included 16 independently registered back-face aliases and omitted creature-front roots whose backs are Auras. Reconciliation through the existing physicalCards projection and recursive face inspection gives the table above. Radiant Grace supplies two face records under one physical card. The 230-card improvement count survives physical deduplication. The supported restriction shapes are creature (482), land (40), creature you control (34) and permanent (14). Those are exact supported native target requirement shapes, including their other fields. A description that looks similar can still be unresolved because another requirement differs.

The three supported triggered declarations belong to Cursed Land, Stab Wound and Wanderlust. Cursed Land is a catalog generalization finding; it has no independently reviewed source or execution scenario in this cohort. Static and trigger counts describe declarations, and the 230-card total deduplicates cards with multiple operations. These are structural findings across integrated native definitions, with source fidelity established only at the frozen cohort's bounded scope. Existing generic UNKNOWN and unsupported records remain intact alongside this new dependency evidence.

## Controlled Discover preservation

All 18 final exports passed exact comparison against unchanged merged main on the same engine and eligible populations. The comparison validates every existing report field and action, allowing only the new Aura attachment and enchant-target evidence on the relevant roots. Every dependency action ID and referenced event outcome must exist in that report. Results, verdicts, facts, prerequisites, unsupported records, eligibility, historical assurance and existing cost, Class, state, event and counter records were preserved.

| Commander | Eligible physical cards | Aura-bearing cards / face records in the export | New supported stat / trigger dependencies |
| --- | ---: | ---: | ---: |
| Kenrith | 20,143 | 659 / 660 | 225 / 3 |
| Judith | 7,728 | 179 / 179 | 87 / 2 |
| Krenko | 4,308 | 80 / 80 | 43 / 0 |

The commander populations overlap. A native Aura absent from these eligible populations is distinct from a mechanically incompatible Aura. The new shared evidence does not create a new Discover objective or change any existing objective admission. The legacy general extraction warnings remain beside the new bounded interpretation.

Reproduce exports with the installed CLI and normal one-GiB limit for each commander and the six current objective IDs. Use scripts/compare-aura-attachments.py with baseline directory, candidate directory and receipt path for the exact preservation assertions. The [comparison receipt](evidence/aura-attachment-comparison.json) includes each objective's unchanged MATCH, UNKNOWN and NO_MATCH cards/pathways and exact population digests. The [physical inventory driver](evidence/AuraInventory.java) reuses Catalog, physicalCards and PathwayExtractor. Compile it against the distribution libraries into an ignored generated directory and execute at one GiB.

## Revisions and verification

The unchanged application baseline is 767c60960e0e01813f8d54fee715dff90f076c7c. The maintained engine is clean app-stable at 4e72523c8832ecee30c9b142cd91c63acdb7af39. The implementation is at 2fee1218855e5ce4f8b45d9af6d1f913717e220b, with final test and inventory revisions through 1c73ff912c0b1672105c5e317ac8d9f07e6698f9. Temurin java and javac 25.0.4.1 were executed. No engine code or upstream integration was changed.

All 14 focused attachment checks passed in the final complete application run. The final Kotlin suite passed 780 tests with zero failures, errors or skips, and the distribution build passed. The 18 normal-limit file exports and strict comparisons passed. The four frozen Oracle/printing records, snapshot digest and normalized native source hashes were rechecked. Relative documentation links and the final Git diff were checked.

One early baseline build picked up in-progress source edits and failed on a missing CardScript import. That attempt supplied no baseline receipt. The corrected unchanged baseline was built and exported from an isolated main worktree. A preliminary 12-check attachment run and earlier complete application run preceded the final dependency-link/source-boundary review. The final full run supersedes those intermediate verification observations. Logs and complete generated exports remain under ignored generated/aura-attachments.

The registry/physical-card discrepancy is a measurement finding. The raw registry test inventory is retained alongside the reconciled physical inventory. It was not used to inflate physical card counts or to rewrite a historical benchmark.

## Historical compatibility and runtime

The candidate decoded a merged-baseline Krenko report through the existing audit coverage command. An additional pre-integration PR #59 report failed on the retired SDK EventPattern serial name LandTappedForMana. Unchanged main and the candidate both returned exit code 2 with the identical failure at the same JSON location. No historical snapshot was rewritten or reinterpreted to hide this boundary. Supporting that earlier SDK schema requires a separate compatibility decision.

The older historical catalog fingerprints remain different from the frozen pre-consolidation reference, as recorded in the integration work. The current catalog contract passes independently of those fingerprints.

The real Explorer passed cold Progress, Kenrith creature-death discovery, the existing Stab Wound diagnostic detail and retained-run Progress with a one-GiB heap. The card-detail response exposed the typed enchant restriction, continuous modifier and host-controller trigger links. Health remained ready at the end. The owned service was stopped after verification. Its application stamp is the tested revision with +dirty while documentation/evidence files were in progress.

The [verification receipt](evidence/aura-attachment-verification.json) records test totals, physical and registry units, native source/test hashes, exact runtime provenance and the reproduced compatibility failure. The separate full engine regression suite and browser automation were unexecuted. Engine code and frontend production code were unchanged.

Key verification commands were the application dev wrapper's focused Aura tests, scripts/verify.sh full, the eighteen installed-CLI discover exports with -Xmx1g, the Aura comparison script, the native Catalog/physicalCards inventory driver, and audit coverage --snapshot for the merged and pre-integration reports. The latter uses the established offline four-GiB audit allowance. Live Explorer used -Xmx1g -XX:ActiveProcessorCount=4. All full exports and logs are reproducible generated outputs rather than committed bulk evidence.
