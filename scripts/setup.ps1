$ErrorActionPreference = 'Stop'
$Root = Split-Path $PSScriptRoot -Parent
$Sha = (Get-Content (Join-Path $Root 'ARGENTUM_COMMIT') -Raw).Trim()
$SourceDir = Join-Path $Root 'argentum'
function Git-Checked { & git @args; if ($LASTEXITCODE -ne 0) { throw "Git failed: $args" } }
Get-Command git -ErrorAction Stop | Out-Null
Get-Command java -ErrorAction Stop | Out-Null
$JavaVersion = (& java --version | Out-String)
if ($JavaVersion -notmatch '(?m)^(openjdk|java) 21[. ]') { throw 'JDK 21 is required. Set JAVA_HOME and PATH.' }
if (-not (Test-Path $SourceDir)) {
    New-Item -ItemType Directory $SourceDir | Out-Null
    Git-Checked -C $SourceDir init
    Git-Checked -C $SourceDir remote add origin https://github.com/wingedsheep/argentum-engine.git
}
$Top = (& git -C $SourceDir rev-parse --show-toplevel 2>$null)
if ($LASTEXITCODE -ne 0 -or [IO.Path]::GetFullPath($Top) -ne [IO.Path]::GetFullPath($SourceDir)) {
    throw 'argentum must be its own Git checkout.'
}
Git-Checked -C $SourceDir config core.longpaths true
$Current = (& git -C $SourceDir rev-parse --verify HEAD 2>$null)
if ($LASTEXITCODE -ne 0) { $Current = $null }
if ($Current -and $Current -ne $Sha) { throw "Wrong Argentum revision. Use a separate pinned checkout; no changes were made." }
if ($Current -ne $Sha) {
    if (& git -C $SourceDir status --porcelain) { throw 'Refusing to switch a modified checkout.' }
    Git-Checked -C $SourceDir fetch --depth 1 origin $Sha
    Git-Checked -C $SourceDir checkout --detach $Sha
}
if (& git -C $SourceDir status --porcelain) { throw 'Argentum source files are modified.' }
Write-Host "Argentum ready: $Sha"
