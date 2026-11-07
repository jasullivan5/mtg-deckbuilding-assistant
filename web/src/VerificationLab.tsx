import {useEffect, useRef, useState} from 'react';
import {api, type CardImage, type Run} from './api';
import {CardPicture} from './CardPicture';

type Scenario = {
  id: string; card: string; title: string; question: string; mechanic: string; conditions: string;
  expected: string; rules: string; limits: string; cardSource: string; supportingSource: string | null;
};
type Sources = {
  reviewedOn: string; rulesUrl: string; rulesSha256: string;
  cards: {card: {name: string; oracle_text: string; scryfall_uri: string; set: string; collector_number: string}}[];
};
type Listing = {scenarios: Scenario[]; images: Record<string, CardImage>; sources: Sources};
type ObjectRef = {entityId: string; generation: number};
type Checkpoint = {
  sourceZone: string | null; sourceType: string | null; opponentLife: number; stackSize: number;
  ownerGraveyard: string[]; battlefield: string[]; creatureCardInGraveyard: boolean | null;
  sourceRef: ObjectRef | null; triggerSource: ObjectRef | null;
};
type Result = {
  scenarioId: string; execution: string; comparison: string; problem: string | null; startedAt: string;
  scenario: Scenario;
  applicationRevision: string; argentumCommit: string; engineMode: string; creatureDied: boolean | null;
  sourceReturnedAsNewObject: boolean | null;
  initial: Checkpoint | null; final: Checkpoint | null;
  actions: {label: string; outcome: string; message: string | null; events: string[]; checkpoint: Checkpoint}[];
};
export type LabContext = {
  run: Run; card: string; scenarioId: string; objectiveLabel: string; finding: string | null;
};

function CheckpointView({state}: {state: Checkpoint}) {
  return <dl className="lab-state">
    <div><dt>Source card</dt><dd>{state.sourceZone?.toLowerCase() ?? 'Absent'} · {state.sourceType ?? 'Type unknown'}</dd></div>
    <div><dt>Battlefield</dt><dd>{state.battlefield.join(', ') || 'Empty'}</dd></div>
    <div><dt>Owner’s graveyard</dt><dd>{state.ownerGraveyard.join(', ') || 'Empty'}</dd></div>
    <div><dt>Opponent’s life</dt><dd>{state.opponentLife}</dd></div>
    <div><dt>On the stack</dt><dd>{state.stackSize}</dd></div>
  </dl>;
}

function ObjectiveReading({value}: {value: boolean | null}) {
  return <strong>{value === true ? 'Observed' : value === false ? 'Did not occur' : 'Unresolved'}</strong>;
}

function IdentityEvidence({result}: {result: Result}) {
  const references = [
    {label: 'Before the action', ref: result.initial?.sourceRef},
    {label: 'Source held by the trigger', ref: result.actions.find(action => action.checkpoint.triggerSource)?.checkpoint.triggerSource},
    {label: 'At the final checkpoint', ref: result.final?.sourceRef},
  ];
  return <dl className="lab-state">{references.map(({label, ref}) => ref && <div key={label}>
    <dt>{label}</dt><dd><code>{ref.entityId}</code> · Generation {ref.generation}</dd>
  </div>)}</dl>;
}

