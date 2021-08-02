plugins {
    kotlin("jvm")
    id("io.spring.dependency-management")
    id("org.springframework.boot")
}

val javaVersion = JavaVersion.toVersion(extra["version.java"] as String)
java.sourceCompatibility = javaVersion
java.targetCompatibility = javaVersion

if (!JavaVersion.current().isCompatibleWith(javaVersion)) {
    throw GradleException("Java $javaVersion required, but found ${JavaVersion.current()}")
}

dependencies {
    implementation("io.github.microutils:kotlin-logging-jvm")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("com.willowtreeapps.assertk:assertk")
    testImplementation("org.mockito.kotlin:mockito-kotlin")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=enable")
        javaParameters = true
        jvmTarget = javaVersion.majorVersion
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    maxHeapSize = "3g"
    testLogging {
        outputs.upToDateWhen { false }
        showStandardStreams = true
    }
}

springBoot {
    buildInfo()
}

val activeSpringProfiles = System.getProperty("spring.profiles.active") ?: ""
if (activeSpringProfiles.contains("openapi")) {
    project.apply(plugin = "com.github.johnrengelman.processes")
    project.apply(plugin = "org.springdoc.openapi-gradle-plugin")

    dependencies {
        implementation("org.springdoc:springdoc-openapi-ui:${project.extra["version.springdoc-ui"]}")
    }

    project.configure<org.springdoc.openapi.gradle.plugin.OpenApiExtension>() {
        outputDir.set(projectDir)
        forkProperties.set("-Dspring.profiles.active=$activeSpringProfiles")
    }
}

tasks.named("clean") {
    finalizedBy("cleanLucene")
}

tasks.register<Delete>("cleanLucene") {
    group = "build"
    delete("${project.projectDir}/lucenefiles/")
    delete("${project.rootDir}/lucenefiles/")
}
