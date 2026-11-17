from pathlib import Path
import json, hashlib, xml.etree.ElementTree as E

out=Path('.integration-tools/delivery-evidence/catalog-verification.json')
earlier=json.loads(out.read_text())
root=Path('argentum-full-upstream/mtg-sets/build/test-results/test')
results=[E.parse(p).getroot() for p in sorted(root.glob('TEST-*.xml'))]
final={'engine':'56eeb394ef4fd2b9b7467b2ae95b3ccf04ac0906','suites':len(results),**{k:sum(int(r.get(k,0)) for r in results) for k in ['tests','failures','errors','skipped']}}
final['firstSuiteTimestamp']=min(r.get('timestamp') for r in results)
final['lastSuiteTimestamp']=max(r.get('timestamp') for r in results)
assert final['tests']==493 and final['suites']==21
assert final['failures']==final['errors']==final['skipped']==0
assert final['firstSuiteTimestamp']>='2026-11-17T21:48:'
log=Path('upstream-catalog-canonical-56eeb.log')
assert 'BUILD SUCCESSFUL' in log.read_text()
final['logSha256']=hashlib.sha256(log.read_bytes()).hexdigest()
final['explicitRerunTasks']=[':mtg-sets:processTestResources',':mtg-sets:test']
out.write_text(json.dumps({
    'rawWindowsRun':{'engine':'3de9082227b0269ba6991551bc4d0395c5741619',**earlier['rawWindowsRun']},
    'earlierCanonicalBuildResourceReplay':{'engine':'3de9082227b0269ba6991551bc4d0395c5741619',**earlier['canonicalBuildResourceReplay']},
    'finalCanonicalBuildResourceReplay':final,
    'initScriptSha256':hashlib.sha256(Path('.integration-tools/canonical-catalog-resources.init.gradle').read_bytes()).hexdigest(),
    'note':'Only copied build resources were canonicalized. Source golden Git blobs equal all 27 original fresh mismatches byte-for-byte. Original resource files and strict assertions remain unchanged. The raw failure observation belongs to 3de9082; final replay is a diagnostic, not a re-blessing.'
},indent=2)+'\n')
print(json.dumps(final))
