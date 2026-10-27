#!/usr/bin/env python3
"""Assess fixed source-led labels against the full-snapshot artifact; never tune or alter associations."""
import argparse,collections,gzip,hashlib,json,pathlib
ROOT=pathlib.Path(__file__).resolve().parent.parent
def read(p):return json.loads(pathlib.Path(p).read_text(encoding='utf-8-sig'))
def main(args):
 artifact=read(args.artifact);context=artifact['context'];assignments=artifact['assignments']
 held=read(ROOT/'reference/phase28/held-out-review.json')
 algorithm=ROOT/'cli/src/main/kotlin/dev/mtg/assistant/coverage/OracleEnrichment.kt'
 assert hashlib.sha256(algorithm.read_bytes().replace(b'\r\n',b'\n')).hexdigest()==held['frozenAlgorithmSha256'],'Held-out assessment must use the frozen rules'
 assert context['snapshotSha256']==read(ROOT/'reference/phase27/snapshot.manifest.json')['sha256']
 assert context['argentumCommit']==(ROOT/'ARGENTUM_COMMIT').read_text().strip()
 rows={r['native']['name']:r for r in assignments}
 byid=collections.defaultdict(list)
 for a in assignments:
  if a['oracleId'] is not None:byid[a['oracleId']].append(a)
 result={'context':context,'frozenRulesCommit':held['frozenCommit'],'frozenAlgorithmSha256':held['frozenAlgorithmSha256'],'reviewLimit':held['reviewLimit'],'subsets':{},'nativeProfile':{}}
 for subset in ['development','held-out']:
  review=read(ROOT/f'reference/phase28/{subset}-review.json');observations=[]
  totals=dict(correctConfidentMatches=0,missedConfidentMatches=0,incorrectConfidentMatches=0,appropriatelyAmbiguous=0,correctMissingDefinitions=0,manualReviewRequired=0)
  for case in review['cases']:
   expected=case['expected'];oid=case['externalOracleId'];claims=byid[oid]
   native=rows.get(case['nativeName']);observed=native['confidence'] if native else 'MISSING'
   correct=False
   if expected=='CONFIDENT':
    if len(claims)==1 and claims[0]['native']['name']==case['nativeName'] and native['oracleId']==oid:totals['correctConfidentMatches']+=1;correct=True
    elif claims:totals['incorrectConfidentMatches']+=1
    else:totals['missedConfidentMatches']+=1;totals['manualReviewRequired']+=1
   elif expected=='AMBIGUOUS':
    assert native is not None and observed=='AMBIGUOUS' and oid in native['candidateOracleIds']
    if claims:totals['incorrectConfidentMatches']+=1
    else:totals['appropriatelyAmbiguous']+=1;totals['manualReviewRequired']+=1;correct=True
   else:
    assert not any(a['oracleId'] is None and oid in a['candidateOracleIds'] for a in assignments),'Expected missing physical definition has an unresolved native candidate'
    if claims:totals['incorrectConfidentMatches']+=1
    else:totals['correctMissingDefinitions']+=1;correct=True
   observations.append({'name':case['externalName'],'oracleId':oid,'nativeName':case['nativeName'],'expected':expected,'observed':observed,'correct':correct})
  assert totals['incorrectConfidentMatches']==0 and totals['missedConfidentMatches']==0,totals
  result['subsets'][subset]={'totals':totals,'cases':observations}
 result['nativeProfile']={'nativePhysicalDefinitions':len(assignments),'missingNativeOracleId':sum(a['native']['oracleId'] is None for a in assignments),'confidence':dict(collections.Counter(a['confidence'] for a in assignments)),'methods':dict(collections.Counter(a['method'] for a in assignments)),'unassociated':[{'name':a['native']['name'],'confidence':a['confidence'],'method':a['method'],'reasons':a['evidence']} for a in assignments if a['confidence']!='CONFIDENT']}
 for slug in ['judith','krenko','oviya']:
  assert read(ROOT/f'generated/phase28/after/coverage-{slug}.json')['enrichment']==context
 current=read(ROOT/'reference/phase28/metadata-current-check.json')
 archived={c['oracle_id']:c for c in read(ROOT/'reference/phase28/development-oracle.json')}
 result['metadataFindings']=[]
 for source in current['cards']:
  a=rows[source['name']];m=a['metadata'];assert a['oracleId']==source['oracle_id'];assert set(m['externalIdentity'])==set(source['color_identity'])
  impacts={}
  for slug in ['judith','krenko','oviya']:
   allowed=set(read(ROOT/f'generated/phase28/after/coverage-{slug}.json')['allowedIdentity'])
   impacts[slug]={'currentNativeColorEligible':m['nativeIdentity'] is not None and set(m['nativeIdentity'])<=allowed,'wouldBeColorEligibleIfReviewedCorrectionApplied':set(m['externalIdentity'])<=allowed}
  result['metadataFindings'].append({'name':source['name'],'oracleId':a['oracleId'],'classification':'INCORRECT_PRESENT_NATIVE_COLOR_OVERRIDE' if m['nativeIdentity'] is not None else 'MISSING_NATIVE_COLOR_OVERRIDE','nativeIdentity':m['nativeIdentity'],'sdkFallbackIdentity':m['derivedNativeIdentity'],'snapshotIdentity':m['externalIdentity'],'currentIdentity':source['color_identity'],'currentOracleTextAgreesWithArchivedSnapshot':source['oracle_text']==archived[source['oracle_id']]['oracle_text'],'currentCheckDate':current['retrievedDate'],'currentSource':source['scryfall_uri'],'candidateCorrections':m['candidateCorrections'],'hypotheticalColorEligibilityImpacts':impacts,'correctionsApplied':False})
 args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(json.dumps(result,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
 print(json.dumps({k:v['totals'] for k,v in result['subsets'].items()},indent=2))
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--artifact',type=pathlib.Path,default=ROOT/'generated/phase28/after/enrichment.json');p.add_argument('--output',type=pathlib.Path,default=ROOT/'generated/phase28/mapping-review.json');main(p.parse_args())
