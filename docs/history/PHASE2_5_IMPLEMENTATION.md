# Phase 2.5 — engineering outcomes

Continues the exact delivered Phase2.4 source (archive SHA256 `80ec101e5aa22877250569b991edc83f6acbdcdaa32255257aa67c71e6b9adf9`), not a reconstruction. Argentum remains at `ebf5bf2e9fbb80d13a556d75491dd462e10c206f`; its source checkout is clean. The supplied application archive had no established application Git history; source manifests identify this delivery.

## Implementation and reuse

Objective C is `create-creature-tokens`. `Discovery.kt` applies the same Commander legality/full-identity pipeline, optionally intersects native mtg-search, and calls the existing extractor once. `tokenCreationFacts` composes operation, created-creature, entry-controller and positive-quantity predicates. It has no SDK walker and no card-name exceptions.

| Component | Reuse/change |
|---|---|
| Catalog, CardRegistry, native definitions/search, Judith registration, eligibility | Reused without source changes |
| PathwayExtractor.walk, action identities, cost routes, composite/gate/modal/collection frames, native source evidence | Reused; token cases added inside the existing walker |
| MechanicalFact / FactQuery | Extended with CREATE_TOKEN and one correlated TokenCreation variant; type/controller/count remain together |
| TokenFacts.kt | Reusable created-object and quantity predicates; noncreature composition uses the same facts and changes one predicate |
| Reanimation.kt | Narrow B/C evidence/report projection shared; original public helper retained; B query/traversal semantics unchanged |
| Diagnostics / RootDiagnostics / Benchmark | Existing tools extended for creation nodes and uncertainty; token matches no longer require nontoken compatibility; source-zone uncertainty is only applied to movement facts |
| Objective-specific logic | Four-predicate C composition, contribution label and result counts; no independent parsing/traversal |

This avoids reimplementing catalog resolution, eligibility, ability/cost/branch walking, conventional query parsing, result/action provenance, export, root grouping, or regression comparison. It does not establish a measured reduction in engineering time. A still contains legacy death/removal/dependency projection in the shared extractor. The generic vocabulary is deliberately limited; it is not a universal MTG ontology.

## Three bounded native interpretations

1. **Creature and predefined templates.** CreateToken preserves its creature template, entry riders, granted abilities and dynamic quantities. CreatePredefinedToken resolves characteristics through upstream PredefinedTokens.allTokens, including noncreature artifacts. Unknown native template names remain UNKNOWN. Fixed nonpositive counts fail; supported count/X expressions require a positive evaluated quantity and legal resources; unresolved stored/conditional amounts remain unknown. Existing death AmountBounds behavior is unchanged.
2. **Mixed creation batches.** CreateTokenBatch visits native groups through the same walk, with one shared event/controller and separate group facts. Creature/opponent and noncreature/our-controller groups cannot be combined into a match. Positive/negative native fixtures verify the boundaries. No authored batch case was located, so this is not claimed as real-card batch coverage.
3. **Source/target token copies.** Native merged CopyExceptions replacement/addition precedence is respected. Copy-source type/control and the created token's controller remain distinct. Clear templates/targets qualify conditionally; complex source filters, contradictory selections, moved/unbound references and unresolved copiable characteristics remain unknown. Other copy primitives are not approximated.

Fields preserve native node JSON, face/path/action, role, quantity expression, template reference, selected-object binding, branch prerequisites, timing/circumstances and uncertainty. Native actions retain targets and complete costs. Tokens' own granted future abilities are not automatically attributed as actions available from the source card.

## Independent source review and tests

Frozen C review: 16 pathway cases on 15 real authored cards. Scryfall Oracle/ruling snapshots and pinned native references accompany it. The review was source-led by the implementing reviewer, not external adjudication or catalog-output-derived expectations. The original SHA remains `b16589779c7bd63c8042598d601921c761ef157ffa445534bc51c03e0ee501e3`.

- Development: Dragon Fodder, Krenko, Young Pyromancer, Siege-Gang Commander, Ophiomancer, Heat Shimmer, Hunted Horror, Big Score, Kuldotha Rebirth, Haunted Dead, Rite of Replication.
- Held-out transfer: Hordeling Outburst, Goblin Rabblemaster, Raise the Alarm. All three match their frozen expectations. Expectations were source-reviewed in advance; these are not blind human-adjudicated gold cases.
- Boundaries: opponent-created Centaurs and Treasure are excluded; both Rite kicker branches remain UNKNOWN under the retained generic gate limitation. Amass is separately unresolved, not counted as a confirmed true negative or supported creation.
- Missing authored candidates: Krenko's Command, Bitterblossom, Tempt with Vengeance. No production substitutes were authored.

