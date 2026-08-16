<div align="center">

# SiliconFlow

### Apple-Silicon performance companion for Minecraft on M-chip Macs

<p>
  <a href="https://github.com/chackrahunter/siliconflow"><img src="https://img.shields.io/badge/GitHub-Repository-181717?style=for-the-badge&logo=github&logoColor=white" alt="GitHub repository"></a>
  <a href="https://github.com/chackrahunter/siliconflow/releases"><img src="https://img.shields.io/github/v/release/chackrahunter/siliconflow?display_name=tag&style=for-the-badge&label=Latest%20release&logo=github&logoColor=white" alt="Latest release"></a>
  <img src="https://img.shields.io/badge/Minecraft-1.21.4-5E7D3A?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft 1.21.4 only">
  <img src="https://img.shields.io/badge/Fabric-1.21.4-DBB48C?style=for-the-badge&logo=fabric&logoColor=white" alt="Fabric 1.21.4">
  <img src="https://img.shields.io/badge/Apple%20Silicon-macOS-000000?style=for-the-badge&logo=apple&logoColor=white" alt="Apple Silicon macOS">
</p>
<p>
  <img src="https://img.shields.io/badge/Status-Beta-F59E0B?style=for-the-badge" alt="Beta software">
  <a href="https://ko-fi.com/chackrahunter"><img src="https://img.shields.io/badge/Support_on-Ko--fi-FF5E5B?style=for-the-badge&logo=kofi&logoColor=white" alt="Support on Ko-fi"></a>
  <a href="https://www.paypal.me/Donsko2007"><img src="https://img.shields.io/badge/Donate-PayPal-00457C?style=for-the-badge&logo=paypal&logoColor=white" alt="Donate with PayPal"></a>
</p>

<p><strong>High-performance Minecraft optimization for Apple-Silicon Macs.</strong><br>
<sub>Client-side frame pacing, memory discipline, and visual-workload controls for M-series chips.</sub></p>

</div>

> [!IMPORTANT]
> **Beta software, exact target:** SiliconFlow currently builds one version-specific Fabric artifact for **Minecraft 1.21.4**. The dependency target is compile-verified; broader launch compatibility is not implied. It is not a universal JAR. Companion-mod integrations and optional troubleshooting tools are best-effort, and no setting guarantees a particular FPS, frame time, GPU result, or shader result.

<p align="center">
  <img src="docs/assets/hud_preview.png" alt="Minecraft forest gameplay with the SiliconFlow F8 diagnostic overlay visible" width="100%">
</p>

<p align="center"><em>Genuine Minecraft capture from the project. The optional F8 overlay is diagnostic context, not a benchmark result; labels may differ between builds.</em></p>

## Start here

SiliconFlow is a high-performance boost and optimization mod for Fabric Minecraft on Apple-Silicon Macs with M-series chips. It focuses on practical client-side improvements: steadier frame pacing, conservative memory behavior, reduced rendering overhead, and configurable visual-workload controls. Profiles are bounded and user-owned so you can tune the balance between smoothness, image detail, and system headroom.

The optional F8 overlay and performance recorder were created for development and troubleshooting. They are not the product’s purpose, are not required for normal gameplay, and do not turn SiliconFlow into a tracking or telemetry product.

1. **Install** the exact Minecraft 1.21.4 artifact.
2. **Start** with the `PLAYABLE` profile.
3. **Tune** individual settings only when your workload calls for it.

