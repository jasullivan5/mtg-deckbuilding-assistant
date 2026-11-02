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
export function CardInspection({evidence, image, revision}: {evidence: Evidence; image?: CardImage; revision?: string}) {
  const relevant = evidence.abilities.filter(a => a.verdict !== 'NO_MATCH');
  const available = relevant.length ? relevant : evidence.abilities;
  const [selected, setSelected] = useState(available[0]?.id ?? '');
  const ability: AbilityInspection | undefined = available.find(a => a.id === selected) ?? available[0];
  const rows = ability?.resultIndexes.map(i => evidence.results[i]) ?? [];
  const unassigned = evidence.unsupported.filter(u => !evidence.abilities.some(a => a.actionId === u.actionId));
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
      {!!ability.objectiveNotes?.length && <ul className="objective-notes">{ability.objectiveNotes.map((note, i) => <li key={i}>{note}</li>)}</ul>}
      {ability.unresolvedNotes?.map((note, i) => <p className="unresolved-note" key={i}>{note}</p>)}
      <EvidenceInspector key={ability.id} indicators={ability.evidence} revision={revision}/>
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
        {evidence.results.some(r => r.verdict === 'NO_MATCH') && <Raw label="Nonqualifying checks (including objective-policy exclusions)" data={evidence.results.filter(r => r.verdict === 'NO_MATCH')}/>}
        <Raw label="All card evidence, source bindings and image association" data={{...evidence, image}}/>
      </details>
    </article> : <>
      <p>No contribution identified for this objective.</p>
      <EvidenceInspector indicators={evidence.evidenceSummary} revision={revision}/><Concerns items={evidence.concerns}/>
      <details><summary>Technical investigation</summary>
      {evidence.unsupported.length > 0 && <Raw label="Instructions needing interpretation" data={evidence.unsupported}/>}
      <Raw label="All card evidence and image association" data={{...evidence, image}}/>
      </details>
    </>}
  </>;
}
