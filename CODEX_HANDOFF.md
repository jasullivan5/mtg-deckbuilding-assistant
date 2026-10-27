# Codex handoff

Current development adds structured `commander inspect` diagnostics, recursive native-extension safeguards and the application-owned Oviya Pashiri, Sage Lifecrafter definition. See docs/COMMANDER_AUTHORING.md for the repeatable workflow and bounded engine-test coverage. The accepted Phase2.5 migration notes below describe the baseline delivery; they are historical publication/environment records, not the current GitHub or local setup state.

Start with AGENTS.md, README.md and docs/ARCHITECTURE.md. This is accepted Phase2.5 prepared for Git development, not a new feature phase.

Current objectives: A own nontoken creature deaths (direct vs enhanced-removal classifications/policy preserved); B own graveyard creature cards directly to our battlefield; C creature-token creation under our control. Native Argentum integration, shared MechanicalFact/FactQuery, Judith extension/gameplay tests, diagnostics, frozen reviews and all 210 Kotlin tests are preserved byte-for-byte. A retains legacy-specific projection.

Pin: ebf5bf2e9fbb80d13a556d75491dd462e10c206f. Source archive SHA256: f5d7732d7cafbd7a183bd11068ddad0333183cebf605c7f9f98cb0cedc5e12da. Git tag phase2.5-baseline identifies the accepted import. Exact delivered HEAD/baseline SHAs and sizes are in the generated REPOSITORY_STATE.json receipt; git log is authoritative.

The first unpublished migration workspace reset before its Git objects/logs were saved. That lost local history is unavailable; this recovered repository has a new legitimate baseline import, not fabricated historical phase commits. See HANDOFF_VERIFICATION.md for verified versus incomplete checks. The prior session actually reported 210 baseline tests and 48 routine tests passing, but its current cold/full outcomes and logs could not be recovered.

No GitHub destination was confirmed. The available connection listed private research repositories but did not offer repository creation; unrelated repositories were untouched. This fallback includes complete local Git history. To publish with your own authorized GitHub CLI: `gh repo create mtg-deckbuilding-assistant --private --source=. --remote=origin --push`, then `git push origin --tags`. Inspect an existing destination first; never force-push unrelated work. No remote CI/environment success is claimed.

Work on focused branches. Run appropriate test classes, then all tests/full catalog comparisons for shared extraction, eligibility or policy changes. Preserve full before/after JSON in ignored generated/ for native audit diff. Compact baselines retain decision rows and complete-evidence hashes; changed hashes require review and are not automatically policy changes. Generated catalog reports are excluded from Git.

Known unsupported behavior: complex token-copy/controller/quantity variants, Amass/populate, multi-step reanimation and incomplete bindings/conditions. Unknown eligibility metadata is a separate data gap. Shared action IDs do not fully solve cross-objective causality. Frozen source-led benchmarks are not external adjudication or full-MTG recall. No new mechanical interpretations are added here.

Recommended first Codex task (not implemented): make the existing all-colors benchmark JSON an explicit report task with declared inputs/outputs, so cached/filtered tests cannot suppress it. Acceptance: regenerate a deleted report without recompiling unchanged upstream modules; preserve 11 accepted/four excluded/two unknown-boundary/one separately unresolved Amass outcomes; all 210 tests pass; no effect walker/objective change. Currently full verification reruns the selected Test task to produce the side output.

Recovery completed: fresh cold build 210/0, routine gate 48/0, four setup-guard tests, and all six catalog/complete-evidence comparisons passed. See HANDOFF_VERIFICATION.md and verification/handoff. Remote publication and hosted CI remain unperformed.
