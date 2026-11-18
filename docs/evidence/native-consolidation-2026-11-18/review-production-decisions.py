import collections,gzip,json,pathlib
root=pathlib.Path(__file__).resolve().parent.parent/'mtg-native-consolidation/generated/native-consolidation/production-comparison'
allrows=[]
for p in sorted(root.glob('*-changes.json.gz')):
    d=json.load(gzip.open(p,'rt',encoding='utf8')); fields=collections.Counter(); changes=[]; ops=[]
    key=lambda r:(r['face'],r['actionId'],r['abilityPath'],r['branch'])
    for name,v in d.get('results',{}).items():
        b={key(r):r for r in v['before']};a={key(r):r for r in v['after']}
        for k in sorted(b.keys()|a.keys()):
            x=b.get(k);y=a.get(k)
            if x is None or y is None or x['verdict']!=y['verdict']:
                changes.append({'card':name,'key':k,'before':None if x is None else {'verdict':x['verdict'],'operation':x['operation'],'mechanicalCompatibility':x['mechanicalCompatibility'],'policyAcceptance':x['policyAcceptance']},'after':None if y is None else {'verdict':y['verdict'],'operation':y['operation'],'mechanicalCompatibility':y['mechanicalCompatibility'],'policyAcceptance':y['policyAcceptance']}})
            if x is not None and y is not None:
                for f in sorted(x.keys()|y.keys()):
                    if x.get(f)!=y.get(f):fields[f]+=1
                if x.get('nativeOperation')!=y.get('nativeOperation'):ops.append({'card':name,'key':k,'before':x.get('nativeOperation'),'after':y.get('nativeOperation')})
    row={'report':p.name.removesuffix('-changes.json.gz'),'changedResultFields':dict(fields),'decisionChanges':changes,'nativeOperationChangesAtSamePath':ops}
    allrows.append(row)
    print(row['report'],'flips',sum(x['before'] is not None and x['after'] is not None for x in changes),'addedRemoved',len(changes),'operationChanges',len(ops),flush=True)
(root/'decision-review.json').write_text(json.dumps(allrows,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
