# Sibling app-stable workflow

Keep the application and maintained engine as separate sibling repositories. Normal builds use `../argentum-engine`, clean on the maintained fork's **app-stable** branch. The nested `argentum/` checkout is retired and has been removed. Setup explicitly rejects that nested path.

## Everyday use

Run the Explorer or dev/dev.ps1 from the application. Setup creates the sibling on app-stable if absent, otherwise validates and reuses it. It does not automatically fetch, change branches, reset, stash or delete work.

To deliberately obtain merged stable improvements:

```powershell
.\scripts\setup.ps1 -Update
```

This fetches and fast-forwards only. Rebuild/relaunch afterward. The branch chooses the normal engine code; the exact SHA in each build/report records what produced that result. There is no separate pin-adoption PR. The branch name alone is not a compatibility certificate.

## Engine corrections

Develop engine corrections on feature branches, using a temporary worktree when the application's normal stable checkout must remain available. Before merging into app-stable, check appropriate native regressions and application compatibility against the exact clean candidate.

The application's **Verify** workflow and the maintained engine branch's **CI** and **Application compatibility** workflows run on manual dispatch. Local verification is the developer's responsibility. GitHub does not automatically test PR updates or stable pushes with these workflows, or enforce the local results at merge time. Record the tested SHAs and outcomes in the PR so reviewers can confirm the evidence still applies after updates. Dispatch the retained workflows when hosted verification is useful.

For explicit candidate validation only, set ARGENTUM_ENGINE_DIR to an absolute clean isolated engine checkout and ARGENTUM_CANDIDATE_REVISION to its exact 40-character HEAD. Use the usual wrappers. Unset both afterward. Setup refuses branch/origin/dirty mismatches and never advances a candidate. Engine main tracks upstream; upstream merging is not ordinary application work.

## Complete local verification

1. From a clean application checkout, record `git rev-parse HEAD` and `git -C ../argentum-engine rev-parse HEAD`. For a candidate, record the isolated checkout's HEAD instead. Use JDK 21, Python 3, Node and pnpm as described in the [README](../README.md).
2. From the application root, run `bash scripts/bootstrap.sh`, then `python3 scripts/test-bootstrap.py`, `python3 scripts/test-oracle-snapshot.py` and `bash scripts/verify.sh full`. The full gate reruns all Kotlin tests, including the source-discard and source-sacrifice scenarios, installs the CLI and runs the catalog checks. On Windows, `.\scripts\bootstrap.ps1` and `.\dev.ps1 test --rerun` cover setup and Kotlin. Use Bash with the same toolchain for the full catalog gate.
3. For an engine correction, run the relevant native regressions in the exact engine checkout with `just test-class <ClassName> --rerun` or the broader `just test-rules`. Include `SparkElementalScenarioTest` and source-instance and source-zone regressions for source-identity changes.
4. Build the Explorer and run `pnpm test:ui` and `pnpm test:live` from `web` using the [browser verification procedure](EXPLORER.md). Live tests require the URL of the freshly built Kotlin service. Check `/api/v1/health` to confirm the application and engine SHAs before testing.
5. For changes that may affect discovery, run the CLI's JSON discovery for Judith with each objective on both revisions. Compare the exact `mechanicalInputCards` lists before interpreting changes. Compare results, actions, facts, eligibility and assurance, separating SHA-bound source references and historical evidence from mechanical differences. Use `audit diff --before <report> --after <report> --json` for the report comparison. Record pass, fail and unexecuted checks.

## Evidence and history

Generated engine-build.properties is packaged in the application. Reports and native source URLs use that actual SHA, including when a source checkout later moves. `mtg-assistant stats` reports the mode and implementation fingerprint; runtime does not require Git.

Historical source, benchmark, eligibility and assurance records retain their original revisions. engine-evidence.properties permits documented equivalence only when all tracked engine content except root guidance and GitHub workflow configuration is identical. Mechanical changes cannot inherit old execution claims. Definition/source hashes, effective eligibility bindings, provisional qualification and open concerns remain guarded. New engine compatibility does not independently qualify a card.

The cleanup export holds obsolete transition instructions and receipts; they are not active dependency selectors. See [repository layout](REPOSITORY_LAYOUT.md).
