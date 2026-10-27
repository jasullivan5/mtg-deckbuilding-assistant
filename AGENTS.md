# Development instructions

This long-term Commander deckbuilding research project values mechanical accuracy and reproducibility over release speed. The user directs new objectives and major architecture; implement assigned scope without inventing a next phase.

- Kotlin/JDK21/Gradle; preserve Argentum's exact ARGENTUM_COMMIT and native CardRegistry/CardDefinition/CardScript/mtg-search integration. Do not edit upstream or upgrade unrelated dependencies.
- Extend existing shared native extraction/facts/predicates. Keep objective policies separate from mechanical compatibility. Preserve action/cost/effect/branch/controller/ownership/zone/target/quantity/timing relationships and explicit UNKNOWN/unsupported states.
- No production card-name exceptions, new Oracle parser, subjective utility score, speculative ontology, database/UI or simulator without assignment. Judith's verified native extension and engine scenarios remain intact.
- Oracle identity enrichment is observational: keep association, eligibility metadata and mechanical support separate. Bind it to the exact snapshot, native identity fingerprint and Argentum pin; increment the mapping algorithm version for rule changes. Metadata correction candidates require a separate reviewed assignment before affecting discovery.
- A still has legacy death/removal-specific projection in PathwayExtractor/MechanicalQuery; B/C query shared facts. Do not portray the architecture as fully generalized.

Commands from root:
- Setup/compile: `./scripts/bootstrap.sh`; incremental compile/install: `./dev installDist`; application build: `./dev build`.
- Focused tests: `./dev test --tests 'dev.mtg.assistant.Phase25FactsTest'`; all application tests: `./dev test`.
- Routine gate: `./scripts/verify.sh fast`; complete gate: `./scripts/verify.sh full`; expensive catalog-only gate: `./scripts/verify.sh catalog`.
- CLI: `cli/build/install/mtg-assistant/bin/mtg-assistant discover --commander 'Judith, the Scourge Diva' --objective create-creature-tokens`.
- Other objectives: own-nontoken-creature-deaths and reanimate-own-creatures; same Commander filters.
- Benchmark: launcher `audit benchmark --snapshot generated/result.json --benchmark benchmark/phase25/review-v1.json --json --output generated/benchmark.json` (choose matching review).
- Coverage: launcher `audit coverage --snapshot generated/result.json --json --output generated/coverage.json`; cross-examiner: `audit inconsistencies` with snapshot and matching --benchmark.
- Regression: launcher `audit diff --before generated/before.json --after generated/after.json --json --output generated/diff.json`.

Use dev/dev.ps1, not root `gradlew test`: the upstream checkout owns Gradle settings. Windows/setup details are in README/CODEX_HANDOFF. No new formatter is imposed.

Use focused tests; shared extraction/eligibility/policy changes require all tests and controlled discovery comparisons on unchanged pools. Inspect unexpected matches/exclusions/evidence changes. Frozen source-led samples are not whole-catalog recall or external adjudication. SDK construction is not engine-execution proof. Report blocked/unexecuted checks candidly.

Keep source, tests, compact reviewed inputs and provenance in Git. Never commit upstream, caches/builds, generated full-catalog JSON, credentials or machine-specific config. Preserve real history; no force-push over unrelated work. See docs/ARCHITECTURE.md for actual extension points and CODEX_HANDOFF.md for known issues.

- Commander eligibility consumers must use Catalog.eligibility effective metadata. Raw native identity/legality helpers remain diagnostics. Activate only individually reviewed, source-bound correction records; preserve native definitions and original discrepancy history. See docs/ELIGIBILITY_CORRECTIONS.md.
