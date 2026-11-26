"""Count compiled static trigger declarations separately from interpreted grants and bodies."""
import argparse,hashlib,json,re
from collections import Counter,defaultdict
from pathlib import Path
from runpy import run_path

nodes=run_path(str(Path(__file__).with_name('inventory-legacy-exile.py')))['nodes']

def census(source):
    cards=set();faces=set();native=defaultdict(list);records=[];facts=Counter();outcomes=Counter();unknown=Counter()
    stages=0;uses=0;unsupported=Counter();body_cards=set();cohort=[]
    source_names={c['name'].split(' //')[0] for c in json.loads(Path('src/test/resources/regression/granted-trigger-sources.json').read_text())['cases']}
    for line in source.open(encoding='utf-8-sig'):
        if not line.startswith('{'):continue
        row=json.loads(line);card,face=row['physical'],row['face'];cards.add(card);faces.add((card,face))
        identities={(g['card'],g['face'],g['path']) for g in row['grants']}
        for path,node in nodes(row['script']):
            if node.get('type')!='GrantTriggeredAbility':continue
            entry=dict(card=card,face=face,path=path,source=row['source'])
            if 'target' in node or 'duration' in node:native['temporaryTriggered'].append(entry);continue
            native['staticTriggered'].append(entry)
            native['root' if re.fullmatch(r'script.staticAbilities\[\d+\]',path) else 'nested'].append(entry)
            scope=node['filter'].get('scope',{}).get('type','Battlefield') if 'filter' in node else 'AttachedTo'
            category=('aura' if row['aura'] else 'equipment' if row['equipment'] else 'otherAttached') if scope=='AttachedTo' else 'filterGroup' if scope=='Battlefield' else 'otherScope'
            native[category].append(entry)
            if (card,face,path) not in identities:native['untraversed'].append(entry)
        for grant in row['grants']:
            record=dict(card=card,face=face,path=grant['path'],id=grant['id'],kind=grant['kind'],recipientSupport=grant['recipientSupport'],eventSupport=grant['eventSupport'])
            records.append(record);unknown.update(grant['unresolved'])
            body=grant['body'];facts.update(f['kind']+':'+f['operation'] for f in body['facts'])
            outcomes.update(o['kind'] for o in grant['eventOutcomes'])
            stages+=len(body['action'].get('collectionStages',[]));uses+=len(body['action'].get('collectionUses',[]))
            unsupported.update(u['reason'] for u in body['unsupported'])
            if body['facts'] or grant['eventOutcomes']:body_cards.add(card)
            if card in source_names:cohort.append(dict(**record,bodyFacts=len(body['facts']),eventOutcomes=len(grant['eventOutcomes']),bodyUnsupported=len(body['unsupported'])))
    def units(rows):return dict(physicalCards=len({r['card'] for r in rows}),faceContainers=len({(r['card'],r['face']) for r in rows}),declarations=len(rows))
    return dict(population=dict(physicalCards=len(cards),faceContainers=len(faces)),
        native={k:units(v) for k,v in native.items()},untraversedNativePaths=native['untraversed'],
        interpretation=dict(relationships=units(records),supportedRecipients=units([r for r in records if r['recipientSupport']=='MATCH']),
            supportedEventControllers=units([r for r in records if r['recipientSupport']=='MATCH' and r['eventSupport']=='MATCH']),
            unknownRecipients=units([r for r in records if r['recipientSupport']!='MATCH']),unknownEvents=units([r for r in records if r['eventSupport']!='MATCH']),
            byKind={k:units([r for r in records if r['kind']==k]) for k in sorted({r['kind'] for r in records})},
            recipientUncertainties=dict(unknown),bodyFacts=dict(facts),bodyEventOutcomes=dict(outcomes),bodyCards=len(body_cards),
            collectionStages=stages,collectionUses=uses,bodyUnsupported=dict(unsupported)),cohort=cohort,
        inputSha256=hashlib.sha256(source.read_bytes()).hexdigest(),
        scope='Declaration/recipient/event support is structural potential. Native detection, exact supplier binding, stack persistence, source fidelity and whole-card qualification are separate. Face containers include aggregate roots; counts overlap.')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('input',type=Path);p.add_argument('output',type=Path)
    a=p.parse_args();data=census(a.input);a.output.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:v for k,v in data.items() if k not in ('cohort','untraversedNativePaths')},indent=2))
