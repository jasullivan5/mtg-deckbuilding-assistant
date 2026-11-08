import {useEffect, useState} from 'react';
import {api} from './api';

type AssuranceRecord = {card: string; evidenceCurrent: boolean; sourceFidelity: string; engineExecution: string; qualification: string; reasons: string[]; evidence: unknown};
type Progress = {
  applicationRevision: string; argentumCommit: string; measuredAt: string; population: string[]; registeredNames: number;
  definitionOrigins: Record<string, number>;
  external: {state: string; reason: string; snapshot?: {sha256: string; updatedAt: string}; commander: string; counts: Record<string, number>; exclusions: Record<string, number>};
  mechanics: {unit: string; denominator: number; counts: Record<string, number>};
  families: {nativeType: string; occurrences: number; supported: number}[];
  factKinds: Record<string, number>; factConfidence: Record<string, number>;
  assurance: {population: number; notMeasured: number; current: number; stale: number; missing: number; fidelity: Record<string, number>; qualification: Record<string, number>; records: AssuranceRecord[]};
  execution: string;
  obstacles: {id: string; problem: string; impact: string; why: string; examples: string; investigation: string; evidenceStatus: string; records: string}[];
  capabilities: {capability: string; state: string; limit: string}[];
};
type Group = {fingerprint: string; nativeType: string; occurrences: number; distinctCards: number; supportStatus: string; reasons: string[]; representatives: {card: string; face: string; path: string; sourceReferences: string[]}[]};
type History = {
  observation: string; engine: string; commander: string; objective: string; population: string[]; exclusions: Record<string, number>; comparisonMethod: string;
  stages: {label: string; applicationRevision: string; sourceSha256: string; matches: number; matchingCards: number; unknowns: number; nonmatches: number; unsupported: number}[];
  comparisons: {comparable: boolean; baselineApplicationRevision: string; newMatchingPathways: number; newMatchingCards: string[]; resolvedNonmatches: number; lostMatches: unknown[]; newUnknowns: number; removedPathways: unknown[]; changes: {card: string; face: string; path: string; before: string; after: string; prerequisites: string[]; decisions: unknown[]; sourceReferences: string[]}[]}[];
};
const number = (n: number) => n.toLocaleString();
const human = (s: string) => s.replaceAll('_', ' ').toLowerCase();
const repository = 'https://github.com/jasullivan5/mtg-deckbuilding-assistant/blob/';
function SourceLink({path}: {path: string}) {return <a href={`${repository}main/${path}`} target="_blank" rel="noreferrer">Read investigation</a>;}
function Counts({counts, caption}: {counts: Record<string, number>; caption: string}) {
  return <div className="progress-table"><table><caption>{caption}</caption><thead><tr><th>Category</th><th>Count</th></tr></thead><tbody>
    {Object.entries(counts).map(([key, count]) => <tr key={key}><th scope="row">{human(key)}</th><td>{number(count)}</td></tr>)}
  </tbody></table></div>;
}
function JsonDetails({label, value}: {label: string; value: unknown}) {
  return <details><summary>{label}</summary><pre>{JSON.stringify(value, null, 2)}</pre></details>;
}

