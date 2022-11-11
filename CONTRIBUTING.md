# How to contribute

Contribution welcome!

## How to prepare

* You will need a [GitHub account](https://github.com/signup/free)
* Submit an [issue ticket](https://github.com/creek-service/creek-test/issues/new) for your issue if the is no one yet.
    * Describe the issue and include steps to reproduce if it's a bug.
    * Ensure to mention the earliest version that you know is affected.
* If you are able and want to fix this, [fork the repository on GitHub](https://docs.github.com/en/get-started/quickstart/fork-a-repo)

## Gradle commands

During normal development, run `./gradlew`, which will format your code changes, run static code analysis and then the tests.

* Running `./gradlew format` will ensure the code is correctly formatted.
* Running `./gradlew static` will run static code analysis.
* Running `./gradlew test` will run all the tests.
* Running `./gradlew javadocs` will check for documentation issues.
* Running `./gradlew coverage` will produce a code coverage report in
  [`<project-roo>/build/reports/jacoco/coverage/html/index.html>`](build/reports/jacoco/coverage/html/index.html)
* Running `./gradlew currentVersion` will output the current version, i.e. the version the built jars will use.

## Make Changes

* In your forked repository, create a feature branch for your upcoming patch. (e.g. `feat/22-enhance-four-candles` or `bugfix/42-crash-when-laughing`)
   * Usually this is based on the main branch.
   * Create a branch based on main;
     * `git checkout main`
     * `git checkout -b feat/22-enhance-four-candles`
   * Please avoid working directly on the `main` branch.
* Make sure you stick to the coding style that is used already.
  * The project uses [Spotless](https://github.com/diffplug/spotless).
  * Run `./gradlew format` to format the code.
* Make commits of logical units and describe them properly.
* Submit tests to your patch / new feature, so it can be tested easily.
* Ensure documentation is updated for any change in, or new, functionality. This includes:
  * Javadocs comments in the code itself, paying particular attentions to documenting public apis.
  * Any documentation under `_docs` directory, (this is served up on `https://www.creekservice.org`)

## Submit Changes

* Before submitting run `./gradlew format javadoc check`:
* Push your changes to a feature branch in your fork of the repository.
* [Open a pull request](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request-from-a-fork) to merge the changes from your branch into the main branch.
* Please reference the GitHub issue number in your pull request. For example:
   ```
   Fixes: https://github.com/creek-service/creek-service/issues/103
   ```
* Ensure all checks pass on the pull request.

