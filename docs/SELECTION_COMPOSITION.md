# Focused discovery, basic selection and composition: operational assessment

2026-11-10 · Research prototype and Product Studio recommendation. **No production objective admission, extraction, frontend classification or engine behavior changed.**

**The selection split and elementary composition work. The entire revised admission model is not yet mechanically implemented or validated.** Ordinary resources and requested interaction categories can be recovered without declaring them focused contributors. Union and restriction preserve physical identity and inclusion reasons. Event/history separation is useful, but internal-history, independent-state and modification relationships still require interpretation beyond current discovery. The ordinary-capability baseline remains contextual, particularly for nonland mana sources.

Recommend a bounded normal-Discover pilot of **basic selection plus add/restrict**, keeping current focused evidence intact. Do not claim a universal admission algorithm, automatically migrate historical results, or infer player enjoyment from this CLI experiment.

## What ran

The [offline research CLI](../scripts/selection-composition.py) combines an unchanged complete Kenrith death report, the retained Oracle snapshot, existing confident physical-identity associations and pinned public Scryfall query memberships. It provides:

- Current accepted focused results, unchanged.
- A separately labeled **research event projection**: retain accepted death producers, intrinsic suppliers and draw/token responses; retain legacy enhanced-removal results when their recorded native action is an explicit direct death trigger. Withhold history-only dependencies and expose unresolved associations. This reuses current specialized policy; it is not a new general event/state extractor.
- Basic land cards, creature cards and numeric printed power 4+ from structured source metadata.
- Basic removal, broad board wipes and a narrower creature-destruction wipe category from functional query results.
- Addition of either wipe category to the event projection, and restriction of that projection to creature cards.
- An optional [source-expectation overlay](../src/test/resources/regression/selection-anchor-profiles.json), visibly separate from native observations and accepted membership. The overlay cannot add candidates.

The [protocol](../src/test/resources/regression/selection-composition-protocol.json) was committed at **`62638ab`** before new source/native inspection. Nineteen named controls include seven new purposive challenges; [source captures](../src/test/resources/regression/selection-composition-sources.json) were committed at **`513a530`** before native outcomes. Familiar controls reuse PR #48 rather than repeat its broad policy investigation. These are author-selected known cards, not blinded or representative validation.

## Actual selection results and units

Context: **Kenrith, the Returned King**, five-color identity. Native discovery searched **15,955 eligible authored cards**, accepting **946 cards / 975 pathways**. The composition experiment uses a distinct, declared population: **30,944 normal-layout Commander-legal source cards** from the pinned external snapshot. Only confidently associated native results inside that population participate in composition. Consequently, **36 accepted native cards / 41 rows** are outside its layout/identity join and remain in the original report and explicit diagnostics; they are not new mechanical negatives or regressions.

| Selection within the declared external population | Distinct physical cards | Membership evidence records |
| --- | ---: | ---: |
| Current focused results, mapped into scope | 910 | 934 |
| Research event projection | 907 | 931 |
| Basic land cards | 1,108 | 1,108 |
| Basic creature cards | 17,216 | 17,216 |
| Basic numeric printed power 4+ | 4,115 | 4,115 |
| Basic removal query | 5,898 | 5,898 |
| Basic broad board wipes | 854 | 854 |
| Basic creature-destruction wipes | 221 | 221 |
| Event projection **+ broad wipes** | **1,748** | 1,785 |
| Event projection **+ destruction wipes** | **1,127** | 1,152 |
| Event projection **restricted to creatures** | **747** | 1,515 |

An evidence record is a native pathway reference or a basic selection reason, not always an ability/pathway. Overlapping selections cannot be summed as distinct cards. [The result ledger](evidence/selection-composition-results.json) records context, complete composed Oracle-ID lists, selection identity digests, source/native controls, membership reasons, excluded associations, UNKNOWN counts and full-output hashes.

The projection withholds Tragic Slip's history-only pathway. Barrensteppe Siege and Tragic Banshee have legacy trigger forms outside this narrow direct-death projection and remain explicitly unresolved here. Their original MATCHes remain intact. This is not proof that every omitted legacy dependency is a historical state or irrelevant.

### Query conjunctions make the basic path practical

Following Product Studio's clarification, the narrowed query is:

```text
legal:commander id<=wubrg otag:boardwipe otag:removal-creature otag:removal-destroy
```

