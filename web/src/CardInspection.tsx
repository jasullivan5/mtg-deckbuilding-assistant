import {useState} from 'react';
import type {AbilityInspection, CardImage, Evidence} from './api';
import {EvidenceInspector, Concerns} from './EvidenceInspector';
import {CardPicture} from './CardPicture';
import {CardRules} from './CardRules';

function List({title, values}: {title: string; values: string[]}) {
  return values.length > 0 && <section className="ability-part"><h4>{title}</h4><ul>{values.map((v, i) => <li key={i}>{v}</li>)}</ul></section>;
}
function Raw({label, data}: {label: string; data: unknown}) {
  const [open, setOpen] = useState(false);
  return <details onToggle={e => setOpen(e.currentTarget.open)}><summary>{label}</summary>{open && <pre>{JSON.stringify(data, null, 2)}</pre>}</details>;
}
export function CardInspection({evidence, image, revision, preferGraveyard}: {evidence: Evidence; image?: CardImage; revision?: string; preferGraveyard?: boolean}) {
  const relevant = evidence.abilities.filter(a => a.verdict !== 'NO_MATCH');
  const available = relevant.length ? relevant : evidence.abilities;
  const [selected, setSelected] = useState(() => {
    const arrival = preferGraveyard && evidence.graveyardContributions?.find(item => item.relationship.decision.verdict === 'MATCH');
    return (arrival && available.find(a => a.actionId === arrival.relationship.actionId)?.id) || available[0]?.id || '';
  });
  const ability: AbilityInspection | undefined = available.find(a => a.id === selected) ?? available[0];
  const rows = ability?.resultIndexes.map(i => evidence.results[i]) ?? [];
  const policyExcluded = ability?.verdict === 'NO_MATCH' &&
    rows.some(row => row.mechanicalCompatibility === 'MATCH' && row.policyAcceptance === 'NO_MATCH');
  const indicators = policyExcluded ? ability.evidence.map(item => item.id === 'mechanic'
    ? {...item, status: 'Excluded by objective policy'} : item) : ability?.evidence;
  const unassigned = evidence.unsupported.filter(u => !evidence.abilities.some(a => a.actionId === u.actionId));
  const arrivals = evidence.graveyardContributions?.filter(item => item.relationship.actionId === ability?.actionId) ?? [];
  const supplies = ability?.resultIndexes.filter(i => evidence.results[i].branch === 'INTRINSIC_CREATURE_INPUT') ?? [];
  const stateChecks = rows.filter(row => row.branch === 'STATE_BENEFICIARY');
  const responses = ability?.resultIndexes.filter(i => evidence.results[i].branch === 'DEATH_TRIGGERED_OUTCOME') ?? [];
  return <>
    <div className="card-overview"><CardPicture image={image}/>
      {evidence.authoredCard && <div className="authored">
        <dl className="card-properties"><div><dt>Mana cost</dt><dd>{evidence.authoredCard.manaCost}</dd></div><div><dt>Type</dt><dd>{evidence.authoredCard.typeLine}</dd></div></dl>
        <CardRules lines={evidence.rules} text={evidence.authoredCard.text} selected={ability?.actionId} onSelect={id => {if (available.some(a => a.actionId === id)) setSelected(id);}}/>
      </div>}
    </div>
    {available.length > 1 && !available.every(a => evidence.rules?.some(line => line.actionId === a.actionId)) && <label className="ability-picker">Ability to inspect<select value={ability?.id} onChange={e => setSelected(e.target.value)}>
      {available.map(a => <option key={a.id} value={a.id}>{a.title}{a.face ? ` · ${a.face}` : ''}</option>)}
    </select></label>}
    {ability ? <article className="ability-inspection">
      {supplies.length > 0 && <section className="pathway" aria-label="Creature input supply">
        <h3>{supplies.some(i => evidence.results[i].verdict === 'MATCH') ? 'Supplies creature inputs' : 'Potential creature input'}</h3>
        {supplies.map(i => <div key={evidence.results[i].abilityPath}>
          {evidence.results[i].verdict === 'UNKNOWN' && <p><strong>Creature input needs investigation.</strong></p>}
          {evidence.results[i].verdict === 'NO_MATCH' && <p><strong>This movement does not qualify.</strong></p>}
          <p>{evidence.explanations[i].summary}</p>
          <List title="How the creature becomes available" values={evidence.explanations[i].requirements}/>
          <List title="Continued availability and limits" values={evidence.explanations[i].unknowns}/>
        </div>)}
      </section>}
      {stateChecks.length > 0 && <section className="pathway" aria-label="Death history">
        <h3>{stateChecks.some(row => row.verdict === 'MATCH') ? 'Benefits from death history' : 'Death history needs investigation'}</h3>
        {ability.resultIndexes.filter(i => evidence.results[i].branch === 'STATE_BENEFICIARY').map(i => <div key={i}>
          <p>{evidence.explanations[i].summary}</p>
          <p>{evidence.explanations[i].outcome}</p>
          <List title="When checked and what must hold" values={evidence.explanations[i].requirements}/>
          <List title="Interpretation limits" values={evidence.explanations[i].unknowns}/>
        </div>)}
      </section>}
      {responses.length > 0 && <section className="pathway" aria-label="Death response">
        <h3>{responses.some(i => evidence.results[i].verdict === 'MATCH') ? 'Benefits from deaths' : 'Potential death response'}</h3>
        {responses.map(i => <div key={evidence.results[i].abilityPath}>
          {evidence.results[i].verdict === 'UNKNOWN' && <p><strong>Conditional relationship needs investigation.</strong></p>}
          {evidence.results[i].verdict === 'NO_MATCH' && <p><strong>This event response does not qualify.</strong></p>}
          <p>{evidence.explanations[i].summary}</p>
          <List title="What must hold" values={evidence.explanations[i].requirements}/>
          <List title="Interpretation limits" values={evidence.explanations[i].unknowns}/>
        </div>)}
      </section>}
      {!!ability.objectiveNotes?.length && <ul className="objective-notes">{ability.objectiveNotes.map((note, i) => <li key={i}>{note}</li>)}</ul>}
      {ability.unresolvedNotes?.map((note, i) => <p className="unresolved-note" key={i}>{note}</p>)}
      {arrivals.length > 0 && <section className="pathway" aria-label="Graveyard contribution">
        <h3>Potential graveyard contribution</h3>
        <p><strong>Event objective</strong> · My nontoken creatures die</p>
        <p><strong>Separate state criterion</strong> · {arrivals[0].stateCriterion}</p>
        {arrivals.map(({relationship, mechanism, prerequisites, sources}) => <div key={relationship.factId ?? relationship.path}>
          {mechanism && <p><strong>{mechanism}</strong></p>}
          <p>{relationship.decision.verdict === 'UNKNOWN' && <strong>Unresolved. </strong>}{relationship.decision.reason}</p>
          <List title="What must hold" values={prerequisites}/>
          <details><summary>Native evidence</summary>
            <p>Action <code>{relationship.actionId}</code> · operation <code>{relationship.path}</code></p>
            {sources.filter(source => source.startsWith('https://') && source.includes('/blob/')).map(source =>
              <p key={source}><a href={source} target="_blank" rel="noreferrer">Open authored native definition</a></p>)}
            <Raw label="Recorded action and sacrifice fact" data={{
              action: evidence.actions.find(a => a.id === relationship.actionId),
              fact: evidence.facts.find(f => f.id === relationship.factId),
              decision: relationship.decision,
            }}/>
          </details>
        </div>)}
        <details><summary>Separate engine validation</summary>
          <p>Discovery checks native structure. Actual play determines whether the event happens and whether the arriving card supplies the state. The arrival establishes the state only if it was previously false.</p>
          <p>The seven controlled engine cases include borrowed creatures, finality, animated land, milling and an already satisfied graveyard. <a href="https://github.com/jasullivan5/mtg-deckbuilding-assistant/blob/b36b1dc8b098e9ef5a26d3a2312be2f24ca9f5a3/docs/EVENT_TO_STATE.md" target="_blank" rel="noreferrer">Read the retained execution evidence</a></p>
        </details>
      </section>}
      <EvidenceInspector key={ability.id} indicators={indicators ?? ability.evidence} revision={revision}/>
      <Concerns items={evidence.concerns}/>
      <details><summary>Technical investigation</summary>
      <Raw label="Ability identity and text association" data={{actionId: ability.actionId, title: ability.title, linkedRules: evidence.rules?.filter(line => line.actionId === ability.actionId)}}/>
      <List title="Complete ability projection" values={[...ability.costs, ...ability.effects, ...ability.conditions, ...ability.tradeoffs, ...ability.uncertainties]}/>
      {(ability.questions.length > 0 || unassigned.length > 0) && <details><summary>Interpretation questions ({ability.questions.length + unassigned.length})</summary>
        <Concerns items={ability.questions}/>
        {unassigned.length > 0 && <p>Other instructions on this card were not interpreted for this objective. They do not invalidate the identified part of this ability or establish a rules defect.</p>}
        {unassigned.length > 0 && <Raw label="Other instruction records" data={unassigned}/>}
      </details>}
        <Raw label="Conditions and objective decisions" data={rows.map(r => ({prerequisites: r.prerequisites, decisions: r.decisions, circumstances: r.circumstances}))}/>
        <Raw label="Whole authored ability and related checks" data={{actions: evidence.actions.filter(a => a.id === ability.actionId), results: rows}}/>
        {rows.some(row => row.stateEntry) && <Raw label="Native movement facts for this ability"
          data={evidence.facts.filter(fact => rows.some(row => row.stateEntry?.factId === fact.id))}/>}
        {evidence.results.some(r => r.verdict === 'NO_MATCH') && <Raw label="Nonqualifying checks (including objective-policy exclusions)" data={evidence.results.filter(r => r.verdict === 'NO_MATCH')}/>}
        <Raw label="All card evidence, source bindings and image association" data={{...evidence, image}}/>
      </details>
    </article> : <>
      <p>No discovery relationship was evaluated for this card.</p>
      <EvidenceInspector indicators={evidence.evidenceSummary} revision={revision}/><Concerns items={evidence.concerns}/>
      <details><summary>Technical investigation</summary>
      {evidence.unsupported.length > 0 && <Raw label="Instructions needing interpretation" data={evidence.unsupported}/>}
      <Raw label="All card evidence and image association" data={{...evidence, image}}/>
      </details>
    </>}
  </>;
}
