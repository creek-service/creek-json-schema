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
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    `java-library`
}

val creekVersion : String by extra
val jacksonVersion : String by extra
val jsonSchemaVersion : String by extra
val kotlinVersion : String by extra
val scalaVersion : String by extra

dependencies {
    implementation("org.creekservice:creek-base-annotation:$creekVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.kjetland:mbknor-jackson-jsonschema_2.13:$jsonSchemaVersion")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

    // The following are set to bring in dependency versions beyond known security vulnerabilities:
    // The following can be removed once https://github.com/mbknor/mbknor-jackson-jsonSchema/issues/174 is resolved:
    // Also see: https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-36944
    implementation("org.scala-lang:scala-library:$scalaVersion")
    // The following can be removed once https://github.com/mbknor/mbknor-jackson-jsonSchema/issues/178 is resolved:
    // Also see: https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-24329
    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:$kotlinVersion")
}
