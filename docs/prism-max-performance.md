# Prism — max sustained performance (Apple M3, 8 GB)

Exact steps for Fabric Minecraft with **Sodium / Lithium / FerriteCore** + **m3-frametime**.  
Aligned with [`stutter-research.md`](stutter-research.md). Goal: **max sustained FPS without SSD swap** — not a huge heap.

**Never** set `-Xmx4G` / `4096M` on this machine.

---

## 0. Before launch (OS)

1. **Plug in.** System Settings → Battery → **Low Power Mode = Off**.
2. Prefer **macOS Game Mode** when the game is focused (menu-bar game controller).
3. **Quit** Cursor, Chrome/Electron, Discord HW overlays, CleanShot, etc. Watch Activity Monitor → Memory → **Memory Pressure** (aim green).
4. Keep **≥20% SSD free** so swap + APFS aren’t fighting.
5. Optional but strong: Spotlight Privacy → exclude the Prism **instance** folder (or at least `saves/`).

---

## 1. Java (mandatory)

1. Instance → **Settings → Java**.
2. Select **Java 21** JDK built for **`aarch64` / Apple** (Temurin, Zulu, or Microsoft).
3. Confirm architecture: Activity Monitor → Java → kind **Apple** (not Intel / Rosetta).
4. Paste JVM args from one profile below into **JVM arguments** (replace Prism’s default `-Xmx` soup; do **not** mix Aikar G1 flags with ZGC).

### Dedicated MAX (everything else quit) — `jvm/m3-8gb.vmoptions`

Preferred ceiling for 8 GB unified memory (≤3G; SoftMax keeps the working set under Xmx):

```text
-Xms1536M -Xmx2560M -XX:SoftMaxHeapSize=2048M -XX:+UseZGC -XX:+ZGenerational -XX:+AlwaysPreTouch -XX:+DisableExplicitGC -XX:+PerfDisableSharedMem
```

### Shared (Cursor / browser still open) — `jvm/m3-8gb-shared.vmoptions`

Leave headroom for Electron + Metal — prefer quitting apps instead:

```text
-Xms1024M -Xmx2048M -XX:SoftMaxHeapSize=1536M -XX:+UseZGC -XX:+ZGenerational -XX:+AlwaysPreTouch -XX:+DisableExplicitGC -XX:+PerfDisableSharedMem
```

### Pressure yellow at 2560 — fallback `jvm/m3-8gb-3584.vmoptions`

Filename is historical; heap is **2048M**, not 3584 — still never 4G:

```text
-Xms1024M -Xmx2048M -XX:SoftMaxHeapSize=1536M -XX:+UseZGC -XX:+ZGenerational -XX:+AlwaysPreTouch
```

If still swapping → research baseline **`-Xmx2G -Xms1G`**. Still never **4G**.

### Optional G1 — `jvm/m3-8gb-g1.vmoptions`

Same **2560M** heap if ZGC feels worse on your JDK build. Do not combine with ZGC flags.

---

## 2. Replace the companion mod + wipe old config

1. Open the instance → **Folder** → `mods/`.
2. **Delete** every old `m3-frametime-*.jar`.
3. Copy in the current build JAR (e.g. `build/libs/m3-frametime-1.0.1+1.21.4.jar`).
4. Open `config/` and **delete** `m3-frametime.json` (required on upgrade — old files keep soft culls or `pacingEnabled: true` ≈ ~10 FPS).
5. Mod order (typical): Fabric API → Sodium → Lithium → FerriteCore → ImmediatelyFast (optional) → **m3-frametime**.  
   **Do not** add heavy Iris shader packs for “max FPS” on M-series (unsupported; see below).

---

## 3. Sodium (macOS-critical)

1. Launch once → **Options → Video Settings → Sodium → Advanced**.
2. Set **Chunk Memory Allocator = `SWAP`** (not `ASYNC`).  
   Default `ASYNC` causes severe chunk hitching on Apple Silicon GL→Metal ([sodium#1063](https://github.com/CaffeineMC/sodium-fabric/issues/1063)).
3. Optional **Sodium Extra**: enable **Reduce Resolution on macOS**, then **fully restart** the game (cuts Retina ~4× pixel cost).

---

## 4. Video / pacing (vanilla + Sodium)

| Setting | Recommendation |
|--------|----------------|
| Render distance | **8–12** (PLAYABLE: user-owned; emergency may dip to 4 temporarily then restore) |
| Simulation distance | **≤ render** (often **6–8**) |
| Graphics / clouds / particles | Fancy off / clouds off while diagnosing |
| VSync | Often **broken / doubled** vs panel rate on macOS GLFW. Prefer **VSync OFF** + **Max Framerate** = panel rate (60) or panel−3 (e.g. 57) for steadier delivery |
| ProMotion | For hitch tests: System Settings → Displays → fix **60 Hz** |
| Uncapped FPS | VSync off + unlimited max FPS is fine for peak FPS; for *feel*, cap to panel rate |

---

## 5. Iris / shaders (caution)

Iris marks **ARM / M-series macOS as unsupported**. Expect stutter, flushes, and crashes with heavy packs.

- Prefer **no shaders** for max stability / FPS on this Mac.
- If you insist: FPS **≤ 60**, try CPU render-ahead **0**, light packs only, low shadow distance.

---

## 6. Quick verify after changes

| Check | Expect |
|--------|--------|
| JVM arch | Apple / aarch64 |
| Heap | **≤3072M**, never 4G; Pressure **green** |
| Sodium allocator | **SWAP** |
| Old config gone | Fresh `m3-frametime.json` with `performanceProfile=PLAYABLE`, pacing off, RD user-owned |
| Apps | Cursor/Chrome quit while measuring |
| Frame feel | Fewer multi-100ms freezes when crossing chunks |

Change **one** variable at a time. Details and sources: [`stutter-research.md`](stutter-research.md). Kurz: [`max-fps-checklist.md`](max-fps-checklist.md).
