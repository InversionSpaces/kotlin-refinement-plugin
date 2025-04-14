import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("jvm") version "2.1.20"
    `maven-publish`
}

group = "com.example"
version = "1.0-SNAPSHOT"

publishing {
    publications {
        create<MavenPublication>("plugin") {
            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(kotlin("compiler-embeddable"))

    compileOnly("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")

    testImplementation(kotlin("test"))
}

fun Project.optInTo(annotationFqName: String) {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions.optIn.add(annotationFqName)
    }
}

optInTo("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}