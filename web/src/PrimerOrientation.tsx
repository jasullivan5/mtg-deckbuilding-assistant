import {useState} from 'react';
import comparison from './integration-change-evidence.json';
import cohort from '../../src/main/resources/discovery-coverage-v1.json';

const contributions = [
  {role: 'Cause an event', card: 'Viscera Seer', mechanic: 'Sacrifice a creature to scry 1. Sacrificing a nontoken creature can supply the death.', requirement: 'You need a creature you can sacrifice. For this objective, it must be nontoken and reach the graveyard.', coverage: 'The death query identifies this sacrifice contribution. This does not verify the whole ability.'},
  {role: 'Supply a creature', card: 'Reassembling Skeleton', mechanic: 'Return Skeleton from your graveyard so you have a creature to sacrifice later.', requirement: 'Pay {1}{B} while Skeleton is in the graveyard. It returns tapped. A separate sacrifice ability can use it later.', coverage: 'Reanimation finds the return; renewable creature fuel is not yet recognized by the death query.'},
  {role: 'Respond with draw', card: 'Grim Haruspex', mechanic: 'Draw when another nontoken creature you control dies.', requirement: 'The death has to happen first. Haruspex’s draw is outside the present death query’s payoff coverage.', coverage: 'This illustrates a mechanical relationship, not a current death-payoff discovery result.'},
  {role: 'Ordinary material', card: 'Walking Corpse', mechanic: 'A creature body can pay a sacrifice cost even without a special ability.', requirement: 'You still need the creature on the battlefield and a way to sacrifice it. The card adds no distinctive sacrifice or return ability.', coverage: 'Participating as ordinary material does not establish a distinctive contribution.'},
];

export function PrimerPurpose() {
  const [selected, setSelected] = useState(0);
  const example = contributions[selected];
  return <>
    <p>We’re building a way to explain why a card could matter in a specific Commander strategy. Start with a plan: <strong>My nontoken creatures die.</strong> Compare three distinct mechanics with a creature that can participate as ordinary material.</p>
    <div className="primer-choices" role="group" aria-label="Ways to support a creature-death plan">
      {contributions.map(({role, card}, index) => <button key={role} aria-pressed={index === selected} onClick={() => setSelected(index)}>
        <strong>{role}</strong><small>{card}</small>
      </button>)}
    </div>
    <div className="primer-reveal" aria-live="polite">
      <h4>{example.card}</h4>
      <p>{example.mechanic}</p>
      <p><strong>What has to be true?</strong> {example.requirement}</p>
    </div>
    <p><strong>Current coverage.</strong> {example.coverage}</p>
    <p>These relationships describe how a mechanic could support this objective. They can overlap without becoming permanent card types. Consuming a creature describes a payment; renewability and repeatability describe resource qualities. The app does not produce general relationship classifications.</p>
  </>;
}

const stages = [
  {label: 'Card identity and rules', status: 'Available for reviewed sources', question: 'Which card and which rules are we working from?',
    example: 'The reviewed Bloodghast printing supplies the Oracle wording for its optional landfall return.',
    limit: 'A printing association establishes which card the evidence describes. Rules fidelity requires comparing the relevant behavior.',
    connection: 'The source gives us an expectation to compare with the native definition.'},
  {label: 'Native definitions', status: 'Available for authored cards', question: 'How is the card written in a form the engine can use?',
    example: 'Bloodghast’s definition expresses a land-entry trigger active in the graveyard and a move to the battlefield.',
    limit: 'A definition can be missing, incomplete or mistranslate an instruction.',
    connection: 'The same definition is input to gameplay and to mechanical extraction.'},
  {label: 'Mechanical facts and capabilities', status: 'Implemented within supported structures', question: 'What reusable facts can we read from the native ability?',
    example: 'Bloodghast can move itself from the graveyard to the battlefield under the landfall condition.',
    limit: 'Costs, conditions, events and state changes need their original bindings. Some structures are still unread or only partly interpreted.',
    connection: 'The objective uses those facts to decide which relationships it can recognize.'},
  {label: 'Objective discovery and selective layering', status: 'Four queries implemented; relationship filtering and layering intended', question: 'Which contributions does this objective need?',
    example: 'A battlefield return can fit reanimation. Blood Speaker’s return to hand falls outside that check.',
    limit: 'A structural MATCH is conditional potential. General relationship filtering and selective objective composition are not implemented.',
    connection: 'Mechanical requirements can guide complementary questions without proving that two cards work together.'},
  {label: 'Experimental deck evaluation', status: 'Future research hypothesis', question: 'Does this choice help this deck under controlled conditions?',
    example: 'Compare a deck with and without a renewable creature resource, measuring access, objective progress and resilience under disruption.',
    limit: 'No deck evaluator, objective-aware pilot or deck simulation is implemented. Results would depend on mechanics, decision policies, baselines and environment.',
    connection: 'We want to test how far mechanics, objectives and empirical comparisons can take us without a separate card-to-card strategic synergy model.'},
  {label: 'Commander eligibility', status: 'Separate gate before discovery', question: 'May this card go in the selected Commander deck?',
    example: 'Dryad Arbor’s green color identity places it outside Judith’s black-red pool.',
    limit: 'Unknown eligibility can exclude a card from the search. An eligible card can still have unresolved mechanics.',
    connection: 'Format legality and full color identity determine which cards enter the objective search.'},
  {label: 'Evidence and verification', status: 'Scoped checks across the system', question: 'What evidence supports this particular claim?',
    example: 'Source comparison checks a represented instruction against the rules. Execution tests observe gameplay under stated conditions. Independent assurance requires separate review of the exact evidence and requirements.',
    limit: 'A scenario may test one branch. A source match, a scenario pass and an independent decision can exist separately and apply to different claims.',
    connection: 'These checks can inform any part of the map and can progress independently.'},
];

