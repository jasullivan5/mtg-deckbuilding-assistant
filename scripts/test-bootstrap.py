#!/usr/bin/env python3
"""Local Git setup guards, not mocked Argentum integration."""
import pathlib,tempfile,subprocess,shutil,unittest,os
ROOT=pathlib.Path(__file__).resolve().parent.parent
class BootstrapGuards(unittest.TestCase):
 def setUp(self):
  self.t=tempfile.TemporaryDirectory();self.root=pathlib.Path(self.t.name);(self.root/'scripts').mkdir();shutil.copy2(ROOT/'scripts/setup.sh',self.root/'scripts/setup.sh');self.repo=self.root/'argentum';self.repo.mkdir();self.g('init','-q');self.g('remote','add','origin','https://github.com/jasullivan5/argentum-engine.git');self.g('config','user.name','Fixture');self.g('config','user.email','fixture@example.invalid');self.g('config','commit.gpgsign','false');(self.repo/'source').write_text('one');self.g('add','.');self.g('commit','-qm','one');self.pin=self.g('rev-parse','HEAD').strip();(self.root/'ARGENTUM_COMMIT').write_text(self.pin)
 def tearDown(self):self.t.cleanup()
 def g(self,*args):return subprocess.check_output(['git','-C',str(self.repo),*args],text=True,stderr=subprocess.DEVNULL)
 def setup(self):return subprocess.run([os.environ.get('MTG_TEST_BASH','bash'),str(self.root/'scripts/setup.sh')],capture_output=True,text=True)
 def test_reuses_exact_checkout_without_fetch(self):self.assertEqual(self.setup().returncode,0);self.assertEqual(self.setup().returncode,0)
 def test_rejects_wrong_revision_without_switching(self):
  (self.repo/'source').write_text('two');self.g('commit','-qam','two');head=self.g('rev-parse','HEAD');self.assertNotEqual(self.setup().returncode,0);self.assertEqual(self.g('rev-parse','HEAD'),head)
 def test_preserves_dirty_work(self):
  (self.repo/'source').write_text('user changes');self.assertNotEqual(self.setup().returncode,0);self.assertEqual((self.repo/'source').read_text(),'user changes')
 def test_rejects_non_git_directory(self):
  source=self.repo/'.git';target=self.root/'detached-git';assert source.resolve().is_relative_to(self.root.resolve()) and target.resolve().is_relative_to(self.root.resolve());source.rename(target);self.assertNotEqual(self.setup().returncode,0)
if __name__=='__main__':unittest.main()
