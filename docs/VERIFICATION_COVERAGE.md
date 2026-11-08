# Verification coverage and independent fidelity challenge

Increment 3 uses the existing global Commander universe and evidence gates. It adds an index of actual scoped evidence, not another certification authority. The author and implementer are Codex Build; independent qualification is pending a genuinely separate review.

## What can I do now?

Open **Progress → Cards / Trust and evidence** without running discovery. Compare native availability with attributable interpretation, current source-review and dated execution records. Expand a card to inspect its physical/face identity, ability scope, requirement results, source hash, definition fingerprint, author, engine and missing assurance. Open the challenge findings to see deliberately incorrect definitions that the original checks detect, leave unresolved or miss.

Discover, Progress and Primer remain the primary navigation. Windows launch is unchanged:

```powershell
.\scripts\explorer.ps1
```

Global external audit counts still require the explicit compatible bulk snapshot:

```powershell
.\scripts\explorer.ps1 -OracleSnapshot generated/oracle/oracle-cards-20261026210159.jsonl.gz
```

The verification index independently uses the **retained PR39 reference universe**, whose complete physical identity list, counting policy, source hash and associations are already packaged. It does not fetch bulk source data. It checks the existing native-identity digest before using those associations; changed identities fail visibly and require an explicitly reviewed compatible association baseline. An absent live bulk source does not silently select a new universe.

## Measurement contract

The denominator remains **32,109 Commander-legal physical cards**, source update `2026-10-26T21:01:59.922+00:00`, SHA256 `081a50ef82c767523f955e914153cf988ff045a72e3765b6aa2382567f9eb53a`, 25,135,745 bytes, `physical-format-population-v1` / `physical-identity-v1`. PR39's compressed representation baseline is unchanged. The same source has 39,049 records: 32,116 legal, 83 banned and 6,850 not legal; seven legal meld-result records are not additional physical cards. No unknown legality is silently excluded.

`coverage/VerificationCoverage.kt` owns `scope-aware-verification-v1`. Its units are deliberately separate:

- **Physical cards:** distinct legal Oracle identities in the retained universe. Faces/printings and native aliases do not increase this count.
- **Interpretation records:** existing `StructuralSupport` occurrences with exact native face, path and action IDs. Per-card summaries include supported, partial and unsupported counts. A card with any record is evidenced, not fully interpreted. Absence of records is not a nonmatch.
- **Reviewed scopes:** one explicitly bounded ability or a legacy broad review whose ability allocation is unknown. Multiple requirements and scenarios do not multiply cards.
- **Requirements:** shared requirement identifiers plus source-specific parameters; their applications are counted separately. Supported, contradicted, unresolved and execution-only UNTESTED remain distinct.
- **Execution observations:** an actual scenario, original execution time, exact engine, native fingerprint and source/harness bindings. A second ingestion of the same observation is not another run.
- **Qualification:** only approvals accepted by the existing `assessTranslation` gate. The index issues no approvals. Whole-card qualification is not inferred from a qualified partial scope.

Categories overlap and are not a certification funnel. A recorded-current observation is one whose exact bindings agree; it does not mean a fresh execution on page load.

## Evidence sources and identity

The versioned index joins evidence to the same global physical identities, not another identity matcher. Each record has a stable observation ID, physical Oracle ID, optional printing/face, definition fingerprint, native ability/action scope or explicit unresolved scope, source references and hashes, requirement results, engine/application identities, author, expectation, observation, scenario/date when applicable, limitations and concerns.

Ingestion is deterministic:

1. The **22 extension assurance records** retain their source/requirements/tests, stale definitions/revisions, broad scope and material concerns. They are not retroactively assigned to exact abilities. Historical card-level execution labels and test references are not scenario receipts.
2. The **six-card pilot** contributes its live bounded source comparisons and its recorded 11 scenario observations. The five Lab positions are already among those observations and are not ingested a second time. Reading the Lab registry is not execution.
3. The **additional source challenge** contributes three available source-reviewed sacrifice scopes and two unresolved missing-native scopes. Its native execution receipt separates four production-definition observations from one deliberately erroneous test-only definition.
4. The **historical Spark failure** is indexed as documentation-bound historical execution on engine `03869c08665bf476a8b40dacbfbcd501c54a33bd`. The missing exact old definition fingerprint/raw receipt remains explicit. It cannot become current-engine evidence.
5. Other historical test files without attributable executed observations remain **unindexed**. Their corpus breadth is unmeasured, not zero passing or zero failing.

