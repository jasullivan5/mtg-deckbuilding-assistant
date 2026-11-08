# Global Commander coverage and reusable verification

## Product Studio report

Progress now separates **Cards**, **Commanders** and **Objectives**, with a bounded source-bound sacrifice pilot in the same page. These are different research dimensions, not stages of one cumulative certification funnel. Discover, Progress and Primer remain the primary destinations. No discovery policy, Commander admission policy, objectives, native definitions or engine code changed.

The whole-format measurement uses the explicit October 26 source snapshot already used by reviewed eligibility associations. It is a dated source population, not a claim to include later releases. Current pilot sources were retrieved separately on November 8; those six observations do not silently change the global denominator.

### What can I do now

Open Progress before running discovery. Compare the complete selected Commander-legal card population with Judith's restricted pool; inspect missing definitions and uncertain associations; examine the bounded commander-selection inventory; see the four actual objective questions and their operational search reach; and inspect six source-reviewed sacrifice ability scopes with separately bound execution observations.

### Launch and three guided investigations

The supported one-step Windows launch remains:

```powershell
.\scripts\explorer.ps1
```

Without a selected compatible snapshot, global card coverage and external commander coverage say **Not measured**. Native mechanical inventory and the existing objective queries remain available. Deliberately reproduce the compatible source and select it with:

```powershell
python scripts/oracle-snapshot.py download --directory generated/oracle --manifest src/test/resources/progress/reference-snapshot.manifest.json
.\scripts\explorer.ps1 -OracleSnapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
```

The fixed manifest has SHA256 `081a50ef82c767523f955e914153cf988ff045a72e3765b6aa2382567f9eb53a`, 25,135,745 bytes and source update `2026-10-26T21:01:59.922+00:00`. No dashboard read fetches a changing bulk source. Incompatible source-bound corrections fail visibly instead of being silently adopted.

1. **Full Commander universe.** Read Cards, expand the global population records, and compare the denominator with Judith's external pool below. Distinguish physical definitions from printings/faces, missing definitions from uncertain associations, and availability from fidelity.
2. **Commanders and objectives.** Inspect selection candidates and filter Daretti to see an eligible source candidate with no native definition. Compare Krenko and Ishai, which pass today's single-selection gate. Inspect unsupported configuration families, then all four objective scopes and their distinct-card/pathway/unsupported units. Operational reach does not establish independent precision or recall.
3. **Reusable fidelity.** Open the sacrifice cohort and inspect held-out Reckless Barbarian or Ball Lightning. Compare Seal of Fire's noncreature arrival with Evolving Wilds' deceptive animated-departure case. Inspect the separate execution receipt and the retained historical Spark failure; source review, static checks, runtime observations and independent approval remain distinct.

## Measurement contract

### Cards

The existing `catalogCoverage` audit now accepts a whole-format scope. The same `OracleSnapshot`, `NativeIndex`, `OracleEnrichment`, effective metadata resolver and one-to-one matching algorithm serve both scopes. No second association system or Oracle mechanics parser was added.

`externalEligibility(source, null)` means no commander's identity restriction. Known banned/not-legal records remain excluded; unknown/restricted legality remains unresolved. Missing/conflicting physical identity information remains unresolved. Known non-deck layouts are excluded. Meld results are not separately counted physical cards; source `all_parts` identifies a record's own component by its printing ID or its unique exact source name within that record's parts. This handles alternative-printing references without approximating an association to a native card. Back/flip/DFC source identities stay single records and the loader collapses duplicate Oracle identities, preserving source conflicts.

The global denominator is confirmed legal physical identities under `physical-format-population-v1`, not all source records. Raw source legality counts, physical/layout exclusions and unresolved pool records remain inspectable. Confident associations divided by the same eligible denominator is the availability percentage. Metadata disagreement does not erase an identity association or certify a translation.

The scoped `--commander` audit retains its original identity restrictions. Global scope does not run commander-specific objectives; doing so is rejected. `audit catalog-coverage --global` uses the same CLI inventory/report machinery as `--commander`.

