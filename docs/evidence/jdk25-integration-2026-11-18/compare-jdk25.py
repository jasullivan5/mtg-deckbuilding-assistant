"""Full production report comparison: only current runtime SHA provenance may differ."""
import hashlib,json,pathlib
root=pathlib.Path(__file__).resolve().parent.parent/'mtg-native-consolidation'
before=root/'generated/native-consolidation/production-after'
after=root/'generated/jdk25-integration/production-after-529a78e'
out=root/'generated/jdk25-integration/receipts';out.mkdir(parents=True,exist_ok=True)
application='529a78ea38b12b13598fd3c06f2e59ec9a4e9abe'
assert (after/'application-build.properties').read_text().strip()=='revision='+application
rows=[]
baseline=json.loads((root/'docs/evidence/native-consolidation-2026-11-18/production-final-replay.json').read_bytes())
baselineHashes={r['report']:r['finalSha256'] for r in baseline['reports']}
def canonical(value,revision):
    value=json.dumps(value,sort_keys=True,ensure_ascii=False,separators=(',',':'))
    for kind in ('blob','tree'):
        value=value.replace('https://github.com/jasullivan5/argentum-engine/'+kind+'/'+revision,'https://github.com/jasullivan5/argentum-engine/'+kind+'/RUNTIME_SHA')
    return value
for p in sorted(before.glob('*.json')):
    q=after/p.name
    assert hashlib.sha256(p.read_bytes()).hexdigest()==baselineHashes[p.name],('JDK21 baseline changed',p.name)
    b=json.loads(p.read_bytes());a=json.loads(q.read_bytes())
    assert b.keys()==a.keys(),p.name
    differences=[];provenance=[]
    for k in b:
        if k=='argentumCommit':continue
        if b[k]!=a[k]:provenance.append(k)
        if canonical(b[k],b['argentumCommit'])!=canonical(a[k],a['argentumCommit']):differences.append(k)
    row=dict(report=p.name,beforeSha256=hashlib.sha256(p.read_bytes()).hexdigest(),afterSha256=hashlib.sha256(q.read_bytes()).hexdigest(),beforeEngine=b['argentumCommit'],afterEngine=a['argentumCommit'],mechanicalCards=len(a['mechanicalInputCards']),counts=a['counts'],runtimeUrlOnlySections=provenance,materialDifferences=differences)
    rows.append(row);print(p.name,'differences',differences,flush=True)
    del a,b
assert len(rows)==18,len(rows)
(out/'production-comparison.json').write_text(json.dumps(dict(application=application,reports=rows,normalization='Only top-level argentumCommit and literal current-runtime engine blob/tree SHA URL prefixes. All populations, lists and ordering, identities, actions, costs, targets, conditions, effects, UNKNOWN, unsupported, and historical assurance records compared without filtering.'),indent=2)+'\n',encoding='utf8')
assert not any(r['materialDifferences'] for r in rows), 'Material report differences require investigation'
