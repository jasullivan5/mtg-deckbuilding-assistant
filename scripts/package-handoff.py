#!/usr/bin/env python3
import pathlib,tempfile,subprocess,json,zipfile,hashlib,sys
root=pathlib.Path(__file__).resolve().parent.parent
out=pathlib.Path(sys.argv[1]).resolve()
def git(*a,cwd=root):return subprocess.check_output(['git','-C',str(cwd),*a],text=True).strip()
assert not git('status','--porcelain'),'Commit reviewed changes before packaging'
with tempfile.TemporaryDirectory() as td:
 dest=pathlib.Path(td)/'mtg-deckbuilding-assistant'
 subprocess.run(['git','clone','--quiet','--no-local',str(root),str(dest)],check=True)
 for line in git('for-each-ref','--format=%(refname) %(objectname)','refs/heads','refs/tags').splitlines():
  ref,sha=line.split();subprocess.run(['git','-C',str(dest),'update-ref',ref,sha],check=True)
 subprocess.run(['git','-C',str(dest),'remote','remove','origin'],check=True)
 assert not (dest/'.git/objects/info/alternates').exists()
 subprocess.run(['git','-C',str(dest),'fsck','--full'],check=True,stdout=subprocess.DEVNULL)
 assert not git('status','--porcelain',cwd=dest),'Exported checkout is not clean'
 receipt={'finalCommit':git('rev-parse','HEAD'),'baselineCommit':git('rev-parse','phase2.5-baseline^{commit}'),'fullGitHistoryIncluded':True,'upstreamPin':(root/'ARGENTUM_COMMIT').read_text().strip(),'publication':'NOT_PERFORMED','status':'RECOVERY_CHECKPOINT; see HANDOFF_VERIFICATION.md'}
 (dest/'REPOSITORY_STATE.json').write_text(json.dumps(receipt,indent=2)+'\n')
 files=[p for p in dest.rglob('*') if p.is_file() and not ('.git' in p.parts and any(x in p.relative_to(dest).parts for x in ['logs','hooks']))]
 receipt['trackedBytes']=sum((dest/p).stat().st_size for p in git('ls-files','-z',cwd=dest).split('\0') if p)
 receipt['gitMetadataBytes']=sum(p.stat().st_size for p in files if '.git' in p.relative_to(dest).parts)
 receipt['exportedUncompressedBytes']=sum(p.stat().st_size for p in files)
 (dest/'REPOSITORY_STATE.json').write_text(json.dumps(receipt,indent=2)+'\n')
 with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
  for p in sorted(files):
   info=zipfile.ZipInfo('mtg-deckbuilding-assistant/'+p.relative_to(dest).as_posix());info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=(0o100000|(p.stat().st_mode&0o777))<<16;z.writestr(info,p.read_bytes())
 assert out.stat().st_size<=20_971_520
 with zipfile.ZipFile(out) as z:assert z.testzip() is None
 print(json.dumps({'file':str(out),'bytes':out.stat().st_size,'sha256':hashlib.sha256(out.read_bytes()).hexdigest(),'repository':receipt},indent=2))
