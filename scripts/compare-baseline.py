#!/usr/bin/env python3
"""Strict compact evidence comparison; preserves duplicates, never interprets MTG mechanics."""
import pathlib,json,collections,hashlib,sys
root=pathlib.Path(__file__).resolve().parent.parent;out=pathlib.Path(sys.argv[1]);checks={}
for who in ['judith','krenko']:
 for letter in 'abc':
  key=f'{who}-{letter}';before=json.loads((root/f'samples/phase2_5/final-{key}.json').read_text());after=json.loads((out/f'{key}.json').read_text())
  identity=lambda r:tuple(r[x] for x in ['card','face','actionId','abilityPath','branch','operation','costOrEffect','verdict','mechanicalCompatibility','policyAcceptance','localSupport'])
  same=collections.Counter(identity(r) for r in before['results'])==collections.Counter(identity(r) for r in after['results'])
  old=collections.Counter(r['evidenceSha256'] for r in before['results']);new=collections.Counter(hashlib.sha256(json.dumps(r,sort_keys=True).encode()).hexdigest() for r in after['results'])
  pool=hashlib.sha256('\n'.join(sorted(after['mechanicalInputCards'])).encode()).hexdigest()
  bench=json.loads((out/f'{key}-benchmark.json').read_text())
  checks[key]={'samePin':before['argentumCommit']==after['argentumCommit'],'samePool':before['eligibleInputSha256']==pool,'samePathwayDecisionMultiset':same,'sameEvidenceMultiset':old==new,'zeroConfirmedBenchmarkMismatches':bench['numerators']['confirmed_mismatch_cases']==0}
checks['identicalDiffEmpty']=not json.loads((out/'diff-identical.json').read_text())['changes']
passed=all(all(v.values()) if isinstance(v,dict) else v for v in checks.values())
(out/'comparison-summary.json').write_text(json.dumps({'passed':passed,'checks':checks},indent=2)+'\n');print('Catalog comparison:',passed)
sys.exit(0 if passed else 1)
