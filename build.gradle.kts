import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.compose") version "1.5.10"
    kotlin("plugin.serialization") version "1.9.20"
}

group = "net.bullmc"
version = "0.1.1"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    implementation("io.ktor:ktor-client-core:2.3.6")
    implementation("io.ktor:ktor-client-cio:2.3.6")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.6")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "net.bullmc.client.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.AppImage, TargetFormat.Exe, TargetFormat.Msi)
            packageName = "BullMCClient"
            packageVersion = "0.1.1"
            description = "BullMC Client Launcher"
            vendor = "BullMC"
        }
    }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "net.bullmc.client.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
