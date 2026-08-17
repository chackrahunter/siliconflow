package dev.doncalvin.m3frametime.telemetry;

import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive Diagnostic Error & Stutter Code Catalog for Apple Silicon M3 (236+ Granular Codes).
 * Spans 9 distinct hardware and software subsystems for pinpoint diagnostics.
 */
public enum StutterErrorCode {
	// 0. SYSTEM STATUS OK
	OK_000("OK-000", "Measured Stable", "No recent frame-time spike or quality threshold breach", 0x3FB950),

	FRAME_001("FRAME-001", "Frame-Time Spike", "Measured frame exceeded the configured spike threshold; cause is not instrumented", 0xF85149),

	// 1. JAVA GARBAGE COLLECTION & JVM HEAP (GC-001 .. GC-030)
	GC_001("GC-001", "ZGC Major Pause", "ZGC collection pause duration exceeded 10ms", 0xF85149),
	GC_002("GC-002", "GC Allocation Spike", "Rapid heap object creation causing sudden GC cycle", 0xD29922),
	GC_003("GC-003", "ZGC Relocation Stall", "ZGC memory relocation worker lock wait", 0xF85149),
	GC_004("GC-004", "ZGC Mark Phase Delay", "Concurrent mark phase backlog in JVM memory", 0xD29922),
	GC_005("GC-005", "ZGC Allocation Stall", "Thread blocked waiting for available memory pages", 0xF85149),
	GC_006("GC-006", "Root Scanning Lag", "Thread stack and native handle root scan stall", 0xD29922),
	GC_007("GC-007", "Reference Processing Lag", "Weak and soft reference cleanup queue backlog", 0xD29922),
	GC_008("GC-008", "Metaspace Allocation Stall", "JVM class metadata space expansion stall", 0xF85149),
	GC_009("GC-009", "Safepoint Sync Wait", "Threads taking long to reach JVM safepoint", 0xF85149),
	GC_010("GC-010", "Safepoint Cleanup Stall", "JVM safepoint state cleanup time spike", 0xD29922),
	GC_011("GC-011", "G1 Evacuation Failure", "G1 young generation object evacuation failed", 0xF85149),
	GC_012("GC-012", "Humongous Allocation Burst", "Large object arrays bypassing Eden space", 0xD29922),
	GC_013("GC-013", "Old Gen Promotion Burst", "High volume of survivor objects promoted to Old Gen", 0xD29922),
	GC_014("GC-014", "Dirty Card Queue Flood", "Post-write barrier card queue saturation", 0xD29922),
	GC_015("GC-015", "SATB Buffer Overflow", "Snapshot-at-the-beginning buffer drain lag", 0xD29922),
	GC_016("GC-016", "Class Unloader Pause", "Unused dynamic class unloading cycle", 0xD29922),
	GC_017("GC-017", "JNI Weak Ref Scan", "Native JNI handle reference table verification", 0xD29922),
	GC_018("GC-018", "Survivor Space Overflow", "Survivor space capacity exceeded threshold", 0xD29922),
	GC_019("GC-019", "Eden Space Thrash", "Excessive short-lived temporary object churn", 0xD29922),
	GC_020("GC-020", "Direct ByteBuffer Low", "Off-heap direct byte buffer limit approaching", 0xF85149),
	GC_021("GC-021", "Direct Buffer Leak", "Unreleased direct memory buffers", 0xF85149),
	GC_022("GC-022", "Finalizer Queue Backlog", "Object finalization backlog stalling GC", 0xD29922),
	GC_023("GC-023", "Unmapper Thread Lag", "OS virtual memory page unmapping delay", 0xD29922),
	GC_024("GC-024", "NMT Tracking Overhead", "Native memory tracking diagnostic overhead", 0x8B949E),
	GC_025("GC-025", "GC Ergonomics Thrash", "Automatic JVM heap boundary resizing oscillation", 0xD29922),
	GC_026("GC-026", "JVMTI Hook Stall", "JVM Tool Interface event listener delay", 0x8B949E),
	GC_027("GC-027", "CodeCache Compaction", "JIT compiled code cache memory defragmentation", 0xD29922),
	GC_028("GC-028", "JIT Deoptimization Burst", "Dynamic branch misprediction deopt cascade", 0xD29922),
	GC_029("GC-029", "Thread Stack Expansion", "JVM thread stack memory page allocation", 0x8B949E),
	GC_030("GC-030", "Heap Expansion Stall", "OS physical RAM allocation for JVM heap commit", 0xF85149),

