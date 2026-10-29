#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
source_dir=${ARGENTUM_ENGINE_DIR:-"$(dirname "$root")/argentum-engine"}
repository=https://github.com/jasullivan5/argentum-engine.git
update=${1:-}
[[ "$update" == '' || "$update" == '--update' ]] || { echo 'Usage: setup.sh [--update]' >&2; exit 2; }
command -v git >/dev/null || { echo 'Install Git first.' >&2; exit 1; }
command -v java >/dev/null || { echo 'Install JDK 21 first.' >&2; exit 1; }
java -XshowSettings:properties -version 2>&1 | tr -d '\r' | grep -Eq 'java.specification.version = 21$' || { echo 'JDK 21 is required. Set JAVA_HOME and PATH.' >&2; exit 1; }
if [ ! -e "$source_dir" ]; then
  [ -z "${ARGENTUM_CANDIDATE_REVISION:-}" ] || { echo 'Candidate checkout must already exist.' >&2; exit 1; }
  git clone --branch app-stable --single-branch "$repository" "$source_dir"
fi
source_dir=$(cd "$source_dir" && pwd -P)
top=$(git -C "$source_dir" rev-parse --show-toplevel)
[ "$(cd "$top" && pwd -P)" = "$source_dir" ] || { echo 'Engine path must be its own Git checkout.' >&2; exit 1; }
[ "$source_dir" != "$root/argentum" ] || { echo 'The retired nested argentum checkout is not the application dependency.' >&2; exit 1; }
remote=$(git -C "$source_dir" config --get remote.origin.url)
case "$remote" in
  https://github.com/jasullivan5/argentum-engine.git|https://github.com/jasullivan5/argentum-engine|git@github.com:jasullivan5/argentum-engine.git) ;;
  *) echo 'Engine origin must be the maintained fork; no origin was changed.' >&2; exit 1 ;;
esac
[ -z "$(git -C "$source_dir" status --porcelain)" ] || { echo 'Engine has local changes; preserve them and use a clean app-stable checkout.' >&2; exit 1; }
current=$(git -C "$source_dir" rev-parse HEAD)
if [ -n "${ARGENTUM_CANDIDATE_REVISION:-}" ]; then
  [[ "$ARGENTUM_CANDIDATE_REVISION" =~ ^[0-9a-f]{40}$ && "$current" = "$ARGENTUM_CANDIDATE_REVISION" && -z "$update" ]] || { echo 'Candidate must identify this exact clean checkout; candidate validation never updates it.' >&2; exit 1; }
  echo "Explicit candidate validation: $current"
else
  [ "$(git -C "$source_dir" branch --show-current)" = app-stable ] || { echo 'Engine must be on app-stable; setup never switches existing branches.' >&2; exit 1; }
  if [ "$update" = --update ]; then
    git -C "$source_dir" fetch origin app-stable
    git -C "$source_dir" merge --ff-only origin/app-stable
    current=$(git -C "$source_dir" rev-parse HEAD)
  fi
  echo "Argentum app-stable ready: $current ($source_dir)"
fi
