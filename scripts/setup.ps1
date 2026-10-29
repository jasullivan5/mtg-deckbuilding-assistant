param([switch]$Update)
$ErrorActionPreference = 'Stop'
$Root = Split-Path $PSScriptRoot -Parent
$SourceDir = if ($env:ARGENTUM_ENGINE_DIR) { [IO.Path]::GetFullPath($env:ARGENTUM_ENGINE_DIR) } else { Join-Path (Split-Path $Root -Parent) 'argentum-engine' }
$Repository = 'https://github.com/jasullivan5/argentum-engine.git'
function Git-Checked { & git @args; if ($LASTEXITCODE -ne 0) { throw "Git failed: $args" } }
Get-Command git,java -ErrorAction Stop | Out-Null
if ((& java --version | Out-String) -notmatch '(?m)^(openjdk|java) 21[. ]') { throw 'JDK 21 is required. Set JAVA_HOME and PATH.' }
if (-not (Test-Path -LiteralPath $SourceDir)) {
    if ($env:ARGENTUM_CANDIDATE_REVISION) { throw 'Candidate checkout must already exist.' }
    Git-Checked clone --branch app-stable --single-branch $Repository $SourceDir
}
$Top = Git-Checked -C $SourceDir rev-parse --show-toplevel
if ([IO.Path]::GetFullPath($Top) -ne [IO.Path]::GetFullPath($SourceDir)) { throw 'Engine path must be its own Git checkout.' }
if ([IO.Path]::GetFullPath($SourceDir) -eq [IO.Path]::GetFullPath((Join-Path $Root 'argentum'))) { throw 'The retired nested argentum checkout is not the application dependency.' }
$Remote = Git-Checked -C $SourceDir config --get remote.origin.url
if ($Remote -notin @($Repository,'https://github.com/jasullivan5/argentum-engine','git@github.com:jasullivan5/argentum-engine.git')) { throw 'Engine origin must be the maintained fork; no origin was changed.' }
if (Git-Checked -C $SourceDir status --porcelain) { throw 'Engine has local changes; preserve them and use a clean app-stable checkout.' }
$Current = Git-Checked -C $SourceDir rev-parse HEAD
if ($env:ARGENTUM_CANDIDATE_REVISION) {
    if ($env:ARGENTUM_CANDIDATE_REVISION -notmatch '^[0-9a-f]{40}$' -or $Current -ne $env:ARGENTUM_CANDIDATE_REVISION) { throw 'Candidate revision must identify this exact clean checkout.' }
    if ($Update) { throw 'Candidate validation never updates the checkout.' }
    Write-Host "Explicit candidate validation: $Current"
} else {
    $Branch = Git-Checked -C $SourceDir branch --show-current
    if ($Branch -ne 'app-stable') { throw 'Engine must be on app-stable. Preserve feature work in a worktree; setup never switches existing branches.' }
    if ($Update) {
        Git-Checked -C $SourceDir fetch origin app-stable
        Git-Checked -C $SourceDir merge --ff-only origin/app-stable
        $Current = Git-Checked -C $SourceDir rev-parse HEAD
    }
    Write-Host "Argentum app-stable ready: $Current ($SourceDir)"
}