	// 2. APPLE SILICON UNIFIED MEMORY & MACOS VM (MEM-001 .. MEM-025)
	MEM_001("MEM-001", "Critical Low RAM", "Free physical Unified RAM < 64 MB (severe paging)", 0xF85149),
	MEM_002("MEM-002", "Warning Low RAM", "Free physical Unified RAM < 128 MB (paging imminent)", 0xF85149),
	MEM_003("MEM-003", "Moderate Low RAM", "Free physical Unified RAM < 256 MB", 0xD29922),
	MEM_004("MEM-004", "Swapfile Activation", "macOS swapfile creation on NVMe SSD", 0xF85149),
	MEM_005("MEM-005", "Memory Compressor Peak", "macOS memory compressor CPU saturation", 0xD29922),
	MEM_006("MEM-006", "Dirty Page Flush", "macOS VM dirty page writeout stall", 0xD29922),
	MEM_007("MEM-007", "Unified Bus Contention", "High CPU and GPU simultaneous RAM access", 0xD29922),
	MEM_008("MEM-008", "CPU-GPU Interleave Stall", "CPU-GPU shared memory synchronization delay", 0xD29922),
	MEM_009("MEM-009", "Metal Resource Evict", "Metal driver evicting textures from Unified RAM", 0xF85149),
	MEM_010("MEM-010", "Purgeable RAM Release", "Purgeable memory release cleanup cycle", 0x8B949E),
	MEM_011("MEM-011", "Mach Pressure Level 2", "macOS Mach kernel memory pressure critical", 0xF85149),
	MEM_012("MEM-012", "Mach Pressure Level 1", "macOS Mach kernel memory pressure warning", 0xD29922),
	MEM_013("MEM-013", "VM Sleep/Wake Hitch", "macOS power management memory state restore", 0x8B949E),
	MEM_014("MEM-014", "Anonymous RAM Burst", "Sudden expansion of anonymous dirty pages", 0xD29922),
	MEM_015("MEM-015", "File Page Thrashing", "Disk cache page eviction and re-read storm", 0xD29922),
	MEM_016("MEM-016", "Kernel Wired RAM High", "macOS kernel wired unpageable memory spike", 0xD29922),
	MEM_017("MEM-017", "Shared Cache Eviction", "macOS dyld shared cache paging delay", 0x8B949E),
	MEM_018("MEM-018", "Dynamic Linker Rebind", "Runtime native symbol resolution latency", 0x8B949E),
	MEM_019("MEM-019", "MMAP Fault Stall", "Memory-mapped file page fault latency", 0xD29922),
	MEM_020("MEM-020", "COW Fork Latency", "Copy-On-Write page table duplication stall", 0x8B949E),
	MEM_021("MEM-021", "Page Zero Fill Delay", "Kernel zero-filling newly allocated RAM pages", 0xD29922),
	MEM_022("MEM-022", "TLB Shootdown Stall", "ARM64 Translation Lookaside Buffer shootdown", 0x8B949E),
	MEM_023("MEM-023", "IPC Pipe Backpressure", "Inter-process memory pipe buffer full", 0x8B949E),
	MEM_024("MEM-024", "CoreAnimation Tile Evict", "macOS window server surface reallocation", 0xD29922),
	MEM_025("MEM-025", "Metal Command Backlog", "Metal command buffer queue memory saturation", 0xF85149),

