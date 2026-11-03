import {useEffect, useMemo, useRef, useState} from 'react';
import {createRoot} from 'react-dom/client';
import {api, type CardImage, type Evidence, type Finding, type Health, type CommanderView, type Objective, type Run} from './api';
import {CardPicture} from './CardPicture';
import {CardInspection} from './CardInspection';
import {CommanderPanel} from './CommanderPanel';
import {DiscoveryCoverage} from './DiscoveryCoverage';
import './style.css';

const categories = [
  ['IDENTIFIED', 'Identified mechanics', 'Cards with an identified way to contribute to your objective.'],
  ['UNRESOLVED', 'Needs interpretation', 'Possible connections the application could not establish. Inspect the card to see what remains unresolved.'],
  ['ALL', 'All findings', 'Every card returned by this search, including unresolved and excluded findings.'],
] as const;
const diagnostics = [
  ['MATCH', 'Has accepted checks'], ['UNKNOWN', 'Has unresolved checks'],
  ['NO_MATCH', 'Has nonqualifying checks'], ['UNSUPPORTED', 'Has unread instructions'],
] as const;
const inCategory = (card: Finding, category: string) => category === 'ALL' ||
  (category === 'IDENTIFIED' || category === 'UNRESOLVED' ? card.browsingState === category :
    category === 'UNSUPPORTED' ? card.unsupportedRecords > 0 : card.pathways[category] > 0);
function Json({value}: {value: unknown}) { return <pre>{JSON.stringify(value, null, 2)}</pre>; }
function RawDetails({label, value}: {label: string; value: unknown}) {
  const [expanded, setExpanded] = useState(false);
  return <details onToggle={e => setExpanded(e.currentTarget.open)}><summary>{label}</summary>{expanded && <Json value={value}/>}</details>;
}

