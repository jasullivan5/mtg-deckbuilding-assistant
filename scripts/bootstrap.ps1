param([switch]$NoBuild)
$ErrorActionPreference = 'Stop'
& "$PSScriptRoot/setup.ps1"
if (-not $NoBuild) { & "$PSScriptRoot/../dev.ps1" installDist; exit $LASTEXITCODE }