The tags `removal-creature`, `removal-destroy` and `sweeper` were verified directly on [Wrath's Scryfall Tagger page](https://tagger.scryfall.com/card/lea/45); the accepted API conjunction was then captured independently. It returned **246 live candidates**, of which **221** fall inside the pinned normal-layout population. Wrath and Damnation are included; Cyclonic Rift is excluded. No Oracle rules parser or custom wipe taxonomy was necessary.

The broad query returned **919**, with **854** in scope; removal returned **6,264**, with **5,898** in scope. Rift's inclusion in the broad board-wipe query is appropriate, not a tag failure. Its exclusion from the narrower conjunction demonstrates that specifying the desired category matters. Functional membership remains source-derived search evidence rather than a claim that a death successfully occurs or every objective restriction is satisfied.

Complete queries, original page hashes, timestamps and cohort memberships are in the [search receipt](evidence/selection-search-receipt.json). In-scope functional candidates showed no differences in the checked layout/type/power/color-identity fields between live records and the pinned snapshot. Live-only or out-of-scope members are retained as exclusions: 65 broad wipes, 366 removal cards and 25 destruction wipes. These are scope/time differences, not estimated false-positive rates.

The source-metadata land and numeric-power selections were also checked against actual Scryfall `t:land` and `t:creature pow>=4` queries. After aligning physical identities and the declared population, their sets agree **exactly**, at 1,108 and 4,115 cards respectively. The prototype leaves 207 variable/nonnumeric printed creature powers unresolved; it does not guess effective power. Multiface/non-normal layouts are outside this basic metadata pilot. Oracle-text search remains an available source-selection option, but the tested conjunction sufficed; no lexical mechanics interpreter was added.

## What the discriminating controls establish

| Control | Verified source/native distinction | Actual prototype outcome and boundary |
| --- | --- | --- |
| Murder | Can destroy our creature; current mechanical MATCH / policy NO_MATCH | Basic removal includes it; focused and projected-event sets exclude it. The possibility of a death is preserved |
| Wrath / Damnation | Destruction includes own creatures; current compatibility MATCH / primary policy NO_MATCH | Basic destruction-wipe selection and union include them **only with that basic reason**. Mass versus targeted removal is not the proposed boundary |
| Attrition | {B}, sacrifice cost, required nonblack creature target, then removal | Focused cost retained; basic removal also includes it. Removal restrictions/uncertainty and cost/output paths remain separate |
| Grave Pact | Our creature dies → each other player sacrifices; token-inclusive trigger can accept a qualifying nontoken death | **No catalog definition available.** Basic removal retrieves the source card, while source overlay expects an event beneficiary. A synthetic native opponent-response contract passes existing extraction/dependency checks; it tests the opponent subset, not broader team-player coverage, a Grave Pact catalog MATCH or gameplay execution |
| Haruspex | Another own nontoken death → draw | Existing supported response retained; creature restriction keeps it because its source card is a creature |
| Skeleton | Original self-return from own graveyard, mana payment and tapped entry | Supplier retained, including under creature restriction. Return is not a death or a proven repeating cycle |
| Walking Corpse | Ordinary nontoken creature card | Basic creature selection includes it; restriction cannot manufacture a focused relationship, so it is absent from the restricted focused set |
| Tragic Slip | Earlier death changes reduction on resolution; any creature death can establish that state | Current historical MATCH preserved in original selection; omitted from event projection, retained by basic removal and identified as state-dependent **only in the explicit source overlay**. No state query was implemented |
| Tannuk | Land entry → damage; extra draw checks the second **resolution** of this ability this turn | Native sequence contains `IncrementAbilityResolutionCountEffect` and a source-ability resolution-count condition. Current draw relationship is UNKNOWN and the increment effect is unsupported. Internal history is not promoted to another objective; no landfall objective MATCH is invented |
| Scute Swarm | Land entry and independent six-land condition evaluated at resolution | Two shared event/token branches remain UNKNOWN. Source overlay permits overlapping event and independent-state relationships; neither is an implemented state objective |
| Hardened Scales | Replacement increases impending positive counter placement on own creature | Native replacement exists, but reusable modification interpretation is absent. Not reclassified as a trigger or initiating placement |
| Forest / Wastes | Ordinary land mana; Wastes has explicitly printed tap-for-one text | Basic land selection retrieves both. Native catalog availability is retained; source selection does not override native eligibility uncertainty. Land membership is not a general mana-source classifier |
| Llanowar Elves / Sol Ring | Native one-mana creature ability / two-mana artifact ability | Source profiles expose a baseline challenge, not an automatic focused mana-engine verdict. Elves is available through basic creature selection; no general artifact/mana selector was introduced |
| Beast Within | Destroy a permanent, then its controller creates a Beast token | Basic removal includes it; no focused death contribution is promoted merely for having an additional output. Token creation has unresolved controller interpretation here and does not replenish an original nontoken creature |
| Fleshbag Marauder | Each-player sacrifice, including our choice; mechanically compatible but current policy-excluded | Basic creature/removal selections retrieve it without changing focused membership. It challenges “all sacrifice effects are distinctive”; costs and interaction-only effects need their actual relationships preserved |

Tannuk demonstrates why historical conditions need **scope**, not merely a HISTORY label. Its same-ability execution history differs from Tragic Slip's independently established turn history. Swarm's land count is independently meaningful and can coexist with its event dependency. Scales is a replacement, not a trigger. The source/native distinctions survive these cases; general executable anchor admission does not yet exist.

The retained PR #48 Exploration evidence supplies the contrasting additional-access example: its native `GrantAdditionalLandDrop` changes an allowance, while ordinary Forest mana does not change the ordinary land allowance. That permission is not currently interpreted as a general supplier fact. The new nonland mana controls show that “ordinary” cannot be resolved universally by checking for printed text, a mana operation or quantity greater than one.

### Multiple reasons without duplicate physical candidates

Grafted Butcher retains two focused references to the same native activation: sacrifice-cost production and original-card self-return supply. Its creature-metadata restriction adds a third reason without inventing another relationship or another physical candidate.

Endemic Plague is the one overlap between the event projection and the narrowed destruction-wipe selection. The union keeps its existing native cost-path evidence and source-tag membership. Thirteen cards overlap the broad wipe set. These are post-run observations, not predeclared held-out successes or proof of multi-card causality. Union never turns Wrath's basic reason into a focused relationship; intersection never turns creature type into the reason an ability qualifies.

## Identity, provenance and counterexamples

Composition keys candidates by **Oracle identity**, not card name or printing ID. Confident existing native associations bridge reports to source cards. Competing/unavailable associations are quarantined; same-name different identities are not collapsed. Each native reason retains original report hash, row index/hash, action ID, path, mechanical compatibility and policy decision. Basic reasons retain source printing/query provenance and explicitly make no native-mechanical claim. Context/population mismatches reject composition.

This mattered during validation: the snapshot contains both the original Llanowar Elves and a same-name token. Initial source acquisition selected the token as its final name match. The actual selector correctly excluded that nonlegal/token-layout record, but the cohort-summary helper looked up the wrong identity. The [identity addendum](../src/test/resources/regression/selection-source-identity-addendum.json) binds the original **`68954295-54e3-4303-a6bc-fc4547a4e3a3`**, distinguishes token **`eaddc7cf-5d58-422a-ba00-6708dee2d853`**, and preserves the original frozen capture. A regression checks both. This is a research source-binding correction, not a production eligibility overlay or silently rewritten evidence.

A file scan also initially suggested Wastes was unavailable. The actual Catalog probe corrected that assumption: it is available without a standalone same-named definition file. Grave Pact's availability gap remains real. Native existence, discoverability of a source path, identity association, Commander eligibility and mechanical interpretation are separate observations.

Other counterexamples remain consequential. A source can be both an ordinary creature and a conventional mana producer; a wipe can have a qualifying sacrifice cost; interactive output can genuinely depend on our event; extra token output can still fail the required input type. A single interaction/ordinary flag cannot resolve all of these. The architecture makes deliberate primary exclusions recoverable through basic selection, but does **not** eliminate all objective-specific policy or establish a universal baseline taxonomy.

## Practical recommendation and next decision

**Proceed with a constrained pilot of the separation and composition, not adoption of a universal admission system.** Basic metadata and combined source tags supply useful candidate sets at modest conceptual cost. Add/restrict work without a synergy graph, strategic scores or a general objective editor. This is stronger operational evidence than retrospectively naming relationships: actual candidates, identities and reasons change as requested while the original evidence remains intact.

The strongest remaining limitation is focused interpretation. The prototype still reuses `DeathGenerationPolicy`; it does not replace it with a proven general rule. Grave Pact needs a catalog representation before it can appear as a real native positive. Historical-state, internal-resolution-history and replacement relationships need bounded shared interpretation before source overlays can become accepted objective findings. User enjoyment and comprehension were not measured; the CLI is not a demonstrated normal browsing experience.

One substantial next increment, if authorized: add a **bounded basic-selection and add/restrict mode to existing Discover**, using the existing explicit source-snapshot/identity path plus versioned functional-query evidence. Limit it to creature/land/printed-power selectors and the verified creature-destruction-wipe conjunction. Preserve current focused admissions; show ordinary/source-only candidates and availability/eligibility basis explicitly; retain all reasons on one physical card. Demonstrate Forest/Walking Corpse retrieval, Wrath through basic union, creature restriction, and source-only candidates without mechanical certification. Use identity-conflict and scope-drift controls, complete unchanged-pool admission comparisons, appropriate application/browser tests and real mobile inspection. Do not build a general editor or simultaneously implement all state/modification objectives. **This next increment has not begun.**

## Revisions, verification and reproduction

Application base/main: **`f039a6bcde462424d9d3a82a06819abbe418a978`**, merged PR #47. PRs #45/#46/#47 were verified merged. PR #48 was **open and unmerged** at the start; its branch remains unchanged at **`3264dc30f5fd29bc490df71ac2f90e2ea80bae14`**. Its committed raw-observation receipt was checked against the reused local artifact, and all **12 common controls' existing death-result rows compare exactly**. No cherry-pick, merge or overwrite of that work was performed.

Unchanged maintained engine HEAD and live `origin/app-stable`: **`bd26b756b8b4d347f19e92bc77ef020919ebfa48`**. Source snapshot: `oracle-cards-20261026210159.jsonl.gz`, SHA-256 **`081a50ef82c767523f955e914153cf988ff045a72e3765b6aa2382567f9eb53a`**. Existing physical-identity evidence uses native digest `af45623ce691b4d3f129121b0ce73e7224adf941295be45d414d0906f3ad0adb`; source and native printings are not assumed identical or independently qualified.

- **35 focused JVM tests passed**, zero failures/errors/skips, at `e16c030`: the three new source/observation/synthetic-contract tests plus existing death correctness, sacrifice boundary, event outcome and self-replenishment suites. Initial two-test observation also passed.
- **19 Python tests passed**, most recently against prototype `e7742db`, including real pinned memberships, the tag conjunction, overlap, physical dedup, same-name token/original identity, source-only cards, UNKNOWN identity, full color identity, variable power, missing actions, source-overlay isolation, context mismatch and failed-query handling. `e7742db` also preserves original prerequisites and partial-support annotations directly in composed membership evidence; subsequent delivery changes are evidence/documentation only.
- Complete public API captures succeeded for all five final queries. Original response hashes and frozen source bytes were verified; output IDs are unique per selection; the synthetic contract remains separate from catalog admission; production source/frontend/build trees are unchanged.
- Earlier guessed tag combinations returned HTTP 404 and were retained as failed probes, not mechanical negatives. The provider's actual vocabulary was read in Tagger, then the final conjunction succeeded. Documentation browsing returned 403; the report relies on actual API results and the observed Tagger UI rather than claiming those pages were read. A Windows helper initially used the wrong text encoding; it was corrected to explicit UTF-8 before producing the identity addendum. No unresolved test failure remains.

With JDK 21 and the clean sibling engine, from the application root:

```powershell
$env:SELECTION_STUDY_EXPORT = Join-Path (Get-Location) 'generated/selection-composition'
.\dev.ps1 test --tests 'dev.mtg.assistant.SelectionCompositionStudyTest'
python scripts/test-selection-composition.py
python scripts/capture-selection-search.py --output generated/selection-composition/new-search-capture
python scripts/selection-composition.py --report generated/selection-composition/kenrith-current.json --snapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz --identities generated/creature-inputs/current-identities.json --tags generated/selection-composition/combined-search.json --profiles src/test/resources/regression/selection-anchor-profiles.json --observations generated/selection-composition/native-controls.json --output generated/selection-composition/results.json
```

For exact replay use the pinned `combined-search.json` in the portable bundle. A fresh capture is a separately dated observation; the capture command requires an empty output directory and never overwrites old receipts. The full focused test command adds `DeathQueryCorrectnessTest`, `SacrificeBoundaryTest`, `EventOutcomeDiscoveryTest` and `SelfReplenishmentDiscoveryTest` to the shown JVM command. The normal full source/identity inputs are explicit research dependencies, not test/build requirements; the Python suite uses a small pinned sample without network access.

Raw current report, API pages, failed probes, native observations, synthetic contract, exact selection output, logs and XML receipts remain under ignored `generated/selection-composition/` and in the portable review bundle. The living direction record received a small clarification; no Primer rewrite, runtime selection API or React composition experience was added. Full application/native/catalog gates, application browser runs, fresh gameplay execution, independent qualification, whole-catalog precision/recall adjudication and user-comprehension studies were **not performed** for this research-only increment. Existing services and historical evidence were preserved. No automatic merge or next implementation is authorized by this report.
