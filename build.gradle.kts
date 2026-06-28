plugins {
    id("java-library")
    id("com.gradleup.shadow") version "8.3.5"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("io.lumine:Mythic-Dist:5.7.0-SNAPSHOT")
    compileOnly("org.apache.logging.log4j:log4j-core:2.24.1")
    implementation("com.github.cryptomorin:XSeries:11.2.0.1")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
    
    shadowJar {
        // Removed relocate to avoid ASM Java 21 bug
    }
    
    build {
        dependsOn(shadowJar)
    }
}
