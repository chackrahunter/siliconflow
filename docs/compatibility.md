# Minecraft compatibility policy

## Verified artifact

The current Gradle project produces one version-specific Fabric artifact:

- Minecraft: `1.21.4`
- Yarn mappings: `1.21.4+build.8`
- Fabric API: `0.119.4+1.21.4`
- Fabric Loader: `0.16.14`
- Java: `21+`
- Declared Fabric dependency: exact `minecraft: 1.21.4`

`./gradlew build` is the evidence for compile-time compatibility with this dependency set. It does not verify any other Minecraft release.

## Why runtime detection is not universal compatibility

Fabric metadata is evaluated before the mod's runtime code can safely adapt. A 1.21.4-compiled class can reference a class, method descriptor, field, or mapping that is absent or changed in another release. A version detector cannot repair that linkage failure,; it does not make Java bytecode or Mixin callback signatures portable.

`VersionDetector` is consequently limited to diagnostics and exact-target gating. It reports only whether the runtime matches this exact artifact target.

## Path to broader automatic support

For each supported Minecraft release, create a separately compiled artifact (or a controlled Gradle multi-project/source-set variant) with:

1. that release's Mojang client dependency and mappings;
2. its matching Fabric API/loader constraints;
3. version-specific Mixin targets and callback descriptors where APIs changed;
4. a separate `fabric.mod.json` dependency range or exact dependency;
5. compile, launch, and in-game smoke testing for that release.

A launcher/installer may then select the artifact using the detected Minecraft version. It must not install one universal JAR and rely on runtime detection alone.

## Prism deployment

`./gradlew build` deploys the remapped JAR to the Prism instance named by `minecraft_version` (currently `1.21.4`). Override it explicitly when testing a matching version-specific artifact:

```bash
./gradlew build -Pprism_instance=1.21.4
```

Do not deploy this artifact to a different Minecraft instance and treat a successful file copy as compatibility evidence.
