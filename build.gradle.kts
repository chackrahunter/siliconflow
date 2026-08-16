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

	mods {
		create("m3-frametime") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.getByName("client"))
		}
	}
}

tasks.processResources {
	val modVersion = project.version.toString()
	inputs.property("version", modVersion)
	filesMatching("fabric.mod.json") {
		expand("version" to modVersion)
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
	from("LICENSE") {
		rename { "${it}_${base.archivesName.get()}" }
	}
}

tasks.register("deployToPrism") {
	dependsOn("remapJar")
	doLast {
		val jarFile = tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar").get().archiveFile.get().asFile
		val targetDir = file("/Users/don-calvinkuhn/Library/Application Support/PrismLauncher/instances/1.21.11/minecraft/mods")
		if (targetDir.exists() && jarFile.exists()) {
			jarFile.copyTo(targetDir.resolve(jarFile.name), overwrite = true)
			println("--> Auto-deployed ${jarFile.name} to Prism Launcher instance!")
		}
	}
}

tasks.build {
	finalizedBy("deployToPrism")
}
