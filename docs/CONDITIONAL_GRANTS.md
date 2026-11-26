# Conditional ability grants and availability

The shared static walk reads one direct `ConditionalStaticAbility` around a granted activated or triggered ability. The existing static action keeps the complete wrapper. Its grant record binds the native condition and the inner declaration at deterministic paths. Nested costs, targets, effects and facts follow the receiving source. They remain attached to the grant record, so the objective walk still sees the original intrinsic actions and facts.

The scope includes creature attachment grants, previously supported recipients and exact native conditional self scope. The bounded condition recognizer covers fixed source or attached permanent characteristics, the granter controller's battlefield or graveyard existence checks, and a native surveilled-this-turn comparator. It retains the full condition for other shapes with UNKNOWN support. Recipient, condition and event support are independent. For instance, a transformed-permanent existence predicate remains UNKNOWN even when the self recipient and departure event are understood.

A current observation binds the exact static index, root, inner path, face and both object generations. The native evaluator gets the granter and its projected controller. Existing activation or trigger lookup still has to expose the ability. A false condition can explain the absence of a grant without predicting the availability of other declarations. The condition is evaluated at activation availability, trigger time or a native pre-departure checkpoint. It stays distinct from a trigger restriction, intervening-if, target announcement and resolution choice.

The engine captures self-granted departure abilities at the event. For a departing source this is its own frozen conditional grant IDs. For a still-present battlefield observer such as Relic Vial it is a separate map on the departing object's snapshot. The stack observer checks the actual event, exact captured source generation and rule, controller, unique declaration and corresponding frozen grant ID. A missing snapshot or ambiguous identity leaves attribution UNKNOWN. Group provider stamps remain subject to the existing native limitation.

The source cohort was frozen against application baseline `60de24454646395b8239c6b834fbd8993efee9f5` and engine `0878d800edbec56f0c82dac080ae4fec32816b5f`. It records six printing identities, Oracle requirements, rulings and exact native paths and hashes. [Source cohort](../src/test/resources/regression/conditional-grant-sources.json)

The baseline compiled catalog contains 21,699 physical cards and 22,487 authoring containers. Of 15 nested activated declarations on 14 cards, eight are direct conditional wrappers. The other seven are Class levels, a composite static, a Saga-created static and Aura-conversion grants. All nine nested triggered declarations on eight cards are direct conditional wrappers. The census keeps the exact paths for the remaining nested declarations.

| Family | New direct wrapper records | Recipient MATCH | Condition MATCH | Both MATCH | Still untraversed |
| --- | ---: | ---: | ---: | ---: | ---: |
| Activated | 8 on 8 cards | 5 | 2 | 1 | 7 |
| Triggered | 9 on 8 cards | 8 | 7 | 7 | 0 |

All columns count declarations except where physical cards are stated. The prior walk emitted zero conditional grant records. A native condition can be understood even when the recipient is outside the bounded Aura or land rules. Unresolved condition shapes include threshold counts, a turn gate, state predicates for renown and transformation, the attached land requirement and Essence Leak's color disjunction.

## Native scenarios

| Control | Observation |
| --- | --- |
| Helm of the Ghastlord | Four real hosts cover neither, blue-only, black-only and both. The Aura and host have different controllers. The established draw and discard triggers retain their host-controller binding when Blind Seer makes the host blue only and when the Aura leaves. Two same-name Helms yield separately stamped triggers. A new Aura generation cannot claim an earlier trigger, and blinking the host renews its receiving generation. |
| Villainous Ogre | A Demon on the opposing battlefield fails the gate. A Demon controlled by the Ogre's controller enables the ability and the Ogre pays one black mana to activate from itself. After the qualifying Demon leaves, the established ability still provides a regeneration shield. A new activation is rejected. Wrong static index and wrong wrapper path are rejected by the observer. |
| Fire Nation Cadets | An opponent's Lesson fails the gate. The controller's Lesson enables the attack trigger. Exiling that Lesson with Tormod's Crypt disables the grant, and the established trigger still puts two combat-duration red mana units into the pool. |
| Relic Vial | Sacrificing the final Cleric leaves the condition false on the current battlefield. The actual departure carries the frozen observer grant ID. The trigger attribution uses that event and survives to resolve. A Wrath of God batch gives two placed triggers with distinct departing generations and the preserved source/controller, even though the qualifying Cleric is in the batch. |
| Darkblade Agent | Before surveil, combat damage creates no granted draw trigger. Consider establishes native turn history and the resulting trigger draws for the controller. The condition resets at the next turn. |
| Oculus Whelp | Wrath of God destroys the Whelp with its controller's transformed permanent. The departure snapshot carries the Whelp's active grant ID, and the resulting trigger draws. The application retains the transformed condition and leaves structural availability UNKNOWN. |

