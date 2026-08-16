# SiliconFlow

Frame-time telemetry and optional client-side render optimizations for Apple Silicon Macs running Fabric Minecraft.

> This repository currently builds one version-specific artifact: **Minecraft 1.21.4**.
> It does not provide a universal JAR for all Minecraft versions, and it does not guarantee an FPS or stutter outcome.

## What it does

SiliconFlow focuses on client frame-time visibility and conservative, configurable optimizations around systems that are outside the main responsibilities of common performance mods. The implementation includes:

- an in-game F8 telemetry overlay;
- optional JSON recording to `m3-live-telemetry.json` when recording is enabled;
- frame-time, spike, GC, memory-pressure, and selected runtime telemetry;
- best-effort Darwin QoS requests, without claiming CPU-core affinity or real-time priority;
- optional soft render/entity/particle/UI reductions controlled by the active performance profile;
- compatibility checks for optional Sodium, Lithium, FerriteCore, ImmediatelyFast, and Iris integrations.

The mod does not expose GPU utilization or VRAM usage through its own telemetry. macOS remains responsible for scheduling threads and placing them on performance or efficiency cores.

## Compatibility

The current artifact is compiled and tested for the following target:

- Minecraft `1.21.4`
- Yarn mappings `1.21.4+build.8`
- Fabric API `0.119.4+1.21.4`
- Fabric Loader `0.16.14`
- Java `21+`, preferably a native Apple/aarch64 runtime on Apple Silicon

A version detector cannot make classes, mappings, or Mixin descriptors portable between Minecraft releases. Supporting another release requires a separate build and version-specific launch/smoke testing. See [`docs/compatibility.md`](docs/compatibility.md) and [`docs/versions/README.md`](docs/versions/README.md).

## F8 overlay and recording

Press **F8** in-game to toggle the telemetry overlay. It reports values the client can actually observe, such as frame-time samples, FPS derived from those samples, memory/GC probes, runtime flags, and active profile state. Values that the implementation cannot measure—such as GPU utilization, VRAM, exact CPU-core placement, and network ping in a local-only context—are shown as unavailable or omitted.

A real Minecraft screenshot of the overlay is preserved below. The image is illustrative of the UI, not a benchmark result.

<img src="docs/assets/hud_preview.png" alt="Minecraft gameplay with the SiliconFlow F8 telemetry overlay" width="95%">

The optional recorder writes periodic JSON snapshots to the configured telemetry path. Treat the file as diagnostic output, not as a standardized benchmark format; retain the raw file and configuration when sharing measurements.

## Benchmark and reproducible testing

The Prism instance used for compatibility testing includes the optional stack visible in its launch log: Sodium `0.6.13`, Iris `1.8.8`, C2ME `0.3.2`, Lithium `0.15.3`, FerriteCore `7.1.3`, ImmediatelyFast `1.8.7`, ModernFix `5.20.3`, MoreCulling `1.2.10`, Sodium Extra `0.6.1`, Cloth Config, Fabric API, and their declared dependencies. This establishes that the artifact was exercised in that mod environment; it does **not** establish a performance improvement for every mod or shader combination.

No universal FPS, frametime, GPU, VRAM, P-core, or stutter-improvement figure is claimed here. Any number copied from a local HUD or telemetry file must be labeled **measured** with its hardware, Minecraft build, mod versions, settings, scene, capture window, and raw source file. Values used only to explain the procedure must be labeled **illustrative**.

For a reproducible comparison:

1. Record the exact Mac model, RAM, macOS version, Java vendor/architecture, Minecraft/Fabric versions, mod versions, shader state, display refresh rate, render/simulation distance, and JVM arguments.
2. Create two profiles that differ only by SiliconFlow being present or absent. Keep the same world, seed, camera route, resource pack, render settings, and background applications.
3. Warm up each profile for five minutes, then repeat the same route for at least five runs. Capture raw F8/recorder output for every run; do not use a single peak FPS reading as a result.
4. Report median and p95/p99 frame time, sample count, run duration, and notable spikes. Report FPS only as a secondary summary.
5. Publish the raw telemetry and configuration alongside any aggregate. If a result was not captured under this protocol, mark it **illustrative** or **unverified**, not measured.

The companion-mod stack is supported as a compatibility test environment, not a promise that every combination has identical behavior. Iris/shader behavior remains dependent on the exact driver, pack, and version; SiliconFlow does not guarantee shader compatibility or shader performance.

## Configuration and useful guides

- [`docs/compatibility.md`](docs/compatibility.md) — exact-target policy and Prism deployment.
- [`docs/versions/README.md`](docs/versions/README.md) — version matrix and artifact selection.
- [`docs/max-fps-checklist.md`](docs/max-fps-checklist.md) — concise English checklist.
- [`docs/prism-max-performance.md`](docs/prism-max-performance.md) — conservative 8 GB Apple Silicon setup guidance.
- [`docs/stutter-research.md`](docs/stutter-research.md) — research notes and reproducible troubleshooting.
- [`docs/sodium-lithium-gap-analysis.md`](docs/sodium-lithium-gap-analysis.md) — boundaries with common performance mods.

Delete an obsolete `config/m3-frametime.json` only when upgrading if the release notes or implementation require a reset; preserve and inspect configuration when troubleshooting. The recorder and F8 overlay are useful diagnostics and are intentionally retained.

## Build and Prism deployment

```bash
./gradlew build --console=plain -Pprism_instance=1.21.11
```

The build compiles the exact `1.21.4` target and deploys the remapped JAR to the named Prism instance. A successful copy into a differently-versioned instance is not compatibility evidence. The artifact is written to `build/libs/` with the project version from `gradle.properties`.

## License

MIT — see [`LICENSE`](LICENSE).
