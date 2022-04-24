<!-- ChangeMe: replace /multi-module-template in the badge urls below with the name of the repo-->
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Coverage Status](https://coveralls.io/repos/github/creek-service/multi-module-template/badge.svg?branch=main)](https://coveralls.io/github/creek-service/multi-module-template?branch=main)
[![build](https://github.com/creek-service/multi-module-template/actions/workflows/gradle.yml/badge.svg)](https://github.com/creek-service/multi-module-template/actions/workflows/gradle.yml)
[![CodeQL](https://github.com/creek-service/multi-module-template/actions/workflows/codeql.yml/badge.svg)](https://github.com/creek-service/multi-module-template/actions/workflows/codeql.yml)

# Multi-module template Repo
Template repo used to create other multi-module repos.

## Features

The template sets up the following:

* Multi-module Gradle Java project, including:
  * Code formatting by [Spotless][1]
  * Static code analysis by [Spotbugs][2] and [Checkstyle][3]
  * Release versioning by the [Axion-release-plugin][4]
  * Code coverage analysis by [Jacoco][5]
  * Code coverage tracking by [Coveralls.io][6]
  * Default set of test dependencies:
    * [Unit5][7]
    * [Mockito][8]
    * [Hamcrest][9]
    * [Guava TestLib][10]
    * [Log4J 2.x][11]
* GitHub build workflow, including:
  * Gradle build
  * [Coveralls.io][6] reporting
  * Release versioning
* GitHub code owners and PR template.

## Usage

### Creating a new repo from the template

1. Click the "Use this template" button on the main page and follow the instructions.
2. Import the new repo into [Coveralls.io][12], noting the repo token.
3. Customise the repo in GitHub `Settings`->:
   1. `General`->
      1. `Pull Requests`: 
         1. un-tick: `Allow merge commits` and `Allow rebase merging`.
         2. tick: `Always suggest updating pull request branches`, `Allow auto-merging` and `Automatically delete head branches`
   2. `Collaborators and teams`->
       1. `Manage access`: add `code-reviews` team with the `Write` role.
   3. `Secrets`->:
      1. Add a new repository secret called `COVERALLS_REPO_TOKEN`, grabbing the value from Coveralls.io.,
4. Customise the files in the new repo:
    1. Replace the `multi-module-template` repo name with the name of the new project.
       Each place is marked with a `ChangeMe` comment.
    2. Replace the [`example`](example) module with the repos first module.
    3. Replace the `creek.template.module.multi` module name with a suitable module name.
       Each place is marked with a `ChangeMe` comment.
    4. Replace this README.md
    5. Commit changes as a PR (so you can test the PR build works!)
5. Finish customising the repo in GitHub `Settings`->`Branches` and protect the `main` branch:
    1. Tick `Require a pull request before merging`
       1. With `Require approvals` set to 1.
    2. Tick `Dismiss stale pull request approvals when new commits are pushed`
    3. Tick `Require status checks to pass before merging`
       1. With `Require branches to be up to date before merging`
       2. With status checks:
          * `build`
          * `codeQL`
          * `coverage/coveralls`
    4. Click `Create`.
6. Finish customising the repo in Coveralls.io `Settings`->`Pull Request Alerts`:
   1. Tick `Leave comments`
   2. Set `COVERAGE THRESHOLD FOR FAILURE` to `80`%
   3. Set `COVERAGE DECREASE THRESHOLD FOR FAILURE` to `1`%
   4. Save changes.

### Gradle commands

* `./gradlew format` will format the code using [Spotless][1].
* `./gradlew static` will run static code analysis, i.e. [Spotbugs][2] and [Checkstyle][3].
* `./gradlew check` will run all checks and tests.
* `./gradlew coverage` will generate a cross-module [Jacoco][5] coverage report.

[1]: https://github.com/diffplug/spotless
[2]: https://spotbugs.github.io/
[3]: https://checkstyle.sourceforge.io/
[4]: https://github.com/allegro/axion-release-plugin
[5]: https://www.jacoco.org/jacoco/trunk/doc/
[6]: https://coveralls.io/
[7]: https://junit.org/junit5/docs/current/user-guide/
[8]: https://site.mockito.org/
[9]: http://hamcrest.org/JavaHamcrest/index
[10]: https://github.com/google/guava/tree/master/guava-testlib
[11]: https://logging.apache.org/log4j/2.x/
[12]: https://coveralls.io/
