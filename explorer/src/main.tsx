import {useEffect, useState} from 'react';
import {createRoot} from 'react-dom/client';
import './style.css';

type RecordData = Record<string, unknown>;
type Inspection = {
  requestedName: string; selectable: boolean; colorIdentity: string[]; availability: string;
  blockers: {code: string; reason: string; nextAction: string}[]; knownLimitations: string[];
  mechanicalCaveat: string; constructionScope: string; argentumCommit: string;
};
type Objective = {id: string; label: string; explanation: string};
type Finding = {
  index: number; card: string; pathways: Record<string, number>; branches: string[];
  unsupportedRecords: number; partialPathways: number; reason: string | null;
};
type Run = {
  schemaVersion: number; execution: string; runId: string; startedAt: string; durationMs: number;
  applicationRevision: string; argentumCommit: string; engineMode: string; commander: string;
  objective: string; allowedIdentity: string[]; counts: Record<string, number>;
  pathwayCounts: Record<string, number>; limitations: string[]; cards: Finding[];
};
type Row = {
  card: string; face: string; actionId: string; abilityPath: string; branch: string;
  operation: string; verdict: string; interpretation: string; prerequisites: string[];
  decisions: {stage: string; verdict: string; reason: string}[];
  sourceReferences: string[]; circumstances: RecordData | null;
};
type Assurance = {qualification: string; sourceFidelity: string; engineExecution: string; reasons: string[]};
type Evidence = {
  runId: string; card: string; authoredCard: {manaCost: string; typeLine: string; text: string} | null;
  results: Row[]; actions: RecordData[]; facts: RecordData[]; unsupported: {reason: string}[];
  structuralSupport: RecordData[]; eligibility: RecordData[]; translationAssurance: Assurance[];
  explanations: {summary: string; requirements: string[]}[];
};
type Health = {applicationRevision: string; argentumCommit: string; engineMode: string};

async function api<T>(path: string, body?: unknown, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`/api/v1/${path}`, {
    ...(body === undefined ? {} : {
      method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body),
    }),
    signal: signal ?? AbortSignal.timeout(135_000),
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data.message ?? `Request failed (${response.status})`);
  return data;
}
const verdictLabel = (v: string) => v === 'MATCH' ? 'Conditional potential' :
  v === 'UNKNOWN' ? 'Unresolved' : 'Pathway does not match';
const branchLabel: Record<string, string> = {
  DIRECT_DEATH_GENERATOR: 'Direct death generation',
  DEATH_ENHANCED_REMOVAL: 'Death-enhanced removal',
  REANIMATE_OWN_CREATURE: 'Return our creature card',
  CREATE_CREATURE_TOKEN: 'Create a creature token',
};
const categories = [
  ['MATCH', 'Conditional potential'], ['UNKNOWN', 'Unresolved pathways'],
  ['NO_MATCH', 'Nonmatching pathways'], ['UNSUPPORTED', 'Unsupported structures'], ['ALL', 'All findings'],
] as const;
const inCategory = (card: Finding, category: string) =>
  category === 'ALL' || (category === 'UNSUPPORTED' ? card.unsupportedRecords > 0 : card.pathways[category] > 0);

function Json({value}: {value: unknown}) { return <pre>{JSON.stringify(value, null, 2)}</pre>; }
function RawDetails({label, value}: {label: string; value: unknown}) {
  const [expanded, setExpanded] = useState(false);
  return <details onToggle={e => setExpanded(e.currentTarget.open)}>
    <summary>{label}</summary>{expanded && <Json value={value}/>}
  </details>;
}

