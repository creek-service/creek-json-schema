// Standard coverage configuration of Creek projects, utilising Jacoco and Coveralls.io
// Apply to root project only

plugins {
    java
    jacoco
    id("com.github.kt3k.coveralls")
}

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "java")

    tasks.withType<JacocoReport>().configureEach{
        dependsOn(tasks.test)
    }
}

val coverage = tasks.register<JacocoReport>("coverage") {
    group = "creek"
    description = "Generates an aggregate code coverage report from all subprojects"

    val coverageReportTask = this

    // If a subproject applies the 'jacoco' plugin, add the result it to the report
    subprojects {
        val subproject = this
        subproject.plugins.withType<JacocoPlugin>().configureEach {
            subproject.tasks.matching { it.extensions.findByType<JacocoTaskExtension>() != null }.configureEach {
                sourceSets(subproject.sourceSets.main.get())
                executionData(files(subproject.tasks.withType<Test>()).filter { it.exists() && it.name.endsWith(".exec") })
            }

            subproject.tasks.matching { it.extensions.findByType<JacocoTaskExtension>() != null }.forEach {
                coverageReportTask.dependsOn(it)
            }
        }
    }

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

coveralls {
    sourceDirs = subprojects.flatMap{it.sourceSets.main.get().allSource.srcDirs}.map{it.toString()}
    jacocoReportPath = "$buildDir/reports/jacoco/coverage/coverage.xml"
}

tasks.coveralls {
    group = "creek"
    description = "Uploads the aggregated coverage report to Coveralls"

    dependsOn(coverage)
    onlyIf{System.getenv("CI") != null}
}