export default function VerificationLab({context}: {context?: LabContext}) {
  const [listing, setListing] = useState<Listing>();
  const [selected, setSelected] = useState(context?.scenarioId ?? '');
  const [result, setResult] = useState<Result>();
  const [error, setError] = useState('');
  const [running, setRunning] = useState(false);
  const [technical, setTechnical] = useState(false);
  const request = useRef<AbortController>(null);
  useEffect(() => {
    const controller = new AbortController();
    api<Listing>('lab/scenarios', undefined, controller.signal)
      .then(data => {setListing(data); setSelected(id => data.scenarios.some(s => s.id === id) ? id : data.scenarios[0].id);})
      .catch(e => {if (!controller.signal.aborted) setError(e.message);});
    return () => {controller.abort(); request.current?.abort();};
  }, []);
  const scenario = result?.scenario ?? listing?.scenarios.find(s => s.id === selected);
  const cardChoices = listing?.scenarios.filter((s, i, list) => list.findIndex(other => other.card === s.card) === i);
  const sources = listing?.sources.cards.filter(({card}) =>
    [scenario?.cardSource, scenario?.supportingSource].includes(card.scryfall_uri.split('?')[0]));
  const discovery = context?.card === scenario?.card ? context : undefined;
  const comparable = discovery && result && discovery.run.applicationRevision === result.applicationRevision &&
    discovery.run.argentumCommit === result.argentumCommit;

  function choose(id: string) {
    if (id === selected) return;
    request.current?.abort();
    setSelected(id); setResult(undefined); setRunning(false); setError(''); setTechnical(false);
  }
  async function execute() {
    if (!scenario) return;
    request.current?.abort();
    const controller = new AbortController();
    request.current = controller;
    setResult(undefined); setError(''); setRunning(true); setTechnical(false);
    try {
      const value = await api<Result>('lab/execute', {scenarioId: scenario.id},
        AbortSignal.any([controller.signal, AbortSignal.timeout(135_000)]));
      if (!controller.signal.aborted) {
        if (value.scenarioId !== scenario.id || value.scenario?.card !== scenario.card)
          throw new Error('The service returned evidence for another scenario.');
        setResult(value);
      }
    } catch (e) {
      if (!controller.signal.aborted) setError(e instanceof Error ? e.message : String(e));
    } finally { if (!controller.signal.aborted) setRunning(false); }
  }

  return <section className="verification-lab" aria-labelledby="lab-heading">
    <div className="lab-heading">
      <p className="lab-kicker">Native execution · Curated positions</p>
      <h2 id="lab-heading">Mechanical Verification Lab</h2>
      <p>Run a small Magic interaction in Argentum and compare what happened with the rules expectation.</p>
    </div>
    {error && <p className="warning" role="alert">{error}</p>}
    {!listing && !error && <p role="status">Loading scenarios…</p>}
    {scenario && listing && <>
      <div className="lab-choices" role="group" aria-label="Cards to investigate">
        {cardChoices?.map(s => <button key={s.card} aria-pressed={s.card === scenario.card} onClick={() => choose(s.id)}>{s.card}</button>)}
      </div>
      <div className="lab-intro">
        <CardPicture key={scenario.card} image={listing.images[scenario.card]}/>
        <div>
          <h3>{scenario.question}</h3>
          <p>{scenario.mechanic}</p>
          {discovery && <p className="lab-context"><strong>From discovery · {discovery.objectiveLabel}</strong><br/>
            {discovery.finding && <>{discovery.finding} </>}A discovery match describes conditional potential.</p>}
          <div className="lab-choices" role="group" aria-label="Scenario conditions">
            {listing.scenarios.filter(s => s.card === scenario.card).map(s =>
              <button key={s.id} aria-pressed={s.id === selected} onClick={() => choose(s.id)}>{s.title}</button>)}
          </div>
          <h4>Before the action</h4>
          <p>{scenario.conditions}</p>
          <h4>Rules expectation</h4>
          <p>{scenario.expected}</p>
          <button className="primary" disabled={running} onClick={() => void execute()}>Run scenario</button>
          {running && <p role="status">Running a fresh Argentum position…</p>}
        </div>
      </div>
      {result && <section className="lab-result" aria-labelledby="lab-result-heading">
        <div className="lab-result-head">
          <h3 id="lab-result-heading">What happened</h3>
          <p role="status" className={result.comparison === 'AGREES' ? 'lab-agrees' : 'warning'}>
            {result.comparison === 'AGREES' ? 'Observed outcome agrees with the rules expectation' :
              result.comparison === 'DIFFERS' ? 'Observed outcome differs from the rules expectation' : 'The comparison could not be completed'}
          </p>
          <p className="hint">{result.execution === 'FRESH_NATIVE' ? 'Fresh Argentum execution' :
            result.execution === 'FRESH_NATIVE_INCOMPLETE' ? 'Fresh Argentum attempt, incomplete' : 'Native execution did not start'} · {new Date(result.startedAt).toLocaleString()}</p>
          {result.problem && <p className="warning">{result.problem}</p>}
        </div>
        <div className="lab-checkpoints">
          {result.initial && <div><h4>Starting position</h4><CheckpointView state={result.initial}/></div>}
          {result.final && <div><h4>Final checkpoint</h4><CheckpointView state={result.final}/></div>}
        </div>
        <h4>What was done</h4>
        <ol className="lab-timeline">
          {result.actions.map((action, i) => <li key={i}>
            <strong>{action.label}</strong>
            {action.message && <p>{action.outcome === 'ILLEGAL_ACTION' ? 'Rejected by Argentum. ' : ''}{action.message}</p>}
            {action.events.map((event, j) => <p key={j}>{event}</p>)}
            <small>Source {action.checkpoint.sourceZone?.toLowerCase() ?? 'absent'} · {action.checkpoint.sourceType ?? 'unknown type'} ·
              {' '}{action.checkpoint.stackSize} on stack · Opponent {action.checkpoint.opponentLife} life</small>
          </li>)}
        </ol>
        <h4>Why this matters</h4>
        {result.sourceReturnedAsNewObject === true && <p>Argentum tracked the same physical card through exile and back. The permanent that returned is a new game object.</p>}
        {result.comparison !== 'UNASSESSED' && <dl className="lab-observations">
          <div><dt>Player 1’s nontoken source died as a creature</dt><dd><ObjectiveReading value={result.creatureDied}/></dd></div>
          <div><dt>Player 1 has a creature card in their graveyard at the checkpoint</dt><dd><ObjectiveReading value={result.final?.creatureCardInGraveyard ?? null}/></dd></div>
        </dl>}
        {discovery && (comparable ?
          <p>Selected discovery objective · <strong>{discovery.objectiveLabel}</strong>. {discovery.run.objective === 'own-nontoken-creature-deaths' ?
            'Compare with the death of the source.' : discovery.run.objective === 'stock-own-graveyard' ?
              'Compare with the creature card in the graveyard at the final checkpoint.' :
              'The Lab measures source death and graveyard arrival. This objective asks about a different interaction.'}</p> :
          <p className="warning">This execution used different revisions from the linked discovery. Rerun discovery to compare the two.</p>)}
        <details className="lab-technical" onToggle={e => setTechnical(e.currentTarget.open)}><summary>Exact revisions, object references, native actions, events and state</summary>
          <p>Application <code>{result.applicationRevision}</code><br/>Argentum <code>{result.argentumCommit}</code> · {result.engineMode}</p>
          <IdentityEvidence result={result}/>
          {technical && <pre>{JSON.stringify(result, null, 2)}</pre>}
        </details>
      </section>}
      <div className="lab-limits">
        <h4>What remains untested</h4>
        <p>{scenario.limits} These observations apply to this position. Whole-card behavior and independent translation qualification require separate evidence.</p>
        <details><summary>Oracle printings, rulings and rules provenance</summary>
          <p>Source review dated {listing.sources.reviewedOn} · {scenario.rules}. <a href={listing.sources.rulesUrl}>Wizards Comprehensive Rules</a></p>
          {sources?.map(({card}) => <div key={card.name}><p><strong>{card.name}</strong> · {card.oracle_text}</p>
            <p><a href={card.scryfall_uri}>Scryfall · {card.set.toUpperCase()} #{card.collector_number}</a></p></div>)}
          <details><summary>Exact source identities, rulings and snapshot hashes</summary>
            <pre>{JSON.stringify({...listing.sources, cards: sources}, null, 2)}</pre>
          </details>
        </details>
      </div>
    </>}
  </section>;
}
