# Collection counter placement: isolated engine correction

## Scope and implementation

Application base: `6e1c26028fd9dffe6335d9494edce8aa362e32e7`. The approved engine and application pin remain `ebf5bf2e9fbb80d13a556d75491dd462e10c206f`. Its checkout is clean and untouched. The candidate lives on a separate engine worktree/branch, `fix/collection-counter-placement`, at local commit `57aa530bca47e2c36aa37092958c7d283cf11ed3`.

The candidate is not published or merged upstream. An authenticated dry-run push was denied (HTTP 403); no fork was created. `reference/engine-counter-correction/argentum-collection-counter-placement.patch` contains the actual-source correction, native tests and directly related comment updates. `git apply --check` succeeds against the approved checkout. `candidate.json` records its identity and delivery boundary.

The collection executor previously duplicated placement logic. It omitted counter prohibition, emitted events/history for replacements reduced to zero, and did not record a successful player counter-cap lock. A lone prohibition guard would have left those differences.

`AddCountersExecutor.place()` retains its existing target-resolution behavior and delegates its existing placement body to a small internal `placeOn()` operation. `AddCountersToCollectionExecutor` reuses that operation for each existing recipient. This shares projected prohibition checks, existing replacement/cap handling, positive-only mutation/events, first-placement history, player naming and turn locks. Collection dynamic amount is still evaluated once before any recipient changes; nonpositive base amounts and absent recipients remain no-ops. No SDK/public serialization, registry wiring, card definitions, decisions, DTOs or client code change.

The operation returns immutable state/events without running state-based actions or processing triggers between recipients. The collection aggregates its results and leaves normal continuation/settling to the existing engine. The native event tests verify recipient, placer, kind, actual quantity and first-placement flags. No card-name or Oracle-text exceptions are added.

## Source invariant and baseline

