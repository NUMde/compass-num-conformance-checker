import java.util.*

plugins {
    id("io.gitlab.arturbosch.detekt")
    id("org.sonarqube")
    jacoco
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:${project.extra["version.detekt"]}")
}

detekt {
    autoCorrect = true
    buildUponDefaultConfig = true
    config = files("${rootDir}/detekt.yml")
    ignoreFailures = true
    parallel = true

    reports {
        html { enabled = false }
        sarif { enabled = false }
        txt { enabled = false }
        xml {
            enabled = true
            destination = file("${project.buildDir}/reports/detekt/detekt.xml")
        }
    }
}

jacoco {
    toolVersion = project.extra["version.jacoco"] as String
}

tasks.withType<JacocoReport> {
    dependsOn(tasks.withType<Test>())
    reports {
        xml.required.set(true)
        html.required.set(false)
    }
}

tasks.sonarqube {
    dependsOn(tasks.withType<JacocoReport>())
}

sonarqube {
    properties {
        property("sonar.projectKey", project.name)
        property("sonar.kotlin.detekt.reportPaths", "${project.buildDir}/reports/detekt/detekt.xml")

        @Suppress("UNCHECKED_CAST")
        properties(
            file("../sonar-project.properties")
                .inputStream()
                .let { HashMap(Properties().apply { load(it) }) as Map<String, Any> }
        )
    }
}
