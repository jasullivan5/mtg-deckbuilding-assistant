# Oracle identity observations

The application associates authored native physical cards with an explicit Oracle snapshot for coverage research. This observational layer does not change native IDs, card scripts, Commander legality or translation qualification.

Existing Oracle IDs and physical/face relationships are preserved. Deterministic matching uses available native identity and supported layout/face relationships, retaining unresolved and competing claims rather than guessing. Ambiguous names, contradictory native IDs or multiple claims cannot silently become a confident association. A matching text/name observation is not an eligibility or mechanics certificate.

Snapshot context and matching-algorithm version identify what produced an association. Change the algorithm version when matching rules change. Keep the parent external pool, native physical catalog, matched records, unresolved cases and exclusions separate in reports. Do not silently turn a new source version into authority for old reviewed corrections.

The current implementation is coverage/OracleEnrichment.kt; development and held-out fixture/review pairs remain under reference/phase28 because OracleEnrichment tests still consume them. They are bounded regression samples, not whole-catalog accuracy claims.

Use the maintained [coverage tools](CARD_COVERAGE.md) with explicit source files. Reviewed eligibility is a separate [effective metadata layer](ELIGIBILITY_CORRECTIONS.md). Old algorithm experiments, snapshot receipts and one-off scripts have been exported; see [repository layout](REPOSITORY_LAYOUT.md).
