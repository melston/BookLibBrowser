import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    //alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.materialIconsExtended) // Fixed key

            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.dropbox)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.firebase.crashlytics.buildtools)
            implementation(libs.google.code.gson)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.mysql.connector)
        }
    }
}


compose.desktop {
    application {
        mainClass = "org.elsoft.bkdb.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ebook-lib-browser"
            packageVersion = "1.0.0"
            description = "Ebook Library Browser"
            copyright = "@ 2026 Mark Elston"
            vendor = "Mark Elston"

            modules(
                "java.instrument",
                "java.sql",
                "jdk.unsupported",
                "java.naming",
                "java.desktop"
            )

            // This helps include standard library parts often missed by
            // reflection-heavy libraries like Gson
            includeAllModules = false

            linux {
                // This creates a shortcut in the Ubuntu app list
                shortcut = true
                // Point to an .png or .icns file for the sidebar icon
                iconFile.set(project.file("src/jvmMain/resources/books.png"))
            }
            windows {
                shortcut = true
                menuGroup = "EBook Library Browser"
                // upgradeUuid is vital for updates (prevents double-installing)
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"

                // Windows requires an .ico file specifically.
                // You can convert your books.png to books.ico online or via GIMP.
                iconFile.set(project.file("src/jvmMain/resources/books.png"))

                // This puts the app in "Add/Remove Programs" correctly
                dirChooser = true
                perUserInstall = true // Set to true if you don't have Admin rights on the work laptop

                // Optional: If you want a specific splash screen on Windows startup
                // bitmapEditor = project.file("src/jvmMain/resources/splash.bmp")
            }
        }
    }
}
