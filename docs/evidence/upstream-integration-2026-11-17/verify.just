# Uses the application wrapper's exact clean candidate/origin and module guards.
verify:
    bash ./dev test :mtg-sdk:test :rules-engine:test :mtg-sets:scenarioTest --continue -Pkotlin.daemon.jvmargs=-Xmx6g

catalog:
    bash ./dev help :mtg-sets:test --continue -Pkotlin.daemon.jvmargs=-Xmx6g

finalize:
    bash ./dev test :assistant:installDist :mtg-sets:test --continue -Pkotlin.daemon.jvmargs=-Xmx6g

application:
    bash ./dev test :assistant:installDist --continue -Pkotlin.daemon.jvmargs=-Xmx6g

catalog-canonical:
    bash ./dev help :mtg-sets:processTestResources --rerun :mtg-sets:test --rerun --continue --no-build-cache -I ../.integration-tools/canonical-catalog-resources.init.gradle -Pkotlin.daemon.jvmargs=-Xmx6g

delivery:
    bash ./dev test --rerun :assistant:installDist :mtg-sdk:test --rerun :rules-engine:test --rerun :mtg-sets:1993-1999:tests:test --rerun :mtg-sets:2000-2002:tests:test --rerun :mtg-sets:2003-2007:tests:test --rerun :mtg-sets:2008-2016:tests:test --rerun :mtg-sets:2017-2022:tests:test --rerun :mtg-sets:2023:tests:test --rerun :mtg-sets:2024:tests:test --rerun :mtg-sets:2025:tests:test --rerun :mtg-sets:2026:tests:test --rerun --continue -Pkotlin.daemon.jvmargs=-Xmx6g