	// 3. GPU, METAL TBDR & SHADERS (GPU-001 .. GPU-040)
	GPU_001("GPU-001", "Shadow Cascade 0 Overdraw", "Iris shadow cascade 0 rendering excess geometry", 0xF2CC60),
	GPU_002("GPU-002", "Shadow Cascade 1 Overdraw", "Iris shadow cascade 1 rendering excess geometry", 0xF2CC60),
	GPU_003("GPU-003", "Shadow Cascade 2 Overdraw", "Iris shadow cascade 2 distant geometry overload", 0xF2CC60),
	GPU_004("GPU-004", "Shadow Map Resolution Stall", "High shadow resolution GPU fillrate overload", 0xF85149),
	GPU_005("GPU-005", "Normal Map Fragment Stall", "PBR surface normal mapping calculation surge", 0xF2CC60),
	GPU_006("GPU-006", "Specular PBR Compute", "Roughness and specular metallic shader passes", 0xF2CC60),
	GPU_007("GPU-007", "Atmospheric Scatter Stall", "Volumetric sky Rayleigh and Mie scattering load", 0xF2CC60),
	GPU_008("GPU-008", "Volumetric Fog Step Overload", "Raymarching fog samples exceeded budget", 0xF85149),
	GPU_009("GPU-009", "SSR Reflection Raycast Stall", "Screen Space Reflection ray steps overload", 0xF85149),
	GPU_010("GPU-010", "POM Parallax Step Burst", "Parallax Occlusion Mapping depth iterations", 0xF85149),
	GPU_011("GPU-011", "Bloom Blur Downsample Stall", "Multi-pass bloom Gaussian blur downsample", 0xF2CC60),
	GPU_012("GPU-012", "Depth of Field Bokeh Stall", "Circular Bokeh blur pixel overdraw", 0xF2CC60),
	GPU_013("GPU-013", "Motion Blur Vector Stall", "Velocity buffer motion blur reconstruction", 0xF2CC60),
	GPU_014("GPU-014", "Chromatic Aberration Stall", "RGB split screen post-processing pass", 0x8B949E),
	GPU_015("GPU-015", "HDR Tonemap LUT Stall", "HDR 3D LUT color grading interpolation", 0x8B949E),
	GPU_016("GPU-016", "TBDR Tile Buffer Spill", "Apple Metal tile memory spill to system DRAM", 0xF85149),
	GPU_017("GPU-017", "Depth Pre-pass Thrash", "Early-Z depth buffer pipeline re-priming", 0xF2CC60),
	GPU_018("GPU-018", "Alpha Discard Stall", "Fragment shader discard instructions in foliage", 0xF2CC60),
	GPU_019("GPU-019", "Translucent Sort Stall", "Water and glass depth sorting backpressure", 0xF2CC60),
	GPU_020("GPU-020", "Vertex Upload Stall", "Dynamic vertex array buffer upload delay", 0xD29922),
	GPU_021("GPU-021", "Uniform Buffer Ring Full", "Shader uniform parameter ring buffer wait", 0xD29922),
	GPU_022("GPU-022", "Metal Translation Barrier", "OpenGL to Metal translation pipeline fence", 0xD29922),
	GPU_023("GPU-023", "Shader Compile Stutter", "Runtime GLSL to Metal MSL shader compile", 0xF85149),
	GPU_024("GPU-024", "Shader Program Link Stall", "Metal shader pipeline state creation delay", 0xF85149),
	GPU_025("GPU-025", "Texture Binding Thrash", "Excessive OpenGL texture atlas binds in frame", 0xD29922),
	GPU_026("GPU-026", "Mipmap Gen Hiccup", "Dynamic texture mipmap generation delay", 0x8B949E),
	GPU_027("GPU-027", "Anisotropic Filter Stall", "16x AF texture sampling memory bandwidth", 0xF2CC60),
	GPU_028("GPU-028", "GL Framebuffer Blit Wait", "Offscreen framebuffer blit to display queue", 0xD29922),
	GPU_029("GPU-029", "Swapchain Present Latency", "Metal drawable presentation queue full", 0xD29922),
	GPU_030("GPU-030", "Metal Fence Sync Delay", "GPU command buffer execution fence wait", 0xD29922),
	GPU_031("GPU-031", "Shadow Culling Queue Full", "Entity shadow frustum culler backlog", 0xF2CC60),
	GPU_032("GPU-032", "Lightmap Sub-Image Stall", "16x16 vanilla lightmap texture upload stall", 0xD29922),
	GPU_033("GPU-033", "Dynamic Light SSBO Compute", "Shader storage buffer object compute stall", 0xF2CC60),
	GPU_034("GPU-034", "Indirect Draw Validation", "Multi-draw indirect command buffer validation", 0x8B949E),
	GPU_035("GPU-035", "Render Dimension Mismatch", "Dynamic viewport resize framebuffer reset", 0x8B949E),
	GPU_036("GPU-036", "MSAA Resolve Stall", "Multisample antialiasing resolve bandwidth", 0xF85149),
	GPU_037("GPU-037", "Stencil Clear Latency", "Screen stencil buffer clear pipeline stall", 0x8B949E),
	GPU_038("GPU-038", "Primitive Vertex Bottleneck", "High triangle count geometry submission", 0xF2CC60),
	GPU_039("GPU-039", "Rasterizer Tile Saturation", "Metal on-chip rasterizer tile full", 0xF85149),
	GPU_040("GPU-040", "GPU Thermal Throttle", "Apple M3 GPU thermal throttling downclock", 0xF85149),

