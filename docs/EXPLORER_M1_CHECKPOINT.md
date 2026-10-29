# Explorer M1 checkpoint — 2026-10-29

Continue on `feature/explorer-m1`. Original foundation **a35b9b5** is preserved; **aa0f497** reconciles application main b66dd0d and its sibling app-stable integration. Checkpoints def3615 and 7d680f7 are pushed. Never restart or restore the retired nested/pinned dependency arrangement.

Normal engine: clean sibling `../argentum-engine`, branch `app-stable`, revision **e46fab1475ddc6f355e1c8fdc770b12bdc697b0f**. No engine changes are part of M1.

Implemented: bounded loopback JDK HTTP API, compact summaries and selected-card evidence, complete streamed exports, React controls/findings/evidence with readable verdict restatements, three existing objectives, actual build provenance, Windows one-command foreground launch, focused boundary and browser tests, frontend CI and delivery documentation. Read [M1 launch, contract, exercises and limits](EXPLORER_M1.md).

Fresh verification completed: 465 application tests / 59 suites passed; five focused HTTP tests rerun after startup diagnostics passed; four fixture browser tests passed. HTTP tests compare every complete report field with fresh CLI output for all three objectives. An earlier genuine browser run passed Judith tokens → Bitterblossom → reanimation → Oviya tokens → rejected Murder. Final live rerun including mobile keyboard/layout remains next. Earlier Windows launch and Ctrl+C shutdown succeeded; occupied-port failure was checked.

Next: commit final source/docs, launch `scripts/explorer.ps1` from a clean source revision, run `pnpm test:live` with EXPLORER_URL and installed Chrome channel, inspect screenshots, measure final response sizes/query timing, stop server and verify port closure. Write compact final receipt, update this checkpoint, push and open/attach a focused PR against main. No M1 PR exists at this checkpoint. Do not merge it; readiness for Prototype Lab is not user acceptance. No M2+ work is authorized.

Avoid installDist while a live server/CLI reads packaged jars. Generated reports/screenshots and node_modules stay ignored. Final evidence should identify the tested source commit separately from any subsequent documentation-only receipt commit.
