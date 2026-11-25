"""Permit only grant evidence nested on existing static actions; no admission/fact changes."""
import argparse
import json
from pathlib import Path
from runpy import run_path

shared=run_path(str(Path(__file__).with_name('compare-counter-placement.py')))
OBJECTIVES=(*shared['OBJECTIVES'],'place-plus-one-counters')


def compare(before,after,seen=None):
    assert before.keys()==after.keys()
    assert all(before[k]==after[k] for k in before if k!='actions'),(after['commander'],after['objective'])
    grants=[]
    for old,new in zip(before['actions'],after['actions'],strict=True):
        assert old=={k:v for k,v in new.items() if k!='grantedTriggers'},new['id']
        for grant in new.get('grantedTriggers',[]):
            if seen is not None:
                if grant['id'] in seen:assert seen[grant['id']]==grant,('objective-dependent grant',grant['id'])
                else:seen[grant['id']]=grant
            assert grant['actionId']==new['id']
            body=grant['body'];action=body['action']
            assert action['associatedActionId']==new['id']
            assert body['source']=='RECEIVING_OBJECT' and body['controller']=='TRIGGER_CONTROLLER'
            assert all(f['actionId']==action['id'] for f in body['facts'])
            grants.append(grant)
    return dict(objective=after['objective'],population=after['counts']['commander_eligible_cards'],
        populationSha256=shared['population_hash'](after),unchangedResults=shared['counts'](after['results']),
        grantDeclarations=len(grants),grantCards=len({g['card'] for g in grants}),
        bodyFacts=sum(len(g['body']['facts']) for g in grants))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('baseline','candidate','output'):p.add_argument(name,type=Path)
    args=p.parse_args();result={};seen={}
    for commander in ('judith','krenko','kenrith'):
        result[commander]=[]
        for objective in OBJECTIVES:
            name=f'{commander}-{objective}.json'
            result[commander].append(compare(shared['read'](args.baseline/name),shared['read'](args.candidate/name),seen))
            print(commander,objective,'existing evidence unchanged')
    args.output.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