const objectiveExamples = [
  {label: 'Put +1/+1 counters', title: 'Put +1/+1 counters on my creatures',
    mechanic: 'Travel Preparations can place counters on your chosen creatures. Goblin Chieftain gives other Goblins you control a continuous bonus; it does not place counters.',
    boundary: 'Counter type, recipient and actual placement matter. Hardened Scales changes how many counters another placement puts there; it does not start that placement.'},
  {label: 'Increase power', title: 'Increase my creatures’ power',
    mechanic: 'Travel Preparations can increase power through +1/+1 counters. Goblin Chieftain can increase it through a continuous bonus. Both can help this question through different mechanics.',
    boundary: 'Choose the right recipients and keep conditions and timing. Wall of Roots puts a 0/−1 counter on itself for mana; that does not increase power. Spending a +1/+1 counter is also different from placing one.'},
  {label: 'Reach power 4', title: 'Control a creature with power 4 or greater',
    mechanic: 'A +1/+1 counter can raise a 3-power creature to 4. Chieftain can do the same for another 3-power Goblin you control while its bonus applies. A sufficiently large creature can also establish the state without a power increase.',
    boundary: 'Flamewake Phoenix uses this threshold; it does not establish it. The condition must hold when your combat begins and again when its return trigger resolves. A boost that arrives after attacks are declared is too late for that trigger.'},
];

export function PrimerObjectives() {
  const [selected, setSelected] = useState(0);
  const example = objectiveExamples[selected];
  return <>
    <p>Start with a precise question about an <strong>event</strong>, a <strong>state</strong> or a <strong>game element</strong>. Then ask how a mechanic causes, responds to, establishes, supplies, consumes or changes it. These relationships can overlap within one ability.</p>
    <details><summary>Events, states, game elements and effects</summary>
      <dl className="evidence-fields">
        <div><dt>Events</dt><dd>Things happening, such as a creature dying. Causing a death, triggering from it and checking whether one happened earlier are different relationships.</dd></div>
        <div><dt>States</dt><dd>Conditions such as controlling a creature with power 4. A condition can enable an ability, select its result or change its size; when it is checked still matters.</dd></div>
        <div><dt>Game elements</dt><dd>Things mechanics operate on: creatures, cards, tokens, counters, mana, players, zones or permissions. This working umbrella is broader than a Magic game object; these things follow different rules.</dd></div>
        <div><dt>Effects and outcomes</dt><dd>What the instructions do and what can follow. They connect the subjects rather than requiring an equivalent fourth kind of objective. Asking for a particular change, such as counter placement, is still useful.</dd></div>
      </dl>
    </details>
    <p className="eyebrow">CONCEPTUAL EXAMPLES · NOT NEW DISCOVER QUERIES</p>
    <div className="primer-choices" role="group" aria-label="Compare objective questions">
      {objectiveExamples.map(({label}, index) => <button key={label} aria-pressed={selected === index} onClick={() => setSelected(index)}>{label}</button>)}
    </div>
    <div className="primer-reveal" aria-live="polite">
      <h4>{example.title}</h4><p>{example.mechanic}</p><p><strong>What must stay precise?</strong> {example.boundary}</p>
    </div>
    <p>Likewise, Pitiless Plunderer responds to a death and supplies Treasure. The same ability can matter to a death question and a resource question. This illustrates a relationship, not a current death-to-Treasure discovery result.</p>
    <p>Selective layering could take creature-producing contributions from a token objective without taking all its other contributions. Grouping candidates does not prove that their objects, resources, timing and conditions connect, or that the combination is valuable.</p>
    <details><summary>What happens to the earlier role names?</summary>
      <p>Provider, event responder, event facilitator, state facilitator, state responder and modifier remain possible shorthand for relationships. The earlier role studies are retained. The bounded comparison favors precise relationships as the foundation, with optional role descriptions; it establishes neither a final taxonomy nor a general classifier.</p>
    </details>
    <p className="hint">This is a working research direction. Relationship filters, objective construction and selective layering are not implemented. Chieftain and Wall of Roots are source-only examples in this study; understanding an illustration does not establish extraction or gameplay support.</p>
  </>;
}

