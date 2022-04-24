# How to contribute

Contribution welcome!

## How to prepare

* You will need a [GitHub account](https://github.com/signup/free)
<!-- ChangeMe: replace //multi-module-template/ in the urls below with the name of the repo-->
* Submit an [issue ticket](https://github.com/creek-service/multi-module-template/issues/new) for your issue if the is no one yet.
    * Describe the issue and include steps to reproduce if it's a bug.
    * Ensure to mention the earliest version that you know is affected.
* If you are able and want to fix this, [fork the repository on GitHub](https://docs.github.com/en/get-started/quickstart/fork-a-repo)

## Make Changes

* In your forked repository, create a feature branch for your upcoming patch. (e.g. `feature/issue-22-enhance-four-candles` or `bugfix/issue-42-crash-when-laughing`)
   * Usually this is based on the main branch.
   * Create a branch based on main;
     * `git checkout main`
     * `git checkout -b feature/issue-22-enhance-four-candles`
   * Please avoid working directly on the `main` branch.
* Make sure you stick to the coding style that is used already.
  * The project uses [Spotless](https://github.com/diffplug/spotless).
  * Run `./gradlew format` to format the code.
* Make commits of logical units and describe them properly.
* Submit tests to your patch / new feature, so it can be tested easily.

## Submit Changes

* Before submitting run `./gradlew format check`:
  * Running `./gradlew format` will ensure the code is correctly formatted. 
  * Running `./gradlew static` will run static code analysis.
  * Running `./gradlew test` will run all the tests.
  * Running `./gradlew coverage` will produce a code coverage report in 
    [`<project-roo>/build/reports/jacoco/coverage/html/index.html>`](build/reports/jacoco/coverage/html/index.html)
* Push your changes to a feature branch in your fork of the repository.
* [Open a pull request](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request-from-a-fork) to merge the changes from your branch into the main branch.
* Please reference the Github issue number in your pull request. 
* Ensure all checks pass on the pull request.

