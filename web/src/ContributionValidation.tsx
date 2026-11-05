import {useState} from 'react';
import {CardPicture} from './CardPicture';
import {retainedPrintingImage} from './retainedPrintingImage';
import {contributionCases, families, roleDefinitions} from './contributionCases';
import {engine, sources} from './contribution-sources.json';
import cohort from '../../src/main/resources/discovery-coverage-v1.json';

const featured = ['goblin-chieftain', 'cranial-plating', 'scute-swarm', 'flamewake-phoenix', 'zulaport-cutthroat', 'coat-of-arms'];
const heldOut = ['goblin-piledriver', 'tragic-slip'];
const views = [...families, 'Events versus states', 'Held-out controls'];

export default function ContributionValidation() {
  const [view, setView] = useState(0);
  const [selected, setSelected] = useState<string>(contributionCases[0].id);
  const [lifeGain, setLifeGain] = useState(false);
  const casesFor = (index: number) => contributionCases.filter(row => index === 3
    ? featured.includes(row.id) : index === 4 ? heldOut.includes(row.id) : row.family === index);
  const cases = casesFor(view);
  const row = cases.find(item => item.id === selected) ?? cases[0];
  const lens = lifeGain && row.lifeGainLens ? row.lifeGainLens : row;
  const source = sources[row.id];
  const discovery = cohort.cards.find(card => card.card === source.name);
  const pickView = (index: number) => {
    setView(index);
    setLifeGain(false);
    setSelected(index === 3 ? featured[0] : casesFor(index)[0].id);
  };

  return <section id="contribution-validation" className="detail primer contribution-study" aria-labelledby="contribution-title">
    <p className="eyebrow">SOURCE-REVIEWED CONCEPTUAL EXPERIMENT</p>
    <h2 id="contribution-title">How do cards contribute to a strategy?</h2>
    <p>Choose a research comparison or start with <strong>Events versus states</strong>. Both provisional models use the same ability and game conditions. These controls do not run discovery.</p>
    <nav className="categories" aria-label="Contribution study views">
      {views.map((label, index) => <button key={label} aria-pressed={index === view} onClick={() => pickView(index)}>{label}</button>)}
    </nav>
    <label className="contribution-select">Reviewed card
      <select value={row.id} onChange={event => {setSelected(event.target.value); setLifeGain(false);}}>
        {cases.map(item => <option key={item.id} value={item.id}>{sources[item.id].name}</option>)}
      </select>
    </label>
    <section aria-labelledby="contribution-card">
      <h3 id="contribution-card">{source.name}</h3>
      {heldOut.includes(row.id) && <p className="eyebrow">HELD-OUT CONTROL · CRITERIA SET BEFORE EVALUATION</p>}
      {row.lifeGainLens && <div className="actions" role="group" aria-label="Objective lens">
        <button aria-pressed={!lifeGain} onClick={() => setLifeGain(false)}>Creature-death lens</button>
        <button aria-pressed={lifeGain} onClick={() => setLifeGain(true)}>Life-gain lens</button>
      </div>}
      <div className="primer-card">
        <CardPicture key={source.printingId} image={retainedPrintingImage(source)}/>
        <div>
          <p className="eyebrow">{families[row.family]} · ILLUSTRATED MECHANICS</p>
          <ol className="primer-flow">
            <li><strong>Objective requirement</strong>{lens.need}</li>
            <li><strong>Capability</strong>{row.mechanic}</li>
            <li><strong>How it helps</strong>{lens.connection}</li>
          </ol>
          <p><strong>Prerequisites.</strong> {row.prerequisites}</p>
        </div>
      </div>
      {row.complement && <p className="primer-reveal"><strong>What the strategy supplies.</strong> {row.complement}</p>}
      <div className="contribution-models">
        <section aria-label="Model A"><h4>Model A · Five roles</h4>
          <p className="contribution-roles">{[...row.roles, ...(row.leverage ? ['Leverage State'] : [])].join(' + ')}</p>
          <p>{row.five}</p>
        </section>
        <section aria-label="Model B"><h4>Model B · Four roles + state properties</h4>
          <p className="contribution-roles">{row.roles.join(' + ')}</p>
          <p>{row.state}</p>
        </section>
      </div>
      <p className="hint"><strong>Discovery evidence.</strong> {row.discovery}</p>
      <p className="hint"><strong>Native evidence.</strong> {row.native}</p>
      <details key={row.id}>
        <summary>Printing, source and evidence scope</summary>
        <p className="card-text">{source.oracleText || 'The reviewed card has no rules text.'}</p>
        <p>{source.set.toUpperCase()} #{source.collectorNumber} · <a href={source.cardUrl}>Source printing</a>{source.nativePath &&
          <> · <a href={`https://github.com/jasullivan5/argentum-engine/blob/${engine}/${source.nativePath}`}>Native definition at reviewed revision</a></>}</p>
        {discovery && <p><strong>M3.1 retained review.</strong> {discovery.diagnosis} To inspect a live result, run Judith with the creature-death objective and open Discovery Coverage.</p>}
        <p>This study reviewed selected instructions and source bindings. It includes no fresh gameplay execution or independent qualification.</p>
        <p className="hint">Native review engine <code>{engine}</code> · Printing ID <code>{source.printingId}</code></p>
      </details>
    </section>
    <details>
      <summary>Compare the role definitions</summary>
      <ul>{roleDefinitions.map(([name, description]) => <li key={name}><strong>{name}.</strong> {description}</li>)}</ul>
    </details>
    <details>
      <summary>What does the sample suggest?</summary>
      <p><strong>Provisional recommendation.</strong> Revise the boundary. Make Leverage State a visible relationship from an established state to a capability, while Provide, Facilitate, Respond to Event and Modify describe what the ability does. Phoenix shows why that relationship matters even when the return never gets bigger.</p>
      <p>Four roles plus structured state dependencies could expose the same complementary cards if the system can join a state supplier to a consumer at the right time, for the right player and object. A fifth label makes the question visible but does not perform that join. Generic mana remains a cost. Supplying red mana could matter to a strategy that reliably solves that particular constraint.</p>
      <p>The original sample favored four roles when it emphasized scaling. The expanded comparison makes binary access and maintaining a state explicit. Exploration’s permission still sits between Facilitate and Modify. Product Studio can judge whether a visible state relationship earns a fifth role in the experience.</p>
    </details>
  </section>;
}