export function App() {
  const [objectives, setObjectives] = useState<Objective[]>([]);
  const [health, setHealth] = useState<Health | null>(null);
  const [commander, setCommander] = useState('Judith, the Scourge Diva');
  const [objective, setObjective] = useState('own-nontoken-creature-deaths');
  const [inspection, setInspection] = useState<Inspection | null>(null);
  const [run, setRun] = useState<Run | null>(null);
  const [selected, setSelected] = useState<number | null>(null);
  const [detail, setDetail] = useState<Evidence | null>(null);
  const [detailError, setDetailError] = useState('');
  const [detailLoading, setDetailLoading] = useState(false);
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [category, setCategory] = useState('MATCH');
  const [name, setName] = useState('');
  const [page, setPage] = useState(0);
  const pageSize = 40;

  useEffect(() => {
    Promise.all([api<Objective[]>('objectives'), api<Health>('health')])
      .then(([definitions, ready]) => { setObjectives(definitions); setHealth(ready); })
      .catch(() => setError('Cannot connect to the local application. Check its terminal and reload this page.'));
  }, []);

  const runId = run?.runId;
  useEffect(() => {
    setDetail(null); setDetailError('');
    if (!runId || selected === null) { setDetailLoading(false); return; }
    const controller = new AbortController();
    setDetailLoading(true);
    api<Evidence>(`runs/${runId}/cards/${selected}`, undefined, controller.signal)
      .then(value => { if (!controller.signal.aborted) setDetail(value); })
      .catch(e => { if (!controller.signal.aborted) setDetailError(e.message); })
      .finally(() => { if (!controller.signal.aborted) setDetailLoading(false); });
    return () => controller.abort();
  }, [runId, selected]);

  function changed() {
    setInspection(null); setRun(null); setSelected(null); setDetail(null);
    setError(''); setDetailError(''); setName(''); setPage(0);
  }
  async function research(discovery: boolean) {
    setBusy(discovery ? 'Running discovery…' : 'Inspecting commander…');
    changed();
    try {
      const inspected = await api<Inspection>('commander', {commander});
      setInspection(inspected);
      if (discovery && inspected.selectable) {
        const fresh = await api<Run>('discover', {commander, objective});
        setRun(fresh);
        setCategory(fresh.cards.some(c => c.pathways.MATCH > 0) ? 'MATCH' :
          fresh.cards.some(c => c.pathways.UNKNOWN > 0) ? 'UNKNOWN' :
          fresh.cards.some(c => c.unsupportedRecords > 0) ? 'UNSUPPORTED' : 'ALL');
        setSelected((fresh.cards.find(c => c.pathways.MATCH > 0) ?? fresh.cards[0])?.index ?? null);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally { setBusy(''); }
  }

  const filtered = run?.cards.filter(c =>
    inCategory(c, category) && c.card.toLocaleLowerCase().includes(name.toLocaleLowerCase())) ?? [];
  const visible = filtered.slice(page * pageSize, (page + 1) * pageSize);
  const pages = Math.max(1, Math.ceil(filtered.length / pageSize));

  return <>
    <header><a href="/">MTG / RESEARCH LAB</a><span>Explorer · M1 · {health ? 'Local application' : 'Connecting…'}</span></header>
    <main>
      <div className="eyebrow">COMMANDER DISCOVERY</div>
      <h1>Find a possibility.<br/><em>Understand its conditions.</em></h1>
      <p className="intro">Explore authored card mechanics in your Commander’s colors. Follow a finding from its mechanical pathway to its evidence and boundaries.</p>
      <section className="controls" aria-label="Research controls">
        <form onSubmit={e => { e.preventDefault(); void research(true); }}>
          <label>Commander’s exact name
            <input list="commanders" value={commander} onChange={e => { changed(); setCommander(e.target.value); }}
              disabled={!!busy} required maxLength={200}/>
          </label>
          <datalist id="commanders">
            <option>Judith, the Scourge Diva</option><option>Oviya Pashiri, Sage Lifecrafter</option><option>Krenko, Mob Boss</option>
          </datalist>
          <label>Research objective
            <select value={objective} onChange={e => { changed(); setObjective(e.target.value); }} disabled={!!busy || !objectives.length}>
              {objectives.map(o => <option value={o.id} key={o.id}>{o.label}</option>)}
            </select>
          </label>
          <p className="hint">{objectives.find(o => o.id === objective)?.explanation}</p>
          <div className="actions">
            <button type="button" onClick={() => void research(false)} disabled={!!busy || !commander.trim()}>Inspect commander</button>
            <button className="primary" disabled={!!busy || !objectives.length || !commander.trim()}>Run discovery →</button>
          </div>
        </form>
        <aside aria-live="polite">
          <h2>Your research question</h2>
          {busy ? <p role="status">{busy} The first query also loads source references. Please wait.</p> :
            !inspection ? <p>Choose a commander and an objective, then run a fresh query. Exact spelling and case matter.</p> : <>
              <p><b>{inspection.requestedName}</b></p>
              <p>{inspection.selectable ? 'Available for single-commander discovery' : 'Selection blocked'} · {inspection.colorIdentity.join(' / ') || 'No resolved colors'}</p>
              <p className="hint">{inspection.availability.replaceAll('_', ' ')}</p>
              {inspection.blockers.map((b, i) => <p className="warning" key={i}>
                <b>{b.code.replaceAll('_', ' ')}</b><br/>{b.reason}<br/>{b.nextAction}
              </p>)}
              {inspection.knownLimitations.map((l, i) => <p key={i}>{l}</p>)}
              <details><summary>Commander eligibility and review</summary>
                <p>{inspection.constructionScope}</p><p>{inspection.mechanicalCaveat}</p>
                <Json value={inspection}/>
              </details>
            </>}
          {error && <p role="alert" className="warning">{error}</p>}
        </aside>
      </section>
      <p className="notice">A structural match is a conditional possibility. It does not prove successful resolution, certify the card’s translation, or recommend a deck inclusion. No emitted pathway is not a proven nonmatch.</p>

      {run && <>
        <section className="run-summary" aria-label="Completed discovery">
          <div className="eyebrow">FRESH KOTLIN DISCOVERY · COMPLETED</div>
          <h2>{run.commander}</h2>
          <p>{objectives.find(o => o.id === run.objective)?.label} · {(run.durationMs / 1000).toFixed(1)} s discovery · {new Date(run.startedAt).toLocaleString()}</p>
          <p>{run.pathwayCounts.MATCH} conditional-match pathways · {run.pathwayCounts.UNKNOWN} unresolved pathways · {run.pathwayCounts.NO_MATCH} nonmatching pathways · {run.counts.unsupported_structure_records} unsupported structure records</p>
          <p className="hint">Scope: {run.counts.mechanical_input_cards_after_conventional_search} eligible physical cards from this engine’s authored catalog and application extensions.
            This is not the entire Commander card pool. {run.counts.format_legality_unknown_cards} cards with unresolved format legality are excluded before mechanical discovery.
            The findings index contains {run.cards.length} cards with emitted pathways or unsupported records.</p>
        </section>

        <nav className="categories" aria-label="Finding categories">
          {categories.map(([key, label]) => <button key={key} aria-pressed={category === key}
            onClick={() => { setCategory(key); setPage(0); }}>
            {label}<span>{run.cards.filter(c => inCategory(c, key)).length} cards</span>
          </button>)}
        </nav>
        <p className="hint">Cards may appear in several categories. Nonmatching pathways do not establish that the whole card cannot contribute.</p>
        <div className="workspace">
          <section aria-label="Discovery results">
            <h2>Explore findings</h2>
            <label>Find within these findings
              <input value={name} onChange={e => { setName(e.target.value); setPage(0); }} placeholder="Card name"/>
            </label>
            <p className="hint">{filtered.length} cards in this view · alphabetical order · page {page + 1} of {pages}</p>
            <div className="results">
              {visible.length === 0 && <p>No findings in this view. This is not proof that these mechanics are absent.</p>}
              {visible.map(card => <button key={card.index} className={`card ${selected === card.index ? 'selected' : ''}`}
                onClick={() => setSelected(card.index)} aria-pressed={selected === card.index}>
                <strong>{card.card}</strong>
                <span>{card.pathways.MATCH} conditional · {card.pathways.UNKNOWN} unresolved · {card.pathways.NO_MATCH} nonmatching pathways</span>
                <small>{card.branches.map(b => branchLabel[b] ?? b).join(' · ') || 'No objective pathway emitted'}</small>
                {card.unsupportedRecords > 0 && <small>{card.unsupportedRecords} unsupported structure records</small>}
                {card.partialPathways > 0 && <small>Partial action evidence retained</small>}
                {card.reason && <p className="preview">{card.reason}</p>}
              </button>)}
            </div>
            <div className="actions pager">
              <button onClick={() => setPage(page - 1)} disabled={page === 0}>Previous cards</button>
              <button onClick={() => setPage(page + 1)} disabled={page + 1 >= pages}>Next cards</button>
            </div>
          </section>

          <section className="detail" aria-label="Result inspection" tabIndex={0} aria-live="polite">
            <div className="eyebrow">INSPECT THE EVIDENCE</div>
            {detailLoading && <p role="status">Loading card evidence…</p>}
            {detailError && <p role="alert" className="warning">{detailError}</p>}
            {!detail && !detailLoading && !detailError && <h2>Select a finding</h2>}
            {detail && <>
              <h2>{detail.card}</h2>
              {detail.authoredCard && <div className="authored">
                <p>{detail.authoredCard.manaCost} · {detail.authoredCard.typeLine}</p>
                <p className="card-text">{detail.authoredCard.text}</p>
                <small>Text from the authored native definition; not independent translation certification.</small>
              </div>}
              <p className="hint">No verified printing/face image association is loaded for this increment.</p>
              {detail.results.length === 0 && <p>No objective pathway emitted for this card. Its unsupported structures remain visible below.</p>}
              {detail.results.map((r, i) => <article key={i} className="pathway">
                <span className={`badge ${r.verdict}`}>{verdictLabel(r.verdict)}</span>
                <h3>{branchLabel[r.branch] ?? r.branch}</h3>
                <p>Native action: <b>{r.operation}</b> · {r.face}</p>
                <p className="hint">{r.interpretation.startsWith('PARTIAL') ?
                  'This recognized pathway belongs to a partly interpreted action. Inspect the unsupported structures as well.' :
                  'Structural evidence only; successful gameplay resolution is not established.'}</p>
                <h4>Why this pathway appeared</h4>
                <p>{detail.explanations[i]?.summary ?? 'Inspect the application decision records below.'}</p>
                <h4>What must be true</h4>
                <ul>{(detail.explanations[i]?.requirements ?? r.prerequisites).map((p, j) => <li key={j}>{p}</li>)}</ul>
                <RawDetails label="Exact timing, costs, branch prerequisites and application decisions"
                  value={{prerequisites: r.prerequisites, decisions: r.decisions}}/>
                {r.circumstances && <RawDetails label="Timing, resources and circumstances" value={r.circumstances}/>}
                <details><summary>Exact pathway and source evidence</summary>
                  <p>Path: <code>{r.abilityPath}</code></p>
                  {r.sourceReferences.map((s, j) => s.startsWith('https://github.com/') ?
                    <p key={j}><a href={s} target="_blank" rel="noreferrer">{s}</a></p> : <p key={j}>{s}</p>)}
                  <RawDetails label="Structured pathway record" value={r}/>
                </details>
              </article>)}
              <h3>Unsupported structures and limitations</h3>
              {detail.unsupported.map((u, i) => <p className="warning" key={i}>{u.reason}</p>)}
              {detail.unsupported.length === 0 && <p>No unsupported record emitted for this card; this is not whole-card certification.</p>}
              <h3>Translation assurance</h3>
              {detail.translationAssurance.length === 0 ?
                <p>No application translation-assurance record supplied. Inherited native definitions are not certified by discovery.</p> :
                detail.translationAssurance.map((a, i) => <div key={i}>
                  <p>{a.qualification} · Source fidelity: {a.sourceFidelity} · Recorded execution: {a.engineExecution}</p>
                  <ul>{a.reasons.map((reason, j) => <li key={j}>{reason}</li>)}</ul>
                  <RawDetails label="Assurance evidence and concern history" value={a}/>
                </div>)}
              <RawDetails label="Related native actions, facts, eligibility and structural support"
                value={{actions: detail.actions, facts: detail.facts, eligibility: detail.eligibility,
                  structuralSupport: detail.structuralSupport, unsupported: detail.unsupported}}/>
            </>}
          </section>
        </div>
        <details className="provenance"><summary>Run scope, revisions, exclusions and limitations</summary>
          <p>Application build: <code>{run.applicationRevision}</code><br/>
            Engine: <code>{run.argentumCommit}</code> · {run.engineMode}<br/>
            Objective: <code>{run.objective}</code><br/>Allowed identity: {run.allowedIdentity.join(', ')}</p>
          {run.applicationRevision.endsWith('+dirty') && <p className="warning">This build includes uncommitted application changes. The commit alone does not identify all its source.</p>}
          <ul>{run.limitations.map((l, i) => <li key={i}>{l}</li>)}</ul>
          <RawDetails label="Exact population counts" value={run.counts}/>
          <p>Only the latest run is retained by this local service. A new query replaces it.</p>
          <a href={`/api/v1/runs/${run.runId}/report`} download>Download complete live evidence, including all eligibility records</a>
        </details>
      </>}
      <footer>Local mechanical research · Magic: The Gathering © Wizards of the Coast · Unofficial project</footer>
    </main>
  </>;
}

createRoot(document.getElementById('root')!).render(<App/>);
