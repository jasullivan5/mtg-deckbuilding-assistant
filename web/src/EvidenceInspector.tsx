import {useId, useState} from 'react';
import type {EvidenceIndicator, ResearchConcern} from './api';

function Lines({title, lines}: {title: string; lines: string[]}) {
  return lines.length > 0 && <><h5>{title}</h5><ul>{lines.map((line, i) => <li key={i}>{line}</li>)}</ul></>;
}
export function EvidenceInspector({indicators, revision}: {indicators: EvidenceIndicator[]; revision?: string}) {
  const [selected, setSelected] = useState<string | null>(null);
  const id = useId();
  const current = indicators.find(s => s.id === selected);
  const sourceLink = (path: string) => {
    if (path.startsWith('https://github.com/') || path.startsWith('https://scryfall.com/')) return path;
    if (revision && /^(src\/main\/|src\/test\/)/.test(path) && !path.split('/').includes('..'))
      return `https://github.com/jasullivan5/mtg-deckbuilding-assistant/blob/${revision.replace('+dirty', '')}/${path}`;
    return null;
  };
  return <div className="evidence-inspector">
    <div className="evidence-buttons" role="group" aria-label="Evidence and verification">
      {indicators.map(item => <button key={item.id} type="button" aria-expanded={selected === item.id}
        aria-controls={`${id}-panel`} className={`evidence-button ${item.state} ${item.historical ? 'historical' : ''}`}
        onClick={() => setSelected(selected === item.id ? null : item.id)}>
        <span><span aria-hidden="true">{item.state === 'ESTABLISHED' ? item.historical ? '◷ ' : '✓ ' : item.state === 'KNOWN_PROBLEM' ? '! ' : '· '}</span>{item.label}</span>
        <small>{item.status}</small>
      </button>)}
    </div>
    {current && <section id={`${id}-panel`} aria-label={`${current.label} evidence`} className="evidence-panel">
      <h4>{current.label}: {current.status}</h4>
      <p className="evidence-scope"><b>Scope:</b> {current.scope}</p>
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
    </section>}
  </div>;
}
export function Concerns({items}: {items: ResearchConcern[]}) {
  return <>{items.map((item, i) => <article key={i} className={item.state === 'KNOWN_PROBLEM' ? 'recorded-problem' : 'research-question'}>
    <h4>{item.title}</h4><p>{item.detail}</p><p className="hint">{item.scope}</p>
    <p><b>Next evidence needed:</b> {item.nextStep}</p>
  </article>)}</>;
}