### Commanders

The source candidate denominator is deliberately bounded: **SDK-identifiable legal single candidates** in the global legal physical pool, using the existing `CommanderEligibility.isLegalCommander` on an eligibility-only projection of source front type/text/printed stats. The projection is never registered, executed or used to change application eligibility. Printed stats are loaded through the existing snapshot reader solely to satisfy the SDK model's creature invariants.

For a confident native candidate, the inventory invokes the actual `inspectCommanderDefinition` gate with catalog provenance and effective metadata. A counted selection success also requires agreeing effective source/native metadata. Missing natives, uncertain associations, unsupported constructions and unresolved/blocked inspections remain disjoint outcomes within the identified denominator.

This is not an exhaustive count of all legally possible individual commanders or configurations. Current rules admit special noncreature forms beyond the existing application's selection model; legendary Vehicle/Spacecraft records and Background components are shown separately for review. Characteristic-dependent exceptions such as Grist, unparsed source characteristics and future forms remain limits of this inventory. Ordinary nonidentified cards are not reclassified as proven ineligible by this measurement.

Individual cards and pair configurations are different units. Some partner-bearing creatures are usable alone; Partner, named partners, Friends forever, Choose a Background, Doctor's companion and pre-game identity arrangements do not acquire pair support here. No combination denominator or per-candidate operational-success count is invented. Representative end-to-end checks supplement the measured predicates.

### Objectives

The report imports the actual four `server/ExplorerModels.kt` objective definitions and runs existing `discover` queries sequentially on the same Judith-native eligible physical population with no conventional filter. It asserts identical ordered input populations across all four reports.

For each objective it reports examined cards; distinct matching, unresolved, nonmatch and unsupported-bearing cards; cards with no emitted relevant pathway; and separate matching/unknown/nonmatch pathway and unsupported-record totals. Card categories overlap, and a no-pathway card can still carry unsupported native records. Absence is not NO_MATCH.

Operational reach does not establish independent search correctness. Existing frozen source-led regression samples and positive/negative/adversarial tests are useful bounded evidence, not an independent catalog reference set. **Correctness at catalog scale is not measured**; no recall/precision percentage is calculated.

Reanimation, token and graveyard-state evaluation consume shared `MechanicalFact` records through their existing query implementations. The death objective retains its legacy pathway projection. This is an inspectable implementation relationship, not an objective-reuse score. General death-response payoffs, cross-card dependencies/packages, affordability and timing satisfiability, generalized renewable capabilities, substitutions/deck-slot opportunity cost and competitive-position relevance remain outside current expressiveness.

## Source-bound sacrifice pilot

### Source-first scope and review status

The six exact printing/Oracle identities, current Oracle text, rulings, retrieval times and source-derived expectations are retained in `src/test/resources/fidelity/source-sacrifice/`. The official current rules were confirmed through Wizards' rules page and the November 6, 2026 rules download; the requirements record retains that URL, the complete downloaded-file hash and relevant excerpts. The full download stays under ignored generated output.

Expectations and the generic method were committed at `16ee0f8` before held-out execution. Prior source-sacrifice investigations were already known, so this is not a blind independent experiment. **Codex Build is the author/reviewer. No separate human approval is recorded.** Independent source grounding is not independent qualification.

| Scope | Source role | Expected distinction |
| --- | --- | --- |
| Mogg Fanatic | Activation cost | Exact source pays after a legal target is chosen; damage resolves separately |
| Seal of Fire | Activation cost | Sacrificed enchantment is a noncreature card at arrival |
| Evolving Wilds | Tap and sacrifice cost | An animated battlefield creature still arrives as a land card |
| Spark Elemental | End-step trigger resolution | The same battlefield object must remain available when the trigger resolves |
| Reckless Barbarian | Held-out cost variant | Untargeted mana activation pays with its own creature source |
| Ball Lightning | Held-out trigger variant | Reuses end-step source-sacrifice requirements |

