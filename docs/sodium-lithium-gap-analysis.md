# Sodium / Lithium / FerriteCore / ImmediatelyFast — gap analysis for m3-frametime

Public-source reverse pass for **Minecraft 1.21.x Fabric** (verified against CaffeineMC / RaphiMC / malte0811 docs + local Prism jars: Sodium `0.6.13+mc1.21.4`, Lithium `0.15.3+mc1.21.4`, FerriteCore `7.1.3`, ImmediatelyFast `1.8.7+1.21.4`). Goal: own leftovers those mods leave on **macOS Apple Silicon (esp. 8GB M3)**, without fighting their systems.

Pacing stays **OFF**. Sources: GitHub READMEs, Lithium mixin-config.md, ImmediatelyFast README, jar package listings, [`stutter-research.md`](stutter-research.md).

---

## Owned by Sodium

Client **rendering engine replacement** — do not re-mesh, re-cull terrain, or re-drive GL state.

| Area | What Sodium owns |
|------|------------------|
| **Terrain meshing** | Async chunk build tasks, pooled level slices, section meshes per pass |
| **Chunk render** | Region/section graph, draw batches, upload limits, translucency sort / quad split |
| **Occlusion / frustum (terrain)** | Graph / octree culling, visible section lists |
| **GL / buffers** | Custom vertex formats, buffer arenas, tessellation, device state |
| **Fog / sky integration** | Hooks into world render fog path (with Iris caveats) |
| **Block models / quads** | Baked quad paths, block colors, FRAPI renderer |
| **Animated textures** | *Animate only visible textures* (tracking + upload/mipmap opts) |
| **Clouds (partial)** | Immediate cloud renderer replacement when enabled |
| **Particles (partial)** | Particle render path opts in newer builds — **not** spawn budgets |
| **Window / context** | Context creation workarounds, some Window/Minecraft mixins |
| **macOS note** | Not officially “modern GL 4.5”; chunk allocator must be **`SWAP`** on Apple Silicon (see stutter-research) |

Sodium does **not** own: entity distance policy, HUD element deletion, weather strip skip, sound distance, limb animation throttle, enchantment glint policy, lightmap cadence, Retina/HiDPI policy, JVM/GC, or frame pacing.

**M3 note (1.0.7):** Sodium auto `chunk_builder_threads=0` resolves to `clamp(max(cores/3, cores−6), 1, 10)` — often **~2** on 8-core M3. m3-frametime soft-boosts to **cores−1** (reflection + `sodium-options.json`) so P-cores actually mesh; does not rewrite the mesher.

---

## Owned by Lithium

**Game-logic** optimizations (client + integrated server + dedicated). Behavior-preserving.

| Area | What Lithium owns |
|------|-------------------|
| **Mob AI** | Goal/brain/task/sensor/POI/pathing stream replacements |
| **Physics / collisions** | Entity–block/entity collision, fluid, movement, shapes |
| **Entity ticking data** | Data tracker arrays, equipment tracking, class groups, retrieval |
| **Block / redstone / hoppers** | Wire power, hopper inventory caches, fluid flow |
| **Tick scheduling** | Scheduled ticks O(1), chunk random ticks, block-entity sleeping |
| **Alloc / collections** | Hot-path allocation cuts, entity iteration, NBT, etc. |
| **Experimental client tick** | Skip unused client ambient sounds, water-supply, brains; biome particle chance order |

Lithium does **not** own: GPU fill-rate, entity *render* distance, HUD overlays, weather geometry, item glint, lightmap uploads, GLFW/VSync, or Retina.

---

## Owned by FerriteCore

**Memory structure** dedup (client + server) — shrinks heap / GC pressure, not FPS directly.

- Blockstate neighbor **FastMap** + property map replacement  
- Baked-quad / multipart predicate dedup  
- Model-side / voxel-shape related caching  
- Data-component related memory cuts (newer versions)

**Do not** re-implement blockstate maps or model predicate caches.

---

## Owned by ImmediatelyFast

**Immediate-mode** batching + targeted client render opts (stacks with Sodium).

| Area | Ownership |
|------|-----------|
| Entity / BE / particle **draw batching** | Custom immediate buffers |
| Text / GUI / HUD batching | `hud_batching`, font atlas, fast text lookup |
| Maps | Map atlas generation |
| Signs (experimental) | Sign text buffering |
| **Apple GPU** | `fast_buffer_upload` / Apple-oriented buffer upload path |

**Do not** replace VertexConsumerProvider.Immediate, HUD batching, map atlases, or sign text buffers.

---

## Still free for m3-frametime (prioritized mixin targets)

Leftovers after Sodium + Lithium + FerriteCore + ImmediatelyFast, ranked for **felt hitch / GPU fill on 8GB M3**. Soft `require = 0`, Sodium-aware where noted.

### P0 — implement / keep (high leftover value)

