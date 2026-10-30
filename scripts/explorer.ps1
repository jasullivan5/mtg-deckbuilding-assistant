param([ValidateRange(1024,65535)][int]$Port = 8787)
$ErrorActionPreference = 'Stop'
$Root = Split-Path $PSScriptRoot -Parent
# VS Code can retain the environment from before tools were installed.
# Refresh saved paths without depending on its PowerShell profile loading.
if ([Environment]::OSVersion.Platform -eq [PlatformID]::Win32NT) {
    if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
        $env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
        if (-not $env:JAVA_HOME) {
            $env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine')
        }
    }
    $ToolPaths = @()
    if ($env:JAVA_HOME) { $ToolPaths += Join-Path $env:JAVA_HOME 'bin' }
    $ToolPaths += $env:Path -split ';'
    $ToolPaths += [Environment]::GetEnvironmentVariable('Path', 'User') -split ';'
    $ToolPaths += [Environment]::GetEnvironmentVariable('Path', 'Machine') -split ';'
    # Packaged Windows apps can see different AppData files. Prefer the shared
    # user Tools folder, with the older AppData installation as a fallback.
    $ToolRoots = @(
        (Join-Path $env:USERPROFILE 'Tools/mtg-dev-tools'),
        (Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'mtg-dev-tools')
    )
    foreach ($LocalTools in $ToolRoots) {
        if (-not (Test-Path -LiteralPath $LocalTools)) { continue }
        if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
            $NodeInstall = Get-ChildItem -LiteralPath $LocalTools -Directory -Filter 'node-*-win-*' |
                Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'node.exe') } |
                Sort-Object LastWriteTime -Descending | Select-Object -First 1
            if ($NodeInstall) { $ToolPaths = @($NodeInstall.FullName) + $ToolPaths }
        }
        if (-not $env:JAVA_HOME) {
            $JavaInstall = Get-ChildItem -LiteralPath $LocalTools -Directory -Filter 'jdk-21*' |
                Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin/java.exe') } |
                Sort-Object LastWriteTime -Descending | Select-Object -First 1
            if ($JavaInstall) {
                $env:JAVA_HOME = $JavaInstall.FullName
                $ToolPaths = @((Join-Path $env:JAVA_HOME 'bin')) + $ToolPaths
            }
        }
    }
    $env:Path = ($ToolPaths | Where-Object { $_ } | Select-Object -Unique) -join ';'
}
foreach ($Tool in @('git','node','pnpm','java')) {
    if (-not (Get-Command $Tool -ErrorAction SilentlyContinue)) {
        throw "Missing $Tool. Install Git, JDK 21, Node 22.12+ and pnpm 11 before launching."
    }
}
if ((& java --version | Out-String) -notmatch '(?m)^(openjdk|java) 21[. ]') {
    throw 'JDK 21 is required. Set JAVA_HOME and PATH as described in README.'
}
$NodeVersion = (& node -p 'process.versions.node').Split('.')
if ([int]$NodeVersion[0] -lt 22 -or ([int]$NodeVersion[0] -eq 22 -and [int]$NodeVersion[1] -lt 12)) {
    throw 'Node 22.12 or newer is required.'
}
$JavaCommand = (Get-Command java).Source
$PortProbe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $Port)
try { $PortProbe.Start() }
catch [Net.Sockets.SocketException] { throw "Port $Port is already in use. Stop the existing service or choose -Port <number>." }
finally { $PortProbe.Stop() }
Push-Location $Root
try {
    & "$PSScriptRoot/setup.ps1"
    Push-Location (Join-Path $Root 'explorer')
    try {
        & pnpm install --frozen-lockfile
        if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed; check pnpm and network access.' }
        & pnpm build
        if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
    } finally { Pop-Location }
    # dev.ps1 exits its calling PowerShell; keep orchestration in this parent.
    & (Get-Process -Id $PID).Path -NoProfile -File (Join-Path $Root 'dev.ps1') installDist
    if ($LASTEXITCODE -ne 0) { throw 'Kotlin build failed.' }
    Write-Host "Starting Explorer at http://127.0.0.1:$Port (wait for ready). Ctrl+C stops it."
    # One foreground JVM serves the Vite build; no batch prompt or orphaned dev server on shutdown.
    & $JavaCommand '-Xmx1g' '-XX:ActiveProcessorCount=4' -cp (Join-Path $Root 'cli/build/install/mtg-assistant/lib/*') dev.mtg.assistant.MainKt explorer (Join-Path $Root 'explorer/dist') $Port
    if ($LASTEXITCODE -notin @(0,130,-1073741510)) {
        throw 'Explorer stopped with an error; see output above. Check whether the port is already in use.'
    }
} finally { Pop-Location }
