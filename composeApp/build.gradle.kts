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

            linux {
                // This creates a shortcut in the Ubuntu app list
                shortcut = true
                // Point to an .png or .icns file for the sidebar icon
                iconFile.set(project.file("src/jvmMain/resources/books.png"))
            }
            windows {
                shortcut = true
                menuGroup = "EBook Library"
                // This ensures it puts it in a sensible place in the Start Menu
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            }
        }
    }
}
