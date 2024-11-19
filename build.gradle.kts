plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "ua.valeriishymchuk"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")



    val configVersion = "4.1.2"
    implementation("org.spongepowered:configurate-core:$configVersion")
    implementation("org.spongepowered:configurate-yaml:$configVersion")
    implementation("io.vavr:vavr:0.10.4")

    val cloudVersion = "1.8.4"
    implementation("cloud.commandframework:cloud-paper:$cloudVersion")
    implementation("cloud.commandframework:cloud-bukkit:$cloudVersion")
    implementation("cloud.commandframework:cloud-core:$cloudVersion")
    implementation("cloud.commandframework:cloud-minecraft-extras:$cloudVersion")

}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.named("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    exclude("net/kyori/**")
}

tasks.test {
    useJUnitPlatform()
}