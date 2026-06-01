/*
 * Copyright 2026 Creek Contributors (https://github.com/creek-service)
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
}

dependencies {
    implementation(project(":generator"))
}

// Task runs the check program with ONLY main runtimeClasspath.
// No test deps = no extra jars masking missing requires.
tasks.register<JavaExec>("checkGeneratorModulePath") {
    group = "verification"
    description = "Verify generator module-info is complete by running on isolated module path"

    mainModule.set("creek.json.schema.generator.testmodule")
    mainClass.set("org.creekservice.test.generator.modulepath.GeneratorModuleCheck")
    modularity.inferModulePath.set(true)
    classpath = sourceSets.main.get().runtimeClasspath

    // Fail build if check fails
    isIgnoreExitValue = false
}

tasks.named("check") {
    dependsOn("checkGeneratorModulePath")
}
