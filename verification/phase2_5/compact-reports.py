#!/usr/bin/env python3
"""Keep measured summaries, evidence hashes and representative native results, never bulky history."""
import pathlib,json,hashlib,collections
root=pathlib.Path('generated/phase25');out=pathlib.Path('samples/phase2_5');out.mkdir(parents=True,exist_ok=True)
manifest=[]
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(name,d): (out/name).write_text(json.dumps(d,indent=2,sort_keys=True)+'\n')
for stage in ['baseline-pinned','final']:
 for p in sorted((root/('final-verified' if stage=='final' else stage)).glob('*.json')):
  manifest.append(dict(path=str(p),bytes=p.stat().st_size,sha256=digest(p)))
  d=json.loads(p.read_text());name=stage+'-'+p.name
  if 'objective' in d and 'results' in d:
   accepted=[r for r in d['results'] if r['verdict']=='MATCH']
   pool=d.get('mechanicalInputCards') or []
   keep={k:d[k] for k in ['argentumCommit','objective','commander','allowedIdentity','conventionalQuery','counts']}
   keep.update(eligibleInputSha256=hashlib.sha256('\n'.join(sorted(pool)).encode()).hexdigest(),inputCards=len(pool),acceptedCards=sorted({r['card'] for r in accepted}),
    results=[{k:r[k] for k in ['card','face','actionId','abilityPath','branch','operation','costOrEffect','verdict','mechanicalCompatibility','policyAcceptance','localSupport']} | {'evidenceSha256':hashlib.sha256(json.dumps(r,sort_keys=True).encode()).hexdigest()} for r in d['results']])
   if d['objective']=='create-creature-tokens':
    reviewed={'Dragon Fodder','Krenko, Mob Boss','Young Pyromancer','Hunted Horror','Big Score','Kuldotha Rebirth','Goblin Rabblemaster','Ophiomancer','Heat Shimmer'}
    reps=[r for r in d['results'] if r['card'] in reviewed]
    keep['representativeResults']=reps
    ids={r['actionId'] for r in reps};keep['representativeActions']=[a for a in d['actions'] if a['id'] in ids]
   write(name,keep)
  elif 'groups' in d:
   groups=d['groups'];write(name,{k:v for k,v in d.items() if k!='groups'}|{'uniqueStructuralGroups':len(groups),'statusGroups':dict(collections.Counter(g['supportStatus'] for g in groups)),
    'representativeGroups':[{k:v for k,v in g.items() if k!='affected'} for g in groups[:12]]})
  elif 'findings' in d:
   groups=d.get('rootIssues',[]);tasks=d.get('engineeringTasks',[])
   write(name,{k:v for k,v in d.items() if k not in ['findings','rootIssues','engineeringTasks','benchmark']}|{'groupedRootIssues':len(groups),'engineeringTaskCount':len(tasks),
    'priorityTasks':tasks[:12], 'representativeRootIssues':[{k:v for k,v in g.items() if k!='affected'} for g in groups[:12]],'representativeFindings':d['findings'][:6]})
  elif 'changes' in d:
   cs=d['changes'];group=collections.defaultdict(list)
   for c in cs:group[c.get('kind','benchmark-change')].append(c)
   write(name,{k:v for k,v in d.items() if k!='changes'}|{'totalChanges':len(cs),'representativeChangesByKind':{k:v[:3] for k,v in group.items()}})
  else: write(name,d)
p=root/'benchmark-all-colors.json'
if p.exists():
 d=json.loads(p.read_text());write('benchmark-all-colors.json',d);manifest.append(dict(path=str(p),bytes=p.stat().st_size,sha256=digest(p)))
 splits=json.loads(pathlib.Path('benchmark/phase25/splits.json').read_text());write('benchmark-splits.json',{split:{'cases':len(ids),'evaluated':sum(r['id'] in ids for r in d['cases']),'statuses':dict(collections.Counter(r['status'] for r in d['cases'] if r['id'] in ids))} for split,ids in splits.items()})
write('OUTPUT_MANIFEST.json',{'fullOutputsOmitted':True,'reason':'Reproducible full-catalog snapshots excluded to keep primary ZIP lean','outputs':manifest,'regenerate':'bash verification/phase2_5/run-reports.sh final; python3 verification/phase2_5/compact-reports.py'})
print('Compacted',len(manifest),'executed outputs into',out)
