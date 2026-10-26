#!/usr/bin/env python3
"""Explicit lean packaging; generated catalog dumps/caches/upstream are never traversed."""
import pathlib,zipfile,hashlib,json,sys
root=pathlib.Path(__file__).resolve().parent.parent
output=pathlib.Path(sys.argv[1] if len(sys.argv)>1 else 'MTG_Deckbuilding_Assistant_Phase2_5.zip').resolve()
files=set()
def add(rel):
 p=root/rel
 if p.is_file():files.add(p)
def tree(rel):
 p=root/rel
 if p.exists():
  for f in p.rglob('*'):
   if f.is_file() and not any(x in ['build','.gradle','__pycache__'] for x in f.relative_to(p).parts):files.add(f)
for n in ['README.md','BUILD_VERIFICATION.md','PHASE2_5_IMPLEMENTATION.md','KNOWN_LIMITATIONS.md','THIRD_PARTY_NOTICES.md','ARGENTUM_COMMIT','argentum.init.gradle','dev','dev.ps1','gradlew','gradlew.bat','.gitignore','APPLICATION_REVISION.txt','SOURCE_MANIFEST.sha256']:add(n)
for n in ['cli/src','gradle','benchmark','licenses','reference','samples/phase2_5','verification/phase2_5']:tree(n)
add('cli/build.gradle.kts')
for n in ['setup.sh','setup.ps1','create-phase24-baseline.py','package-phase25.py']:add('scripts/'+n)
add('verification/hosted-proxy-runner.py')
files={p for p in files if p.name not in ['baseline-workflow.log','final-workflow.log']}
with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 for p in sorted(files):
  rel=p.relative_to(root);info=zipfile.ZipInfo('mtg-assistant-phase2_5/'+rel.as_posix());info.compress_type=zipfile.ZIP_DEFLATED
  info.external_attr=((0o100755 if p.name in ['gradlew','dev'] or p.suffix in ['.sh','.py'] else 0o100644)<<16)
  z.writestr(info,p.read_bytes())
assert output.stat().st_size<=20_971_520,'Hard ZIP size limit exceeded'
with zipfile.ZipFile(output) as z:assert z.testzip() is None
print(json.dumps(dict(file=str(output),bytes=output.stat().st_size,MiB=round(output.stat().st_size/1048576,3),sha256=hashlib.sha256(output.read_bytes()).hexdigest(),members=len(files))))
