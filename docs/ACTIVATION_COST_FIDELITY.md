# Complete printed activation-cost evidence

Increment 4 is an author-led, source-first payment investigation. It neither qualifies entire abilities/cards nor changes discovery, Commander eligibility, the engine, the external population or existing approvals.

## What the method establishes

The comparison unit is one native activated ability's **complete printed base payment tree**. Source expectations were frozen in commit `e61eae35d620f482e4caa9a5851fec0a4510f48b` before native bodies were inspected. The separate inspected bindings were committed as `21e4b29c9f85419957372f6805417db922dc3542`. These are technical held-outs, not blind or independently certified examples.

`src/test/resources/fidelity/activation-cost/requirements.json` retains source/printing/Oracle IDs, captured card and ruling hashes, rules references, source-first analysis and excluded dependencies. `bindings.json` binds each scope to the actual native definition hash, ability index and ID. Raw native definitions, costs, declaration/resolution fields and comparison trees are available in `/api/v1/progress/activation-costs`.

| Scope | Source payment | Role |
| --- | --- | --- |
| Mogg Fanatic | Sacrifice this source | Familiar baseline |
| Caustic Caterpillar | {1}{G} AND sacrifice this source | Combined payment and original missed defect |
| Evolving Wilds | Tap AND sacrifice this source | Tap control |
| Hapless Researcher | Sacrifice this source | Draw/discard remains a resolving effect |
| Sakura-Tribe Elder | Sacrifice this source | Original printing concern retained |
| Burnished Hart | {3} AND sacrifice this source | Technical held-out |
| Mind Stone, draw ability | {1} AND tap AND sacrifice this source | Technical held-out; three simultaneous components |
| Mind Stone, mana ability | Tap | Technical held-out; produced mana is an effect |
| Viscera Seer | Sacrifice one controlled creature, including this source | Technical held-out; selected object, owner may differ |
| Greed | {B} AND pay two life | Technical held-out; independent non-sacrifice payment variation |

CR 602.1a establishes the payment boundary before the colon; 602.2/602.2b and 601.2h require full payment and restore illegal activation; 118.3 requires sufficient resources. Rules 107.4 distinguish generic, colored and colorless mana. CR 701.21a distinguishes controller from owner. CR 302.6 supplies a declaration dependency, not a static cost certificate. CR 118.9 concerns alternative **spell** costs and is expressly not used as an authority for an activation-cost alternative.

The rules capture is the official November 6, 2026 Comprehensive Rules, SHA256 `524f5195a1e5f54bb13c735edf399ac717a4458e2713f398d1a3e6877186b829`; source URL/excerpts are retained in the requirements. Per-card current source captures do not change the October 26 global denominator.

## Architecture and fail-closed comparison

`coverage/ActivationCostModel.kt` projects the live SDK's typed `AbilityCost`/`CostAtom` tree. It does not read Oracle text to decide acceptance and does not calculate payment legality. Source expectations are explicitly reviewed records.

Supported vocabulary: fixed generic/colored/colorless mana; mandatory conjunction; source tap/untap; exact self versus granting-source sacrifice; simple payer-controlled creature sacrifice with count and self-exclusion preserved; fixed life. Only associative mandatory conjunction and the addition of fixed mana are normalized. Other component multiplicity remains. A generic mana requirement is distinct from colorless. Unknown fields/children, unsupported filters, dynamic amounts, hybrid/X/snow/phyrexian mana and alternative/conditional structures yield UNRESOLVED. A supported subset never compensates for an unknown component.

The SDK has no generic mana-OR-sacrifice constructor at this revision. Its real Forage choice is retained as an unsupported alternative, not flattened into AND. Therefore generic alternative-vs-combined fidelity is **not demonstrated**. Untap has structural unit controls, but no source-reviewed untap cohort/native execution claim. Contextual reductions, additional global costs, activation permissions, source-zone/timing restrictions, summoning sickness, targets, replacement outcomes and resolving effects remain explicit dependencies. The result is named complete **printed base** payment to preserve this boundary.

`ActivationCostFidelity` joins existing confident physical identities and native action IDs. It reuses the immutable global baseline and existing definition digest. No new association system, Oracle parser, payment solver or certification gate is introduced. `ActivationCostExecution` parameterizes the existing Lab native transaction builder, which delegates actions to genuine Argentum `ActionProcessor`. Test-only mutated card copies never enter the production catalog or translation ledger.

The existing verification index receives an additive SOURCE_REVIEW record per cost tree and attributable production execution observations. Its counting definition remains `scope-aware-verification-v1`; the added method is independently versioned `printed-base-activation-cost-v1`. Ten cost-tree comparisons become ten requirement applications in that index. The 26 source tree nodes (including conjunction nodes) are reported separately; they are not 26 independent tests or certifications. Five existing action scopes gain depth, five scopes are additional, and four additional physical cards receive review. No duplicate new-card gain is inferred from the strengthened familiar scopes.

