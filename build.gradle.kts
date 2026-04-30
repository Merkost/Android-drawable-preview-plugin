plugins {
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.intellij") version "1.17.4"
}

dependencies {
    implementation("org.apache.xmlgraphics:batik-transcoder:1.18")
}

group = "com.merkost.drawablepreview"
version = "1.1.10"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    // https://plugins.jetbrains.com/docs/intellij/android-studio-releases-list.html
    // https://plugins.jetbrains.com/docs/intellij/android-studio.html#android-studio-releases-listing
    version.set("2024.3.2.2")
    type.set("AI") // Target IDE Platform

//    version.set("2024.1")
//    type.set("IC") // Target IDE Platform

    /* Plugin Dependencies */
//    https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html
    plugins.set(
        listOf(
            "android",
            "org.jetbrains.kotlin",
        )
    )

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
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("243")
        untilBuild.set("")
    }

    runIde {
        ideDir.set(file("/Applications/Android Studio Preview.app/Contents"))
    }
}
