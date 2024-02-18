import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
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
        buildTypes.release {
            proguard {
                configurationFiles.from("compose-desktop.pro")
            }
        }
        nativeDistributions {
            modules("java.sql")
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "me.jack.compose.chart"
            packageVersion = "1.0.0"
        }
    }
}
