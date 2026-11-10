#!/usr/bin/env python3
"""Compare retained exact reports and project only matched history-only legacy gates."""
import argparse
import collections
import hashlib
import json
from pathlib import Path

def load(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def sha(path):
    digest=hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda:stream.read(1024*1024),b''):
            digest.update(block)
    return digest.hexdigest()

def summarize(report):
    rows=report['results']
    return {verdict:{'paths':len(found),'cards':len({r['card'] for r in found})}
            for verdict in ('MATCH','UNKNOWN','NO_MATCH')
            for found in [[r for r in rows if r['verdict']==verdict]]}

def projection(current, history):
    assert current['mechanicalInputCards']==history['mechanicalInputCards']
    assert current['eligibility']==history['eligibility']
    assert current['argentumCommit']==history['argentumCommit']
    state={}
    for r in history['results']:
        d=r['stateDependency']
        t=(d.get('incomingEvent') or {}).get('trigger',{})
        death_trigger=(t.get('type')=='ZoneChangeEvent' and
                       t.get('from')=='Battlefield' and t.get('to')=='Graveyard')
        if r['verdict']=='MATCH' and d['checkpoint']=='RESOLUTION' and not death_trigger:
            state.setdefault((r['actionId'],d['conditionPath']),[]).append(r['nativeOperation'])
    accepted=[r for r in current['results'] if r['verdict']=='MATCH']
    legacy=[r for r in accepted if r['branch']=='DEATH_ENHANCED_REMOVAL']
    withheld=[r for r in legacy if r['nativeOperation'] in state.get((r['actionId'],r['abilityPath']),[])]
    retained=[r for r in accepted if r not in withheld]
    return dict(legacyAccepted=len(legacy),notProvenHistoryOnly=len(legacy)-len(withheld),
                matchedPathwaysBefore=len(accepted),matchedPathwaysProjected=len(retained),
                matchedCardsBefore=len({r['card'] for r in accepted}),
                matchedCardsProjected=len({r['card'] for r in retained}),
                withheld=[{k:r[k] for k in ('card','actionId','abilityPath','dependency','nativeOperation')} for r in withheld],
                onlyTheseMatchedCardsWouldLeave=sorted({r['card'] for r in accepted}-{r['card'] for r in retained}))

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('legacy-dir','candidate-dir','kenrith-before','output'):
        p.add_argument('--'+name,type=Path,required=True)
    a=p.parse_args()
    reports={}
    for before in sorted(a.legacy_dir.glob('*.json')):
        after=a.candidate_dir/before.name
        before_sha,after_sha=sha(before),sha(after)
        c=load(after)
        differences=[]
        if before_sha!=after_sha:
            b=load(before)
            differences=sorted(k for k in b.keys()|c.keys() if b.get(k)!=c.get(k))
            del b
        reports[before.stem]=dict(exact=not differences,differences=differences,
            eligible=len(c['mechanicalInputCards']),results=summarize(c),beforeSha256=before_sha,afterSha256=after_sha)
        assert not differences,(before,differences)
        del c
    assert len(reports)==8,reports.keys()
    state={}
    for name in ('judith','krenko','kenrith'):
        path=a.candidate_dir/(f'{name}-creature-died-this-turn.json' if name!='kenrith' else 'kenrith-history.json')
        death=a.candidate_dir/(f'{name}-own-nontoken-creature-deaths.json' if name!='kenrith' else 'kenrith-death.json')
        baseline=a.kenrith_before if name=='kenrith' else a.legacy_dir/death.name
        death_sha,base_sha=sha(death),sha(baseline)
        h,e=load(path),load(death)
        if death_sha!=base_sha:
            assert e==load(baseline),(name,'death baseline drift')
        state[name]=dict(historySha256=sha(path),deathSha256=death_sha,engine=h['argentumCommit'],
            deathBaselineSha256=base_sha,deathBaselineExact=True,
            eligible=len(h['mechanicalInputCards']),history=summarize(h),
            retainedDependencyScopes=dict(collections.Counter(d['scope'] for d in h['stateDependencies'])),
            matchedCards=sorted({r['card'] for r in h['results'] if r['verdict']=='MATCH'}),
            unresolvedCards=sorted({r['card'] for r in h['results'] if r['verdict']=='UNKNOWN'}),
            proposedProjection=projection(e,h))
        del h,e
    a.output.parent.mkdir(parents=True,exist_ok=True)
    a.output.write_text(json.dumps(dict(legacy=reports,state=state,scope='Diagnostic only; old admissions are unchanged. Unproven legacy rows are retained.'),indent=2)+'\n',encoding='utf-8')
    for name,r in state.items():
        print(name,r['history'],len(r['proposedProjection']['withheld']),r['proposedProjection']['onlyTheseMatchedCardsWouldLeave'])

if __name__=='__main__':main()
