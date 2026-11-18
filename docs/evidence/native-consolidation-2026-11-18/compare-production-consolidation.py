import collections, hashlib, json, pathlib

root=pathlib.Path(__file__).resolve().parent.parent
before=root/'mtg-upstream-baseline/generated/native-consolidation-before'
after=root/'mtg-native-consolidation/generated/native-consolidation/production-after'
out=root/'mtg-native-consolidation/generated/native-consolidation/production-comparison'
out.mkdir(exist_ok=True)
migrated=set(['Bitterblossom','Galadhrim Ambush','Hour of Promise','Judith, the Scourge Diva',"Krenko's Command",'Labyrinth of Skophos','Might of Murasa','Oviya Pashiri, Sage Lifecrafter','Planewide Celebration','Spined Tyrranax','Throne of Makindi','Tyrite Sanctum'])
def read(p):
    raw=p.read_bytes(); return json.loads(raw),hashlib.sha256(raw).hexdigest()
def canon(v,sha):
    s=json.dumps(v,sort_keys=True,ensure_ascii=False,separators=(',',':'))
    for kind in ['blob','tree']:
        s=s.replace(f'https://github.com/jasullivan5/argentum-engine/{kind}/{sha}',f'https://github.com/jasullivan5/argentum-engine/{kind}/RUNTIME_SHA')
    return s
def groups(rows,pop):
    d=collections.defaultdict(list)
    for r in rows:
        if isinstance(r,dict) and r.get('card') in pop:d[r['card']].append(r)
    return d
summaries=[]
for p in sorted(before.glob('*.json')):
    q=after/p.name
    if not q.exists():raise RuntimeError(f'Missing final report {q}')
    b,bh=read(p);a,ah=read(q)
    assert (b['objective'],b['commander'],b['allowedIdentity'],b['conventionalQuery'])==(a['objective'],a['commander'],a['allowedIdentity'],a['conventionalQuery'])
    bp=set(b['mechanicalInputCards']);ap=set(a['mechanicalInputCards']);common=bp&ap
    eligible={r['card'] for r in b['eligibility']}&{r['card'] for r in a['eligibility']}
    sections={};details={}
    for key in sorted(b.keys()|a.keys()):
        if not isinstance(b.get(key),list) or not isinstance(a.get(key),list) or key in ['mechanicalInputCards','limitations','allowedIdentity']:continue
        pop=eligible if key=='eligibility' else common
        bg=groups(b[key],pop);ag=groups(a[key],pop)
        raw=[n for n in sorted(pop) if bg[n]!=ag[n]]
        changed=[n for n in raw if canon(bg[n],b['argentumCommit'])!=canon(ag[n],a['argentumCommit'])]
        sections[key]={'beforeRecords':sum(map(len,bg.values())),'afterRecords':sum(map(len,ag.values())),'rawChangedCards':len(raw),'revisionUrlOnlyCards':len(raw)-len(changed),'changedMigrated':sorted(set(changed)&migrated),'changedCommonNative':sorted(set(changed)-migrated)}
        if key not in ['actions','eligibility','structuralSupport']:
            details[key]={n:{'before':bg[n],'after':ag[n]} for n in changed}
    summary={'commander':b['commander'],'objective':b['objective'],'before':{'sha256':bh,'engine':b['argentumCommit'],'mechanicalCards':len(bp),'counts':b['counts']},'after':{'sha256':ah,'engine':a['argentumCommit'],'mechanicalCards':len(ap),'counts':a['counts']},'commonMechanicalCards':len(common),'newNativeCards':sorted(ap-bp),'removedCards':sorted(bp-ap),'migratedInCommon':sorted(common&migrated),'sections':sections,'limits':'Normal production catalogs including extensions and eligibility corrections. Only exact runtime repository URLs normalized. UNKNOWN, prerequisites, definition trees and original evidence bindings retained. Structural agreement is not fidelity or whole-card qualification.'}
    (out/p.name).write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    import gzip
    with gzip.open(out/(p.stem+'-changes.json.gz'),'wt',encoding='utf-8') as f:json.dump(details,f,ensure_ascii=False)
    summaries.append(summary)
    print(p.name,len(common),len(ap-bp),{k:len(v['changedCommonNative']) for k,v in sections.items()},flush=True)
    del b,a,details
(out/'summary.json').write_text(json.dumps(summaries,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
