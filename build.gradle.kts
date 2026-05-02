import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
    // Compose compiler plugin — processes @Composable functions during kotlinc.
    // Compose runtime + Jewel come from the IDE's bundled modules (declared
    // below), so this plugin only adds the kotlinc compose-compiler-plugin,
    // not any shipped runtime weight.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.intellij.platform)
}

group = "com.merkost.drawablepreview"
version = "1.3.0"

// AS 243+ ships on JBR 21 and requires Java 21 bytecode.
java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
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
// Falls back to the pinned downloadable version from the version catalog
// for fresh checkouts / CI.
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
    // Batik 1.18 transitively pulls xml-apis 1.4.01 (2009-vintage) which
    // ships its own javax/xml/parsers/DocumentBuilder*.class. That stale
    // duplicate ends up on the plugin classloader and clashes with the
    // JDK's java.base copy: DocumentBuilderFactory.newInstance() returns
    // an Xerces impl from the platform classloader (extending the JDK's
    // class) which then fails to cast back to our plugin classloader's
    // copy with ClassCastException. The JDK has had javax.xml.parsers
    // built-in since Java 5, so we never need xml-apis.
    implementation(libs.batik.transcoder) {
        exclude(group = "xml-apis", module = "xml-apis")
        exclude(group = "xml-apis", module = "xml-apis-ext")
    }
    testImplementation(libs.junit)

    intellijPlatform {
        if (localStudioDir != null) {
            logger.lifecycle("Using local Android Studio at ${localStudioDir.absolutePath}")
            local(localStudioDir)
        } else {
            // https://plugins.jetbrains.com/docs/intellij/android-studio-releases-list.html
            androidStudio(libs.versions.androidStudio.get())
        }

        // Required at runtime — without it the sandbox IDE crashes during
        // Kotlin file indexing ("No ID found for serializer kotlin.FILE").
        // We compile cleanly against newer Kotlin metadata via the
        // -Xskip-metadata-version-check kotlinc flag below; we don't actually
        // call any kotlin-plugin APIs from our code so the version drift is
        // safe to ignore.
        bundledPlugin("org.jetbrains.kotlin")

        // Use the IDE's bundled Compose runtime + Jewel UI library so we
        // don't ship a duplicate ~12 MB of Skia + Compose binaries.
        //
        // Narwhal (251) is the floor; Meerkat (243) bundles Compose but no
        // Jewel. The compose.runtime.desktop module isn't a separate
        // bundle on 251 — its classes live inside compose.foundation.desktop,
        // so we don't need a third bundledModule reference for it.
        bundledModule("intellij.platform.compose")
        bundledModule("intellij.libraries.compose.runtime.desktop")
        bundledModule("intellij.libraries.compose.foundation.desktop")
        bundledModule("intellij.platform.jewel.foundation")
        bundledModule("intellij.platform.jewel.ui")
        bundledModule("intellij.platform.jewel.ideLafBridge")

        localPlugin(androidPluginPath)

        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = libs.versions.sinceBuild.get()
            // Open-ended; bump per release once we verify against the next IDE.
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

    // JetBrains Marketplace publishing.
    //
    // Token: obtained from https://plugins.jetbrains.com/author/me/tokens
    // and supplied via either:
    //   - the JETBRAINS_MARKETPLACE_TOKEN environment variable, or
    //   - a `jetbrains.marketplace.token=...` line in ~/.gradle/gradle.properties
    //     (NEVER in the repo's gradle.properties — that file is committed).
    //
    // Channel: "default" (stable) by default. Switch to "eap" / "beta" via
    // JETBRAINS_MARKETPLACE_CHANNEL when shipping previews.
    //
    // Usage:
    //   ./gradlew publishPlugin
    publishing {
        token = providers.environmentVariable("JETBRAINS_MARKETPLACE_TOKEN")
            .orElse(providers.gradleProperty("jetbrains.marketplace.token"))
        channels = providers.environmentVariable("JETBRAINS_MARKETPLACE_CHANNEL")
            .orElse(providers.gradleProperty("jetbrains.marketplace.channel"))
            .orElse("default")
            .map { listOf(it) }
        // Block release if plugin verifier fails on any recommended IDE.
        // Comment out if you want to publish anyway (e.g. EAP-only fixes).
        ideServices = false
        hidden = false
    }

    // Optional: sign the plugin so the Marketplace shows the signed-by name
    // and so the IDE doesn't warn end-users on install.
    //
    // Generate a key with:
    //   openssl genrsa -aes256 -out private.pem 4096
    //   openssl req -new -x509 -key private.pem -days 365 -out chain.crt
    //
    // Then supply the paths + password via env or ~/.gradle/gradle.properties:
    //   JETBRAINS_PLUGIN_CERT_CHAIN_FILE / jetbrains.plugin.cert.chain.file
    //   JETBRAINS_PLUGIN_PRIVATE_KEY_FILE / jetbrains.plugin.private.key.file
    //   JETBRAINS_PLUGIN_PRIVATE_KEY_PASSWORD / jetbrains.plugin.private.key.password
    //
    // Signing is silently skipped when those properties aren't set, so the
    // build still works for contributors without keys.
    signing {
        certificateChainFile = layout.file(
            providers.environmentVariable("JETBRAINS_PLUGIN_CERT_CHAIN_FILE")
                .orElse(providers.gradleProperty("jetbrains.plugin.cert.chain.file"))
                .map { File(it) },
        )
        privateKeyFile = layout.file(
            providers.environmentVariable("JETBRAINS_PLUGIN_PRIVATE_KEY_FILE")
                .orElse(providers.gradleProperty("jetbrains.plugin.private.key.file"))
                .map { File(it) },
        )
        password = providers.environmentVariable("JETBRAINS_PLUGIN_PRIVATE_KEY_PASSWORD")
            .orElse(providers.gradleProperty("jetbrains.plugin.private.key.password"))
    }
}

tasks {
    val jvmTarget = JvmTarget.fromTarget(libs.versions.jvm.get())

    compileKotlin {
        compilerOptions {
            this.jvmTarget.set(jvmTarget)
            // The bundled Kotlin plugin in newer Android Studio releases is
            // built against a newer Kotlin metadata version than our compiler.
            // Safe to ignore because we don't call any kotlin-plugin APIs.
            freeCompilerArgs.add("-Xskip-metadata-version-check")
        }
    }
    compileTestKotlin {
        compilerOptions {
            this.jvmTarget.set(jvmTarget)
            freeCompilerArgs.add("-Xskip-metadata-version-check")
        }
    }

    // Pre-indexing plugin settings for Search Everywhere is nice-to-have but
    // launches a sandboxed IDE which conflicts with any locally-running AS
    // instance ("Only one instance of Studio can be run at a time"). Skip it
    // by default — the plugin works fine without it and CI / dedicated build
    // hosts can re-enable as needed.
    buildSearchableOptions {
        enabled = false
    }
}
