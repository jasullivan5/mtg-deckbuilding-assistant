# Active frozen regression inputs

These files are required by current tests and scripts/check-catalog.sh. They are historical source-reviewed expectations retained as current regression guards, not generated results or whole-catalog recall claims.

- deaths-initial.json: original death/removal review.
- deaths-expanded.json: expanded death/collection review used by the catalog gate.
- reanimation.json: graveyard-to-battlefield review.
- tokens.json: creature-token review.
- catalog-baseline.json: complete report hashes for Judith/Krenko across the three objectives.

Cleanup relocated these five files without content changes. path-relocations.json explicitly maps current file locations back to the original baseline location strings. Old source revisions inside them remain evidence. Runtime provenance and exact declared location strings are narrowly normalized by compare-baseline.py; every other report field remains checked. Never regenerate expectations merely to make a test pass.
