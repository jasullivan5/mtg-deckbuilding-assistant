import {useState} from 'react';
import baseline from './event-to-state-receipt.json';
import receipt from './objective-semantics-receipt.json';

const cases = [
  {id: 'owned', title: 'Owned creature', setup: 'You own and control Wolfbat.', expectation: 'A qualifying death can put your creature card into your graveyard.'},
  {id: 'borrowed', title: 'Opponent-owned creature', setup: 'You control a Wolfbat owned by your opponent.', expectation: 'The creature can qualify for the death while its owner gets the graveyard card.'},
  {id: 'token', title: 'Creature token', setup: 'You control a creature token (a Wolfbat copy in the fixture).', expectation: 'A token can die. It does not meet the nontoken event criterion or leave a creature card behind.'},
  {id: 'finality', title: 'Finality counter', setup: 'You own and control Wolfbat with a finality counter already on it.', expectation: 'Finality sends it to exile instead. Paying the sacrifice cost does not guarantee a death.'},
  {id: 'mill', title: 'Mill a creature card', setup: 'Your Wolfbat is the top card of your library.', expectation: 'A creature card can enter your graveyard from the library without a death.'},
  {id: 'animated', title: 'Animated land', setup: 'You own and control Raging Ravine. Its own animation ability resolves before the sacrifice.', expectation: 'The animation makes Raging Ravine a creature on the battlefield. Its graveyard object should be a land card.'},
  {id: 'already-satisfied', title: 'Prefilled graveyard', setup: 'Your Wolfbat is already in your graveyard. You control a different Wolfbat owned by your opponent.', expectation: 'The opponent-owned creature can die while your existing card continues to supply the state.'},
];

const verdict = (value: boolean | null) => value == null ? 'Unresolved' : value ? 'Satisfied' : 'Not satisfied';

const attributionText = {
  ARRIVAL_SUPPLIES: 'Yes. The card from the qualifying move still supplies the state at the checkpoint.',
  NO_QUALIFYING_EVENT: 'No. This move did not meet the death criterion.',
  STATE_UNSATISFIED: 'No. The graveyard state was unsatisfied at the checkpoint.',
  ARRIVAL_DOES_NOT_SUPPLY: 'No. The state was satisfied independently of this arrival.',
  UNRESOLVED: 'Unresolved. The execution evidence is insufficient to attribute the state to this move.',
} as const;

export function EventToState() {
  const [selected, setSelected] = useState(cases[0].id);
  const example = cases.find(item => item.id === selected)!;
  const observed = receipt.cases.find(item => item.id === selected)!;
  const original = baseline.cases.find(item => item.id === selected);
  const route = observed.actual === 'Graveyard' ? (observed.owner === 'You' ? 'your graveyard' : 'opponent’s graveyard') : observed.actual.toLowerCase();

  return <section id="event-to-state" aria-labelledby="event-state-title">
    <h3 id="event-state-title">Can a death establish a graveyard state?</h3>
    <p className="eyebrow">APPLICATION EVALUATION · RETAINED EXECUTION RECORD</p>
    <dl className="contribution-models">
      <div><dt><strong>A · My nontoken creatures die</strong></dt><dd>A nontoken creature you controlled as it left the battlefield reaches a graveyard.</dd></div>
      <div><dt><strong>B · I have a creature card in my graveyard</strong></dt><dd>Your graveyard contains at least one creature card at the observation point.</dd></div>
    </dl>
    <p>The checkpoint is after the action, stack and pending decisions settle.</p>
    <nav className="categories" aria-label="Controlled cases">
      {cases.map(item => <button key={item.id} aria-pressed={item.id === selected} onClick={() => setSelected(item.id)}>{item.title}</button>)}
    </nav>
    <section className="pathway" aria-label="Selected observation" aria-live="polite">
      <h4>{example.title}</h4>
      <ol className="primer-flow">
        <li><strong>Starting setup</strong>{example.setup}</li>
        <li><strong>Native action</strong>{observed.sacrificed ? 'Sacrifice it to activate Viscera Seer’s scry ability.' : 'Cast Tome Scour on yourself to mill five cards.'}</li>
        <li><strong>Recorded move</strong>{observed.from} → {route}.{observed.requested !== observed.actual && <> The move requested {observed.requested.toLowerCase()} and was redirected.</>}</li>
      </ol>
      <p><strong>Graveyard state before the move.</strong> {verdict(observed.stateBefore)}.</p>
      <p><strong>Your graveyard at the checkpoint.</strong> {observed.myGraveyard.join(', ') || 'Empty.'}
        {' '}<strong>Opponent’s graveyard.</strong> {observed.opponentGraveyard.join(', ') || 'Empty.'}</p>
      <p>{observed.observedZone ? <>The tracked card is in {observed.observedZone.toLowerCase()} with card types {observed.observedTypes.join(', ').toLowerCase()}.</> :
        'The tracked token is gone at the checkpoint.'}</p>
      <div className="contribution-models">
        <section aria-label="Event outcome"><h4>A · Death event</h4><p className="contribution-roles">{verdict(observed.event)}</p></section>
        <section aria-label="State outcome"><h4>B · Graveyard state</h4><p className="contribution-roles">{verdict(observed.state)}</p></section>
      </div>
      <p><strong>Does this death supply a card that satisfies the state?</strong> {attributionText[observed.attribution as keyof typeof attributionText]}</p>
      <details key={example.id}>
        <summary>Rules-derived expectation and recorded inputs</summary>
        <p>{example.expectation}</p>
        <p>Move owner: {observed.owner}. Controller before departure: {observed.controllerBefore ?? 'not applicable'}. Battlefield creature: {observed.battlefieldCreature == null ? 'not applicable' : observed.battlefieldCreature ? 'yes' : 'no'}. Token: {observed.token == null ? 'not applicable' : observed.token ? 'yes' : 'no'}. Finality counters at departure: {observed.finalityCounters}.</p>
        <p>The engine linked departure reference {observed.fromGeneration} and arrival reference {observed.toGeneration} for the same tracked entity. The arrival reference {observed.atArrivalReference ? 'was current' : 'was gone'} at the checkpoint.</p>
        {original && <p><strong>Retained PR #30 verdicts.</strong> A {verdict(original.event).toLowerCase()} · B {verdict(original.state).toLowerCase()} · link {original.linked ? 'yes' : 'no'}.</p>}
      </details>
    </section>
    <details>
      <summary>Evidence and limits of this comparison</summary>
      <p>A controlled Kotlin test executed the moves and called the reusable application evaluators. Opening this panel reads the retained results. The original five PR #30 verdicts appear separately under each case’s recorded inputs.</p>
      <p>The fixtures set up ownership, borrowed control, token status, the finality counter and the prefilled graveyard directly. Argentum executed the animation, sacrifices and mill. The evaluator read the graveyard types at the checkpoint.</p>
      <p>Static discovery facts describe possible movement and object filters. Attribution needs the actual destination, last-known battlefield characteristics and the current graveyard objects. Missing execution evidence can leave attribution unresolved. A later departure can remove the card while the historical death stays true.</p>
      <p>Scenario application <code>{receipt.application}</code><br/>Resolved engine <code>{receipt.engine}</code></p>
      <p>PR #30 scenario application <code>{baseline.application}</code><br/>Resolved engine <code>{baseline.engine}</code></p>
    </details>
  </section>;
}
