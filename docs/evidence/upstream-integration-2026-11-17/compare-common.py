"""Compare exact common mechanical populations; retain original reports and identity hashes."""
import argparse, collections, hashlib, json, pathlib
p=argparse.ArgumentParser(); p.add_argument('before',type=pathlib.Path);p.add_argument('after',type=pathlib.Path);p.add_argument('output',type=pathlib.Path);a=p.parse_args()
def read(path):
 raw=path.read_bytes();return json.loads(raw),hashlib.sha256(raw).hexdigest()
b,bhash=read(a.before);c,chash=read(a.after)
assert (b['commander'],b['objective'],b['conventionalQuery'])==(c['commander'],c['objective'],c['conventionalQuery'])
common=sorted(set(b['mechanicalInputCards']) & set(c['mechanicalInputCards']))
# Only literal repository URLs bound to each runtime SHA are normalized. No native tree,
# definition, historical revision, eligibility, outcome or evidence binding is rewritten.
def canonical(value,sha):
 text=json.dumps(value,sort_keys=True,ensure_ascii=False,separators=(',',':'))
 for kind in ('blob','tree'):
  text=text.replace('https://github.com/jasullivan5/argentum-engine/'+kind+'/'+sha, 'https://github.com/jasullivan5/argentum-engine/'+kind+'/RUNTIME_SHA')
 return text
def group(values,population):
 result=collections.defaultdict(list)
 for value in values:
  if isinstance(value,dict) and value.get('card') in population: result[value['card']].append(value)
 return result
common_set=set(common); eligibility_names=sorted({r['card'] for r in b['eligibility']} & {r['card'] for r in c['eligibility']}); sections={}
for key in sorted(b.keys() | c.keys()):
 if not isinstance(b.get(key),list) or not isinstance(c.get(key),list) or key in ('mechanicalInputCards','limitations','allowedIdentity'):continue
 population=eligibility_names if key=='eligibility' else common
 bg=group(b[key],set(population));cg=group(c[key],set(population)); raw=[];normalized=[]
 for card in population:
  if bg[card]!=cg[card]:raw.append(card)
  if canonical(bg[card],b['argentumCommit'])!=canonical(cg[card],c['argentumCommit']):normalized.append(card)
 sections[key]={'populationNames':len(population),'beforeRecords':sum(map(len,bg.values())),'afterRecords':sum(map(len,cg.values())), 'rawChangedCards':raw,'runtimeUrlNormalizedChangedCards':normalized}
out={'before':{'path':str(a.before.resolve()),'sha256':bhash,'engine':b['argentumCommit']},'after':{'path':str(a.after.resolve()),'sha256':chash,'engine':c['argentumCommit']},'commander':b['commander'],'objective':b['objective'],'commonMechanicalPopulation':common,'commonEligibilityNames':eligibility_names,'onlyBefore':sorted(set(b['mechanicalInputCards'])-common_set),'onlyAfter':sorted(set(c['mechanicalInputCards'])-common_set),'sections':sections,'limits':'Native-tree and path differences are retained. Equality is observed structural agreement, not source fidelity or qualification. Lists retain their reported order.'}
a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'commonCards':len(common),'sections':{k:len(v['runtimeUrlNormalizedChangedCards']) for k,v in sections.items()}}))
