#!/usr/bin/env python3
"""Compare complete reports with the retained regression baseline, preserving evidence bindings."""
import pathlib,json,hashlib,sys
if len(sys.argv)!=3:
 raise SystemExit("Usage: compare-baseline.py <report-directory> <catalog-baseline.json>")
out=pathlib.Path(sys.argv[1]);checks={}
baseline=json.loads(pathlib.Path(sys.argv[2]).read_text(encoding='utf-8'))
# Normalize only runtime provenance, never historical evidence revisions or mechanics.
# The immutable baseline remains untouched; every other byte of canonical JSON is checked.
pin=baseline['argentumCommit']
runtime=None
for key,expected in baseline['reports'].items():
 report=json.loads((out/f'{key}.json').read_text(encoding='utf-8'))
 runtime=runtime or report['argentumCommit']
 def sources(value):
  if isinstance(value,dict):return {k:sources(v) for k,v in value.items()}
  if isinstance(value,list):return [sources(v) for v in value]
  if isinstance(value,str):
   for kind in ('blob','tree'):
    value=value.replace(f'https://github.com/jasullivan5/argentum-engine/{kind}/{runtime}',f'https://github.com/jasullivan5/argentum-engine/{kind}/{pin}')
  return value
 normalized=sources(report)
 normalized['argentumCommit']=pin
 digest=hashlib.sha256((json.dumps(normalized,sort_keys=True,separators=(',',':'),ensure_ascii=False)+'\n').encode('utf-8')).hexdigest()
 benchmark=json.loads((out/f'{key}-benchmark.json').read_text(encoding='utf-8'))
 checks[key]={'sameActualEngineAcrossRuns':report['argentumCommit']==runtime,'sameCompleteReportExceptRuntimeProvenance':digest==expected['sha256CanonicalJsonUtf8'],'zeroConfirmedBenchmarkMismatches':benchmark['numerators']['confirmed_mismatch_cases']==0}
checks['identicalDiffEmpty']=not json.loads((out/'diff-identical.json').read_text(encoding='utf-8'))['changes']
passed=all(all(v.values()) if isinstance(v,dict) else v for v in checks.values())
(out/'comparison-summary.json').write_text(json.dumps({'passed':passed,'actualEngineCommit':runtime,'historicalBaselineEngineCommit':pin,'normalization':'Only top-level runtime revision and fork native source URL revision; historical evidence bindings untouched','checks':checks},indent=2)+'\n',encoding='utf-8')
print('Fork catalog comparison:',passed);sys.exit(0 if passed else 1)
