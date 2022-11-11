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
 * Standard coverage configuration of Creek projects, utilising Jacoco and Coveralls.io
 *
 * <p>Version: 1.1
 *
 * <p>Apply to root project only
 */

plugins {
    java
    jacoco
    id("com.github.kt3k.coveralls")
}

repositories {
    mavenCentral()
}

allprojects {
    apply(plugin = "java")

    tasks.withType<JacocoReport>().configureEach{
        dependsOn(tasks.test)
    }
}

val coverage = tasks.register<JacocoReport>("coverage") {
    group = "creek"
    description = "Generates an aggregate code coverage report"

    val coverageReportTask = this

    allprojects {
        val proj = this
        // Roll results of each (test) task that has Jacoco extension, i.e. plugin applied, into the main coverage task
        proj.tasks.matching { it.extensions.findByType<JacocoTaskExtension>() != null && it != coverageReportTask }.forEach {
            coverageReportTask.sourceSets(proj.sourceSets.main.get())
            coverageReportTask.executionData(files(proj.tasks.withType<Test>()).filter { it.exists() && it.name.endsWith(".exec") })
            coverageReportTask.dependsOn(it)
        }
    }

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

coveralls {
    sourceDirs = allprojects.flatMap{it.sourceSets.main.get().allSource.srcDirs}.map{it.toString()}
    jacocoReportPath = "$buildDir/reports/jacoco/coverage/coverage.xml"
}

tasks.coveralls {
    group = "creek"
    description = "Uploads the aggregated coverage report to Coveralls"

    dependsOn(coverage)
    onlyIf{System.getenv("CI") != null}
}