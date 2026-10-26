# Judith verification sources

Retrieved 2026-10-25 directly from Scryfall:

- https://api.scryfall.com/cards/rna/185 — current Oracle/characteristics and original RNA #185 printing metadata.
- https://api.scryfall.com/cards/0a742125-730d-4082-bfd8-5feb7733def4/rulings — three official rulings, retained in native metadata.
- https://api.scryfall.com/cards/search?order=released&dir=asc&q=oracleid%3Ac01516e0-eec3-4370-b935-7674982e8850&unique=prints — canonical printing check, ignoring promo objects.

The raw responses are archived beside this file. Legality and BR identity are a verified dated snapshot, not a live rules feed. All formats representable by the pinned SDK and legal in the Scryfall response are included; unrepresentable formats are not invented.

Argentum Assay's `compile --file` successfully read both abilities. `judith-assay-reading.json` is the emitted native reading, extracted from the actual command log. It is independent implementation guidance, never the runtime loader; its partial metadata is not authoritative printing metadata. The application authors the DSL definition explicitly and validates it with native CardValidator/CardLinter. Gameplay scenarios test actual resolution. The final default ability-source damage spelling agrees with Assay and preserves last-known information when Judith dies simultaneously with other creatures.
