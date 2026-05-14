/*
 * Copyright 2022-2026 Creek Contributors (https://github.com/creek-service)
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
val victoolsVersion : String by extra
val swaggerAnnotationsVersion : String by extra
val classGraphVersion : String by extra

dependencies {
    implementation("org.creekservice:creek-base-annotation:$creekVersion")
    implementation("org.creekservice:creek-base-type:$creekVersion")
    implementation("org.creekservice:creek-base-schema:$creekVersion")

    implementation("com.github.spotbugs:spotbugs-annotations:$spotBugsVersion")
    implementation("info.picocli:picocli:$picoCliVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("com.github.victools:jsonschema-generator:$victoolsVersion")
    implementation("com.github.victools:jsonschema-module-jackson:$victoolsVersion")
    implementation("com.github.victools:jsonschema-module-swagger-2:$victoolsVersion")
    implementation("io.swagger.core.v3:swagger-annotations:$swaggerAnnotationsVersion")
    implementation("io.github.classgraph:classgraph:$classGraphVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")
    implementation("org.slf4j:slf4j-api:2.0.17")

    testImplementation(project(":test-types"))
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    testImplementation(project(":validator"))
}

application {
    mainModule.set("creek.json.schema.generator")
    mainClass.set("org.creekservice.api.json.schema.generator.JsonSchemaGenerator")
}

tasks.test {
    dependsOn("installDist")
    dependsOn(":test-types:jar")

    doFirst {
        val generatorLibJars = file("build/install/generator/lib")
            .listFiles()?.map { it.name }?.toSet() ?: emptySet()

        // generator's test dependencies includes test-types production dependencies:
        val testTypeDeps = configurations.getByName("testRuntimeClasspath")
            .resolvedConfiguration.resolvedArtifacts
            .filter { it.file.name !in generatorLibJars }
            .filter { !it.file.absolutePath.contains("/test-types/") }
            .joinToString(File.pathSeparator) { it.file.absolutePath }

        if (testTypeDeps.isNotEmpty()) {
            systemProperty("test.types.dependency.jars", testTypeDeps)
        }
    }
}