Execution freshness binds the exact engine, requirements, inspected bindings, all cohort/mutant/support definition hashes and the combined normalized source hash of the cost model/report/executor/Lab helper. Receipts retain actual dates, application SHAs, expected outcomes, native actions/results, initial/final states and individual assertions. No execution occurs merely by opening Progress. Changed bindings make a receipt stale. Missing receipts are not failures. Mutants stay outside production-card aggregates.

## Adversarial findings and limits

Ten deliberate payment defects are contradicted: missing mana, wrong quantity, wrong color, generic replaced with colorless, missing sacrifice, missing tap, wrong granting source, wrong selected source, sacrifice moved to resolution and wrong life amount. Forage-versus-conjunction is rejected as unresolved. Correct payment with wrong target/effect remains outside payment scope. This does not repair the historical eight-check target/effect miss.

The original eight-check missing-mana miss, original wrong-target/effect miss, Spark generation failure, PR40 baseline and original receipts remain unchanged. New cost evidence does not rewrite those historical conclusions.

The shared native transaction pattern covers sufficient resources, no mana, wrong colors, tapped source, insufficient life, own/self/borrowed creature sacrifice and an unspecified selection. Fixed source, selected creature and fixed life are three parameterizations. Only two scopes need manually grounded target setup (Mogg opponent-player target, Caterpillar artifact target). Unique targets are fixture inputs, not acceptance exceptions.

Same-name witnesses start tapped so a second Mind Stone cannot automatically produce mana in an insufficient-resource control. Borrowed creatures are seeded consistently in both native owner representations. The first exploratory harness exposed these two fixture mistakes; neither required changing source expectations or production mechanics. The final receipt supersedes those diagnostic attempts, not historical research failures.

An explicitly submitted noncreature sacrifice leaves native state/payments unchanged but reports `ExecutionFailed` instead of the expected `IllegalAction`. This is an INCOMPLETE observation, not a clean negative pass. An unspecified creature selection remains UNRESOLVED; its choice/continuation is not certified. No engine change was needed for the bounded cost comparison, and no engine approval/qualification is issued. This narrow transaction outcome is available for separate investigation.

Payments are checked from actual pools/life, tap/sacrifice/zone-change events, old/new object references, requested/actual owner destination, unchanged unrelated objects and stack state. Ordinary effects remain on the stack. This proves neither their resolution nor every replacement, object-generation, activation-restriction or life-at-zero interaction.

## Baselines and independent examination

- `global-commander-baseline.json.gz` remains SHA256 `c22434d2abf3f5f1443508bf03b18c20ebf10e774436fde6c162d92abe185911`.
- The original `verification-baseline.json.gz` remains SHA256 `373191816510bb0c0c5d886d3667f67a2e8f95639511f2c68f8ef90764123440`.
- `verification-cost-baseline.json.gz` is a separate additive observation. Inspect with `research verification-compare`; changed measurement rules must not manufacture fidelity gains.
- Exact measurements, new receipt/baseline hashes, code revisions, comparisons and test outcomes are recorded in the [Product Studio report](PRODUCT_STUDIO_ACTIVATION_COST.md) and `src/test/resources/progress/activation-cost-stability.json`.

A separate reviewer can reproduce the source expectations, inspect native trees/mutants, rerun native observations and challenge the scope. Proposed examination: the ten printed-base payment scopes only, with declared unsupported choices and nonpayment exclusions. The implementer cannot approve this package. Existing qualification remains unchanged and pending; whole-card completeness remains unestablished.

## Reproduction

Use clean maintained `app-stable` at `84fb6147f5b6ae11445f215ce23c3a9bb6a72db1` and JDK 21. Do not silently replace source/baseline files. CLI output paths must be new (CREATE_NEW).

```powershell
.\dev.ps1 test
.\dev.ps1 installDist
# With build/install/mtg-assistant/lib/* on the Java classpath, MainKt:
# research activation-cost-execution --output generated/cost-execution-new.json
# research activation-cost-report --output generated/cost-report-new.json
# research verification-baseline --output generated/cost-baseline-new.json.gz
# research verification-compare --before src/main/resources/verification-baseline.json.gz --after generated/cost-baseline-new.json.gz --output generated/cost-change-new.json
cd web
pnpm build
$env:EXPLORER_URL='http://127.0.0.1:8800'
$env:EXPLORER_BROWSER_CHANNEL='msedge'
pnpm exec playwright test
```

Launch from the application root using the existing one-step Windows launcher:

```powershell
.\scripts\explorer.ps1 -Port 8800 -OracleSnapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
```

The snapshot is explicit and hash verified; omission preserves the application's existing missing-snapshot behavior. Source hash `081a50ef82c767523f955e914153cf988ff045a72e3765b6aa2382567f9eb53a`; 32,109 legal physical cards, 16,927 confidently represented, 52.72%. Commander selection and all four objective definitions remain unchanged.
