#!/usr/bin/env python3

"""Bounded research evidence only: deterministic selection and pinned Assay diagnostics."""

import argparse, datetime, gzip, hashlib, json, os, pathlib, subprocess, time, shutil, urllib.parse, urllib.request

ROOT=pathlib.Path(__file__).resolve().parent.parent

OUT=ROOT/'reference/alignment-batch1'
GEN=ROOT/'generated/alignment-batch1'

HEADERS={"User-Agent":"MTGDeckbuildingAssistant/coverage-batch (github.com/jasullivan5/mtg-deckbuilding-assistant)","Accept":"application/json"}

def read(p): return json.loads(pathlib.Path(p).read_text(encoding='utf-8-sig'))

def save(p,x):

 p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(x,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')

def digest(p): return hashlib.sha256(pathlib.Path(p).read_bytes()).hexdigest()

def fetch(url):

 time.sleep(.15)

 with urllib.request.urlopen(urllib.request.Request(url,headers=HEADERS),timeout=60) as r:return json.load(r)

def select(a):

 report=read(a.report); snapshot=a.snapshot

 assert report['snapshot']['sha256']==digest(snapshot)

 rows={r['external']['oracleId']:r for r in report['records'] if r['status']=='NATIVE_DEFINITION_MISSING'}

 with gzip.open(snapshot,'rt',encoding='utf-8') as f: raw={c['oracle_id']:c for line in f if (c:=json.loads(line)).get('oracle_id') in rows}

 seed=report['snapshot']['sha256']+f':alignment-batch-{a.batch}:'
 prior={r['oracleId'] for r in read(ROOT/'reference/alignment-batch1/review.json')['cards']} if a.batch==2 else set()
 slots=['Creature','Instant','Sorcery','Enchantment','Artifact','Land']*2 if a.batch==2 else ['Creature','Instant','Sorcery','Enchantment','Artifact','Land','Creature','Artifact']

 rank=lambda c:hashlib.sha256((seed+c['oracle_id']).encode()).hexdigest()

 selected=[];used=set()

 for kind in slots:

  pool=[c for c in raw.values() if c['oracle_id'] not in used and kind in c.get('type_line','').split(' — ')[0].split()]

  c=min(pool,key=rank);used.add(c['oracle_id'])

  assert c['legalities']['commander']=='legal' and set(c['color_identity'])<=set(report['allowedIdentity'])



  selected.append({'oracleId':c['oracle_id'],'name':c['name'],'stratum':kind,'selectionHash':rank(c),'colorIdentity':c['color_identity'],'layout':c['layout'],'coverageStatus':rows[c['oracle_id']]['status'],'previouslyInvestigated':c['oracle_id'] in prior})

 if (OUT/'selection.json').exists():
  frozen=read(OUT/'selection.json')
  assert [c['oracleId'] for c in frozen['cards']]==[c['oracleId'] for c in selected], 'Frozen selection changed; use a separately reviewed batch'
  assert frozen['snapshotSha256']==digest(snapshot) and frozen['coverageReportSha256']==digest(a.report), 'Frozen selection inputs changed'
  print('Frozen selection reproduced; existing evidence retained');return
 for item in selected:
  c=raw[item['oracleId']]
  save(OUT/'cards'/(c['oracle_id']+'.json'),{k:c[k] for k in ['oracle_id','name','layout','color_identity','legalities','oracle_text','mana_cost','type_line','power','toughness','keywords','card_faces','prints_search_uri'] if k in c} if a.batch==2 else c)
 save(OUT/'selection.json',{'baseCommit':a.base,'commander':report['commander'],'allowedIdentity':report['allowedIdentity'],'snapshotSha256':digest(snapshot),'coverageReportSha256':digest(a.report),'eligiblePool':report['counts']['eligible_oracle_cards'],'missingPool':len(rows),'method':f'Ordered slots {slots}; unused minimum SHA256(snapshotSha256 + :alignment-batch-{a.batch}: + oracleId) among confirmed missing physical cards of that primary type. No mechanical, objective, popularity, familiarity or Assay filter; prior investigations do not affect ranking. Frozen before diagnosis.','cards':selected})

 for c in selected: print(c['stratum'],c['name'],c['oracleId'],flush=True)

def sources(a):

 for selected in read(OUT/'selection.json')['cards']:

  oid=selected['oracleId'];c=read(OUT/'cards'/(oid+'.json'))
  if (OUT/'sources'/(oid+'.json')).exists() and (OUT/'rulings'/(oid+'.json')).exists():
   print('Retaining archived source:',selected['name'],flush=True);continue

  start=time.monotonic();prints=fetch('https://api.scryfall.com/cards/search?'+urllib.parse.urlencode({'q':'oracleid:'+oid,'unique':'prints','order':'released','dir':'asc'}))

  real=[p for p in prints['data'] if p.get('set_type') not in ['promo','token','art_series','memorabilia','alchemy','minigame'] and not p.get('digital')]

  p=real[0];exact=fetch('https://api.scryfall.com/cards/named?'+urllib.parse.urlencode({'exact':p['name'],'set':p['set']}))

  assert exact['oracle_id']==oid and exact['legalities']['commander']=='legal'
  assert exact['color_identity']==c['color_identity']

  if a.batch==2:
   keep={'object','id','oracle_id','name','layout','mana_cost','type_line','oracle_text','power','toughness','loyalty','defense','colors','color_identity','color_indicator','keywords','produced_mana','card_faces','all_parts','legalities','set','set_type','collector_number','released_at','rarity','artist','image_uris','flavor_text','uri','scryfall_uri','rulings_uri','prints_search_uri'}
   exact={k:v for k,v in exact.items() if k in keep}
  save(OUT/'sources'/(oid+'.json'),exact);save(OUT/'rulings'/(oid+'.json'),fetch(exact['rulings_uri']))

  save(OUT/'sources'/(oid+'.provenance.json'),{'wallSeconds':round(time.monotonic()-start,3),'projection':'Mechanics, printing and source links; market fields omitted' if a.batch==2 else 'Full response','retrievedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'printsQuery':prints['uri'] if 'uri' in prints else c['prints_search_uri'],'earliestPrinting':{'set':p['set'],'collectorNumber':p['collector_number'],'releasedAt':p['released_at'],'id':p['id']},'snapshotOracleTextMatches':c.get('oracle_text')==exact.get('oracle_text'),'sourceSha256':digest(OUT/'sources'/(oid+'.json'))})

  print(exact['name'],exact['set'],exact['collector_number'],flush=True)

def assay(a):

 exe=ROOT/'argentum/oracle-assay/build/install/oracle-assay/bin'/('oracle-assay.bat' if os.name=='nt' else 'oracle-assay')

 home=(ROOT/'generated/oracle/assay-home' if a.batch==2 else GEN/'assay-home'); cache=home/'.cache/scryfall/_bulk-oracle-cards.jsonl.gz'

 cache.parent.mkdir(parents=True,exist_ok=True)
 if a.batch==1: shutil.copyfile(a.snapshot,cache)
 elif not cache.exists():
  try: os.link(a.snapshot.resolve(),cache)
  except OSError: shutil.copyfile(a.snapshot,cache)

 assert digest(cache)==read(OUT/'selection.json')['snapshotSha256']

 results=[];env=os.environ.copy();env['JAVA_TOOL_OPTIONS']=env.get('JAVA_TOOL_OPTIONS','')+f' -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Duser.home={home}'

 for c in read(OUT/'selection.json')['cards']:

  oid=c['oracleId'];commands=[]

  for mode in ['parse','explain','compile']:

   argv=[str(exe),mode,*(['--file',str(OUT/'sources'/(oid+'.json'))] if mode=='compile' else [c['name']])]

   start=time.monotonic();r=subprocess.run(argv,cwd=ROOT,env=env,capture_output=True,text=True,encoding='utf-8',errors='strict',timeout=180)

   assert digest(cache)==read(OUT/'selection.json')['snapshotSha256'], 'Assay changed the pinned shared input'
   path=GEN/'assay'/f'{oid}.{mode}.txt';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(r.stdout+'\nSTDERR:\n'+r.stderr,encoding='utf-8')

   # Compact tracked diagnostics, complete stdout remains reproducible under generated/.

   if r.returncode not in ([0,1] if mode=='compile' else [0]): raise RuntimeError(f'{mode} failed for {c["name"]}: {r.stderr}')
   commands.append({'wallSeconds':round(time.monotonic()-start,3),'mode':mode,'arguments':[str(x).replace(str(ROOT)+os.sep,'') for x in argv[1:]],'exitCode':r.returncode,'stdoutSha256':hashlib.sha256(r.stdout.encode()).hexdigest(),'diagnostics':r.stdout if mode!='compile' or r.returncode else 'Compiled proposal retained under generated; definition is independently authored and reviewed.','stderr':'\n'.join(line for line in r.stderr.splitlines() if not line.startswith('Picked up JAVA_TOOL_OPTIONS:'))})

  results.append({'oracleId':oid,'name':c['name'],'commands':commands});save(OUT/'assay.json',results);print(c['name'],[(r['mode'],r['exitCode']) for r in commands],flush=True)

def status(a):
 old=read(ROOT/'reference/phase27/translation-review.json');review=read(OUT/'review.json')
 if a.batch==2: old['cards']+=read(ROOT/'reference/alignment-batch1/review.json')['cards']
 combined={r['oracleId']:{'oracleId':r['oracleId'],'status':r['status']} for r in old['cards']+review['cards']}
 save(OUT/'coverage-status.json',{'snapshotSha256':read(OUT/'selection.json')['snapshotSha256'],'derivedFrom':['reference/phase27/translation-review.json']+(['reference/alignment-batch1/review.json'] if a.batch==2 else [])+[str((OUT/'review.json').relative_to(ROOT)).replace(os.sep,'/')],'cards':sorted(combined.values(),key=lambda r:r['oracleId'])})
def receipt(a):
 import xml.etree.ElementTree as ET
 review=read(OUT/'review.json');selected=read(OUT/'selection.json');completed={r['name'] for r in review['cards'] if r['status']=='VERIFIED_NATIVE_EXTENSION'}
 completed_ids={r['oracleId'] for r in review['cards'] if r['name'] in completed}
 base=GEN if a.batch==2 else ROOT/'generated/phase29';before=base/'before';after=base/'after'
 proof={'argentumCommit':review['argentumCommit'],'snapshotSha256':selected['snapshotSha256'],'coverage':{},'discovery':{},'tests':{},'measurements':{stage:[{k:r[k] for k in ['command','wallSeconds','peakWorkingSetBytes']} for r in read(base/stage/'timings.json')] for stage in ['before','after']}}
 def retained(report):
  result={}
  for key,value in report.items():
   if key=='counts':continue
   if isinstance(value,list):value=[x for x in value if not (isinstance(x,dict) and x.get('card') in completed) and not (isinstance(x,str) and key=='mechanicalInputCards' and x in completed)]
   result[key]=value
  return result
 for slug in ['judith','krenko','oviya']:
  b=read(before/f'coverage-{slug}.json');c=read(after/f'coverage-{slug}.json')
  old={r['external']['oracleId']:r for r in b['records']};new={r['external']['oracleId']:r for r in c['records']};assert old.keys()==new.keys()
  added=completed_ids & old.keys()
  assert c['counts']['native_definition_missing']==b['counts']['native_definition_missing']-len(added)
  assert c['counts']['confident_native_identity_associations']==b['counts']['confident_native_identity_associations']+len(added)
  for oid,row in old.items():
   if oid in added:
    association=new[oid]['mapping']['association'];assert association['confidence']=='CONFIDENT' and association['oracleId']==oid
    assert association['native']['oracleId']==oid
   else:
    # The two investigated but still missing rows intentionally gain a review status.
    assert {k:v for k,v in row.items() if k!='translationStatus'}=={k:v for k,v in new[oid].items() if k!='translationStatus'},(slug,row['external']['name'])
  proof['coverage'][slug]={'eligibleOracleCards':len(old),'newConfidentDefinitions':len(added),'missingBefore':b['counts']['native_definition_missing'],'missingAfter':c['counts']['native_definition_missing'],'retainedRecordsUnchangedApartFromReviewStatus':True}
  for letter in ['a','b','c']:
   b=read(before/f'{slug}-{letter}.json');c=read(after/f'{slug}-{letter}.json')
   assert retained(b)==retained(c),f'Retained discovery changed: {slug}-{letter}'
   assert c['counts']['authored_physical_cards']==b['counts']['authored_physical_cards']+len(completed)
   proof['discovery'][slug+'-'+letter]={'retainedEvidenceIdentical':True,'beforeSha256':digest(before/f'{slug}-{letter}.json'),'afterSha256':digest(after/f'{slug}-{letter}.json'),'beforeCounts':b['counts'],'afterCounts':c['counts']}
 suites={}
 for path in (ROOT/'cli/build/test-results/test').glob('*.xml'):
  suite=ET.parse(path).getroot();assert int(suite.attrib['failures'])==0 and int(suite.attrib['errors'])==0 and int(suite.attrib['skipped'])==0,path
  suites[suite.attrib['name']]={'tests':int(suite.attrib['tests']),'failures':0,'errors':0,'seconds':float(suite.attrib['time']),'cases':[x.attrib['name'] for x in suite.findall('testcase')]}
 assert ('dev.mtg.assistant.BatchTwoIntegrationTest' if a.batch==2 else 'dev.mtg.assistant.BatchOneIntegrationTest') in suites and len(suites)>20
 proof['tests']={'total':sum(s['tests'] for s in suites.values()),'failures':0,'errors':0,'skipped':0,'suiteCount':len(suites),'batchScenarios':{k:v for k,v in suites.items() if any(k.endswith(r['engineExecution'].get('scenarios') or '__none__') for r in review['cards'])}}
 for row in review['cards']:
  source=read(OUT/'sources'/(row['oracleId']+'.json'));assert source['oracle_id']==row['oracleId'] and source['legalities']['commander']=='legal' and source['color_identity']==row['colorIdentity']
  if row['name'] not in completed:continue
  if row['engineExecution'].get('scenarios'):
   scenario=suites['dev.mtg.assistant.'+row['engineExecution']['scenarios']]
   row['engineExecution']={'status':'EXECUTED_BOUNDED_SCENARIOS','scenarios':row['engineExecution']['scenarios'],'passed':scenario['tests'],'cases':scenario['cases']}
  row['discovery']={'status':'EVALUATED_NATIVE_DEFINITION','contexts':{}}
  for letter in ['a','b','c']:
   report=read(after/f'oviya-{letter}.json');results=[x for x in report['results'] if x['card']==row['name']]
   row['discovery']['contexts'][report['objective']]={'resultVerdicts':[x['verdict'] for x in results],'resultEvidence':[{k:x.get(k) for k in ['verdict','mechanicalCompatibility','policyAcceptance','localSupport','operation','branch','affectedScope','decisions']} for x in results],'mechanicalFacts':sum(x['card']==row['name'] for x in report['facts']),'unsupportedRecords':sum(x['card']==row['name'] for x in report['unsupported'])}
  row['sourceVerification']={'sourceSha256':digest(OUT/'sources'/(row['oracleId']+'.json')),'rulingsSha256':digest(OUT/'rulings'/(row['oracleId']+'.json')),'fieldAssertions':('BatchTwoIntegrationTest' if a.batch==2 else 'BatchOneIntegrationTest')+'; archived printing, Oracle text, mana cost, type, identity, legality and printing id'}
 save(OUT/'review.json',review);save(OUT/'verification.json',proof);print('Verified',proof['tests']['total'],'tests and nine retained discovery contexts')
if __name__=='__main__':

 p=argparse.ArgumentParser(description=__doc__);p.add_argument('stage',choices=['select','sources','assay','status','receipt']);p.add_argument('--batch',type=int,choices=[1,2],default=1);p.add_argument('--report',type=pathlib.Path);p.add_argument('--snapshot',type=pathlib.Path);p.add_argument('--base',default='d07a16f7f328963f1b5d0babc4d18ac05965ab0f');a=p.parse_args();OUT=ROOT/f'reference/alignment-batch{a.batch}';GEN=ROOT/f'generated/alignment-batch{a.batch}';globals()[a.stage](a)