export function PrimerMap() {
  const [selected, setSelected] = useState(0);
  const stage = stages[selected];
  const maturity = ['Reviewed sources', 'Authored catalog', 'Implemented · partial', 'Queries now · layering intended', 'Hypothesis · future work'];
  const control = (index: number) => <button aria-pressed={selected === index} onClick={() => setSelected(index)}>
    <span>{stages[index].label}</span>{maturity[index] && <small className="stage-maturity">{maturity[index]}</small>}
  </button>;
  return <>
    <p>Select a step from today’s mechanical research toward future deck experiments. This is a conceptual map, not a claim that every stage is complete. Eligibility filters what enters discovery; evidence can inform any step.</p>
    <ol className="primer-map" aria-label="From card rules to deck choices">
      {stages.slice(0, 5).map(({label}, index) => <li key={label}>{control(index)}</li>)}
    </ol>
    <div className="primer-support" role="group" aria-label="What supports the reasoning">
      {control(5)}{control(6)}
    </div>
    <div className="primer-reveal" aria-live="polite">
      <p className="eyebrow">{stage.status}</p>
      <h4>{stage.question}</h4>
      <p>{stage.example}</p>
      <p><strong>Where to be careful.</strong> {stage.limit}</p>
      <p><strong>Connection.</strong> {stage.connection}</p>
    </div>
  </>;
}

const {population, totals, revisions, objectives} = comparison;
const capabilityTopics = ['Demonstrated', 'Measured', 'Known limits', 'Unmeasured', 'Future research'];

export function PrimerCapabilities() {
  const [selected, setSelected] = useState(0);
  const observations = [
    <><p>The Explorer can filter by Commander legality and full color identity, then search supported mechanics for creature deaths, creature tokens, reanimation or creature cards in our graveyard. You can inspect the card and the specific relationship it found.</p>
      <p>For example, the death query recognizes Viscera Seer’s sacrifice, and the reanimation query recognizes Skeleton’s paid return. Retained source comparisons and bounded engine scenarios exist for selected cards. The Bitterblossom ledger records a historical source review and execution pass with provisional evidence.</p></>,
    <><p><strong>Historical reanimation comparison.</strong> On the same {population.toLocaleString()} eligible Judith card inputs, accepted results changed from <strong>{totals.before.cards} → {totals.after.cards} distinct cards</strong> and <strong>{totals.before.pathways} → {totals.after.pathways} pathways</strong>. The correction changed {objectives.reanimation.verdictChanges} verdicts. The engine and input pool were held fixed.</p>
      <p>M3.1 instead uses a deliberately selected {cohort.cards.length}-card Judith death-objective cohort to investigate different kinds of findings and omissions.</p></>,
    <><p>The death query recognizes selected death sources and death-enhanced removal. General draw payoffs such as Grim Haruspex and recurring fuel such as Skeleton remain outside its present coverage. Agent’s land-creature overlap remains unresolved in the reviewed finding.</p>
      <p>The Explorer can expose an unsupported instruction or missing review. Eligibility, mechanical recognition and assurance stay separate.</p></>,
    <p>These studies leave catalog-wide rules fidelity, gameplay reliability and discovery recall unmeasured. The selected cohort gives examples of gaps. Scenario passes and authored definitions describe their own scopes. Independent assurance needs evidence bound to specific requirements and revisions.</p>,
    <p>Recognizing renewable creature resources, filtering objective-relative roles and selectively layering objectives are intended research. Controlled deck experiments may investigate value without a separate pairwise strategic synergy model. These are hypotheses and future capabilities, not an authorized next increment.</p>,
  ];
  return <>
    <p>Explore what the running Explorer supports and what the recorded investigations establish.</p>
    <div className="primer-choices" role="group" aria-label="Capability evidence categories">
      {capabilityTopics.map((topic, index) => <button key={topic} aria-pressed={index === selected}
        onClick={() => setSelected(index)}>{topic}</button>)}
    </div>
    <div className="primer-reveal" aria-live="polite">
      <h4>{capabilityTopics[selected]}</h4>
      {observations[selected]}
    </div>
    {selected === 1 && <details>
      <summary>Measurement scope and revisions</summary>
      <p>Reanimation comparison applications <code>{revisions.before}</code> and <code>{revisions.after}</code>. Shared engine <code>{revisions.engine}</code>.</p>
      <p>M3.1 reviewed engine <code>{cohort.reviewedEngine}</code>. Its cohort is a set of selected examples. The live coverage view labels the actual application and engine revisions for your run.</p>
    </details>}
  </>;
}
