# Phased activation-cost examination

This bounded investigation examines the ten Increment 4 cost scopes. It does not extend objectives, Commander selection, card registrations, the native engine or the independent-approval gate.

## Examination protocol and independence

The starting application is merged main `0b23d5e16ac3145118a4ae15e7a66c99b610d24a`. The maintained engine was fetched and verified clean on app-stable at `84fb6147f5b6ae11445f215ce23c3a9bb6a72db1` and remains unchanged.

The implementing coordinator already knew Increment 4's conclusions. A separate no-history agent, `source_examiner`, received only nine original Scryfall card/ruling captures, the full hash-verified rules text and general repository guidance. It did not receive the user's payment table or prior verdicts. Access separation was instruction-based, within the same provider/coordinator and shared filesystem; it was not an externally enforced sandbox or independent human examination. No approval or qualification record was created.

| Phase | Inputs and boundary | Frozen evidence |
| --- | --- | --- |
| A: source derivation | Original Oracle/printing/face identities, text, rulings and full rules; no native definitions, old requirements, code, tests or verdicts | `source-examination.json`, frozen in `0849db4`; original raw SHA256 `151853ce0c427845a04c359f1aa5a58e56c92c1a817e43060d726189e581c6b3` |
| B: native assessment | Frozen A plus fresh exact-main native exports and SDK/engine source; still no original application checks or execution results | `native-examination.json`, frozen in `a6532fd`; original raw SHA256 `07a44c79b23ba32805df78ec3d534bcc19ba4deb996303be53346b45c743e40a` |
| C: adjudication | Frozen A/B, original method/mutations and actual native actions/states; derived coordinator labels are not accepted as proof | Initial `adjudication.json`, then `adjudication-current.json` for the final receipt. Both remain available. |

The full review package is under `src/test/resources/fidelity/cost-examination/`. `inputs.json` is the source-only entry point. To repeat the examination without verdict exposure, derive and freeze A first, inspect `native-inputs.json` and freeze B second, then consult checks, receipts and C. Phase artifacts record actual files consulted, timestamps, provenance, limitations and exact identities. The source agent did not execute the native scenarios; the separately recorded application harness did.

The original rules are the official November 6, 2026 Comprehensive Rules, full-file SHA256 `524f5195a1e5f54bb13c735edf399ac717a4458e2713f398d1a3e6877186b829`. The explicit URL is retained in the inputs. Download that exact text and verify its hash if replaying externally; do not substitute a later rules revision silently. Card/ruling snapshots remain dated and hash-bound, separate from the October 26 global card-universe snapshot.

## What survived examination

The isolated source/native phases agree on the ten printed payment structures: Mogg Fanatic, Caustic Caterpillar, Evolving Wilds, Hapless Researcher, Sakura-Tribe Elder, Burnished Hart, both Mind Stone abilities, Viscera Seer and Greed. This is agreement on complete supported **printed base payment**, not full activation legality or card fidelity.

Source control, ownership and nominal destination were traced through actual SDK/native code and challenged with execution, rather than inferred from the canonical string. Viscera Seer can sacrifice itself, another controlled creature or a controlled creature owned by the opponent. Ownership alone does not permit sacrificing an opponent-controlled creature. Actual sacrifice events, owner-graveyard membership and old/new object generations are retained. Replacement contexts are not examined.

One additional test-only structural variant incorrectly excludes Viscera Seer itself. The unchanged shared cost checker detects that contradiction. All original detected/missed/unsupported variants remain unchanged, including the original missing-mana miss, the expanded checker detecting missing mana, the unsupported alternative and the correct-cost/wrong-target-effect counterexample. No mutant enters the production catalog.

## Why the noncreature selection returned ExecutionFailed

The SDK accepts a list of prefilled sacrifice choices. Native `ActivationChoicePauses.sacrificeChoice` skips its selection prompt when that list is nonempty. `ActivationValidator.checkCostPayable` establishes that some legal payment exists; it does not validate each submitted sacrifice ID. With Viscera Seer present, there is a legal creature available even when the submitted ID names the Spellbomb.

`CostHandler.paySacrificeList` then checks battlefield, controller and filter eligibility of the actual chosen ID. It refuses the noncreature or foreign-controller choice. A downstream payment refusal becomes `ExecutionFailed`. `ActionProcessor` exposes the exact original state and no events for either rejected outcome. These are valid typed negative-control requests, not evidence of an accepted illegal sacrifice. The prior expectation of specifically `IllegalAction` was an unsubstantiated diagnostic assumption; Magic's source rules do not name the engine's enums.

A missing selection is different: the engine creates a `SelectCardsDecision` before payment, offering only eligible creatures. An invalid response is rejected as `IllegalAction` with the pending decision preserved. A subsequent valid response completes the native payment. The old Lab's inability to supply that response was a harness workflow limit, not proof that Argentum lacks the workflow.

We retain the original INCOMPLETE/UNRESOLVED observations and separately show the new path evidence. Prefilled invalid-choice scenarios remain UNKNOWN in the existing aggregate because they are not clean initial-validation refusals. Unchanged state alone is never treated as a legal rejection: typed rejection, event absence, state identity/equality and failure location are inspected together.

