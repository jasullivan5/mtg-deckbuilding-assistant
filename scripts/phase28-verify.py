#!/usr/bin/env python3
"""Reproduce source-bound coverage and unchanged native discovery; Windows RSS is measured per JVM."""
import argparse,ctypes,gzip,hashlib,json,os,pathlib,subprocess,time,xml.etree.ElementTree as ET
ROOT=pathlib.Path(__file__).resolve().parent.parent
COMMANDERS={'judith':'Judith, the Scourge Diva','krenko':'Krenko, Mob Boss','oviya':'Oviya Pashiri, Sage Lifecrafter'}
OBJECTIVES={'a':'own-nontoken-creature-deaths','b':'reanimate-own-creatures','c':'create-creature-tokens'}
def read(p):return json.loads(pathlib.Path(p).read_text(encoding='utf-8-sig'))
def save(p,value):
 p=pathlib.Path(p);p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def digest(p):
 with pathlib.Path(p).open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
class Counters(ctypes.Structure):
 _fields_=[('cb',ctypes.c_ulong),('PageFaultCount',ctypes.c_ulong)]+[(k,ctypes.c_size_t) for k in ['PeakWorkingSetSize','WorkingSetSize','QuotaPeakPagedPoolUsage','QuotaPagedPoolUsage','QuotaPeakNonPagedPoolUsage','QuotaNonPagedPoolUsage','PagefileUsage','PeakPagefileUsage']]
def run(args):
 base=ROOT/'generated/phase28';out=base/args.stage;out.mkdir(parents=True,exist_ok=True)
 java=pathlib.Path(os.environ['JAVA_HOME'])/'bin'/('java.exe' if os.name=='nt' else 'java')
 dist=args.dist.resolve();measurements=[]
 def cli(label,argv):
  log=out/(label+'.log');start=time.monotonic();peak=0
  with log.open('w',encoding='utf-8') as stream:
   p=subprocess.Popen([str(java),'-Xmx1g','-XX:ActiveProcessorCount=4','-cp',str(dist/'lib'/'*'),'dev.mtg.assistant.MainKt',*argv],cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,creationflags=0x08000000 if os.name=='nt' else 0)
   while p.poll() is None:
    if os.name=='nt':
     counter=Counters();counter.cb=ctypes.sizeof(counter)
     if ctypes.windll.psapi.GetProcessMemoryInfo(ctypes.c_void_p(int(p._handle)),ctypes.byref(counter),counter.cb):peak=max(peak,counter.PeakWorkingSetSize)
    time.sleep(.1)
  if p.returncode:raise RuntimeError(f'{label} failed ({p.returncode}); see {log}')
  measurements.append({'command':label,'arguments':argv,'wallSeconds':round(time.monotonic()-start,3),'peakWorkingSetBytes':peak or None})
  save(out/'timings.json',measurements);print(label,measurements[-1]['wallSeconds'],flush=True)
 for slug,commander in COMMANDERS.items():
  path=out/f'coverage-{slug}.json'
  argv=['audit','catalog-coverage','--commander',commander,'--oracle-cards',str(args.snapshot.resolve()),'--objective','create-creature-tokens','--translation-review','reference/phase27/translation-review.json','--json','--output',str(path)]
  if args.stage=='after' and slug=='judith':argv+=['--enrichment-output',str(out/'enrichment.json')]
  cli('coverage-'+slug,argv)
  cli('missing-'+slug,['audit','catalog-missing','--report',str(path),'--limit','20','--json','--output',str(out/f'missing-{slug}.json')])
  for letter,objective in OBJECTIVES.items():
   cli(f'{slug}-{letter}',['discover','--commander',commander,'--objective',objective,'--json','--output',str(out/f'{slug}-{letter}.json')])
