import {useState} from 'react';
import type {AbilityInspection, CardImage, Evidence} from './api';
import {EvidenceInspector, Concerns} from './EvidenceInspector';
import {CardPicture} from './CardPicture';

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
      {evidence.authoredCard && <div className="authored"><p>{evidence.authoredCard.manaCost} · {evidence.authoredCard.typeLine}</p>
        <p className="card-text">{evidence.authoredCard.text}</p></div>}
    </div>
    {available.length > 1 && <label className="ability-picker">Ability to inspect<select value={ability?.id} onChange={e => setSelected(e.target.value)}>
      {available.map(a => <option key={a.id} value={a.id}>{a.title}{a.face ? ` · ${a.face}` : ''}</option>)}
    </select></label>}
    {ability ? <article className="ability-inspection">
      <h3>{ability.title}{ability.face ? ` · ${ability.face}` : ''}</h3>
      <p className="contribution">{ability.contribution}</p>
      <div className="ability-parts"><List title="Pay" values={ability.costs}/><List title="Effect of this ability" values={ability.effects}/></div>
      <List title="Conditions and dependencies" values={ability.conditions}/>
      <List title="Costs to consider" values={ability.tradeoffs}/>
      <List title="What remains unresolved" values={ability.uncertainties}/>
      <h4>What supports this interpretation?</h4>
      <EvidenceInspector key={ability.id} indicators={ability.evidence} revision={revision}/>
      <Concerns items={evidence.concerns}/>
      {(ability.questions.length > 0 || unassigned.length > 0) && <details><summary>Outstanding interpretation questions ({ability.questions.length + unassigned.length})</summary>
        <Concerns items={ability.questions}/>
        {unassigned.length > 0 && <p>Other instructions on this card were not interpreted for this objective. They do not invalidate the identified part of this ability or establish a rules defect.</p>}
        {unassigned.length > 0 && <Raw label="Other instruction records" data={unassigned}/>}
      </details>}
      <details><summary>Investigate this ability</summary>
        <Raw label="Conditions and objective decisions" data={rows.map(r => ({prerequisites: r.prerequisites, decisions: r.decisions, circumstances: r.circumstances}))}/>
        <Raw label="Whole authored ability and related checks" data={{actions: evidence.actions.filter(a => a.id === ability.actionId), results: rows}}/>
        {evidence.results.some(r => r.verdict === 'NO_MATCH') && <Raw label="Nonqualifying checks (including objective-policy exclusions)" data={evidence.results.filter(r => r.verdict === 'NO_MATCH')}/>}
        <Raw label="All card evidence, source bindings and image association" data={{...evidence, image}}/>
      </details>
    </article> : <>
      <p>No objective-specific relationship was established for this card. Any unread instructions remain unresolved, not negative results.</p>
      <EvidenceInspector indicators={evidence.evidenceSummary} revision={revision}/><Concerns items={evidence.concerns}/>
      {evidence.unsupported.length > 0 && <Raw label="Instructions needing interpretation" data={evidence.unsupported}/>}
      <Raw label="All card evidence and image association" data={{...evidence, image}}/>
    </>}
  </>;
}
