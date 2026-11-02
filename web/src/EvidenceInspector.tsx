import {useId, useState} from 'react';
import type {EvidenceIndicator, ResearchConcern} from './api';

function Lines({title, lines}: {title: string; lines: string[]}) {
  return lines.length > 0 && <><h5>{title}</h5><ul>{lines.map((line, i) => <li key={i}>{line}</li>)}</ul></>;
}
export function EvidenceInspector({indicators, revision}: {indicators: EvidenceIndicator[]; revision?: string}) {
  const [selected, setSelected] = useState<string | null>(null);
  const id = useId();
  const current = indicators.find(s => s.id === selected);
  const help: Record<string, string> = {
    mechanic: 'Checks how a mechanic can contribute to the chosen objective.',
    source: 'Records comparisons between programmed card behavior and retained Magic rules sources.',
    execution: 'Records outcomes from engine test scenarios.',
    independent: 'Records a separate review of the interpretation and its supporting evidence.',
  };
  const sourceLink = (path: string) => {
    if (path.startsWith('https://github.com/') || path.startsWith('https://scryfall.com/')) return path;
    if (revision && /^(src\/main\/|src\/test\/)/.test(path) && !path.split('/').includes('..'))
      return `https://github.com/jasullivan5/mtg-deckbuilding-assistant/blob/${revision.replace('+dirty', '')}/${path}`;
    return null;
  };
  return <div className="evidence-inspector">
    <div className="evidence-buttons" role="group" aria-label="Evidence and verification">
      {indicators.map(item => <button key={item.id} type="button" aria-expanded={selected === item.id}
        title={help[item.id]} aria-description={help[item.id]}
        aria-controls={`${id}-panel`} className={`evidence-button ${item.state} ${item.historical ? 'historical' : ''}`}
        onClick={() => setSelected(selected === item.id ? null : item.id)}>
        <span><span aria-hidden="true">{item.state === 'ESTABLISHED' ? item.historical ? '◷ ' : '✓ ' : item.state === 'KNOWN_PROBLEM' ? '! ' : '· '}</span>{item.label}</span>
        <small>{item.status}</small>
      </button>)}
    </div>
    {current && <section id={`${id}-panel`} aria-label={`${current.label} evidence`} className="evidence-panel">
      <h4>{current.label}</h4>
      {!!current.fields?.length && <dl className="evidence-fields">{current.fields.map((field, i) => {
        const url = field.reference && sourceLink(field.reference.path);
        return <div key={i}><dt>{field.label}</dt><dd>{url ? <a href={url} target="_blank" rel="noreferrer">{field.value}</a> : field.value}</dd></div>;
      })}</dl>}
      <details className="technical-evidence"><summary>Technical evidence and recorded interactions</summary>
      <p className="evidence-scope">{current.scope}</p>
      <p>{current.explanation}</p>
      <Lines title="What is recorded" lines={current.established}/>
      <Lines title="What this does not establish" lines={current.limits}/>
      <Lines title="What would improve this evidence" lines={current.nextSteps}/>
      {current.references.length > 0 && <><h5>Supporting records</h5><ul className="record-links">{current.references.map((ref, i) => {
        const url = sourceLink(ref.path);
        return <li key={i}>{url ? <a href={url} target="_blank" rel="noreferrer">{ref.label}</a> : <span>{ref.label}</span>}
          <details><summary>Exact reference and binding</summary><code>{ref.path}</code>{ref.sha256 && <p>SHA256: <code>{ref.sha256}</code></p>}</details>
        </li>;
      })}</ul></>}
      {current.recordedEngine && <details><summary>Recorded and current versions</summary>
        <p>Recorded engine: <code>{current.recordedEngine}</code><br/>Current engine: <code>{current.currentEngine}</code></p>
        {current.historical && <p>The recorded run used a different engine revision. It is not a fresh execution on this build.</p>}
      </details>}
      </details>
    </section>}
  </div>;
}
export function Concerns({items}: {items: ResearchConcern[]}) {
  return <>{items.map((item, i) => <article key={i} className={item.state === 'KNOWN_PROBLEM' ? 'recorded-problem' : 'research-question'}>
    <h4>{item.reading ? 'Recorded card concern' : item.title}</h4><p>{item.reading || item.detail}</p>
    <details><summary>Investigation details</summary><p>{item.scope}</p>{item.reading && <p>{item.detail}</p>}<p>{item.nextStep}</p></details>
  </article>)}</>;
}
