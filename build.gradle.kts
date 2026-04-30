import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformExtension
import java.io.File
import java.util.Properties

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

// Optional per-developer override: point the build at a locally-installed
// Android Studio (faster iteration, no ~1GB download). Add to local.properties:
//   studio.dir=/Applications/Android Studio.app/Contents
// Falls back to the pinned downloadable version below for fresh checkouts / CI.
val localStudioDir: File? = run {
    val props = Properties()
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { props.load(it) }
    val path = props.getProperty("studio.dir") ?: return@run null
    val dir = File(path)
    if (dir.isDirectory) dir else null
}

// The Android plugin ships with Android Studio but is NOT enumerated in
// product-info.json's bundledPlugins list, so bundledPlugin("org.jetbrains.android")
// fails. We resolve its on-disk path lazily via the IntelliJPlatformExtension.
val androidPluginPath: Provider<File> = provider {
    extensions.getByType<IntelliJPlatformExtension>()
        .platformPath
        .resolve("plugins/android")
        .toFile()
}

dependencies {
    implementation("org.apache.xmlgraphics:batik-transcoder:1.18")
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        if (localStudioDir != null) {
            logger.lifecycle("Using local Android Studio at ${localStudioDir.absolutePath}")
            local(localStudioDir)
        } else {
            // https://plugins.jetbrains.com/docs/intellij/android-studio-releases-list.html
            androidStudio("2024.3.2.2")
        }

        // Note: we don't depend on the Kotlin plugin — our code doesn't use
        // any of its APIs. Pulling it in only created Kotlin metadata version
        // conflicts when running against a newer local AS than the pinned one.

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
