param(
    [ValidateSet('build','test','run','installDist','clean','help')]
    [string]$Task = 'help',
    [Parameter(ValueFromRemainingArguments=$true)] [string[]]$GradleArgs
)
$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot
$SourceDir = if ($env:ARGENTUM_ENGINE_DIR) { [IO.Path]::GetFullPath($env:ARGENTUM_ENGINE_DIR) } else { Join-Path (Split-Path $Root -Parent) 'argentum-engine' }
& (Join-Path $Root 'scripts/setup.ps1')
& (Join-Path $Root 'gradlew.bat') -p $SourceDir -I (Join-Path $Root 'gradle/argentum.init.gradle') `
    --no-daemon --no-configuration-cache --max-workers=2 `
    '-PkotlinCompileParallelism=1' '-Pkotlin.daemon.jvmargs=-Xmx4g' ":assistant:$Task" @GradleArgs
exit $LASTEXITCODE
