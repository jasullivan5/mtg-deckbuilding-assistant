import {PrimerMap, PrimerObjectives, PrimerPurpose} from './PrimerOrientation';

export default function ResearchPrimer() {
  return <article className="progress-page" aria-label="Primer">
    <p className="eyebrow">READING THE RESEARCH</p>
    <h2>How Card Integration Works</h2>
    <p className="progress-intro">We study how card mechanics contribute to a Commander deck’s goals, and eventually how to test their value in a deck. Evidence at one stage does not certify the next.</p>
    <section className="progress-panel"><h3>From rules to deck experiments</h3><PrimerMap/></section>
    <section className="progress-panel" aria-labelledby="objective-questions"><h3 id="objective-questions">Build a precise objective</h3><PrimerObjectives/></section>
    <section className="progress-panel"><h3>Test strategic value through experiments</h3>
      <p>Our working hypothesis is that probability and resource-access analysis, objective-aware piloting, controlled substitutions, native game simulation where supported, and pressure or disruption tests can help investigate deck value.</p>
      <p>We would measure objective progress, resource efficiency, resilience, competitive outcomes and uncertainty. A separate card-to-card strategic synergy model is not currently a prerequisite. Causal mechanical relationships still matter, and useful piloting without additional strategic guidance is unproven.</p>
      <p><strong>Future work.</strong> These deck experiments are not implemented. Their results would be only as reliable as the rules fidelity, pilot, objectives, baselines and environment.</p>
    </section>
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
    <p><strong>Available today.</strong> Discover runs four bounded queries. Progress inspects representation, partial interpretation and scoped trust evidence. The curated Mechanical Verification Lab runs specific native positions; it is not a deck simulator. General relationship filtering, objective composition, substitutions and contextual deck evaluation remain future work.</p>
  </article>;
}
