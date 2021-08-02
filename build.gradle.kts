import io.spring.gradle.dependencymanagement.internal.dsl.StandardDependencyManagementExtension

plugins {
    idea
    id("license-report")
    id("io.spring.dependency-management") apply false
    id("org.jetbrains.kotlin.plugin.spring") apply false
}

subprojects {
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.gradle.idea")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "code-quality")

    repositories {
        mavenLocal()
        mavenCentral()
    }

    configure<StandardDependencyManagementExtension>() {
        imports {
            mavenBom("ca.uhn.hapi.fhir:hapi-fhir-bom:${extra["version.hapi"]}")
            mavenBom("org.springframework.boot:spring-boot-dependencies:${extra["version.springBoot"]}")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:${extra["version.springCloud"]}")
            mavenBom("org.jetbrains.kotlin:kotlin-bom:${extra["version.kotlin"]}")
        }
        dependencies {
            dependency("ca.uhn.hapi.fhir:hapi-fhir-jpaserver-base:${extra["version.hapi"]}") {
                exclude("co.elastic.apm:apm-agent-api")
                exclude("org.apache.commons:commons-dbcp")
                exclude("org.hibernate.search:hibernate-search-backend-elasticsearch")
            }
            dependency("ca.uhn.hapi.fhir:org.hl7.fhir.validation:${extra["version.fhir"]}")
            dependency("com.willowtreeapps.assertk:assertk:${extra["version.assertK"]}")
            dependency("io.github.microutils:kotlin-logging-jvm:${extra["version.kotlinLogging"]}")
            dependency("org.apache.pdfbox:pdfbox:${extra["version.pdfbox"]}")
            dependency("org.mockito.kotlin:mockito-kotlin:${extra["version.mockitoKotlin"]}")
        }
    }

    idea {
        module {
            isDownloadSources = true
        }
    }
}
