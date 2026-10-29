# Recorded verification and adoption

These are recorded results from the fork transition, not tests rerun by the documentation-maintenance assignment. Application [PR #9](https://github.com/jasullivan5/mtg-deckbuilding-assistant/pull/9) was merged by the owner on 2026-10-29 at `088f5c4e8c352816fad0e26660f6cbec23d89ef7`. Its latest `verify` job [succeeded](https://github.com/jasullivan5/mtg-deckbuilding-assistant/actions/runs/40813458391/job/123248940586); that workflow runs the fast gate, not the entire native suite or catalog matrix.

[ARGENTUM_COMMIT](ARGENTUM_COMMIT) now adopts `4133241856227d1d36269e92aaba633bbaf93f5c` from our fork. The preceding approved revision was `ebf5bf2e9fbb80d13a556d75491dd462e10c206f`. Engine code/test sources at the adopted revision match the original tested correction `57aa530bca47e2c36aa37092958c7d283cf11ed3`; only fork guidance differs.

| Recorded transition check | Outcome | Compact receipt |
|---|---|---|
| Native rules engine, including 17 focused collection fixtures | 7,507 passed; 13 representative native-card scenarios also passed | [Pre-adoption verification](reference/fork-foundation/pre-adoption-verification.json) |
| Normal published-source application build | 457 tests / 57 suites; zero failures/errors/skips; all nine mutation controls/detections pass | [Normal verification](reference/fork-foundation/normal-verification.json) |
| PowerShell/Git Bash setup and wrapper safeguards | 16 passed | [Setup](reference/fork-foundation/setup-verification.json) |
| Existing local-Git setup fixtures | Four passed | [Bootstrap fixtures](reference/fork-foundation/local-bootstrap-tests.json) |
| Complete discovery, coverage and missing-card comparisons | Nine + three + three matched after declared binding/timing differences | [Controlled comparison](reference/fork-foundation/controlled-comparison.json) |
| Separate catalog gate | Six full-report comparisons and six benchmarks passed; zero confirmed mismatches; coverage/cross-examination and empty self-diff | [Catalog gate](reference/fork-foundation/catalog-gate.json) |

The [initial normal-run failure](reference/fork-foundation/normal-initial-failure.json) preserves five benchmark revision refusals and a CRLF-only golden mismatch. Their corrections preserved historical expectations and golden data. [Versioned benchmark bindings](reference/fork-foundation/benchmark-rebindings.json) preserve the old inputs. Full outputs and logs remain ignored; receipts bind exact sources, runtime revisions and relevant hashes.

Passing source-correct execution does not supply independent certification. Tatterkite's persisted concern remains CONFIRMED/BLOCKED until a separate adopted-revision resolution record is reviewed; the other 21 application records remain provisional. Other counter emitters and retained replacement policies are not universally certified. No browser/UX or hosted-environment creation is claimed.

See [engine integration](docs/ENGINE_COUNTER_CORRECTION.md) for reproduction and limitations, [assurance procedure](docs/TRANSLATION_RELIABILITY.md) for qualification boundaries, and [historical recovery text](docs/history/PRE_CONSOLIDATION_HANDOFF.md) for old platform/count/export records. Documentation checks and workspace inventory from the maintenance pass are reported separately; this file does not turn them into mechanical execution evidence.
