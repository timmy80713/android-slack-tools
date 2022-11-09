import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
}

repositories {
    mavenCentral()
}

val invoker by configurations.creating

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    implementation("com.google.cloud.functions:functions-framework-api:1.0.4")

    implementation("io.ktor:ktor-client-core:2.1.3")
    implementation("io.ktor:ktor-client-cio:2.1.3")
    implementation("io.ktor:ktor-client-content-negotiation:2.1.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.1.3")

    implementation("com.github.ajalt.clikt:clikt:3.5.0")

    // To run function locally using Functions Framework's local invoker
    invoker("com.google.cloud.functions.invoker:java-function-invoker:1.2.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("functions.AppKt")
}


tasks.named("build") {
    dependsOn(":shadowJar")
}

task<Copy>("buildFunction") {
    dependsOn("build")
    println("$version")
    from("build/libs/${rootProject.name}-all.jar") {
        rename { "${rootProject.name}.jar" }
    }
    into("build/deploy")
}

task<JavaExec>("runFunction") {
    mainClass.set("com.google.cloud.functions.invoker.runner.Invoker")
    classpath(invoker)
    inputs.files(configurations.runtimeClasspath, sourceSets.main.get().output)
    args(
        "--target", project.findProperty("run.functionTarget") ?: "functions.App",
        "--port", project.findProperty("run.port") ?: 8080,
    )
    doFirst {
        args("--classpath", files(configurations.runtimeClasspath, sourceSets.main.get().output).asPath)
    }
}