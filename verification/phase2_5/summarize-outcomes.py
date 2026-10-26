#!/usr/bin/env python3
import pathlib,json,collections,hashlib
base=pathlib.Path('generated/phase25');out=pathlib.Path('samples/phase2_5');out.mkdir(parents=True,exist_ok=True)
read=lambda p:json.loads(p.read_text())
summary={};families={};regressions={}
for who in ['judith','krenko']:
 reports={x:read(base/'final-verified'/f'{who}-{x}.json') for x in 'abc'}
 old={x:read(base/'baseline-pinned'/f'{who}-{x}.json') for x in 'ab'}
 pool=reports['a']['mechanicalInputCards']
 assert all(r['mechanicalInputCards']==pool for r in reports.values())
 assert all(r['facts']==reports['a']['facts'] for r in reports.values())
 for x in 'ab':
  assert old[x]['eligibility']==reports[x]['eligibility']
  key=lambda r:(r['card'],r['face'],r['abilityPath'],r['operation'],r['branch'])
  before={key(r):r for r in old[x]['results']};after={key(r):r for r in reports[x]['results']}
  assert before.keys()==after.keys()
  fields=['verdict','mechanicalCompatibility','policyAcceptance','contributionClassification','prerequisites','nativeOperation']
  changes={f:sum(before[k][f]!=after[k][f] for k in before) for f in fields}
  regressions[who+'-'+x]=dict(eligiblePoolUnchanged=True,pathwayIdentitiesUnchanged=True,changedFields=changes,counts=read(base/'final-verified'/f'{who}-{x}-diff.json')['counts'],transitions=read(base/'final-verified'/f'{who}-{x}-diff.json')['transitions'])
 summary[who]={x:r['counts'] for x,r in reports.items()}
 fs=[f for f in reports['c']['facts'] if f['kind']=='CREATE_TOKEN'];rs=reports['c']['results']
 families[who]={}
 for t,values in __import__('itertools').groupby(sorted(fs,key=lambda f:f['native']['type']),lambda f:f['native']['type']):
  vs=list(values);ids={(f['card'],f['face'],f['path']) for f in vs};rows=[r for r in rs if (r['card'],r['face'],r['abilityPath']) in ids]
  families[who][t]=dict(facts=len(vs),cards=len({f['card'] for f in vs}),actions=len({f['actionId'] for f in vs}),verdicts=dict(collections.Counter(r['verdict'] for r in rows)),representatives=[f['card']+'|'+f['path'] for f in vs[:5]])
summary['regressions']=regressions
summary['tokenStructuralReach']=families
summary['benchmark']=read(base/'benchmark-all-colors.json')['numerators']
summary['benchmarkDenominators']=read(base/'benchmark-all-colors.json')['denominators']
summary['limitations']=['Counts measure this pinned authored corpus and Commander pool, not normalized strategic quality or whole-MTG recall.','A/B facts gain token evidence; no change in their objective pathways/decisions is required for that generic expansion.','Exact same facts are exported by A/B/C; each query uses different operation predicates; cross-objective action relationships are distinct from identical operation consumption.']
(out/'OUTCOMES.json').write_text(json.dumps(summary,indent=2,sort_keys=True)+'\n')
print(json.dumps(summary,indent=2))
