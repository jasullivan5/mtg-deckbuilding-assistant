# Integrating a missing commander

Use native Argentum definitions, not a second rules interpreter. Assisted authoring means a reviewed SDK definition plus source evidence and consequential engine tests; it does not automatically translate arbitrary Oracle text or certify every interaction.

## Inspect before authoring

After setup and `dev.ps1 installDist` on Windows (`./dev installDist` on Bash), use the installed launcher:

```powershell
.\cli\build\install\mtg-assistant\bin\mtg-assistant.bat commander inspect "Oviya Pashiri, Sage Lifecrafter"
.\cli\build\install\mtg-assistant\bin\mtg-assistant.bat commander inspect "Krenko, Mob Boss" --json
```

Inspection uses exact, case-sensitive native lookup. It distinguishes upstream definitions, application extensions, and absence from the pinned catalog. Absence is not a statement that the Magic card does not exist. Structured blockers distinguish unresolved legality/identity, confirmed format exclusion, ineligible cards, non-front aliases, unsupported construction and invalid native structures. Successful inspection exits 0 even if selection is blocked; inspect `selectable` and `blockers` in JSON. Invalid command syntax and blocked discovery exit 2.

Single fixed-identity commander selection is supported. A partner-capable legendary creature can be selected alone; selecting a pair, a standalone Background or pre-game chosen colors is not supported. Existing native explicit commander overrides remain usable.

## Verify sources and native coverage

1. Check `Catalog(emptyList())` and search authored upstream definitions at `ARGENTUM_COMMIT`; never replace a definition just because it has not been independently tested here.
2. Retrieve the exact printing from Scryfall's named-card API with `exact` and `set`, plus `rulings_uri` (requests require User-Agent and Accept headers). Verify Oracle text, faces, types, costs, characteristics, Oracle ID, full color identity and current legalities. Retain dated source evidence under `reference/`; map only formats represented by native `DeckFormat`.
3. Inspect the pinned SDK, existing definitions and effect executors. Trace costs, targets, conditions, controller/owner relations, timing and zone changes. Reject approximations of consequential mechanics; choose another bounded example if faithful implementation requires an upstream change outside the assignment.
4. Distinguish structural errors or explicitly recorded omissions from lack of independent verification. `CardValidator` checks native structure, not complete correspondence to Oracle text. An empty script alone is not proof that a card is incomplete.

## Author and register

Put a native `CardDefinition` in `cli/src/main/kotlin/dev/mtg/assistant/extensions/`, using the existing SDK DSL. Preserve authoritative identity/legality metadata and relevant faces; do not use card-name exceptions in extraction or queries. Add an `ExtensionDefinition` to `Extensions.all` with dated `APPLICATION_EXTENSION` provenance and a source path. Record scenario references in `DefinitionReview`; these describe bounded test coverage, not universal correctness.

Registration validates every nested native definition, required metadata/provenance, case-insensitive names/face aliases and Oracle identities against both upstream and other additions before registering anything. Known-incomplete additions are refused. On a future pin change, a newly authored upstream version must cause a collision rather than silently override either definition: compare them, preserve tests/provenance, and deliberately retire the extension in a separately reviewed change.

## Verify mechanics and discovery

Write isolated SDK/inspection/validation tests for metadata, ability structure and adversarial cases. Write a separate `<CardName>ScenarioTest.kt` using native `ScenarioTestBase` / `GameTestDriver` when claiming consequential mechanics: actually activate/cast/trigger, pay costs, resolve the stack, and assert state. Include meaningful negative cases and relevant rulings. Serialization is not engine execution.

Oviya is the second worked example (KLD #165): two mana-and-tap activations create colorless artifact creature tokens. Her Construct base stats are sampled once at resolution, excluding the new token. Scenarios cover changing the creature count in response, a source leaving, zero creatures, fixed later stats, mana, tap/summoning-sickness restrictions, controller restrictions and instant-speed activation. These scenarios do not certify every replacement/copy/control interaction. Source snapshots and WotC rulings retrieved on 2026-10-26 are in `reference/oviya-*-scryfall.json`.

The routine `scripts/verify.sh fast` gate includes commander integration, Oviya and Judith scenarios as well as the existing integration/fact tests. Run focused tests, then the complete application suite:

```powershell
.\dev.ps1 test --tests dev.mtg.assistant.CommanderIntegrationTest --tests dev.mtg.assistant.OviyaPashiriSageLifecrafterScenarioTest --tests dev.mtg.assistant.JudithTheScourgeDivaScenarioTest
.\dev.ps1 test
.\dev.ps1 installDist
```

Select the commander and run all three existing objectives. A commander need not contribute to any objective. Check full identity filtering and compare existing commanders on unchanged native input pools. Classify raw catalog/count changes separately from eligible-pool, mechanical-verdict and evidence changes. Oviya adds one green card to the catalog; Judith and Krenko cannot include her. `CommanderIntegrationTest` checks their entire eligibility lists (apart from that new excluded row) and controlled A/B/C action/fact/result evidence. Since the extractor and queries are unchanged, this bounded comparison does not require full-catalog regeneration. Broader shared extraction/eligibility changes require the existing full comparison workflow.

Frozen reviews and the generated all-colors benchmark remain applicable; their documented UNKNOWN and unresolved cases must not be relabeled as mechanical nonmatches. Generated JSON, build outputs and upstream sources stay ignored.

## Ordinary native cards

The same registration and verification workflow applies to noncommander cards. Phase 2.7 adds Krenko's Command and Bitterblossom through Extensions.all, with source/ruling evidence, validation, engine scenarios and cross-commander/objective tests. See CARD_COVERAGE.md for the external missing-card inventory and PHASE2_7_REPORT.md for the bounded Assay experiment. A generated proposal remains unverified until its consequential scenarios actually execute.

## Reviewed eligibility metadata

Existing native definitions can receive a small application-owned, individually approved eligibility correction without authoring a replacement definition. Commander inspection/selection and discovery use Catalog.eligibility; original native mechanics and metadata remain available. Inspection JSON/text records applied corrections and original values. Identity/Commander-format search filters and coverage share these values. See [ELIGIBILITY_CORRECTIONS.md](ELIGIBILITY_CORRECTIONS.md). Missing/unreviewed fields still block admission; corrected metadata does not certify mechanics or expand supported construction rules.
