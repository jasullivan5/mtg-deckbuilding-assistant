#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
bash "$root/scripts/setup.sh"
[ "${1:-}" != --no-build ] || exit 0
exec bash "$root/dev" installDist "$@"
