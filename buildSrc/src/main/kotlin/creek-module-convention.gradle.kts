// Common configuration of JPMS
plugins {
    java
    id("org.javamodularity.moduleplugin")
}

java {
    modularity.inferModulePath.set(false)
}
