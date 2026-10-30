# External card coverage tools

Scryfall snapshots supply ordinary Commander-legal physical identities; Argentum supplies authored native mechanics. `audit catalog-coverage` compares these layers without translating cards or changing the three objectives.

## Explicit source data

Download deliberately with Python 3.11+; ordinary application queries do not fetch Oracle data:

```powershell
python scripts/oracle-snapshot.py download --directory generated/oracle
.\dev.ps1 installDist
```

Use the downloaded snapshot path printed by the tool. Its adjacent manifest records version, retrieval/source URI, encoding, size and hash. The downloader checks hosts and integrity, and manifest replay can reproduce an explicitly supplied archived version. The Kotlin loader streams declared JSON/JSONL/gzip forms and verifies integrity; no implicit current-source update is performed.

```powershell
$cli = '.\build\install\mtg-assistant\bin\mtg-assistant.bat'
$snapshot = '<downloaded snapshot path>'
& $cli audit catalog-coverage --commander 'Judith, the Scourge Diva' --oracle-cards $snapshot --objective create-creature-tokens --json --output generated/judith-coverage.json
& $cli audit catalog-missing --report generated/judith-coverage.json --limit 20
& $cli audit catalog-missing --report generated/judith-coverage.json --name Goblin --limit 20 --json
```

## Interpret scope and unknowns

Keep the eligible external physical-card denominator separate from authored native availability and objective observations. Unknown format/identity metadata, unsupported layouts, ambiguous or competing identity mappings, absent native definitions, unsupported mechanics and nonmatching represented pathways are different outcomes. No emitted pathway is not a proven negative. A confident association or available native definition is not translation certification.

When requested, existing objective extraction attaches matching/unresolved pathways and distinct unsupported native nodes. Coverage does not add another mechanics parser. Source provenance, actual engine revision, matching version and exclusions remain part of each report. Reviewed eligibility approvals remain bound to their source/native evidence; supplying another snapshot requires reconciliation where those approvals differ.

Offline downloader checks: `python scripts/test-oracle-snapshot.py`. Application coverage/identity/eligibility tests run through dev/dev.ps1. Historical sampling plans, old source versions, phase counts and Assay experiments have moved to the [historical export](REPOSITORY_LAYOUT.md); they are not current commands or next tasks.
