/*
 * Copyright 2024 Creek Contributors (https://github.com/creek-service)
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

plugins {
    java
    jacoco
    `creek-common-convention` apply false
    `creek-module-convention` apply false
    `creek-coverage-convention`
    `creek-publishing-convention` apply false
    `creek-sonatype-publishing-convention`
    id("pl.allegro.tech.build.axion-release") version "1.18.18" // https://plugins.gradle.org/plugin/pl.allegro.tech.build.axion-release
}

project.version = scmVersion.version

allprojects {
    tasks.jar {
        onlyIf { sourceSets.main.get().allSource.files.isNotEmpty() }
    }
}

subprojects {
    project.version = project.parent?.version!!

    apply(plugin = "creek-common-convention")
    apply(plugin = "creek-module-convention")

    if (name.startsWith("test-")) {
        tasks.javadoc { onlyIf { false } }
    } else {
        apply(plugin = "creek-publishing-convention")
        apply(plugin = "jacoco")
    }

    extra.apply {
        set("creekVersion", "0.4.2-SNAPSHOT")
        set("spotBugsVersion", "4.9.3")         // https://mvnrepository.com/artifact/com.github.spotbugs/spotbugs-annotations
        set("picoCliVersion", "4.7.7")          // https://mvnrepository.com/artifact/info.picocli/picocli
        set("jacksonVersion", "2.19.0")         // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-annotations
        set("jsonSchemaVersion", "1.0.39")      // https://mvnrepository.com/artifact/com.kjetland/mbknor-jackson-jsonschema
        set("classGraphVersion", "4.8.179")     // https://mvnrepository.com/artifact/io.github.classgraph/classgraph
        set("kotlinVersion", "2.1.20")          // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib-common
        set("scalaVersion", "2.13.16")

        set("log4jVersion", "2.24.3")           // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core
        set("guavaVersion", "33.4.0-jre")         // https://mvnrepository.com/artifact/com.google.guava/guava
        set("junitVersion", "5.12.2")            // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
        set("junitPioneerVersion", "2.3.0")     // https://mvnrepository.com/artifact/org.junit-pioneer/junit-pioneer
        set("mockitoVersion", "5.18.0")         // https://mvnrepository.com/artifact/org.mockito/mockito-junit-jupiter
    }

    val creekVersion : String by extra
    val guavaVersion : String by extra
    val log4jVersion : String by extra
    val junitVersion: String by extra
    val junitPioneerVersion: String by extra
    val mockitoVersion: String by extra

    dependencies {
        testImplementation("org.creekservice:creek-test-hamcrest:$creekVersion")
        testImplementation("org.creekservice:creek-test-util:$creekVersion")
        testImplementation("org.creekservice:creek-test-conformity:$creekVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
        testImplementation("org.junit-pioneer:junit-pioneer:$junitPioneerVersion")
        testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
        testImplementation("com.google.guava:guava-testlib:$guavaVersion")
        testRuntimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    }
}

defaultTasks("format", "static", "check")
