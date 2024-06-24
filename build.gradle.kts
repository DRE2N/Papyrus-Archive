import io.papermc.paperweight.util.constants.*

plugins {
    java
    `maven-publish`
    //id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("io.papermc.paperweight.patcher") version "1.7.1"
}

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/") {
        content { onlyForConfigurations(PAPERCLIP_CONFIG) }
    }
}

dependencies {
    remapper("net.fabricmc:tiny-remapper:0.10.3:fat")
    decompiler("org.vineflower:vineflower:1.10.1")
    paperclip("io.papermc:paperclip:3.0.3")
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21);
        options.isFailOnError = true;
    }
    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources> {
        filteringCharset = Charsets.UTF_8.name()
    }

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://ci.emc.gs/nexus/content/groups/aikar/")
        maven("https://repo.aikar.co/content/groups/aikar")
        maven("https://repo.md-5.net/content/repositories/releases/")
        maven("https://hub.spigotmc.org/nexus/content/groups/public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://jitpack.io")

    }
}
val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"
paperweight {
    serverProject = project(":papyrus-server")

    remapRepo = paperMavenPublicUrl
    decompileRepo = paperMavenPublicUrl

    usePaperUpstream(providers.gradleProperty("paperCommit")) {
        withPaperPatcher {
            apiPatchDir = layout.projectDirectory.dir("patches/api")
            apiOutputDir = layout.projectDirectory.dir("papyrus-api")

            serverPatchDir = layout.projectDirectory.dir("patches/server")
            serverOutputDir = layout.projectDirectory.dir("papyrus-server")
        }

        patchTasks.register("generatedApi") {
            isBareDirectory = true
            upstreamDirPath = "paper-api-generator/generated"
            patchDir = layout.projectDirectory.dir("patches/generated-api")
            outputDir = layout.projectDirectory.dir("paper-api-generator/generated")
        }
    }
}

tasks.generateDevelopmentBundle {
    apiCoordinates.set("de.erethon.papyrus:papyrus-api")
    libraryRepositories.set(
        listOf(
            "https://repo.maven.apache.org/maven2/",
            "https://libraries.minecraft.net/",
            "https://papermc.io/repo/repository/maven-public/",
            "https://maven.quiltmc.org/repository/release/",
            "https://erethon.de/repo"
        )
    )
}

allprojects {
    // Publishing API:
    // ./gradlew :ForkTest-API:publish[ToMavenLocal]
    publishing {
        repositories {
            maven {
                name = "erethon"
                url = uri("sftp://grebe.bloom.host:2022")
                credentials(PasswordCredentials::class) // Put credentials in ~/.gradle/gradle.properties
            }
        }
        // Always publish dev bundle
        publications.create<MavenPublication>("devBundle") {
            artifact(rootProject.tasks.generateDevelopmentBundle) {
                artifactId = "dev-bundle"
            }
        }
    }
}

publishing {
    // Publishing dev bundle:
    // ./gradlew publishDevBundlePublicationTo(MavenLocal|MyRepoSnapshotsRepository) -PpublishDevBundle
    if (project.hasProperty("publishDevBundle")) {
        publications.create<MavenPublication>("devBundle") {
            artifact(tasks.generateDevelopmentBundle) {
                artifactId = "dev-bundle"
            }
        }
    }
}