	// 4. CHUNK GENERATION & MESHING / SODIUM (CHK-001 .. CHK-030)
	CHK_001("CHK-001", "Chunk Queue Saturation", "Sodium chunk builder task backlog", 0x79C0FF),
	CHK_002("CHK-002", "Mesher Memory Spike", "Chunk tessellator memory allocation surge", 0x79C0FF),
	CHK_003("CHK-003", "Section Priority Thrash", "Camera movement causing chunk rebuild thrash", 0x79C0FF),
	CHK_004("CHK-004", "Chunk Traversal Stall", "Breadth-first chunk visibility graph search", 0x79C0FF),
	CHK_005("CHK-005", "Frustum Occlusion Lag", "Sodium chunk occlusion buffer lookup delay", 0x79C0FF),
	CHK_006("CHK-006", "Chunk Worker Contention", "Sodium meshing threads competing for locks", 0x79C0FF),
	CHK_007("CHK-007", "VBO Staging Upload Stall", "Chunk VBO memory transfer to Metal buffer", 0x79C0FF),
	CHK_008("CHK-008", "Palette Decode Latency", "Chunk section block state palette decompression", 0x79C0FF),
	CHK_009("CHK-009", "Fluid Tessellation Stall", "Water/lava surface mesh generation latency", 0x79C0FF),
	CHK_010("CHK-010", "Biome Blend Color Interp", "Foliage and grass color blend calculation", 0x79C0FF),
	CHK_011("CHK-011", "Block Model Resolution", "Complex multipart JSON block model lookup", 0x79C0FF),
	CHK_012("CHK-012", "Smooth Light Recalc", "Ambient occlusion smooth lighting calculation", 0x79C0FF),
	CHK_013("CHK-013", "Animated Texture Tick", "Water/lava/fire animated texture buffer tick", 0x79C0FF),
	CHK_014("CHK-014", "Chunk Unloader Surge", "Unloading distant chunks memory deallocation", 0x79C0FF),
	CHK_015("CHK-015", "Dirty Section Flood", "Explosions or block updates dirtying chunks", 0x79C0FF),
	CHK_016("CHK-016", "C2ME Thread Starvation", "C2ME worldgen threads competing for cores", 0x79C0FF),
	CHK_017("CHK-017", "Async Chunk IO Stall", "Disk read latency for chunk region MCA files", 0xD29922),
	CHK_018("CHK-018", "Anvil Region Sync", "Region file header write synchronization", 0xD29922),
	CHK_019("CHK-019", "NBT Deserialization", "Chunk NBT compound tag parsing latency", 0x79C0FF),
	CHK_020("CHK-020", "Heightmap Update Wave", "Surface block changes updating heightmaps", 0x79C0FF),
	CHK_021("CHK-021", "Light Engine Batch Lag", "Starlight/Vanilla light engine propagation", 0x79C0FF),
	CHK_022("CHK-022", "Skylight Column Recalc", "Translucent block column skylight trace", 0x79C0FF),
	CHK_023("CHK-023", "Block Light Flood", "Lava or torch block light propagation flood", 0x79C0FF),
	CHK_024("CHK-024", "Chunk Boundary Hitch", "Neighboring chunk boundary mesh synchronization", 0x79C0FF),
	CHK_025("CHK-025", "Connected Texture Lookup", "Connected block texture state evaluation", 0x79C0FF),
	CHK_026("CHK-026", "LOD Mesh Simplify", "Distant chunk LOD mesh simplification", 0x79C0FF),
	CHK_027("CHK-027", "Chunk Render Sort", "Translucent section render order sorting", 0x79C0FF),
	CHK_028("CHK-028", "Empty Section Skip Fail", "Air section bounding box calculation delay", 0x8B949E),
	CHK_029("CHK-029", "Chunk Cache Miss", "L2 CPU cache line miss in chunk block storage", 0x8B949E),
	CHK_030("CHK-030", "Sub-chunk Re-indexing", "Section coordinate spatial re-indexing stall", 0x8B949E),

