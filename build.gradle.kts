plugins {
    kotlin("jvm") version "1.8.22"
    id("org.jetbrains.intellij") version "1.15.0"
}

//dependencies {
//    implementation("batik:batik-transcoder:1.6-1")
//    implementation("javax.xml.parsers:jaxp-api:1.4.5")
//}

group = "com.merkost.drawablepreview"
version = "1.1.9"

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    // https://plugins.jetbrains.com/docs/intellij/android-studio-releases-list.html
    // https://plugins.jetbrains.com/docs/intellij/android-studio.html#android-studio-releases-listing
    version.set("2024.1.1.1")
    type.set("AI") // Target IDE Platform

    /* Plugin Dependencies */
    plugins.set(listOf("android"))

    /**
     * Patch plugin.xml with since and until build
     * values inferred from IDE version.
     */
    updateSinceUntilBuild.set(false)

}

repositories {
    google()
    mavenCentral()
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("242.*")
    }

    runIde {
        ideDir.set(file("/Applications/Android Studio Preview.app/Contents"))
    }
}
