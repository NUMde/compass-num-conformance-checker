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
