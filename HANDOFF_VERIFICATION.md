# Current fork-foundation verification

The independent-fork transition is documented in docs/ENGINE_COUNTER_CORRECTION.md and reference/fork-foundation/. Fresh execution passed 7,507 native tests (including 17 focused fixtures), 13 representative native scenarios, and 457 application tests. A separate normal build fetched the published stable fork commit and passed all 457 application tests and nine mutation controls/detections. All 16 Windows PowerShell/Git Bash setup and wrapper safeguards passed. Nine full discovery, three coverage and three missing-card report comparisons matched after declared revision/provenance/assurance bindings and coverage timings; no mechanical or eligibility changes. The separate current catalog gate passed six complete-report comparisons and six benchmarks with zero confirmed mismatches, coverage/cross-examination and empty self-diff; four local-Git setup fixtures also pass. Initial benchmark-pin refusals and a CRLF-only golden failure are preserved with their corrections.

This branch proposes engine 4133241856227d1d36269e92aaba633bbaf93f5c; application main remains on approved ebf5bf2e9fbb80d13a556d75491dd462e10c206f until an explicit application PR merge. Historical recovery counts and platform limitations below are not current transition results. No hosted CI or independent qualification is claimed.

## Historical Phase 2.5 recovery record

# Handoff verification — verified local delivery

Status: recovery cold build and all 210 Kotlin tests PASSED. All six catalog comparisons and routine verification also passed; remote CI remains unexecuted.

- Source artifact: MTG_Deckbuilding_Assistant_Phase2_5(1).zip; SHA256 f5d7732d7cafbd7a183bd11068ddad0333183cebf605c7f9f98cb0cedc5e12da, verified on import.
- Pin unchanged: ebf5bf2e9fbb80d13a556d75491dd462e10c206f.
- Required toolchain: JDK21 and Gradle9.6.1 wrapper (checksum retained); Git/Bash/Python3.
- Earlier attempt: baseline 210/0 and routine 48/0 plus 10 workflow/comparison tests passed, as recorded in the conversation. Logs/Git objects disappeared with the workspace reset; they are not shipped or claimed as fresh recovery execution evidence.
- Earlier cold build: first attempt hit Plugin Portal Foojay HTTP403; retry reached native compilation before the reset. Final result is unavailable. Full catalog result likewise unavailable.
- This recovery keeps Kotlin application/test and benchmark bytes unchanged. Setup changes are limited to refusing existing wrong-revision checkouts and correctly checking unborn HEAD with --verify. A PowerShell counterpart is provided by inspection.
- Current execution logs are added under verification/handoff. Historical Phase2.5 evidence is separate under docs/history and verification/phase2_5.
- GitHub publication and hosted CI: NOT PERFORMED; no suitable app destination/creation capability. Codex environment: NOT CREATED.
- Recovery cold build completed successfully in 10m21s, 53/53 actionable tasks executed with --no-build-cache. The checkout was freshly retrieved from public GitHub (no symlink/warm native outputs), and the user Gradle cache was initially absent. All 210 tests passed with zero failures/errors/skips. See recovery-results.json and recovery-cold-build.log.

Commands: ./scripts/bootstrap.sh; ./dev build; ./scripts/verify.sh fast; ./scripts/verify.sh full. Catalog gate: ./scripts/verify.sh catalog. It regenerates six queries/reviews, coverage, cross-examination and native identical-snapshot diff, then compares strict pathway/evidence multisets with the compact accepted baseline. Mismatches fail; no expectations are rewritten.

The final export receipt supplies exact Git commits and repository size. The package must remain <=20,971,520 bytes and excludes upstream/cache/builds/generated dumps. Platform and successful/unsuccessful commands will be recorded explicitly as execution completes.

Recovery checks: four actual local-Git setup guard tests passed. Fresh export Git fsck passed; wrapper normalization was corrected so export status is clean. All 38 application/test Kotlin files match the imported baseline bytes (source-preservation.json). Cold command: JAVA_HOME=<installed JDK21> PATH="$JAVA_HOME/bin:$PATH" ./dev build :assistant-cli:installDist --no-build-cache. This host also supplied JVM proxy/truststore configuration externally; no credentials or environment-specific helper is committed.

## Final recovery execution

- `./scripts/bootstrap.sh --no-build`: fetched a new shallow exact-pin upstream checkout from public GitHub; source-setup.log. No existing Work checkout/symlink or native build outputs were used.
- `./dev build :assistant-cli:installDist --no-build-cache`: SUCCESS 10m21s, 53/53 actionable tasks executed, 210 tests / zero failures/errors/skips. This was the cold application/native dependency build, with an initially absent Gradle user cache restored from public dependencies. No mock or unpublished local library was substituted.
- `./scripts/verify.sh fast`: SUCCESS, 48 Kotlin tests / zero failures/errors/skips plus actual Judith CLI lookup; fast-results.json and recovery-fast.log.
- `JAVA_HOME=<JDK21> PATH=... python3 scripts/test-bootstrap.py`: four local-Git guard tests passed. Tests preserve dirty/wrong-revision work and reject invalid directories; they are not rules-engine fixtures.
- `./scripts/check-catalog.sh`: SUCCESS; all A/B/C Judith/Krenko contexts regenerated, six matching frozen benchmarks reported zero confirmed mismatches, independent coverage and cross-examination executed, identical-snapshot native diff had zero changes. Strict complete-evidence and pathway/decision multiset comparisons all matched the accepted compact baseline. See catalog-summary.json/catalog.log. Full generated JSON is intentionally omitted from Git/ZIP.
- The complete verification components were executed as cold build/all-tests plus catalog script. The aggregate `verify.sh full` wrapper was syntax-checked; it was not rerun merely to duplicate the already successful components. Hosted Actions was not executed.

Platform: Linux x86_64, Temurin JDK21.0.13+9, Gradle9.6.1/Kotlin2.4.0 retained. Exact wrapper/runtime report: toolchain.log. Native upstream remains clean at the required full SHA. Windows/PowerShell/macOS are inspection-only. A trusted environment public CA/JVM proxy was configured externally for HTTPS; no TLS bypass or credentials are committed.

The initial pre-reset handoff draft/commits/logs were lost; the accepted Phase2.5 source and the completed recovery evidence are now preserved durably in this export. No previously unavailable cold/full result is claimed. There are no unresolved Linux build/test/catalog blockers in this recovered delivery. Unsupported mechanical behavior remains as documented in KNOWN_LIMITATIONS.md.

Git history is included in the fallback ZIP, beginning with the accepted source import tag. Final commit/repository byte counts are generated into REPOSITORY_STATE.json after commit (avoids a self-referential SHA). Source and complete reachable Git objects are exported by a fresh --no-local clone and validated by git fsck/clean status. ZIP CRC and <=20 MiB limits are asserted. Exact compressed size is reported with delivery. No remote repository, Codex environment or hosted CI was published.
