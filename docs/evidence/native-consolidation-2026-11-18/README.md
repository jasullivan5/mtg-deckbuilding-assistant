# Native consolidation verification receipts

Executable application `9304f480e43a59ebece781ece4e5aa2ad2687d6a`; engine `682d4e034a5b9078a6be3146b50258313c9af2fa`; fixed upstream `1e2783f51bdd0436f7bc45ed6e2fce1c8c13a796`.

- `test-summary.json` and `junit.zip`: all 764 application tests and 34,149 native test entries, with zero failures/errors. One SDK entry is an opt-in report writer, skipped by default; 34,148 native tests passed. Named controls include test source hashes, Git blobs, and whether they changed since the merged baseline. Native engine code stayed fixed while the final application-only decoder/export fixes were verified against it.
- `explorer-health.json`, `browser-summary.json`, `kenrith-runtime-1g.json`: actual installed revisions, 18 live Edge checks, eight separate mocked-API UI checks, and the 20,143-card Kenrith population at the normal 1 GiB heap. The task-owned test server was stopped after verification.
- `catalog-comparison.json`: the current gate passed. Frozen historical fingerprints differ and remain unchanged.
- `production-comparison-receipt.json`, `production-final-replay.json`, `built-in-audit-receipt.json`: all eighteen normal production comparisons, raw hashes, populations, changed sections/paths and uncertainty. All final 1 GiB streaming exports are byte-identical to the reviewed intermediate exports. Built-in historical audits used those identical preserved inputs, with a larger heap for two complete exports. Whole populations differ; controlled common-name comparisons remain separate.
- `source-reassociation-review.json`, `source-bundle.json`, `resource-preservation.json`: unchanged source universe and historical resources, explicit current physical associations, and dated source/printing/ruling observations. Association is not eligibility approval or mechanical qualification.
- `activation-cost-receipt.json` and `activation-cost-execution.json.gz`: fresh source-bound printed-payment observations with full native state/event/check traces. Twenty-two agree, two adversarial observations differ, one is incomplete and one remains unresolved. These are observations, not 26 cards or qualified abilities.
- `translation-mutation-receipt.json`: original M1 non-discrimination is explicitly historical under current LKI; M10 tests actual damage-source identity. Original frozen evidence hashes remain available.
- `intermediate-observations.json`: failed baseline/intermediate observations remain failures at their original revisions.
- `native-golden-review.json`, `windows-snapshots-final.json`: thirteen intentional native definition changes; all 213 source snapshot files have LF endings. The 493-check native gate used normal resources and strict assertions.
- `log-manifest.json` and `artifact-manifest.json`: observation log and delivered-artifact hashes. Bulk reports and logs remain in the workspace's ignored generated output, rather than becoming runtime dependencies.

## Replay

Use JDK 21, the exact clean candidate checkout, and the normal candidate/origin guards described in [the engine workflow](../../ENGINE_WORKFLOW.md). Set `ARGENTUM_ENGINE_DIR` to the absolute engine checkout and `ARGENTUM_CANDIDATE_REVISION` to the engine SHA above. Keep Gradle runs serialized; on the verified Windows host `GRADLE_REAP=0` preserved shared daemons.

From the application checkout, run `just --justfile docs/evidence/native-consolidation-2026-11-18/verification.just --working-directory . bootstrap application native catalog`. Build `web` with its frozen pnpm lock, start the installed Explorer normally, confirm `/api/v1/health`, and run `pnpm test:ui` plus `EXPLORER_URL=<local service> pnpm test:live` using the project's browser procedure.

The one-off comparison helpers retain their original workspace layout: restore them to `.integration-tools/` beside `mtg-native-consolidation/`, `argentum-native-consolidation/`, and `mtg-upstream-baseline/` before replaying. Compile `CompareProductionPopulation.java` against each installed distribution separately; it constructs the normal production `Catalog`, including extensions and active corrections. Before uses application `e0fa95970f546c19b79e32bc323af74367d6c353` / engine `bd26b756b8b4d347f19e92bc77ef020919ebfa48`. No diagnostic native-only catalog substitutes for these observations.

Passing these bounded checks does not independently qualify a translation or establish complete MTG fidelity.
