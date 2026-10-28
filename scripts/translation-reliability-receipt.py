#!/usr/bin/env python3
"""Strict receipt for this bounded reliability experiment; reuses existing comparison outputs."""
import hashlib,json,pathlib,xml.etree.ElementTree as ET
ROOT=pathlib.Path(__file__).resolve().parent.parent
GEN=ROOT/'generated/translation-reliability';OUT=ROOT/'reference/translation-reliability'
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def save(p,v):p.write_text(json.dumps(v,indent=2,ensure_ascii=False)+'\n',encoding='utf-8',newline='\n')
def metadata_only(v):
 if isinstance(v,dict):return {k:metadata_only(x) for k,x in v.items() if k!='translationAssurance'}
 if isinstance(v,list):return [metadata_only(x) for x in v]
 return v
sword='Sword of Truth and Justice'
proof={'implementationCommit':'4a69578f14c6425e1f50a37b64d6a271808aabe0','fullRunLogSha256':digest(GEN.parent/'reliability-full-tests.log'),'baseCommit':'4e047c796e2fd88335fa65fd95ed6fa745f48bd7','argentumCommit':(ROOT/'ARGENTUM_COMMIT').read_text().strip(),'comparisons':{},'coverage':{},'testSuites':{},'measurements':{}}
for stage in ['before','after']:
 rows=read(GEN/stage/'timings.json');proof['measurements'][stage]={'wallSeconds':round(sum(r['wallSeconds'] for r in rows),3),'commands':[{k:r[k] for k in ['command','wallSeconds','peakWorkingSetBytes']} for r in rows]}
for who in ['judith','krenko','oviya']:
 before=read(GEN/'before'/f'coverage-{who}.json');after=read(GEN/'after'/f'coverage-{who}.json')
 assert before['counts']==after['counts'],('coverage counts',who)
 b={r['external']['oracleId']:r for r in before['records']};a={r['external']['oracleId']:r for r in after['records']};assert b.keys()==a.keys()
 changed=[]
 for oid in b:
  old=metadata_only(b[oid]);new=metadata_only(a[oid])
  if old!=new:
   assert old['external']['name']==sword,(who,old['external']['name'])
   assert {k:v for k,v in old.items() if k!='objectives'}=={k:v for k,v in new.items() if k!='objectives'}
   changed.append({'card':sword,'before':old['objectives'],'after':new['objectives']})
 proof['coverage'][who]={'eligibleOracleCards':len(b),'countsUnchanged':True,'nonAssuranceDifferences':changed,'beforeSha256':digest(GEN/'before'/f'coverage-{who}.json'),'afterSha256':digest(GEN/'after'/f'coverage-{who}.json')}
 for letter in ['a','b','c']:
  label=f'{who}-{letter}';old=metadata_only(read(GEN/'before'/f'{label}.json'));new=metadata_only(read(GEN/'after'/f'{label}.json'))
  assert old['eligibility']==new['eligibility'];assert old['mechanicalInputCards']==new['mechanicalInputCards']
  count_changes={k:[old['counts'][k],new['counts'][k]] for k in old['counts'] if old['counts'][k]!=new['counts'][k]}
  assert set(count_changes)<=set(['unsupported_structure_records','cards_with_unsupported_structures']),label
  def retained(r):return {k:([x for x in v if not(isinstance(x,dict) and x.get('card')==sword)] if isinstance(v,list) and k not in ['eligibility'] else v) for k,v in r.items() if k!='counts'}
  assert retained(old)==retained(new),label
  proof['comparisons'][label]={'eligibilityAndInputsUnchanged':True,'retainedEvidenceExceptCorrectedSwordIdentical':True,'countChanges':count_changes,'correctedSwordEvidenceChanged':{k:[v,new[k]] for k,v in old.items() if k not in ['counts'] and v!=new[k]},'beforeSha256':digest(GEN/'before'/f'{label}.json'),'afterSha256':digest(GEN/'after'/f'{label}.json')}
  # Keep only bounded changed-card rows, never duplicate whole catalog evidence.
  def changed_rows(stage):
   rows=[x for x in stage if isinstance(x,dict) and x.get('card')==sword]
   return {'rowCount':len(rows),'sha256':hashlib.sha256(json.dumps(rows,sort_keys=True,separators=(',',':'),ensure_ascii=False).encode()).hexdigest(),'nativeTypes':sorted({x['nativeType'] for x in rows if 'nativeType' in x})}
  proof['comparisons'][label]['correctedSwordEvidenceChanged']={k:[changed_rows(stage) for stage in vs] for k,vs in proof['comparisons'][label]['correctedSwordEvidenceChanged'].items()}
  report=read(GEN/'after'/f'{label}.json');assurance=report['translationAssurance']
  assert len(assurance)==22 and all(x['evidenceCurrent'] for x in assurance)
  assert all(x['qualification']==('BLOCKED' if x['card']=='Tatterkite' else 'PROVISIONAL') for x in assurance)
  proof['comparisons'][label]['assurance']={'provisional':21,'blocked':1,'independentlyQualified':0}
for f in (ROOT/'cli/build/test-results/test').glob('*.xml'):
 s=ET.parse(f).getroot();assert all(int(s.attrib[k])==0 for k in ['failures','errors','skipped']),f
 proof['testSuites'][s.attrib['name']]={'tests':int(s.attrib['tests']),'seconds':float(s.attrib['time'])}
assert len(proof['testSuites'])>=56 and 'dev.mtg.assistant.TranslationMutationTest' in proof['testSuites']
proof['totalTests']=sum(v['tests'] for v in proof['testSuites'].values())
proof['knownEngineFailure']='TK2 source expectation fails in opt-in reproduction; standard suite retains an explicit expected-failure contract. Not an Oracle correctness pass.'
mut=read(OUT/'mutations.json')['outcomes'];assert mut==read(GEN/'mutations.json'), 'Final full-run mutation results changed';assert len(mut)==9 and all(m['validatorErrors']==0 for m in mut)
assert sum(m['executionDetected'] for m in mut)==8 and sum(m['executionConfounded'] for m in mut)==1
assert all(m['sourceProbeViolations'] for m in mut)
proof['mutationResults']={'count':9,'validatorRejected':0,'sourceProbeDetected':9,'executionDiscriminated':8,'confoundedByFailingControl':1,'qualifiedWithoutIndependentApproval':0,'scope':'Known injected cases, same-agent design; not an estimate of verification accuracy'}
save(OUT/'verification.json',proof)
print('Verified',proof['totalTests'],'test contracts, nine discovery comparisons, unchanged coverage pools/counts and explicit assurance states')
