$ErrorActionPreference = 'Stop'
$integrationRoot = Split-Path $PSScriptRoot -Parent
$candidateRoot = Join-Path $integrationRoot 'mtg-full-upstream'
$startupLog = Join-Path $integrationRoot 'upstream-candidate-startup.log'
$startupReceipt = Join-Path $PSScriptRoot 'delivery-evidence/candidate-startup.json'
$candidateHead = (& git -C $candidateRoot rev-parse HEAD).Trim()
if ($candidateHead -ne 'ccaf1926ec754799711fa921466de2f636137e7e') { throw 'Unexpected application source revision' }
$startupPort = 8875
$startupProbe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $startupPort)
try { $startupProbe.Start() } finally { $startupProbe.Stop() }
$candidateJava = 'C:/Users/joes/.jdks/jdk-21.0.12.1+1/bin/java.exe'
& $candidateJava '-Xmx1g' '-XX:ActiveProcessorCount=4' -cp (Join-Path $candidateRoot 'build/install/mtg-assistant/lib/*') dev.mtg.assistant.MainKt explorer (Join-Path $candidateRoot 'web/dist') $startupPort *> $startupLog
$startupExit = $LASTEXITCODE
$startupOutput = Get-Content -LiteralPath $startupLog -Raw
$healthReady = $false
$healthError = $null
try {
    $startupHealth = Invoke-RestMethod "http://127.0.0.1:$startupPort/api/v1/health" -TimeoutSec 3
    $healthReady = $startupHealth.status -eq 'ready'
} catch { $healthError = $_.Exception.Message }
@{
    application = $candidateHead
    engine = '56eeb394ef4fd2b9b7467b2ae95b3ccf04ac0906'
    javaExitCode = $startupExit
    port = $startupPort
    readyHealthObserved = $healthReady
    healthError = $healthError
    catalogCollisionObserved = $startupOutput.Contains('Extension identity collision: Spined Tyrranax')
    log = $startupLog
    logSha256 = (Get-FileHash -LiteralPath $startupLog -Algorithm SHA256).Hash.ToLowerInvariant()
    note = 'Normal installed candidate startup; no diagnostic catalog substitution. Packaged revisions are independently recorded; no candidate health SHA is asserted.'
} | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $startupReceipt -Encoding utf8
Get-Content -LiteralPath $startupLog -Tail 12
exit $startupExit
