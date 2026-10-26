param(
    [ValidateSet('build','test','run','installDist','clean','help')]
    [string]$Task = 'help',
    [Parameter(ValueFromRemainingArguments=$true)] [string[]]$GradleArgs
)
$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot
$SourceDir = Join-Path $Root 'argentum'
$Sha = (Get-Content (Join-Path $Root 'ARGENTUM_COMMIT') -Raw).Trim()
if (-not (Test-Path (Join-Path $SourceDir '.git'))) { throw 'Run scripts/setup.ps1 first.' }
$Current = & git -C $SourceDir rev-parse HEAD
if ($LASTEXITCODE -ne 0 -or $Current -ne $Sha) { throw 'Argentum revision mismatch. Run setup.' }
if (& git -C $SourceDir status --porcelain) { throw 'Argentum source files are modified.' }
& (Join-Path $Root 'gradlew.bat') -p $SourceDir -I (Join-Path $Root 'argentum.init.gradle') `
    --no-daemon --no-configuration-cache --max-workers=2 `
    '-PkotlinCompileParallelism=1' '-Pkotlin.daemon.jvmargs=-Xmx4g' ":assistant-cli:$Task" @GradleArgs
exit $LASTEXITCODE
