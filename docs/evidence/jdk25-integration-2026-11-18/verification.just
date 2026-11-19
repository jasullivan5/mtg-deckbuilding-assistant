versions:
    java --version
    javac --version
    bash ./gradlew --version
    bash ../argentum-native-consolidation/gradlew --version

bootstrap:
    bash scripts/bootstrap.sh -Pkotlin.daemon.jvmargs=-Xmx6g

full:
    bash -c 'cd ../argentum-native-consolidation && bash scripts/gradle-locked --no-daemon --no-configuration-cache --max-workers=2 -PkotlinCompileParallelism=1 -Pkotlin.daemon.jvmargs=-Xmx6g :mtg-sdk:test --rerun :rules-engine:test --rerun :mtg-sets:1993-1999:tests:test --rerun :mtg-sets:2000-2002:tests:test --rerun :mtg-sets:2003-2007:tests:test --rerun :mtg-sets:2008-2016:tests:test --rerun :mtg-sets:2017-2022:tests:test --rerun :mtg-sets:2023:tests:test --rerun :mtg-sets:2024:tests:test --rerun :mtg-sets:2025:tests:test --rerun :mtg-sets:2026:tests:test --rerun :mtg-sets:test --rerun --continue' > ../jdk25-native-full.log 2>&1
    bash ./dev test --rerun :assistant:installDist --continue -Pkotlin.daemon.jvmargs=-Xmx6g

native-launcher:
    python ../argentum-native-consolidation/scripts/test-class MandatoryTapPaymentTest --rerun --no-daemon --no-configuration-cache --max-workers=2 -PkotlinCompileParallelism=1 -Pkotlin.daemon.jvmargs=-Xmx6g

catalog:
    bash scripts/check-catalog.sh
application:
    bash ./dev test --rerun :assistant:installDist --continue -Pkotlin.daemon.jvmargs=-Xmx6g
focused-memory:
    bash ./dev test --tests '*ReportDiagnosticsTest' --rerun :assistant:installDist -Pkotlin.daemon.jvmargs=-Xmx6g
focused-progress:
    bash ./dev test --tests '*ReportDiagnosticsTest' --tests '*ResearchProgressTest' --rerun :assistant:installDist -Pkotlin.daemon.jvmargs=-Xmx6g