One relevant sacrifice ability per card is reviewed, not full card behavior. Eight shared structure checks apply to all six scopes: physical source binding, exact source, battlefield-to-owner-graveyard nominal movement, cost/resolution role, arriving card characteristics, declared targets, tap requirements and trigger timing. A ninth requirement concerns actual object-generation behavior and is not promoted by static inspection. Native facts come from the existing extractor; typed action fields are checked against source expectations without Oracle regex acceptance gates or a new interpreter.

The six scope records supply expected parameters, not card-name acceptance rules. Two generic ordinary execution patterns cover activation payment and trigger resolution. Five existing curated Lab positions retain their card-specific setup, including required-target rejection, animated Wilds and Spark's zone round trip. No new prominent Lab or case-study destination was added.

### Evidence binding and responsibility

The explicit `research fidelity-pilot --output <new-path>` command executes actual Argentum actions. It records scenario identity, mechanic, states/events/object references, observed result, engine SHA, application revision, requirement digest, definition digests and harness digest. Dashboard reads never execute this command automatically.

A recorded receipt is current only when actual engine revision, source requirements, all six definitions and the packaged harness digest agree. Missing, stale, contradicted, unresolved, incomplete and passing results stay distinct. The existing qualification gate and extension ledger are unchanged; no inherited definition is promoted to QUALIFIED or full-card SOURCE_REVIEWED.

Potential reach counts distinct native physical definitions and source-bound fact occurrences with the same structural prerequisites across the measured native inventory. Those definitions have not thereby been source reviewed. The pilot does not estimate global error rates or claim every eligible variant generalizes.

The historical Spark Elemental failure remains in [source-sacrifice research](SOURCE_SACRIFICE_DISCOVERY.md), bound to engine `03869c08665bf476a8b40dacbfbcd501c54a33bd`. A current passing adverse branch does not erase that observation.

### Early failures retained

The first explicit execution attempt completed the bounded actions but failed to publish its receipt because the harness-property lookup used the wrong classloader. No aggregate pass was published from that attempt. Initial static tests then exposed a platform hash-format mismatch: source capture had hashed Windows CRLF bytes while verification used canonical UTF8 LF. The original raw capture hashes remain in the requirements record alongside corrected canonical hashes; source bytes and mechanical expectations were not rewritten.

The initial tap check looked for `Tap` rather than the SDK's existing `CostTap` discriminator and falsely contradicted Evolving Wilds. Inspection of the native cost and existing UI formatter identified the instrumentation error. The generic check now uses the exact SDK discriminator; positive and deceptive-negative assertions preserve the source requirement. A test also compared a serialized zone spelling rather than the SDK enum and was corrected to decode the existing enum. These were harness/test defects, not alleged native card defects or relaxed source acceptance standards. Failed logs remain under ignored `generated/global-coverage/`; first requirements/method history remains in Git.

## Reproducible global baseline

The explicit CLI creates a new immutable output path:

```powershell
.\build\install\mtg-assistant\bin\mtg-assistant.bat research baseline --oracle-cards generated/oracle/oracle-cards-20261026210159.jsonl.gz --output generated/global-coverage/new-baseline.json.gz
.\build\install\mtg-assistant\bin\mtg-assistant.bat research compare --before <baseline.json.gz> --after <later-baseline.json.gz> --output generated/global-coverage/new-comparison.json
```

The compact baseline retains the complete source identity universe with explicit legal/physical states, confident native identities, missing/uncertain associations, metadata issues, exact snapshot/native/application/engine identities, measurement and matching versions, bounded commander inventory and separate objective scope. Compressed retention avoids duplicating the full bulk card source. A baseline is never silently overwritten; the CLI uses CREATE_NEW.

