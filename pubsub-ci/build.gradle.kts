plugins {
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
}

repositories {
    mavenCentral()
}

val invoker by configurations.creating

dependencies {
    // Jetbrains
    implementation("io.ktor:ktor-client-core:2.2.4")
    implementation("io.ktor:ktor-client-cio:2.2.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.2.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    // Google
    implementation("com.google.cloud.functions:functions-framework-api:1.0.4")
    invoker("com.google.cloud.functions.invoker:java-function-invoker:1.2.0")

    // Clikt
    implementation("com.github.ajalt.clikt:clikt:3.5.2")
}


application {
    mainClass.set("functions.AppKt")
}

tasks.apply {
    jar { enabled = false }
    build { dependsOn(":shadowJar") }
}

task<JavaExec>("runFunction") {
    mainClass.set("com.google.cloud.functions.invoker.runner.Invoker")
    classpath(invoker)
    inputs.files(configurations.getByName("runtimeClasspath"), sourceSets.main.get().output)
    args(
        "--target", project.findProperty("run.functionTarget") ?: "functions.App",
        "--port", project.findProperty("run.port") ?: 8080,
    )
    doFirst {
        args("--classpath", files(configurations.runtimeClasspath, sourceSets.main.get().output).asPath)
    }
}