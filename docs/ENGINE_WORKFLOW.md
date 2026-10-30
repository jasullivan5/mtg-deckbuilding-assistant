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

Develop engine corrections on feature branches, using a temporary worktree when the application's normal stable checkout must remain available. Before merging into app-stable, check appropriate native regressions and application compatibility against the exact candidate. Engine workflow CI tests the PR merge revision and runs again after stable pushes. Application CI independently resolves app-stable once, records its SHA and runs its checks. Failed checks require investigation; there is no automatic rollback or qualification promotion.

For explicit candidate validation only, set ARGENTUM_ENGINE_DIR to an absolute clean isolated engine checkout and ARGENTUM_CANDIDATE_REVISION to its exact 40-character HEAD. Use the usual wrappers. Unset both afterward. Setup refuses branch/origin/dirty mismatches and never advances a candidate. Engine main tracks upstream; upstream merging is not ordinary application work.

## Evidence and history

Generated engine-build.properties is packaged in the application. Reports and native source URLs use that actual SHA, including when a source checkout later moves. `mtg-assistant stats` reports the mode and implementation fingerprint; runtime does not require Git.

Historical source, benchmark, eligibility and assurance records retain their original revisions. engine-evidence.properties permits documented equivalence only when all tracked engine content except root guidance and GitHub workflow configuration is identical. Mechanical changes cannot inherit old execution claims. Definition/source hashes, effective eligibility bindings, provisional qualification and open concerns remain guarded. New engine compatibility does not independently qualify a card.

The cleanup export holds obsolete transition instructions and receipts; they are not active dependency selectors. See [repository layout](REPOSITORY_LAYOUT.md).
