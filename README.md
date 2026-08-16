<div align="center">

# SiliconFlow

### Frame-time telemetry and optional render-side controls for Apple Silicon Macs

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

<p><strong>See what your client is doing. Change only what you choose.</strong></p>

</div>

> [!IMPORTANT]
> **Beta software, exact target:** this repository currently builds one version-specific Fabric artifact for **Minecraft 1.21.4**. It is not a universal JAR and it does not promise a particular FPS, frame time, GPU result, or shader result.

![Minecraft gameplay with the genuine SiliconFlow F8 telemetry overlay](docs/assets/hud_preview.png)

*The screenshot above is a real Minecraft capture preserved from the project. It illustrates the overlay UI, not a benchmark result. Some labels shown in the older capture may differ from the current implementation.*

## At a glance

SiliconFlow is a client-side companion for Apple Silicon Macs running Fabric Minecraft. Its center of gravity is **diagnostics**: frame-time samples, spike context, memory/GC probes, active profile state, and an optional recorder. It also offers conservative, opt-in render/entity/particle/UI reductions for workloads where the active profile calls for them.

It is designed to complement—not replace—specialized mods such as Sodium, Lithium, FerriteCore, ImmediatelyFast, and Iris.

| What you get | What you do not get |
| --- | --- |
| In-game **F8** telemetry overlay | A universal compatibility layer |
| Optional JSON snapshots at `m3-live-telemetry.json` | GPU utilization or VRAM from SiliconFlow's own probes |
| Frame-time, spike, GC, memory-pressure, and runtime signals | Exact P-core/E-core placement or real-time scheduling |
| Profiles for playable, balanced, maximum, and telemetry-oriented use | A guaranteed performance improvement |
| Best-effort Darwin QoS requests | Shader compatibility or shader performance guarantees |

## How the pieces fit

The schematic below describes data flow and ownership. It is a product diagram, not a measured performance chart.

<img src="docs/assets/architecture.svg" alt="Schematic showing Minecraft client signals flowing into SiliconFlow telemetry, optional controls, and the F8 overlay or JSON recorder" width="100%">

<details>
<summary><strong>Architecture notes</strong></summary>

- **Signals:** frame intervals, spikes, GC activity, heap and physical-memory probes, runtime flags, and compatibility state.
- **Decisions:** the active profile applies bounded, configurable policies; optional integrations remain best-effort.
- **Outputs:** the F8 overlay is for live inspection; the optional recorder is for retaining raw diagnostic context.
- **Ownership:** macOS still schedules threads and chooses core placement. Sodium still owns terrain rendering; Iris still owns shader pipelines.

</details>

## Features

### Inspect frame delivery, not just FPS

Press **F8** to toggle the overlay. It reports values the client can actually observe, including frame-time samples, derived FPS, memory/GC probes, active profile state, and recent spike information. Unavailable values are omitted or marked unavailable rather than invented.

### Record a reproducible diagnostic snapshot

When enabled, the recorder keeps a bounded in-memory window and can write periodic JSON diagnostics to:

```text
<instance>/m3-live-telemetry.json
```

Keep the raw file together with the matching configuration and test notes. It is diagnostic output, not a standardized benchmark format.

### Use conservative, configurable controls

The implementation can apply soft reductions around systems common performance mods do not fully own: distant entities and block entities, particle budgets, selected overlays, clouds/weather extras, item glint, lightmap cadence, and related client-side work. Policies are profile-driven and intentionally do not attempt to replace Sodium's terrain engine or Iris's shader pipeline.

### Understand the limits of Apple Silicon scheduling

SiliconFlow can request Darwin QoS or thread-priority changes where supported. These are **best effort**. They are not CPU affinity, P-core locking, or real-time priority, and macOS remains in control.

## Exact compatibility

The current artifact is compiled for this dependency set:

| Component | Supported target |
| --- | --- |
| Minecraft | **1.21.4 only** |
| Yarn mappings | `1.21.4+build.8` |
| Fabric API | `0.119.4+1.21.4` |
| Fabric Loader | `0.16.14` |
| Java | `21+`; native Apple/aarch64 is recommended on Apple Silicon |
| Platform | Fabric client on macOS / Apple Silicon |

A runtime version detector can report whether the client matches the target; it cannot make bytecode, mappings, or Mixin descriptors portable across releases. See [`docs/compatibility.md`](docs/compatibility.md) and [`docs/versions/README.md`](docs/versions/README.md).

### Optional companion mods

SiliconFlow can coexist with Sodium, Lithium, FerriteCore, ImmediatelyFast, and Iris. The exact tested stack documented in the repository includes Sodium `0.6.13`, Iris `1.8.8`, C2ME `0.3.2`, Lithium `0.15.3`, FerriteCore `7.1.3`, ImmediatelyFast `1.8.7`, ModernFix `5.20.3`, MoreCulling `1.2.10`, Sodium Extra `0.6.1`, Cloth Config, Fabric API, and their dependencies.

