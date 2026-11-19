# From a clean application checkout with exact ARGENTUM_ENGINE_DIR and
# ARGENTUM_CANDIDATE_REVISION selected, JDK 21, Git Bash, Python, Node and pnpm.
# Serialize these Gradle recipes; no resource normalization or snapshot updates.
bootstrap:
    bash scripts/bootstrap.sh -Pkotlin.daemon.jvmargs=-Xmx6g

application:
    bash ./dev test :assistant:installDist --continue -Pkotlin.daemon.jvmargs=-Xmx6g

native:
    bash ./dev help :mtg-sdk:test --rerun :rules-engine:test --rerun :mtg-sets:1993-1999:tests:test --rerun :mtg-sets:2000-2002:tests:test --rerun :mtg-sets:2003-2007:tests:test --rerun :mtg-sets:2008-2016:tests:test --rerun :mtg-sets:2017-2022:tests:test --rerun :mtg-sets:2023:tests:test --rerun :mtg-sets:2024:tests:test --rerun :mtg-sets:2025:tests:test --rerun :mtg-sets:2026:tests:test --rerun :mtg-sets:test --rerun --continue -Pkotlin.daemon.jvmargs=-Xmx6g

catalog:
    bash scripts/check-catalog.sh