The all-colors reviewed subset uses actual Kenrith eligibility before extraction so white/blue cases are evaluated separately from Judith/Krenko deck restrictions. **11/11 expected accepted pathways recovered; 4/4 excluded cases not accepted (two mechanical nonmatches and two expected unknown boundaries); zero confirmed mismatches; one separately unresolved Amass case.** This is sample coverage, not recall across all cards.

Forty new tests supplement all 170 prior tests. Native fixtures cover negative/zero/variable quantities, opposing receivers, missing templates/copy sources, type override precedence, optional/conditional branches, composite/batch correlation, multiple operations, shared A/C and B/C actions and the noncreature fourth-objective predicate. Authored Kuldotha Rebirth ties sacrifice and creation to one action. Haunted Dead correctly keeps ETB token creation and graveyard activation as separate actions; event linkage is not collapsed into simultaneity.

Three actual ActionProcessor scenarios verify Dragon Fodder's two controlled creature tokens, Hunted Horror's opposing token receiver, and Heat Shimmer copying an opponent's creature under our control with haste. Judith and previous engine scenarios are retained. Structural matching does not prove arbitrary gameplay execution.

## Diagnostics and remaining work

The existing independent serialized-native traversal and root/family grouping now recognize creation suspects. They retain specialized Amass/populate/chosen-copy gaps, incomplete enclosing actions and objective constraint uncertainty. A review caught and fixed a diagnostic false attribution: creation has no source zone, so its default UNKNOWN sourcePlayer must not become an ownership-data warning.

The next defensible gaps include specialized Amass behavior, chosen/equipped-copy binding, more complex copy filters/controller references, and unresolved quantity/condition semantics. These require positive and negative native/rules verification. Unsupported records are not equated with confirmed misses, and lower warning counts are not used as a correctness score.

## Delivery and reproduction

See README for setup/build/test/run/audit commands; verification/phase2_5/COMMANDS.md records exact executed commands, failures corrected and final successful runs. Compact summaries keep card/pathway/node/group/warning counts distinct. OUTPUT_MANIFEST hashes omitted full reports. The reverse patch reconstructs original Phase2.4 source in a separate directory for controlled regeneration; original bytes were verified.

The primary ZIP deliberately excludes upstream, dependencies/caches, compiled outputs and historical/full-catalog dumps. All source/tests/wrapper, frozen reviews and source evidence, compact reports, actual logs and regeneration scripts remain. Packaging asserts the hard 20 MiB limit and validates CRC. No database, UI, strategy scores, deck optimization or gameplay simulator was added.

## Executed catalog results

The measured table and controlled comparisons below are generated from final verified snapshots, not configured targets.

| Commander | Eligible cards | A direct cards/pathways | A enhanced cards/pathways | B cards/pathways | C cards/pathways | C unknown pathways |
|---|---:|---:|---:|---:|---:|---:|
| Judith | 6104 | 462/474 | 25/25 | 95/97 | 345/362 | 55 |
| Krenko | 3466 | 195/200 | 8/8 | 14/15 | 181/189 | 33 |

All four A/B comparisons use identical eligible pools and pathway identities. Zero match-verdict, mechanical-compatibility, policy, contribution, native-operation or prerequisite changes. Explanation-only action-completeness changes: Judith A 63/B 24; Krenko A 30/B 2. Those do not count as policy/mechanical changes.

Judith has 647 token facts: 362 accepted, 230 incompatible, 55 unknown. Krenko has 361: 189 accepted, 139 incompatible, 33 unknown. Facts include noncreature/excluded variants; they are not accepted card counts. Native node/group/warning counts are in compact coverage reports.

| Native token structure | Judith cards/actions/facts | Krenko cards/actions/facts |
|---|---:|---:|
| CreatePredefinedToken | 217/228/231 | 134/140/142 |
| CreateToken | 342/360/363 | 174/182/184 |
| CreateTokenCopyOfSource | 17/17/17 | 11/11/11 |
| CreateTokenCopyOfTarget | 31/34/36 | 21/22/24 |

All three objectives export identical complete fact collections in both Commander contexts. Shared accepted action identities: Judith A/C 7 and B/C 1; Krenko A/C 4 and B/C 0. Different operation kinds are generally consumed by different objectives; common action/frame interpretation is genuine reuse, not evidence that all three objectives accept the same operation. The all-colors B/C composite fixture separately proves same-action movement and creation.

The corrected Judith C root diagnostics separate 53 missing objective interpretations from two genuine unresolved-controller records, rather than assigning all 55 unknown paths to ownership/control. Specialized mechanisms and 98 incomplete accepted actions remain inspectable; no complete-action support claim is inferred from local token matches.