| Priority | Target (Yarn 1.21.4) | Why free | Notes |
|----------|----------------------|----------|-------|
| **1** | `ItemStack.hasGlint` + `ItemRenderer.getItemGlintConsumer` / `getArmorGlintConsumer` | Nobody skips foil multipass | Big Metal fill win; additive visual tradeoff |
| **2** | `LightmapTextureManager.update` | Sodium doesn’t cadence lightmap | Soft every-other-frame when no NV/darkness/water |
| **3** | Entity **distance** cull (`EntityRenderDispatcher.shouldRender`) | Sodium frustum ≠ distance policy | Distance-only when Sodium present (`StackCompat`) |
| **4** | Particle **spawn budget** + far spawn skip | IF/Sodium batch draws; Lithium only biome chance | Already: `ParticleManager` / `ClientWorld` |
| **5** | Weather particles + `renderWeather` geometry | Sodium keeps weather path | Already: `WorldRenderer` / `WeatherRendering` |
| **6** | Far limb `LivingEntity.updateLimbs` (client-only) | Lithium ticks logic, not limb anim | Already: `LivingEntityLimbMixin` |
| **7** | Far positional `SoundSystem.play` | Lithium skips *unused ambient attempts*; not distance policy | Already: `SoundSystemMixin` |
| **8** | HUD / overlay skips (vignette, nausea, scoreboard, underwater, bob/hurt) | IF batches HUD; doesn’t delete elements | Already: HUD / overlay / `GameRenderer` mixins |
| **9** | Clouds / stars / border / beacon beams | Optional Sodium clouds ≠ our skip policy | Already present |
| **10** | Block-entity **distance** render skip | Sodium lists BEs; distance leftover | Already: `BlockEntityRenderDispatcher` |

### P1 — strong candidates (not yet / careful)

| Priority | Target | Why free | Risk |
|----------|--------|----------|------|
| **11** | Retina / backing scale (`Window` / GLFW cocoa) | Stack doesn’t own HiDPI policy | Prefer Sodium Extra reduce-res when present |
| **12** | View/sim distance clamp + FAST graphics nudge | Options only | Don’t touch Sodium chunk allocator |
| **13** | Memory-pressure / GC / spike telemetry | Unique to companion | Measure-only OK |
| **14** | Skip fire overlay / portal overlays | Fill-rate | Fire = gameplay cue — opt-in only |
| **15** | Client chunk **light** updates cadence | Not Lithium’s server light | Easy to desync; soft only |
| **16** | Experience orb / item-entity render throttle | Distance cull covers most | Avoid fighting EntityCulling mod if present |
| **17** | Worker QoS / tiny pool | Amplify Sodium meshing cores | Keep `StackCompat.preferredWorkerThreads` |

### P2 — low / situational

- Chat / tab-list density, toast skip (have), boss bar (opt-in)  
- Fishing line / leash / painting extras  
- Status-effect icon density (IF already batches)  
- Autosave I/O / Spotlight — OS/docs, not mixins  

### Vanilla macOS waste the stack still leaves

1. **OpenGL→Metal** translation + buffer flushes (allocator `SWAP` is config, not ours)  
2. **Retina 2×** framebuffer (~4× pixels)  
3. **Unified memory / swap** (heap sizing — docs/JVM, not mixins)  
4. **Broken VSync / ProMotion** (GLFW) — our swapInterval hooks only; pacing OFF  
5. **Enchantment glint multipass** (P0 #1)  
6. **Per-frame lightmap rebuild/upload** (P0 #2)  
7. **Weather / HUD / far entities** fill and CPU (our existing soft skips)

---

## Do-not-touch (will conflict)

| System | Why |
|--------|-----|
| Chunk meshing / section rebuild / upload scheduler | Sodium core |
| Terrain occlusion graph / render lists | Sodium |
| Sodium fog / cloud renderer internals | Sodium |
| Animated texture visibility tracking / upload | Sodium `features/textures` |
| Entity AI, collisions, tick scheduler, BE sleeping | Lithium |
| Lithium experimental `client_tick.*` ambient/brain/biome-particle | Overlap risk — stay at distance/budget layer |
| Blockstate FastMap / model predicate dedup | FerriteCore |
| Immediate buffer impl, HUD/map/sign batching, Apple buffer upload | ImmediatelyFast |
| Iris shader pipeline (if present) | Unsupported on M-series; don’t patch |
| Frame pacing sleep / busy-wait | Explicitly OFF (historical ~10 FPS bug) |
| Sodium **Chunk Memory Allocator** | User must set `SWAP` on macOS — never override via mixin |

### Soft-compat rules (m3-frametime)

- All injectors: **`defaultRequire: 0`**  
- With Sodium: **no aggressive entity frustum redo** — distance only (`StackCompat.useAggressiveEntityFrustum`)  
- Tiny worker pool when Sodium loaded  
- Skip `SoundSystemMixin` if Sound Physics / Simple Voice Chat present (`M3MixinPlugin`)  
- Prefer additive cancels (skip draw / skip spawn) over replacing algorithms  

---

## Implemented (1.0.4)

### Gaps filled
- `ItemStackMixin` + `ItemRendererGlintMixin` — `skipItemGlint`  
- `LightmapTextureManagerMixin` — `lightmapThrottle`  
- `LivingEntityPotionSwirlMixin` — `farPotionSwirlSkip`  
- Existing particle/BE/entity-distance/weather/HUD/sound/limb soft skips  
- **`RamDiscipline`**: soft cache hints (ScratchPool + particle trim, never `System.gc`), auto emergency under `MemoryPressureProbe`  
- Soft mipmap / view / sim clamps (emergency tightens further)  

### Overlaps improved (amplify, not mesh rewrite)
- `overrideSodiumEntityCull` (PLAYABLE/MAX): distance + cheap AABB frustum early-out with Sodium  
- PLAYABLE particle budget (**192** / **40m**; emergency **48** / **20m**) vs Sodium/IF render-only opts  
- HUD element *deletion* optional (MAX); PLAYABLE keeps clouds/weather/stars/portals/effects  
- Client-only limb + potion swirl skips (Lithium owns tick *logic*)
- PLAYABLE: **no hard RD clamp** (user Video setting); emergency view≤**4** temporary + restore; MAX soft ceiling ≤**16**
