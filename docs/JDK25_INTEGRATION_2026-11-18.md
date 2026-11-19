# JDK 25 amendment to native upstream consolidation

**Verified on Windows:** 34,148 native passes (one optional skip), 766 application passes, 41 browser passes, all 18 production exports and historical audits. Normal Explorer and file exports retain the 1 GiB heap. The complete JDK 21/25 comparison has zero material differences.

This amendment supersedes the original report's JDK 21 support decision and runtime verification. The [original consolidation report](NATIVE_CONSOLIDATION_2026-11-18.md) remains the historical record of the twelve native adoptions, mechanical corrections, source observations and earlier JDK 21 execution. The project now supports JDK 25 for development, compilation, testing and runtime, aligning with the incorporated upstream platform instead of maintaining a separate Java compatibility policy.

## Exact candidate and upstream alignment

- Executable application: `529a78ea38b12b13598fd3c06f2e59ec9a4e9abe`.
- Engine: `3969d0f6dca34fe79eb52d8ba703366ea4004121`.
- Prior JDK 21 executable application: `9304f480e43a59ebece781ece4e5aa2ad2687d6a`, delivered with evidence at `1db2f9e6d17aa5b856f8bb91555cc3b60cba06bb`; engine `682d4e034a5b9078a6be3146b50258313c9af2fa`.
- Fixed incorporated upstream: `1e2783f51bdd0436f7bc45ed6e2fce1c8c13a796`. No newer upstream commits were integrated.

Six engine files now match that upstream snapshot byte for byte: `buildSrc/build.gradle.kts`, the `kotlin-jvm` convention plugin, `mtg-search/build.gradle.kts`, daemon JVM criteria, dependency versions, and `CardDiscovery.kt`. These restore JVM toolchain 25, upstream daemon provisioning criteria, ClassGraph 6.0.1, explicit classpath scanning and `getAllClassesImplementing`. The former ClassGraph 4.8.184/older discovery API adaptation is removed. Applicable engine workflows and contribution instructions also require 25.

Application setup guards, Windows launcher, CI and current developer instructions require 25. Its separate definition-source scanner explicitly opts into classpath scanning and uses `Class.forName` with the application classloader, following upstream instead of the removed `ClassInfo.loadClass` API. The initial compile caught that second API change; the corrected clean application revision supersedes the failed first attempt. There is no alternate compatibility path. Existing wrapper versions remain application 9.6.1 and engine 9.8.2. No unrelated Kotlin, frontend or infrastructure dependency was upgraded.

The complete tree delta against the fixed snapshot shrank from 52 changed files (+888/-130 lines) to 44 (+868/-97). Nine files cease to differ, including the six configuration/discovery files and three inherited workflows. One additional inherited contribution page now correctly says JDK 25, where the incorporated upstream page still said 21. The remaining paths are listed in the upstream-alignment receipt.

The fork retains application policy, native metadata/card corrections, mandatory source-tap enforcement, source-object identity during sacrifice, collection counter placement and bounded native regressions. These are mechanical corrections, not Java compatibility adaptations. The Windows native test launcher still invokes Git Bash from Python; this platform adjustment is independent of the JDK baseline. Gradle's upstream `shlock` semaphore is unavailable in Git Bash, so these heavy builds were serialized, with shared-daemon reaping disabled. No shared daemon or unrelated checkout was stopped or modified.

The full browser invocation also exposed pre-existing supplementary checks that still treated the original 16,927 native associations as current and used the earlier Progress cohort. Those checks now independently assert the immutable 16,927 baseline and the already established 21,319 current associations; Progress keeps its five-objective cohort separate from six Discover objectives. Its source-population check requires the existing hash-checked Oracle snapshot. Fresh screenshots now go to ignored generated outputs; all historical documentation images were restored byte for byte. That browser-maintenance commit (`263c46d`) changed no backend, frontend production, Kotlin test or frozen-resource files. Subsequent allocation fixes below are separately recorded, with complete application and browser verification on the final clean code revision.

## Bounded memory corrections discovered during expanded verification

The supplementary browser checks were also exercised with the complete pinned Oracle source snapshot. That broader workload exposed a cold Progress heap exhaustion at `263c46d`, followed by retained-projection pressure when navigating back to Discover. The intermediate shape-sharing revision `e4bd3fe` completed cold Progress, but later navigation still failed. Revision `a5ce239` passed cold Progress → Kenrith counter discovery → Kenrith deaths → Progress at 1 GiB, then its supplementary verification view timed out while reconstructing all extraction output to obtain only instruction records. Revision `cbfdcea` then passed 40 of 41 browser checks, including every required UI/live check, but reopening detailed mechanical groups after the complete browser discovery sequence still exhausted the heap. Streaming the serialized-node grouping at `63e1b0b` preserved the complete 90 MB audit and its cold 1 GiB execution, but rebuilding it beside a retained Kenrith report still needed less temporary input data. The final revision also feeds extraction into that same audit one definition at a time. These intermediate failures and the deliberately cancelled superseded application runs remain recorded; they are not counted as passes or attributed to Java alone without a matched workload experiment.

