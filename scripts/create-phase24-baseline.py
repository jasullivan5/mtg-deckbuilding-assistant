#!/usr/bin/env python3
"""Restore original Phase 2.4 application sources into a separate development directory."""
import pathlib,shutil,subprocess,sys
root=pathlib.Path(__file__).resolve().parent.parent
dest=pathlib.Path(sys.argv[1]).resolve()
if dest.exists():raise SystemExit('Destination must not already exist')
dest.mkdir(parents=True)
for name in ['cli','gradle','scripts']:
 shutil.copytree(root/name,dest/name,ignore=shutil.ignore_patterns('build','__pycache__'))
for name in ['ARGENTUM_COMMIT','argentum.init.gradle','dev','dev.ps1','gradlew','gradlew.bat']:
 shutil.copy2(root/name,dest/name)
shutil.copytree(root/'benchmark',dest/'benchmark')
subprocess.run(['patch','--batch','--reverse','-p1','-i',str(root/'verification/phase2_5/phase24-to-phase25.patch')],cwd=dest,check=True)
if (root/'argentum').exists(): (dest/'argentum').symlink_to((root/'argentum').resolve(),target_is_directory=True)
for f in ['gradlew','dev']:(dest/f).chmod(0o755)
print('Restored baseline sources:',dest)
print('Use JDK 21; run bash scripts/setup.sh (if no reused checkout), then ./dev build installDist.')
