import {useEffect, useState} from 'react';
import {api, type Run, type Evidence, type CardImage} from './api';
import {CardInspection} from './CardInspection';

const labels = {
  RECOGNIZED: 'Recognized contribution',
  UNRESOLVED: 'Unresolved relationship',
  POLICY_EXCLUDED: 'Excluded by objective policy',
  OBJECTIVE_GAP: 'Outside current objective coverage',
  ORDINARY_BODY: 'Ordinary creature body',
  INELIGIBLE: 'Excluded by Commander eligibility',
  MISSING_DEFINITION: 'Missing native definition',
  IDENTITY_UNRESOLVED: 'Unresolved printing association',
  UNEXPLAINED: 'Insufficient evidence',
};
type Category = keyof typeof labels;
type Entry = {
  source: {card: string; expectation: string; oracleText: string; sourceUrl: string; printingId: string; oracleId: string; snapshotSha256: string};
  category: Category; explanation: string; definitionState: string;
  eligibility: {format: 'MATCH' | 'NO_MATCH' | 'UNKNOWN'; deck: 'MATCH' | 'NO_MATCH' | 'UNKNOWN'; identity: string[]; reason: string} | null;
  index: number | null; inFindings: boolean; relationshipChecks: number; cardChecks: number; unreadInstructions: number;
};
type Comparison = {reviewedOn: string; reviewedEngine: string; cards: Entry[]};

function CoverageInspection({entry, run}: {entry: Entry; run: Run}) {
  const [evidence, setEvidence] = useState<Evidence>();
  const [image, setImage] = useState<CardImage>();
  const [error, setError] = useState('');
  useEffect(() => {
    if (entry.index === null) return;
    const controller = new AbortController();
    const path = 'runs/' + run.runId;
    api<Evidence>(path + '/cards/' + entry.index, undefined, controller.signal)
      .then(setEvidence).catch(e => {if (!controller.signal.aborted) setError(e.message);});
    api<CardImage[]>(path + '/images?indices=' + entry.index, undefined, controller.signal)
      .then(images => setImage(images[0])).catch(() => { /* CardPicture shows the fallback. */ });
    return () => controller.abort();
  }, [entry.index, run.runId]);

  return <section id="coverage-inspection" className="detail" aria-label="Coverage card inspection">
    <h2>{entry.source.card}</h2>
    <h3>Source-reviewed relationship</h3>
    <p>{entry.source.expectation}</p>
    <details><summary>Retained Oracle text and identity</summary>
      <p className="coverage-oracle">{entry.source.oracleText}</p>
      <p>Oracle ID <code>{entry.source.oracleId}</code><br/>Printing ID <code>{entry.source.printingId}</code><br/>
        Snapshot SHA-256 <code>{entry.source.snapshotSha256}</code></p>
      <a href={entry.source.sourceUrl}>Scryfall printing record</a>
    </details>
    <h3>{labels[entry.category]}</h3>
    <p>{entry.explanation}</p>
    <p>{entry.definitionState} · {entry.eligibility?.format === 'MATCH' && entry.eligibility.deck === 'MATCH' ? 'Eligible' :
      (entry.eligibility?.format === 'NO_MATCH' || entry.eligibility?.deck === 'NO_MATCH') ? 'Ineligible' : 'Eligibility unassessed'}
      {entry.eligibility && ' · ' + (entry.eligibility.identity.join(', ') || 'Colorless')}</p>
    <p>{entry.inFindings ? 'In the ordinary findings index.' : 'Absent from the ordinary findings index.'}{' '}
      {entry.eligibility?.format === 'MATCH' && entry.eligibility.deck === 'MATCH' && <>
        {entry.cardChecks} pathway checks and {entry.unreadInstructions} unread instruction records for the card.
        {' '}{entry.relationshipChecks === 0 ? 'No pathway check for the reviewed relationship.' :
          entry.relationshipChecks + ' pathway checks for the reviewed relationship.'}
      </>}
    </p>
    {error && <p role="alert">{error}</p>}
    {entry.index !== null && !evidence && !error && <p role="status">Loading card evidence…</p>}
    {evidence && <>
      <h3>Native card and retained evidence</h3>
      <p className="hint">Rules comparison, engine tests and independent verification apply to the recorded scope. The cohort review adds no assurance records.</p>
      <CardInspection evidence={evidence} image={image} revision={run.applicationRevision}/>
    </>}
  </section>;
}

export function DiscoveryCoverage({run}: {run: Run}) {
  const [comparison, setComparison] = useState<Comparison>();
  const [error, setError] = useState('');
  const [category, setCategory] = useState<Category | 'ALL'>('ALL');
  const [selected, setSelected] = useState<string>();
  useEffect(() => {
    const controller = new AbortController();
    api<Comparison>('runs/' + run.runId + '/coverage', undefined, controller.signal)
      .then(setComparison).catch(e => {if (!controller.signal.aborted) setError(e.message);});
    return () => controller.abort();
  }, [run.runId]);
  const cards = comparison?.cards ?? [];
  const filtered = cards.filter(c => category === 'ALL' || c.category === category);
  const entry = filtered.find(c => c.source.oracleId === selected) ?? filtered[0];

  return <section aria-label="Discovery Coverage" className="coverage">
    <h2>Discovery Coverage</h2>
    <p><strong>Judith, the Scourge Diva · My nontoken creatures die</strong></p>
    <p>These cards were chosen to contrast sacrifice, death rewards, renewable creature fuel and boundary cases. This is a deliberately selected cohort.</p>
    {error && <p role="alert">{error}</p>}
    {!comparison && !error && <p role="status">Loading comparison…</p>}
    {comparison && <>
      <p className="hint">One category per reviewed relationship, with one relationship per distinct card. Counts partition all {cards.length} cards and stay fixed as you filter. Other abilities and evidence can differ.</p>
      <nav className="categories coverage-categories" aria-label="Coverage categories">
        <button aria-pressed={category === 'ALL'} onClick={() => setCategory('ALL')}><strong>{cards.length}</strong> All cards</button>
        {(Object.keys(labels) as Category[]).map(key => {
          const count = cards.filter(c => c.category === key).length;
          return <button key={key} disabled={!count} aria-pressed={category === key} onClick={() => setCategory(key)}>
            <strong>{count}</strong> {labels[key]}</button>;
        })}
      </nav>
      <details className="provenance"><summary>Comparison sources and revisions</summary>
        <p>Live structural discovery from this run, joined to retained Oracle snapshots and the adopted objective model. Review date {comparison.reviewedOn}. Gameplay evidence in the inspector may be historical.</p>
        <p>Application <code>{run.applicationRevision}</code><br/>Live engine <code>{run.argentumCommit}</code><br/>
          Retained comparison engine <code>{comparison.reviewedEngine}</code></p>
      </details>
      <div className="workspace">
        <section aria-label="Cohort cards">
          <h3>{filtered.length} of {cards.length} cards</h3>
          <div className="results">{filtered.map(c => <button className="card" key={c.source.oracleId}
            aria-label={c.source.card} aria-pressed={entry === c} aria-controls="coverage-inspection"
            onClick={() => setSelected(c.source.oracleId)}>
            <div><strong>{c.source.card}</strong><small>{labels[c.category]}</small></div>
          </button>)}</div>
        </section>
        {entry && <CoverageInspection key={entry.source.oracleId} entry={entry} run={run}/>}
      </div>
    </>}
  </section>;
}