Bounded allocation changes preserve the existing outputs: an audit-local pool shares equal immutable canonical JSON subtrees; complete reproducible Progress extraction/coverage projections may be reclaimed; Progress projections retain only the required fields and collect them one native definition at a time; native coverage groups are accumulated during independent serialized traversal instead of retaining every original JSON tree and node occurrence. Group keys, counts, first five representatives, reasons, canonical shapes and fingerprint text are preserved. They use the same walker and preserve record ordering, per-root alpha-renaming, counts, UNKNOWN, unsupported evidence and immutable baselines. The pre/post sharing Judith token coverage export is byte identical (37,018,611 bytes). The complete pre/post streaming native audit is also byte identical at 1 GiB: 90,426,831 bytes, 21,699 physical cards, 256,022 typed nodes and 57,706 groups. Its SHA-256 is `3ce4a387333dae27e3ba8202ce1e40b86415c2c5422656e14e28b1094890e248`, without normalization. Two new regression cases verify subtree sharing semantics and ordered per-definition action, unsupported, instruction and fact equivalence across seven real controls. Final full-suite and runtime checks below cover the resulting code. Heap size, request deadlines and browser timeouts were not increased.

## Fresh verification

Temurin **25.0.4.1+1-LTS** and `javac 25.0.4.1` were actually executed. Kotlin is 2.4.21; the application wrapper is 9.6.1 and the engine wrapper is 9.8.2. The actual Gradle/compiler/test-worker executable paths identify JDK 25, and representative SDK, discovery, rules and application classfiles target major 69 (Java 25).

| Fresh gate | Result |
| --- | --- |
| SDK | 713 passed; one optional `SdkTailReport` case skipped |
| Rules engine | 9,479 passed |
| Era 1993-1999 | 1,768 passed |
| Era 2000-2002 | 659 passed |
| Era 2003-2007 | 2,143 passed |
| Era 2008-2016 | 2,481 passed |
| Era 2017-2022 | 6,391 passed |
| Era 2023 | 2,773 passed |
| Era 2024 | 3,475 passed |
| Era 2025 | 1,737 passed |
| Era 2026 | 2,036 passed |
| Strict native catalog | 493 passed |
| Application Kotlin | 766 passed |
| Native total, above SDK/rules/era/catalog modules only | **34,148 passed; one optional skip** |
| Bootstrap/install and normal Windows launcher | Passed on the exact clean candidate; frontend production build passed |
| Setup guards | Git Bash: 17 passed. PowerShell: 17 enumerated, OK with five Bash-only skip observations |
| Comparator / source snapshot guards | Five comparator and three Oracle fixture checks passed; the current catalog gate also reran comparator checks |
| Browser, production frontend against the real service | **41 passed**, zero skipped/flaky: eight synthetic UI, 18 required live, 15 additional existing checks |
| Current catalog gate | All six benchmark runs passed with zero confirmed mismatches; identical-report diff empty |
| Production Discover file exports | All 18 completed at `-Xmx1g`; zero material differences from the verified JDK 21 candidate |
| Historical report audits | All 18 completed using the existing offline 4 GiB two-report limit |
| Printed-cost native execution | All 26 outcomes/checks and source/binding/harness/definition hashes retained |
| Health / normal runtime | Actual application `529a78e…`, engine `3969d0f…`; ordinary Kenrith discovery passed at 1 GiB |
| Full-source Progress and detailed mechanical coverage | Passed before and after retained Kenrith runs at 1 GiB; complete 90 MB audit hash unchanged |

The current catalog gate preserves `historicalFingerprintsMatch: false`: its older pre-consolidation baseline is not silently reblessed. That historical comparison is separate from the complete JDK 21/25 comparison, which has zero material differences.

The SDK/rules/era/catalog suites use the final engine revision; subsequent application allocation changes do not alter that engine source tree. The full application, runtime, browser, CLI and catalog checks use the final clean application revision. Native controls include mandatory tap payment (26), collection counter placement (17), source-zone identity (33), Spark Elemental (six), Sunforger (three), Planewide Celebration (four) and Tyrite Sanctum (two). These controls are subsets of the module totals, not additional passes. The two new application regression cases bring the previous 764 to 766.

Normal Explorer remains `-Xmx1g -XX:ActiveProcessorCount=4`; file exports use the same limits. The complete pinned-source workload measured 39,049 source records, 32,109 eligible physical cards and 21,319 current native associations. Detailed native coverage was read both before and after real Kenrith counter/death runs, with the exact full-audit SHA-256 checked on both responses. All three immutable baseline downloads also completed after the browser/runtime sequence. The owned verification server was stopped intentionally after final ready health; launcher exit on teardown is recorded separately from successful gates.

## Controlled behavior and preserved evidence

All 18 normal production Discover exports completed under JDK 25 at `-Xmx1g -XX:ActiveProcessorCount=4`. Their preserved JDK 21 inputs were first hash-checked against the committed prior production receipt. A full JSON comparison checks every field and list in every report, without restricting to a common subset or dropping UNKNOWN, unsupported structures or assurance. Only top-level current `argentumCommit` and literal current-runtime engine blob/tree URL prefixes are accounted for as provenance.

