#!/usr/bin/env python3
"""Real local Git setup guards for Bash and PowerShell; no GitHub connection."""
import os, pathlib, shutil, subprocess, tempfile, unittest
ROOT = pathlib.Path(__file__).resolve().parent.parent
FORK = "https://github.com/jasullivan5/argentum-engine.git"

class BootstrapGuards(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix="mtg sibling & spaces ")
        self.base = pathlib.Path(self.tmp.name)
        self.app = self.base / "application"
        (self.app / "scripts").mkdir(parents=True)
        for name in ("setup.sh", "setup.ps1"):
            shutil.copy2(ROOT / "scripts" / name, self.app / "scripts" / name)
        self.repo = self.base / "argentum-engine"
        self.repo.mkdir()
        self.env = os.environ.copy()
        for key in ("ARGENTUM_ENGINE_DIR", "ARGENTUM_CANDIDATE_REVISION"):
            self.env.pop(key, None)
        self.env.update(GIT_CONFIG_GLOBAL=str(self.base / "gitconfig"), GIT_CONFIG_NOSYSTEM="1",
                        GIT_ALLOW_PROTOCOL="file")
        self.g("init", "-q", "-b", "app-stable")
        self.g("config", "user.name", "Fixture")
        self.g("config", "user.email", "fixture@example.invalid")
        self.g("config", "commit.gpgsign", "false")
        (self.repo / "source").write_text("one")
        self.g("add", ".")
        self.g("commit", "-qm", "one")
        self.g("remote", "add", "origin", FORK)
        self.initial = self.g("rev-parse", "HEAD").strip()
        self.shell = os.environ.get("MTG_TEST_SHELL", "bash")
    def tearDown(self):
        self.tmp.cleanup()
    def g(self, *args):
        return subprocess.check_output(["git", "-C", str(self.repo), *args],
                                       env=self.env, text=True, stderr=subprocess.DEVNULL)
    def setup(self, update=False):
        if self.shell == "powershell":
            argv = [os.environ.get("MTG_TEST_POWERSHELL", "pwsh"), "-NoProfile", "-File",
                    str(self.app / "scripts/setup.ps1")]
            if update: argv.append("-Update")
        else:
            argv = [os.environ.get("MTG_TEST_BASH", "bash"), str(self.app / "scripts/setup.sh")]
            if update: argv.append("--update")
        return subprocess.run(argv, env=self.env, text=True, capture_output=True, timeout=30)
    def succeeds(self, update=False):
        r = self.setup(update)
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
    def rejects(self):
        r = self.setup()
        self.assertNotEqual(r.returncode, 0, r.stdout + r.stderr)
    def test_reuses_clean_sibling_without_fetch(self):
        self.succeeds()
        self.succeeds()
    def test_new_app_stable_commit_needs_no_pin_approval(self):
        (self.repo / "source").write_text("compatible change")
        self.g("commit", "-qam", "next")
        self.succeeds()
        self.assertNotEqual(self.g("rev-parse", "HEAD").strip(), self.initial)
    def test_preserves_dirty_work(self):
        (self.repo / "source").write_text("user changes")
        self.rejects()
        self.assertEqual((self.repo / "source").read_text(), "user changes")
    def test_preserves_untracked_work(self):
        (self.repo / "untracked").write_text("retain")
        self.rejects()
        self.assertTrue((self.repo / "untracked").exists())
    def test_rejects_feature_branch_without_switching(self):
        self.g("switch", "-qc", "feature/active")
        self.rejects()
        self.assertEqual(self.g("branch", "--show-current").strip(), "feature/active")
    def test_rejects_detached_normal_checkout(self):
        self.g("switch", "--detach", "-q")
        self.rejects()
        self.assertEqual(self.g("rev-parse", "HEAD").strip(), self.initial)
    def test_rejects_unrelated_origin_without_rewriting(self):
        self.g("remote", "set-url", "origin", "https://example.invalid/engine")
        self.rejects()
        self.assertEqual(self.g("remote", "get-url", "origin").strip(), "https://example.invalid/engine")
    def test_explicit_candidate_requires_exact_clean_revision(self):
        self.g("switch", "--detach", "-q")
        self.env["ARGENTUM_CANDIDATE_REVISION"] = self.initial
        self.succeeds()
        self.env["ARGENTUM_CANDIDATE_REVISION"] = "a" * 40
        self.rejects()
    def test_nested_checkout_is_retired_without_deleting(self):
        nested = self.app / "argentum"
        nested.mkdir()
        self.env["ARGENTUM_ENGINE_DIR"] = str(nested)
        self.rejects()
        self.assertTrue(nested.is_dir())
    def test_non_git_sibling_rejected(self):
        (self.repo / ".git").rename(self.base / "retained-git")
        self.rejects()
        self.assertTrue((self.base / "retained-git").exists())
    def remote_with_new_commit(self):
        seed = self.base / "update-origin.git"
        author = self.base / "remote-author"
        subprocess.run(["git", "clone", "-q", "--bare", str(self.repo), str(seed)], env=self.env, check=True)
        subprocess.run(["git", "clone", "-q", str(seed), str(author)], env=self.env, check=True)
        (author / "source").write_text("new stable source")
        for args in (["add", "."], ["-c","user.name=Fixture","-c","user.email=fixture@example.invalid",
                                  "-c","commit.gpgsign=false","commit","-qm","remote stable change"],
                     ["push","-q","origin","app-stable"]):
            subprocess.run(["git","-C",str(author),*args],env=self.env,check=True)
        subprocess.run(["git","config","--global",f"url.{seed.as_uri()}.insteadOf",FORK],env=self.env,check=True)
        return subprocess.check_output(["git","-C",str(author),"rev-parse","HEAD"],env=self.env,text=True).strip()
    def test_update_fast_forwards_explicitly(self):
        new = self.remote_with_new_commit()
        self.succeeds(update=True)
        self.assertEqual(self.g("rev-parse","HEAD").strip(),new)
        self.assertEqual((self.repo/"source").read_text(),"new stable source")
    def test_divergent_update_preserves_local_commit(self):
        self.remote_with_new_commit()
        (self.repo/"source").write_text("local stable work")
        self.g("commit","-qam","local")
        before=self.g("rev-parse","HEAD")
        result=self.setup(update=True)
        self.assertNotEqual(result.returncode,0,result.stdout+result.stderr)
        self.assertEqual(self.g("rev-parse","HEAD"),before)
        self.assertEqual((self.repo/"source").read_text(),"local stable work")
    def test_explicit_worktree_accepts_git_file(self):
        worktree=self.base/"candidate worktree"
        self.g("worktree","add","-q","--detach",str(worktree),self.initial)
        self.env["ARGENTUM_ENGINE_DIR"]=str(worktree)
        self.env["ARGENTUM_CANDIDATE_REVISION"]=self.initial
        self.succeeds()
    def test_clone_app_stable_when_sibling_missing(self):
        seed = self.base / "seed.git"
        subprocess.run(["git", "clone", "-q", "--bare", str(self.repo), str(seed)], env=self.env, check=True)
        moved = self.base / "retained-source"
        self.repo.rename(moved)
        subprocess.run(["git", "config", "--global", f"url.{seed.as_uri()}.insteadOf", FORK],
                       env=self.env, check=True)
        self.succeeds()
        self.assertEqual(self.g("branch", "--show-current").strip(), "app-stable")
        self.assertTrue(moved.exists())

if __name__ == "__main__":
    unittest.main()