def compare(args):
 base=ROOT/'generated/phase28';before=base/'before';after=base/'after'
 manifest=read(str(args.snapshot)+'.manifest.json');assert digest(args.snapshot)==manifest['sha256']
 with gzip.open(args.snapshot,'rt',encoding='utf-8') as f:cards=[json.loads(line) for line in f]
 receipt={'snapshot':manifest,'coverage':{},'discovery':{},'timings':{s:read(base/s/'timings.json') for s in ['before','after']},'tests':{},'artifacts':{}}
 land={'Plains':'W','Island':'U','Swamp':'B','Mountain':'R','Forest':'G'}
 for slug,commander in COMMANDERS.items():
  b=read(before/f'coverage-{slug}.json');a=read(after/f'coverage-{slug}.json')
  assert b['schemaVersion']==1 and a['schemaVersion']==2
  assert b['snapshot']['sha256']==a['snapshot']['sha256']==manifest['sha256']
  assert b['argentumCommit']==a['argentumCommit']==(ROOT/'ARGENTUM_COMMIT').read_text().strip()
  source=[c for c in cards if c.get('name')==commander and c.get('legalities',{}).get('commander')=='legal'];assert len(source)==1
  allowed=set(source[0]['color_identity']);assert set(a['allowedIdentity'])==allowed
  eligible=set()
  for c in cards:
   if c.get('legalities',{}).get('commander')!='legal' or not set(c['color_identity'])<=allowed:continue
   types=' '.join([c.get('type_line','')]+[x.get('type_line','') for x in c.get('card_faces',[])]).replace('—',' ').split()
   if any(t in types and color not in allowed for t,color in land.items()):continue
   eligible.add(c['oracle_id'])
  assert eligible=={r['external']['oracleId'] for r in b['records']}=={r['external']['oracleId'] for r in a['records']}
  assert sum(a['counts'][s] for s in ['native_available','native_metadata_unresolved','native_mechanically_unresolved','native_definition_missing','ambiguous_mapping','native_identity_review_required'])==len(eligible)
  assert a['counts']['confident_native_identity_associations']+a['counts']['identity_review_required']+a['counts']['ambiguous_mapping']+a['counts']['native_definition_missing']==len(eligible)
  receipt['coverage'][slug]={'before':b['counts'],'after':a['counts'],'sameExternallyEligibleIds':True,'nativePoolUnchanged':True,'timingsMillis':{'before':b['timingsMillis'],'after':a['timingsMillis']}}
  for letter in OBJECTIVES:
   p=before/f'{slug}-{letter}.json';q=after/f'{slug}-{letter}.json';x=read(p);y=read(q)
   assert x==y,f'Unexpected discovery change: {slug}-{letter}'
   receipt['discovery'][f'{slug}-{letter}']={'identical':True,'sha256':digest(q),'bytes':q.stat().st_size,'compared':'Entire parsed discovery report: eligibility, actions, facts, verdicts, unsupported structures, support, counts and explanations'}
 for p in (ROOT/'cli/build/test-results/test').glob('*.xml'):
  e=ET.parse(p).getroot();receipt['tests'][e.get('name')]={k:int(e.get(k,'0')) for k in ['tests','failures','errors','skipped']}
 for stage in ['before','after']:
  receipt['artifacts'][stage]={p.name:{'bytes':p.stat().st_size,'sha256':digest(p)} for p in (base/stage).glob('*.json') if p.name!='enrichment.json'}
 artifact=after/'enrichment.json';receipt['enrichmentArtifact']={'bytes':artifact.stat().st_size,'sha256':digest(artifact),'context':read(artifact)['context']}
 save(base/'verification.json',receipt);print(json.dumps({'coverage':receipt['coverage'],'discoveryContextsIdentical':len(receipt['discovery'])},indent=2))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('stage',choices=['before','after','compare']);p.add_argument('--snapshot',type=pathlib.Path,required=True);p.add_argument('--dist',type=pathlib.Path,default=ROOT/'cli/build/install/mtg-assistant');a=p.parse_args()
 compare(a) if a.stage=='compare' else run(a)
