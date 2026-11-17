"""Archive final JUnit observations and record exact Git/source provenance."""
from pathlib import Path
import hashlib, json, subprocess, zipfile

ENGINE='56eeb394ef4fd2b9b7467b2ae95b3ccf04ac0906'
APP='ccaf1926ec754799711fa921466de2f636137e7e'
OUT=Path('.integration-tools/delivery-evidence')
OUT.mkdir(exist_ok=True)
def git(repo,*args):
    return subprocess.check_output(['git','-C',repo,*args],text=True).strip()
assert git('argentum-full-upstream','rev-parse','HEAD')==ENGINE
assert git('mtg-full-upstream','rev-parse','HEAD')==APP
assert not git('argentum-full-upstream','status','--porcelain')
assert not git('mtg-full-upstream','status','--porcelain')
for ancestor in ('bd26b756b8b4d347f19e92bc77ef020919ebfa48','21bc1908bad22b3a6d4c84e5447d7b736655dbd0'):
    subprocess.run(['git','-C','argentum-full-upstream','merge-base','--is-ancestor',ancestor,ENGINE],check=True)
subprocess.run(['git','-C','argentum-full-upstream','diff','--check','21bc1908bad22b3a6d4c84e5447d7b736655dbd0'],check=True)
assert not git('mtg-full-upstream','diff','e0fa95970f546c19b79e32bc323af74367d6c353','--','src/main/resources','src/test/resources')
roots=[Path('argentum-full-upstream/mtg-sdk/build/test-results/test'),Path('argentum-full-upstream/rules-engine/build/test-results/test'),Path('argentum-full-upstream/mtg-sets/build/test-results/test'),Path('mtg-full-upstream/build/test-results/test')]
roots+=sorted(Path('argentum-full-upstream/mtg-sets').glob('*/tests/build/test-results/test'))
archive=Path('mtg-full-upstream/generated/upstream-integration/delivery-junit-56eeb.zip')
with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED) as z:
    for root in roots:
        for xml in sorted(root.glob('TEST-*.xml')):z.write(xml,xml.as_posix())
raw_catalog=Path('mtg-full-upstream/generated/upstream-integration/native-catalog-raw-results/test')
raw_archive=archive.with_name('catalog-raw-junit-3de9082.zip')
with zipfile.ZipFile(raw_archive,'w',compression=zipfile.ZIP_DEFLATED) as z:
    for xml in sorted(raw_catalog.glob('TEST-*.xml')):z.write(xml,xml.name)
def archive_receipt(path):
    return {'path':str(path.resolve()),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'bytes':path.stat().st_size}
receipt={
    'application':APP,'engine':ENGINE,
    'applicationSrcTree':git('mtg-full-upstream','rev-parse',APP+':src'),
    'applicationWebTree':git('mtg-full-upstream','rev-parse',APP+':web'),
    'baselineWebTree':git('mtg-full-upstream','rev-parse','e0fa95970f546c19b79e32bc323af74367d6c353:web'),
    'packagedApplicationMetadata':Path('mtg-full-upstream/build/generated/engine-resources/application-build.properties').read_text(),
    'packagedEngineMetadata':Path('mtg-full-upstream/build/generated/engine-resources/engine-build.properties').read_text(),
    'mergeParents':git('argentum-full-upstream','rev-list','--parents','-n','1','8002e26a974775203973cc7014878c5d4f70fa71').split(),
    'integratedUpstream':'21bc1908bad22b3a6d4c84e5447d7b736655dbd0',
    'deliveryUpstreamRecheck':'ec70462e5789af7789e0135d1e9a85bd607848f2',
    'upstreamCommitsNotIntegrated':4,
    'forkTrackingMain':git('argentum-full-upstream','rev-parse','origin/main'),
    'protectedEngineAppStable':git('argentum-engine','rev-parse','app-stable'),
    'protectedApplicationOriginMain':git('mtg-deckbuilding-assistant','rev-parse','origin/main'),
    'existingLocalApplicationMain':git('mtg-deckbuilding-assistant','rev-parse','main'),
    'primaryEngineCheckout':{'branch':git('argentum-engine','branch','--show-current'),'head':git('argentum-engine','rev-parse','HEAD'),'status':git('argentum-engine','status','--porcelain')},
    'primaryApplicationCheckout':{'branch':git('mtg-deckbuilding-assistant','branch','--show-current'),'head':git('mtg-deckbuilding-assistant','rev-parse','HEAD'),'status':git('mtg-deckbuilding-assistant','status','--porcelain')},
    'junitArchive':archive_receipt(archive),
    'earlierNativeObservation':{'engine':'3de9082227b0269ba6991551bc4d0395c5741619','archive':archive_receipt(archive.with_name('native-junit-3de9082.zip'))},
    'rawCatalogObservation':{'engine':'3de9082227b0269ba6991551bc4d0395c5741619','archive':archive_receipt(raw_archive)},
    'sourceResourcesChanged':False,
    'note':'Observation receipt, not a qualification record. Raw catalog CRLF failure receipt is retained separately; this archive contains the final diagnostic canonical catalog replay. Subsequent application commits add only this report and evidence documentation.'
}
assert receipt['protectedEngineAppStable']=='bd26b756b8b4d347f19e92bc77ef020919ebfa48'
assert receipt['protectedApplicationOriginMain']=='e0fa95970f546c19b79e32bc323af74367d6c353'
assert receipt['existingLocalApplicationMain']=='f039a6bcde462424d9d3a82a06819abbe418a978'
assert receipt['applicationWebTree']==receipt['baselineWebTree']
(OUT/'revision-receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps(receipt,indent=2))