Identical observation IDs deduplicate; conflicting payloads with the same ID fail closed. Face binding, definition changes, missing scenario identity and engine revisions are independently checked. Execution requires the actual exact engine; source-ledger compatibility can use the existing `EngineBuild.evidenceCompatible` rule. Source/harness changes stale a receipt even if a definition happens to agree. Known failures and open concerns survive staleness. Current source comparisons can contain contradicted requirements; “current” is freshness, not approval.

The index also supplies exact current native definitions for reviewed cards and the deliberately mutated definitions, allowing a separate reviewer to inspect implementation rather than rely on prose. Legacy historical fingerprints are not silently relabeled as those current definitions.

## Whole-card assurance criteria

The dashboard says **Not established at catalog scale** instead of deriving a verified-card percentage. No complete-card numerator is published.

A defensible bounded whole-card claim would require a source-derived inventory accounting for all relevant faces, printed abilities, keywords, intrinsic and alternative mechanics within a declared scope; current source/identity/definition-bound fidelity requirements for every material scope; appropriate positive, negative and consequential native execution observations; and no unresolved material concern inside the declared scope. Independently qualified whole-card assurance additionally requires separate approval of that exact bundle through the existing gate.

This does not promise exhaustive testing of every possible game state. It requires complete accounting for the declared relevant scopes, a justified bounded scenario strategy and explicit exclusions. SDK ability lists alone cannot prove that all Oracle-required mechanics were represented. The present native walker and assurance ledger do not supply that complete source-to-native inventory.

## Challenge of the frozen sacrifice method

The unchanged method is `server/FidelityPilot.kt`, SHA256 `520a5b3d03a024731586b6ca6e37d3e23acfd6d1fb934f97a6ea154536519e7a`. Its source expectations were not weakened. Source text, rulings and additional requirements for new cases were committed before examining their native implementations (`2cd9463`, then `e454d62`). Current pilot source capture is separate from the dated global universe. The rules references retain the official November 6 rules URL, digest and excerpts.

Initial source cases were Sakura-Tribe Elder, Augur of Skulls and held-out Qasali Pridemage. Actual native inspection found the last two absent; they remain unresolved rather than being integrated. Source-grounded Hapless Researcher and held-out Caustic Caterpillar were then frozen as available varied controls. Availability filenames were checked before reading implementations. The same agent knew public mechanics and historical research; this is **not fully blind or independent certification**.

| Original check | What it actually establishes | Important limit |
| --- | --- | --- |
| Physical source identity | Exact native printing **or** native Oracle match against the captured source | A different source printing can fail despite the global audit's confident physical association |
| Exact source | One recognized SELF sacrifice fact across the examined card | Not an exhaustive ability inventory or runtime copy/identity proof |
| Battlefield to owner graveyard | Nominal source/destination/owner fields in that fact | Not borrowed-source or replacement execution; controller is described rather than comprehensively constrained |
| Cost or resolution | Recognized activation-cost versus effect role | Not full mana quantity or every activation restriction |
| Arriving characteristics | Extracted source-card creature status agrees | Not every type-changing or replacement interaction |
| Target declaration | Target presence versus absence | Not target filter, controller restriction, quantity or resolving effect fidelity |
| Tap requirement | Existing SDK CostTap declaration | Not all other payment/resource requirements |
| Trigger timing | Activation versus an end-step StepEvent | Not every activation window, trigger controller or conditional branch |
| Object generation (separate) | Explicitly UNTESTED statically | Needs native adverse execution; no structural pass may promote it |

### Findings

- **Four detected contradictions:** missing required target, missing tap payment, wrong arriving creature characteristics and wrong trigger step.
- **Two variants not accepted as supported:** wrong/granting source and sacrifice moved from cost to resolving effect produce unresolved extraction rather than a proven specific contradiction. The first test incorrectly expected the latter to be a contradiction; the observed unresolved category was retained. The method was not changed to force a result.
- **Two missed adversarial defects:** removal of Caustic Caterpillar's `{1}{G}` payment and replacement of its artifact/enchantment destruction target/effect with an any-target damage relationship. The original eight checks still support these static variants. This is an observed method limitation, not a catalog-wide false-positive percentage.
- **Two execution-dependent classes:** source generation after a zone round trip and owner/replacement behavior cannot be established by this static method. The historical Spark failure demonstrates why nominal self-sacrifice facts are insufficient.
- **Exact-printing limitation:** the new Sakura source capture is a different printing from its native definition. The original physical check contradicts that exact binding even though the unchanged global association identifies the same Oracle physical card confidently. This is retained as a method identity-binding concern, not promoted to a confirmed rules mistranslation.

