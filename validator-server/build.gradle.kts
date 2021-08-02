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
