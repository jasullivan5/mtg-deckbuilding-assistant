import {useRef, useState} from 'react';
import {CardPicture} from './CardPicture';
import {PrimerCapabilities, PrimerMap, PrimerPurpose} from './PrimerOrientation';
import {retainedPrintingImage} from './retainedPrintingImage';
import evidence from './integration-change-evidence.json';
import cohort from '../../src/main/resources/discovery-coverage-v1.json';
import agentSnapshot from '../../src/test/resources/cards/agent-of-shauku-source.json';

const bloodghast = evidence.relationships.find(row => row.source.name === 'Bloodghast')!;
const agent = cohort.cards.find(row => row.card === 'Agent of Shauku')!;
const agentImage = retainedPrintingImage({
  name: agent.card, printingId: agent.printingId, oracleId: agent.oracleId, sha256: agent.snapshotSha256,
  set: agentSnapshot.set, collectorNumber: agentSnapshot.collector_number, cardUrl: agentSnapshot.scryfall_uri,
  image: agentSnapshot.id === agent.printingId && agentSnapshot.oracle_id === agent.oracleId &&
    agentSnapshot.name === agent.card && agentSnapshot.layout === 'normal' ? agentSnapshot.image_uris.normal : null,
  artist: agentSnapshot.artist,
});
const verdictLabels: Record<string, string> = {MATCH: 'Recognized', NO_MATCH: 'Excluded', UNKNOWN: 'Unresolved'};

const parts = [
  {label: 'The goal', title: 'What are we building?'},
  {label: 'The system', title: 'How does the system work?'},
  {label: 'Today’s capabilities', title: 'What can the Explorer do today?'},
  {label: 'Research examples', title: 'Apply the model to two cards'},
];