Comparisons validate identity uniqueness and record-derived numerators/denominators. They separate newly added legal source cards, existing cards becoming legal, cards no longer legal, newly integrated previously missing cards, resolved uncertain associations, lost coverage among still-legal cards and absent source records. Changed schema, physical policy or matching definitions suppress comparable gain claims. The same source hash with changed identity/legality/physical roles is also flagged. Source growth is not treated as a regression, and resolved uncertainty is not mislabeled as a newly authored integration.

The first global baseline has no earlier comparable global measurement. Historical Judith discovery stages remain a separate observation series.

## Architecture and API

`coverage/CapabilityInventory.kt` projects existing selection and discovery interfaces. `coverage/GlobalBaseline.kt` owns compact baseline retention/comparison. `server/FidelityPilot.kt` provides narrow source-bound checks and explicit bounded execution using the existing Lab engine harness. `server/ResearchProgress.kt` reuses those projections and caches them for one running build. React displays and filters existing records; it does not decide MTG legality or mechanical acceptance.

The local API adds `/api/v1/progress/global`, `/commanders`, `/objectives`, `/pilot` and `/global-baseline` beneath the existing progress namespace. Expensive reads retain the existing single research worker/deadline and do not expire Discover results. Capability panels load sequentially to avoid competing for that worker. Native inventories and the original Judith comparison remain available with their original units.

## Remaining limitations and next decision

No exhaustive legal commander/configuration denominator, per-candidate execution count, independent objective-quality benchmark, corpus fidelity percentage, whole-card certification or overall completion percentage is established. The source universe remains the explicit dated snapshot. Full definition availability would still leave contextual evaluation, resources/timing, compositional dependencies, substitution and competitive-position research unfinished.

The recommended next bounded decision is an independent source-first review of the pilot's exact ability requirements and evidence bundle, including the held-out and deceptive cases, before considering any broader fidelity promotion or extension of the common abstraction. Do not automatically expand to more cards or change eligibility/objective policy. This follow-up is not started by this increment.

## Delivery evidence

The concise [Product Studio report](PRODUCT_STUDIO_GLOBAL_COVERAGE.md) provides the working experience, guided investigations and screenshots. Product Studio's assessment determines product acceptance; tests and a PR alone do not.

| Measurement | Result and unit |
| --- | --- |
| Global legal physical identities | 32,109 |
| Confident native associations | 16,927 / 32,109 (52.72%); inherited 16,905, extension 22 |
| Missing / uncertain associations | 15,143 / 39 (2 ambiguous, 37 identity review) |
| Effective metadata disagreements | 1,052 associated definitions; overlaps with availability |
| Raw source legal / banned / not legal | 32,116 / 83 / 6,850; seven legal meld results excluded from physical denominator |
| Unresolved legality / physical pool | 0 / 0 in this selected snapshot |
| Judith external eligible / confidently associated | 12,828 / 6,658 |
| Bounded single candidates / selection gate passed | 3,436 / 1,466; not all legal commanders or per-candidate execution |
| Candidate missing / uncertain / unresolved-blocked | 1,772 / 6 / 192 |
| Separately flagged special noncreature / Background / unresolved source forms | 32 / 29 / 19; outside identified-single denominator |
| Objectives and common examined population | Four / 6,111 native physical cards each |
| Pilot static check applications | 48 supported, 0 contradicted, 0 unresolved; 6 execution-only requirements untested statically |
| Actual execution | 11 passing bounded scenarios across six cards; two generic ordinary patterns and five existing curated positions |
| Potential structural reach | 647 native physical definitions, 671 source-bound facts; only six abilities source reviewed |

Revisions and source bindings:

