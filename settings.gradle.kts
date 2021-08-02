pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.spring.dependency-management") {
                useVersion("${extra["version.dependencyManagement"]}")
            }
            if (requested.id.id == "org.jetbrains.kotlin.plugin.spring") {
                useVersion("${extra["version.kotlin"]}")
            }
            if (requested.id.id == "org.springframework.boot") {
                useVersion("${extra["version.springBoot"]}")
            }
        }
    }
}

rootProject.name = "compass"

include("validator-server")
include("compass-conformance-server")