export default function IntegrationPrimer({openCase, openCoverage, busy}: {
  openCase: () => void; openCoverage: () => void; busy: boolean;
}) {
  const [part, setPart] = useState(0);
  const [journey, setJourney] = useState<'bloodghast' | 'agent' | 'recap'>('bloodghast');
  const [prediction, setPrediction] = useState<'recognized' | 'excluded'>();
  const [payment, setPayment] = useState<'land' | 'landCreature'>();
  const heading = useRef<HTMLHeadingElement>(null);
  const blood = journey === 'bloodghast';
  function openPart(next: number) {
    setPart(next);
    heading.current?.focus();
  }
  function visit(next: typeof journey) {
    setJourney(next);
    heading.current?.focus();
  }

  return <section id="integration-primer" className="detail primer" aria-label="How Card Integration Works">
    <p className="eyebrow">PART {part + 1} OF {parts.length} · COMMANDER RESEARCH</p>
    <h2>How Card Integration Works</h2>
    <nav className="categories" aria-label="Primer parts">
      {parts.map(({label}, index) => <button key={label} aria-current={part === index ? 'step' : undefined}
        onClick={() => openPart(index)}>{index + 1}. {label}</button>)}
    </nav>
    <h3 tabIndex={-1} ref={heading}>{part !== 3 ? parts[part].title : blood ? 'A trigger in the graveyard' :
      journey === 'agent' ? 'When a land could also be a creature' : 'What would convince you?'}</h3>
    {part === 0 && <PrimerPurpose/>}
    {part === 1 && <PrimerMap/>}
    {part === 2 && <PrimerCapabilities/>}
    {part !== 3 && <button className="primary" onClick={() => openPart(part + 1)}>Next · {parts[part + 1].label}</button>}
    {part === 3 && <>
      <nav className="categories" aria-label="Research examples">
        <button aria-pressed={blood} onClick={() => visit('bloodghast')}>Bloodghast</button>
        <button aria-pressed={journey === 'agent'} onClick={() => visit('agent')}>Agent of Shauku</button>
        <button aria-pressed={journey === 'recap'} onClick={() => visit('recap')}>Recap</button>
      </nav>
    {journey !== 'recap' && <div className="primer-card">
      <CardPicture key={journey} image={blood ? retainedPrintingImage(bloodghast.source) : agentImage}/>
      <div>
        <p className="card-text">{blood ? bloodghast.source.oracleText.split('\n').find(line => line.startsWith('Landfall')) : agent.oracleText}</p>
        <p className="eyebrow">ILLUSTRATED RULES RELATIONSHIP · NO GAMEPLAY RUN</p>
        {blood ? <ol className="primer-flow">
          <li><strong>Battlefield</strong>A land enters under your control.</li>
          <li><strong>Graveyard</strong>Bloodghast’s ability triggers here.</li>
          <li><strong>Your choice</strong>You may return Bloodghast to the battlefield.</li>
        </ol> : <ol className="primer-flow">
          <li><strong>Cost</strong>Pay {'{1}{B}'} and sacrifice a land you control.</li>
          <li><strong>Movement</strong>The land goes from the battlefield to the graveyard under ordinary conditions.</li>
          <li><strong>Question</strong>Was it also a nontoken creature when it left?</li>
        </ol>}
      </div>
    </div>}
    {blood && <>
      <h4>Predict what the old search found</h4>
      <p>The native definition described the graveyard return. What did the old search conclude?</p>
      <div className="actions" role="group" aria-label="Predict old discovery">
        <button aria-pressed={prediction === 'recognized'} onClick={() => setPrediction('recognized')}>It recognized the return</button>
        <button aria-pressed={prediction === 'excluded'} onClick={() => setPrediction('excluded')}>It excluded the return</button>
      </div>
      <div aria-live="polite">{prediction && <div className="primer-reveal">
        <p className="eyebrow">RECORDED DISCOVERY · BEFORE → AFTER</p>
        <p><strong>{verdictLabels[bloodghast.outcome.before.verdict]} → {verdictLabels[bloodghast.outcome.after.verdict]}</strong></p>
        <p>{prediction === 'recognized' ? 'That follows from the card’s rules. The old search missed the return anyway.' :
          'That was the recorded outcome. The card’s rules still allowed the return.'}
          {' '}Discovery read the native ability as if Bloodghast had to stay on the battlefield. The correction taught the search to read the declared graveyard zone.</p>
      </div>}</div>
      {prediction && <>
        <p>The printed rules describe what should happen. The native definition expresses the trigger and move.
          Discovery reads that definition for an objective. Actual engine execution needs a gameplay scenario with the relevant setup and checks.
          No such scenario or independent qualification is linked to this return in the comparison.</p>
        <div className="actions">
          <button onClick={openCase}>Open Bloodghast’s historical case study</button>
          <button className="primary" onClick={() => visit('agent')}>Next: Agent of Shauku</button>
        </div>
      </>}
    </>}
    {journey === 'agent' && <>
      <h4>Compare two possible payments</h4>
      <p>Assume the activation is otherwise legal and the sacrificed permanent goes to the graveyard. Which payment could count for the nontoken creature-death objective?</p>
      <div className="actions" role="group" aria-label="Compare sacrifice payments">
        <button aria-pressed={payment === 'land'} onClick={() => setPayment('land')}>A land that is only a land</button>
        <button aria-pressed={payment === 'landCreature'} onClick={() => setPayment('landCreature')}>A land that is also a nontoken creature</button>
      </div>
      <div aria-live="polite">{payment && <div className="primer-reveal">
        <p>{payment === 'land' ? 'That permanent can pay the land cost. It was never a creature in this example, so its move does not count as a creature death.' :
          'The same permanent can satisfy the land cost and count as a nontoken creature death. This describes a possible overlap in types and leaves the actual board setup open.'}</p>
        <p><strong>Recorded M3.1 finding · Unresolved relationship.</strong> {agent.diagnosis}</p>
        <p>Agent’s native land cost is present. The open question is whether the search can establish the needed type overlap.
          Commander eligibility is a separate deck permission check.</p>
      </div>}</div>
      {payment && <div className="actions">
        <button disabled={busy} onClick={openCoverage}>Open Agent in Discovery Coverage</button>
        <button className="primary" onClick={() => visit('recap')}>Continue to recap</button>
        <p className="hint">Discovery Coverage uses a live Judith creature-death search. Explorer will use a matching run or start one.</p>
      </div>}
    </>}
    {journey === 'recap' && <>
      <p>Start with a deck goal, look for a concrete mechanic, and check the conditions that connect them. Bloodghast shows how extraction can misread a represented mechanic. Agent shows how discovery can leave a plausible relationship open when it cannot establish a condition.</p>
      <details>
        <summary>Would nine extra matches convince you?</summary>
        <p>The controlled comparison held the engine and input pool fixed. The reviewed rules supported the new battlefield returns.
          Reassembling Skeleton stayed recognized, and Blood Speaker’s hand return stayed excluded. Inspect unexpected changes and unresolved checks before interpreting any count increase.</p>
      </details>
      <details>
        <summary>Which evidence answers which question?</summary>
        <ul>
          <li>Identity links a specific printing to its rules source and authored definition. Inspect the native action and compare its behavior with the rules for the claim you’re making.</li>
          <li>Discovery asks what a represented relationship contributes to the selected objective. Recognized, excluded and unresolved describe different outcomes. A policy can exclude a mechanically possible relationship.</li>
          <li>Gameplay scenarios test what actually happens under stated conditions. Independent assurance requires its own scoped evidence. A missing review leaves the claim open.</li>
          <li>Commander eligibility asks whether the card may go in the deck. Strategic relevance asks whether the available mechanic helps this deck’s plan.</li>
        </ul>
      </details>
    </>}
    </>}
    <div className="primer-restart"><button onClick={() => {
      setPrediction(undefined);
      setPayment(undefined);
      setJourney('bloodghast');
      openPart(0);
    }}>Restart primer</button></div>
  </section>;
}
