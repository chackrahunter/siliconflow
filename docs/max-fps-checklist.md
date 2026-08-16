# Max-FPS checklist (8 GB Apple Silicon)

This checklist is a compact starting point for reducing frame-time spikes. It is not a guarantee of a particular FPS or frametime.

## Before launch

- [ ] Plug in the Mac; disable Low Power Mode.
- [ ] Close unnecessary browser/Electron apps and keep Activity Monitor Memory Pressure green.
- [ ] Keep at least 20% free SSD space.

## Java and JVM

- [ ] Use a native Apple/aarch64 Java 21 runtime.
- [ ] Start with the instance's documented heap profile; on an 8 GB Mac, avoid treating `-Xmx4G` as a default.
- [ ] Change one JVM variable at a time and record it with the test run.

## Mods and configuration

- [ ] Install the exact `siliconflow-<version>+1.21.4.jar` produced for Minecraft 1.21.4.
- [ ] Keep Fabric API, Sodium, Lithium, FerriteCore, ImmediatelyFast, C2ME, ModernFix, MoreCulling, Sodium Extra, Iris, Cloth Config, and other optional mods at the versions being tested.
- [ ] Do not assume a mod stack or shader pack is interchangeable across Minecraft versions.
- [ ] Preserve `config/m3-frametime.json` unless an upgrade note specifically requires resetting it.

## Sodium and video

- [ ] On Apple Silicon, test Sodium's `SWAP` chunk memory allocator where applicable; compare against a documented baseline.
- [ ] If using Sodium Extra's macOS resolution option, restart the game before measuring.
- [ ] Choose render/simulation distance, VSync, and FPS cap deliberately and record them.
- [ ] Treat Iris/shader tests as a separate workload; SiliconFlow does not guarantee shader support or performance.

## In-game verification

- [ ] Press F8 and confirm that frame-time samples and the active profile are visible.
- [ ] If recording is enabled, retain `m3-live-telemetry.json` and the matching configuration.
- [ ] Confirm that unavailable GPU/VRAM/core-placement values are not being interpreted as measurements.
- [ ] Repeat the same route and compare median/p95/p99 frame time, not only peak FPS.
