import collections,hashlib,json,pathlib
r=pathlib.Path(__file__).resolve().parent.parent/'mtg-native-consolidation/generated/native-consolidation'
summaries=json.loads((r/'production-comparison/summary.json').read_text())
decisions=json.loads((r/'production-comparison/decision-review.json').read_text())
out=[]
for row,review in zip(summaries,decisions):
    assert row['commander'].split(',')[0].lower() in review['report']
    sections={k:{'beforeRecords':v['beforeRecords'],'afterRecords':v['afterRecords'],'rawChangedCards':v['rawChangedCards'],'revisionUrlOnlyCards':v['revisionUrlOnlyCards'],'changedMigrated':v['changedMigrated'],'changedCommonNativeCount':len(v['changedCommonNative'])} for k,v in row['sections'].items()}
    out.append({k:row[k] for k in ['commander','objective','before','after','commonMechanicalCards']} | {'newNativeCards':len(row['newNativeCards']),'newNativeNamesSha256':hashlib.sha256('\n'.join(row['newNativeCards']).encode()).hexdigest(),'removedCards':row['removedCards'],'migratedInCommon':row['migratedInCommon'],'sections':sections,'samePathVerdictChanges':[x for x in review['decisionChanges'] if x['before'] is not None and x['after'] is not None],'addedOrRemovedPaths':review['decisionChanges'],'changedResultFields':review['changedResultFields'],'nativeOperationChangesAtSamePath':review['nativeOperationChangesAtSamePath']})
receipt={'afterApplication':(r/'production-after/application-build.properties').read_text().strip(),'observedMode':'NORMAL_PRODUCTION_CATALOG_INCLUDING_EXTENSIONS_AND_ACTIVE_ELIGIBILITY_CORRECTIONS','reports':out,'limits':summaries[0]['limits'],'rawReports':'generated/native-consolidation/production-after; before reports in sibling mtg-upstream-baseline/generated/native-consolidation-before; complete card populations and raw changed sections retained separately in generated/native-consolidation/production-comparison'}
# Do not guess the old application SHA: use the packaged runtime receipt.
receipt['beforeApplication']=(r.parent.parent.parent/'mtg-upstream-baseline/generated/native-consolidation-before/application-build.properties').read_text().strip()
(r/'production-comparison-receipt.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print('reports',len(out),'bytes',(r/'production-comparison-receipt.json').stat().st_size)