The eight native scenario tests assert one accepted activated grant with exact source attribution, eleven attributed trigger placements, and one further native trigger placement with condition support UNKNOWN. Four more tests bind source hashes, exact wrapper and body paths, optional-field serialization and same-ID ambiguity. The distinct observation units are narrower than catalog coverage. The unsupported condition and existing group provider boundaries remain explicit, as does the limit on observer attribution when the observer itself departs or renews in the same event batch.

## Verification

The tested application was `a6033a4e683b74c9ba3f2fde8b0245b48c29edc3`, with Temurin JDK 25 and clean Argentum `0878d800edbec56f0c82dac080ae4fec32816b5f` on `app-stable`. The baseline reports were exported from the verified PR #69 tree, which matches merged main at `60de24454646395b8239c6b834fbd8993efee9f5`. This delivery uses the same sibling engine for both sides. Native controls run in the application suite. No independent engine suite was rerun.

The complete Kotlin suite passed 910 tests across 126 classes, with zero failures or skips. The Windows bootstrap built the installed distribution. The rerun used `dev.ps1 test --rerun`. The bootstrap and source guards used `scripts/test-bootstrap.py` and `scripts/test-oracle-snapshot.py`. The installed catalog gate ran through `bash scripts/verify.sh catalog`. The browser checks used the production frontend and installed Explorer with Edge Playwright via `pnpm test:ui` and `pnpm test:live`. All eight interface checks and 18 live checks passed on the final runs.

The installed CLI passed the current catalog gate on the same engine across all runs, with zero confirmed benchmark mismatches and an empty self-diff. An archived report decoded through the installed CLI at the supported schema boundary and also produced an empty diff. The older frozen catalog fingerprints remain separate from the current gate and retain their original revision. The bootstrap and Oracle guard suites passed 17 and three tests respectively.

All 18 controlled Discover comparisons preserved every pre-existing field, including verdicts and admissions, physical-card populations, intrinsic actions and facts, existing grants and attachments, and historical assurance. Each commander received the same objective-independent conditional evidence in all six reports.

| Commander | Eligible physical cards | Added activated grant records | Added triggered grant records |
| --- | ---: | ---: | ---: |
| Judith, the Scourge Diva | 7,728 | 2 | 2 |
| Krenko, Mob Boss | 4,308 | 1 | 2 |
| Kenrith, the Returned King | 20,143 | 8 | 7 |

The isolated production Explorer reported the tested application and engine SHAs through its health endpoint, and `jcmd` verified a maximum Java heap of 1,073,741,824 bytes. The Kenrith run exposed the expected conditional grant evidence in five card responses, including Oculus Whelp's UNKNOWN condition. It retained Helm's separately reported UNKNOWN deck eligibility. All displayed grants retained UNKNOWN source fidelity and their body facts stayed outside the intrinsic fact list.

The initial Kenrith API request hit the server deadline while verification jobs overlapped. The first live test then encountered the busy research worker. The full browser rerun and the separate API probe passed. The focused native tests also required fixture corrections for first-strike timing and the representation of combat-duration mana, recorded in the receipt.

[Verification receipt](evidence/conditional-grants-verification.json) · [Compiled census and remaining paths](evidence/conditional-grants-inventory.json) · [Discover comparison receipt](evidence/conditional-grants-discover.json)

## Next research question

Temporary grants are the strongest next bounded direction. The prior census measured 116 temporary triggered-grant declarations, and the existing model can provide recipient, controller and stack identity. The next question would be how Argentum exposes grant creation and expiry for a small native duration family.
