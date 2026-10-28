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
 base=ROOT/'generated/phase29';out=base/args.stage;out.mkdir(parents=True,exist_ok=True)
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
  save(out/('coverage-recheck-timings.json' if args.coverage_only else 'timings.json'),measurements);print(label,measurements[-1]['wallSeconds'],flush=True)
 for slug,commander in COMMANDERS.items():
  path=out/f'coverage-{slug}.json'
  argv=['audit','catalog-coverage','--commander',commander,'--oracle-cards',str(args.snapshot.resolve()),'--objective','create-creature-tokens','--translation-review',str(args.translation_review),'--json','--output',str(path)]
  if slug=='judith':argv+=['--enrichment-output',str(out/'enrichment.json')]
  cli('coverage-'+slug,argv)
  cli('missing-'+slug,['audit','catalog-missing','--report',str(path),'--limit','20','--json','--output',str(out/f'missing-{slug}.json')])
  if args.coverage_only:continue
  for letter,objective in OBJECTIVES.items():
   cli(f'{slug}-{letter}',['discover','--commander',commander,'--objective',objective,'--json','--output',str(out/f'{slug}-{letter}.json')])
   if args.stage=='after':cli(f'diff-{slug}-{letter}',['audit','diff','--before',str(base/'before'/f'{slug}-{letter}.json'),'--after',str(out/f'{slug}-{letter}.json'),'--json','--output',str(out/f'diff-{slug}-{letter}.json')])
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('stage',choices=['before','after']);p.add_argument('--coverage-only',action='store_true');p.add_argument('--translation-review',type=pathlib.Path,default=ROOT/'reference/phase27/translation-review.json');p.add_argument('--snapshot',type=pathlib.Path,required=True);p.add_argument('--dist',type=pathlib.Path,default=ROOT/'cli/build/install/mtg-assistant');a=p.parse_args()
 run(a)
