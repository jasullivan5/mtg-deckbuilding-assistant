import {useRef, useState} from 'react';
import {CardPicture} from './CardPicture';
import {retainedPrintingImage} from './retainedPrintingImage';
import evidence from './integration-change-evidence.json';

const {relationships, revisions, totals, population, objectives} = evidence;
const preferred = ['Bloodghast', 'Reassembling Skeleton', 'Blood Speaker', 'Wolfbat'];
const explanations: Record<string, string> = {
  Bloodghast: 'When a land enters under your control, Bloodghast can return from your graveyard. The old search treated the trigger as if Bloodghast had to be on the battlefield, so it rejected the return.',
  'Reassembling Skeleton': 'Pay {1}{B} to return Skeleton tapped from your graveyard. This is an activated ability and the search already understood where it could be used.',
  'Blood Speaker': 'The Demon-triggered ability returns Blood Speaker to your hand. The search corrected where the trigger operates and still excluded the return from direct battlefield reanimation.',
  Wolfbat: 'When you draw your second card in a turn, you can pay {B} to return Wolfbat with a finality counter. If it would go to the graveyard from the battlefield with that counter, it goes to exile instead. This return alone does not supply indefinitely reusable creature fuel.',
  "All Hallow's Eve": 'This movement puts the spell itself into the graveyard from exile. Its separate mass return still has an unresolved pathway in these records.',
};
type Relationship = (typeof relationships)[number];
const groups = [
  {label: 'Newly recognized returns', includes: (row: Relationship) => row.outcome.before.verdict === 'NO_MATCH' && row.outcome.after.verdict === 'MATCH'},
  {label: 'Still outside direct reanimation', includes: (row: Relationship) => row.outcome.after.verdict === 'NO_MATCH'},
  {label: 'Already recognized control', includes: (row: Relationship) => row.outcome.before.verdict === 'MATCH' && row.outcome.after.verdict === 'MATCH'},
];
const verdictText: Record<string, string> = {MATCH: 'Recognized', NO_MATCH: 'Excluded', UNKNOWN: 'Unresolved'};

