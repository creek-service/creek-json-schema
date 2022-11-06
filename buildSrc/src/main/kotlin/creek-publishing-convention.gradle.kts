// Common configuration for Creek library publishing
// Apply this plugin only to subprojects if in multi-module setup.

plugins {
    java
    signing
    `maven-publish`
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).apply {
            addBooleanOption("html5", true)
            // -quite? See: https://github.com/gradle/gradle/issues/2354
            addStringOption("Xwerror", "-quiet")
        }
    }
}

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
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "${rootProject.name}-${project.name}"

            pom {
                name.set("${project.group}:${artifactId}")

                description.set("${rootProject.name.capitalize()} ${project.name.capitalize()} library")

                url.set("https://www.creekservice.org")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        name.set("Andy Coates")
                        email.set("8012398+big-andy-coates@users.noreply.github.com")
                        organization.set("Creek-Service")
                        organizationUrl.set("https://www.creekservice.org")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/creek-service/${rootProject.name}.git")
                    developerConnection.set("scm:git:ssh://github.com/creek-service/${rootProject.name}.git")
                    url.set("https://github.com/creek-service/${rootProject.name}")
                }
            }
        }
    }
}

/**
 * Configure signing of publication artifacts.
 *
 * <p>Can be skipped for snapshot builds.<p>
 *
 * <p>Even snapshot builds will be signed if signing credentials are available.
 * See https://central.sonatype.org/publish/publish-gradle/#credentials
 */
signing {
    setRequired {
        !project.version.toString().endsWith("-SNAPSHOT")
                && !project.hasProperty("skipSigning")
    }

    if (project.hasProperty("signingKey")) {
        useInMemoryPgpKeys(properties["signingKey"].toString(), properties["signingPassword"].toString())
    }

    sign(publishing.publications["mavenJava"])
}
