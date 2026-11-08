import {PrimerMap, PrimerPurpose} from './PrimerOrientation';

export default function ResearchPrimer() {
  return <article className="progress-page" aria-label="Primer">
    <p className="eyebrow">READING THE RESEARCH</p>
    <h2>How Card Integration Works</h2>
    <p className="progress-intro">We study why a card matters to a Commander deck’s goals. Each stage answers a different question; evidence at one stage does not certify the next.</p>
    <section className="progress-panel"><h3>From rules to a contribution</h3><PrimerMap/></section>
    <section className="progress-panel"><h3>What can we trust?</h3>
      <dl className="evidence-fields">
        <div><dt>Native definitions</dt><dd>Argentum SDK constructs represent a card for the rules engine. Availability establishes representation, not correspondence to Oracle text.</dd></div>
        <div><dt>Extraction and objectives</dt><dd>The application reads shared mechanical facts, then applies one of four bounded objective policies. MATCH is conditional structural potential. Unread structures, prerequisites and UNKNOWN remain visible.</dd></div>
        <div><dt>Execution</dt><dd>Argentum executes a specified position. A passing observation covers that scenario and engine revision, not the entire card or every gameplay condition. Historical failures remain evidence.</dd></div>
        <div><dt>Translation fidelity</dt><dd>Source review compares Oracle requirements with a native definition. The application ledger mainly covers its own extensions; inherited catalog fidelity is not measured at catalog scale.</dd></div>
        <div><dt>Independent qualification</dt><dd>A separate human must explicitly approve the exact source- and revision-bound evidence bundle. Author tests and successful demonstrations cannot substitute for that review. Stale and blocked evidence remain distinct.</dd></div>
      </dl>
    </section>
    <details className="progress-panel"><summary>Why mechanical contributions need context</summary><PrimerPurpose/></details>
    <p>Discover investigates a card’s mechanics. Progress measures coverage and evidence, shows controlled reusable gains, and identifies missing capabilities. Neither view currently evaluates substitutions, deck-slot opportunity cost or competitive position.</p>
  </article>;
}
