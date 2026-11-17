# Full upstream Argentum integration attempt — 17 November 2026

**Status: partial candidate; application acceptance is blocked. Do not merge either candidate.** The full upstream history and source are integrated on an isolated engine branch. JDK 21 compilation and the retained engine corrections work in the checks below. Normal application startup/discovery still refuses duplicate translations, and source-bound assurance checks expose additional incompatibilities. No stable branch was merged, reset, rebased, or force-pushed.

## Revisions and strategy

| Reference at investigation | Exact revision |
|---|---|
| Upstream snapshot integrated | `21bc1908bad22b3a6d4c84e5447d7b736655dbd0` |
| Fork tracking `main`, before explicit fast-forward | `6cc9ef42b5ac4b4eabc1189100d129813fff4fb6` |
| Preserved engine `app-stable` | `bd26b756b8b4d347f19e92bc77ef020919ebfa48` |
| Upstream / stable merge base | `ebf5bf2e9fbb80d13a556d75491dd462e10c206f` |
| Application `origin/main` baseline | `e0fa95970f546c19b79e32bc323af74367d6c353` |
| Tested engine candidate | `56eeb394ef4fd2b9b7467b2ae95b3ccf04ac0906` |
| Tested executable application candidate | `ccaf1926ec754799711fa921466de2f636137e7e` |

The repository was shallow initially; missing history was fetched before determining ancestry. Live comparison confirmed 1,641 upstream commits / 22 fork commits since the common ancestor. Fork tracking main was 1,277 commits behind, with its head an ancestor of upstream. Its explicit, non-forced fast-forward to `21bc1908` succeeded. During validation, upstream advanced first to `e7cde249b4b0ba1b39b026d2e20bfae8018ce748`, then to `ec70462e5789af7789e0135d1e9a85bd607848f2` at the final 15:50 local recheck; tracking main was safely fast-forwarded to each tip after checking ancestry. The tested candidate remains at the original upstream snapshot and is four upstream commits behind that recheck (two M15 changes and two AI-baseline updates). No generic fork-sync action was used.

Both candidates use `integration/full-upstream-2026-11-17` in their respective repositories. Engine merge `8002e26a974775203973cc7014878c5d4f70fa71` has stable and upstream as its two parents. Both remain ancestors of the tested head. Four conflict files concerned counter placement and source sacrifice; a merge was practical and retains future merge ancestry. All upstream modules and source capabilities remain present, including components the application does not consume.

The named historical branch `fix/collection-counter-placement` is absent from live remote heads. Its commit `d72f2c365a3006b0c3eb41271c4f0e35f782c392` was fetched and retained under `refs/archive/full-upstream/historical-counter`; it was not substituted for the stable base. Stable already contains the corresponding `007301f316` correction.

The normal engine remains clean on app-stable. The primary application remains clean on its pre-existing feature/chosen-target-costs branch at `3689f46`; the new work uses isolated engine/application worktrees. A separate application baseline worktree uses the unchanged stable engine. Existing sibling checkouts, branches, archival data and unrelated work were retained. No upstream PR was submitted.
## Fork corrections and build compatibility

- **Mandatory tap payment retained:** validation independent of Explicit mana selection, recursive composite source-tap detection, and refusal of missing/already-tapped payment sources. The effect tap atom can still perform its existing no-op. Payment rollback remains native.
- **Source identity retained and adapted:** sacrifice resolves the original object through current upstream reference checks, then checks its logical battlefield zone and projected controller. Upstream's general sacrifice prohibition is also retained. No card-name exception was added.
- **Counter collection correction retained and adapted:** collection placement reuses resolved-recipient handling for prohibitions, replacements, caps, placement history and events. Upstream's deferred spell-entry counters, per-kind restrictions and `totalCount` event field survive. The obsolete trigger-detector warning was replaced by current upstream documentation after tracing the shared placement path.
- **Fork safeguards retained:** maintained origin, clean candidate SHA, manual compatibility workflow, source/evidence history and application provenance remain guarded.

Upstream raised Java to 25 and ClassGraph to 6. The user chose preserving JDK 21 support where feasible. `0adb6a9` restores JDK 21 toolchains/daemon criteria and ClassGraph 4.8.184's scanning API, while retaining upstream's shared corpus index, Kotlin 2.4.21 and module organization. `3de9082` aligns inherited workflow JDK declarations, adapts retained counter tests and their event totals, and lets native Windows Python invoke the test-class Bash child. `56eeb394` resolves Bash to its configured absolute executable before spawning it on Windows, avoiding System32 launcher precedence; its Gradle 9.8.2 dry run passed. These are explicit compatibility deviations from upstream. Hosted workflows and unconsumed server/AI/gym modules have not been certified by the application checks.

