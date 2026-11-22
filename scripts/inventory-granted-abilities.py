"""Census compiled declarations and separately count production grant evidence.

No Oracle/name matching establishes grant relationships. Bulk exact native paths stay generated.
"""
import argparse
from collections import Counter, defaultdict
import hashlib
import json
from pathlib import Path
import re
from runpy import run_path

nodes=run_path(str(Path(__file__).with_name('inventory-legacy-exile.py')))['nodes']


def census(source):
    cards,faces=set(),set()
    declarations=defaultdict(list)
    relationships=[]
    body_facts=Counter()
    body_unsupported=Counter()
    problems=Counter()
    collection_stages=Counter()
    collection_uses=Counter()
    collection_cards=set()
    fact_cards=set()
    cohort_names={c['name'] for c in json.loads(Path('src/test/resources/regression/granted-ability-sources.json').read_text())['cases']}
    cohort=[]
    for line in source.open(encoding='utf-8-sig'):
        if not line.startswith('{'):continue
        row=json.loads(line)
        card,face=row['physical'],row['face']
        cards.add(card);faces.add((card,face))
        for path,node in nodes(row['script']):
            kind=node.get('type')
            categories=[]
            if kind in ('GrantActivatedAbility','GrantTriggeredAbility','GrantStaticAbility'):
                # SDK static/effect classes share serial names; effects explicitly carry target/duration.
                effect='target' in node or 'duration' in node
                if kind=='GrantActivatedAbility':
                    categories.append('temporaryActivatedGrants' if effect else 'staticActivatedGrants')
                    if not effect:
                        # GrantActivatedAbility's omitted filter defaults to attachedCreature;
                        # an explicit GroupFilter's omitted scope defaults to Battlefield.
                        scope=node['filter'].get('scope',{}).get('type','Battlefield') if 'filter' in node else 'AttachedTo'
                        if scope=='AttachedTo':categories.append('auraActivatedGrants' if row['aura'] else 'equipmentActivatedGrants' if row['equipment'] else 'otherAttachmentActivatedGrants')
                        elif scope=='Battlefield':categories.append('filterGroupActivatedGrants')
                        else:categories.append('otherStaticActivatedGrants')
                        if re.fullmatch(r'script.staticAbilities\[\d+\]',path):categories.append('rootStaticActivatedGrants')
                        else:categories.append('nestedStaticActivatedGrants')
                    categories.append('explicitActivatedBodies')
                elif kind=='GrantTriggeredAbility':categories.append('temporaryTriggeredGrants' if effect else 'staticTriggeredGrants')
                else:categories.append('otherStaticAbilityGrants' if not effect else 'temporaryStaticAbilityGrants')
            if kind=='GrantActivatedAbilityToGroup':categories.extend(('temporaryGroupActivatedGrants','explicitActivatedBodies'))
            if kind=='GrantStateTriggeredAbility':categories.append('stateTriggeredGrants')
            if kind in ('HasAllActivatedAbilitiesOfCards','GainActivatedAbilitiesOfPermanents','GainAllActivatedAbilitiesOf'):
                categories.append('donorDerivedActivatedGrants')
            for key,category in (('addedActivatedAbilities','copyAddedActivatedAbilities'),('addedTriggeredAbilities','copyAddedTriggeredAbilities')):
                for i,_ in enumerate(node.get(key,[])):
                    declarations[category].append(dict(card=card,face=face,path=f'{path}.{key}[{i}]',source=row['source']))
            for category in categories:
                declarations[category].append(dict(card=card,face=face,path=path,source=row['source']))
        for grant in row['grants']:
            identity=dict(card=card,face=face,path=grant['path'],id=grant['id'],kind=grant['kind'],support=grant['support'])
            relationships.append(identity)
            problems.update(grant['unresolved'])
            body=grant['body']
            if body['facts']:fact_cards.add(card)
            if body['action'].get('collectionStages') or body['action'].get('collectionUses'):collection_cards.add(card)
            body_facts.update(f['kind'] for f in body['facts'])
            body_unsupported.update(u.get('reason','UNKNOWN') for u in body['unsupported'])
            collection_stages.update(s['membership'] for s in body['action'].get('collectionStages',[]))
            collection_uses.update(u['support'] for u in body['action'].get('collectionUses',[]))
            if card in cohort_names:cohort.append(dict(**identity,bodyFactKinds=[f['kind'] for f in body['facts']],bodyUnsupported=len(body['unsupported']),collectionStages=len(body['action'].get('collectionStages',[])),collectionUses=len(body['action'].get('collectionUses',[]))))
    def units(rows):return dict(physicalCards=len({r['card'] for r in rows}),authoringContainers=len({(r['card'],r['face']) for r in rows}),declarations=len(rows))
    supported=[r for r in relationships if r['support']=='MATCH']
    return dict(population=dict(physicalCards=len(cards),authoringContainers=len(faces)),
        native={k:units(v) for k,v in sorted(declarations.items())},
        interpretation=dict(relationships=units(relationships),supportedRecipientBindings=units(supported),
            unknownRecipientBindings=units([r for r in relationships if r['support']!='MATCH']),
            byKind={kind:units([r for r in relationships if r['kind']==kind]) for kind in sorted({r['kind'] for r in relationships})},
            unresolvedReasons=dict(problems),newBodyFacts=dict(body_facts),bodyUnsupported=dict(body_unsupported),
            bodyFactCards=len(fact_cards),collectionBodyCards=sorted(collection_cards),
            collectionStages=dict(collection_stages),collectionUses=dict(collection_uses)),cohort=cohort,
        inputSha256=hashlib.sha256(source.read_bytes()).hexdigest(),
        boundaries='Recipient binding MATCH is structural; body support, actual activation, source fidelity and whole-card qualification are independent. Copy-added counts are native exception declarations, not a universal copy-ability census.')


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('input',type=Path);p.add_argument('output',type=Path)
    args=p.parse_args();result=census(args.input)
    args.output.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:v for k,v in result.items() if k not in ('cohort',)},indent=2))
