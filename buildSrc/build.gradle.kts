import java.util.*

plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

val versions = file("../gradle.properties").inputStream()
    .let { Properties().apply { load(it) } }
    .filter { it.key.toString().startsWith("version.") }

dependencies {
    implementation("com.github.kkdad:gradle-license-report:${versions["version.dependencyLicenseReport"]}")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${versions["version.detekt"]}")
    implementation("io.spring.gradle:dependency-management-plugin:${versions["version.dependencyManagement"]}")
    implementation("gradle.plugin.com.github.jengelman.gradle.plugins:gradle-processes:${versions["version.gradleProcesses"]}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${versions["version.kotlin"]}")
    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:${versions["version.sonarqube"]}")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:${versions["version.springBoot"]}")
    implementation("org.springdoc:springdoc-openapi-gradle-plugin:${versions["version.springdoc-gradle"]}")
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.apache.logging.log4j") {
            useVersion("[2.16.0,)")
            because("earlier versions of log4j are affected by log4shell vulnerability (https://nvd.nist.gov/vuln/detail/CVE-2021-44228)")
        }
    }
}
