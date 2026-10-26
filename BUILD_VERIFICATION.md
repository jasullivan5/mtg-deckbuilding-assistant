# Phase 2.5 build verification

- Argentum: `ebf5bf2e9fbb80d13a556d75491dd462e10c206f`, unchanged, clean checkout.
- Platform: Linux 6.18.55 x86_64, glibc 2.39.
- Java: Eclipse Temurin OpenJDK 21.0.13+9 LTS.
- Gradle wrapper: 9.6.1, distribution checksum pinned in wrapper properties; retained Kotlin 2.4.0 integration.
- Baseline: exact Phase2.4 source restored and built, **170 tests passed**, zero failures/errors/skips, 7m15s.
- Expanded implementation: **210 tests passed**, zero failures/errors/skips. All previous test sources retained.
- Fresh lean archive extraction: build successful in 46s, compilation and matching test outputs FROM-CACHE; historical samples/generated data were absent. This was a packaging dependency check, not an independent new test execution. It preceded the final diagnostic attribution assertion; final source was then executed in delivery-build.log.
- Benchmark JSON is explicitly declared as a Gradle test input, so changing reviewed expectations invalidates cached test results.

Executed command, from project root:

```bash
chmod +x gradlew dev
python3 verification/hosted-proxy-runner.py bash ./dev build :assistant-cli:installDist -Pkotlin.daemon.jvmargs='-Xmx2g -XX:ActiveProcessorCount=4'
```

The hosted helper supplies this environment's JDK/proxy settings; on a normal JDK21 machine use `./dev build` and `./dev installDist` directly. No proxy/JDK/dependency cache is bundled. Environment restoration used a trusted public proxy CA without disabling TLS. Restored tracked symlink repair made upstream clean; there are no engine source modifications.

Actual logs, XML, versions and source/archive hashes are in `verification/phase2_5/`. `COMMANDS.md` distinguishes failed intermediate attempts from successful execution. The final report workflow uses immutable installed distributions; outputs from an earlier overlapping install/run were discarded.

Reproduce with Git/JDK21 and network access to source/dependencies:

```bash
bash scripts/setup.sh
chmod +x gradlew dev
./dev build
./dev installDist
cli/build/install/mtg-assistant/bin/mtg-assistant discover --commander "Judith, the Scourge Diva" --objective create-creature-tokens --json --output generated/tokens.json
```

README includes Windows/macOS guidance and baseline/final report regeneration. Windows/macOS were not executed. There are no unresolved build/test blockers on the recorded Linux environment. Mechanical support limitations remain explicit in KNOWN_LIMITATIONS.md; no exhaustive SDK/catalog/gameplay claim is made.

Final delivery command above completed **SUCCESS in 2m24s**, 53 tasks (9 executed, 44 up-to-date); all **210 tests passed** with zero failures/errors/skips. This includes the corrected diagnostic attribution assertion. Final XML was copied after this run.
