# Codex environment

Canonical engine source is our fork, `https://github.com/jasullivan5/argentum-engine.git`. New environments fetch the exact application pin, not a floating app-stable/main tip. app-stable is the engine development base; main is tracking only. Bash and PowerShell setup preserve exact clean-checkout guards. Use a fresh application worktree for a new pin; see ENGINE_COUNTER_CORRECTION.md for actual transition verification and upstream-review policy. The original hosted-environment notes below remain historical; no hosted environment was created here.

No hosted environment was created. After publishing this Git repository to an authorized private destination, select the application repository in Codex Cloud (not the standalone upstream). Request Git, JDK21 including javac, Bash and Python3; set JAVA_HOME/PATH to its real installed JDK. Run `bash scripts/codex-setup.sh` as the install/setup command. Review and test setup before publishing the environment. No application server, start service or API key is needed.

Ordinary maintenance: `bash scripts/setup.sh` validates/reuses the pin; incremental work uses ./dev installDist or focused tests. Full-catalog checks are expensive and do not belong in every startup. Gradle uses its normal user dependency/build cache; GRADLE_USER_HOME may select another cache location. Never commit caches or rely on a temporary filesystem as source control.

Public downloads need GitHub (including redirects/releases), Gradle distribution hosts, Maven Central and Gradle Plugin Portal/artifact hosts. A network proxy may require JVM Gradle proxy/truststore configuration; use platform-supported public CA settings, never disable TLS or commit secrets. Missing JDK, blocked downloads and wrong/dirty checkouts must be diagnosed separately.

The scripts use repository-relative paths and the standard JAVA_HOME/Gradle environment. No former Work directory/JDK/cache is required. PowerShell entry points remain available; their new guards are inspection-only unless HANDOFF_VERIFICATION says otherwise.

Official documentation reviewed during this migration: https://learn.chatgpt.com/docs/environments/cloud-environments . The environment UI can change; repository selection, explicit installation/testing and review before publication are the contract. No undocumented repository configuration schema is assumed.