## Application adapter and demonstrated blockers

The application compiles and installs against the exact candidate on JDK 21. `10e40fe` maps `ScaleCounterPlacement` and its `Player` placer relation: doubling keeps the existing interpretation; opponent-only placement is explicit; halving stays retained and UNKNOWN. `3a4d919` maps `totalAtMost` and keeps aggregate caps, different-name/controller distributions, one-per-type/color and non-controller target choosers outside the bounded discount potential. `f1be875` also retains the newly added emerge-sacrifice restriction as UNKNOWN across event, return, legacy-removal and entry-counter interpretation. Three added adversarial controls passed. `ccaf192` corrects the new synthetic trigger construction to use the native trigger DSL. No objective policy, source acceptance rule, approval or frozen expectation was relaxed.

**The normal catalog remains blocked by 12 explicit overlaps.** A read-only native-only export found 22,118 registry names and these actual overlapping application definitions: Bitterblossom; Galadhrim Ambush; Hour of Promise; Judith, the Scourge Diva; Krenko's Command; Labyrinth of Skophos; Might of Murasa; Oviya Pashiri, Sage Lifecrafter; Planewide Celebration; Spined Tyrranax; Throne of Makindi; Tyrite Sanctum. Default construction refuses Spined Tyrranax before overwriting anything.

All 12 upstream versions have matching Oracle-text strings, but lack the application versions' archived Oracle IDs. Four freshly serialized scripts are identical; others differ. Notable structural differences include Hour of Promise's Desert filter (`land` versus `permanent`), Planewide Celebration's explicit graveyard `fromZone` versus an absent bound, and Tyrite Sanctum's permanent `AddSubtype` versus `AddCreatureType`. These are differences to review, not demonstrated rules regressions or equivalence. This comparison establishes neither source approval nor translation/evidence equivalence. Both exports and original archived definitions are preserved.

Other failures remain visible:

- **Sunforger eligibility binding:** upstream adds search signaling and `EmitLibrarySearchedEventEffect`; the old source/definition-bound correction correctly rejects the changed definition. Its historical fingerprint was not replaced.
- **M1 mutation discrimination:** the formerly defective Predatory Urge damage-source mutation and its unchanged control both satisfy the bounded execution assertion now. The source probe still records `PU4`; execution discrimination is lost in that scenario. Upstream commit `c4f7380afaf535755ef6f74fae88b4bcfd98ad83` adds departed-object damage-source resolution, explicitly including `Self` and last-known information. Code tracing suggests this explains why the former mutation now survives; it does not prove equivalence in every context. This needs a fresh source-derived adversarial contract, not re-blessing the old observation. Qualification remains provisional.
- Two availability assertions expect Oviya/Krenko's Command to be missing from the old native catalog. They expose legitimate upstream availability changes; no frozen records were rewritten to hide them.

Selecting/retiring these source-bound translations, migrating metadata bindings and qualifying replacement evidence are consequential acceptance work. This partial candidate deliberately leaves the existing admission barriers intact. It is not a compatible revision pair.

## Verification

| Recorded check (context and limits below) | Result |
|---|---|
| Stable application bootstrap / CLI installation | Passed |
| Stable complete Kotlin application | 740 tests / 105 suites; all passed |
| Candidate bootstrap / required SDK, catalog eras, rules/search and application compilation / CLI install | Passed on JDK 21 |
| Candidate complete Kotlin application | 743 tests; 398 passed, 345 failed; no skips |
| Normal installed candidate Explorer startup / health | Java exited 2 on Spined Tyrranax collision; no ready health response observed on checked free port 8875 |
| Candidate SDK | 714 tests / 127 suites; zero failures/errors; one opt-in report skipped |
| Candidate core rules | 9,471 tests / 1,392 suites; zero failures/errors/skips |
| Candidate era scenarios | All nine eras passed in the broad native run: 23,419 tests; core plus eras total 32,890 tests / 9,341 suites, zero failures/errors/skips |
| Candidate native catalog tests | Raw Windows run at `3de9082`: 27 CRLF-only snapshot failures; canonical copied-resource replay at final `56eeb394`: all 493 test records passed (no assertions/source goldens changed) |
| Bootstrap guards | 14 PowerShell passes, 3 Bash-only skips |
| Offline Oracle snapshot downloader | 3 passed |
| Offline catalog comparator | 5 passed; custom comparator self-control had zero differences |
| Frontend locked install, TypeScript and production build | Passed; candidate/baseline frontend Git trees are identical |
| Synthetic browser UI checks, Edge | 8 passed |
| Live stable-service browser checks, Edge | 17 passed, 1 failed |