The existing engine executed a reusable activation-payment pattern for Sakura, Hapless and Caustic. Four production-definition positions agree with the declared source expectations, including rejecting Caustic without required mana. The deliberately missing-mana mutant then activates and sacrifices with no mana; that observation **DIFFERS** from the same frozen source requirement. Its mutated fingerprint, native actions/states and mismatch are retained in the challenge receipt. It never contributes a failure to production-card coverage. Search, draw/discard and destruction resolution remain outside these payment observations.

The first challenge execution test completed actions but failed receipt publication due to a properties-class resource lookup. The classloader was corrected explicitly; this harness failure is not an engine defect. Failed focused-run logs remain under ignored generated output. The original method, first source expectations and initial historical failures are unchanged.

## Reproduction and future comparisons

```powershell
.\dev.ps1 test ':assistant:installDist'
# Explicit bounded execution; dashboard reads do not run scenarios:
.\build\install\mtg-assistant\bin\mtg-assistant.bat research fidelity-challenge --output generated/verification-coverage/new-execution.json
.\build\install\mtg-assistant\bin\mtg-assistant.bat research verification-baseline --output generated/verification-coverage/new-baseline.json.gz
.\build\install\mtg-assistant\bin\mtg-assistant.bat research verification-compare --before <baseline.json.gz> --after <later.json.gz> --output generated/verification-coverage/new-comparison.json
```

The existing CREATE_NEW workflow refuses overwrites. The first verification baseline is separately packaged as `verification-baseline.json.gz`; it does not replace the representation baseline or historical Judith comparisons. It retains source and population digests, engine/application identities, measurement version, attributable interpretation summaries, scoped records, freshness/results/concerns, qualified scopes, exact review definitions, challenge findings and unindexed areas.

Comparisons separate new current source scopes, new execution observation identities, records made stale, new contradictions, concern changes, qualification changes and population growth/removal. New source scopes on newly added population identities are separate from review gains within the previous universe. Changed schema/counting definitions suppress gain claims. A changed source universe under the same source hash is invalid for like-for-like claims. Re-running a test file does not itself add an indexed execution observation. Future receipts should be retained immutably and ingested explicitly rather than silently replacing historical records.

API: `/api/v1/progress/verification` supplies the cached live projection and examinable bundle; `/api/v1/progress/verification-baseline` exposes the immutable initial baseline. The normal bounded Kotlin research worker handles computation, preserving Discover's latest run. React filters/presents records and does not evaluate Magic mechanics or qualification.

## Product Studio investigations

1. Compare Cards' 16,927 available definitions with the independent verification rows, source/engine scope counts and whole-card limitation. Inspect the provenance and explicitly overlapping units.
2. Expand **Hapless Researcher**. Its sacrifice payment has bounded evidence; its resolving draw/discard behavior and whole-card source inventory remain outside this review. Filter **Spark Elemental** to HISTORICAL and inspect the old failure, then return to CURRENT. Filter **Tatterkite** to see the stale ledger and preserved material concern.
3. Open challenge findings and inspect **missing mana** and **wrong target filter**. Compare the static missed defects with the isolated native missing-mana mismatch. Notice that the real production definition rejects missing mana and that test mutants do not enter production-card failure counts.

## Next bounded decision

Commission a separate source-first review of a **complete activation-cost requirement** for this exact cohort, using the missed `{1}{G}` mutant as a mandatory negative control, before considering wider source-fidelity promotion. This should assess the shared requirement and evidence, not certify additional cards. Target-filter gaps, full scope enumeration and foreign-owner/replacement coverage remain documented limits. This follow-up has not begun.

## Delivery receipt

Final measured counts, exact clean-build identities, fresh verification outcomes, controlled-comparison hashes and actual desktop/390px screenshots are appended after validation. Product acceptance remains with the user; implementation checks do not grant source qualification.
