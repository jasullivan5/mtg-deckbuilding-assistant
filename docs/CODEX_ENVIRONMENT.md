# Development environment

The application uses the sibling maintained fork on `app-stable`. See [engine workflow](ENGINE_WORKFLOW.md) for setup, explicit candidate isolation, provenance, CI and historical evidence.

Use `bash scripts/bootstrap.sh` for setup. `scripts/codex-setup.sh` remains a compatibility entry point for external environment configurations and forwards all arguments to bootstrap; it has no internal repository caller. External setup configuration must be checked before removing that alias.

Required: Git, JDK 25 including javac, Bash and Python 3. Set JAVA_HOME and its bin on PATH. Network access is needed for an initial clone and Gradle dependencies; existing clean stable checkouts can be reused offline. Never disable TLS or commit credentials.

Use `dev` / `dev.ps1`, not the engine root test task. The wrapper attaches the external application module to the sibling engine's Gradle settings and validates its origin, branch and clean state. It never silently resets, cleans or switches active work. `setup.ps1 -Update` / `setup.sh --update` explicitly fetch and fast-forward app-stable.

Ordinary application work uses feature branches from main. Keep the normal engine checkout clean on app-stable; isolate engine feature work when the application needs that checkout. Build caches and full generated reports remain outside committed sources. The retired nested engine checkout has been removed; use only the sibling engine. Reproduce historical experiments using their recorded revisions and receipts, not the current branch name as a substitute for provenance.

On Windows, use Git Bash for Bash verification. If another bash is first on PATH, set `MTG_TEST_BASH` to the Git Bash executable when running scripts/test-bootstrap.py. The ordinary Explorer launch uses PowerShell and does not require a separate Bash process.

JDK 25 is the sole current baseline. Older reports retain their actual JDK 21 observations; they are historical replay instructions, not a second supported toolchain. Gradle uses the engine's JDK 25 daemon criteria and Kotlin toolchain. Check both `java --version` and `javac --version`, then both wrappers' `--version` output; accepting Java on PATH alone does not establish which compiler or daemon ran.

Use an existing JDK 25 installation or the upstream Gradle/Foojay provisioning mechanism. On the verified Windows host, an existing Temurin 25 installation was available under `%USERPROFILE%/.gradle/jdks/`; JAVA_HOME and PATH were set only for the task's processes. No machine-global configuration or additional installer is required.
