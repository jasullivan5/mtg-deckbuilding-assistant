import type {RulesLine} from './api';

export function CardRules({lines, text, selected, onSelect}: {
  lines?: RulesLine[]; text: string; selected?: string; onSelect?: (id: string) => void;
}) {
  const shown = lines?.length ? lines : text.split('\n').filter(Boolean).map(line => ({text: line, actionId: null, relevance: null}));
  return <div className="rules-text">{shown.map((line, i) => {
    const marker = line.relevance && <span role="img" className={`objective-marker ${line.relevance}`}
      aria-label={line.relevance === 'MATCH' ? 'Relevant to this objective' : 'Relevance unresolved'}
      title={line.relevance === 'MATCH' ? 'Relevant to this objective' : 'Relevance unresolved'}>{line.relevance === 'MATCH' ? '◆' : '?'}</span>;
    const content = <>{marker}<span>{line.text}</span></>;
    return onSelect && line.actionId ? <button className="rules-line" type="button" key={i}
      aria-pressed={selected === line.actionId} onClick={() => onSelect(line.actionId!)}>{content}</button> :
      <p className="rules-line" key={i}>{content}</p>;
  })}</div>;
}
