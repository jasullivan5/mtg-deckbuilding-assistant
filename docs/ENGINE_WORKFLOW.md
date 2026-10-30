# Sibling app-stable integration

The application normally builds against `../argentum-engine` on the maintained fork's `app-stable` branch. The branch selects the dependency. The exact engine commit recorded inside each application build and report identifies the code that produced it. There is no separate application dependency-pin approval.

## Local use

Keep these sibling checkouts:

```text
MTG Deckbuilding R&D/
  mtg-deckbuilding-assistant/
  argentum-engine/                 # clean app-stable
```

From the application, run `.\scripts\bootstrap.ps1`, then `.\dev.ps1 test` or `.\dev.ps1 installDist`. Bash equivalents remain `bash scripts/bootstrap.sh` and `./dev test`. Git, JDK 21, Bash and Python 3 are prerequisites. Set JAVA_HOME and put JDK 21's bin on PATH.

Setup clones the sibling fork on app-stable if absent. Existing checkouts are validated and reused without fetching, resetting, changing branch, stashing, or deleting work. To deliberately obtain newly merged stable changes, run `.\scripts\setup.ps1 -Update` (Bash: `bash scripts/setup.sh --update`); it fetches and fast-forwards only. Inspect/preserve local commits before updating. A clean local app-stable commit is usable; the branch name alone does not certify compatibility.

Wrong branch, dirty/untracked source, an unrelated origin, and the retired nested dependency are refused. Paths with spaces and ampersands are supported. The ignored old `argentum/` checkout is retained for historical evidence, but no normal build reads it. Do not delete it until its history, outputs and consumers have been reviewed.

## Engine corrections and acceptance

Develop engine corrections on feature branches isolated from the normal app-stable checkout, ordinarily in a temporary Git worktree when the application is in use. Run the appropriate native regressions and the complete application suite against the exact clean candidate before accepting its PR into app-stable. No automatic upstream integration or merge is authorized.

For explicit candidate validation, set `ARGENTUM_ENGINE_DIR` to an absolute clean engine worktree and `ARGENTUM_CANDIDATE_REVISION` to its exact 40-character HEAD, then use the normal application dev wrapper. Both must agree; setup never advances a candidate. Unset both variables to return to the normal sibling. This exception supports pre-merge CI and historical reproduction, not silent substitution during ordinary use.

The engine's Application compatibility workflow checks native tests and application main against the exact PR merge revision, and also runs after app-stable pushes. The application CI independently resolves app-stable once, records its SHA and runs the application checks. Inspect the named checks before merging; repository branch-protection enforcement is a separate server setting. A post-merge failure is a failed check, not automatic rollback or certification.

Rollout order: merge the application migration PR first, then the engine compatibility-workflow PR. Until the application migration is on main, the new engine workflow cannot validate the old application's nested-pin setup. Once available, workflow dispatch supports selecting an explicit application ref for controlled validation.

Both repositories are public. The engine workflow reads the application using normal checkout; no cross-repository credential or token secret is needed. Its application build must report the exact engine candidate SHA and candidate mode, preventing accidental validation through the retired nested dependency.

## Reproducible evidence

`engine-build.properties` is generated from the actual engine checkout and packaged in the CLI. Reports and native source URLs use that SHA even if the source checkout later moves. `mtg-assistant stats` prints the SHA, mode and implementation fingerprint. CLI research needs no Git checkout at runtime.

Historical source, eligibility, translation and benchmark records retain their original revisions. The historical `ARGENTUM_COMMIT` file now lives under `reference/fork-foundation/`; it is not read by setup or builds. It exists only for historical receipt reproduction.

The evidence compatibility manifest compares the entire tracked engine tree (mode, blob, path), excluding root AGENTS.md/README.md and GitHub workflow configuration (.github/). At this transition, b58ff301 differs from the historical evidence revision only in those two guidance files. This permits existing evidence to remain applicable without relabeling it as fresh execution. Any other tracked change breaks that equivalence and cannot inherit old execution claims. Definition hashes, source snapshots, identity bindings, provisional qualification and open concerns remain guarded. Reviewed legality corrections remain applicable across engine revisions only while all existing physical identity, native definition, approved field and source-snapshot checks pass. Benchmarks can be re-evaluated against another engine while recording both review and discovery revisions. Engine-execution assurance still becomes stale on an implementation change; that does not block structural discovery or confer new qualification. Engine improvements need appropriate native/application revalidation before acceptance; no dependency-pin PR is required.

Catalog regression checks compare against the unchanged historical baseline after narrowly normalizing runtime revision and native source URLs. Historical evidence revisions are never normalized. Every other report field, pathway, decision and prerequisite remains part of the comparison.

## Scope and review

Explorer M1 resumed on `feature/explorer-m1`, preserving a35b9b5 and merging the completed migration before further implementation. See [M1 delivery](EXPLORER_M1.md); integration migration does not establish Explorer acceptance. Permanent ChatGPT Project instructions are outside these repositories; replace any remaining nested-pin guidance there with this workflow.
