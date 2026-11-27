# Temporary ability grants

The existing effect walker keeps each reached single-recipient grant under the action that creates it. The record retains the exact native effect and target, the authored path, mode and prerequisites, and the end-of-turn cleanup checkpoint when applicable. The granted body uses the same cost, effect, target and event interpretation as static grants. Its source is the receiving object, and its player references belong to the activator or captured trigger controller. The original target filter remains attached to creation. Its controller restriction does not become a restriction on who can later use the ability.

Creation observations use adjacent native resolution states and the native resolution event. A stored target must bind the exact battlefield generation, and a modal grant must bind the captured chosen mode and its target slot. The observer compares native store multiplicity and requires a single matching addition. The grant store has a raw source ID and no unique creation identifier. It cannot establish attribution on its own. Equal native ability IDs can leave a later use ambiguous even when distinct origins are observable at creation.

Availability reads the receiving object and runtime grant without requiring the originating spell to stay on the stack. An end-of-turn observation uses a native cleanup transition and the disappearance of the unique record while the same recipient remains. Recipient renewal can separately remove availability. The persistence observer uses the previously captured stack generation and its receiving source and controller, so loss of availability does not erase it.

Current exact creation covers bounded front-definition ordinary spells and abilities, direct and ordered effects and captured root modes. Runtime trigger observations are limited to single self combat damage to a player. Effects reached through other branches and durations retain their native declarations and explicit uncertainty. Group effects, copy-derived grants, exact creation during unresolved choices and general layer interactions remain outside the observed subset.