The official [Comprehensive Rules](https://media.wizards.com/2026/downloads/MagicCompRules%2020260925.txt), effective 2026-09-25, confirm prohibition precedence, ignoring impossible instructions while continuing, and written resolution order (101.2, 101.3, 614.17c and 608.2c). `rules-and-scope.json` binds the downloaded version and pairs the bounded requirements with tests. Existing counter replacement policy is reused; it is not independently reimplemented or universally certified.

The approved application reproducer was rerun with `VERIFY_KNOWN_ENGINE_GAPS=1`, alongside Tatterkite's direct-counter scenarios. It failed precisely the collection source expectation. Tatterkite's actual native prohibition was present, and the direct scenarios passed. The new application mixed-recipient observation checks the ordinary recipient, subsequent life gain and exact placement events; it records the approved wrong state explicitly rather than calling it correctness.

The finalized generic native fixture has **17 cases**. The same source hash runs before and after: **10 failures and seven passing controls before; all 17 pass after**. Coverage includes three counter kinds, mixed/all-prohibited and multiple ordinary recipients, direct/collection consistency, additive and doubling modifiers, controller scope, zero replacements, maximum-counter no-ops, a player cap lock after source removal, zero/negative quantities, absent/empty members, one-time dynamic evaluation, first-placement flags and state-based death ordering. The player-collection case is an executor-level compatibility check, not a new player-gather DSL claim.

The first cold run compiled the engine's entire test source set and took 20m03s; the finalized baseline took 6m23s and the corrected focused run 3m33s. These include compilation/cache effects and are not a performance comparison of counter placement or a labor estimate. Compact actual cases, failure messages and log hashes are retained in `native-before.json` and `native-focused-after.json`.

## Explicit integration configuration

The same application sources are tested in two configurations. Normal `dev.ps1` uses the clean approved checkout. `scripts/engine-candidate.init.gradle` is opt-in: it requires an exact clean candidate commit descended from the approved pin, rejects the approved checkout, includes the external application only in the main engine build, and puts its output in the candidate worktree's `build/assistant-cli`. It prints both actual candidate and approved revision.

Candidate mode changes only test expectations: the known Tatterkite source assertion must pass, and mutation M7 must have a passing unmodified control. The current Sword definition is retained. An isolated original collection-based Sword fixture compares the repaired primitive without entering the production registry. Tatterkite itself is not modified.

The initial experimental init script also included the application inside Gradle's buildSrc build, causing a convention-plugin configuration error. Restricting the settings hook to the main engine build corrected that setup failure. It was not an engine scenario failure; `integration-setup-failure.json` preserves its diagnosis.

The experiment's configuration manifests and receipts identify actual execution commits and distribution JAR hashes. Existing CLI `argentumCommit` fields remain approved definition/eligibility metadata; they are not used as proof of the experimental runtime. Generated before/after directories and the compact experiment receipt must be read with those manifests. No unapproved runtime is represented as an adopted dependency.

## Reproduce without changing the pin

Create an isolated engine worktree at the approved commit and apply the patch with `git am`; its new local commit may differ because of committer metadata, so obtain that checkout's actual full SHA. Leave the application pin and original checkout untouched. Use JDK 21 and the existing underlying `scripts/gradle-locked` wrapper (the `just` binary is unavailable here; the wrapper reports its Windows no-shlock fallback). Run builds sequentially with bounded workers.

```powershell
. <application>/.env.ps1
# From the isolated engine worktree, using its actual clean commit:
bash scripts/gradle-locked --no-daemon --no-configuration-cache --max-workers=2 -PkotlinCompileParallelism=1 -Pkotlin.daemon.jvmargs=-Xmx4g -I <application>/scripts/engine-candidate.init.gradle -Dmtg.assistant.root=<application> -Dmtg.engine.candidate=<full-candidate-SHA> :rules-engine:test :assistant-cli:test :assistant-cli:installDist --continue
```

The approved baseline uses `dev.ps1 test --rerun :assistant-cli:installDist`. Immutable approved/candidate distributions and the existing `phase29-verify.py --output-base` mechanism keep their controlled CLI runs separate. Full logs, bulk Oracle data, native build outputs and full-catalog reports stay ignored; compact receipts and the patch are tracked.

## Executed regression results

The candidate passed the complete rules-engine test task: **7,507 tests in 1,110 suites**, with no failures, errors or skips. The approved and candidate application configurations each passed **457 tests in 57 suites**. Approved mode explicitly observes the known defect; the separately enabled approved source assertion fails, while candidate mode requires and passes that assertion. Candidate application execution includes the existing direct Tatterkite cases, unchanged production Sword scenarios, the isolated original Sword composition, extension admission and structural checks, translation reliability and all application regressions.

The controlled CLI comparisons show **all nine complete discovery JSON objects equal** across Judith, Krenko and Oviya with the three existing objectives. All three full-catalog coverage reports are equal except for the two explicitly named timing fields; identities, counts, eligibility, mechanics and limitations remain unchanged. All 22 assurance records remain current: 21 provisional and Tatterkite blocked. `controlled-comparison.json` retains both runtime manifests, binary hashes, comparison hashes/counts and mutation outcomes.

All nine candidate semantic mutations now have passing unmodified controls and detected defective variants. M7 was confounded by the approved engine defect; its candidate control passes and the prohibited-counter mutation fails. These are agent-authored bounded checks, not independent certification. `approved-application.json`, `candidate-full.json` and the focused before/after receipts preserve suite/case counts and log identities.

To reproduce the controlled CLI runs, invoke the existing runner once for each retained distribution (same snapshot and translation-review input):

```powershell
python scripts/phase29-verify.py before --output-base generated/engine-counter-correction --dist generated/engine-counter-correction/approved-dist --translation-review reference/alignment-batch2/coverage-status.json --snapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
python scripts/phase29-verify.py after --output-base generated/engine-counter-correction --dist generated/engine-counter-correction/candidate-dist --translation-review reference/alignment-batch2/coverage-status.json --snapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
python scripts/engine-counter-receipt.py
```

The last command validates retained outputs and writes the compact comparison receipt; it does not rebuild engines. Keep each distribution's manifest with its results. Binary JAR hashes are raw SHA-256; text evidence and patch hashes explicitly use UTF-8 with LF normalization. Preserve original failed execution output separately from passing approved known-defect contracts.

## Assurance and outstanding limits

Tatterkite's approved-pin concern remains confirmed and blocked. Candidate success is supplemental evidence, not adoption or independent human certification. The existing concern history, source review and all other provisional definitions remain intact. No browser/UX pass is claimed; state, event, engine and application checks are the execution evidence.

Independent counter emitters, including the distribution family and dynamic-single placement, have separate implementations and need their own bounded review; this patch does not certify or refactor them. Stored collections contain entity IDs; this repair preserves their existing zone/object semantics, skips missing entities, and does not establish general stale-object identity correctness. General replacement-choice policy and arbitrary simultaneous continuous-effect dependencies are not newly certified.

Adoption requires review of the engine patch and downstream results, a publishable corrected revision, and explicit approval before changing the pin. Only then can new revision-bound execution evidence resolve the approved concern; qualification should move to provisional, not automatically to independently qualified. The concise reusable workflow is in COMMANDER_AUTHORING.md.
