#!/usr/bin/env bash
set -euo pipefail
stage=${1:?baseline or final}
default_stage=final-verified
[ "$stage" != baseline ] || default_stage=baseline-pinned
out=${MTG_REPORT_DIR:-generated/phase25/$default_stage}
mkdir -p "$out"
cli=${MTG_REPORT_CLI:-cli/build/install/mtg-assistant/bin/mtg-assistant}
for who in judith krenko; do
  commander='Judith, the Scourge Diva'
  [ "$who" != krenko ] || commander='Krenko, Mob Boss'
  objectives='a b'
  [ "$stage" = baseline ] || objectives='a b c'
  for letter in $objectives; do
    case "$letter" in
      a) objective=own-nontoken-creature-deaths; review=benchmark/phase23/review-v1.json ;;
      b) objective=reanimate-own-creatures; review=benchmark/phase24/review-v1.json ;;
      c) objective=create-creature-tokens; review=benchmark/phase25/review-v1.json ;;
    esac
    prefix="$out/$who-$letter"
    "$cli" discover --commander "$commander" --objective "$objective" --json --output "$prefix.json"
    for audit in coverage inconsistencies; do
      extra=()
      if [ "$letter" = c ] && [ "$audit" = inconsistencies ]; then extra=(--benchmark "$review"); fi
      "$cli" audit "$audit" --snapshot "$prefix.json" "${extra[@]}" --json --output "$prefix-$audit.json"
    done
    "$cli" audit benchmark --snapshot "$prefix.json" --benchmark "$review" --json --output "$prefix-benchmark.json"
    if [ "$letter" = a ]; then
      "$cli" audit benchmark --snapshot "$prefix.json" --benchmark benchmark/review-v1.json --json --output "$prefix-phase22-benchmark.json"
    fi
    if [ "$stage" != baseline ] && [ "$letter" != c ]; then
      "$cli" audit diff --before "generated/phase25/baseline-pinned/$who-$letter.json" --after "$prefix.json" --json --output "$prefix-diff.json"
    fi
  done
  if [ "$stage" != baseline ]; then
    for letter in a b; do "$cli" audit cross-objective --snapshot "$out/$who-$letter.json" --other-snapshot "$out/$who-c.json" --json --output "$out/$who-$letter-c-reuse.json"; done
  fi
done
