plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("com.github.hierynomus.license-base") version "0.15.0"
}

defaultTasks("clean", "licenseMain", "test", "shadowJar")

project.group = "io.github.emilyy-dev"
project.version = "2.0.4"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

sourceSets {
    main {
        java.srcDir("src/main/java")
        resources.srcDir("src/main/resources")
    }

    test {
        java.srcDir("src/test/java")
        resources.srcDir("src/test/resources")
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(8)
    }

    compileTestJava {
        options.encoding = "UTF-8"
        options.release.set(8)
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        relocate("net.kyori", "io.github.emilyydev.luckycrates.lib.kyori")
        relocate("com.mojang.brigadier", "io.github.emilyydev.luckycrates.lib.brigadier")
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        from(sourceSets.main.get().resources.srcDirs) {
            expand("pluginVersion" to project.version)
        }
    }
}

license {
    header = rootProject.file("header.txt")
    encoding = "UTF-8"

    mapping("java", "DOUBLESLASH_STYLE")

    ext["year"] = 2021
    ext["name"] = "emilyy-dev"

    include("**/*.java")
}

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://libraries.minecraft.net")
    maven("https://jitpack.io/")
}

dependencies {
    implementation("net.kyori:adventure-api:4.7.0")
    implementation("net.kyori:adventure-platform-bukkit:4.0.0-SNAPSHOT")
    implementation("com.mojang:brigadier:1.0.18")
    compileOnly("com.mojang:authlib:1.5.25")
    compileOnly("com.destroystokyo.paper:paper-api:1.12.2-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude("org.bukkit", "bukkit")
    }
    compileOnly("org.jetbrains:annotations:20.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testImplementation("com.google.guava:guava:21.0")
}
