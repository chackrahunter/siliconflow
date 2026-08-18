# Contributing to SiliconFlow

SiliconFlow is a beta, client-side Fabric optimization mod for Apple-Silicon Macs. Contributions should preserve the project's exact Minecraft 1.21.4 target and keep diagnostics secondary to frame-time, memory, and visual-workload improvements.

## Before opening a pull request

- Explain the user-visible problem and the smallest supported change.
- Do not claim FPS or frame-time improvements without reproducible, raw evidence.
- Keep Minecraft API and Mixin changes backed by the 1.21.4 mappings used in `gradle.properties`.
- Update focused documentation when behavior, configuration, or compatibility changes.

Run the repository validation command locally:

```bash
./gradlew build --console=plain -Pprism_instance=1.21.11
```

This compiles the exact 1.21.4 artifact, removes obsolete SiliconFlow build artifacts, and deploys the result to the matching local Prism test instance when present. The project currently has no automated game-launch smoke test; report manual launch results separately.

## Pull requests

Keep pull requests focused. Include the Minecraft/Fabric/Java versions, Mac model and memory, companion mods, profile/configuration, and test evidence when relevant. Do not commit generated `build/`, `run/`, or local launcher files.