Of the 345 application failures, 336 directly report extension collisions. Five CLI exit-code assertions are consistent with that refused catalog. The remaining four concern two new native availability cases, the Sunforger binding and M1. Nine fresh Lab execution tests and eight source-sacrifice scenarios passed within the application run. The retained native suites passed: mandatory tap payment 26, collection placement 17, Spark Elemental source identity 6 and deferred spell-entry counters 5. Those overlap the native totals and are not extra independent observations.

The stable health endpoint on port 8873 confirmed clean application `e0fa959...` and engine `bd26b756...`. Its one live-browser failure is pre-integration: Midnight Reaper's evidence panel displays the damage-to-you scope while a source-investigation assertion expects `draw 1`. The screenshot and trace are retained. The first synthetic-browser attempt could not start because bundled Chromium was absent; the successful rerun used installed Edge.

**Blocked compatibility checks:** normal installed candidate startup was attempted and exited 2 on the catalog collision; the health connection was refused. Candidate live-browser tests, the production catalog gate and ordinary Judith before/after comparison remain unexecuted/incomplete. The diagnostic below does not replace them. No hosted workflow, server/client game, AI, gym, Assay, mtgish or whole-card qualification pass is claimed. The 27 raw native catalog snapshot failures are attributable to checked-out CRLF resources: every fresh mismatch equals its committed Git blob byte-for-byte. Canonical diagnostic replays at `3de9082` and final `56eeb394` change only copied build resources and leave all source resources and strict assertions intact; the raw failing observation belongs to `3de9082`. A synthetic-test compile error and a launcher precedence error were corrected; their original logs remain retained. The final verification binds the recorded delivery heads. Initial Java 25 bootstrap/native attempts were canceled or failed at the Windows child invocation and are retained as non-passes. Whole-import whitespace warnings belong to upstream; the fork delta's `git diff --check 21bc1908bad22b3a6d4c84e5447d7b736655dbd0` passed.

## Controlled mechanical comparison

An additional **native-only diagnostic**, explicitly absent extensions and correction plans on BOTH sides, ran all six objectives with Krenko, Mob Boss. It uses the installed, revision-bound distributions and changes no production catalog or acceptance rules. The ordinary Judith baseline reports for all six objectives are separately retained.

Top-level native card definitions grow from 16,995 to 21,670; registry names including faces grow from 17,351 to 22,118. The Krenko mechanical population grows from 3,466 to 4,300 native names. The same 3,466 admitted names form the controlled intersection for all six objectives; no old admitted name is lost. Full populations, exclusions and raw report hashes are retained, rather than assuming whole-catalog numerical equality.

- No common card gains or loses objective acceptance, and no common card newly gains UNKNOWN status at card level. This is a bounded observation, not unchanged semantics or full recall.
- Infernal Captor now exposes separate conditional alternatives. Death MATCH records increase 233 to 234; graveyard-arrival UNKNOWN records increase 252 to 253. The extra records are on an already represented card and are not independent capabilities/cards to sum.
- Twelve common eligibility entries change Commander legality UNKNOWN to NO_MATCH for known-banned examples; full identity values are unchanged. They were excluded from mechanics in both runs.
- New library-search/exploit emissions remain explicitly unsupported. Mana/copy SDK refactors preserve unsupported reporting. On the common pool, raw action/native/fact/prerequisite differences remain available, including 8,904 to 8,908 action records and new keyword records for Grab the Reins/Incite War.
- Of 748 changed prerequisite strings, 663 differ only by a displayed `requireEmergeSacrifice=false` field. This is a classification of textual differences, not normalization of frozen evidence or proof of mechanical equivalence. Other structural changes remain inspectable. Actual result decisions, constraints, branches and source identities were not normalized away. The comparison separately reports raw differences and normalization of literal current-runtime Git URLs only.

The common list is an exact native-name population, not independent approval of physical identity or whole-card fidelity. The native-only diagnostic excludes the very extension/metadata acceptance work that blocks production compatibility.

## Remaining work and maintenance

