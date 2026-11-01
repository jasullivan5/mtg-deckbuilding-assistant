import type {CommanderView} from './api';
import {Concerns, EvidenceInspector} from './EvidenceInspector';
import {CardPicture} from './CardPicture';

export function CommanderPanel({view, revision}: {view: CommanderView; revision?: string}) {
  return <section className="commander-summary" aria-label="Selected commander">
    <div className="commander-overview">
      {view.inspection.selectable && <CardPicture image={view.image ?? undefined}/>}
      <div>
        <h2>{view.inspection.requestedName}</h2>
        <p className="commander-identity">{view.headline}{view.inspection.selectable && ' color identity'}</p>
        {view.inspection.selectable && view.authoredCard && <>
          <p>{view.authoredCard.manaCost} · {view.authoredCard.typeLine}</p>
          <p className="commander-rules">{view.authoredCard.text}</p>
        </>}
        {view.objectiveConnection && <p className="contribution">{view.objectiveConnection}</p>}
        {view.inspection.blockers.map((b, i) => <p className="warning" key={i}>{b.reason}<br/>{b.nextAction}</p>)}
        <EvidenceInspector indicators={view.evidenceSummary} revision={revision}/>
        <Concerns items={view.concerns}/>
        <details><summary>Commander technical records</summary><pre>{JSON.stringify({inspection: view.inspection, image: view.image}, null, 2)}</pre></details>
      </div>
    </div>
  </section>;
}
