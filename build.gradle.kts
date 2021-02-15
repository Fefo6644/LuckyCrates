plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

defaultTasks("clean", "shadowJar")

project.group = "com.github.fefo"
project.version = "2.0"

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
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(8)
    }

    shadowJar {
        relocate("net.kyori", "com.github.fefo.luckycrates.lib.kyori")
        relocate("com.mojang.brigadier", "com.github.fefo.luckycrates.lib.brigadier")
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        from(sourceSets.main.get().resources.srcDirs) {
            expand("pluginVersion" to project.version)
        }
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
    maven { url = uri("https://libraries.minecraft.net") }
}

dependencies {
    implementation("net.kyori:adventure-api:4.5.0") {
        exclude(group = "org.checkerframework")
        exclude(group = "org.jetbrains")
    }
    implementation("net.kyori:adventure-platform-bukkit:4.0.0-SNAPSHOT") {
        exclude(group = "org.checkerframework")
        exclude(group = "org.jetbrains")
    }
    implementation("com.mojang:brigadier:1.0.17")
    compileOnly("com.destroystokyo.paper:paper-api:1.12.2-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:20.1.0")
}