| If you want to… | Start with… |
| --- | --- |
| Install the exact target build | [Installation](#installation) |
| Understand what the mod owns | [Ownership boundaries](#ownership-boundaries) |
| Inspect a live session | [Optional diagnostics](#optional-developer-and-troubleshooting-tools) |
| Compare two configurations | [Optional benchmark method](#optional-benchmark-and-troubleshooting-methodology) |
| Tune an 8 GB Apple-Silicon Mac | [`docs/max-fps-checklist.md`](docs/max-fps-checklist.md) |
| Use optional diagnostics | [Diagnostics](#optional-developer-and-troubleshooting-tools) |
| Understand version support | [`docs/compatibility.md`](docs/compatibility.md) |

## Why SiliconFlow optimizes Apple Silicon

Minecraft Java on macOS uses an OpenGL path translated onto Metal. Apple Silicon also uses unified memory: the JVM heap, native Minecraft allocations, GPU resources, the compositor, and other applications share one pool. A larger Java heap is therefore not automatically more headroom.

SiliconFlow applies controls that are useful and supportable from the client. It does not pretend to measure GPU utilization, VRAM, or exact core placement when those values are outside its probes; macOS remains authoritative for scheduling and memory pressure.

<p align="center">
  <img src="docs/assets/performance-model.svg" alt="Diagram showing Apple-Silicon optimization across shared memory, rendering, scheduling, and optional diagnostics" width="100%">
</p>

*Technical model, not a performance chart. It shows why frame-time work must consider the whole client and operating system rather than FPS alone.*

## Performance optimization features

- **Frame-time discipline** — bounded pacing and spike-aware behavior intended to reduce avoidable client-side variance.
- **Memory discipline** — conservative defaults and pressure-aware signals for shared-memory Macs.
- **Visual workload controls** — optional budgets or reductions for particles, distant entities and block entities, selected overlays, clouds and weather extras, glints, lightmap cadence, and related client work.
- **Shader-aware boundaries** — optional reductions do not attempt to replace or patch a shader pipeline.
- **Apple-Silicon-friendly requests** — best-effort Darwin QoS and thread-priority requests where supported; macOS retains scheduling authority.
- **Optional developer tools** — an F8 overlay and bounded JSON recorder for troubleshooting and controlled comparisons; neither is required for normal gameplay.
- **User choice** — profiles and individual settings keep visual and performance trade-offs explicit.

### Profiles

| Profile | Intended use | Character |
| --- | --- | --- |
| `PLAYABLE` | Normal gameplay | Recommended starting point; conservative and reversible |
| `BALANCED` | Heavier scenes | Stronger performance trade-off |
| `MAX` | Difficult workloads | Aggressive visual-cost profile |
| `TELEMETRY` | Investigation | Optional diagnostics-oriented behavior |

A profile is a policy preset, not a guarantee. Start with `PLAYABLE`, change one variable at a time, and compare the same workload.

## Ownership boundaries

SiliconFlow is designed to complement specialized mods instead of replacing their core systems:

| System | Primary owner | SiliconFlow’s boundary |
| --- | --- | --- |
| Terrain rendering, chunk meshes, terrain culling | Sodium | Does not replace the terrain renderer or its mesh pipeline |
| Game-logic optimizations and ticking | Lithium | Does not replace AI, collision, or tick scheduling |
| Memory-structure deduplication | FerriteCore | Does not re-implement its model and blockstate caches |
| Immediate-mode batching | ImmediatelyFast | Does not replace its immediate buffers or HUD batching |
| Shader pipeline | Iris | Treats shader runs as a separate workload; no shader guarantee |
| Scheduling, memory pressure, swap, core placement | macOS | May request QoS; the OS remains authoritative |
| Client-side policy, diagnostics, and selected visual workload | SiliconFlow | Bounded, configurable, and best-effort |

<p align="center">
  <img src="docs/assets/architecture.svg" alt="Schematic showing Minecraft client workload flowing through SiliconFlow optimization policies to optional diagnostics and performance controls, with macOS retaining scheduling authority" width="100%">
</p>

*Data flow and ownership schematic. It is not a measured performance chart.*

## Optional developer and troubleshooting tools

These tools support SiliconFlow development and focused troubleshooting. SiliconFlow is an optimization mod, not a tracking or telemetry product.

### F8 overlay

Press **F8** to toggle the optional overlay. It reports observable signals such as frame-time samples, derived FPS, memory and GC probes, active profile state, and recent spikes. Values SiliconFlow cannot instrument are omitted or marked unavailable rather than invented.

### JSON recorder

When explicitly enabled for troubleshooting, the bounded recorder can write periodic diagnostics to:

```text
<instance>/m3-live-telemetry.json
```

Keep the file with the matching configuration and test notes. It is troubleshooting evidence, not a standardized benchmark format, and it is not required for normal gameplay.

## Exact compatibility

The current artifact is compiled for this exact target:

| Component | Verified target |
| --- | --- |
| Minecraft | **1.21.4 only** |
| Yarn mappings | `1.21.4+build.8` |
| Fabric API | `0.119.4+1.21.4` |
| Fabric Loader | `0.16.14` |
| Java | `21+`; native Apple/aarch64 is recommended on Apple Silicon |
| Platform | Fabric client on macOS / Apple Silicon |

The version detector can report whether the client resembles the target; it cannot make this artifact compatible with another Minecraft release. A different release needs a separately compiled and smoke-tested target. See [`docs/compatibility.md`](docs/compatibility.md) and [`docs/versions/README.md`](docs/versions/README.md).

### Tested companion environment

SiliconFlow can coexist with Sodium, Lithium, FerriteCore, ImmediatelyFast, and Iris. The repository’s tested environment includes Sodium `0.6.13`, Iris `1.8.8`, C2ME `0.3.2`, Lithium `0.15.3`, FerriteCore `7.1.3`, ImmediatelyFast `1.8.7`, ModernFix `5.20.3`, MoreCulling `1.2.10`, Sodium Extra `0.6.1`, Cloth Config, Fabric API, and their dependencies.

That is a test environment, not a universal compatibility promise. Verify optional modules against the exact versions and workload you use. Treat Iris and shader runs separately from unshaded runs.

## Installation

1. Install **Minecraft 1.21.4**, Fabric Loader `0.16.14`, Fabric API `0.119.4`, and Java 21 or newer.
2. Install the SiliconFlow release JAR built for **1.21.4** into the instance’s `mods/` directory.
3. Add companion mods only when their exact versions match the workload you intend to test.
4. Launch the game. SiliconFlow starts with its safe default profile; press **F8** only if you want the optional overlay.
5. Enable recording in the generated configuration only when you need diagnostic evidence.

Do not install this artifact into another Minecraft release and treat a successful file copy as compatibility evidence.

## Configuration

The configuration is stored at:

```text
<instance>/config/m3-frametime.json
```

The configuration owns user intent: active optimization profile, optional F8 overlay, bounded performance recording, spike threshold, optional spike logging, render-thread/Darwin QoS requests, and swap-interval behavior. It does not override the ownership boundaries above.

Keep the file when troubleshooting. Reset it only when release notes or an implementation migration explicitly call for it.

| Need | Guide |
| --- | --- |
| A concise Apple-Silicon checklist | [`docs/max-fps-checklist.md`](docs/max-fps-checklist.md) |
| Conservative Prism guidance for 8 GB systems | [`docs/prism-max-performance.md`](docs/prism-max-performance.md) |
| Boundaries with common performance mods | [`docs/sodium-lithium-gap-analysis.md`](docs/sodium-lithium-gap-analysis.md) |
| Causes, evidence, and repeatable troubleshooting | [`docs/stutter-research.md`](docs/stutter-research.md) |
| Version and integration policy | [`docs/compatibility.md`](docs/compatibility.md) |

## Optional benchmark and troubleshooting methodology

Benchmarking is optional and exists to help developers and users troubleshoot or compare configuration changes. SiliconFlow makes no universal performance claim. Label a number **measured** only when the raw source and test conditions are available; otherwise label it **illustrative** or **unverified**.

<p align="center">
  <img src="docs/assets/benchmark-loop.svg" alt="Optional five-step optimization check: record conditions, match workloads, warm up, repeat runs, and report distributions" width="100%">
</p>

*Benchmark discipline at a glance. The loop describes method, not an expected result.*

1. **Record conditions.** Mac model and M-chip generation, RAM, macOS version, Java vendor and architecture, Minecraft/Fabric versions, mod versions, shader state, refresh rate, render/simulation distance, and JVM arguments.
2. **Match the workload.** Create profiles that differ only by SiliconFlow being present or absent. Keep Sodium, Iris, shader pack, resource pack, world, seed, camera route, settings, and background applications identical. If optional mods are tested, keep the same set in both runs.
3. **Warm up and repeat.** Warm up for five minutes, then repeat the same route for at least five runs. Use the F8 overlay or recorder only when optional diagnostic evidence is useful; retain raw output for every run when enabled.
4. **Compare distributions.** Report median and p95/p99 frame time, sample count, run duration, and notable spikes. Treat FPS as a secondary summary, not proof of causality.
5. **Publish the evidence.** Keep the configuration, mod list, and test notes beside any aggregate. Do not infer GPU utilization, VRAM use, or core placement from values SiliconFlow cannot measure.

For practical Apple-Silicon troubleshooting, see [`docs/max-fps-checklist.md`](docs/max-fps-checklist.md) and [`docs/stutter-research.md`](docs/stutter-research.md).

## Troubleshooting first checks

| Symptom | First checks |
| --- | --- |
| Overlay does not appear | Confirm the exact 1.21.4 artifact, launch once, press **F8**, then check the log for mixin or dependency errors. |
| Frequent long freezes | Use native aarch64 Java, watch macOS Memory Pressure, reduce render distance, and avoid oversized heaps on 8 GB systems. |
| Chunk-loading hitching with Sodium | Test Sodium’s macOS **Chunk Memory Allocator = `SWAP`** and compare against a documented baseline. |
| Shaders behave differently | Record the exact Iris version, shader pack, driver state, and settings; shader runs are separate and not guaranteed. |
| An optional diagnostic says unavailable | This is expected for signals SiliconFlow does not instrument, including GPU utilization, VRAM, and exact core placement. |
| A configuration change worsens behavior | Restore the previous file or reset only when an upgrade note calls for it; then retest one variable at a time. |

When reporting an issue, include exact Minecraft/Fabric/Java versions, Mac model and RAM, the full mod list, active profile, configuration, log, and optional diagnostic output only if recording was enabled.

## Beta status and limitations

- **Beta:** APIs, profiles, labels, and behavior can change.
- **Exact target:** the currently verified build is for Minecraft 1.21.4 only.
- **Optional tools and integrations:** F8 diagnostics, recording, QoS requests, and companion integrations are best-effort; diagnostics may report unavailable data.
- **OS authority:** macOS controls scheduling, memory pressure, swap, and core placement.
- **Integration boundaries:** Sodium, Iris, shaders, resource packs, drivers, launchers, and other mods can change behavior.
- **Visual trade-offs:** optional reductions can alter particles, overlays, glints, clouds, weather, or distant-entity presentation.
- **No automatic magic:** choose settings deliberately and validate changes against a repeatable workload.

## Build and Prism deployment

```bash
./gradlew build --console=plain -Pprism_instance=1.21.11
```

This compiles the exact `1.21.4` target and deploys the remapped artifact to the named Prism instance. The resulting JAR is written to `build/libs/`. Deployment to a differently versioned instance is not compatibility evidence.

## Support

If SiliconFlow is useful, support continued development here:

<p align="center">
  <a href="https://ko-fi.com/chackrahunter"><img src="https://img.shields.io/badge/Support_on-Ko--fi-FF5E5B?style=for-the-badge&logo=kofi&logoColor=white" alt="Support on Ko-fi"></a>
  &nbsp;
  <a href="https://www.paypal.me/Donsko2007"><img src="https://img.shields.io/badge/Donate_with-PayPal-00457C?style=for-the-badge&logo=paypal&logoColor=white" alt="Donate with PayPal"></a>
</p>

- [Open an issue](https://github.com/chackrahunter/siliconflow/issues) with your exact setup.
- [Browse releases](https://github.com/chackrahunter/siliconflow/releases).
- [Support on Ko-fi](https://ko-fi.com/chackrahunter) or [donate with PayPal](https://www.paypal.me/Donsko2007).

## License

MIT — see [`LICENSE`](LICENSE).