## A separate native payment defect

Before seeing execution, Phase B identified that `ActivationValidator.checkCostPayable` returns early for `PaymentStrategy.Explicit`. Its explicit-source check validates listed mana sources, not independently the outer source's required tap. `CostHandler` delegates `AbilityCost.Tap` to an effect-style tap helper and reports success even when tapping an already-tapped object is a no-op. The handler's additional already-tapped guard applies to locked-cost replay, not this ordinary path.

Fresh real execution confirms the consequence on two unmodified native abilities:

| Subject | Untapped Explicit | Already tapped AutoPay | Already tapped Explicit |
| --- | --- | --- | --- |
| Mind Stone mana ability | Completes; actual tap event and mana produced | IllegalAction; no payments/state changes | **Done, no tap event, adds colorless mana: source payment contradicted** |
| Evolving Wilds | Completes; tap and source sacrifice observed | IllegalAction; no payments/state changes | **Done, no tap event, source sacrificed and ability stacked: source payment contradicted** |

`Explicit(emptyList())` requests no additional mana sources for these zero-mana payments; the SDK does not grant permission to omit the outer ability's nonmana cost. The source requirements were frozen beforehand. The verified rules describe tap-symbol payment and prohibit paying it with an already-tapped permanent (107.5, 118.3); they were not rewritten to match the engine.

This is an engine payment-enforcement finding, not a mistranslated printed cost tree. The source definitions remain structurally correct. Mind Stone's draw ability was a static concern but has **not** been executed under Explicit here. Untap, contextual modifiers and other payment strategies are not certified by these examples. No engine source or baseline was changed and no engine PR was created.

## Evidence integrity and counting

The examiner caught an ingestion error: original freeze hashes described raw CRLF JSON, while Git transported equivalent LF text. Initial projection incorrectly reported stale phases. The original raw bytes are preserved in `*.json.raw.gz`; `hash-bindings.json` lists raw and UTF-8 LF hashes. Only line endings may differ. Both original raw identity and canonical text identity must verify; no requirements, JSON fields or mechanical verdicts are normalized away. Tests reject changed content. The original phase artifacts and their raw hash declarations remain untouched.

The final native receipt is `src/main/resources/cost-boundary-execution.json`, SHA256 `f860245a9a137f1d12af8246b4e7ddbff9b27081dee15f0175798e8b4702b573`, executed by clean application `ec6bc9989df4558b9e4db3fda24aa14a5327aba6` on the engine above. It contains **14 scenarios / 16 native actions**: ten bounded expectations supported, two atomic refusal observations with diagnostic limits, and two genuine source-payment contradictions. The first receipt and its original adjudication remain preserved, but are not added again to current counts.

The existing verification index gains only the final 14 new scenario observations. Reviewing the same receipt in A/B/C creates no execution observation. Current source-review coverage stays 13 physical cards / 14 scopes / 91 requirement applications; native availability stays 16,927 / 32,109. Current execution becomes 53 scenario observations on the same 13 cards, including two cards with failures and one with unknowns; categories overlap. Independent qualification stays zero and whole-card completeness stays unestablished.

Potential source-tap investigation reach is calculated from existing projection logic over confidently associated legal primary definitions. It excludes other faces, granted/lowered actions and unprojectable cost trees. The count is a candidate population for investigation, not a count of confirmed affected cards. The prior 4,956-tree / 3,880-card cost vocabulary measurement remains potential applicability, not source certification.

The application owns the compact phase/evidence projection and index joins. Argentum owns actual activation, validation, payments, choices, state transitions and events. Opening Progress reads recorded evidence; it does not execute games or call a model. Existing approval and completeness rules remain authoritative.

## Reproduction and next decision

Use JDK 21 and the clean sibling app-stable checkout. New research output paths are required; existing records are not silently replaced.

```powershell
.\dev.ps1 test
.\dev.ps1 installDist
$java = Join-Path $env:JAVA_HOME 'bin/java.exe'
& $java '-Xmx1g' '-XX:ActiveProcessorCount=4' -cp 'build/install/mtg-assistant/lib/*' dev.mtg.assistant.MainKt research cost-boundary-execution --output generated/boundary-new.json
& $java '-Xmx1g' '-XX:ActiveProcessorCount=4' -cp 'build/install/mtg-assistant/lib/*' dev.mtg.assistant.MainKt research cost-examination --output generated/examination-new.json
.\scripts\explorer.ps1 -Port 8801 -OracleSnapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
```

Fresh test results, all code/source hashes, four-objective comparisons and actual browser captures are recorded in [Product Studio's report](PRODUCT_STUDIO_COST_EXAMINATION.md) and `src/test/resources/progress/cost-examination-stability.json`. The separate native-module `just test-class ActionProcessorAtomicityTest --rerun` could not run because just is unavailable; application execution is not claimed as native-module verification.

Exactly one next investment is recommended: **a separately scoped native source-tap enforcement correction across payment strategies**, with source-grounded native regressions and full application compatibility on an isolated candidate before approval to merge into app-stable. Do not begin that correction automatically.
