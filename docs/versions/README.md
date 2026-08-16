# Version matrix and artifact selection

This repository intentionally builds one Fabric JAR per Minecraft release. The current target is **Minecraft 1.21.4**. The current artifact version is `1.0.36+1.21.4`. `compatibility-matrix.json` is the source-of-truth inventory for implementation status; it is not a claim that every listed release is supported.

## Adding a target

1. Copy the project into a version-specific Gradle subproject (or add a controlled target source set).
2. Set `minecraft_version`, matching Yarn/Mojang mappings, Fabric API, Loader, and Loom line.
3. Keep changed mixins and callback descriptors in that target's source/resources; do not merge incompatible descriptors into the shared JAR.
4. Generate target metadata from `src/main/resources/m3frametime.build-target.json`.
5. Compile, launch, and smoke-test the exact target before changing its matrix status to `built` or `launch-verified`.

## Selection

A launcher or installer must select an artifact whose embedded `m3frametime.build-target.json` has an exact `minecraft` match. The repository includes a deterministic selector:

```bash
python3 tools/select-artifact.py 1.21.4 build/libs
```

It rejects unknown, snapshot, or mismatched artifacts rather than relying on `VersionDetector`.

Fabric Loom supports version-specific Minecraft dependencies and multiple source sets, but it does not translate one compiled classpath across incompatible Minecraft APIs.
