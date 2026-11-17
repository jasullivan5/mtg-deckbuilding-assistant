from pathlib import Path
import json,collections,hashlib,xml.etree.ElementTree as E,shutil
BASE=Path('mtg-upstream-baseline/generated/upstream-native-control')
AFTER=Path('mtg-full-upstream/generated/upstream-native-control-56eeb')
OUT=Path('.integration-tools/delivery-evidence')
OUT.mkdir(parents=True,exist_ok=True)
ENGINE='56eeb394ef4fd2b9b7467b2ae95b3ccf04ac0906'
APP='ccaf1926ec754799711fa921466de2f636137e7e'
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def suite(path):
 files=sorted(path.glob('TEST-*.xml'))
 roots=[E.parse(p).getroot() for p in files]
 stamps=sorted(r.get('timestamp','') for r in roots)
 manifest='\n'.join(p.name+' '+digest(p) for p in files)
 return {'suites':len(roots),**{k:sum(int(r.get(k,0)) for r in roots) for k in ['tests','failures','errors','skipped']},'firstSuiteTimestamp':stamps[0] if stamps else None,'lastSuiteTimestamp':stamps[-1] if stamps else None,'sortedXmlManifestSha256':hashlib.sha256(manifest.encode()).hexdigest()}
# Verification receipts retain failures, not just successful rows.
paths=[('sdk',Path('argentum-full-upstream/mtg-sdk/build/test-results/test')),('rules',Path('argentum-full-upstream/rules-engine/build/test-results/test'))]+[(p.parents[3].name,p) for p in Path('argentum-full-upstream/mtg-sets').glob('*/tests/build/test-results/test')]
rows=[dict(module=name,**suite(path)) for name,path in paths]
assert len(rows)==11
assert all(r['failures']==r['errors']==0 for r in rows)
assert sum(r['tests'] for r in rows if r['module']!='sdk')==32890
assert all(r['firstSuiteTimestamp']>='2026-11-17T20:56:' for r in rows)
(OUT/'native-test-summary.json').write_text(json.dumps({'engine':ENGINE,'modules':rows},indent=2)+'\n')
application_path=Path('mtg-full-upstream/build/test-results/test')
application=suite(application_path)
assert application['tests']==743 and application['failures']==345 and application['errors']==application['skipped']==0
assert application['firstSuiteTimestamp']>='2026-11-17T20:56:'
failure_groups=collections.Counter()
failure_cases=collections.defaultdict(list)
failure_examples={}
for path in application_path.glob('TEST-*.xml'):
 for case in E.parse(path).getroot().findall('testcase'):
  failure=case.find('failure')
  if failure is not None:
   message=failure.get('message','')
   group='extension collision' if 'Extension identity collision:' in message else 'metadata correction binding' if 'review correction' in message else message.splitlines()[0][:180]
   failure_groups[group]+=1
   failure_cases[group].append({'class':case.get('classname'),'test':case.get('name')})
   failure_examples.setdefault(group,{'message':message,'stackTrace':failure.text})
(OUT/'application-test-summary.json').write_text(json.dumps({'application':APP,'engine':ENGINE,**application,'failureGroups':dict(failure_groups),'note':'All failures retained; normal catalog remains blocked.'},indent=2)+'\n')
(OUT/'application-failure-details.json').write_text(json.dumps({'application':APP,'engine':ENGINE,'groups':[{'group':group,'count':count,'cases':failure_cases[group],'representativeFailure':failure_examples[group]} for group,count in failure_groups.items()]},indent=2)+'\n')
assert sum(failure_groups.values())==345 and failure_groups['extension collision']==336
# Explicit controlled populations and SHA-bound raw observations.
observations=[];population=None
for before in sorted(BASE.glob('krenko-*.json')):
 after=AFTER/before.name;b=json.loads(before.read_text());a=json.loads(after.read_text());common=sorted(set(b['mechanicalInputCards']) & set(a['mechanicalInputCards']))
 if population is None:population=common
 assert population==common
 common_set=set(common)
 def accepted(r):return {x['card'] for x in r['results'] if x['card'] in common_set and x['verdict']=='MATCH'}
 def unknown(r):return {x['card'] for x in r['results'] if x['card'] in common_set and x['verdict']=='UNKNOWN'}
 def verdicts(r):return dict(collections.Counter(x['verdict'] for x in r['results'] if x['card'] in common_set))
 observations.append({'objective':b['objective'],'beforeReport':{'path':str(before.resolve()),'sha256':digest(before),'engine':b['argentumCommit']},'afterReport':{'path':str(after.resolve()),'sha256':digest(after),'engine':a['argentumCommit']},'beforePopulation':len(b['mechanicalInputCards']),'afterPopulation':len(a['mechanicalInputCards']),'commonPopulation':len(common),'onlyBefore':sorted(set(b['mechanicalInputCards'])-common_set),'onlyAfter':sorted(set(a['mechanicalInputCards'])-common_set),'newlyAccepted':sorted(accepted(a)-accepted(b)),'noLongerAccepted':sorted(accepted(b)-accepted(a)),'newUnknownCards':sorted(unknown(a)-unknown(b)),'beforeVerdicts':verdicts(b),'afterVerdicts':verdicts(a)})
