import {useEffect, useMemo, useRef, useState} from 'react';
import {createRoot} from 'react-dom/client';
import {api, type CardImage, type Evidence, type Finding, type Health, type Inspection, type Objective, type Run} from './api';
import {CardPicture} from './CardPicture';
import './style.css';

const categories = [
  ['MATCH', 'May help', 'At least one represented ability can contribute if its conditions hold.'],
  ['UNKNOWN', 'Needs investigation', 'At least one ability has an unresolved requirement.'],
  ['NO_MATCH', 'Abilities ruled out', 'At least one represented ability did not meet this objective. Other abilities may still help.'],
  ['UNSUPPORTED', 'Not fully understood', 'Some instructions could not be interpreted. That establishes neither a positive nor a negative result.'],
  ['ALL', 'All findings', 'Every card with an objective check or an instruction the application could not interpret.'],
] as const;
const inCategory = (card: Finding, category: string) => category === 'ALL' ||
  (category === 'UNSUPPORTED' ? card.unsupportedRecords > 0 : card.pathways[category] > 0);
const verdictLabel = (v: string) => v === 'MATCH' ? 'May help if conditions hold' : v === 'UNKNOWN' ? 'Needs investigation' : 'This ability did not meet the objective';
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
  const [inspection, setInspection] = useState<Inspection | null>(null);
  const [run, setRun] = useState<Run | null>(null);
  const [selected, setSelected] = useState<number | null>(null);
  const [detail, setDetail] = useState<Evidence | null>(null);
  const [detailError, setDetailError] = useState('');
  const [detailLoading, setDetailLoading] = useState(false);
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [category, setCategory] = useState('MATCH');
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
    setInspection(null); setRun(null); setSelected(null); setDetail(null); setImages({});
    setError(''); setDetailError(''); setName(''); setMechanic('ALL'); setPage(0);
  }
  async function research(discovery: boolean) {
    setBusy(discovery ? 'Running discovery…' : 'Inspecting commander…'); changed();
    try {
      const inspected = await api<Inspection>('commander', {commander}); setInspection(inspected);
      if (discovery && inspected.selectable) {
        const fresh = await api<Run>('discover', {commander, objective}); setRun(fresh);
        setCategory(fresh.cards.some(c => c.pathways.MATCH > 0) ? 'MATCH' :
          fresh.cards.some(c => c.pathways.UNKNOWN > 0) ? 'UNKNOWN' : 'ALL');
      }
    } catch (e) { setError(e instanceof Error ? e.message : String(e)); }
    finally { setBusy(''); }
  }
  const shownDetail = detail?.card === selectedCard?.card ? detail : null;
  const imageFor = (index: number) => images[`${runId}:${index}`];
  const mechanics = [...new Set(run?.cards.flatMap(c => c.mechanics ?? []) ?? [])].sort();

  return <>
    <header><h1>Commander Discovery</h1><span>{health ? 'Local research' : 'Connecting…'}</span></header>
    <main>
      <section className="controls" aria-label="Research controls">
        <form onSubmit={e => { e.preventDefault(); void research(true); }}>
          <label>Commander’s exact name<input list="commanders" value={commander}
            onChange={e => { changed(); setCommander(e.target.value); }} disabled={!!busy} required maxLength={200}/></label>
          <datalist id="commanders"><option>Judith, the Scourge Diva</option><option>Oviya Pashiri, Sage Lifecrafter</option><option>Krenko, Mob Boss</option></datalist>
          <label>Research objective<select value={objective} onChange={e => { changed(); setObjective(e.target.value); }} disabled={!!busy || !objectives.length}>
            {objectives.map(o => <option value={o.id} key={o.id}>{o.label}</option>)}
          </select></label>
          <div className="actions"><button type="button" onClick={() => void research(false)} disabled={!!busy || !commander.trim()}>Inspect commander</button>
            <button className="primary" disabled={!!busy || !objectives.length || !commander.trim()}>Run discovery</button></div>
        </form>
        <p className="hint objective-hint">{objectives.find(o => o.id === objective)?.explanation}</p>
        <div aria-live="polite">
          {busy && <p role="status">{busy} Please wait for the card search to finish.</p>}
          {inspection && <><p className="inspection-status"><b>{inspection.requestedName}</b> · {inspection.selectable ? 'Available for single-commander discovery' : 'Selection blocked'} · {inspection.colorIdentity.join(' / ') || 'No resolved colors'}</p>
            {inspection.blockers.map((b, i) => <p className="warning" key={i}>{b.reason}<br/>{b.nextAction}</p>)}
            <details><summary>Commander eligibility and review</summary><p>{inspection.constructionScope}</p><p>{inspection.mechanicalCaveat}</p>
              {inspection.knownLimitations.map((l, i) => <p key={i}>{l}</p>)}<Json value={inspection}/></details></>}
          {error && <p role="alert" className="warning">{error}</p>}
        </div>
      </section>
      {!run && <p className="hint">Choose a commander and a question, then run discovery to explore the cards in its colors. Findings are research leads, not deck recommendations.</p>}
      {run && <>
        <section className="run-summary" aria-label="Completed discovery">
          <div><h2>{run.cards.length.toLocaleString()} card findings</h2><p>{objectives.find(o => o.id === run.objective)?.label} · {run.commander}</p></div>
          <p className="hint">Searched {run.counts.mechanical_input_cards_after_conventional_search?.toLocaleString()} eligible authored cards · {(run.durationMs / 1000).toFixed(1)} s</p>
        </section>
        <p className="count-note"><b>These views overlap.</b> Each count is distinct cards, not abilities. A card may have a useful ability, an unresolved ability, and instructions we cannot read. Do not add the counts together.</p>
        <nav className="categories" aria-label="Finding categories">
          {categories.map(([key, label, description]) => <button key={key} aria-pressed={category === key} title={description}
            onClick={() => { setCategory(key); setPage(0); }}><span className="category-count">{run.cards.filter(c => inCategory(c, key)).length.toLocaleString()}</span>{label}<small>cards</small></button>)}
        </nav>
        <p className="hint category-explanation">{categories.find(c => c[0] === category)?.[2]} These are conditional findings, not proof of successful gameplay.</p>
        <div className="workspace">
          <section aria-label="Discovery results">
            <h2>Explore cards</h2>
            <div className="filters"><label>Find within these findings<input value={name} onChange={e => { setName(e.target.value); setPage(0); }} placeholder="Card name"/></label>
              <label>Mechanical relationship<select value={mechanic} onChange={e => { setMechanic(e.target.value); setPage(0); }}>
                <option value="ALL">All relationships</option>{mechanics.map(m => <option key={m}>{m}</option>)}
              </select></label></div>
            <p className="hint">{filtered.length.toLocaleString()} cards in this view · name order · page {page + 1} of {pages}</p>
            {selectedCard && <a className="inspection-link" href="#card-inspection" onClick={() => detailRef.current?.focus()}>Read selected card: {selectedCard.card} ↓</a>}
            <div className="results">
              {!visible.length && <p>No findings in this view. This is not proof that these mechanics are absent.</p>}
              {visible.map(card => <button key={card.index} className={`card ${selected === card.index ? 'selected' : ''}`}
                onClick={() => setSelected(card.index)} aria-pressed={selected === card.index} aria-controls="card-inspection" aria-label={card.card}>
                <CardPicture compact image={imageFor(card.index)}/>
                <div><strong>{card.card}</strong>
                  {selected === card.index && <small className="selected-marker">Selected for inspection</small>}
                  <span className="card-states">{card.pathways.MATCH > 0 && 'May help · '}{card.pathways.UNKNOWN > 0 && 'Needs investigation · '}{card.pathways.NO_MATCH > 0 && 'An ability ruled out · '}{card.unsupportedRecords > 0 && 'Not fully understood'}</span>
                  <small>{card.mechanics?.join(' · ') || 'No recognized relationship to this objective'}</small>
                  {card.reason && <p className="preview">{card.reason}</p>}
                </div>
              </button>)}
            </div>
            <div className="actions pager"><button onClick={() => setPage(page - 1)} disabled={!page}>Previous cards</button><button onClick={() => setPage(page + 1)} disabled={page + 1 >= pages}>Next cards</button></div>
          </section>
          <section id="card-inspection" ref={detailRef} className="detail" aria-label="Result inspection" tabIndex={-1} aria-live="polite">
            {selectedCard && <p className="eyebrow">SELECTED CARD</p>}
            <h2>{selectedCard?.card ?? 'Select a finding'}</h2>
            {detailLoading && <p role="status">Loading card evidence…</p>}
            {detailError && <p role="alert" className="warning">{detailError}</p>}
            {shownDetail && <>
              <div className="card-overview"><CardPicture key={shownDetail.card} image={imageFor(selected!)} />
                {shownDetail.authoredCard && <div className="authored"><p>{shownDetail.authoredCard.manaCost} · {shownDetail.authoredCard.typeLine}</p><p className="card-text">{shownDetail.authoredCard.text}</p>
                  <small>Text from the application’s card definition. The image may show updated wording; neither is a full verification of the implemented mechanics.</small></div>}
              </div>
              {!shownDetail.results.length && <p>No relationship to this objective was extracted. This card has instructions the application could not interpret; do not treat it as ruled out.</p>}
              {shownDetail.results.map((r, i) => {
                const explanation = shownDetail.explanations[i];
                return <article key={i} className="pathway">
                  <span className={`badge ${r.verdict}`}>{verdictLabel(r.verdict)}</span>
                  <h3>{explanation?.mechanic ?? 'Relationship to your question'} · {r.face}</h3>
                  <h4>How it relates</h4><p>{explanation?.summary ?? 'A precise explanation is unavailable. Inspect the recorded evidence before drawing a conclusion.'}</p>
                  <h4>What needs to happen</h4><ul>{(explanation?.requirements ?? []).map((p, j) => <li key={j}>{p}</li>)}</ul>
                  <h4>What isn’t established</h4><ul>{(explanation?.unknowns ?? ['Successful gameplay and whole-card correctness have not been verified.']).map((p, j) => <li key={j}>{p}</li>)}</ul>
                  <details><summary>Ability evidence and technical details</summary>
                    <p>Operation: {r.operation} · classification: {r.verdict} · {r.branch}</p><p>Path: <code>{r.abilityPath}</code></p>
                    <RawDetails label="Exact timing, costs, branch prerequisites and application decisions" value={{prerequisites: r.prerequisites, decisions: r.decisions}}/>
                    {r.circumstances && <RawDetails label="Timing, resources and circumstances" value={r.circumstances}/>}
                    {r.sourceReferences.map((s, j) => s.startsWith('https://github.com/') ? <p key={j}><a href={s} target="_blank" rel="noreferrer">Authored source at recorded revision</a></p> : <p key={j}>{s}</p>)}
                    <RawDetails label="Structured pathway record" value={r}/>
                  </details>
                </article>;
              })}
              <details><summary>What else could the application not read? ({shownDetail.unsupported.length} records)</summary>
                <p>These are instructions the application could not fully interpret. They may change how a card works; they do not establish that a card cannot help.</p>
                {shownDetail.unsupported.map((u, i) => <p className="warning" key={i}>{u.reason}</p>)}
                {!shownDetail.unsupported.length && <p>No unread instruction was reported. This does not establish whole-card correctness.</p>}
              </details>
              <details><summary>How much has this card been checked?</summary><p>Discovery reads represented abilities; it does not play out a game or independently certify an entire card.</p>
                {!shownDetail.translationAssurance.length && <p>No separate application card-review record was supplied.</p>}
                <RawDetails label="Card review, execution evidence and concern history" value={shownDetail.translationAssurance}/>
              </details>
              <RawDetails label="Related native actions, facts, eligibility and structural support" value={{actions: shownDetail.actions, facts: shownDetail.facts, eligibility: shownDetail.eligibility, structuralSupport: shownDetail.structuralSupport, unsupported: shownDetail.unsupported}}/>
              <RawDetails label="Image identity and source metadata" value={imageFor(selected!) ?? {status: 'UNAVAILABLE'}}/>
            </>}
          </section>
        </div>
        <details className="provenance"><summary>Search scope, counts, exclusions and source revisions</summary>
          <p>The search covers the authored catalog and application extensions, not every Commander-legal Magic card. {run.counts.format_legality_unknown_cards} cards with unresolved format legality were excluded before checking mechanics.</p>
          <p>The findings index includes {run.cards.length} distinct physical cards. Categories above overlap; counts below are pathway checks and unread instruction records, not additional cards.</p>
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
      <footer>Unofficial Magic: The Gathering research · © Wizards of the Coast · Card metadata and unaltered images from <a href="https://scryfall.com" target="_blank" rel="noreferrer">Scryfall</a></footer>
    </main>
  </>;
}
createRoot(document.getElementById('root')!).render(<App/>);