export default function ResearchProgress() {
  const [data, setData] = useState<Progress>();
  const [history, setHistory] = useState<History>();
  const [groups, setGroups] = useState<Group[]>();
  const [error, setError] = useState('');
  const [historyError, setHistoryError] = useState('');
  const [recordError, setRecordError] = useState('');
  const [query, setQuery] = useState('');
  const [populationQuery, setPopulationQuery] = useState('');
  const [state, setState] = useState('ALL');
  const [page, setPage] = useState(0);
  const [historyQuery, setHistoryQuery] = useState('');
  const [selectedComparison, setSelectedComparison] = useState(1);
  const [loadingRecords, setLoadingRecords] = useState(false);
  function load() {
    setError('');
    api<Progress>('progress').then(setData).catch(e => setError(e.message));
  }
  useEffect(load, []);
  useEffect(() => { api<History>('progress/history').then(setHistory).catch(e => setHistoryError(e.message)); }, []);
  async function inspectRecords() {
    if (groups || loadingRecords) return;
    setLoadingRecords(true); setRecordError('');
    try { setGroups((await api<{groups: Group[]}>('progress/mechanics')).groups); }
    catch (e) { setRecordError(e instanceof Error ? e.message : String(e)); }
    finally { setLoadingRecords(false); }
  }
  if (!data) return <section className="progress-page" aria-label="Research Progress Dashboard">
    <h2>Research Progress Dashboard</h2>{error ? <><p role="alert">{error}</p><button onClick={load}>Retry measurement</button></> : <p role="status">Measuring the native catalog through Kotlin…</p>}
  </section>;
  const {external, mechanics, assurance} = data;
  const supported = mechanics.counts.SUPPORTED ?? 0;
  const percentage = mechanics.denominator ? 100 * supported / mechanics.denominator : undefined;
  const rows = groups?.filter(g => (state === 'ALL' || g.supportStatus === state) &&
    `${g.nativeType} ${g.representatives.map(r => r.card).join(' ')}`.toLowerCase().includes(query.toLowerCase())) ?? [];
  const comparison = history?.comparisons[selectedComparison];
  const historyRows = comparison?.changes.filter(r => r.card.toLowerCase().includes(historyQuery.toLowerCase())) ?? [];
  return <article className="progress-page" aria-label="Research Progress Dashboard">
    <div className="progress-heading"><div><p className="eyebrow">BREADTH · TRUST · REUSABLE GAINS</p><h2>Research Progress Dashboard</h2>
      <p className="progress-intro">How much can we understand today, and what will unlock the next useful research capability?</p></div>
      <span className="progress-badge">Current native inventory<br/>Historical gains kept separate</span></div>
    <p className="hint">Measured {new Date(data.measuredAt).toLocaleString()} · All authored native physical definitions, with no Commander or objective filter. External coverage below has its own population.</p>
    <div className="progress-headlines">
      <a href="#definitions" className="progress-stat"><strong>{number(data.population.length)}</strong><span>native physical definitions</span><small>{number(data.registeredNames)} registered names, including faces</small></a>
      <a href="#mechanics" className="progress-stat"><strong>{percentage === undefined ? 'Not measured' : `${percentage.toFixed(1)}%`}</strong><span>typed nodes explicitly supported</span><small>{number(supported)} / {number(mechanics.denominator)} nodes; local interpretation only</small></a>
      <a href="#assurance" className="progress-stat uncertainty"><strong>Not measured</strong><span>catalog-wide source fidelity</span><small>{assurance.records.length} extension scopes in the narrower ledger</small></a>
      <a href="#history" className="progress-stat"><strong>{history ? `+${history.comparisons[1].newMatchingCards.length}` : 'Historical'}</strong><span>newly matching cards</span><small>Source-bound sacrifice; controlled historical population only</small></a>
    </div>
    <aside className="progress-callout"><strong>Research bottlenecks:</strong> inherited source fidelity is not measured, and native interpretation remains partial. <a href="#obstacles">Explore the prioritized obstacles and smallest next investigations</a>.</aside>

    <section className="progress-panel" id="definitions"><p className="eyebrow">01 / REPRESENTATION</p><h3>What can we represent?</h3>
      <p>The native inventory groups registry faces into physical definitions using the existing <code>physicalCards</code> policy. Back/flip aliases and standalone meld results are excluded. This is an authored inventory, not a count of reviewed Oracle identities or printings.</p>
      <Counts caption="Disjoint native definition origins · all native physical definitions" counts={data.definitionOrigins}/>
      <div id="external" className="progress-callout"><h4>External reference population · {external.state === 'MEASURED' ? external.commander : 'Not measured'}</h4><p>{external.reason}</p>
        {external.state === 'MEASURED' && <>
          <p><strong>{number(external.counts.confident_native_identity_associations)} / {number(external.counts.eligible_oracle_cards)}</strong> confidently associated external physical identities {external.counts.eligible_oracle_cards > 0 ? `(${(100 * external.counts.confident_native_identity_associations / external.counts.eligible_oracle_cards).toFixed(1)}%)` : '(percentage undefined for an empty eligible population)'}. A confident association does not establish translation correctness.</p>
          <p>Reference snapshot: {number(external.counts.distinct_snapshot_records)} distinct records. The selected pool excludes {number(external.counts.excluded_records)} records and keeps {number(external.counts.unresolved_pool_records)} unresolved pool records outside its denominator.</p>
          <p>{number(external.counts.native_definition_missing)} eligible physical identities lack a native definition. {number(external.counts.eligible_oracle_cards - external.counts.confident_native_identity_associations - external.counts.native_definition_missing)} other eligible identities lack a confident association; missing and ambiguous are separate from failed translation.</p>
          <p>The audit retains {number(external.counts.effective_metadata_discrepancies)} effective metadata disagreements among associated identities. Association is not discovery eligibility. This external audit runs no objectives; its objective counters are not measured mechanical outcomes.</p>
          <details><summary>Audit categories, unresolved identities and exclusions</summary><Counts counts={external.counts} caption="Existing catalog-coverage audit · units are named in each row; diagnostic counts overlap"/>
          <Counts counts={external.exclusions} caption="Exclusion reasons overlap; do not sum reasons into excluded cards"/></details>
          <p>Snapshot updated {external.snapshot?.updatedAt} · <code>{external.snapshot?.sha256}</code></p>
          <a href="/api/v1/progress/external" target="_blank" rel="noreferrer">Inspect external records, mappings, pool exclusions and source manifest</a>
        </>}
        {external.state !== 'MEASURED' && <SourceLink path="docs/CARD_COVERAGE.md"/>}
      </div>
      <details><summary>Inspect the native physical population and exclusions</summary><p>Registered-name exclusions: {number(data.registeredNames - data.population.length)} names. This difference includes faces/aliases/meld results and is not an external missing-card count.</p>
        <label>Find native physical definition<input value={populationQuery} placeholder="Card name" onChange={e => setPopulationQuery(e.target.value)}/></label>
        <p>{data.population.filter(n => n.toLowerCase().includes(populationQuery.toLowerCase())).length} matching names; showing the first 40.</p>
        <ul>{data.population.filter(n => n.toLowerCase().includes(populationQuery.toLowerCase())).slice(0,40).map(n => <li key={n}>{n}</li>)}</ul>
        <a href="/api/v1/progress" target="_blank" rel="noreferrer">Complete measured population and report</a>
      </details>
    </section>

    <section className="progress-panel" id="mechanics"><p className="eyebrow">02 / INTERPRETATION & REUSE</p><h3>How much can we mechanically read?</h3>
      <p>The existing audit independently traverses serialized native definitions and compares typed nodes with the existing extractor’s support records. Nodes include operations, costs, conditions, filters and quantities. They are not cards or uniformly sized instructions. A supported node does not certify its entire ability.</p>
      {percentage !== undefined && <><label htmlFor="node-coverage">Explicitly supported native typed nodes · {number(supported)} of {number(mechanics.denominator)}</label><progress id="node-coverage" max={mechanics.denominator} value={supported}>{percentage.toFixed(1)}%</progress></>}
      <Counts caption={`Disjoint native typed-node distribution · denominator ${number(mechanics.denominator)} · current engine`} counts={mechanics.counts}/>
      <p className="progress-callout">Observed without local pathway is unclassified, not NO_MATCH. Some nodes are outside the four objectives or are descriptive metadata. No whole-card interpretation rate or whole-SDK coverage is claimed. Unvisited keyword-lowered behavior is outside this audit’s denominator.</p>
      <details><summary>Shared mechanical facts and unresolved requirements</summary><p>The same extractor emits reusable facts separately from objective acceptance. Confidence here describes the fact’s retained mechanical constraints, not translation fidelity or the chance of winning.</p>
        <Counts counts={data.factKinds} caption="Emitted mechanical facts by kind · all native physical definitions"/>
        <Counts counts={data.factConfidence} caption="Disjoint emitted-fact confidence states · not objective verdicts"/>
        <a href="/api/v1/progress/facts" target="_blank" rel="noreferrer">Inspect facts, requirements, constraints and native locations</a>
      </details>
      <h4>A defensible reuse measure</h4>
      <p><strong>{data.families.filter(f => f.supported > 0).length} native type labels</strong> have at least one explicitly supported occurrence through shared extraction logic. The audit observes {data.families.length} distinct serialized type values, including metadata and structures outside objective scope; that total is not a count of all mechanic families. A type can have both supported and unresolved occurrences. This is observable extractor reach, not a code-reuse score.</p>
      <details><summary>Inspect native type families and their occurrence coverage</summary><div className="progress-table"><table><caption>Per family · counts are typed-node occurrences in the same native population</caption><thead><tr><th>Native type</th><th>Supported</th><th>Observed</th></tr></thead><tbody>
        {data.families.map(f => <tr key={f.nativeType}><th scope="row">{f.nativeType}</th><td>{number(f.supported)}</td><td>{number(f.occurrences)}</td></tr>)}
      </tbody></table></div><p>Native SDK versus bespoke implementation reuse, objective consumer counts and reusable scenario-pattern reach are not measured automatically.</p></details>
      <details onToggle={e => {if (e.currentTarget.open) void inspectRecords();}}><summary>Inspect mechanical groups and representative cards</summary>
        <p>Each audit group retains up to five representative native locations; its card count is distinct within that group. Cards can appear in multiple groups. Do not sum card counts.</p>
        <div className="filters"><label>Native type or representative card<input value={query} onChange={e => {setQuery(e.target.value); setPage(0);}}/></label>
          <label>Support state<select value={state} onChange={e => {setState(e.target.value); setPage(0);}}><option value="ALL">All states</option>{Object.keys(mechanics.counts).map(s => <option key={s}>{s}</option>)}</select></label></div>
        {loadingRecords && <p role="status">Loading existing audit records…</p>}{recordError && <><p role="alert">{recordError}</p><button onClick={() => void inspectRecords()}>Retry records</button></>}
        <p>{rows.length} groups match; showing {rows.length ? page * 20 + 1 : 0}–{Math.min((page + 1) * 20, rows.length)}.</p>
        {rows.slice(page * 20,(page + 1) * 20).map(g => <details className="progress-record" key={g.fingerprint}><summary>{g.nativeType} · {human(g.supportStatus)} · {number(g.occurrences)} nodes / {number(g.distinctCards)} cards</summary>
          <ul>{g.reasons.map(r => <li key={r}>{r}</li>)}</ul>{g.representatives.map(r => <div key={`${r.card}:${r.face}:${r.path}`}><strong>{r.card}</strong><p><code>{r.face} · {r.path}</code></p>{r.sourceReferences.filter(s => s.startsWith('https://')).map(s => <p key={s}><a href={s} target="_blank" rel="noreferrer">Native source at measured revision</a></p>)}</div>)}
        </details>)}
        <div className="actions"><button disabled={page === 0} onClick={() => setPage(page - 1)}>Previous groups</button><button disabled={(page + 1) * 20 >= rows.length} onClick={() => setPage(page + 1)}>Next groups</button></div>
        <a href="/api/v1/progress/mechanics" target="_blank" rel="noreferrer">Complete existing grouped audit</a> · <a href="/api/v1/progress/instructions" target="_blank" rel="noreferrer">Every extractor support record</a>
      </details>
    </section>

    <section className="progress-panel" id="assurance"><p className="eyebrow">03 / TRUST</p><h3>How much can we trust?</h3>
      <div className="progress-trust"><div><h4>Translation fidelity</h4><p><strong>Not measured at catalog scale.</strong> {number(assurance.notMeasured)} native definitions are outside the extension ledger and receive no reviewed/failed classification.</p>
        <p>Within {assurance.records.length} extension scopes: {assurance.current} evidence-current, {assurance.stale} stale, {assurance.missing} without evidence. These states are disjoint.</p>
        <Counts caption="Source fidelity · current extension evidence only" counts={assurance.fidelity}/></div>
        <div id="execution"><h4>Engine execution</h4><p>{data.execution}</p><SourceLink path="docs/SOURCE_SACRIFICE_DISCOVERY.md"/></div>
        <div><h4>Independent qualification</h4><p>The existing gate evaluates {assurance.records.length} extension scopes. {number(assurance.notMeasured)} other definitions are not measured under this process.</p>
          <Counts caption="Qualification gate · extension scopes only" counts={assurance.qualification}/><p>Zero qualified scopes, when shown, is a measured gate result within this ledger. It is not a catalog-wide rejection.</p></div>
      </div>
      <details><summary>Inspect source review, staleness, material concerns and exact approval records</summary>
        <p>“Current” uses the existing identity, definition and engine-implementation compatibility gate. Recorded engine execution keeps its original revision; compatibility does not turn it into a fresh run.</p>
        {assurance.records.map(r => <details className="progress-record" key={r.card}><summary>{r.card} · {r.qualification} · {r.evidenceCurrent ? 'evidence current' : 'missing or stale evidence'}</summary><p>{r.sourceFidelity} · {r.engineExecution}</p><ul>{r.reasons.map(s => <li key={s}>{s}</li>)}</ul><JsonDetails label="Exact source / requirements / tests / engine / concern and approval bundle" value={r.evidence}/></details>)}
      </details>
    </section>

    <section className="progress-panel" id="history"><p className="eyebrow">04 / RESPONSIBLE COMPOUNDING GAINS</p><h3>Did a shared improvement help more cards?</h3>
      {historyError && <p role="alert">Historical baseline unavailable: {historyError}. No trend is inferred.</p>}
      {!history && !historyError && <p role="status">Loading retained comparison…</p>}
      {history && <><p className="progress-badge">Historical recorded comparison · not current-engine execution</p>
        <p>{history.commander} · graveyard-state objective · same {number(history.population.length)} eligible physical cards, with no search filter. Both controlled comparisons use engine <code>{history.engine}</code>. The identical intermediate report digest connects these stages. No current trend line is inferred.</p>
        <div className="progress-table"><table><caption>Like-for-like historical stages · structural conditional potential</caption><thead><tr><th>Stage</th><th>Matching pathways</th><th>Matching cards</th><th>Unknown pathways</th><th>Nonmatch pathways</th><th>Unsupported records</th></tr></thead><tbody>
          {history.stages.map(s => <tr key={s.label}><th scope="row">{s.label}</th><td>{s.matches}</td><td>{s.matchingCards}</td><td>{s.unknowns}</td><td>{s.nonmatches}</td><td>{number(s.unsupported)}</td></tr>)}
        </tbody></table></div>
        <p>Source-card discard gained nine pathways across eight newly matching cards. Source-bound sacrifice gained 93 pathways across 88 newly matching cards and resolved 223 unknown pathways as justified nonmatches. Neither comparison lost a prior match or removed a pathway. These are bounded structural observations, not independent source verification of every affected card. Corpus-wide false positives and translation concerns introduced by these corrections were not independently measured; no error-rate claim is made.</p>
        <details><summary>Inspect gains, negative cases, regressions and comparison provenance</summary>
          <label>Reusable correction<select value={selectedComparison} onChange={e => setSelectedComparison(Number(e.target.value))}><option value={0}>Source-card discard</option><option value={1}>Source-bound sacrifice</option></select></label>
          {comparison && <><p>{comparison.newMatchingPathways} new matching pathways · {comparison.newMatchingCards.length} newly matching cards · {comparison.resolvedNonmatches} unknown-to-nonmatch · {comparison.lostMatches.length} lost matches · {comparison.newUnknowns} new unknowns · {comparison.removedPathways.length} removed pathways.</p>
            <label>Filter changed card<input value={historyQuery} onChange={e => setHistoryQuery(e.target.value)}/></label><p>{historyRows.length} changed verdicts; showing first 30.</p>
            {historyRows.slice(0,30).map((r,i) => <details className="progress-record" key={i}><summary>{r.card} · {r.before} → {r.after}</summary><p><code>{r.face} · {r.path}</code></p><ul>{r.prerequisites.map((p,i) => <li key={i}>{p}</li>)}</ul><JsonDetails label="Exact historical decisions and source references" value={r}/></details>)}
          </>}
          <p>{history.comparisonMethod}</p><JsonDetails label="Historical application revisions, source report digests and stage counts" value={history.stages}/><JsonDetails label="Historical eligibility counts and exclusions" value={history.exclusions}/>
          <a href="/api/v1/progress/history" target="_blank" rel="noreferrer">Complete retained comparison, eligible population and changed records</a>
          <p><SourceLink path="docs/SOURCE_DISCARD_DISCOVERY.md"/> · <SourceLink path="docs/SOURCE_SACRIFICE_DISCOVERY.md"/></p>
        </details></>}
    </section>

    <section className="progress-panel progress-obstacles" id="obstacles"><p className="eyebrow">05 / RESEARCH PRIORITIES</p><h3>What is holding us back?</h3>
      <p>Ordered by foundational dependencies and measured evidence gaps, then capability reach. This is an authored research judgment, not an automatic severity score. Impacts overlap.</p>
      <ol>{data.obstacles.map(o => <li key={o.id}><h4>{o.problem}</h4><p>{o.impact}</p><details><summary>Why it matters and a bounded investigation</summary>
        <p>{o.why}</p><p><strong>Example:</strong> {o.examples}</p><p><strong>Promising investigation:</strong> {o.investigation}</p><p className="hint">Evidence status: {o.evidenceStatus}</p><a href={`#${o.records}`}>Inspect supporting evidence</a>
      </details></li>)}</ol>
    </section>
    <section className="progress-panel" id="capabilities"><p className="eyebrow">06 / DISTANCE TO CONTEXTUAL DECKBUILDING</p><h3>Which capabilities remain?</h3>
      <p>Card coverage is one foundation. These capabilities do not have a defensible combined completion percentage.</p>
      <div className="progress-table"><table><caption>Grounded capability states · current application</caption><thead><tr><th>Capability</th><th>State</th><th>Boundary</th></tr></thead><tbody>{data.capabilities.map(c => <tr key={c.capability}><th scope="row">{c.capability}</th><td>{c.state}</td><td>{c.limit}</td></tr>)}</tbody></table></div>
    </section>
    <details className="progress-panel"><summary>Measurement definitions, revisions and reproducibility</summary>
      <p>Application <code>{data.applicationRevision}</code><br/>Engine <code>{data.argentumCommit}</code></p><p>All present-tense native counts use these packaged build revisions. Historical comparisons name separate source revisions. A change in catalog, engine or denominator requires a fresh controlled comparison; this page does not join unmatched baselines.</p>
      <p><a href="/api/v1/progress" target="_blank" rel="noreferrer">Inspect complete Kotlin dashboard report</a> · <SourceLink path="docs/RESEARCH_PROGRESS.md"/></p>
    </details>
  </article>;
}
