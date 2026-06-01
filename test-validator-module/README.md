# test-validator-module

Verifies that the `validator` module's `module-info.java` declares all required dependencies by
running a check program on an isolated module path.

## Why this exists

Gradle's [module plugin][1] places **all** resolved dependencies on `--module-path` when running
tests. This means that dependencies declared as `requires static` (optional) in upstream libraries
(e.g. `jackson-dataformat-yaml` in networknt) are silently resolved during tests, even if the
validator's own `module-info.java` does not explicitly require them.

Unit tests therefore pass, but a real downstream consumer whose build does not include the optional
dependency will fail at runtime.

## How it works

A `JavaExec` task (`checkValidatorModulePath`) runs a small `main()` program using **only** the
validator's `runtimeClasspath` — no test dependencies. This mirrors what a downstream consumer sees.
If the validator's `module-info.java` is missing a `requires` directive (or the corresponding Gradle
dependency), the check fails with a `NoClassDefFoundError`.

The task is wired into the `check` lifecycle, so `./gradlew check` catches regressions automatically.

[1]: https://github.com/java9-modularity/gradle-modules-plugin
