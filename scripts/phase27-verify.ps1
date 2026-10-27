param(
    [Parameter(Mandatory=$true)][string]$Snapshot,
    [ValidateSet('before','after')][string]$Stage = 'after',
    [switch]$CoverageOnly,
    [string]$Repository = (Split-Path $PSScriptRoot -Parent)
)
$ErrorActionPreference = 'Stop'
$Repository = [IO.Path]::GetFullPath($Repository)
Push-Location -LiteralPath $Repository
try {
    $launcher = Join-Path $Repository 'cli/build/install/mtg-assistant/bin/mtg-assistant.bat'
    $timings = @()
    function Invoke-CheckedCli([string[]]$CliArguments) {
        & $launcher @CliArguments
        if ($LASTEXITCODE -ne 0) { throw "CLI failed with exit $LASTEXITCODE : $CliArguments" }
    }
    foreach ($pair in @(@('judith','Judith, the Scourge Diva'),@('krenko','Krenko, Mob Boss'),@('oviya','Oviya Pashiri, Sage Lifecrafter'))) {
        $watch = [Diagnostics.Stopwatch]::StartNew()
        $coverage = "generated/phase27/coverage-$Stage/$($pair[0]).json"
        $cliArguments = @('audit','catalog-coverage','--commander',$pair[1],'--oracle-cards',$Snapshot,'--objective','create-creature-tokens','--json','--output',$coverage)
        if($Stage -eq 'after') { $cliArguments += @('--translation-review','reference/phase27/translation-review.json') }
        Invoke-CheckedCli -CliArguments $cliArguments
        $timings += @{ command="coverage-$Stage-$($pair[0])"; wallMilliseconds=$watch.ElapsedMilliseconds }
        if($Stage -eq 'after') {
            Invoke-CheckedCli -CliArguments @('audit','catalog-missing','--report',$coverage,'--limit','20','--json','--output',"generated/phase27/missing-$($pair[0])-first20.json")
        }
        if($CoverageOnly -or $pair[0] -eq 'oviya') { continue }
        foreach ($objective in @(@('a','own-nontoken-creature-deaths'),@('b','reanimate-own-creatures'),@('c','create-creature-tokens'))) {
            $watch = [Diagnostics.Stopwatch]::StartNew()
            $snapshotPath = "generated/phase27/$Stage/$($pair[0])-$($objective[0]).json"
            Invoke-CheckedCli -CliArguments @('discover','--commander',$pair[1],'--objective',$objective[1],'--json','--output',$snapshotPath)
            $timings += @{ command="discovery-$Stage-$($pair[0])-$($objective[0])"; wallMilliseconds=$watch.ElapsedMilliseconds }
            if($Stage -eq 'after') {
                Invoke-CheckedCli -CliArguments @('audit','diff','--before',"generated/phase27/before/$($pair[0])-$($objective[0]).json",'--after',$snapshotPath,
                    '--json','--output',"generated/phase27/diffs/$($pair[0])-$($objective[0]).json")
            }
        }
    }
    $timingsPath = "generated/phase27/timings-$Stage.json"
    if($CoverageOnly -and (Test-Path -LiteralPath $timingsPath)) {
        $timings = @((Get-Content -LiteralPath $timingsPath -Raw | ConvertFrom-Json) | Where-Object { $_.command -notlike "coverage-$Stage-*" }) + $timings
    }
    $timings | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $timingsPath -Encoding utf8
} finally { Pop-Location }