export default function IntegrationChange() {
  const [card, setCard] = useState(preferred[0]);
  const inspection = useRef<HTMLElement>(null);
  const {source, outcome, fact, action, otherOutcomes, unsupported, eligibility} = relationships.find(row => row.source.name === card)!;
  const factChanges = Object.keys(fact.after).filter(key =>
    JSON.stringify(fact.before[key as keyof typeof fact.before]) !== JSON.stringify(fact.after[key as keyof typeof fact.after]));
  const movement = [fact.after.sourceZone, fact.after.destinationZone].join(' → ');
  const negative = groups[1].includes;
  const hand = relationships.filter(row => negative(row) && row.fact.after.destinationZone === 'Hand').length;
  const exile = relationships.filter(row => negative(row) && row.fact.after.sourceZone === 'Exile').length;
  function inspect(name: string) {
    setCard(name);
    inspection.current?.focus();
  }

  return <section id="integration-change" className="detail integration-change" aria-labelledby="integration-title">
    <p className="eyebrow">HISTORICAL DISCOVERY COMPARISON</p>
    <h2 id="integration-title">When a return trigger works from the graveyard</h2>
    <p>The Explorer missed returns like Bloodghast’s because it assumed the source of the trigger was on the battlefield.
      The correction was in <strong>objective discovery</strong>, where the application reads native abilities into facts.
      It now respects a single explicitly declared active zone for triggers of this kind. The same native definitions,
      engine and Commander eligibility records were used on both sides.</p>
    <p className="hint">Retained Judith, the Scourge Diva reports · Return our creatures · {population.toLocaleString()} eligible physical card inputs · No additional search filters</p>
    <div className="integration-metrics">
      <p><strong>{totals.before.cards} → {totals.after.cards}</strong> distinct cards with an accepted return</p>
      <p><strong>{totals.before.pathways} → {totals.after.pathways}</strong> accepted reanimation pathways</p>
      <p><strong>{totals.before.unresolved} → {totals.after.unresolved}</strong> unresolved reanimation pathways</p>
    </div>
    <p>{objectives.reanimation.verdictChanges} graveyard-to-battlefield relationships became recognized.
      {' '}{hand} graveyard-to-hand relationships and {exile} exile-to-graveyard relationship stayed excluded for those movements.
      Across the death and token objectives, {objectives.deaths.factChanges} shared movement facts changed while all
      {' '}{objectives.deaths.results.toLocaleString()} death checks and {objectives.tokens.results.toLocaleString()} token checks and their decisions stayed the same.</p>
    <p>Within these captures, the corrected returns account for every changed verdict.
      The comparison covers emitted checks in this population. It leaves gameplay behavior, unrepresented abilities and multi-zone trigger handling open.</p>
    <details>
      <summary>Comparison conditions and receipt hashes</summary>
      <dl>
        <dt>Before application</dt><dd><code>{revisions.before}</code></dd>
        <dt>After application</dt><dd><code>{revisions.after}</code></dd>
        <dt>Shared Argentum revision (app-stable)</dt><dd><code>{revisions.engine}</code></dd>
      </dl>
      <p>The captures contained {objectives.reanimation.results.toLocaleString()} reanimation checks on each side,
        including {totals.before.rejected.toLocaleString()} → {totals.after.rejected.toLocaleString()} excluded pathways.
        The application revisions come from the historical investigation record. The reports carry the engine SHA.</p>
      <pre>{JSON.stringify(objectives, null, 2)}</pre>
    </details>

    <h3>Compare four representative cards</h3>
    <nav className="categories integration-controls" aria-label="Representative cards">
      {preferred.map(name => <button key={name} aria-pressed={name === card} onClick={() => inspect(name)}>{name}</button>)}
    </nav>
    <details>
      <summary>Browse all measured changes and controls</summary>
      {groups.map(group => {
        const rows = relationships.filter(group.includes);
        return <div key={group.label}>
          <h4>{group.label} · {rows.length} relationships</h4>
          <div className="actions">{rows.map(row => <button key={row.source.name} aria-pressed={row.source.name === card}
            onClick={() => inspect(row.source.name)}>{row.source.name}</button>)}</div>
        </div>;
      })}
    </details>

    <section className="pathway" tabIndex={-1} ref={inspection} aria-labelledby="integration-card">
      <h3 id="integration-card">{card}</h3>
      <div className="card-overview">
        <CardPicture key={source.printingId} image={retainedPrintingImage(source)}/>
        <div className="authored">
          <p className="eyebrow">RECORDED OBJECTIVE DISCOVERY · {movement}</p>
          <p><strong>{verdictText[outcome.before.verdict]} → {verdictText[outcome.after.verdict]}</strong></p>
          <p>{explanations[card] ?? (outcome.after.verdict === 'MATCH'
            ? 'The search now recognizes this conditional return to the battlefield. The trigger and any requirements still apply.'
            : 'The search corrected the trigger’s source information. The destination is outside this direct battlefield reanimation check.')}</p>
          <p>{factChanges.length
            ? <>The movement fact changed. The final discovery verdict {outcome.before.verdict === outcome.after.verdict ? 'stayed the same' : 'also changed'}.</>
            : 'The selected movement fact and its verdict were stable in both captures.'}</p>
          <p className="hint">Source-derived expectation from the verified printing</p>
          <p className="card-text">{source.oracleText}</p>
        </div>
      </div>
      <h4>What the evidence establishes</h4>
      <dl className="integration-stages">
        <dt>Definition availability</dt><dd>The native definition in the shared engine revision is associated with
          the {source.set.toUpperCase()} #{source.collectorNumber} source printing.</dd>
        <dt>Native representability</dt><dd>The retained native action and movement operation express the selected {movement.toLowerCase()} relationship.
          The comparison covers that action and leaves whole-card representability open.</dd>
        <dt>Rules-source fidelity</dt><dd>The review compared the selected movement and its context against the retained Oracle snapshot.
          Whole-card behavior and interactions remain outside this review.</dd>
        <dt>Engine execution</dt><dd>No gameplay scenario is linked to this relationship in the comparison. The historical runs executed discovery.</dd>
        <dt>Independent assurance</dt><dd>No independently qualified requirement is linked to this relationship in the retained reports.</dd>
        <dt>Objective discovery</dt><dd><code>{outcome.before.verdict} → {outcome.after.verdict}</code> for this pathway.
          A recognized return still depends on the game state, choices and applicable restrictions.</dd>
      </dl>
      <p className="hint">Separate eligibility gate · Included in Judith’s measured input pool on both sides.</p>
      <details key={card}>
        <summary>Source provenance, native action and recorded before/after evidence</summary>
        <p><a href={source.url} target="_blank" rel="noreferrer">Printing source</a> ·{' '}
          <a href={`https://github.com/jasullivan5/argentum-engine/blob/${revisions.engine}/${source.nativeSource}`} target="_blank" rel="noreferrer">Native definition at recorded revision</a></p>
        <p>Ability path <code>{outcome.after.abilityPath}</code></p>
        {otherOutcomes.length > 0 && <p>Other emitted checks on this card · {otherOutcomes.map(pair =>
          `${verdictText[pair.before.verdict]} → ${verdictText[pair.after.verdict]}`).join(', ')}</p>}
        <p>Unread or unsupported records on this card in the after report · {unsupported.length}</p>
        <pre>{JSON.stringify({source, outcome, fact, action, eligibility, otherOutcomes, unsupported}, null, 2)}</pre>
      </details>
    </section>
    <p className="hint"><strong>Possible follow-up.</strong> A focused Wolfbat gameplay scenario could check the paid return, counter placement
      and what happens when Wolfbat would next go to the graveyard.</p>
  </section>;
}