That list describes a compatibility test environment—not a promise that every version, shader pack, or combination behaves identically. Treat Iris/shader runs as a separate workload.

## Benchmark methodology

SiliconFlow intentionally makes no universal performance claim. If you publish a number, label it **measured** only when the raw source and test conditions are available. Otherwise label it **illustrative** or **unverified**.

1. Record the Mac model, RAM, macOS version, Java vendor and architecture, Minecraft/Fabric versions, mod versions, shader state, display refresh rate, render/simulation distance, and JVM arguments.
2. Create two profiles that differ only by SiliconFlow being present or absent. Use the same world, seed, camera route, resource pack, settings, and background applications.
3. Warm up for five minutes, then repeat the same route for at least five runs. Keep raw F8/recorder output for every run.
4. Compare median and p95/p99 frame time, sample count, run duration, and notable spikes. Treat FPS as a secondary summary—not the result itself.
5. Publish raw telemetry, configuration, and test notes beside any aggregate. Do not infer GPU utilization, VRAM use, or core placement from values SiliconFlow cannot measure.

For Apple Silicon troubleshooting, start with [`docs/max-fps-checklist.md`](docs/max-fps-checklist.md) and the deeper [`docs/stutter-research.md`](docs/stutter-research.md).

## Installation

1. Install **Minecraft 1.21.4**, Fabric Loader `0.16.14`, Fabric API `0.119.4`, and Java 21 or newer.
2. Install the release JAR built for **1.21.4** into the instance's `mods/` directory.
3. Add optional companion mods only when their exact versions match the workload you are testing.
4. Launch the game and press **F8** to verify the overlay.
5. For diagnostics, enable recording in the generated configuration and retain `m3-live-telemetry.json` with your test conditions.

Do not install this artifact into another Minecraft release and treat a successful file copy as compatibility evidence. A new release requires a separately compiled and smoke-tested target.

## Configuration and profiles

The configuration is stored at:

```text
<instance>/config/m3-frametime.json
```

The default profile is `PLAYABLE`. Other supported profiles are `BALANCED`, `MAX`, and `TELEMETRY`. Useful controls include the F8 overlay, bounded performance recording, spike threshold, optional spike logging, render-thread/Darwin QoS requests, and swap-interval behavior. Keep the file when troubleshooting; reset it only when release notes or the implementation require a migration.

The longer guides cover practical setup:

- [`docs/max-fps-checklist.md`](docs/max-fps-checklist.md) — concise Apple Silicon checklist.
- [`docs/prism-max-performance.md`](docs/prism-max-performance.md) — conservative Prism guidance for 8 GB systems.
- [`docs/sodium-lithium-gap-analysis.md`](docs/sodium-lithium-gap-analysis.md) — boundaries with common performance mods.
- [`docs/stutter-research.md`](docs/stutter-research.md) — causes, evidence, and reproducible troubleshooting notes.

## Troubleshooting

| Symptom | First checks |
| --- | --- |
| Overlay does not appear | Confirm the exact 1.21.4 artifact is installed, launch once, then press **F8**. Check the log for mixin or dependency errors. |
| Frequent long freezes | Use native aarch64 Java, watch macOS Memory Pressure, reduce render distance, and avoid oversized heaps on 8 GB systems. |
| Chunk-loading hitching with Sodium | Test Sodium's macOS **Chunk Memory Allocator = `SWAP`** and compare against a controlled baseline. |
| Shaders behave differently | Record the exact Iris version, shader pack, driver state, and settings; shader runs are a separate workload and are not guaranteed. |
| Telemetry says unavailable | This is expected for signals SiliconFlow does not instrument, including GPU utilization, VRAM, and exact core placement. |
| A config change makes behavior worse | Restore the previous file or remove only the obsolete config when an upgrade note explicitly calls for it; then retest one variable at a time. |

When reporting an issue, include the exact Minecraft/Fabric/Java versions, Mac model and RAM, full mod list, active profile, configuration, log, and raw telemetry if recording was enabled.

## Limitations and responsible interpretation

- **Beta:** APIs, profiles, labels, and behavior can change.
- **Exact target:** the current build is for Minecraft 1.21.4 only.
- **No guarantee:** telemetry is diagnostic; it does not guarantee higher FPS, lower frame time, or fewer stutters.
- **OS authority:** macOS controls scheduling, memory pressure, swap, and core placement.
- **Integration boundaries:** Sodium, Lithium, FerriteCore, ImmediatelyFast, Iris, resource packs, shaders, drivers, and launchers can change behavior.
- **Visual trade-offs:** optional reductions can alter particle, overlay, glint, cloud, or distant-entity presentation.

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
- [Browse discussions and releases](https://github.com/chackrahunter/siliconflow/releases).
- [Support on Ko-fi](https://ko-fi.com/chackrahunter) or [donate with PayPal](https://www.paypal.me/Donsko2007).

## License

MIT — see [`LICENSE`](LICENSE).
