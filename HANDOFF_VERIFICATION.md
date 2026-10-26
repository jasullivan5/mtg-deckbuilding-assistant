# Handoff verification — recovery checkpoint

Status: source/Git/developer documentation recovered; execution continues. Do not treat this checkpoint as a completed clean build or hosted CI pass.

- Source artifact: MTG_Deckbuilding_Assistant_Phase2_5(1).zip; SHA256 f5d7732d7cafbd7a183bd11068ddad0333183cebf605c7f9f98cb0cedc5e12da, verified on import.
- Pin unchanged: ebf5bf2e9fbb80d13a556d75491dd462e10c206f.
- Required toolchain: JDK21 and Gradle9.6.1 wrapper (checksum retained); Git/Bash/Python3.
- Earlier attempt: baseline 210/0 and routine 48/0 plus 10 workflow/comparison tests passed, as recorded in the conversation. Logs/Git objects disappeared with the workspace reset; they are not shipped or claimed as fresh recovery execution evidence.
- Earlier cold build: first attempt hit Plugin Portal Foojay HTTP403; retry reached native compilation before the reset. Final result is unavailable. Full catalog result likewise unavailable.
- This recovery keeps Kotlin application/test and benchmark bytes unchanged. Setup changes are limited to refusing existing wrong-revision checkouts and correctly checking unborn HEAD with --verify. A PowerShell counterpart is provided by inspection.
- Current execution logs are added under verification/handoff. Historical Phase2.5 evidence is separate under docs/history and verification/phase2_5.
- GitHub publication and hosted CI: NOT PERFORMED; no suitable app destination/creation capability. Codex environment: NOT CREATED.
- Full cold verification is pending; do not claim it from a warm/symlink build. A fresh public pin retrieval is separately logged.

Commands: ./scripts/bootstrap.sh; ./dev build; ./scripts/verify.sh fast; ./scripts/verify.sh full. Catalog gate: ./scripts/verify.sh catalog. It regenerates six queries/reviews, coverage, cross-examination and native identical-snapshot diff, then compares strict pathway/evidence multisets with the compact accepted baseline. Mismatches fail; no expectations are rewritten.

The final export receipt supplies exact Git commits and repository size. The package must remain <=20,971,520 bytes and excludes upstream/cache/builds/generated dumps. Platform and successful/unsuccessful commands will be recorded explicitly as execution completes.
