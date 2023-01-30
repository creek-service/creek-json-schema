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

plugins {
    `java-library`
    application
}

val creekVersion : String by extra
val picoCliVersion : String by extra
val spotBugsVersion : String by extra
val log4jVersion : String by extra
val jacksonVersion : String by extra
val jsonSchemaVersion : String by extra
val classGraphVersion : String by extra

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
    // An old v1.x SLF4J impl as required by mbknor-jackson-jsonschema
    // Can be updated once https://github.com/mbknor/mbknor-jackson-jsonSchema/pull/172 is resolved:
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")

    testImplementation(project(":test-types"))
}

application {
    mainModule.set("creek.json.schema.generator")
    mainClass.set("org.creekservice.api.json.schema.generator.JsonSchemaGenerator")
}

tasks.test {
    dependsOn("installDist")
    dependsOn(":test-types:jar")
}