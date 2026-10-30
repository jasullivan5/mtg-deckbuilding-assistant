# Active source evidence and test fixtures — required

This directory is not a general historical archive. Its 55 retained files are still consumed by current integration tests or referenced by the packaged `cli/src/main/resources/translation-assurance.json` ledger.

- alignment-batch1 and alignment-batch2: source/rulings, two Assay comparison trees and hash-bound evidence for currently registered native extensions.
- phase27: Krenko's Command/Bitterblossom sources, the Bitterblossom ruling and the compact coverage-loader fixture.
- phase28: development/held-out Oracle identity regression inputs.
- translation-reliability and fork-foundation: requirements, assessment and execution receipts that the current assurance ledger binds by path/hash.
- root card snapshots/rulings: source assertions for the current commander extensions.

Keep the original paths and contents: TranslationAssuranceTest verifies recorded hashes, and integration tests compare definitions against these sources. These records support bounded author/source-led evidence; they do not independently certify a whole card. Tatterkite's material concern remains blocked; other application records remain provisional without independent approval.

Unconsumed samples, old reports and experiment scripts were moved into the externalizable archive described in [repository layout](../docs/REPOSITORY_LAYOUT.md). Frozen objective reviews live in cli/src/test/resources/benchmarks. Do not move this active directory out of the repo.
