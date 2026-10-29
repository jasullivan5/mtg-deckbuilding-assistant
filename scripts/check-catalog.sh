#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out=generated/handoff/catalog
mkdir -p "$out"
cli=cli/build/install/mtg-assistant/bin/mtg-assistant
for who in judith krenko; do
 commander='Judith, the Scourge Diva'; [ "$who" != krenko ] || commander='Krenko, Mob Boss'
 for letter in a b c; do
  case "$letter" in
   a) objective=own-nontoken-creature-deaths; review=benchmark/phase23/review-fork-v1.json ;;
   b) objective=reanimate-own-creatures; review=benchmark/phase24/review-fork-v1.json ;;
   c) objective=create-creature-tokens; review=benchmark/phase25/review-fork-v1.json ;;
  esac
  "$cli" discover --commander "$commander" --objective "$objective" --json --output "$out/$who-$letter.json"
  "$cli" audit benchmark --snapshot "$out/$who-$letter.json" --benchmark "$review" --json --output "$out/$who-$letter-benchmark.json"
 done
done
"$cli" audit coverage --snapshot "$out/judith-c.json" --json --output "$out/coverage.json"
"$cli" audit inconsistencies --snapshot "$out/judith-c.json" --benchmark benchmark/phase25/review-fork-v1.json --json --output "$out/examination.json"
"$cli" audit diff --before "$out/judith-a.json" --after "$out/judith-a.json" --json --output "$out/diff-identical.json"
"${PYTHON:-python3}" scripts/compare-baseline.py "$out" reference/fork-foundation/catalog-baseline.json
