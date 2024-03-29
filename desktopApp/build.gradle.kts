import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
}

kotlin {
    jvm()
    sourceSets {
        val jvmMain by getting  {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(project(":shared"))
                implementation(project(":sharedDemo"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        // https://github.com/JetBrains/compose-multiplatform/issues/2668
//        buildTypes.release {
//            proguard {
//                configurationFiles.from("compose-desktop.pro")
//            }
//        }
        buildTypes.release.proguard {
            optimize.set(false)
        }
        nativeDistributions {
            modules("java.sql")
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "me.jack.compose.chart"
            packageVersion = "1.0.0"
        }
    }
}
