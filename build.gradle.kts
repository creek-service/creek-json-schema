plugins {
    java
    jacoco
    `creek-common-convention` apply false
    `creek-module-convention` apply false
    `creek-coverage-convention`
    `creek-publishing-convention` apply false
    `creek-sonatype-publishing-convention`
    id("pl.allegro.tech.build.axion-release") version "1.16.0" // https://plugins.gradle.org/plugin/pl.allegro.tech.build.axion-release
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
        set("spotBugsVersion", "4.8.2")         // https://mvnrepository.com/artifact/com.github.spotbugs/spotbugs-annotations
        set("picoCliVersion", "4.7.5")          // https://mvnrepository.com/artifact/info.picocli/picocli
        set("jacksonVersion", "2.16.0")         // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-annotations
        set("jsonSchemaVersion", "1.0.39")      // https://mvnrepository.com/artifact/com.kjetland/mbknor-jackson-jsonschema
        set("classGraphVersion", "4.8.165")     // https://mvnrepository.com/artifact/io.github.classgraph/classgraph
        set("kotlinVersion", "1.9.21")          // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib-common
        set("scalaVersion", "2.13.12")

        set("log4jVersion", "2.22.0")           // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core
        set("guavaVersion", "32.1.3-jre")         // https://mvnrepository.com/artifact/com.google.guava/guava
        set("junitVersion", "5.10.1")            // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
        set("junitPioneerVersion", "2.2.0")     // https://mvnrepository.com/artifact/org.junit-pioneer/junit-pioneer
        set("mockitoVersion", "5.7.0")         // https://mvnrepository.com/artifact/org.mockito/mockito-junit-jupiter
        set("hamcrestVersion", "2.2")           // https://mvnrepository.com/artifact/org.hamcrest/hamcrest-core
    }

    val creekVersion : String by extra
    val guavaVersion : String by extra
    val log4jVersion : String by extra
    val junitVersion: String by extra
    val junitPioneerVersion: String by extra
    val mockitoVersion: String by extra
    val hamcrestVersion : String by extra

    dependencies {
        testImplementation("org.creekservice:creek-test-hamcrest:$creekVersion")
        testImplementation("org.creekservice:creek-test-util:$creekVersion")
        testImplementation("org.creekservice:creek-test-conformity:$creekVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
        testImplementation("org.junit-pioneer:junit-pioneer:$junitPioneerVersion")
        testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
        testImplementation("org.hamcrest:hamcrest-core:$hamcrestVersion")
        testImplementation("com.google.guava:guava-testlib:$guavaVersion")
        testImplementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
        // An old v1.x SLF4J impl as required by mbknor-jackson-jsonschema
        // Can be updated once https://github.com/mbknor/mbknor-jackson-jsonSchema/pull/172 is resolved:
        testRuntimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    }
}

defaultTasks("format", "static", "check")
