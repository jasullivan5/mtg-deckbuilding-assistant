import type {CommanderView, Evidence} from './api';
import {Concerns, EvidenceInspector} from './EvidenceInspector';

export function CommanderPanel({view, discovered, revision}: {view: CommanderView; discovered: Evidence | null; revision?: string}) {
  const ability = discovered?.abilities.find(a => a.verdict === 'MATCH') ?? discovered?.abilities.find(a => a.verdict === 'UNKNOWN');
  return <section className="commander-summary" aria-label="Selected commander">
    <p><b>{view.headline}</b></p>
    {view.inspection.selectable && view.authoredCard && <p className="commander-rules">{view.authoredCard.text}</p>}
    {ability && <p className="hint">Objective connection: {ability.contribution}</p>}
    {view.inspection.blockers.map((b, i) => <p className="warning" key={i}>{b.reason}<br/>{b.nextAction}</p>)}
    <details><summary>Commander evidence</summary>
      <EvidenceInspector indicators={ability?.evidence ?? view.evidenceSummary} revision={revision}/>
      <Concerns items={view.concerns}/>
      <details><summary>Exact selection, source and review records</summary><pre>{JSON.stringify(view.inspection, null, 2)}</pre></details>
    </details>
  </section>;
}