Five printings were frozen against the merged application baseline `e0eaddbc1c9e611ef2f0628b9212fb871fc8bb4c` and maintained engine `0878d800edbec56f0c82dac080ae4fec32816b5f`. [Source requirements and native file hashes](../src/test/resources/regression/temporary-grant-sources.json) · [Comprehensive Rules effective November 6, 2026](https://media.wizards.com/2026/downloads/MagicCompRules%2020261106.txt)

## Catalog census

The compiled census inspected 21,699 physical cards and 22,487 authoring containers. The 147 declarations below occupy 136 physical cards. The merged baseline emitted zero temporary-grant relationships. The candidate walks all 136 single-recipient declarations and preserves the 11 group declarations as explicit untraversed paths in the census.

| Native declaration family | Declarations | End of turn | Traversed | Bounded creation | Bodies with facts |
| --- | ---: | ---: | ---: | ---: | ---: |
| Activated, single recipient | 20 | 8 | 20 | 5 | 14 |
| Triggered, single recipient | 116 | 60 | 116 | 41 | 97 |
| Activated, group recipient | 11 | 9 | 0 | 0 | 0 |

The 46 bounded creation records occupy 42 physical cards. They include 11 direct effects, 30 ordered effects and five modal branches. The 22 other single-recipient end-of-turn nodes need additional path, recipient or availability-zone interpretation. Other durations include permanent, until-source-cast, affected-counter and source-controller gates. Body facts are even available for some unsupported creation paths. Fifteen activated and 33 triggered bodies had no unsupported body nodes in the current walker. These dimensions overlap and describe structural evidence only.

## Native checks

| Card | Native observation |
| --- | --- |
| Run Wild | P1's spell granted regeneration to P2's creature. The receiving controller paid one green mana. A same-name creature stayed ungranted. The spell left the stack and the grant remained through the end step. After cleanup the same action failed. Cloudshift before resolution fizzled the grant, and Cloudshift after activation removed availability while the established stack object persisted and resolved. Two true origins produced two native entries, so exact activation supplier attribution stayed UNKNOWN. |
| Glorious Sunrise | Choosing the land mode created a grant on the opponent's Island. Its controller tapped it for three green mana at the native mana checkpoint. Cleanup removed the grant. The life mode gained three life and kept the grant store empty. |
| Trash the Town | The third spree mode granted an opponent's creature the draw trigger. Combat damage put a trigger on the stack under the receiving controller. The recipient's controller drew two cards after a blink removed the grant. A separate native cleanup removed the same grant kind and a later qualifying combat event produced no trigger. |
| Commando Raid | Combat damage from the receiving creature triggered the grant. The tested controller selected an opposing creature, chose the optional instruction and dealt three damage using the recipient's power. Noncombat damage dealt by another receiving creature produced no trigger. Native cleanup removed the grant. The native target declaration filters opposing creatures, where Oracle specifies the damaged player. The scenarios exercise two players. |
| Quicksmith Spy | The exact native source-controller duration remains present and UNKNOWN for this increment. The structural control confirms it has no end-of-turn checkpoint. |

The focused tests assert nine attributed creations, three attributed activations including one immediate mana activation, three attributed trigger placements, four cleanup expiries and two stack persistence checks after renewal. They also reject missing resolution events, wrong paths, illegal or renewed recipients, unchosen modes and indistinguishable activation suppliers. These are counts of test observations. Whole-card source fidelity remains UNKNOWN.

## Discover and verification

The application baseline is the merged PR #70 tree. Its retained census and 18 earlier production exports come from the exact same tree content. The final source and observed application build is `e99b422613fb5367177fa3cb5d290d4b49b3d839`, with clean Argentum `0878d800edbec56f0c82dac080ae4fec32816b5f` on `app-stable` and Temurin 25.0.4.1.

| Commander | Eligible physical cards | Added activated records | Added triggered records |
| --- | ---: | ---: | ---: |
| Judith, the Scourge Diva | 7,728 | 6 | 46 |
| Krenko, Mob Boss | 4,308 | 4 | 16 |
| Kenrith, the Returned King | 20,143 | 14 | 112 |

Each row gives the same temporary-grant counts in all six objectives. The comparison checks every other report field, including admissions and verdicts, effective eligibility, pre-existing grant evidence, targets, attachments, collections, linked movements and historical assurance.

The first catalog run exceeded the installed CLI's 1 GiB heap during report self-diff. The offline audit now shares the decoded inputs for the same file and drops action and fact trees before reading a distinct second report. It still compares recorded decisions and opaque result payloads. The updated CLI successfully compared two distinct full-size Judith token reports from the merged and candidate implementations with zero recorded changes. The focused historical tests also cover an opaque archived trigger and a deliberate opaque-result change across separate files.

The Windows bootstrap installed the CLI. The 17 bootstrap guards, three Oracle snapshot guards and production frontend build passed. The repeated `bash scripts/verify.sh catalog` passed with zero confirmed benchmark mismatches, the same engine on every run and an empty self-diff. Frozen historical fingerprints remain on their original comparison basis.

The complete `dev.ps1 test --rerun` suite passed 919 tests across 128 classes with zero failures or skips. The nine new tests are seven native scenarios and two structural controls. The first focused compile needed the correct SDK damage-pattern fields. The first scenario run needed the native modal option prompt and cleanup discard decision. Both were corrected before the full suite.

All eight interface tests and 18 live Edge tests passed. The isolated Explorer health endpoint reported the tested pair and `jcmd` measured a 1,073,741,824 byte maximum heap. The real Kenrith Explorer run exposed the four eligible control cards' grant paths, recipient evidence and end-of-turn checkpoints. It preserved Quicksmith Spy's UNKNOWN effective deck eligibility. The returned temporary grants retained UNKNOWN source fidelity and their body facts stayed outside the intrinsic fact list.

[Verification receipt](evidence/temporary-grants-verification.json) · [Compiled census and exact unsupported paths](evidence/temporary-grants-inventory.json) · [All 18 Discover comparisons](evidence/temporary-grants-discover.json)

## Next research question

Bounded end-of-turn group grants offer the smallest next experiment. Nine compiled group declarations already use the demonstrated cleanup family. Establish exact live membership and observe whether each recipient can be bound without duplicate-provider assumptions before taking on new duration families or composing larger effect chains.