	// 5. ENTITY & MOB AI / PHYSICS (ENT-001 .. ENT-030)
	ENT_001("ENT-001", "Entity Count Surge", "Over 200 active entities in local chunk radius", 0xD2A8FF),
	ENT_002("ENT-002", "Pathfinding A* Stall", "Complex mob navigation pathfinding calculation", 0xD2A8FF),
	ENT_003("ENT-003", "AABB Collision Broadcast", "Dense entity crowding collision calculation", 0xD2A8FF),
	ENT_004("ENT-004", "Tick Interpolation Lag", "Client-side entity position interpolation lag", 0xD2A8FF),
	ENT_005("ENT-005", "Limb Trigonometry Burst", "Mob walking animation trigonometric angle eval", 0xD2A8FF),
	ENT_006("ENT-006", "Item Frame Matrix Burst", "Dense storage room item frame matrix updates", 0xD2A8FF),
	ENT_007("ENT-007", "Armor Stand Equipment", "Multi-layer armor stand equipment transform", 0xD2A8FF),
	ENT_008("ENT-008", "Dropped Item Sweep", "Dense ground item voxel collision raycast", 0xD2A8FF),
	ENT_009("ENT-009", "XP Orb Merge Contention", "Experience orb magnetic attraction physics", 0xD2A8FF),
	ENT_010("ENT-010", "Villager Brain POI Query", "Villager schedule and workstation scan", 0xD2A8FF),
	ENT_011("ENT-011", "Monster Target Sensor", "Hostile mob player proximity sensor broadcast", 0xD2A8FF),
	ENT_012("ENT-012", "Pet Pathing Recursion", "Tamed wolf/cat follow owner path recursion", 0xD2A8FF),
	ENT_013("ENT-013", "Projectile Ballistics", "Arrow/trident/fireball voxel trajectory trace", 0xD2A8FF),
	ENT_014("ENT-014", "Entity Tracker Packet", "Server/client entity metadata sync packet flood", 0xD2A8FF),
	ENT_015("ENT-015", "Leash Spline Tessellate", "Lead ribbon bezier spline geometry tessellation", 0x8B949E),
	ENT_016("ENT-016", "Mob Equipment Multi-pass", "Layered armor/elytra multi-pass draw calls", 0xD2A8FF),
	ENT_017("ENT-017", "Dragon Matrix Burst", "Ender dragon multi-part hitbox transform", 0xD2A8FF),
	ENT_018("ENT-018", "Warden Sonic Beam Trace", "Warden sonic boom raycast entity penetration", 0xD2A8FF),
	ENT_019("ENT-019", "Slime Collision Burst", "Large slime jump and land bounding box resize", 0xD2A8FF),
	ENT_020("ENT-020", "Shulker Bullet Homing", "Shulker missile vector guidance calculation", 0xD2A8FF),
	ENT_021("ENT-021", "Bat Ambient Flight", "Bat random flight vector obstacle collision", 0x8B949E),
	ENT_022("ENT-022", "Beehive Path Navigation", "Bee flower gathering and hive entry search", 0x8B949E),
	ENT_023("ENT-023", "Golem Attack Sweep", "Iron golem swing attack bounding box sweep", 0xD2A8FF),
	ENT_024("ENT-024", "Passenger Hierarchy", "Entities riding entities nested matrix stack", 0xD2A8FF),
	ENT_025("ENT-025", "Nametag Glyph Miss", "Custom entity nametag font glyph rasterization", 0x8B949E),
	ENT_026("ENT-026", "Shadow Disk Projection", "Entity circular shadow terrain projection", 0x8B949E),
	ENT_027("ENT-027", "Glowing Outline Pass", "Spectral arrow glowing outline shader pass", 0xF2CC60),
	ENT_028("ENT-028", "Potion Swirl Emitter", "Mob potion effect swirl particle generation", 0x8B949E),
	ENT_029("ENT-029", "Death Explosion Particles", "Mob death animation smoke particle explosion", 0x8B949E),
	ENT_030("ENT-030", "Entity Step Audio Loop", "Dense herd footstep sound dispatch loop", 0x8B949E),