(OUT/'common-native-names.json').write_text(json.dumps(population,ensure_ascii=False,indent=2)+'\n')
assert len(population)==3466
(OUT/'native-control-summary.json').write_text(json.dumps({'mode':'NATIVE_ONLY_DIAGNOSTIC','application':APP,'engine':ENGINE,'baselineApplication':'e0fa95970f546c19b79e32bc323af74367d6c353','baselineEngine':'bd26b756b8b4d347f19e92bc77ef020919ebfa48','commander':'Krenko, Mob Boss','extensionDefinitions':[],'correctionPlan':None,'populationUnit':'Exact native names; no physical identity/evidence equivalence is asserted.','commonNamesSha256':digest(OUT/'common-native-names.json'),'limits':'Native-only diagnostic excludes extensions and reviewed correction plans on BOTH sides. It does not replace the failing production catalog gate, live-service compatibility or independent qualification. Original observations are retained unchanged.','objectives':observations},indent=2)+'\n')
# Retain compact section-level differences and original comparison hashes.
comparisons=[];changed_name_sets={}
for p in sorted(Path('mtg-full-upstream/generated/upstream-integration/final-56eeb-comparisons').glob('*.json')):
 comparison=json.loads(p.read_text())
 sections={}
 for section,values in comparison['sections'].items():
  compact=dict(values)
  for key in ('rawChangedCards','runtimeUrlNormalizedChangedCards'):
   names=compact.pop(key)
   name_set_id=hashlib.sha256(json.dumps(names,ensure_ascii=False,separators=(',',':')).encode()).hexdigest()
   assert name_set_id not in changed_name_sets or changed_name_sets[name_set_id]==names
   changed_name_sets[name_set_id]=names
   compact[key+'Count']=len(names)
   compact[key+'NameSet']=name_set_id
  sections[section]=compact
 comparisons.append({'objective':comparison['objective'],'rawComparisonSha256':digest(p),'sections':sections})
assert len(comparisons)==6
(OUT/'mechanical-comparison-summary.json').write_text(json.dumps({'application':APP,'engine':ENGINE,'limits':'Exact names and raw differences are retained. Repeated name lists are stored once in changedNameSets and referenced by hash. Runtime-URL normalization is separately identified and does not rewrite native structures or source/evidence bindings.','comparisons':comparisons,'changedNameSets':changed_name_sets},indent=2)+'\n')
# Compact paired translation inventory; original source and native dumps remain generated artifacts.
root=Path('mtg-full-upstream/generated/upstream-integration/overlaps-56eeb')
def differences(a,b,path=''):
 if type(a)!=type(b):return [path]
 if isinstance(a,dict):return [x for k in sorted(a.keys()|b.keys()) for x in ([path+'.'+k] if k not in a or k not in b else differences(a[k],b[k],path+'.'+k))]
 if isinstance(a,list):return [path+'.length'] if len(a)!=len(b) else [p for i,(x,y) in enumerate(zip(a,b)) for p in differences(x,y,f'{path}[{i}]')]
 return [] if a==b else [path]
cards=[]
for native_path in sorted(root.glob('*-native.json')):
 authored_path=native_path.with_name(native_path.name.replace('-native.json','-authored-current-sdk.json'));a=json.loads(authored_path.read_text());n=json.loads(native_path.read_text());delta=differences(a,n)
 cards.append({'name':n['name'],'authoredOracleId':a.get('oracleId'),'nativeOracleId':n.get('oracleId'),'authoredSha256':digest(authored_path),'nativeSha256':digest(native_path),'oracleTextEqual':a.get('oracleText')==n.get('oracleText'),'scriptTreesEqual':a.get('script')==n.get('script'),'differentFieldPaths':delta})
assert len(cards)==12
(OUT/'overlap-comparison.json').write_text(json.dumps({'application':APP,'engine':ENGINE,'limit':'Fresh serialization is not an update to frozen evidence or approval of translation equivalence. Default catalog still rejects overlaps.','cards':cards},indent=2)+'\n')
logs=[]
for p in sorted(Path('.').glob('upstream-*.log')):logs.append({'path':str(p.resolve()),'sha256':digest(p),'bytes':p.stat().st_size})
(OUT/'log-manifest.json').write_text(json.dumps(logs,indent=2)+'\n')
print(json.dumps({'native':sum(r['tests'] for r in rows if r['module']!='sdk'),'application':application,'common':len(population),'overlaps':len(cards)}))
