/*
 * Copyright 2022 Creek Contributors (https://github.com/creek-service)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Standard configuration of Creek projects
 *
 * <p>Version: 1.2
 *
 * <p>Apply to all java modules, usually excluding the root project in multi-module sets.
 */

plugins {
    java
    checkstyle
    id("com.github.spotbugs")
    id("com.diffplug.spotless")
}

group = "org.creekservice"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()

    // Primary snapshot repo:
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        mavenContent {
            includeGroup("org.creekservice")
            snapshotsOnly()
        }
    }

    // Backup snapshot repo:
    maven {
        url = uri("https://maven.pkg.github.com/creek-service/*")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
        mavenContent {
            includeGroup("org.creekservice")
            snapshotsOnly()
        }
    }

    mavenCentral()
}

configurations.all {
    // Reduce chance of build servers running into compilation issues due to stale snapshots:
    resolutionStrategy.cacheChangingModulesFor(15, TimeUnit.MINUTES)
}

tasks.compileJava {
    options.compilerArgs.add("-Xlint:all,-serial,-requires-automatic,-requires-transitive-automatic,-module")
    options.compilerArgs.add("-Werror")
}

tasks.test {
    useJUnitPlatform()
    setForkEvery(1)
    maxParallelForks = 4
    testLogging {
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showCauses = true
        showExceptions = true
        showStackTraces = true
    }
}

spotless {
    java {
        googleJavaFormat("1.15.0").aosp()
        indentWithSpaces()
        importOrder()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        toggleOffOn("formatting:off", "formatting:on")
    }
}

spotbugs {
    tasks.spotbugsMain {
        reports.create("html") {
            required.set(true)
            setStylesheet("fancy-hist.xsl")
        }
    }
    tasks.spotbugsTest {
        reports.create("html") {
            required.set(true)
            setStylesheet("fancy-hist.xsl")
        }
    }
}

if (rootProject.name != project.name) {
    tasks.jar {
        archiveBaseName.set("${rootProject.name}-${project.name}")
    }
}

tasks.register("format") {
    group = "creek"
    description = "Format the code"

    dependsOn("spotlessCheck", "spotlessApply")
}

tasks.register("static") {
    group = "creek"
    description = "Run static code analysis"

    dependsOn("checkstyleMain", "checkstyleTest", "spotbugsMain", "spotbugsTest")
}

