# Development environment

The application uses the sibling maintained fork on `app-stable`. See [engine workflow](ENGINE_WORKFLOW.md) for setup, explicit candidate isolation, provenance, CI and historical evidence.

Required: Git, JDK 21 including javac, Bash and Python 3. Set JAVA_HOME and its bin on PATH. Network access is needed for an initial clone and Gradle dependencies; existing clean stable checkouts can be reused offline. Never disable TLS or commit credentials.

Use `dev` / `dev.ps1`, not the engine root test task. The wrapper attaches the external application module to the sibling engine's Gradle settings and validates its origin, branch and clean state. It never silently resets, cleans or switches active work. `setup.ps1 -Update` / `setup.sh --update` explicitly fetch and fast-forward app-stable.

Ordinary application work uses feature branches from main. Keep the normal engine checkout clean on app-stable; isolate engine feature work when the application needs that checkout. Build caches, full generated reports and the retired ignored nested clone remain outside committed sources. Reproduce historical experiments using their recorded revisions and receipts, not the current branch name as a substitute for provenance.
