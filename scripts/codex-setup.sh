#!/usr/bin/env bash
set -euo pipefail
# Configure JDK21/JAVA_HOME in the environment, then run from any working directory.
root=$(cd "$(dirname "$0")/.." && pwd)
exec bash "$root/scripts/bootstrap.sh" "$@"