	// 6. BLOCK ENTITIES & WORLD STRUCTURES (BLK-001 .. BLK-025)
	BLK_001("BLK-001", "Chest Lid Matrix", "Dense chest room lid opening angle matrix", 0xD2A8FF),
	BLK_002("BLK-002", "End Gateway Beam", "End gateway beam ray projection geometry", 0xD2A8FF),
	BLK_003("BLK-003", "Beacon Light Beam", "Beacon sky beam segment tessellation", 0xD2A8FF),
	BLK_004("BLK-004", "Enchanting Book Trig", "Enchanting table floating book animation", 0x8B949E),
	BLK_005("BLK-005", "Banner Pattern Layer", "Multi-layer banner pattern texture composite", 0xD2A8FF),
	BLK_006("BLK-006", "Sign Text Rasterize", "Sign text font renderer glyph generation", 0xD2A8FF),
	BLK_007("BLK-007", "Bell Vibration Physics", "Ringing bell physics oscillation transform", 0x8B949E),
	BLK_008("BLK-008", "Decorated Pot Lookup", "Sherd pattern texture lookup and composite", 0x8B949E),
	BLK_009("BLK-009", "Campfire Smoke Spawn", "Campfire particle spawn and drift update", 0x8B949E),
	BLK_010("BLK-010", "Piston Moving Block", "Piston extension moving block entity sweep", 0xD2A8FF),
	BLK_011("BLK-011", "Conduit Eye Orbit", "Conduit frame activation and eye particle orbit", 0x8B949E),
	BLK_012("BLK-012", "Spawner Mini-Model", "Mob spawner rotating miniature entity model", 0xD2A8FF),
	BLK_013("BLK-013", "Daylight Sensor Scan", "Daylight detector skylight level query", 0x8B949E),
	BLK_014("BLK-014", "Hopper Item Transfer", "Dense hopper chain inventory transfer check", 0xD2A8FF),
	BLK_015("BLK-015", "Dispenser Block Scan", "Dispenser target block facing verification", 0x8B949E),
	BLK_016("BLK-016", "Redstone Signal Wave", "Complex redstone wire signal propagation wave", 0xD2A8FF),
	BLK_017("BLK-017", "Comparator Update Queue", "Comparator container fullness calculation", 0xD2A8FF),
	BLK_018("BLK-018", "Observer Pulse Cascade", "Chained observer block state update pulse", 0xD2A8FF),
	BLK_019("BLK-019", "Sculk Sensor Vibration", "Sculk sensor acoustic frequency raycast", 0xD2A8FF),
	BLK_020("BLK-020", "Sculk Shrieker Warning", "Sculk shrieker sound wave particle trace", 0x8B949E),
	BLK_021("BLK-021", "Crafter Automated Tick", "Automated crafter recipe validation tick", 0xD2A8FF),
	BLK_022("BLK-022", "Trial Spawner Wave", "Trial chamber mob spawner wave trigger", 0xD2A8FF),
	BLK_023("BLK-023", "Structure Block Box", "Structure block bounding box outline render", 0x8B949E),
	BLK_024("BLK-024", "Jigsaw Connector Check", "Jigsaw block target connector validation", 0x8B949E),
	BLK_025("BLK-025", "Block Entity Client Sync", "Block entity NBT data sync packet decode", 0x8B949E),

