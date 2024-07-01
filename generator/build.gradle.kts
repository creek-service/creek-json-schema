/*
 * Copyright 2022-2024 Creek Contributors (https://github.com/creek-service)
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
    `java-library`
    application
}

repositories {
    // For net.jimblackler.jsonschemafriend:core:
    maven { url = uri("https://jitpack.io")  }
}

val creekVersion : String by extra
val picoCliVersion : String by extra
val spotBugsVersion : String by extra
val log4jVersion : String by extra
val jacksonVersion : String by extra
val jsonSchemaVersion : String by extra
val classGraphVersion : String by extra
val scalaVersion : String by extra
val kotlinVersion : String by extra

dependencies {
    implementation("org.creekservice:creek-base-annotation:$creekVersion")
    implementation("org.creekservice:creek-base-type:$creekVersion")
    implementation("org.creekservice:creek-base-schema:$creekVersion")

    implementation("com.github.spotbugs:spotbugs-annotations:$spotBugsVersion")
    implementation("info.picocli:picocli:$picoCliVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("com.kjetland:mbknor-jackson-jsonschema_2.13:$jsonSchemaVersion")
    implementation("io.github.classgraph:classgraph:$classGraphVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion") {
        because("An old v1.x SLF4J impl is required by mbknor-jackson-jsonschema" +
                "until https://github.com/mbknor/mbknor-jackson-jsonSchema/pull/172 is resolved:")
    }
    implementation("org.slf4j:slf4j-api:2.0.13")

    // The following are set to bring in dependency versions beyond known security vulnerabilities:
    // The following can be removed once https://github.com/mbknor/mbknor-jackson-jsonSchema/issues/174 is resolved:
    // Also see: https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-36944
    implementation("org.scala-lang:scala-library:$scalaVersion")
    // The following can be removed once https://github.com/mbknor/mbknor-jackson-jsonSchema/issues/178 is resolved:
    // Also see: https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-24329
    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:$kotlinVersion")

    testImplementation(project(":test-types"))
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    testImplementation("net.jimblackler.jsonschemafriend:core:0.12.3")
}

application {
    mainModule.set("creek.json.schema.generator")
    mainClass.set("org.creekservice.api.json.schema.generator.JsonSchemaGenerator")
}

tasks.test {
    dependsOn("installDist")
    dependsOn(":test-types:jar")
}