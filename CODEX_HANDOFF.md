# Development handoff

Read [README](README.md), [AGENTS](AGENTS.md), [architecture](docs/ARCHITECTURE.md) and the [engine workflow](docs/ENGINE_WORKFLOW.md).

Normal application dependency selection is the clean sibling maintained fork on app-stable. The actual engine SHA is packaged with builds and returned in research reports. Native/application compatibility belongs before acceptance of engine PRs into app-stable; no separate application pin-adoption PR is required.

Explorer M1 work is preserved on feature/explorer-m1 at a35b9b5 and paused while the sibling integration migration is reviewed. No Explorer milestone is accepted.

Historical fork-transition receipts are indexed in [HANDOFF_VERIFICATION](HANDOFF_VERIFICATION.md). Historical pins, benchmark expectations, source snapshots and qualification concerns remain evidence, not active dependency selectors. Tatterkite remains BLOCKED with its CONFIRMED concern; the other 21 application translation records remain provisional without independent approval.

Implementation navigation:
- Discovery/eligibility: cli/src/main/kotlin/dev/mtg/assistant/discovery and eligibility.
- Build provenance: EngineBuild.kt, generated engine-build.properties and the evidence-equivalence manifest.
- Native extensions and assurance: extensions, resources/translation-assurance.json.
- Coverage, identity and diagnostics: coverage and audit.
- Source/tests/reviews: reference, benchmark and cli/src/test.

Use dev/dev.ps1. Preserve local work; engine feature work uses isolation when the normal stable checkout is needed. Do not delete the retired ignored nested clone or temporary worktrees without checking history, outputs and consumers.
