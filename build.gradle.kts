plugins {
    java
    jacoco
    `maven-publish`
    id("com.github.spotbugs") version "5.0.9"                           // https://plugins.gradle.org/plugin/com.github.spotbugs
    id("com.diffplug.spotless") version "6.10.0"                         // https://plugins.gradle.org/plugin/com.diffplug.spotless
    id("pl.allegro.tech.build.axion-release") version "1.14.0"          // https://plugins.gradle.org/plugin/pl.allegro.tech.build.axion-release
    id("com.github.kt3k.coveralls") version "2.12.0"                    // https://plugins.gradle.org/plugin/com.github.kt3k.coveralls
    id("org.javamodularity.moduleplugin") version "1.8.12" apply false  // https://plugins.gradle.org/plugin/org.javamodularity.moduleplugin
}

project.version = scmVersion.version

allprojects {
    apply(plugin = "idea")
    apply(plugin = "java")
    apply(plugin = "checkstyle")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "com.github.spotbugs")

    group = "org.creekservice"

    java {
        withSourcesJar()

        modularity.inferModulePath.set(false)
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    repositories {
        mavenCentral()

        maven {
            url = uri("https://maven.pkg.github.com/creek-service/*")
            credentials {
                username = "Creek-Bot-Token"
                password = "\u0067hp_LtyvXrQZen3WlKenUhv21Mg6NG38jn0AO2YH"
            }
        }
    }
}

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "org.javamodularity.moduleplugin")

    if (!name.startsWith("test-")) {
        apply(plugin = "jacoco")
    }

    project.version = project.parent?.version!!

    extra.apply {
        set("creekBaseVersion", "0.2.0-SNAPSHOT")
        set("creekTestVersion", "0.2.0-SNAPSHOT")
        set("spotBugsVersion", "4.6.0")         // https://mvnrepository.com/artifact/com.github.spotbugs/spotbugs-annotations
        set("picoCliVersion", "4.6.3")          // https://mvnrepository.com/artifact/info.picocli/picocli
        set("jacksonVersion", "2.13.3")         // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-annotations
        set("jsonSchemaVersion", "1.0.39")      // https://mvnrepository.com/artifact/com.kjetland/mbknor-jackson-jsonschema
        set("classGraphVersion", "4.8.149")     // https://mvnrepository.com/artifact/io.github.classgraph/classgraph

        set("log4jVersion", "2.18.0")           // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core
        set("guavaVersion", "31.1-jre")         // https://mvnrepository.com/artifact/com.google.guava/guava
        set("junitVersion", "5.9.0")            // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
        set("junitPioneerVersion", "1.7.1")     // https://mvnrepository.com/artifact/org.junit-pioneer/junit-pioneer
        set("mockitoVersion", "4.7.0")          // https://mvnrepository.com/artifact/org.mockito/mockito-junit-jupiter
        set("hamcrestVersion", "2.2")           // https://mvnrepository.com/artifact/org.hamcrest/hamcrest-core
    }

    val creekTestVersion : String by extra
    val guavaVersion : String by extra
    val log4jVersion : String by extra
    val junitVersion: String by extra
    val junitPioneerVersion: String by extra
    val mockitoVersion: String by extra
    val hamcrestVersion : String by extra

    dependencies {
        testImplementation("org.creekservice:creek-test-hamcrest:$creekTestVersion")
        testImplementation("org.creekservice:creek-test-util:$creekTestVersion")
        testImplementation("org.creekservice:creek-test-conformity:$creekTestVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
        testImplementation("org.junit-pioneer:junit-pioneer:$junitPioneerVersion")
        testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
        testImplementation("org.hamcrest:hamcrest-core:$hamcrestVersion")
        testImplementation("com.google.guava:guava-testlib:$guavaVersion")
        testImplementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
        testImplementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    }

    tasks.compileJava {
        options.compilerArgs.add("-Xlint:all,-serial,-requires-automatic,-requires-transitive-automatic")
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
            googleJavaFormat("1.12.0").aosp()
            indentWithSpaces()
            importOrder()
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
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

    tasks.withType<JacocoReport>().configureEach{
        dependsOn(tasks.test)
    }

    tasks.jar {
        archiveBaseName.set("${rootProject.name}-${project.name}")
    }

    tasks.register("format") {
        dependsOn("spotlessCheck", "spotlessApply")
    }

    tasks.register("static") {
        dependsOn("checkstyleMain", "checkstyleTest", "spotbugsMain", "spotbugsTest")
    }

    if (!project.name.startsWith("test-")) {
        publishing {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/creek-service/${rootProject.name}")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                        password = System.getenv("GITHUB_TOKEN")
                    }
                }
            }
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                    artifactId = "${rootProject.name}-${project.name}"

                    pom {
                        url.set("https://github.com/creek-service/${rootProject.name}.git")
                    }
                }
            }
        }
    }
}

val coverage = tasks.register<JacocoReport>("coverage") {
    group = "coverage"
    description = "Generates an aggregate code coverage report from all subprojects"

    val coverageReportTask = this

    // If a subproject applies the 'jacoco' plugin, add the result it to the report
    subprojects {
        val subproject = this
        subproject.plugins.withType<JacocoPlugin>().configureEach {
            subproject.tasks.matching({ it.extensions.findByType<JacocoTaskExtension>() != null }).configureEach {
                sourceSets(subproject.sourceSets.main.get())
                executionData(files(subproject.tasks.withType<Test>()).filter { it.exists() && it.name.endsWith(".exec") })
            }

            subproject.tasks.matching({ it.extensions.findByType<JacocoTaskExtension>() != null }).forEach {
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
    group = "coverage"
    description = "Uploads the aggregated coverage report to Coveralls"

    dependsOn(coverage)
    onlyIf{System.getenv("CI") != null}
}

defaultTasks("format", "static", "check")
