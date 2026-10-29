param([ValidateRange(1024,65535)][int]$Port = 8787)
$ErrorActionPreference = 'Stop'
$Root = Split-Path $PSScriptRoot -Parent
Get-Command git,node,pnpm,java -ErrorAction Stop | Out-Null
if ((& java --version | Out-String) -notmatch '(?m)^(openjdk|java) 21[. ]') { throw 'JDK 21 required; set JAVA_HOME and PATH.' }
Push-Location $Root
try {
    & "$PSScriptRoot/setup.ps1"
    if ($LASTEXITCODE -ne 0) { throw 'Pinned dependency setup failed.' }
    Push-Location (Join-Path $Root 'explorer')
    try {
        & pnpm install --frozen-lockfile
        if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed. Check Node 22.12+ / pnpm 10 and network access.' }
        & pnpm build
        if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
    } finally { Pop-Location }
    # dev.ps1 exits its calling PowerShell; invoke it in a child process.
    & (Get-Process -Id $PID).Path -NoProfile -File (Join-Path $Root 'dev.ps1') installDist
    if ($LASTEXITCODE -ne 0) { throw 'Kotlin build failed.' }
    $env:MTG_APPLICATION_REVISION = (& git rev-parse HEAD)
    if (& git status --porcelain) { $env:MTG_APPLICATION_REVISION += '+dirty' }
    Write-Host "Starting Explorer at http://127.0.0.1:$Port (wait for ready message). Ctrl+C stops the service."
    & (Join-Path $Root 'cli/build/install/mtg-assistant/bin/mtg-assistant.bat') explorer (Join-Path $Root 'explorer/dist') $Port
    if ($LASTEXITCODE -ne 0) { throw 'Explorer stopped with an error; see output above (port may already be occupied).' }
} finally { Pop-Location }