- Base merged main: `fbc49a10245161954e14e70744c5215a60090464` (PR #38).
- Clean implementation/baseline/pilot execution: `bac84a5ba50739f9af950518072550be67cf700b`.
- Packaged evidence build used for browser review: `2494b64ab231f71c374c1ca02f7aa32ece4ee64b` (its parents contain all measured Kotlin code; later delivery changes are frontend anchor repair, documentation and receipts).
- Clean maintained engine: `84fb6147f5b6ae11445f215ce23c3a9bb6a72db1`, `app-stable`; no engine changes.
- Global source update: `2026-10-26T21:01:59.922+00:00`; SHA256 `081a50ef82c767523f955e914153cf988ff045a72e3765b6aa2382567f9eb53a`; 25,135,745 bytes.
- Retained compressed baseline SHA256: `c22434d2abf3f5f1443508bf03b18c20ebf10e774436fde6c162d92abe185911`.
- Pilot requirements SHA256: `a52cf900e5788cca7e85b51aaa9d9b8ae0f194d15bf2e836cbcd1a6d49f2fbfc`; harness SHA256 `520a5b3d03a024731586b6ca6e37d3e23acfd6d1fb934f97a6ea154536519e7a`.
- Pilot executed `2026-11-08T08:18:53.942330500Z`. Exact six definition hashes, native actions, events, scenario IDs and observations are in `src/main/resources/pilot-execution.json`.

Actual validation commands (JDK 21, Windows; outputs in ignored `generated/global-coverage/`):

```powershell
.\dev.ps1 test ':assistant:installDist'
java -Xmx1g -XX:ActiveProcessorCount=4 -cp 'build/install/mtg-assistant/lib/*' dev.mtg.assistant.MainKt research fidelity-pilot --output generated/global-coverage/pilot-final.json
java -Xmx1g -XX:ActiveProcessorCount=4 -cp 'build/install/mtg-assistant/lib/*' dev.mtg.assistant.MainKt research baseline --oracle-cards generated/oracle/oracle-cards-20261026210159.jsonl.gz --output generated/global-coverage/baseline-final.json.gz
java -Xmx1g -XX:ActiveProcessorCount=4 -cp 'build/install/mtg-assistant/lib/*' dev.mtg.assistant.MainKt research compare --before src/main/resources/global-commander-baseline.json.gz --after generated/global-coverage/baseline-final.json.gz --output generated/global-coverage/baseline-readback.json
.\scripts\explorer.ps1 -Port 8798 -OracleSnapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
# In web/, with EXPLORER_URL=http://127.0.0.1:8798 and EXPLORER_BROWSER_CHANNEL=msedge:
pnpm build
pnpm exec playwright test
```

The full Kotlin suite passed **571 tests, zero failures/errors/skips** (7m 9s). The retained baseline was read through the comparison CLI: valid record-derived numerator/denominator, identical universe/definitions and no fabricated changes. Independently recomputed live API global counts equal all retained baseline counts.

The controlled discovery command was `discover --commander 'Judith, the Scourge Diva' --objective <id> --json --output <path>`, run on an isolated clean merged-main worktree and the new packaged implementation, with the same engine. All four full report files are **byte-for-byte identical**, including eligibility, decisions, pathways and evidence. Compact hashes, population and exact revisions are retained in [global coverage stability](../src/test/resources/progress/global-coverage-stability.json). Bulk reports remain ignored. The clean completed worktree was removed normally after checking source state, upstream ancestry and active consumers.

The first browser run passed 28/29 checks and exposed a duplicate HTML ID between the new commander panel and Discover's existing datalist. The panel now uses `commander-coverage`; neither eligibility nor data changed. The final full browser rerun passed **29/29 checks** in 2.1 minutes; actual 1440px and 390px screenshots validate this repair. The first fourth-objective CLI launch overlapped the launcher's installDist refresh and failed class loading; a completed rerun after packaging is the retained comparison, and its complete report is identical to main. This was a validation orchestration failure, not a concealed mechanical difference.

No standalone engine suite was run because engine source was unchanged; the application suite and pilot execute the actual maintained engine. macOS/Linux launchers and browsers other than Windows Edge were not exercised. No independent human source approval, exhaustive commander discovery run, global objective-quality benchmark or representative-gameplay validation was performed. Historical Judith gains and Spark failures remain historical observations, not fresh executions on the current build.
