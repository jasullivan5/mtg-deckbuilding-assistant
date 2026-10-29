# Development handoff

Start with [README](README.md) and [AGENTS](AGENTS.md). The user supplies the next bounded assignment; old phase plans and recovery recommendations are not an active backlog.

## Repository and adoption authority

Application development branches normally start from this repository's approved `main`. Engine branches start from [our fork's `app-stable`](https://github.com/jasullivan5/argentum-engine/tree/app-stable); internal engine PRs target that branch. Fork `main` tracks upstream for comparison only. No upstream PRs or automatic upstream integration.

Application PR #9 was merged by the owner on 2026-10-29 at `088f5c4e8c352816fad0e26660f6cbec23d89ef7`, adopting `4133241856227d1d36269e92aaba633bbaf93f5c`. Read [ARGENTUM_COMMIT](ARGENTUM_COMMIT) for the authoritative dependency and [recorded verification](HANDOFF_VERIFICATION.md) for exact evidence. Later documentation commits in the engine fork do not require an application pin update.

The persisted assurance ledger still has Tatterkite's CONFIRMED concern/BLOCKED qualification, despite bounded corrected-engine execution. Explicit adopted-revision resolution evidence is a separate bounded follow-up; this maintenance pass does not change research verdicts or qualifications. The other 21 records remain provisional, without independent approval.

## Find the implementation

- [Architecture and extension points](docs/ARCHITECTURE.md): native integration, common extraction, objective-specific queries, effective eligibility and assurance.
- [Native authoring](docs/COMMANDER_AUTHORING.md), [reliability procedure](docs/TRANSLATION_RELIABILITY.md), [engine correction/integration](docs/ENGINE_COUNTER_CORRECTION.md).
- [Eligibility](docs/ELIGIBILITY_CORRECTIONS.md), [identity enrichment](docs/ORACLE_IDENTITY.md), [coverage](docs/CARD_COVERAGE.md), [limitations](KNOWN_LIMITATIONS.md).
- Tests: `cli/src/test/kotlin/dev/mtg/assistant/`; active revision-bound reviews: `benchmark/**/review-fork-v1.json`; original `review-v1.json` files are historical frozen inputs.
- Packaged assurance: `cli/src/main/resources/translation-assurance.json`; approved eligibility context: `cli/src/main/resources/eligibility/phase29.json`; retained source/test receipts: `reference/`.

Use ordinary branches in the primary application and engine checkouts. The clean nested `argentum/` supplies the exact dependency; it is not the engine development branch. Worktrees are temporary isolation for demonstrated needs; inspect shared Git storage, active consumers and ignored evidence before removal. See [everyday workflow](README.md#everyday-workflow-and-documentation-ownership).

## Setup and evidence

Use [README commands](README.md#setup-and-verification) and [environment prerequisites](docs/CODEX_ENVIRONMENT.md). Setup refuses an existing wrong/dirty engine revision. Use `dev`/`dev.ps1`; do not bypass its guards or invoke the root test task directly. Current recorded evidence is indexed in [HANDOFF_VERIFICATION](HANDOFF_VERIFICATION.md); reproduce only checks relevant to the assignment.

Meaningful old recovery, publication, count and proposed-task text is preserved in [the historical entry-point archive](docs/history/PRE_CONSOLIDATION_HANDOFF.md). Historical phases, receipts, source archives and benchmark expectations remain unchanged.
