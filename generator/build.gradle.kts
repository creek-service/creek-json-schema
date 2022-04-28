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
val log4jVersion : String by extra

dependencies {
    implementation("org.creek:creek-base-annotation:$creekVersion")
    implementation("org.creek:creek-base-type:$creekVersion")
    implementation("org.creek:creek-base-schema:$creekVersion")

    implementation("info.picocli:picocli:$picoCliVersion")

    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j18-impl:$log4jVersion")
}

application {
    mainModule.set("creek.json.schema.generator")
    mainClass.set("org.creek.api.json.schema.generator.JsonSchemaGenerator")
}

tasks.test {
    dependsOn("installDist")
}