export function App() {
  const [objectives, setObjectives] = useState<Objective[]>([]);
  const [health, setHealth] = useState<Health | null>(null);
  const [commander, setCommander] = useState('Judith, the Scourge Diva');
  const [objective, setObjective] = useState('own-nontoken-creature-deaths');
  const [commanderView, setCommanderView] = useState<CommanderView | null>(null);
  const [commanderOpen, setCommanderOpen] = useState(false);
  const [run, setRun] = useState<Run | null>(null);
  const [coverageRun, setCoverageRun] = useState<string>();
  const coverageOpen = !!run && coverageRun === run.runId;
  const [selected, setSelected] = useState<number | null>(null);
  const [detail, setDetail] = useState<Evidence | null>(null);
  const [detailError, setDetailError] = useState('');
  const [detailLoading, setDetailLoading] = useState(false);
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [category, setCategory] = useState('IDENTIFIED');
  const [mechanic, setMechanic] = useState('ALL');
  const [name, setName] = useState('');
  const [page, setPage] = useState(0);
  const [images, setImages] = useState<Record<string, CardImage>>({});
  const detailRef = useRef<HTMLElement>(null);
  const pageSize = 24;
  const runId = run?.runId;

  useEffect(() => {
    Promise.all([api<Objective[]>('objectives'), api<Health>('health')])
      .then(([definitions, ready]) => { setObjectives(definitions); setHealth(ready); })
      .catch(() => setError('Cannot connect to the local application. Check its terminal and reload this page.'));
  }, []);

  const filtered = useMemo(() => run?.cards.filter(c => inCategory(c, category) &&
    c.card.toLocaleLowerCase().includes(name.toLocaleLowerCase()) &&
    (mechanic === 'ALL' || c.mechanics?.includes(mechanic))) ?? [], [run, category, name, mechanic]);
  const visible = filtered.slice(page * pageSize, (page + 1) * pageSize);
  const pages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const visibleIds = visible.map(c => c.index).join(',');
  const selectedCard = visible.find(c => c.index === selected);
  useEffect(() => {
    if (!visible.some(c => c.index === selected)) setSelected(visible[0]?.index ?? null);
  }, [visibleIds, selected]);

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

  useEffect(() => {
    if (!runId || !visibleIds) return;
    const controller = new AbortController();
    api<CardImage[]>(`runs/${runId}/images?indices=${visibleIds}`, undefined, controller.signal)
      .then(rows => { if (!controller.signal.aborted) setImages(old => ({...old,
        ...Object.fromEntries(rows.map(image => [`${runId}:${image.index}`, image]))})); })
      .catch(() => { if (!controller.signal.aborted) setImages(old => ({...old,
        ...Object.fromEntries(visible.map(c => [`${runId}:${c.index}`, {index: c.index, card: c.card,
          status: 'UNAVAILABLE', reason: 'Image metadata could not be loaded. Discovery evidence is still available.', faces: []} as unknown as CardImage]))})); });
    return () => controller.abort();
  }, [runId, visibleIds]);

  function changed() {
    setCommanderOpen(false); setCommanderView(null); setRun(null); setSelected(null); setDetail(null); setImages({});
    setError(''); setDetailError(''); setName(''); setMechanic('ALL'); setPage(0);
  }
  async function research(discovery: boolean) {
    setBusy(discovery ? 'Finding cards…' : 'Loading commander…');
    if (discovery) changed();
    try {
      const view = await api<CommanderView>('commander-view', {commander, objective}); setCommanderView(view);
      const inspected = view.inspection;
      if (!inspected.selectable) setCommanderOpen(true);
      if (discovery && inspected.selectable) {
        const fresh = await api<Run>('discover', {commander, objective}); setRun(fresh);
        setCategory(fresh.cards.some(c => c.browsingState === 'IDENTIFIED') ? 'IDENTIFIED' :
          fresh.cards.some(c => c.browsingState === 'UNRESOLVED') ? 'UNRESOLVED' : 'ALL');
      }
    } catch (e) { setError(e instanceof Error ? e.message : String(e)); }
    finally { setBusy(''); }
  }
  const shownDetail = detail?.card === selectedCard?.card ? detail : null;
  const imageFor = (index: number) => images[`${runId}:${index}`];
  const mechanics = [...new Set(run?.cards.flatMap(c => c.mechanics ?? []) ?? [])].sort();

  return <>
    <header><h1>Card Discovery</h1>{!health && <span>Connecting…</span>}</header>
    <main>
      <section className="controls" aria-label="Research controls">
        <form onSubmit={e => { e.preventDefault(); void research(true); }}>
          <label>Commander<input list="commanders" value={commander}
            onChange={e => { changed(); setCommander(e.target.value); }} disabled={!!busy} required maxLength={200}/></label>
          <datalist id="commanders"><option>Judith, the Scourge Diva</option><option>Oviya Pashiri, Sage Lifecrafter</option><option>Krenko, Mob Boss</option></datalist>
          <label>Objective<select value={objective} onChange={e => { changed(); setObjective(e.target.value); }} disabled={!!busy || !objectives.length}>
            {objectives.map(o => <option value={o.id} key={o.id}>{o.label}</option>)}
          </select></label>
          <div className="actions"><button className="primary" disabled={!!busy || !objectives.length || !commander.trim()}>Find cards</button></div>
        </form>
        <div className="selection-info">
          <details open={commanderOpen} onToggle={e => {
            const open = e.currentTarget.open; setCommanderOpen(open);
            if (open && !commanderView && !busy && commander.trim()) void research(false);
          }}><summary>About commander</summary>
            {commanderView && <CommanderPanel view={commanderView} revision={run?.applicationRevision ?? health?.applicationRevision}/>}
          </details>
          <details className="objective-help"><summary>About objective</summary>
            <ul>{objectives.find(o => o.id === objective)?.capabilities?.map((item, i) => <li key={i}>{item}</li>)}</ul>
          </details>
        </div>
        <div aria-live="polite">
          {busy && <p role="status">{busy}</p>}
          {error && <p role="alert" className="warning">{error}</p>}
        </div>
      </section>
      {run && <>
        <section className="run-summary" aria-label="Completed discovery">
          <div><h2>Discovery results</h2><p>{objectives.find(o => o.id === run.objective)?.label} · {run.commander}</p></div>
          <p className="hint">Searched {run.counts.mechanical_input_cards_after_conventional_search?.toLocaleString()} available cards · {(run.durationMs / 1000).toFixed(1)} s</p>
        </section>
        {run.commander === "Judith, the Scourge Diva" && run.objective === "own-nontoken-creature-deaths" &&
          <div className="actions"><button aria-expanded={coverageOpen} onClick={() => setCoverageRun(coverageOpen ? undefined : run.runId)}>
            {coverageOpen ? "Back to discovery results" : "Investigate Discovery Coverage"}
          </button></div>}
        {coverageOpen ? <DiscoveryCoverage key={run.runId} run={run}/> : <>
        <nav className="categories" aria-label="Finding categories">
          {categories.map(([key, label, description]) => <button key={key} aria-pressed={category === key} title={description} aria-description={description}
            onClick={() => { setCategory(key); setPage(0); }}><span className="category-count">{run.cards.filter(c => inCategory(c, key)).length.toLocaleString()}</span>{label}<small>cards</small></button>)}
        </nav>
        <details className="diagnostic-views"><summary>Research and diagnostic views</summary>
          <p className="hint">These counts overlap and count distinct cards, not abilities. Identified mechanics remain useful even when another part of a card is excluded or unread. Do not add these counts together.</p>
          <nav className="diagnostics" aria-label="Diagnostic finding categories">
            {diagnostics.map(([key, label]) => <button key={key} aria-pressed={category === key} onClick={() => {setCategory(key); setPage(0);}}>{label} · {run.cards.filter(c => inCategory(c, key)).length.toLocaleString()} cards</button>)}
          </nav>
        </details>
        <div className="workspace">
          <section aria-label="Discovery results">
            <h2>Explore cards</h2>
            <div className="filters"><label>Card name<input value={name} onChange={e => { setName(e.target.value); setPage(0); }}/></label>
              <label>Mechanic<select value={mechanic} onChange={e => { setMechanic(e.target.value); setPage(0); }}>
                <option value="ALL">All mechanics</option>{mechanics.map(m => <option key={m}>{m}</option>)}
              </select></label></div>
            <p className="hint">{filtered.length.toLocaleString()} cards in this view · name order · page {page + 1} of {pages}</p>
            <div className="results">
              {!visible.length && <p>No cards in this view.</p>}
              {visible.map(card => <button key={card.index} className={`card ${selected === card.index ? 'selected' : ''}`}
                onClick={event => { setSelected(card.index); if (event.detail === 0) requestAnimationFrame(() => detailRef.current?.focus()); }} aria-pressed={selected === card.index} aria-controls="card-inspection" aria-label={card.card}>
                <CardPicture compact image={imageFor(card.index)}/>
                <div><strong>{card.card}</strong>
                  {card.browsingState === 'UNRESOLVED' && <small>Relationship needs interpretation</small>}
                  {card.recordedConcern && <small className="concern-marker">Recorded concern — inspect evidence</small>}
                  {!!card.mechanics?.length && <small>{card.mechanics.join(' · ')}</small>}
                </div>
              </button>)}
            </div>
            <div className="actions pager"><button onClick={() => setPage(page - 1)} disabled={!page}>Previous cards</button><button onClick={() => setPage(page + 1)} disabled={page + 1 >= pages}>Next cards</button></div>
          </section>
          <section id="card-inspection" ref={detailRef} className="detail" aria-label="Result inspection" tabIndex={-1} aria-live="polite">
            <h2>{selectedCard?.card ?? 'Select a finding'}</h2>
            {detailLoading && <p role="status">Loading card evidence…</p>}
            {detailError && <p role="alert" className="warning">{detailError}</p>}
            {shownDetail && <CardInspection key={shownDetail.card} evidence={shownDetail} image={imageFor(selected!)} revision={run.applicationRevision}/>}

          </section>
        </div>
        <details className="provenance"><summary>Search scope, counts, exclusions and source revisions</summary>
          {objectives.find(o => o.id === run.objective)?.coverageNote && <p>{objectives.find(o => o.id === run.objective)?.coverageNote}</p>}
          <p>The search covers the authored catalog and application extensions, not every Commander-legal Magic card. {run.counts.format_legality_unknown_cards} cards with unresolved format legality were excluded before checking mechanics.</p>
          <p>The findings index includes {run.cards.length} distinct physical cards. Cards without an emitted check or unread instruction are absent from this index; that absence is not a negative result. Categories above overlap; counts below are pathway checks and unread instruction records, not additional cards.</p>
          <table><caption>Counts by record type</caption><thead><tr><th>Record</th><th>Count</th></tr></thead><tbody>
            <tr><td>Pathway checks that may help</td><td>{run.pathwayCounts.MATCH}</td></tr><tr><td>Unresolved pathway checks</td><td>{run.pathwayCounts.UNKNOWN}</td></tr>
            <tr><td>Pathway checks that did not meet the objective</td><td>{run.pathwayCounts.NO_MATCH}</td></tr><tr><td>Unread instruction records</td><td>{run.counts.unsupported_structure_records}</td></tr>
          </tbody></table>
          <p>Application: <code>{run.applicationRevision}</code><br/>Engine: <code>{run.argentumCommit}</code> · {run.engineMode}<br/>Objective: <code>{run.objective}</code><br/>Allowed identity: {run.allowedIdentity.join(', ')} · {new Date(run.startedAt).toLocaleString()}</p>
          {run.applicationRevision.endsWith('+dirty') && <p className="warning">This build includes uncommitted source changes.</p>}
          <ul>{run.limitations.map((l, i) => <li key={i}>{l}</li>)}</ul><RawDetails label="Exact population counts" value={run.counts}/>
          <p>Only the latest search is retained. A new discovery replaces it.</p><a href={`/api/v1/runs/${run.runId}/report`} download>Download complete live evidence, including all eligibility records</a>
        </details>
        </>}
      </>}
      <details className="shared-help"><summary>About the evidence checks</summary>
        <dl className="evidence-fields">
          <div><dt>Discovery</dt><dd>Which mechanics and conditions the search identified for your objective.</dd></div>
          <div><dt>Rules comparison</dt><dd>Recorded comparisons between programmed behavior and Magic rules sources.</dd></div>
          <div><dt>Engine tests</dt><dd>Recorded outcomes for particular test scenarios.</dd></div>
          <div><dt>Independent verification</dt><dd>A separate review of the interpretation and supporting evidence.</dd></div>
        </dl>
        <p>These checks cover their stated scope, rather than certifying every interaction on a card. Missing verification does not block discovery or require you to review the catalog. Earlier results retain their original engine version.</p>
      </details>
      <footer>Unofficial Magic: The Gathering research · © Wizards of the Coast · Card metadata and unaltered images from <a href="https://scryfall.com" target="_blank" rel="noreferrer">Scryfall</a></footer>
    </main>
  </>;
}
createRoot(document.getElementById('root')!).render(<App/>);
