# Actual execution ledger

Platform and versions: environment-and-tests.json. Pin unchanged; upstream status empty. Restored original Phase2.4 ZIP SHA256 80ec101e5aa22877250569b991edc83f6acbdcdaa32255257aa67c71e6b9adf9, preserving application sources and all 170 existing tests.

The temporary JDK21 was downloaded through the standard Adoptium API and extracted to /tmp/mtg-jdk21. This environment's public proxy CA was imported into that temporary JDK truststore; no TLS bypass. The existing clean upstream checkout and compiled outputs were reused; missing Gradle/dependency caches were downloaded again. The restored tracked .claude/skills symlink was repaired using git restore; no engine source change. None of that environment is packaged.

Executed build commands from project root:

```
chmod +x gradlew dev
python3 verification/hosted-proxy-runner.py bash ./dev build :assistant-cli:installDist -Pkotlin.daemon.jvmargs='-Xmx2g -XX:ActiveProcessorCount=4'
```

- baseline-build.log: SUCCESS 7m15s, 170 tests, zero failures/errors/skips.
- implementation-build-1.log: wrapper executable permission denied after extraction; corrected executable permissions.
- implementation-build-2.log: test/installDist run; compilation succeeded, one new test expected ADDITIONAL_CASTING_COST but original evidence contract is CASTING_COST. Assertion corrected, no card-specific production rule.
- implementation-build-3.log: build/installDist; new reporting test comparison lacked parentheses; Kotlin compilation failed and was corrected.
- implementation-build-4.log: test run; two new reporting tests used root-relative benchmark paths from cli test working directory; corrected using existing root/parent convention.
- implementation-build-5.log: SUCCESS 2m57s; 210 tests, zero failures/errors/skips; build and installed CLI.
- lean-check-build.log: execution against freshly extracted lean ZIP using the same pinned checkout/cache, without historical samples/generated inputs. SUCCESS 46s; compilation and tests restored from the matching Gradle build cache. This checks lean dependency completeness, not an additional independent test execution.

Initial report execution overlapped replacement of an installed application JAR during a later build; a running CLI failed with ClassNotFoundException. Those mixed outputs are discarded. Verified baseline/final workflows use separate immutable installed distributions after their builds, as below. This was an execution orchestration problem, not an upstream/classpath approximation.

```
JAVA_HOME=/tmp/mtg-jdk21 PATH=/tmp/mtg-jdk21/bin:$PATH MTG_REPORT_CLI=/tmp/mtg-phase24-baseline-dist/bin/mtg-assistant MTG_REPORT_DIR=generated/phase25/baseline-pinned bash verification/phase2_5/run-reports.sh baseline
JAVA_HOME=/tmp/mtg-jdk21 PATH=/tmp/mtg-jdk21/bin:$PATH MTG_REPORT_CLI=/tmp/mtg-phase25-final-dist/bin/mtg-assistant MTG_REPORT_DIR=generated/phase25/final-verified bash verification/phase2_5/run-reports.sh final
```

Each command's actual subcommands and output paths are in run-reports.sh and *-workflow.log. These execute native discovery, structural coverage, cross-examination, frozen A/B/C benchmarks, controlled A/B diffs, and pairwise A-C/B-C fact/action reuse. No mock catalog is used. Ineligible benchmark cases are mapping limitations, not extraction misses.

The frozen full-color C benchmark is executed by Phase25ReportingTest against the real reviewed-card subset after actual Commander filters using Kenrith. Output generated/phase25/benchmark-all-colors.json is copied into compact samples. Expected JSON SHA remains b16589779c7bd63c8042598d601921c761ef157ffa445534bc51c03e0ee501e3. 11/11 expected accepted, 4/4 excluded (including two UNKNOWN boundaries), no confirmed mismatches; one separately unresolved Amass case.

```
# For a fresh all-colors benchmark side output if tests were restored FROM-CACHE:
./dev clean
./dev test --no-build-cache
python3 verification/phase2_5/compact-reports.py
python3 verification/phase2_5/summarize-outcomes.py
python3 scripts/package-phase25.py <absolute-delivery-zip-path>
```

Packaging validates the ZIP CRC and asserts <=20,971,520 bytes. Full generated reports are hashed before omission. The reverse source patch was applied into a separate directory and every restored main/test file plus cli/build.gradle.kts SHA256 compared equal to original Phase2.4 bytes.

Final follow-up verification:
- final-build.log: SUCCESS 1m41s after declaring frozen benchmark files as Gradle test inputs.
- delivery-build.log: SUCCESS 2m24s, final source including objective-aware uncertainty classification assertion, all 210 tests passed and installed CLI.
- objective-aware-diagnostics.log: reruns C cross-examination with the corrected generic root classifier and frozen C benchmark. Discovery snapshots did not need reinterpretation: only diagnostic attribution changed.
- legacy-benchmarks.log: additionally evaluates the retained Phase2.2 frozen review on original and final Judith/Krenko A snapshots.
