[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Coverage Status](https://coveralls.io/repos/github/creek-service/creek-json-schema/badge.svg?branch=main)](https://coveralls.io/github/creek-service/creek-json-schema?branch=main)
[![build](https://github.com/creek-service/creek-json-schema/actions/workflows/build.yml/badge.svg)](https://github.com/creek-service/creek-json-schema/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.creekservice/creek-json-schema-generator.svg)](https://central.sonatype.dev/search?q=creek-json-schema-*)
[![CodeQL](https://github.com/creek-service/creek-json-schema/actions/workflows/codeql.yml/badge.svg)](https://github.com/creek-service/creek-json-schema/actions/workflows/codeql.yml)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/creek-service/creek-json-schema/badge)](https://api.securityscorecards.dev/projects/github.com/creek-service/creek-json-schema)
[![OpenSSF Best Practices](https://bestpractices.coreinfrastructure.org/projects/6899/badge)](https://bestpractices.coreinfrastructure.org/projects/6899)

# Creek JSON Schema

The repositor contains classes for generating JSON schemas from JVM classes. 
The generator can be used directly. However, the intended approach is to use a build plugin, 
e.g.  the [Creek JSON schema Gradle plugin](https://github.com/creek-service/creek-json-schema-gradle-plugin).

See [CreekService.org](https://www.creekservice.org) for info on Creek Service.

## Modules in this repo

* **[generator](generator)** [[JavaDocs](https://javadoc.io/doc/org.creekservice/creek-json-schema-generator)]: a command line tool used to generate JSON schemas from code.

