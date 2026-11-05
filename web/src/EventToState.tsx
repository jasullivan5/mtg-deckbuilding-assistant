import {useState} from 'react';
import receipt from './event-to-state-receipt.json';

const cases = [
  {id: 'owned', title: 'Owned creature', setup: 'You own and control Wolfbat.', expectation: 'A qualifying death can put your creature card into your graveyard.'},
  {id: 'borrowed', title: 'Opponent-owned creature', setup: 'You control a Wolfbat owned by your opponent.', expectation: 'The creature can qualify for the death while its owner gets the graveyard card.'},
  {id: 'token', title: 'Creature token', setup: 'You control a creature token (a Wolfbat copy in the fixture).', expectation: 'A token can die. It does not meet the nontoken event criterion or leave a creature card behind.'},
  {id: 'finality', title: 'Finality counter', setup: 'You own and control Wolfbat with a finality counter already on it.', expectation: 'Finality sends it to exile instead. Paying the sacrifice cost does not guarantee a death.'},
  {id: 'mill', title: 'Mill a creature card', setup: 'Your Wolfbat is the top card of your library.', expectation: 'A creature card can enter your graveyard from the library without a death.'},
];

const verdict = (value: boolean | null) => value == null ? 'Unresolved' : value ? 'Satisfied' : 'Not satisfied';


export function EventToState() {
  const [selected, setSelected] = useState(cases[0].id);
  const example = cases.find(item => item.id === selected)!;
  const observed = receipt.cases.find(item => item.id === selected)!;
  const route = observed.actual === 'Graveyard' ? (observed.owner === 'You' ? 'your graveyard' : 'opponent’s graveyard') : observed.actual.toLowerCase();

  const missingLink = observed.event ? 'The death qualified, but the card went to its owner’s graveyard.' :
    observed.state ? 'It establishes the state without a death.' :
      observed.token ? 'The death was of a token, which is outside A and left no card for B.' :
        'The actual destination was exile, so the move supplied neither the death nor a graveyard card.';

  return <section id="event-to-state" aria-labelledby="event-state-title">
    <h3 id="event-state-title">Can a death establish a graveyard state?</h3>
    <p className="eyebrow">CONTROLLED ENGINE SCENARIOS · RETAINED EXECUTION RECORD</p>
    <dl className="contribution-models">
      <div><dt><strong>A · My nontoken creatures die</strong></dt><dd>A nontoken creature you controlled as it left the battlefield reaches a graveyard.</dd></div>
      <div><dt><strong>B · I have a creature card in my graveyard</strong></dt><dd>Your graveyard contains at least one creature card at the observation point.</dd></div>
    </dl>
    <p>Each case starts with no creature cards in your graveyard. The checkpoint is after the action, stack and pending decisions settle.</p>
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
      <p><strong>Your graveyard at the checkpoint.</strong> {observed.myGraveyard.join(', ') || 'Empty.'}
        {' '}<strong>Opponent’s graveyard.</strong> {observed.opponentGraveyard.join(', ') || 'Empty.'}</p>
      <p>{observed.observedZone ? <>The tracked card is in {observed.observedZone.toLowerCase()} with card types {observed.observedTypes.join(', ').toLowerCase()}.</> :
        'The tracked token is gone at the checkpoint.'}</p>
      <div className="contribution-models">
        <section aria-label="Event outcome"><h4>A · Death event</h4><p className="contribution-roles">{verdict(observed.event)}</p></section>
        <section aria-label="State outcome"><h4>B · Graveyard state</h4><p className="contribution-roles">{verdict(observed.state)}</p></section>
      </div>
      <p><strong>Did the death establish the state?</strong> {observed.linked == null ? 'Unresolved.' :
        observed.linked ? 'Yes. The card that arrived from the qualifying move still supplies the state at the checkpoint.' :
          <>No. {missingLink}</>}</p>
      <details key={example.id}>
        <summary>Rules-derived expectation and recorded inputs</summary>
        <p>{example.expectation}</p>
        <p>Move owner: {observed.owner}. Controller before departure: {observed.controllerBefore ?? 'not applicable'}. Battlefield creature: {observed.battlefieldCreature == null ? 'not applicable' : observed.battlefieldCreature ? 'yes' : 'no'}. Token: {observed.token == null ? 'not applicable' : observed.token ? 'yes' : 'no'}. Finality counters at departure: {observed.finalityCounters}.</p>
        <p>The engine linked departure reference {observed.fromGeneration} and arrival reference {observed.toGeneration} for the same tracked entity. The arrival reference {observed.atArrivalReference ? 'was current' : 'was gone'} at the checkpoint.</p>
      </details>
    </section>
    <details>
      <summary>Evidence and limits of this comparison</summary>
      <p>These observations come from an executed Kotlin scenario test. Opening this panel reads the retained record. Earlier Discovery Coverage examined Seer’s sacrifice, and the historical Integration Change Case Study examined Wolfbat’s return. Those earlier discovery records did not execute this combination.</p>
      <p>The fixtures set up control, token status and the finality counter directly. The test executes the sacrifice or mill, rather than acquiring control or earning the counter in play. The graveyard types were read at the checkpoint. Animated noncreatures and later departures were not exercised here.</p>
      <p>Static discovery facts describe potential movement and object filters. The scenario supplied actual destination, battlefield characteristics at departure, and the new object at the checkpoint. A card can later leave the graveyard while the historical death stays true.</p>
      <p>Scenario application <code>{receipt.application}</code><br/>Resolved engine <code>{receipt.engine}</code></p>
    </details>
  </section>;
}
