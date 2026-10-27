#!/usr/bin/env python3
"""Independent source, eligibility and retained-mechanics checks for the nine Phase 2.9 CLI contexts."""
import argparse,collections,gzip,hashlib,json,pathlib,xml.etree.ElementTree as ET
ROOT=pathlib.Path(__file__).resolve().parent.parent
BASE=ROOT/'generated/phase29'
SLUGS=['judith','krenko','oviya']
REMOVED={'judith':{'Heartwood Shard','Sunforger','Toxrill, the Corrosive'},'krenko':{'Heartwood Shard','Hooded Kavu','Sunforger'},'oviya':{'Sunforger'}}
ADDED={'Academy Manufactor','Afterthought Sentry'}
LETTERS=['a','b','c']
def read(p):return json.loads(pathlib.Path(p).read_text(encoding='utf-8-sig'))
def sha(p):
 with pathlib.Path(p).open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def save(p,data):
 p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def normalized(row):return {k:v for k,v in row.items() if k!='effectiveMetadata'}
def permitted(r):return {e['card'] for e in r['eligibility'] if e['format']=='MATCH' and e['deck']=='MATCH'}
def main(a):
 plan=read(ROOT/'cli/src/main/resources/eligibility/phase29.json')
 assert sha(a.snapshot)==plan['snapshot']['sha256']==plan['context']['snapshotSha256']
 with gzip.open(a.snapshot,'rt',encoding='utf-8') as f:bulk=[json.loads(l) for l in f]
 raw={c['oracle_id']:c for c in bulk}
 current=read(ROOT/'reference/phase29/current-source.json')
 sources={c['oracleId']:c for c in plan['sources']}
 afterArtifact=read(BASE/'after/enrichment.json')
 oldArtifact=read(BASE/'before/enrichment.json')
 assert afterArtifact==oldArtifact,'Frozen native identity associations or inputs unexpectedly changed'
 assignments={c['native']['name']:c for c in afterArtifact['assignments']}
 result={'baselineCommit':'7b28c068de23ba9476b9071d4d601cb511064a69','context':plan['context'],'appliedCorrections':[],'discovery':{},'coverage':{},'diagnosticReview':{},'tests':{},'benchmark':{},'timings':{},'artifacts':{},'limits':['Seven real correction cases were used during design; no new held-out accuracy claim. Phase 2.8 held-out identity tests remain unchanged.','Independent current source checks cover the seven activated records. Remaining discrepancy categories are archived-source diagnostics, not approval for repair.','Whole-command before timings overlap baseline test/build activity; do not interpret wall-time differences as isolated correction cost. Resolver validationMillis is measured separately.']}
 for r in plan['corrections']:
  source=raw[r['oracleId']];arch=sources[r['oracleId']]
  now=next(c for c in current['cards'] if c['oracle_id']==r['oracleId'])
  assert source['name']==arch['name']==r['native']['name']==now['name']
  assert source['color_identity']==arch['colorIdentity']==now['color_identity']
  assert source['legalities']['commander']==arch['legalities']['commander']==now['legalities']['commander']
  assert source.get('oracle_text')==arch['oracleText'] and source['type_line']==arch['typeLine'] and source.get('mana_cost')==arch['manaCost']
  assert assignments[r['native']['name']]['confidence']=='CONFIDENT' and assignments[r['native']['name']]['oracleId']==r['oracleId']
  assert len([c for c in bulk if c['name'].lower()==source['name'].lower()])==1,'Correction source needs full-name ambiguity review'
  assert r['approved']==(sorted(source['color_identity']) if r['field']=='FULL_COLOR_IDENTITY' else ['MATCH'])
  result['appliedCorrections'].append({k:r[k] for k in ['id','oracleId','field','expected','approved','sourceUri','verifiedAt','definitionSha256'] } | {'nativeName':r['native']['name'],'parentSnapshotVerified':True,'fullSnapshotAssociationVerified':True})
 corrected={c['native']['name'] for c in plan['corrections']}
 scopeNative={};scopeExternal={}
 for slug in SLUGS:
  for letter in LETTERS:
   beforePath=BASE/'before'/f'{slug}-{letter}.json';afterPath=BASE/'after'/f'{slug}-{letter}.json'
   b=read(beforePath);n=read(afterPath);old=permitted(b);new=permitted(n)
   assert old-new==REMOVED[slug],(slug,old-new)
   assert new-old==ADDED,(slug,new-old)
   assert b['argentumCommit']==n['argentumCommit']==plan['context']['argentumCommit']
   for key in ['objective','commander','allowedIdentity','conventionalQuery']:assert b[key]==n[key],key
   assert {r['card'] for r in b['eligibility']}=={r['card'] for r in n['eligibility']}
   oldRows={r['card']:r for r in b['eligibility']}
   for r in n['eligibility']:
    if r['card'] not in corrected:assert normalized(r)==oldRows[r['card']],r['card']
    else:
     m=r['effectiveMetadata'];assert m and m['corrections'][0]['state']=='APPLIED'
     assert m['originalCommanderLegality']==oldRows[r['card']]['format']
     assert m['commanderLegality']==r['format']
     expectedColors=sorted({'W':'WHITE','U':'BLUE','B':'BLACK','R':'RED','G':'GREEN'}[c] for c in m['identity'])
     assert r['identity']==expectedColors
     assert r['deck']==('MATCH' if set(expectedColors)<=set(n['allowedIdentity']) else 'NO_MATCH')
   common=old & new
   mechanical={}
   for key in ['actions','facts','results','unsupported','structuralSupport']:
    x=[r for r in b[key] if r['card'] in common];y=[r for r in n[key] if r['card'] in common]
    assert x==y,f'Unexplained retained mechanical evidence change: {slug}-{letter} {key}'
    assert all(r['card'] in old for r in b[key]) and all(r['card'] in new for r in n[key])
    mechanical[key]={'unchangedRetainedRows':len(x),'removedRows':sum(r['card'] in REMOVED[slug] for r in b[key]),'addedRows':sum(r['card'] in ADDED for r in n[key])}
   assert [c for c in b['mechanicalInputCards'] if c in common]==[c for c in n['mechanicalInputCards'] if c in common]
   assert n['counts']['commander_eligible_cards']==len(new)==len(old)-len(REMOVED[slug])+len(ADDED)
   assert n['counts']['commander_legal_cards']==b['counts']['commander_legal_cards']+1
   assert n['counts']['format_legality_unknown_cards']==b['counts']['format_legality_unknown_cards']-1
   assert n['counts']['identity_unknown_legal_cards']==b['counts']['identity_unknown_legal_cards']-1
   audit=read(BASE/'after'/f'diff-{slug}-{letter}.json')
   result['discovery'][f'{slug}-{letter}']={'nativeEligibleBefore':len(old),'nativeEligibleAfter':len(new),'removed':sorted(old-new),'admittedMissingMetadata':sorted(new-old),'mechanicalEvidence':mechanical,'unexpectedDifferences':0,'existingAuditDiffCounts':audit['counts'],'beforeSha256':sha(beforePath),'afterSha256':sha(afterPath)}
  scopeNative[slug]=permitted(read(BASE/'before'/f'{slug}-c.json'))
  b=read(BASE/'before'/f'coverage-{slug}.json');n=read(BASE/'after'/f'coverage-{slug}.json')
  assert b['schemaVersion']==2 and n['schemaVersion']==3
  assert b['enrichment']==n['enrichment']==plan['context']
  beforeRows={r['external']['oracleId']:r for r in b['records']};afterRows={r['external']['oracleId']:r for r in n['records']}
  assert beforeRows.keys()==afterRows.keys()
  allowed=set(n['allowedIdentity']);land={'Plains':'W','Island':'U','Swamp':'B','Mountain':'R','Forest':'G'}
  eligible=set()
  for c in bulk:
   if c['legalities'].get('commander')!='legal' or not set(c['color_identity'])<=allowed:continue
   types=' '.join([c.get('type_line','')]+[f.get('type_line','') for f in c.get('card_faces',[])]).replace('—',' ').split()
   if any(t in types and color not in allowed for t,color in land.items()):continue
   eligible.add(c['oracle_id'])
  assert eligible==afterRows.keys()
  scopeExternal[slug]=eligible
  for oid,old in beforeRows.items():
   new=afterRows[oid]
   assert old['external']==new['external'] and old['mapping']['association']==new['mapping']['association']
   assert old['mapping']['metadata']==new['mapping']['metadata'],'Original discrepancy history was erased'
   if new['mapping'].get('effectiveMetadata',{}):
    if new['mapping']['effectiveMetadata']['nativeName'] not in ADDED:assert old['objectives']==new['objectives']
  for key in ['eligible_oracle_cards','confident_native_identity_associations','native_metadata_discrepancies','identity_review_required','ambiguous_mapping','native_definition_missing']:assert b['counts'][key]==n['counts'][key],key
  assert sum(n['counts'][s] for s in ['native_available','native_metadata_unresolved','native_mechanically_unresolved','native_definition_missing','ambiguous_mapping','native_identity_review_required'])==len(eligible)
  assert len(n['eligibilityCorrections'])==7
  result['coverage'][slug]={'before':b['counts'],'after':n['counts'],'externalPoolUnchanged':True,'originalDiscrepanciesRetained':True,'correctionValidationMillis':n['correctionValidationMillis'],'timingsMillis':{'before':b['timingsMillis'],'after':n['timingsMillis']},'correctionImpacts':n['eligibilityCorrections']}
 # Diagnostic totals overlap; examples below remain unapproved unless one of the seven records.
 confident=[c for c in afterArtifact['assignments'] if c['confidence']=='CONFIDENT']
 issueCounts=collections.Counter(i for c in confident for i in c['metadata']['issues'])
 examples=['Bloodletter Quill','Boros Fury-Shield','Brutal Cathar','Abzan Devotee','Aberrant Mind Sorcerer','Accomplished Automaton','Academic Ascent','Aerid Konstrari']
 result['diagnosticReview']={'globalOverlappingIssueCounts':dict(issueCounts),'multiFaceIdentityDisagreements':[{'name':c['native']['name'],'layout':c['native']['layout'],'native':c['metadata']['nativeIdentity'],'external':c['metadata']['externalIdentity']} for c in confident if c['native']['layout']!='normal' and 'NATIVE_EXTERNAL_IDENTITY_DISAGREEMENT' in c['metadata']['issues']],'representatives':[{'name':name,'oracleId':assignments[name]['oracleId'],'metadata':assignments[name]['metadata'],'nativeFaces':assignments[name]['native']['faces'],'externalOracleText':raw[assignments[name]['oracleId']].get('oracle_text'),'externalFaces':raw[assignments[name]['oracleId']].get('card_faces',[]),'scopedNativeIncluded':{s:name in scopeNative[s] for s in SLUGS},'scopedExternalEligible':{s:assignments[name]['oracleId'] in scopeExternal[s] for s in SLUGS},'applied':False,'source':raw[assignments[name]['oracleId']]['scryfall_uri']} for name in examples],'sourceVersionCheck':'Current and archived metadata agree for all seven activated records; no extrapolation to unreviewed discrepancies'}
 for p in (ROOT/'cli/build/test-results/test').glob('*.xml'):
  e=ET.parse(p).getroot();result['tests'][e.get('name')]={k:int(e.get(k,'0')) for k in ['tests','failures','errors','skipped']}
 result['testTotals']={k:sum(t[k] for t in result['tests'].values()) for k in ['tests','failures','errors','skipped']}
 assert result['testTotals']['failures']==result['testTotals']['errors']==result['testTotals']['skipped']==0
 benchmark=read(ROOT/'generated/phase25/benchmark-all-colors.json')
 result['benchmark']={'statuses':dict(collections.Counter(c['status'] for c in benchmark['cases'])),'sha256':sha(ROOT/'generated/phase25/benchmark-all-colors.json')}
 assert result['benchmark']['statuses'].get('CONFIRMED_MISMATCH',0)==0
 for stage in ['before','after']:
  measurements=read(BASE/stage/'timings.json')
  for m in measurements:m['arguments']=[s.replace(str(ROOT),'.') for s in m['arguments']]
  result['timings'][stage]=measurements
  recheck=BASE/stage/'coverage-recheck-timings.json'
  if recheck.exists():
   extra=read(recheck)
   for m in extra:m['arguments']=[s.replace(str(ROOT),'.') for s in m['arguments']]
   result['timings'][stage+'CoverageRecheck']=extra
  result['artifacts'][stage]={p.name:{'bytes':p.stat().st_size,'sha256':sha(p)} for p in (BASE/stage).glob('*.json') if p.name!='enrichment.json'}
 result['retainedGeneratedBytes']=sum(p.stat().st_size for p in BASE.rglob('*') if p.is_file())
 result['correctionResourceBytes']=(ROOT/'cli/src/main/resources/eligibility/phase29.json').stat().st_size
 save(BASE/'verification.json',result)
 print(json.dumps({'tests':result['testTotals'],'benchmark':result['benchmark'],'discovery':{k:{f:v[f] for f in ['nativeEligibleBefore','nativeEligibleAfter','removed','admittedMissingMetadata','unexpectedDifferences']} for k,v in result['discovery'].items()}},indent=2))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--snapshot',type=pathlib.Path,required=True);main(p.parse_args())