1. Review all 12 paired translations against retained sources and current adversarial execution, choose their intended active definitions, and retain both original provenance and historical concern/qualification records. Matching names, Oracle text or four script trees is insufficient for blanket substitution.
2. Create separately reviewed, current source/definition bindings for changed eligibility records, starting with Sunforger; do not transplant the old execution/approval claims.
3. Investigate M1's changed execution behavior and add a discriminating fresh source-derived adversarial case while preserving its old failure/probe records.
4. Incorporate the four subsequent upstream commits through `ec70462e` when preparing the next candidate. Rebuild an exact clean pair, rerun the complete application/catalog/health/live-browser gates, and complete the normal Judith common-population comparison. Investigate any further failures exposed after the constructor barrier is resolved.
5. Address canonical snapshot resource handling on Windows without changing frozen JSON content. Resolve the pre-existing Midnight Reaper browser scope expectation independently of integration acceptance. Validate unconsumed engine components proportionately before claiming their compatibility.

The merge ancestry is a practical foundation for regular upstream integration. The remaining divergence is explicit application source/identity/evidence stewardship plus the JDK/ClassGraph compatibility choice, not missing upstream history. Upstream does not automatically qualify inherited translations. Passing these tests cannot establish complete MTG rules fidelity or whole-card correctness.

## Reproduction and retained artifacts

Use JDK `21.0.12.1+1`, candidate environment `ARGENTUM_ENGINE_DIR=<absolute isolated engine>` and `ARGENTUM_CANDIDATE_REVISION=56eeb394ef4fd2b9b7467b2ae95b3ccf04ac0906`. The normal origin/clean-source safeguards stay enabled.

```powershell
.\scripts\bootstrap.ps1
# just invokes the recorded delivery recipe through the guarded application wrapper.
# Original workspace layout: .integration-tools/verify.just, mtg-full-upstream/, argentum-full-upstream/.
just --justfile ../.integration-tools/verify.just --working-directory . delivery
# Separate diagnostic copied-resource replay, AFTER the delivery graph finishes:
just --justfile ../.integration-tools/verify.just --working-directory . catalog-canonical
python scripts/test-bootstrap.py       # MTG_TEST_SHELL=powershell
python scripts/test-oracle-snapshot.py
python scripts/test-catalog-comparison.py
# From web; EXPLORER_BROWSER_CHANNEL=msedge
pnpm install --frozen-lockfile
pnpm build
pnpm test:ui
# Stable baseline only; EXPLORER_URL=http://127.0.0.1:8873
pnpm test:live
```

The combined build uses the application's Gradle 9.6.1 wrapper with the integrated multi-project build, two workers and one concurrent Kotlin compilation. The original upstream wrapper is Gradle 9.8.2; the repaired native test-class launcher resolved its task graph successfully in a dry run with that wrapper. Native verification runs in one graph through just to protect shared outputs; automatic daemon reaping is disabled. Source-specific compilation warnings are recorded without claiming new unrelated repairs.

Original logs are `upstream-*.log` at the workspace root. Original reports, overlapping definition exports, full common-name lists and raw comparisons are under the isolated application/baseline `generated/` directories. Compact receipts accompany this report; their hashes identify immutable observations. No frozen application resource changed relative to main. Earlier command failures and cancellations remain in the logs, not relabeled as passes. The final delivery replay explicitly reruns SDK, core and each era test task on the final engine head. The earlier broad receipt at `3de9082` is archived separately; no execution/qualification record was transplanted.

Review receipts: [revisions and archived JUnit](evidence/upstream-integration-2026-11-17/revision-receipt.json), [native suites](evidence/upstream-integration-2026-11-17/native-test-summary.json), [application failures](evidence/upstream-integration-2026-11-17/application-failure-details.json), [catalog replay provenance](evidence/upstream-integration-2026-11-17/catalog-verification.json), [controlled population](evidence/upstream-integration-2026-11-17/native-control-summary.json), [mechanical section differences](evidence/upstream-integration-2026-11-17/mechanical-comparison-summary.json), and [overlapping definitions](evidence/upstream-integration-2026-11-17/overlap-comparison.json). These are engineering observations, separate from production translation assurance and historical qualification.

Draft PRs: [engine #8](https://github.com/jasullivan5/argentum-engine/pull/8), targeting `app-stable`; [application #60](https://github.com/jasullivan5/mtg-deckbuilding-assistant/pull/60), targeting `main`. Both remain drafts and neither was merged. The application report/evidence commit follows the tested executable `ccaf1926`; it changes documentation only, with no executable-source, test, or frozen-resource changes. Final documentation head is recorded in the PR description.
