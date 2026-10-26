#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
sha=$(tr -d '\r\n' < "$root/ARGENTUM_COMMIT")
source_dir="$root/argentum"
command -v git >/dev/null || { echo 'Install Git first.' >&2; exit 1; }
command -v java >/dev/null || { echo 'Install JDK 21 and set JAVA_HOME first.' >&2; exit 1; }
if ! java -XshowSettings:properties -version 2>&1 | grep -Eq 'java.specification.version = 21$'; then
  echo 'JDK 21 is required. Point JAVA_HOME and PATH to JDK 21.' >&2; exit 1
fi
if [ ! -d "$source_dir" ]; then
  mkdir -p "$source_dir"
  git -C "$source_dir" init
  git -C "$source_dir" remote add origin https://github.com/wingedsheep/argentum-engine.git
fi
if [ "$(git -C "$source_dir" rev-parse --show-toplevel 2>/dev/null || true)" != "$(cd "$source_dir" && pwd -P)" ]; then
  echo 'argentum must be its own Git checkout.' >&2; exit 1
fi
current=$(git -C "$source_dir" rev-parse --verify HEAD 2>/dev/null || true)
if [ -n "$current" ] && [ "$current" != "$sha" ]; then
  echo "Wrong Argentum revision: $current. Use a separate pinned checkout; no changes were made." >&2; exit 1
fi
if [ "$current" != "$sha" ]; then
  if [ -n "$(git -C "$source_dir" status --porcelain)" ]; then
    echo 'Refusing to switch a modified Argentum checkout.' >&2; exit 1
  fi
  git -C "$source_dir" fetch --depth 1 origin "$sha"
  git -C "$source_dir" checkout --detach "$sha"
fi
[ -z "$(git -C "$source_dir" status --porcelain)" ] || {
  echo 'Argentum has source modifications. Use a clean pinned checkout.' >&2; exit 1;
}
echo "Argentum ready: $sha"
