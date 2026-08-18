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

**Runtime note:** Sodium worker sizing may be adjusted conservatively when the integration is active. The operating system still controls scheduling and core placement; the mod does not lock workers or the render thread to P-cores.

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
| **Apple-oriented upload path** | `fast_buffer_upload` / platform-specific buffer handling |

**Do not** replace VertexConsumerProvider.Immediate, HUD batching, map atlases, or sign text buffers.

---

## Leftovers after the stack (ownership)

Policy leftovers after Sodium + Lithium + FerriteCore + ImmediatelyFast, ranked for **felt hitch / GPU fill on 8GB M3**. SiliconFlow owns **distance / skip / budget policy** on these paths — not Sodium meshing, Lithium AI, ImmediatelyFast batching, or Iris shader internals. Exact-target injections, `defaultRequire: 0`, Sodium-aware where noted. Pacing stays **OFF**.

### P0 — implemented (registered in `m3frametime.client.mixins.json`)

These mixins exist and are wired to `M3Config` flags. They are skip/distance/budget overlays, not replacements of the owning mods.

| Area | Mixin(s) | Policy flag(s) | Why free vs the stack |
|------|----------|----------------|------------------------|
| Enchantment glint | `ItemStackMixin`, `ItemRendererGlintMixin` | `skipItemGlint` | Nobody skips foil multipass |
| Lightmap cadence | `LightmapTextureManagerMixin` | `lightmapThrottle` | Sodium doesn’t cadence lightmap; skipped under NV/darkness/water |
| Entity **distance** cull | `EntityRenderDispatcherMixin` | `entityCull`, `entityCullDistance`, `optimizeShadowPass` | Sodium frustum ≠ distance policy; distance-only when Sodium present unless `overrideSodiumEntityCull` |
| Particle spawn budget + far skip | `ParticleManagerMixin`, `ClientWorldMixin` | `particleCull`, `maxParticles`, `farParticleSpawnSkip`, `ambientParticleThrottle` | IF/Sodium batch draws; Lithium only biome chance |
| Weather particles + geometry | `WeatherRenderingMixin`, `WorldRendererSkipMixin` | `skipWeatherParticles`, `skipWeatherGeometry` | Sodium keeps the weather path |
| Far limb anim (client-only) | `LivingEntityLimbMixin` | `farLimbThrottle` | Lithium ticks logic, not limb anim |
| Far positional sound | `SoundSystemMixin` | `farSoundSkip` | Lithium skips unused ambient *attempts*; not distance policy. Plugin skips if Sound Physics / Voice Chat loaded |
| HUD / overlay skips | `InGameHudMixin`, `InGameOverlayRendererMixin`, `GameRendererMixin` | vignette, nausea, scoreboard, portal, status-effect, underwater, fire, bob, hurt, floating item | IF batches HUD; doesn’t delete elements |
| Clouds / stars / border / beacon | `CloudRendererMixin`, `WorldRendererSkipMixin`, `SkyRenderingMixin`, `WorldBorderRenderingMixin`, `BeaconBlockEntityRendererMixin` | `skipClouds`, `skipStars`, `skipWorldBorder`, `skipBeaconBeams` | Optional Sodium clouds ≠ skip policy |
| Block-entity **distance** skip | `BlockEntityRenderDispatcherMixin` | `blockEntityCull` | Sodium lists BEs; distance leftover |

Also registered (same policy layer, not P0 leftovers): nametags/leashes (`EntityRendererMixin`, `LivingEntityRendererMixin`), potion swirl (`LivingEntityPotionSwirlMixin`), far item/orb render (`ItemEntityRendererMixin`, `ExperienceOrbEntityRendererMixin`), far sign text (`SignTextMixin`), far banner patterns (`BannerPatternMixin`), toasts / subtitles / boss bar (`ToastManagerMixin`, `SubtitlesHudMixin`, `BossBarHudMixin`). Accessors: `ParticleManagerAccessor`, `ScreenAccessor`. HUD chrome: `VanillaDebugHudMixin`, `OptionsScreenMixin`. Loop: `MinecraftClientMixin` (EMA sample; pacing gated and off).

### P1 — not mixins yet / careful

| Target | Why still open | Notes |
|--------|----------------|-------|
| Retina / backing scale (`Window` / GLFW cocoa) | Stack doesn’t own HiDPI policy | Prefer Sodium Extra reduce-res when present |
| View/sim distance clamp | Options / user Video setting | Don’t touch Sodium chunk allocator |
| FAST graphics nudge | `forceFastGraphics` exists on config | No mixin or other caller yet |
| Client chunk **light** updates cadence | Not Lithium’s server light | Easy to desync; no mixin yet |

Non-mixin companion work (not gap mixins): memory-pressure / GC probes (`RamDiscipline`, `MemoryPressureProbe`), worker QoS / tiny pool (`DarwinQos`, `StackCompat.preferredWorkerThreads`). Fire overlay mixin exists (`skipFireOverlay`) but stays **opt-in** (gameplay cue; profiles leave it false).

### P2 — low / situational (no extra mixins)

- Chat / tab-list density
- Fishing line / painting extras (leashes already have `skipLeashes`)
- Status-effect icon density (IF already batches; overlay skip is separate)
- Autosave I/O / Spotlight — OS/docs, not mixins

### Vanilla macOS waste the stack still leaves

1. **OpenGL→Metal** translation + buffer flushes (allocator `SWAP` is config, not ours)
2. **Retina 2×** framebuffer (~4× pixels) — no Window mixin
3. **Unified memory / swap** (heap sizing — docs/JVM, not mixins)
4. **Broken VSync / ProMotion** (GLFW) — `swapInterval` hooks only; pacing OFF
5. Glint / lightmap / weather / HUD / far-entity fill — **policy mixins above**, not a Sodium/Lithium replacement

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
| Iris shader pipeline (if present) | Treat as a separate compatibility workload; do not patch shader internals |
| Frame pacing sleep / busy-wait | Explicitly OFF (historical ~10 FPS bug) |
| Sodium **Chunk Memory Allocator** | User must set `SWAP` on macOS — never override via mixin |

### Soft-compat rules (m3-frametime)

- All injectors: **`defaultRequire: 0`**
- With Sodium: **no aggressive entity frustum redo** — distance only (`StackCompat.useAggressiveEntityFrustum`)
- Tiny worker pool when Sodium loaded
- Skip `SoundSystemMixin` if Sound Physics / Simple Voice Chat present (`M3MixinPlugin`)
- Prefer additive cancels (skip draw / skip spawn) over replacing algorithms

---

## Current SiliconFlow ownership (honest)

**Owns:** config-gated skip / distance / spawn-budget overlays on the P0 (and listed extra) mixins; `RamDiscipline` scratch + particle-trim hints (never `System.gc()`); optional shader auto-throttle of *our* flags; F8 overlay / dashboard / recorder.

**Does not own:** Sodium terrain/occlusion/GL, Lithium AI/collision/ticks, FerriteCore maps, ImmediatelyFast buffers, Iris shader pipeline, macOS scheduling, or frame-pacing sleep (explicitly OFF).

**Does not claim:** FPS, GPU utilization, VRAM, or that mixins are “already done” for targets that have no class in `m3frametime.client.mixins.json` (Retina, chunk-light cadence).
