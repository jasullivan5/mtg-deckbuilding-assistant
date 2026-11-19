"""Collect final-code observations; retain earlier attempts as historical observations."""
import collections,gzip,hashlib,json,pathlib,subprocess,struct
w=pathlib.Path(__file__).resolve().parent.parent
a=w/'mtg-native-consolidation';e=w/'argentum-native-consolidation'
r=a/'generated/jdk25-integration/receipts'
APP='529a78ea38b12b13598fd3c06f2e59ec9a4e9abe';ENGINE='3969d0f6dca34fe79eb52d8ba703366ea4004121'
def read(p):return json.loads(p.read_bytes())
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(n,v):(r/n).write_text(json.dumps(v,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
def git(folder,*args):return subprocess.check_output(['git','-C',str(folder),*args],text=True).strip()
assert git(a,'rev-parse','HEAD')==APP
assert git(e,'rev-parse','HEAD')==ENGINE
pres=read(r/'preservation.json');pres['application']['head']=APP
for repo,folder in [('application',a),('engine',e)]:
 row=pres[repo]
 changed=git(folder,'diff','--name-status',row['baseline'],'HEAD','--',*row['inspectedPaths']).splitlines()
 assert changed==row['changedFiles'],(repo,changed)
assert not git(a,'diff','1db2f9','HEAD','--','docs/images')
write('preservation.json',pres)
p=a/'generated/jdk25-integration/activation-cost-execution-529a78e.json';v=read(p)
assert v['applicationRevision']==APP and v['engine']==ENGINE
old=read(a/'generated/jdk25-integration/activation-cost-execution-263c46d.json')
assert len(v['observations'])==26
for k in ['requirementsSha256','bindingsSha256','harnessSha256','definitions','mutationDefinitions','supportDefinitions']:assert v[k]==old[k],k
for x,y in zip(v['observations'],old['observations']):
 for k in ['scenarioId','result','checks']:assert x[k]==y[k],(x['scenarioId'],k)
counts=dict(collections.Counter(x['result'] for x in v['observations']))
assert counts=={'AGREES':22,'INCOMPLETE':1,'UNRESOLVED':1,'DIFFERS':2},counts
(r/'activation-cost-execution.json.gz').write_bytes(gzip.compress(p.read_bytes(),mtime=0))
keys=['scenarioId','scopeId','card','mutant','variant','result','executed','definitionSha256','checks','problem']
receipt={k:value for k,value in v.items() if k!='observations'}
receipt.update(rawJsonSha256=sha(p),counts=counts,observations=[{k:x.get(k) for k in keys} for x in v['observations']],priorResultAndChecksEqual=True,qualification='Fresh bounded execution; historical observations and provisional qualification remain unchanged.')
prior=json.loads(gzip.decompress((a/'docs/evidence/native-consolidation-2026-11-18/activation-cost-execution.json.gz').read_bytes()))
for k in ['requirementsSha256','bindingsSha256','harnessSha256','definitions','mutationDefinitions','supportDefinitions']:
 assert prior[k]==v[k],k
assert [(x['scenarioId'],x['result'],x['checks']) for x in prior['observations']]==[(x['scenarioId'],x['result'],x['checks']) for x in v['observations']]
assert len(v['observations'])==26
(r/'activation-cost-execution.json.gz').write_bytes(gzip.compress(p.read_bytes(),mtime=0))
write('activation-cost-receipt.json',receipt)
audits=a/'generated/jdk25-integration/audit-diff-529a78e'
rows=[]
for p in sorted(audits.glob('*.json')):
 v=read(p)
 assert v['comparablePool'] and v['beforeStageCounts']==v['afterStageCounts'],p
 assert all(x['attribution']==['EXPLANATION_ONLY'] for x in v['changes']),p
 assert set(v['transitions'])<={'EXPLANATION_ONLY_CHANGED','UNRESOLVED_PATHWAY_IDENTITY_CHANGE'},p
 rows.append({'report':p.name,'sha256':sha(p),'counts':v['counts'],'transitions':v['transitions'],'comparablePool':v['comparablePool'],'ambiguousPathwayBases':v['ambiguousPathwayBases']})
assert len(rows)==18,len(rows)
write('historical-audit-summary.json',{'application':APP,'engine':ENGINE,'heap':'-Xmx4g for two-report offline audits, as in earlier verification; production exports remain -Xmx1g','reports':rows,'qualification':'All changed rows have explanation-only attribution; existing duplicate pathway identity warnings remain disclosed and unresolved.'})
dist=read(r/'production-distribution.json');lib=a/'build/install/mtg-assistant/lib'
assert dist['application']==APP
diff=[row['file'] for row in dist['jars'] if sha(lib/row['file'])!=row['sha256']]
assert not diff,diff
write('distribution-after-full-verification.json',{'application':APP,'engine':ENGINE,'runtimeJars':26,'differingJars':diff,'allJarSha256Equal':True,'comparison':'Final full-test/normal-Explorer distribution equals the immutable copy used for final production exports byte for byte.'})
classrows=[]
for repo,folder,path in [('application',a,'build/classes/kotlin/main/dev/mtg/assistant/discovery/DefinitionSources.class'),('application',a,'build/classes/kotlin/main/dev/mtg/assistant/explorer/ResearchProgressService.class'),('engine',e,'rules-engine/build/classes/kotlin/main/com/wingedsheep/engine/core/ActionProcessor.class')]:
 major=struct.unpack('>H',(folder/path).read_bytes()[6:8])[0];assert major==69
 classrows.append({'repository':repo,'classFile':path,'classfileMajor':major,'javaTarget':25})
write('application-rules-class-targets.json',classrows)
catalog=a/'generated/verification/catalog'
comparison=read(catalog/'comparison-summary.json')
assert comparison['passed'] and comparison['currentChecksPassed'] and comparison['actualEngineCommit']==ENGINE
comparison['applicationRevision']=APP
write('catalog-comparison.json',comparison)
browser=read(a/'generated/explorer/browser-results.json')
assert browser['stats']['unexpected']==0 and browser['stats']['expected']==41 and browser['stats']['skipped']==0,browser['stats']
write('browser-results.json',browser)
write('browser-summary.json',{'application':APP,'engine':ENGINE,'command':'EXPLORER_URL=http://127.0.0.1:8876 EXPLORER_BROWSER_CHANNEL=msedge pnpm exec playwright test','stats':browser['stats'],'requiredSyntheticUi':8,'requiredLive':18,'additionalExistingChecks':15,'heap':'normal Explorer -Xmx1g -XX:ActiveProcessorCount=4','explicitSnapshotSha256':'081a50ef82c767523f955e914153cf988ff045a72e3765b6aa2382567f9eb53a'})
oom=read(r/'oom-observation.json');oom['intermediateColdProgressFix']=oom.pop('fixedApplication',oom.get('intermediateColdProgressFix'));oom['finalApplication']=APP
oom['subsequentObservation']='The intermediate canonical-shape sharing fix passed cold Progress, but retaining complete Progress extraction and coverage projections still caused a later Discover request to time out and exhaust the 1 GiB heap.'
oom['finalChange']='Share equal immutable canonical subtrees; stream independent native-node grouping; feed catalog extraction one definition at a time into the same accumulator; allow reproducible complete projections to be reclaimed. Immutable sources and published summaries remain retained; no data fields, ordering, heap or deadline is changed.'
oom['finalVerification']='progress-discover-memory.json and browser-summary.json: full-source Progress, subsequent Discover and revisits complete with the normal 1 GiB heap.'
write('oom-observation.json',oom)
print('Final runtime, catalog, browser, production, cost and preservation receipts collected.')
