plugins {
	id("fabric-loom") version "1.10.5"
	id("maven-publish")
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
	archivesName.set(project.property("archives_base_name") as String)
}

repositories {
	mavenCentral()
}

dependencies {
	minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
	mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
	modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
}

loom {
	splitEnvironmentSourceSets()
	mixin {
		defaultRefmapName = "m3frametime.refmap.json"
	}

	mods {
		create("m3-frametime") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.getByName("client"))
		}
	}
}

tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<AbstractCopyTask>().configureEach {
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val targetMinecraft = providers.gradleProperty("minecraft_version").orElse("unknown")
val targetMappings = providers.gradleProperty("yarn_mappings").orElse("unknown")

tasks.register("validateVersionTarget") {
	group = "verification"
	description = "Validate that this artifact declares one explicit Minecraft target."
	doLast {
		val minecraft = targetMinecraft.get()
		check(minecraft != "unknown" && minecraft.matches(Regex("\\d+\\.\\d+(\\.\\d+)?"))) {
			error("minecraft_version must be an explicit release, got '$minecraft'")
		}
		check(targetMappings.get().startsWith("$minecraft+")) {
			error("yarn_mappings must match minecraft_version ($minecraft), got '${targetMappings.get()}'")
		}
	}
}

// Every produced JAR carries its exact build target for launchers and artifact audits.
tasks.processResources {
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	val modVersion = project.version.toString()
	inputs.property("version", modVersion)
	filesMatching("fabric.mod.json") {
		expand("version" to modVersion)
	}
	filesMatching("m3frametime.build-target.json") {
		expand(
			"minecraft_version" to targetMinecraft.get(),
			"yarn_mappings" to targetMappings.get(),
			"fabric_version" to project.property("fabric_version"),
			"loader_version" to project.property("loader_version")
		)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(21)
	options.encoding = "UTF-8"
}

java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	from("LICENSE") {
		rename { "${it}_${base.archivesName.get()}" }
	}
}

tasks.register("cleanBuildArtifacts") {
	dependsOn("remapJar")
	group = "build"
	description = "Remove prior SiliconFlow runtime artifacts from build/libs before packaging."
	doLast {
		val currentArtifact = tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar").get().archiveFile.get().asFile.name
		val outputDir = layout.buildDirectory.dir("libs").get().asFile
		val removedArtifacts = outputDir.listFiles().orEmpty()
			.filter { candidate ->
				candidate.isFile &&
					candidate.name != currentArtifact &&
					candidate.name.endsWith(".jar") &&
					!candidate.name.endsWith("-sources.jar") &&
					(candidate.name.startsWith("siliconflow-") || candidate.name.startsWith("m3-frametime-"))
			}
			.onEach { candidate -> check(candidate.delete()) { "Could not remove old build artifact ${candidate.absolutePath}" } }
		println("--> Removed ${removedArtifacts.size} old SiliconFlow build artifact(s) from ${outputDir.absolutePath}")
	}
}

tasks.register("deployToPrism") {
	dependsOn("remapJar")
	doLast {
		val jarFile = tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar").get().archiveFile.get().asFile
		val prismInstance = providers.gradleProperty("prism_instance").orElse(project.property("minecraft_version") as String).get()
		val targetDir = file("/Users/don-calvinkuhn/Library/Application Support/PrismLauncher/instances/$prismInstance/minecraft/mods")
		val targetMinecraftVersion = targetMinecraft.get()
		val instanceDir = targetDir.parentFile?.parentFile
		val metadataFile = instanceDir?.resolve("mmc-pack.json")

		if (!targetDir.isDirectory() || !jarFile.isFile) {
			println("--> Prism deployment skipped: matching instance not found at ${targetDir.absolutePath}")
			return@doLast
		}

		if (metadataFile?.isFile == true) {
			val metadata = metadataFile.readText()
			val minecraftVersion = metadata.substringAfter("\"uid\": \"net.minecraft\"", "").let { instanceMetadata ->
				Regex("""\"version\"\s*:\s*\"([^\"]+)\"""").find(instanceMetadata)?.groupValues?.get(1)
			}
			if (minecraftVersion == null) {
				println("--> Prism deployment warning: Minecraft target metadata was not readable in ${metadataFile.absolutePath}")
			} else {
				check(minecraftVersion == targetMinecraftVersion) {
					"Prism instance $prismInstance targets Minecraft $minecraftVersion, but this artifact targets $targetMinecraftVersion"
				}
				println("--> Verified Prism instance $prismInstance metadata targets Minecraft $minecraftVersion")
			}
		} else {
			println("--> Prism deployment warning: target metadata not found at ${metadataFile?.absolutePath ?: "instance root"}")
		}

		val artifactName = jarFile.name
		val oldArtifactNames = targetDir.listFiles()
			.orEmpty()
			.filter { candidate ->
				candidate.isFile &&
					candidate.name != artifactName &&
					candidate.name.endsWith(".jar") &&
					!candidate.name.endsWith("-sources.jar") &&
					(candidate.name.startsWith("siliconflow-") || candidate.name.startsWith("m3-frametime-"))
			}
			.onEach { candidate -> check(candidate.delete()) { "Could not remove old SiliconFlow artifact ${candidate.absolutePath}" } }

		jarFile.copyTo(targetDir.resolve(artifactName), overwrite = true)
		println("--> Removed ${oldArtifactNames.size} old SiliconFlow artifact(s)")
		println("--> Auto-deployed $artifactName to Prism Launcher instance $prismInstance")
	}
}

tasks.build {
	dependsOn("validateVersionTarget", "cleanBuildArtifacts")
	finalizedBy("deployToPrism")
}
