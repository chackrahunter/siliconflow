# Max-FPS Checkliste (8 GB M3)

Kurz für Don-Calvin. Details: [`prism-max-performance.md`](prism-max-performance.md), [`stutter-research.md`](stutter-research.md).

## Vor dem Start

- [ ] Netzteil dran, **Low Power Mode aus**
- [ ] **Cursor / Chrome / Discord** zu — Memory Pressure **grün**
- [ ] **≥20 % SSD frei**

## Java & JVM

- [ ] **ARM64** JDK 21 (Activity Monitor → Java → **Apple**, kein Rosetta)
- [ ] Args aus `jvm/m3-8gb.vmoptions` (**`-Xmx2560M`**, SoftMax 2048)
- [ ] Mit offenen Apps: `jvm/m3-8gb-shared.vmoptions` (**`-Xmx2048M`**)
- [ ] **Nie** `-Xmx4G` / `4096M`

## Mod & Config

- [ ] Alte `m3-frametime-*.jar` raus, **`m3-frametime-1.0.8+1.21.4.jar`** rein
- [ ] **`config/m3-frametime.json` löschen** (Upgrade! PLAYABLE: RD frei, Wolken/Wetter an)
- [ ] Stack: Fabric API → Sodium → Lithium → FerriteCore → m3-frametime

## Sodium & Video

- [ ] Chunk Memory Allocator = **`SWAP`** (nicht ASYNC)
- [ ] Soft-Boost setzt `chunk_builder_threads` ≈ **cores−1** (Sodium-Auto auf M3 oft nur ~2)
- [ ] Optional: Sodium Extra → Reduce Resolution on macOS → **komplett neu starten**
- [ ] Render **8–12** (frei einstellbar), Sim **6–8**, VSync **aus**, Max FPS **Unlimited** (`swapInterval=0`)
- [ ] Keine schweren Iris-Shader
- [ ] Vollbild → Control Center **Game Mode** (höhere CPU/GPU-Priorität)

## Check im Spiel

- [ ] Log: `ChipPower: renderThread MAX_PRIORITY` + `Sodium worker target=N` + `SodiumSoftBooster: chunk_builder_threads → N`
- [ ] Log: `Started N worker threads` von Sodium mit N ≈ cores−1 (nicht ~2)
- [ ] Video → Render Distance lässt sich über 4 hinaus setzen (ohne Dauer-EMERGENCY)
- [ ] F3: Heap nicht 4G; kein ständiges GC-Sägen; Spike-Panel ohne Dauer-Spikes
- [ ] Chunk-Flug ohne Multi-100ms-Freezes; Activity Monitor: Java nutzt mehrere Kerne produktiv
- [ ] Bei gelbem Pressure: auf shared/`-Xmx2G` runter; Mod zeigt F8 `EMERGENCY` (view→4 temporär, danach Restore)