There are **zero material differences**. Judith retains 7,728 mechanical cards, Krenko 4,308 and Kenrith 20,143 for each of the six objectives. All 21,699 physical eligibility rows, 21,689 native physical definitions, ten application extensions and 22,147 registry names remain consistent. Matched pathways, actions, targets, costs, conditions, effects, facts, UNKNOWN, unsupported evidence and historical assurance associations are equal. Only `eligibility` and `results` contain current-runtime URL changes; assurance records remain equal without normalization. Native source URLs still identify the same files. Strict catalog snapshot verification separately checks compiled native definition payloads.

All 18 built-in historical audits completed on the JDK 21/25 report pairs. Every changed audit row is attributed `EXPLANATION_ONLY`, reflecting the current engine source URLs. The audit retains existing duplicate pathway bases and emits `UNRESOLVED_PATHWAY_IDENTITY_CHANGE` when a duplicate row differs even in a URL. Its unchanged audit attribution logic and the full ordered JSON equality establish that these warnings are not new mechanical differences; they remain disclosed rather than being normalized away or marked resolved. Offline two-report audits used the prior 4 GiB limit; production exports and Explorer retain 1 GiB.

Fresh printed-cost execution retains all 26 outcomes and checks: 22 `AGREES`, two intentional mutant `DIFFERS`, one `INCOMPLETE`, one `UNRESOLVED`. Source requirements, bindings, harness and all native/mutant/support definition hashes equal the prior observation. New execution has its own timestamp and exact JDK 25 candidate revisions; old observations and qualification remain unchanged.

All original application source/evidence resources, historical failure records, golden definitions, cost bindings and the prior committed receipt bundle remain unchanged. Engine card implementations, native test sources and snapshots are unchanged by this amendment. No fingerprint or identity guard was rebaselined. The twelve native adoptions and ten remaining application extensions are retained; passing execution and source associations do not confer independent whole-card qualification.

## Reproduction and limits

The [exact verification recipes](evidence/jdk25-integration-2026-11-18/verification.just) and read-only production/comparison/audit runners are included in the new receipt bundle. Their paths name this assignment's isolated application/engine worktrees. Full raw production reports remain in ignored `generated/jdk25-integration/production-after-529a78e`; their hashes are committed. The JDK 21 comparison inputs remain in `generated/native-consolidation/production-after` and are checked against the original committed receipt before comparison.

For a checked-out application candidate on Windows, select the exact clean engine candidate and a real JDK 25, then use the documented launchers:

```powershell
$env:JAVA_HOME = '<JDK 25 installation>'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:ARGENTUM_ENGINE_DIR = '<clean engine PR #9 checkout>'
$env:ARGENTUM_CANDIDATE_REVISION = '3969d0f6dca34fe79eb52d8ba703366ea4004121'
.\scripts\bootstrap.ps1
.\dev.ps1 test
.\scripts\explorer.ps1 -OracleSnapshot '<hash-checked oracle-cards-20261026210159.jsonl.gz>'
```

Use the existing JDK 25 installation/provisioning supported by Gradle/Foojay, without changing machine-global Java configuration. Select the clean candidate with `ARGENTUM_ENGINE_DIR` and its full `ARGENTUM_CANDIDATE_REVISION`. Run documented bootstrap, complete application tests/install, SDK/rules/all nine era/strict catalog suites, current catalog gate, production Discover/export and Explorer/browser checks. Both wrappers, the actual Gradle and Kotlin compiler processes, classfile target, and packaged health provenance are independently observed in the new receipts.

The [artifact manifest](evidence/jdk25-integration-2026-11-18/artifact-manifest.json) binds the separate JDK 25 bundle to this executable pair. It includes [all JUnit results and source-bound controls](evidence/jdk25-integration-2026-11-18/test-summary.json), the raw JUnit ZIP, [toolchain evidence](evidence/jdk25-integration-2026-11-18/toolchain.json), [upstream alignment](evidence/jdk25-integration-2026-11-18/upstream-alignment.json), [complete production comparison](evidence/jdk25-integration-2026-11-18/production-comparison.json), [historical audit attribution](evidence/jdk25-integration-2026-11-18/historical-audit-summary.json), [runtime/retained-run measurements](evidence/jdk25-integration-2026-11-18/progress-discover-memory.json), [full audit byte equality](evidence/jdk25-integration-2026-11-18/full-native-audit-equivalence.json), [browser results](evidence/jdk25-integration-2026-11-18/browser-summary.json), [resource preservation](evidence/jdk25-integration-2026-11-18/preservation.json), and [intermediate attempts](evidence/jdk25-integration-2026-11-18/verification-attempts.json). Original JDK 21 receipts remain unchanged in their original bundle.

The existing engine #9 and application #61 remain coordinated draft PRs, open and unmerged. Later report/evidence-only application commits do not change the tested executable source tree. Windows is the freshly executed platform; Linux/macOS execution and hosted workflow dispatch are not claimed. Optional SDK report generation remains opt-in. Frozen baseline differences from earlier feature work remain historical observations, rather than Java-upgrade regressions or fresh assurance.