	// 7. PARTICLES & VISUAL EFFECTS (PRT-001 .. PRT-020)
	PRT_001("PRT-001", "Explosion Particle Storm", "Large TNT/Creeper explosion particle burst", 0xF85149),
	PRT_002("PRT-002", "Campfire Smoke Queue", "High density campfire smoke particle queue", 0x8B949E),
	PRT_003("PRT-003", "Potion Bubble Flood", "Area of effect cloud potion particle flood", 0xD29922),
	PRT_004("PRT-004", "Dripping Voxel Raycast", "Water/lava drip block collision trace", 0x8B949E),
	PRT_005("PRT-005", "Spell Particle Surge", "Evoker/witch magic spell particle surge", 0x8B949E),
	PRT_006("PRT-006", "Critical Hit Velocity", "Weapon critical hit sharp particle spread", 0x8B949E),
	PRT_007("PRT-007", "Torch Flame Billboard", "Dense torch placement billboard matrix updates", 0x8B949E),
	PRT_008("PRT-008", "Portal Particle Wave", "Nether portal purple swirl particle wave", 0x8B949E),
	PRT_009("PRT-009", "Spore Blossom Drift", "Spore blossom ambient pollen particle drift", 0x8B949E),
	PRT_010("PRT-010", "Rain Splash Collision", "Heavy rain storm ground particle collisions", 0xD29922),
	PRT_011("PRT-011", "Texture Sheet Switch", "Particle renderer switching texture sheets", 0x8B949E),
	PRT_012("PRT-012", "Particle Cull Exceeded", "Particle count exceeded budget threshold", 0xD29922),
	PRT_013("PRT-013", "Particle Age Trim Lag", "Iterating dead particles in large queues", 0x8B949E),
	PRT_014("PRT-014", "Sub-tick Physics Step", "Particle physics step collision resolution", 0x8B949E),
	PRT_015("PRT-015", "Snow Geometry Mesh", "Weather snow falling geometry mesh generation", 0x8B949E),
	PRT_016("PRT-016", "Rain Splash Audio Spawn", "Rain droplet audio event spawn flood", 0x8B949E),
	PRT_017("PRT-017", "Lightning Segment Vertex", "Lightning bolt branching segment vertices", 0xF2CC60),
	PRT_018("PRT-018", "3D Cloud Cube Mesh", "Custom 3D volumetric cloud mesh build", 0x8B949E),
	PRT_019("PRT-019", "Void Particle Trigger", "Bottom of world void particle trigger scan", 0x8B949E),
	PRT_020("PRT-020", "Shader Particle Light", "Shader illuminated particle lighting pass", 0xF2CC60),

	// 8. AUDIO & OPENAL ENGINE (SND-001 .. SND-015)
	SND_001("SND-001", "OpenAL Channel Exhaust", "OpenAL hardware sound channels full (>64)", 0xF85149),
	SND_002("SND-002", "Far Sound Cull Bypass", "Distant sound events bypassing distance cull", 0x8B949E),
	SND_003("SND-003", "Footstep Rate Limit", "Rapid duplicate footstep audio events", 0x8B949E),
	SND_004("SND-004", "Vorbis Decode Latency", "Ogg Vorbis audio file decompression stall", 0xD29922),
	SND_005("SND-005", "Music Buffer Underrun", "Streaming background music buffer underrun", 0x8B949E),
	SND_006("SND-006", "Underwater Audio Filter", "Underwater low-pass DSP filter calculation", 0x8B949E),
	SND_007("SND-007", "Listener Position Lag", "Audio listener coordinate transformation lag", 0x8B949E),
	SND_008("SND-008", "Biome Audio Fade Lag", "Ambient biome background sound crossfade", 0x8B949E),
	SND_009("SND-009", "Attenuation Curve Eval", "3D sound distance attenuation curve calculation", 0x8B949E),
	SND_010("SND-010", "UI Sound Queue Burst", "Rapid inventory click sound dispatch burst", 0x8B949E),
	SND_011("SND-011", "Audio Lock Contention", "SoundSystem thread synchronization lock wait", 0xD29922),
	SND_012("SND-012", "OpenAL Context Switch", "OpenAL output device context state change", 0x8B949E),
	SND_013("SND-013", "Category Volume Recalc", "Master sound volume slider interpolation", 0x8B949E),
	SND_014("SND-014", "Delayed Sound Backlog", "Scheduled delayed sound task queue backlog", 0x8B949E),
	SND_015("SND-015", "Sound Pool Starvation", "Pooled sound buffer allocator starvation", 0xD29922),

