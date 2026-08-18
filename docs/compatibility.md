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

Fabric metadata is evaluated before the mod's runtime code can safely adapt. A 1.21.4-compiled class can reference a class, method descriptor, field, or mapping that is absent or changed in another release. A version detector cannot repair that linkage failure; it does not make Java bytecode or Mixin callback signatures portable.

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

`./gradlew build` deploys the remapped JAR to the Prism instance named by `-Pprism_instance` (local default in this repo: `1.21.11`). That folder name is a launcher instance label. The compiled game target remains Minecraft **1.21.4**.

```bash
./gradlew build --console=plain -Pprism_instance=1.21.11
```

Do not deploy this artifact to a different Minecraft instance and treat a successful file copy as compatibility evidence.

## SiliconFlow ownership boundary

The implementation does not replace Sodium terrain meshing, Lithium ticking, or the Iris shader pipeline, and it does not silently rewrite `-Xmx`, Sodium `SWAP`, Iris pack settings, VSync/FPS caps, or macOS memory policy. Native ARM64 Java, Sodium `SWAP`, shader pack quality, Retina/framebuffer scale, display mode, and heap/collector choices remain user-controlled.

SiliconFlow-owned behavior is mixin-gated visual workload (entity/particle/HUD culls and related skips), RAM-class budget caps, session shader auto-throttle of those SiliconFlow flags only, bounded local diagnostics (F7/F8), opt-in recorder output, atomic config persistence, and pressure-aware trimming of its own scratch state. It never calls `System.gc()`.
