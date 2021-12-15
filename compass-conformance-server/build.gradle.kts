plugins {
    id("server")
}

group = "science.num-compass.conformance.compass"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":validator-server"))
    implementation("org.apache.pdfbox:pdfbox")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.apache.logging.log4j") {
            useVersion("[2.16.0,)")
            because("earlier versions of log4j are affected by log4shell vulnerability (https://nvd.nist.gov/vuln/detail/CVE-2021-44228)")
        }
    }
}
