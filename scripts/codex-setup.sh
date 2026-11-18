#!/usr/bin/env bash
set -euo pipefail
# Compatibility entry point for externally configured setup commands.
# Keep setup logic in bootstrap.sh; configure JDK25/JAVA_HOME before invoking.
root=$(cd "$(dirname "$0")/.." && pwd)
exec bash "$root/scripts/bootstrap.sh" "$@"
