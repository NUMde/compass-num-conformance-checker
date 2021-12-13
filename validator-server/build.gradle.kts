plugins {
    id("server")
}

group = "science.num-compass.conformance.fhir"
version = "0.0.1-SNAPSHOT"

dependencies {
    api("ca.uhn.hapi.fhir:hapi-fhir-jpaserver-base") {
        exclude("org.apache.commons", "commons-dbcp")
        exclude("org.elasticsearch.client")
        exclude("org.hibernate.search", "hibernate-search-backend-elasticsearch")
    }
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.zaxxer:HikariCP")
    api("org.postgresql:postgresql")
    api("org.springframework.boot:spring-boot-starter-web")
    api("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.apache.logging.log4j") {
            useVersion("[2.15.0,)")
            because("earlier versions of log4j are affected by log4shell vulnerability (https://nvd.nist.gov/vuln/detail/CVE-2021-44228)")
        }
    }
}
