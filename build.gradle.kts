import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformExtension

plugins {
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = "com.merkost.drawablepreview"
version = "1.1.10"

// AS 2024.3 (243+) requires Java 21 source/target compatibility.
java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    google()
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// The Android plugin ships with Android Studio but is NOT enumerated in
// product-info.json's bundledPlugins list, so bundledPlugin("org.jetbrains.android")
// fails. We resolve its on-disk path lazily via the IntelliJPlatformExtension.
val androidPluginPath: Provider<java.io.File> = provider {
    extensions.getByType<IntelliJPlatformExtension>()
        .platformPath
        .resolve("plugins/android")
        .toFile()
}

dependencies {
    implementation("org.apache.xmlgraphics:batik-transcoder:1.18")
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        // https://plugins.jetbrains.com/docs/intellij/android-studio-releases-list.html
        androidStudio("2024.3.2.2")

        // Kotlin is listed in product-info.json so we can use bundledPlugin().
        bundledPlugin("org.jetbrains.kotlin")

        localPlugin(androidPluginPath)

        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "243"
            // Open-ended; bump per release once we verify against the next IDE.
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    compileKotlin {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
    compileTestKotlin {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
