# Current test material

Everything here is consumed by current tests, assurance validation or the catalog gate. It is not an experiment archive.

| Folder | Purpose |
|---|---|
| cards | Named Oracle source/ruling snapshots and native proposal comparisons for current card definitions |
| native-definitions | Reviewed serialization goldens for registered application cards |
| identity | Coverage-loader and development/held-out physical identity fixtures |
| assurance | Source requirements, assessments and dated execution evidence bound by the current assurance ledger |
| regression | Frozen objective reviews, complete-report baseline and exact path-relocation aliases |

fixture-manifest.json binds the 82 retained data files by portable LF-normalized SHA256. They were compared against their pre-move hashes before this manifest was recorded. Current tests verify those bytes as well as the original source/definition/assurance relationships. New fixtures should have a current consumer and a behavioral name.

Some recorded JSON content contains historical paths, immutable review IDs or original engine revisions. Those identify the evidence as originally produced, not the present filesystem or a current assignment. The packaged assurance ledger now references the actual retained fixture locations; source hashes and qualification states are unchanged. Dated evidence is not newly executed certification.

The complete-report gate reverses only declared exact path relocations and runtime engine source-URL provenance when comparing with the immutable baseline. It cannot normalize verdicts, facts, quantities, prerequisites, source content or concern state. Expectation JSON was not regenerated.

Unused research records live in the movable export described in [repository layout](../../../docs/REPOSITORY_LAYOUT.md).

The 2026-10-30 card explanation/image fixtures have separate retrieval URLs and raw hashes in `cards/image-and-explanation-sources.json`, consumed by FindingExplanationTest. They preserve the three sacrifice examples, empty ruling lists where appropriate, and a double-faced image identity case. They do not change the original 82-file relocation manifest or independently certify those cards.
