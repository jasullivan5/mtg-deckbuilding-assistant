import type {CommanderView} from './api';
import {Concerns, EvidenceInspector} from './EvidenceInspector';
import {CardPicture} from './CardPicture';
import {CardRules} from './CardRules';

export function CommanderPanel({view, revision}: {view: CommanderView; revision?: string}) {
  return <section className="commander-summary" aria-label="Selected commander">
    <div className="commander-overview">
      {view.inspection.selectable && <CardPicture image={view.image ?? undefined}/>}
      <div>
        <h2>{view.inspection.requestedName}</h2>
        {!view.inspection.selectable && <p>{view.headline}</p>}
        {view.inspection.selectable && view.authoredCard && <>
          <dl className="card-properties">
            <div><dt>Color identity</dt><dd>{view.inspection.colorIdentity.map(c => c.charAt(0) + c.slice(1).toLowerCase()).join(', ') || 'Colorless'}</dd></div>
            <div><dt>Mana cost</dt><dd>{view.authoredCard.manaCost}</dd></div>
            <div><dt>Type</dt><dd>{view.authoredCard.typeLine}</dd></div>
          </dl>
          <CardRules lines={view.rules} text={view.authoredCard.text}/>
        </>}
        {view.inspection.blockers.map((b, i) => <p className="warning" key={i}>{b.reason}<br/>{b.nextAction}</p>)}
        <EvidenceInspector indicators={view.evidenceSummary} revision={revision}/>
        <Concerns items={view.concerns}/>
        <details><summary>Commander technical records</summary><pre>{JSON.stringify({inspection: view.inspection, image: view.image, objectiveConnection: view.objectiveConnection}, null, 2)}</pre></details>
      </div>
    </div>
  </section>;
}