	// 9. MACOS DARWIN KERNEL, MACH QOS & SYSTEM (SYS-001 .. SYS-020)
	SYS_001("SYS-001", "Darwin QoS Demotion", "Render thread migrated away from P-Cores to E-Cores", 0xF85149),
	SYS_002("SYS-002", "CPU Thermal Throttle", "Apple M3 chip thermal throttling clock frequency", 0xF85149),
	SYS_003("SYS-003", "E-Core Migration Lag", "macOS scheduler thread migration latency", 0xF85149),
	SYS_004("SYS-004", "ForkJoinPool Starve", "ForkJoinPool common pool threads saturated", 0xD29922),
	SYS_005("SYS-005", "WindowServer VSync Slip", "macOS WindowServer display presentation slip", 0xD29922),
	SYS_006("SYS-006", "CoreGraphics Latch Wait", "CoreGraphics surface buffer latch latency", 0xD29922),
	SYS_007("SYS-007", "Retina Blit Mismatch", "Retina non-integer scale framebuffer blit lag", 0xD29922),
	SYS_008("SYS-008", "GLFW Event Poll Latency", "GLFW window event queue poll processing stall", 0x8B949E),
	SYS_009("SYS-009", "HID Input Queue Burst", "High polling rate mouse input event queue burst", 0x8B949E),
	SYS_010("SYS-010", "Fullscreen Mode Switch", "macOS space/fullscreen transition stall", 0xD29922),
	SYS_011("SYS-011", "Priority Inversion", "Mach thread priority inheritance inversion", 0xF85149),
	SYS_012("SYS-012", "JNA Native Binding Lag", "JNA native Darwin Mach library binding delay", 0x8B949E),
	SYS_013("SYS-013", "JVM Safepoint Pause", "Global JVM stop-the-world safepoint sync", 0xF85149),
	SYS_014("SYS-014", "CPU Governor Frequency", "Apple Silicon dynamic voltage/frequency step", 0x8B949E),
	SYS_015("SYS-015", "App Nap Throttling", "macOS App Nap attempting background suspension", 0xF85149),
	SYS_016("SYS-016", "Spotlight IO Spike", "macOS Spotlight / TimeMachine background disk IO", 0xD29922),
	SYS_017("SYS-017", "Low Battery Throttle", "macOS Low Power Mode CPU clock cap active", 0xF85149),
	SYS_018("SYS-018", "Display Hz Mismatch", "External ProMotion variable refresh rate slip", 0xD29922),
	SYS_019("SYS-019", "Metal Present Wait", "Render thread waiting on nextDrawable availability", 0xD29922),
	SYS_020("SYS-020", "OS Notification Burst", "macOS system notification overlay interrupt", 0x8B949E);

	private static final Map<String, StutterErrorCode> CODE_MAP = new HashMap<>();

	static {
		for (StutterErrorCode code : values()) {
			CODE_MAP.put(code.code, code);
		}
	}

	private final String code;
	private final String title;
	private final String description;
	private final int color;

	StutterErrorCode(String code, String title, String description, int color) {
		this.code = code;
		this.title = title;
		this.description = description;
		this.color = color;
	}

	public String getCode() {
		return code;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public int getColor() {
		return color;
	}

	public static int totalCount() {
		return values().length;
	}

	public static StutterErrorCode fromCode(String code) {
		return CODE_MAP.getOrDefault(code, OK_000);
	}

	public static StutterErrorCode fromSpike(
		long deltaNanos,
		GcProbe gc,
		MemoryPressureProbe mem,
		SpikeScope.Phase phase,
		boolean isShaderActive,
		boolean isShadowPass
	) {
		long deltaMs = deltaNanos / 1_000_000L;

		// 1. Check GC pauses
		if (gc != null && gc.frameGcDeltaMs() >= 10) {
			return GC_001; // observed collector pause
		}
		if (gc != null && gc.frameGcCountDelta() > 0 && deltaMs > 15) {
			return GC_002; // observed collection during spike
		}

		// 2. Check Unified RAM & Heap pressure
		if (mem != null) {
			long freePhysicalMb = mem.freePhysicalMb();
			if (freePhysicalMb >= 0L && freePhysicalMb < 64) {
				return MEM_001; // Critical Low RAM
			}
			if (freePhysicalMb >= 0L && freePhysicalMb < 128) {
				return MEM_002; // Warning Low RAM
			}
			if (mem.heapMaxMb() > 0 && (mem.heapUsedMb() * 100 / mem.heapMaxMb()) > 92) {
				return GC_030; // observed heap expansion pressure
			}
		}

		// No concrete subsystem cause was measured. Never report a spike as OK-000.
		return FRAME_001;

	}
}
