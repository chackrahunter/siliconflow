package dev.doncalvin.m3frametime.telemetry;

import dev.doncalvin.m3frametime.engine.ShaderAutoThrottle;
import dev.doncalvin.m3frametime.pool.ParticleTrim;

import java.util.HashMap;
import java.util.Map;

/**
 * Diagnostic stutter-code catalog for Apple Silicon. {@link #totalCount()} is 757:
 * 752 historical entries plus 5 probe-backed codes (MEM-041, GPU-061/062, PRT-036, MOD-026).
 *
 * Categories actually present in this enum:
 *   OK     — System status OK
 *   FRAME  — Generic frame-time spikes
 *   GC     — Java GC & JVM heap (50 codes)
 *   MEM    — Apple Silicon unified memory & macOS VM (41 codes)
 *   GPU    — GPU, Metal TBDR & shaders (62 codes)
 *   CHK    — Chunk generation & meshing / Sodium (50 codes)
 *   ENT    — Entity & mob AI / physics (50 codes)
 *   BLK    — Block entities & world structures (40 codes)
 *   PRT    — Particles & visual effects (36 codes)
 *   SND    — Audio & OpenAL engine (25 codes)
 *   SYS    — macOS Darwin kernel, Mach QoS & system (35 codes)
 *   NET    — Network & packet processing (40 codes)
 *   IO     — Disk I/O & file system (30 codes)
 *   THR    — Threading & concurrency (35 codes)
 *   RND    — Rendering pipeline & draw calls (50 codes)
 *   TEX    — Texture & atlas management (30 codes)
 *   LIT    — Lighting & shadow computation (30 codes)
 *   WLD    — World, dimension & biome (35 codes)
 *   MOD    — Mod compatibility (26 codes)
 *   JIT    — JIT compiler & optimization (20 codes)
 *   VID    — Video settings & display (20 codes)
 *   PHY    — Physics & collision (25 codes)
 *   DAT    — Data, NBT & serialization (25 codes)
 *
 * {@link #fromSpike} only returns codes backed by GcProbe, MemoryPressureProbe,
 * RamDiscipline, Iris shadow latch, ShaderAutoThrottle, ChipPower/Sodium sample,
 * ParticleTrim, or SpikeScope.dominant(). The rest of the catalog is documentation.
 */
public enum StutterErrorCode {
	// ═══════════════════════════════════════════════════════════════
	// 0. SYSTEM STATUS OK
	// ═══════════════════════════════════════════════════════════════
	OK_000("OK-000", "Measured Stable", "No recent frame-time spike or quality threshold breach", 0x3FB950),

	FRAME_001("FRAME-001", "Frame-Time Spike", "Measured frame exceeded the configured spike threshold; cause is not instrumented", 0xF85149),

	// ═══════════════════════════════════════════════════════════════
	// 1. JAVA GARBAGE COLLECTION & JVM HEAP (GC-001 .. GC-050)
	// ═══════════════════════════════════════════════════════════════
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
	GC_031("GC-031", "ZGC Uncommit Delay", "ZGC returning unused memory to OS took too long", 0xD29922),
	GC_032("GC-032", "TLAB Refill Stall", "Thread-local allocation buffer refill contention", 0xD29922),
	GC_033("GC-033", "String Dedup Lag", "G1 string deduplication queue processing delay", 0x8B949E),
	GC_034("GC-034", "Weak Ref Clear Storm", "Mass clearing of weak references in single cycle", 0xD29922),
	GC_035("GC-035", "Phantom Ref Queue", "PhantomReference cleanup queue processing delay", 0x8B949E),
	GC_036("GC-036", "Heap Fragmentation", "Excessive heap fragmentation reducing allocation speed", 0xD29922),
	GC_037("GC-037", "GC Thread Starvation", "GC worker threads delayed by OS scheduling", 0xF85149),
	GC_038("GC-038", "Alloc Rate Ceiling", "Allocation rate exceeding GC throughput capacity", 0xF85149),
	GC_039("GC-039", "Soft Ref Eviction Wave", "Mass soft reference eviction under memory pressure", 0xD29922),
	GC_040("GC-040", "CMS Concurrent Mode Fail", "Concurrent collector fell behind allocation rate", 0xF85149),
	GC_041("GC-041", "Region Exhaust", "All heap regions exhausted before GC completes", 0xF85149),
	GC_042("GC-042", "Mixed GC Backlog", "Mixed collection regions queued beyond threshold", 0xD29922),
	GC_043("GC-043", "Heap Shrink Oscillation", "JVM repeatedly growing and shrinking heap", 0xD29922),
	GC_044("GC-044", "Large Array Alloc", "Single array allocation exceeding 1MB", 0xD29922),
	GC_045("GC-045", "Class Data Sharing Load", "CDS archive memory mapping during runtime", 0x8B949E),
	GC_046("GC-046", "Compressed Oops Limit", "Heap exceeding compressed oops 32GB boundary", 0xD29922),
	GC_047("GC-047", "Biased Lock Revocation", "Mass biased lock revocation at safepoint", 0xD29922),
	GC_048("GC-048", "Thread Park Delay", "LockSupport.park() returning late from OS", 0x8B949E),
	GC_049("GC-049", "GC Log Sync IO", "GC log file write blocking collector thread", 0x8B949E),
	GC_050("GC-050", "Allocation Sampler Hit", "JFR allocation sampler overhead in hot path", 0x8B949E),

	// ═══════════════════════════════════════════════════════════════
	// 2. APPLE SILICON UNIFIED MEMORY & MACOS VM (MEM-001 .. MEM-041)
	// ═══════════════════════════════════════════════════════════════
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
	MEM_026("MEM-026", "Heap >90% Capacity", "JVM heap usage above 90% of maximum", 0xF85149),
	MEM_027("MEM-027", "Heap >80% Capacity", "JVM heap usage above 80% approaching pressure", 0xD29922),
	MEM_028("MEM-028", "Physical RAM <512MB Free", "System has less than 512MB free physical RAM", 0xD29922),
	MEM_029("MEM-029", "Swap Thrashing Detected", "macOS swap read/write rate exceeding threshold", 0xF85149),
	MEM_030("MEM-030", "UMA Bandwidth Saturated", "Unified Memory Architecture bandwidth at capacity", 0xF85149),
	MEM_031("MEM-031", "IOKit Memory Mapping", "IOKit kernel extension memory mapping stall", 0x8B949E),
	MEM_032("MEM-032", "Mach Zone Exhaustion", "Mach kernel memory zone limit reached", 0xF85149),
	MEM_033("MEM-033", "Compressed Page Decompress", "macOS decompressing compressed memory page", 0xD29922),
	MEM_034("MEM-034", "Memory Limit Jetsam", "Process approaching macOS memory jetsam limit", 0xF85149),
	MEM_035("MEM-035", "Shared Region Resize", "macOS shared memory region remapping stall", 0x8B949E),
	MEM_036("MEM-036", "VM Object Coalesce", "Kernel merging adjacent VM objects", 0x8B949E),
	MEM_037("MEM-037", "Page Daemon Active", "macOS page daemon reclaiming memory", 0xD29922),
	MEM_038("MEM-038", "Large Page Promotion", "OS promoting 4KB pages to 16KB superpage", 0x8B949E),
	MEM_039("MEM-039", "Guard Page Fault", "Stack or heap guard page trap", 0x8B949E),
	MEM_040("MEM-040", "Memory Diagnostic Active", "System memory diagnostic tool overhead", 0x8B949E),
	MEM_041("MEM-041", "Heap vs Unified RAM Mismatch", "JVM -Xmx is too large versus physical unified memory (8GB with ~4G heap, or heap ≥ physical)", 0xF85149),

	// ═══════════════════════════════════════════════════════════════
	// 3. GPU, METAL TBDR & SHADERS (GPU-001 .. GPU-062)
	// ═══════════════════════════════════════════════════════════════
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
	GPU_040("GPU-040", "GPU Thermal Throttle", "Apple M-chip GPU thermal throttling downclock", 0xF85149),
	GPU_041("GPU-041", "Shader Variant Explosion", "Too many shader permutations compiled at once", 0xF85149),
	GPU_042("GPU-042", "Tessellation Factor Burst", "Dynamic tessellation level exceeding budget", 0xF2CC60),
	GPU_043("GPU-043", "Compute Shader Dispatch", "Compute shader dispatch exceeding tile buffer", 0xD29922),
	GPU_044("GPU-044", "Render Pass Switch", "Excessive render pass begin/end within single frame", 0xD29922),
	GPU_045("GPU-045", "Color Attachment Load", "Unnecessary color attachment load on render pass", 0x8B949E),
	GPU_046("GPU-046", "Depth Buffer Read-back", "GPU-to-CPU depth buffer readback stall", 0xD29922),
	GPU_047("GPU-047", "Occlusion Query Delay", "Hardware occlusion query result latency", 0xD29922),
	GPU_048("GPU-048", "Pixel Ownership Test", "Window pixel ownership test stall on macOS", 0x8B949E),
	GPU_049("GPU-049", "GPU Memory Defrag", "Metal driver defragmenting GPU memory heap", 0xD29922),
	GPU_050("GPU-050", "Ray Tracing Fallback", "Software ray tracing fallback on non-RT hardware", 0xF85149),
	GPU_051("GPU-051", "SSAO Sample Overload", "Screen Space Ambient Occlusion sample count exceeded", 0xF2CC60),
	GPU_052("GPU-052", "TAA Reprojection Stall", "Temporal Anti-Aliasing history reprojection delay", 0xF2CC60),
	GPU_053("GPU-053", "FXAA Edge Detect", "FXAA edge detection pass taking too long", 0x8B949E),
	GPU_054("GPU-054", "Color Space Convert", "sRGB to linear color space conversion overhead", 0x8B949E),
	GPU_055("GPU-055", "Shader Warmup Stutter", "First-use shader pipeline state compilation", 0xF85149),
	GPU_056("GPU-056", "Global Illumination Bounce", "Path-traced GI bounce calculation exceeded", 0xF2CC60),
	GPU_057("GPU-057", "Caustics Raycast", "Water caustics raycast computation overload", 0xF2CC60),
	GPU_058("GPU-058", "Deferred Lighting Pass", "Deferred lighting G-buffer resolve stall", 0xD29922),
	GPU_059("GPU-059", "Shadow Acne Filter", "Shadow map bias filter artifact correction", 0x8B949E),
	GPU_060("GPU-060", "Texture Streaming Stall", "Virtual texture streaming page fault", 0xD29922),
	GPU_061("GPU-061", "Shader Auto-Throttle L1", "ShaderAutoThrottle session overlay at L1 (cull distances tightened)", 0xD29922),
	GPU_062("GPU-062", "Shader Auto-Throttle L2", "ShaderAutoThrottle session overlay at L2 (shadow/lightmap/particle budgets tightened)", 0xF85149),

	// ═══════════════════════════════════════════════════════════════
	// 4. CHUNK GENERATION & MESHING / SODIUM (CHK-001 .. CHK-050)
	// ═══════════════════════════════════════════════════════════════
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
	CHK_031("CHK-031", "Worldgen Feature Place", "Structure/feature placement during chunk gen", 0x79C0FF),
	CHK_032("CHK-032", "Noise Generator Compute", "Perlin/Simplex noise generation for terrain", 0x79C0FF),
	CHK_033("CHK-033", "Carver Intersection", "Cave carver intersection calculation overload", 0x79C0FF),
	CHK_034("CHK-034", "Surface Builder Apply", "Surface biome material application delay", 0x79C0FF),
	CHK_035("CHK-035", "Structure Bounding Box", "Structure bounding box intersection scan", 0x79C0FF),
	CHK_036("CHK-036", "Chunk Status Promotion", "Chunk status level advancement stall", 0x79C0FF),
	CHK_037("CHK-037", "Chunk Ticket Expire", "Chunk loading ticket expiration processing", 0x8B949E),
	CHK_038("CHK-038", "Force Load Queue Full", "Force-loaded chunk processing backlog", 0x79C0FF),
	CHK_039("CHK-039", "Chunk Data Compression", "Chunk data ZLIB compression for network/save", 0xD29922),
	CHK_040("CHK-040", "Section Palette Resize", "Block state palette dynamic resize operation", 0x79C0FF),
	CHK_041("CHK-041", "Chunk Entity List Scan", "Entity-by-chunk spatial index scan delay", 0x79C0FF),
	CHK_042("CHK-042", "POI Data Update", "Point-of-interest data structure update", 0x8B949E),
	CHK_043("CHK-043", "Blending Chunk Border", "Biome blending at chunk generation border", 0x79C0FF),
	CHK_044("CHK-044", "Chunk Send Rate Limit", "Server chunk send rate throttling client", 0xD29922),
	CHK_045("CHK-045", "Multi-Noise Router", "Multi-noise biome parameter routing overhead", 0x79C0FF),
	CHK_046("CHK-046", "Aquifer Simulation", "Underground aquifer water level simulation", 0x79C0FF),
	CHK_047("CHK-047", "Ore Vein Generation", "Large ore vein feature generation compute", 0x8B949E),
	CHK_048("CHK-048", "Decoration Pass Slow", "Chunk decoration placement pass delay", 0x79C0FF),
	CHK_049("CHK-049", "Chunk Invalidation Flood", "Mass chunk invalidation from world edit", 0xF85149),
	CHK_050("CHK-050", "Render Layer Rebuild", "Complete render layer buffer rebuild trigger", 0x79C0FF),

	// ═══════════════════════════════════════════════════════════════
	// 5. ENTITY & MOB AI / PHYSICS (ENT-001 .. ENT-050)
	// ═══════════════════════════════════════════════════════════════
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
	ENT_031("ENT-031", "Entity Render Distance Cap", "Too many entities at render distance edge", 0xD2A8FF),
	ENT_032("ENT-032", "Mob Spawner Activate", "Nearby spawner activating and creating entities", 0xD2A8FF),
	ENT_033("ENT-033", "TNT Chain Reaction", "Cascading TNT explosion entity chain", 0xF85149),
	ENT_034("ENT-034", "Falling Block Cascade", "Sand/gravel column collapse entity cascade", 0xD2A8FF),
	ENT_035("ENT-035", "Minecart Physics", "Minecart rail physics and switching computation", 0x8B949E),
	ENT_036("ENT-036", "Boat Buoyancy Calc", "Boat water surface buoyancy physics update", 0x8B949E),
	ENT_037("ENT-037", "Enderman Teleport Scan", "Enderman scanning valid teleport destinations", 0xD2A8FF),
	ENT_038("ENT-038", "Wither Skull Barrage", "Multiple wither skull projectile tracking", 0xD2A8FF),
	ENT_039("ENT-039", "Guardian Beam Render", "Guardian laser beam vertex generation", 0xD2A8FF),
	ENT_040("ENT-040", "Phantom Swoop Calc", "Phantom dive attack trajectory calculation", 0x8B949E),
	ENT_041("ENT-041", "Fish School Alignment", "Tropical fish schooling behavior alignment", 0x8B949E),
	ENT_042("ENT-042", "Allay Item Pickup", "Allay item search and pickup pathfinding", 0x8B949E),
	ENT_043("ENT-043", "Breeze Wind Charge", "Breeze wind charge physics simulation", 0xD2A8FF),
	ENT_044("ENT-044", "Sniffer Dig Animation", "Sniffer digging animation state machine", 0x8B949E),
	ENT_045("ENT-045", "Camel Dash Momentum", "Camel dash momentum and cooldown physics", 0x8B949E),
	ENT_046("ENT-046", "Entity Sleep Transition", "Entity sleep/wake state machine transition", 0x8B949E),
	ENT_047("ENT-047", "Mob Goal Selector", "AI goal priority selector evaluation burst", 0xD2A8FF),
	ENT_048("ENT-048", "Entity Data Sync", "Entity tracked data serialization burst", 0xD29922),
	ENT_049("ENT-049", "Raid Wave Spawn", "Village raid wave entity spawning burst", 0xD2A8FF),
	ENT_050("ENT-050", "Entity Culling Overhead", "Entity visibility culling computation cost", 0x8B949E),

	// ═══════════════════════════════════════════════════════════════
	// 6. BLOCK ENTITIES & WORLD STRUCTURES (BLK-001 .. BLK-040)
	// ═══════════════════════════════════════════════════════════════
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
	BLK_026("BLK-026", "Shulker Box Open Anim", "Shulker box opening animation and AABB change", 0x8B949E),
	BLK_027("BLK-027", "Brewing Stand Timer", "Brewing stand potion timer and fuel check", 0x8B949E),
	BLK_028("BLK-028", "Furnace Smelt Tick", "Furnace smelting recipe lookup and progress", 0x8B949E),
	BLK_029("BLK-029", "Lectern Page Render", "Lectern open book text rendering", 0x8B949E),
	BLK_030("BLK-030", "End Crystal Beam", "End crystal ender dragon healing beam render", 0xD2A8FF),
	BLK_031("BLK-031", "Skull Render Skin", "Player skull custom skin texture lookup", 0xD29922),
	BLK_032("BLK-032", "Hanging Sign Chain", "Hanging sign chain geometry tessellation", 0x8B949E),
	BLK_033("BLK-033", "Vault Block Activate", "Vault block activation and particle burst", 0xD2A8FF),
	BLK_034("BLK-034", "Copper Oxidation Tick", "Copper block oxidation state change tick", 0x8B949E),
	BLK_035("BLK-035", "Pointed Dripstone Drip", "Dripstone drip calculation and particle", 0x8B949E),
	BLK_036("BLK-036", "Sculk Spread Growth", "Sculk catalyst spreading block replacement", 0xD2A8FF),
	BLK_037("BLK-037", "Bed Head Render", "Bed head portion block entity render", 0x8B949E),
	BLK_038("BLK-038", "Mob Head Anim", "Mob head note block animation trigger", 0x8B949E),
	BLK_039("BLK-039", "Chiseled Bookshelf", "Chiseled bookshelf book slot render update", 0x8B949E),
	BLK_040("BLK-040", "Suspicious Block Brush", "Suspicious sand/gravel brush progress render", 0x8B949E),

	// ═══════════════════════════════════════════════════════════════
	// 7. PARTICLES & VISUAL EFFECTS (PRT-001 .. PRT-036)
	// ═══════════════════════════════════════════════════════════════
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
	PRT_021("PRT-021", "Cherry Blossom Drift", "Cherry grove petal particle drift physics", 0x8B949E),
	PRT_022("PRT-022", "Sculk Charge Particle", "Sculk charge particle path animation", 0x8B949E),
	PRT_023("PRT-023", "Dust Plume Burst", "Falling block dust plume particle burst", 0x8B949E),
	PRT_024("PRT-024", "Totem Pop Effect", "Totem of undying activation particle explosion", 0xD29922),
	PRT_025("PRT-025", "Enchant Glyph Float", "Enchanting table floating glyph particle", 0x8B949E),
	PRT_026("PRT-026", "Soul Flame Particle", "Soul campfire/torch blue flame particle", 0x8B949E),
	PRT_027("PRT-027", "Wax Particle Spray", "Honeycomb waxing particle spray burst", 0x8B949E),
	PRT_028("PRT-028", "Block Break Fragment", "Block breaking fragment particle shower", 0x8B949E),
	PRT_029("PRT-029", "Lava Pop Particle", "Lava surface random pop particle spawn", 0x8B949E),
	PRT_030("PRT-030", "Ender Pearl Trail", "Ender pearl flight trail particle", 0x8B949E),
	PRT_031("PRT-031", "Firework Explosion", "Firework rocket explosion star particle burst", 0xD29922),
	PRT_032("PRT-032", "Bubble Column Stream", "Bubble column rising/descending stream", 0x8B949E),
	PRT_033("PRT-033", "Dragonbreath Cloud", "Ender dragon breath area effect particles", 0xD2A8FF),
	PRT_034("PRT-034", "Wind Charge Impact", "Wind charge impact area particles burst", 0xD29922),
	PRT_035("PRT-035", "Trial Spawner Smoke", "Trial spawner activation smoke burst", 0x8B949E),
	PRT_036("PRT-036", "Particle Queue Trim", "ParticleTrim ran an aggressive queue trim under RAM PRESSURE", 0xD29922),

	// ═══════════════════════════════════════════════════════════════
	// 8. AUDIO & OPENAL ENGINE (SND-001 .. SND-025)
	// ═══════════════════════════════════════════════════════════════
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
	SND_016("SND-016", "HRTF Processing", "Head-related transfer function audio processing", 0x8B949E),
	SND_017("SND-017", "Sound Reverb Calc", "Environmental reverb zone calculation", 0x8B949E),
	SND_018("SND-018", "Audio Thread Starved", "Audio processing thread CPU-starved by main", 0xD29922),
	SND_019("SND-019", "Sound Event Registry", "Sound event registry lookup for unknown event", 0x8B949E),
	SND_020("SND-020", "Audio Device Lost", "System audio output device disconnected", 0xF85149),
	SND_021("SND-021", "Streaming Chunk Load", "Loading next streaming audio data chunk", 0x8B949E),
	SND_022("SND-022", "Sound Priority Sort", "Sound priority sorting when channels exhausted", 0x8B949E),
	SND_023("SND-023", "Doppler Effect Calc", "3D sound doppler shift calculation", 0x8B949E),
	SND_024("SND-024", "Audio Format Convert", "Audio sample rate or format conversion", 0x8B949E),
	SND_025("SND-025", "AirPods Latency Spike", "Bluetooth audio output buffer latency spike", 0xD29922),

	// ═══════════════════════════════════════════════════════════════
	// 9. MACOS DARWIN KERNEL & SYSTEM (SYS-001 .. SYS-035)
	// ═══════════════════════════════════════════════════════════════
	SYS_001("SYS-001", "Darwin QoS Demotion", "Render thread migrated away from P-Cores to E-Cores", 0xF85149),
	SYS_002("SYS-002", "CPU Thermal Throttle", "Apple M-chip thermal throttling clock frequency", 0xF85149),
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
	SYS_016("SYS-016", "Spotlight IO Spike", "macOS Spotlight/TimeMachine background disk IO", 0xD29922),
	SYS_017("SYS-017", "Low Battery Throttle", "macOS Low Power Mode CPU clock cap active", 0xF85149),
	SYS_018("SYS-018", "Display Hz Mismatch", "External ProMotion variable refresh rate slip", 0xD29922),
	SYS_019("SYS-019", "Metal Present Wait", "Render thread waiting on nextDrawable availability", 0xD29922),
	SYS_020("SYS-020", "OS Notification Burst", "macOS system notification overlay interrupt", 0x8B949E),
	SYS_021("SYS-021", "Time Machine Backup", "Time Machine backup IO competing for SSD bandwidth", 0xD29922),
	SYS_022("SYS-022", "iCloud Sync Burst", "iCloud sync downloading files in background", 0x8B949E),
	SYS_023("SYS-023", "Software Update Check", "macOS checking for software updates background", 0x8B949E),
	SYS_024("SYS-024", "Dock Animation Stall", "macOS dock animation stealing GPU frames", 0x8B949E),
	SYS_025("SYS-025", "Stage Manager Overhead", "macOS Stage Manager window management overhead", 0xD29922),
	SYS_026("SYS-026", "Universal Control Sync", "Universal Control multi-device sync overhead", 0x8B949E),
	SYS_027("SYS-027", "Sidecar Display Encode", "Sidecar display encoding for iPad output", 0xD29922),
	SYS_028("SYS-028", "AirPlay Mirror Encode", "AirPlay mirroring video encoding overhead", 0xD29922),
	SYS_029("SYS-029", "Handoff State Sync", "Handoff state synchronization between devices", 0x8B949E),
	SYS_030("SYS-030", "Metal Shader Cache IO", "Metal shader cache disk read/write operation", 0xD29922),
	SYS_031("SYS-031", "Power Mode Transition", "macOS power mode transition (plugged/battery)", 0x8B949E),
	SYS_032("SYS-032", "External Display Wake", "External display wake-from-sleep blanking", 0xD29922),
	SYS_033("SYS-033", "Kernel Extension Load", "macOS kernel extension late binding load", 0x8B949E),
	SYS_034("SYS-034", "Rosetta Translation", "Rosetta 2 x86-64 code translation overhead", 0xF85149),
	SYS_035("SYS-035", "Game Mode Transition", "macOS Game Mode activation/deactivation", 0x8B949E),

	// ═══════════════════════════════════════════════════════════════
	// 10. NETWORK & PACKET PROCESSING (NET-001 .. NET-040)
	// ═══════════════════════════════════════════════════════════════
	NET_001("NET-001", "Packet Flood", "Server sending excessive packets per tick", 0xD29922),
	NET_002("NET-002", "Chunk Data Decompress", "Received chunk data ZLIB decompression lag", 0xD29922),
	NET_003("NET-003", "Entity Spawn Burst", "Mass entity spawn packet processing", 0xD29922),
	NET_004("NET-004", "Block Update Batch", "Large block update batch from server", 0xD29922),
	NET_005("NET-005", "Chat Message Flood", "Rapid chat message processing queue", 0x8B949E),
	NET_006("NET-006", "Scoreboard Update", "Scoreboard objective mass update packet", 0x8B949E),
	NET_007("NET-007", "Player List Update", "Player list info mass update packet", 0x8B949E),
	NET_008("NET-008", "Map Data Decode", "Map item pixel data decoding and upload", 0x8B949E),
	NET_009("NET-009", "Recipe Unlock Batch", "Mass recipe unlock notification processing", 0x8B949E),
	NET_010("NET-010", "Advancement Sync", "Advancement criteria sync packet burst", 0x8B949E),
	NET_011("NET-011", "Particle Spawn Packet", "Server-sent particle spawn command burst", 0xD29922),
	NET_012("NET-012", "Sound Event Packet", "Server-sent sound event packet burst", 0x8B949E),
	NET_013("NET-013", "Title Display Render", "Title/subtitle/actionbar text rendering", 0x8B949E),
	NET_014("NET-014", "Boss Event Update", "Boss bar creation/update packet processing", 0x8B949E),
	NET_015("NET-015", "Team Update Burst", "Scoreboard team mass update processing", 0x8B949E),
	NET_016("NET-016", "Container Sync Full", "Full container contents sync from server", 0xD29922),
	NET_017("NET-017", "Entity Effect Sync", "Mass entity status effect sync burst", 0x8B949E),
	NET_018("NET-018", "World Border Update", "World border resize/center change processing", 0x8B949E),
	NET_019("NET-019", "Explosion Notify", "Large explosion notification with block deltas", 0xD29922),
	NET_020("NET-020", "Resource Pack Load", "Server resource pack download and apply", 0xF85149),
	NET_021("NET-021", "Disconnect Reconnect", "Connection reset and session re-establishment", 0xF85149),
	NET_022("NET-022", "Encryption Handshake", "Network encryption key exchange overhead", 0x8B949E),
	NET_023("NET-023", "Compression Toggle", "Packet compression threshold change", 0x8B949E),
	NET_024("NET-024", "Plugin Channel Data", "Custom plugin channel data processing", 0x8B949E),
	NET_025("NET-025", "Network Timeout", "Network operation approaching timeout threshold", 0xF85149),
	NET_026("NET-026", "Packet Ordering Stall", "Out-of-order packet reassembly delay", 0xD29922),
	NET_027("NET-027", "Keep Alive Lag", "Keep-alive packet round-trip time exceeded", 0xD29922),
	NET_028("NET-028", "Velocity Plugin Proxy", "Velocity/Waterfall proxy routing overhead", 0x8B949E),
	NET_029("NET-029", "Modded Packet Decode", "Mod-specific network packet decoding delay", 0x8B949E),
	NET_030("NET-030", "Entity Metadata Burst", "Mass entity metadata update packet burst", 0xD29922),
	NET_031("NET-031", "WiFi Latency Spike", "WiFi network latency spike detected", 0xD29922),
	NET_032("NET-032", "DNS Resolution Delay", "Network DNS resolution causing delay", 0x8B949E),
	NET_033("NET-033", "TCP Window Full", "TCP receive window buffer full", 0xD29922),
	NET_034("NET-034", "Packet Loss Recovery", "TCP retransmission for lost packet recovery", 0xD29922),
	NET_035("NET-035", "Server TPS Drop", "Server tick rate drop detected via timestamps", 0xD29922),
	NET_036("NET-036", "Chunk Batch Oversize", "Chunk data batch exceeding optimal size", 0xD29922),
	NET_037("NET-037", "Entity Attach Event", "Entity attachment event sync processing", 0x8B949E),
	NET_038("NET-038", "Command Suggestion", "Tab-completion suggestion list processing", 0x8B949E),
	NET_039("NET-039", "NBT Packet Oversize", "Oversized NBT payload in network packet", 0xD29922),
	NET_040("NET-040", "VPN Overhead", "VPN encryption/tunneling overhead detected", 0x8B949E),

	// ═══════════════════════════════════════════════════════════════
	// 11. DISK I/O & FILE SYSTEM (IO-001 .. IO-030)
	// ═══════════════════════════════════════════════════════════════
	IO_001("IO-001", "Region File Read", "MCA region file disk read latency", 0xD29922),
	IO_002("IO-002", "Region File Write", "MCA region file disk write stall", 0xD29922),
	IO_003("IO-003", "Level.dat Save", "World level.dat save operation", 0xD29922),
	IO_004("IO-004", "Player Data Save", "Player data NBT file save operation", 0x8B949E),
	IO_005("IO-005", "Screenshot Capture", "Screenshot PNG encoding and write stall", 0xD29922),
	IO_006("IO-006", "Config File Write", "Mod configuration file disk write", 0x8B949E),
	IO_007("IO-007", "Resource Pack Read", "Resource pack ZIP/directory read stall", 0xD29922),
	IO_008("IO-008", "Shader Pack Load", "Iris shader pack ZIP extraction and parse", 0xF85149),
	IO_009("IO-009", "Log File Flush", "Log4j log file disk flush operation", 0x8B949E),
	IO_010("IO-010", "Options.txt Save", "Minecraft options.txt save operation", 0x8B949E),
	IO_011("IO-011", "Stats File Read", "Statistics JSON file read operation", 0x8B949E),
	IO_012("IO-012", "Advancements Save", "Advancement progress file save", 0x8B949E),
	IO_013("IO-013", "Skin Cache Fetch", "Player skin texture file cache fetch", 0xD29922),
	IO_014("IO-014", "Font Cache Build", "Unicode font glyph cache file build", 0xD29922),
	IO_015("IO-015", "Sound Asset Load", "Sound asset OGG file initial load from disk", 0xD29922),
	IO_016("IO-016", "Texture Atlas Build", "Texture atlas stitching reading sprite files", 0xD29922),
	IO_017("IO-017", "Model JSON Parse", "Block/item model JSON file parsing batch", 0xD29922),
	IO_018("IO-018", "Language File Load", "Language translation file loading", 0x8B949E),
	IO_019("IO-019", "APFS Snapshot", "macOS APFS creating filesystem snapshot", 0xD29922),
	IO_020("IO-020", "SSD Trim Queue", "NVMe SSD TRIM command queue processing", 0x8B949E),
	IO_021("IO-021", "File Lock Contention", "File system lock contention on world data", 0xD29922),
	IO_022("IO-022", "Directory Listing", "Large directory listing operation stall", 0x8B949E),
	IO_023("IO-023", "Mod JAR Scan", "Fabric Loader scanning mod JAR contents", 0xD29922),
	IO_024("IO-024", "Class File Load", "JVM loading class bytecode from JAR", 0x8B949E),
	IO_025("IO-025", "Mixin Transform IO", "Mixin bytecode transformation reading class", 0xD29922),
	IO_026("IO-026", "Replay Mod Record", "Replay mod recording frames to disk", 0xD29922),
	IO_027("IO-027", "World Backup Copy", "World backup file copy operation", 0xF85149),
	IO_028("IO-028", "Datapack Reload", "Datapack files reload from disk", 0xD29922),
	IO_029("IO-029", "Cache Directory Clean", "Cleaning old cache directory files", 0x8B949E),
	IO_030("IO-030", "External Storage Lag", "External/network storage access latency", 0xD29922),

	// ═══════════════════════════════════════════════════════════════
	// 12. THREADING & CONCURRENCY (THR-001 .. THR-035)
	// ═══════════════════════════════════════════════════════════════
	THR_001("THR-001", "Main Thread Block", "Render thread blocked on synchronization", 0xF85149),
	THR_002("THR-002", "Lock Contention", "Thread lock acquisition wait exceeded 1ms", 0xD29922),
	THR_003("THR-003", "Thread Pool Exhaust", "All thread pool threads busy", 0xD29922),
	THR_004("THR-004", "Queue Backpressure", "Work queue reaching capacity limit", 0xD29922),
	THR_005("THR-005", "CAS Spin Loop", "Compare-and-swap retry loop exceeded threshold", 0x8B949E),
	THR_006("THR-006", "Volatile Write Storm", "Excessive volatile field writes causing cache flush", 0x8B949E),
	THR_007("THR-007", "Thread Spawn Stall", "New thread creation and start overhead", 0xD29922),
	THR_008("THR-008", "Synchronized Block Hot", "Hot synchronized block causing serialization", 0xD29922),
	THR_009("THR-009", "ReadWrite Lock Stall", "ReadWriteLock writer acquisition delay", 0xD29922),
	THR_010("THR-010", "Executor Rejection", "Executor service rejecting task submission", 0x8B949E),
	THR_011("THR-011", "Future.get Block", "Main thread blocking on Future.get()", 0xF85149),
	THR_012("THR-012", "CountDownLatch Wait", "Thread waiting on CountDownLatch countdown", 0xD29922),
	THR_013("THR-013", "Barrier Wait", "Thread waiting at CyclicBarrier sync point", 0xD29922),
	THR_014("THR-014", "Phaser Advance", "Phaser phase advancement synchronization", 0x8B949E),
	THR_015("THR-015", "StampedLock Retry", "StampedLock optimistic read validation retry", 0x8B949E),
	THR_016("THR-016", "Thread Context Switch", "Excessive thread context switching overhead", 0xD29922),
	THR_017("THR-017", "Yield Spin Waste", "Thread.yield() busy-waiting cycle waste", 0x8B949E),
	THR_018("THR-018", "Interrupt Delivery", "Thread interrupt delivery and handling delay", 0x8B949E),
	THR_019("THR-019", "Daemon Thread GC", "Daemon thread cleanup at shutdown", 0x8B949E),
	THR_020("THR-020", "Channel IO Wait", "NIO channel I/O operation thread wait", 0xD29922),
	THR_021("THR-021", "Selector Wakeup", "NIO Selector wakeup overhead", 0x8B949E),
	THR_022("THR-022", "Atomic Contention", "AtomicReference contention under high concurrency", 0x8B949E),
	THR_023("THR-023", "ForkJoin Steal Fail", "ForkJoinPool work-stealing task dequeue fail", 0x8B949E),
	THR_024("THR-024", "CompletableFuture Chain", "Long CompletableFuture chain completion stall", 0xD29922),
	THR_025("THR-025", "Virtual Thread Pin", "Virtual thread pinned during synchronized block", 0xD29922),
	THR_026("THR-026", "Thread Local Cleanup", "ThreadLocal value cleanup overhead", 0x8B949E),
	THR_027("THR-027", "Weak Order Fence", "ARM64 memory ordering fence instruction stall", 0x8B949E),
	THR_028("THR-028", "Timer Thread Jitter", "ScheduledExecutorService timer thread jitter", 0xD29922),
	THR_029("THR-029", "Signal Handler Delay", "POSIX signal handler dispatch delay", 0x8B949E),
	THR_030("THR-030", "Netty Event Loop", "Netty event loop processing backlog", 0xD29922),
	THR_031("THR-031", "Render Submit Wait", "Render thread waiting for frame submit slot", 0xD29922),
	THR_032("THR-032", "Callback Queue Drain", "Main thread callback queue drain burst", 0xD29922),
	THR_033("THR-033", "Async Task Cancel", "Async task cancellation and cleanup overhead", 0x8B949E),
	THR_034("THR-034", "Thread Dump Stall", "Thread dump generation stalling all threads", 0xF85149),
	THR_035("THR-035", "GC Handshake Wait", "Thread waiting for GC handshake completion", 0xD29922),

	// ═══════════════════════════════════════════════════════════════
	// 13. RENDERING PIPELINE & DRAW CALLS (RND-001 .. RND-050)
	// ═══════════════════════════════════════════════════════════════
	RND_001("RND-001", "Draw Call Overflow", "Per-frame draw call count exceeding budget", 0xD29922),
	RND_002("RND-002", "State Change Thrash", "Excessive GL state changes per frame", 0xD29922),
	RND_003("RND-003", "Matrix Stack Deep", "ModelView matrix stack exceeding depth 16", 0x8B949E),
	RND_004("RND-004", "Immediate Mode Flush", "Immediate mode rendering forced buffer flush", 0xD29922),
	RND_005("RND-005", "Batched Render Sort", "Batched render layer sorting overhead", 0xD29922),
	RND_006("RND-006", "Outline Render Pass", "Entity outline additional render pass cost", 0xD29922),
	RND_007("RND-007", "Hand Render Layer", "First-person hand/item dual render pass", 0x8B949E),
	RND_008("RND-008", "Sky Render Complex", "Complex sky dome geometry generation", 0x8B949E),
	RND_009("RND-009", "World Border Render", "World border visual effect render cost", 0x8B949E),
	RND_010("RND-010", "Debug Renderer Active", "Debug visualization renderer active (hitboxes etc)", 0xD29922),
	RND_011("RND-011", "Name Tag Render Burst", "Multiple visible name tag text renders", 0x8B949E),
	RND_012("RND-012", "Armor Trim Layer", "Armor trim pattern additional texture layer", 0x8B949E),
	RND_013("RND-013", "Cape Render Physics", "Player cape cloth physics simulation", 0x8B949E),
	RND_014("RND-014", "Elytra Wing Anim", "Elytra wing angle and flex animation", 0x8B949E),
	RND_015("RND-015", "Item Stack Render", "Item stack 3D model render in hand/ground", 0x8B949E),
	RND_016("RND-016", "Map Render Update", "Map item texture pixel update and upload", 0xD29922),
	RND_017("RND-017", "Book Page Render", "Written book page text layout and render", 0x8B949E),
	RND_018("RND-018", "Villager Profession", "Villager profession-specific model overlay", 0x8B949E),
	RND_019("RND-019", "Shield Pattern Render", "Shield banner pattern texture composite render", 0x8B949E),
	RND_020("RND-020", "Trident Riptide Anim", "Trident riptide spinning animation matrix", 0x8B949E),
	RND_021("RND-021", "Crossbow Charged Model", "Crossbow charged state model switch", 0x8B949E),
	RND_022("RND-022", "Spyglass Overlay", "Spyglass zoom overlay render", 0x8B949E),
	RND_023("RND-023", "Render Region Build", "Sodium render region buffer build stall", 0xD29922),
	RND_024("RND-024", "Frustum Cull Compute", "Camera frustum culling computation burst", 0x8B949E),
	RND_025("RND-025", "Render Layer Switch", "Render layer state change in entity pipeline", 0x8B949E),
	RND_026("RND-026", "Held Item Transform", "Held item transformation matrix computation", 0x8B949E),
	RND_027("RND-027", "Player Skin Load", "Player skin texture network fetch and upload", 0xD29922),
	RND_028("RND-028", "Entity Model Rebake", "Entity model rebaking after resource reload", 0xD29922),
	RND_029("RND-029", "Block Overlay Render", "Block selection/destruction overlay render", 0x8B949E),
	RND_030("RND-030", "Scoreboard Render", "Scoreboard sidebar text layout and render", 0x8B949E),
	RND_031("RND-031", "Tab List Render", "Player tab list GUI render overhead", 0x8B949E),
	RND_032("RND-032", "Chat Render Burst", "Chat message list re-render with many messages", 0x8B949E),
	RND_033("RND-033", "Tooltip Render", "Complex tooltip text layout and render", 0x8B949E),
	RND_034("RND-034", "Inventory Render", "Large inventory container rendering overhead", 0x8B949E),
	RND_035("RND-035", "Creative Tab Switch", "Creative inventory tab switch item list rebuild", 0xD29922),
	RND_036("RND-036", "Sodium Sort Order", "Sodium translucent face sorting overhead", 0xD29922),
	RND_037("RND-037", "ImmediatelyFast Batch", "ImmediatelyFast batching optimization pass", 0x8B949E),
	RND_038("RND-038", "Entity Shadow Map", "Entity shadow map generation pass", 0xD29922),
	RND_039("RND-039", "Post Processing Chain", "Post-processing effect chain execution", 0xD29922),
	RND_040("RND-040", "Deferred Render Queue", "Deferred rendering command queue processing", 0xD29922),
	RND_041("RND-041", "Terrain Fog Calc", "Distance fog gradient calculation", 0x8B949E),
	RND_042("RND-042", "Celestial Body Render", "Sun/moon celestial body quad rendering", 0x8B949E),
	RND_043("RND-043", "End Portal Effect", "End portal starfield effect render", 0xD29922),
	RND_044("RND-044", "Nether Portal Effect", "Nether portal warp overlay render", 0x8B949E),
	RND_045("RND-045", "World Renderer Reset", "Complete world renderer state reset", 0xF85149),
	RND_046("RND-046", "Profiler Overhead", "Debug profiler rendering overhead", 0x8B949E),
	RND_047("RND-047", "Screenshot Capture", "Screenshot framebuffer capture and encode", 0xD29922),
	RND_048("RND-048", "Resource Reload", "Full resource/asset reload operation", 0xF85149),
	RND_049("RND-049", "Shader Toggle", "Shader pack toggle on/off transition", 0xF85149),
	RND_050("RND-050", "Resolution Change", "Window resolution change and framebuffer resize", 0xD29922),

	// ═══════════════════════════════════════════════════════════════
	// 14. TEXTURE & ATLAS MANAGEMENT (TEX-001 .. TEX-030)
	// ═══════════════════════════════════════════════════════════════
	TEX_001("TEX-001", "Atlas Stitch Stall", "Texture atlas stitching and upload delay", 0xD29922),
	TEX_002("TEX-002", "Sprite Animation Tick", "Animated sprite interpolation tick burst", 0xD29922),
	TEX_003("TEX-003", "Skin Texture Fetch", "Remote player skin texture download stall", 0xD29922),
	TEX_004("TEX-004", "Dynamic Texture Update", "Dynamic texture (map/compass) GPU re-upload", 0xD29922),
	TEX_005("TEX-005", "Mipmap Level Generate", "Mipmap level generation for atlas textures", 0xD29922),
	TEX_006("TEX-006", "Texture Upload Stall", "Blocking GPU texture upload from CPU memory", 0xD29922),
	TEX_007("TEX-007", "Atlas Overflow", "Texture atlas exceeding maximum dimensions", 0xF85149),
	TEX_008("TEX-008", "Font Glyph Cache Miss", "Unicode font glyph not in GPU cache", 0xD29922),
	TEX_009("TEX-009", "PBR Texture Load", "PBR normal/specular texture loading for shader", 0xD29922),
	TEX_010("TEX-010", "Custom Model Texture", "Custom item/block model texture binding", 0x8B949E),
	TEX_011("TEX-011", "Emissive Texture Layer", "Emissive texture layer additional bind", 0x8B949E),
	TEX_012("TEX-012", "Connected Texture Calc", "CTM connected texture pattern calculation", 0xD29922),
	TEX_013("TEX-013", "Custom Sky Texture", "Custom sky texture loading for shader", 0x8B949E),
	TEX_014("TEX-014", "GUI Sprite Load", "GUI sprite texture loading on demand", 0x8B949E),
	TEX_015("TEX-015", "Painting Texture Load", "Painting variant texture lookup and load", 0x8B949E),
	TEX_016("TEX-016", "Banner Pattern Cache", "Banner pattern texture cache generation", 0xD29922),
	TEX_017("TEX-017", "Shield Pattern Cache", "Shield pattern texture cache generation", 0xD29922),
	TEX_018("TEX-018", "Mob Texture Variant", "Mob texture variant selection and load", 0x8B949E),
	TEX_019("TEX-019", "Colormap Texture Load", "Biome colormap texture loading", 0x8B949E),
	TEX_020("TEX-020", "Lightmap Texture Regen", "Lightmap texture full regeneration", 0xD29922),
	TEX_021("TEX-021", "Shadow Map Texture", "Shadow map depth texture allocation", 0xD29922),
	TEX_022("TEX-022", "GBuffer Texture Alloc", "G-buffer texture allocation for deferred", 0xD29922),
	TEX_023("TEX-023", "Bloom Buffer Alloc", "Bloom effect buffer texture allocation", 0x8B949E),
	TEX_024("TEX-024", "SSAO Buffer Alloc", "SSAO noise and result buffer allocation", 0x8B949E),
	TEX_025("TEX-025", "Motion Vector Buffer", "Motion vector buffer texture allocation", 0x8B949E),
	TEX_026("TEX-026", "Texture Format Convert", "Texture format conversion (RGBA to BGRA etc)", 0x8B949E),
	TEX_027("TEX-027", "Compressed Tex Decode", "GPU compressed texture format decoding", 0x8B949E),
	TEX_028("TEX-028", "Render Target Resize", "Render target texture resize operation", 0xD29922),
	TEX_029("TEX-029", "Depth Texture Copy", "Depth texture copy for shader access", 0x8B949E),
	TEX_030("TEX-030", "Texture Memory Limit", "Total texture memory approaching GPU limit", 0xF85149),

	// ═══════════════════════════════════════════════════════════════
	// 15. LIGHTING & SHADOW COMPUTATION (LIT-001 .. LIT-030)
	// ═══════════════════════════════════════════════════════════════
	LIT_001("LIT-001", "Light Update Flood", "Mass light level recalculation from block change", 0xD29922),
	LIT_002("LIT-002", "Skylight Propagation", "Skylight value propagation through column", 0xD29922),
	LIT_003("LIT-003", "Block Light Propagation", "Block light source propagation cascade", 0xD29922),
	LIT_004("LIT-004", "Light Engine Queue Full", "Light engine update queue saturation", 0xD29922),
	LIT_005("LIT-005", "Shadow Cascade Compute", "Shadow map cascade split computation", 0xD29922),
	LIT_006("LIT-006", "Shadow Matrix Build", "Shadow projection matrix construction", 0x8B949E),
	LIT_007("LIT-007", "Shadow Frustum Cull", "Shadow pass frustum culling computation", 0xD29922),
	LIT_008("LIT-008", "Dynamic Light Update", "Dynamic light source position update burst", 0xD29922),
	LIT_009("LIT-009", "Light Section Rebuild", "Light data section rebuild for chunk", 0xD29922),
	LIT_010("LIT-010", "Starlight Engine Batch", "Starlight mod light engine batch processing", 0xD29922),
	LIT_011("LIT-011", "Smooth Light Interp", "Smooth lighting interpolation at block edges", 0x8B949E),
	LIT_012("LIT-012", "AO Face Shade", "Ambient occlusion face shade calculation", 0x8B949E),
	LIT_013("LIT-013", "Colored Light Blend", "Colored light source blending computation", 0xD29922),
	LIT_014("LIT-014", "Light Map Update", "Full light map texture update cycle", 0xD29922),
	LIT_015("LIT-015", "Torch Flicker Calc", "Torch light flicker randomization update", 0x8B949E),
	LIT_016("LIT-016", "Moon Phase Light", "Moon phase ambient light level adjustment", 0x8B949E),
	LIT_017("LIT-017", "Weather Light Dim", "Weather rain/thunder light level dimming", 0x8B949E),
	LIT_018("LIT-018", "Cave Light Resolve", "Underground cave darkness light resolve", 0x8B949E),
	LIT_019("LIT-019", "Nether Light Calc", "Nether dimension ambient light calculation", 0x8B949E),
	LIT_020("LIT-020", "End Light Calc", "End dimension ambient light calculation", 0x8B949E),
	LIT_021("LIT-021", "Light Opacity Lookup", "Block light opacity property lookup burst", 0x8B949E),
	LIT_022("LIT-022", "Light Neighbor Query", "Light level neighbor block query cascade", 0x8B949E),
	LIT_023("LIT-023", "Shadow Render Pass", "Complete shadow map render pass execution", 0xD29922),
	LIT_024("LIT-024", "Volumetric Light Ray", "Volumetric god-ray light shaft computation", 0xD29922),
	LIT_025("LIT-025", "PCSS Shadow Filter", "Percentage-closer soft shadow filter compute", 0xD29922),
	LIT_026("LIT-026", "RSM Indirect Light", "Reflective shadow map indirect illumination", 0xD29922),
	LIT_027("LIT-027", "Light Probe Update", "Irradiance light probe update cycle", 0x8B949E),
	LIT_028("LIT-028", "Emissive Block Glow", "Emissive block texture glow render pass", 0x8B949E),
	LIT_029("LIT-029", "Night Vision Effect", "Night vision brightness adjustment compute", 0x8B949E),
	LIT_030("LIT-030", "Conduit Power Light", "Conduit power brightness area computation", 0x8B949E),

	// ═══════════════════════════════════════════════════════════════
	// 16. WORLD, DIMENSION & BIOME (WLD-001 .. WLD-035)
	// ═══════════════════════════════════════════════════════════════
	WLD_001("WLD-001", "Biome Lookup Burst", "Biome parameter space lookup burst", 0xD29922),
	WLD_002("WLD-002", "Feature Generation", "World feature (tree/flower) placement burst", 0xD29922),
	WLD_003("WLD-003", "Structure Start", "Structure generation start calculation", 0xD29922),
	WLD_004("WLD-004", "Structure Piece Place", "Structure piece block placement burst", 0xD29922),
	WLD_005("WLD-005", "Random Tick Burst", "Random block tick processing burst", 0xD29922),
	WLD_006("WLD-006", "Scheduled Tick Burst", "Scheduled tick queue processing burst", 0xD29922),
	WLD_007("WLD-007", "Block Event Queue", "Block event (piston/note) queue processing", 0xD29922),
	WLD_008("WLD-008", "Weather State Change", "Weather state transition computation", 0x8B949E),
	WLD_009("WLD-009", "Time Sync Broadcast", "World time synchronization broadcast", 0x8B949E),
	WLD_010("WLD-010", "Spawn Position Calc", "Player spawn position calculation", 0x8B949E),
	WLD_011("WLD-011", "Difficulty Scale", "Regional difficulty scaling computation", 0x8B949E),
	WLD_012("WLD-012", "Mob Cap Check", "Mob spawning cap category count check", 0x8B949E),
	WLD_013("WLD-013", "Natural Spawn Cycle", "Natural mob spawning attempt cycle", 0xD29922),
	WLD_014("WLD-014", "Patrol Spawn Check", "Pillager patrol spawn eligibility check", 0x8B949E),
	WLD_015("WLD-015", "Wandering Trader Spawn", "Wandering trader spawn timer check", 0x8B949E),
	WLD_016("WLD-016", "Village POI Scan", "Village point-of-interest scan and update", 0xD29922),
	WLD_017("WLD-017", "Raid Status Check", "Village raid status check and progression", 0x8B949E),
	WLD_018("WLD-018", "Dragon Fight Status", "Ender dragon fight state machine update", 0x8B949E),
	WLD_019("WLD-019", "World Border Tick", "World border size interpolation tick", 0x8B949E),
	WLD_020("WLD-020", "Dimension Type Load", "Dimension type data registry lookup", 0x8B949E),
	WLD_021("WLD-021", "Biome Source Init", "Biome source initialization for new chunks", 0xD29922),
	WLD_022("WLD-022", "Surface Rule Apply", "Surface rule material application compute", 0x8B949E),
	WLD_023("WLD-023", "Density Function Eval", "Noise density function evaluation burst", 0xD29922),
	WLD_024("WLD-024", "Blender Source Calc", "Terrain blender source value calculation", 0x8B949E),
	WLD_025("WLD-025", "Erosion Noise Calc", "Terrain erosion noise parameter calculation", 0x8B949E),
	WLD_026("WLD-026", "Temperature Noise", "Biome temperature noise sample burst", 0x8B949E),
	WLD_027("WLD-027", "Humidity Noise", "Biome humidity noise sample burst", 0x8B949E),
	WLD_028("WLD-028", "Continentalness Calc", "Terrain continentalness parameter calculation", 0x8B949E),
	WLD_029("WLD-029", "Game Rule Check", "Game rule value lookup burst", 0x8B949E),
	WLD_030("WLD-030", "World Save Flush", "World save data flush to disk", 0xD29922),
	WLD_031("WLD-031", "Entity Section Move", "Entity moving between world sections", 0x8B949E),
	WLD_032("WLD-032", "Block State Lookup", "Block state property lookup burst", 0x8B949E),
	WLD_033("WLD-033", "Fluid Tick Queue", "Fluid scheduled tick processing burst", 0xD29922),
	WLD_034("WLD-034", "Block Neighbor Update", "Block neighbor update cascade propagation", 0xD29922),
	WLD_035("WLD-035", "Shape Cache Miss", "VoxelShape collision cache miss burst", 0x8B949E),

	// ═══════════════════════════════════════════════════════════════
	// 17. MOD COMPATIBILITY (MOD-001 .. MOD-026)
	// ═══════════════════════════════════════════════════════════════
	MOD_001("MOD-001", "Sodium Mesh Stall", "Sodium chunk mesh builder backlog", 0xD29922),
	MOD_002("MOD-002", "Iris Shader Load", "Iris shader pack compilation stall", 0xF85149),
	MOD_003("MOD-003", "Iris Pass Overhead", "Iris additional render pass overhead", 0xD29922),
	MOD_004("MOD-004", "Lithium Opt Conflict", "Lithium optimization path conflict", 0x8B949E),
	MOD_005("MOD-005", "FerriteCore Compact", "FerriteCore memory compaction cycle", 0x8B949E),
	MOD_006("MOD-006", "ImmediatelyFast Batch", "ImmediatelyFast batching stall", 0x8B949E),
	MOD_007("MOD-007", "Mod Event Handler", "Mod event handler execution delay", 0xD29922),
	MOD_008("MOD-008", "Mixin Injection Lag", "Mixin target method injection overhead", 0x8B949E),
	MOD_009("MOD-009", "Fabric API Event", "Fabric API event dispatch burst", 0x8B949E),
	MOD_010("MOD-010", "Config Reload Burst", "Multiple mods reloading configs simultaneously", 0xD29922),
	MOD_011("MOD-011", "Registry Freeze", "Mod registry freeze and validation cycle", 0xD29922),
	MOD_012("MOD-012", "Resource Reload Hook", "Mod resource reload listener overhead", 0xD29922),
	MOD_013("MOD-013", "Render Hook Cascade", "Multiple mod render hooks in same phase", 0xD29922),
	MOD_014("MOD-014", "Entity Render Override", "Mod overriding entity render pipeline", 0x8B949E),
	MOD_015("MOD-015", "Custom Block Render", "Mod custom block rendering overhead", 0x8B949E),
	MOD_016("MOD-016", "Mod Network Channel", "Mod custom network channel overhead", 0x8B949E),
	MOD_017("MOD-017", "Mod GUI Layer", "Mod HUD overlay or GUI layer overhead", 0x8B949E),
	MOD_018("MOD-018", "Mod Tick Handler", "Mod client tick event handler overhead", 0xD29922),
	MOD_019("MOD-019", "Mod World Render", "Mod world render event handler overhead", 0xD29922),
	MOD_020("MOD-020", "Mod Particle Custom", "Mod custom particle type overhead", 0x8B949E),
	MOD_021("MOD-021", "Mod Sound Custom", "Mod custom sound event overhead", 0x8B949E),
	MOD_022("MOD-022", "Shader Compat Issue", "Shader mod compatibility rendering issue", 0xD29922),
	MOD_023("MOD-023", "C2ME Worldgen Stall", "C2ME async worldgen thread contention", 0xD29922),
	MOD_024("MOD-024", "EntityCulling Conflict", "Entity culling mod conflict or overlap", 0x8B949E),
	MOD_025("MOD-025", "ModMenu Render", "ModMenu config screen rendering overhead", 0x8B949E),
	MOD_026("MOD-026", "Sodium Allocator Not SWAP", "Sodium chunk memory allocator is not SWAP on Apple Silicon (user-owned; not applied)", 0xD29922),

	// ═══════════════════════════════════════════════════════════════
	// 18. JIT COMPILER & OPTIMIZATION (JIT-001 .. JIT-020)
	// ═══════════════════════════════════════════════════════════════
	JIT_001("JIT-001", "C2 Compile Burst", "JIT C2 compiler burst compilation phase", 0xD29922),
	JIT_002("JIT-002", "Deoptimization Wave", "Multiple method deoptimizations in sequence", 0xD29922),
	JIT_003("JIT-003", "OSR Compile", "On-stack replacement compilation trigger", 0x8B949E),
	JIT_004("JIT-004", "Inline Cache Miss", "Polymorphic inline cache miss and re-resolve", 0x8B949E),
	JIT_005("JIT-005", "Code Cache Full", "JIT compiled code cache approaching limit", 0xD29922),
	JIT_006("JIT-006", "Intrinsic Fallback", "JVM intrinsic method falling back to Java impl", 0x8B949E),
	JIT_007("JIT-007", "Loop Unroll Limit", "JIT loop unrolling exceeding budget", 0x8B949E),
	JIT_008("JIT-008", "Escape Analysis Fail", "Object escape analysis preventing stack alloc", 0x8B949E),
	JIT_009("JIT-009", "Branch Profile Flush", "Branch prediction profile data flush", 0x8B949E),
	JIT_010("JIT-010", "Compiler Thread Busy", "All JIT compiler threads busy", 0xD29922),
	JIT_011("JIT-011", "Tier 3 Compile Queue", "C1 tier 3 compilation queue backlog", 0x8B949E),
	JIT_012("JIT-012", "Tier 4 Compile Queue", "C2 tier 4 compilation queue backlog", 0xD29922),
	JIT_013("JIT-013", "Uncommon Trap Hit", "JIT uncommon trap deoptimization trigger", 0x8B949E),
	JIT_014("JIT-014", "Speculative Guard", "Speculative type guard check failure", 0x8B949E),
	JIT_015("JIT-015", "Vectorization Fail", "Auto-vectorization failed for hot loop", 0x8B949E),
	JIT_016("JIT-016", "Null Check Uncommon", "Null check uncommon trap in hot method", 0x8B949E),
	JIT_017("JIT-017", "Array Bounds Check", "Array bounds check preventing optimization", 0x8B949E),
	JIT_018("JIT-018", "Lambda Form Resolve", "MethodHandle/lambda form linkage resolution", 0x8B949E),
	JIT_019("JIT-019", "Dynamic Dispatch", "Megamorphic virtual dispatch in hot loop", 0x8B949E),
	JIT_020("JIT-020", "Graal Compile Stall", "GraalVM JIT compiler long compilation pause", 0xD29922),

	// ═══════════════════════════════════════════════════════════════
	// 19. VIDEO SETTINGS & DISPLAY (VID-001 .. VID-020)
	// ═══════════════════════════════════════════════════════════════
	VID_001("VID-001", "VSync Frame Skip", "VSync causing frame skip on deadline miss", 0xD29922),
	VID_002("VID-002", "Frame Limiter Stall", "Frame rate limiter oversleeping", 0xD29922),
	VID_003("VID-003", "Resolution Scale Change", "Render resolution scale live change", 0xD29922),
	VID_004("VID-004", "Fullscreen Toggle", "Fullscreen/windowed mode toggle transition", 0xD29922),
	VID_005("VID-005", "Display Mode Change", "Display output mode/resolution change", 0xD29922),
	VID_006("VID-006", "GUI Scale Recalc", "GUI scale factor change and layout rebuild", 0x8B949E),
	VID_007("VID-007", "Render Distance Change", "Render distance live change chunk reload", 0xF85149),
	VID_008("VID-008", "Graphics Quality Change", "Graphics quality level change (fancy/fast)", 0xD29922),
	VID_009("VID-009", "Smooth Lighting Toggle", "Smooth lighting toggle and chunk rebuild", 0xD29922),
	VID_010("VID-010", "Entity Shadow Toggle", "Entity shadow rendering toggle", 0x8B949E),
	VID_011("VID-011", "Particle Quality Change", "Particle quality level setting change", 0x8B949E),
	VID_012("VID-012", "Biome Blend Change", "Biome blend radius change and chunk rebuild", 0xD29922),
	VID_013("VID-013", "Mipmap Level Change", "Mipmap level change and texture rebuild", 0xD29922),
	VID_014("VID-014", "FOV Change Interp", "Field-of-view change interpolation", 0x8B949E),
	VID_015("VID-015", "Brightness Change", "Display brightness/gamma adjustment", 0x8B949E),
	VID_016("VID-016", "Cloud Height Change", "Cloud render height setting change", 0x8B949E),
	VID_017("VID-017", "Entity Distance Change", "Entity render distance scaling change", 0x8B949E),
	VID_018("VID-018", "Simulation Distance Change", "Simulation distance setting change", 0xD29922),
	VID_019("VID-019", "ProMotion Rate Change", "ProMotion display refresh rate transition", 0x8B949E),
	VID_020("VID-020", "HDR Toggle", "HDR display mode toggle", 0xD29922),

	// ═══════════════════════════════════════════════════════════════
	// 20. PHYSICS & COLLISION (PHY-001 .. PHY-025)
	// ═══════════════════════════════════════════════════════════════
	PHY_001("PHY-001", "Collision Broadphase", "Entity collision broadphase AABB query burst", 0xD29922),
	PHY_002("PHY-002", "VoxelShape Merge", "Complex VoxelShape boolean merge operation", 0xD29922),
	PHY_003("PHY-003", "Raycast Long Distance", "Long-distance block raycast computation", 0xD29922),
	PHY_004("PHY-004", "Fluid Flow Physics", "Fluid flow direction calculation burst", 0xD29922),
	PHY_005("PHY-005", "Explosion Raycast", "Explosion block destruction raycast burst", 0xF85149),
	PHY_006("PHY-006", "Block Shape Cache", "Block collision shape cache miss burst", 0x8B949E),
	PHY_007("PHY-007", "Entity Push Cascade", "Dense entity pushing physics cascade", 0xD29922),
	PHY_008("PHY-008", "Gravity Computation", "Entity gravity and terminal velocity calculation", 0x8B949E),
	PHY_009("PHY-009", "Piston Push Check", "Piston push/pull block validity check chain", 0xD29922),
	PHY_010("PHY-010", "Boat Collision Complex", "Boat multi-surface collision resolution", 0x8B949E),
	PHY_011("PHY-011", "Elytra Flight Physics", "Elytra flight vector and drag computation", 0x8B949E),
	PHY_012("PHY-012", "Trident Return Path", "Loyalty trident return pathfinding", 0x8B949E),
	PHY_013("PHY-013", "Fishing Hook Physics", "Fishing hook bobber physics simulation", 0x8B949E),
	PHY_014("PHY-014", "Anvil Fall Impact", "Falling anvil impact damage calculation", 0x8B949E),
	PHY_015("PHY-015", "Cactus Contact Check", "Cactus contact damage AABB check", 0x8B949E),
	PHY_016("PHY-016", "Powdered Snow Sink", "Entity sinking in powdered snow physics", 0x8B949E),
	PHY_017("PHY-017", "Honey Block Slide", "Honey block slide and bounce physics", 0x8B949E),
	PHY_018("PHY-018", "Slime Block Bounce", "Slime block entity bounce physics calculation", 0x8B949E),
	PHY_019("PHY-019", "Scaffolding Climb", "Scaffolding climb collision detection", 0x8B949E),
	PHY_020("PHY-020", "Berry Bush Slow", "Sweet berry bush movement speed reduction", 0x8B949E),
	PHY_021("PHY-021", "Web Slow Physics", "Cobweb movement physics calculation", 0x8B949E),
	PHY_022("PHY-022", "Soul Sand Slow", "Soul sand movement speed reduction physics", 0x8B949E),
	PHY_023("PHY-023", "Ice Slide Physics", "Ice block sliding friction computation", 0x8B949E),
	PHY_024("PHY-024", "Bubble Column Push", "Bubble column upward/downward push physics", 0x8B949E),
	PHY_025("PHY-025", "Wind Charge Knockback", "Wind charge area knockback calculation", 0xD29922),

	// ═══════════════════════════════════════════════════════════════
	// 21. DATA, NBT & SERIALIZATION (DAT-001 .. DAT-025)
	// ═══════════════════════════════════════════════════════════════
	DAT_001("DAT-001", "NBT Compound Parse", "Large NBT compound tag parsing stall", 0xD29922),
	DAT_002("DAT-002", "NBT List Iterate", "Long NBT list tag iteration", 0x8B949E),
	DAT_003("DAT-003", "Item Stack Serialize", "Item stack NBT serialization burst", 0x8B949E),
	DAT_004("DAT-004", "Chunk NBT Decode", "Full chunk NBT data decoding", 0xD29922),
	DAT_005("DAT-005", "Entity NBT Save", "Entity NBT data serialization for save", 0x8B949E),
	DAT_006("DAT-006", "Block Entity NBT", "Block entity NBT data sync decode", 0x8B949E),
	DAT_007("DAT-007", "Recipe Deserialize", "Recipe JSON deserialization burst", 0xD29922),
	DAT_008("DAT-008", "Tag Collection Build", "Block/item tag collection rebuild", 0xD29922),
	DAT_009("DAT-009", "Registry Sync", "Data-driven registry sync from server", 0xD29922),
	DAT_010("DAT-010", "Loot Table Parse", "Loot table JSON parsing and validation", 0x8B949E),
	DAT_011("DAT-011", "Predicate Evaluate", "Data predicate evaluation burst", 0x8B949E),
	DAT_012("DAT-012", "Codec Decode Burst", "Mojang Codec decode operation burst", 0xD29922),
	DAT_013("DAT-013", "JSON Parse Large", "Large JSON document parsing stall", 0xD29922),
	DAT_014("DAT-014", "Component Serialize", "Item component serialization overhead", 0x8B949E),
	DAT_015("DAT-015", "Palette Serialize", "Block state palette serialization", 0x8B949E),
	DAT_016("DAT-016", "Bitset Serialize", "Large bitset serialization for chunk data", 0x8B949E),
	DAT_017("DAT-017", "String Intern Burst", "String interning burst from network data", 0x8B949E),
	DAT_018("DAT-018", "UUID Parse Burst", "UUID string parsing burst from player data", 0x8B949E),
	DAT_019("DAT-019", "Identifier Resolve", "ResourceLocation/Identifier resolution burst", 0x8B949E),
	DAT_020("DAT-020", "SNBT Parse", "Stringified NBT command parsing", 0x8B949E),
	DAT_021("DAT-021", "DataFixer Update", "DataFixer legacy data version upgrade", 0xD29922),
	DAT_022("DAT-022", "Packet Decode Large", "Large network packet deserialization", 0xD29922),
	DAT_023("DAT-023", "Inventory Serialize", "Full inventory contents serialization", 0x8B949E),
	DAT_024("DAT-024", "Statistics Merge", "Player statistics data merge operation", 0x8B949E),
	DAT_025("DAT-025", "Component Patch Apply", "Item component data patch application", 0x8B949E);

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

	/** Returns the subsystem category prefix (e.g. "GC", "GPU", "MEM"). */
	public String getCategory() {
		int dash = code.indexOf('-');
		return dash > 0 ? code.substring(0, dash) : code;
	}

	/** Returns the numeric portion of the code. */
	public int getNumber() {
		int dash = code.indexOf('-');
		if (dash < 0 || dash + 1 >= code.length()) return 0;
		try {
			return Integer.parseInt(code.substring(dash + 1));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/** Severity level: 0=info, 1=warning, 2=critical based on color. */
	public int severity() {
		if (color == 0xF85149) return 2; // Red = critical
		if (color == 0xD29922 || color == 0xF2CC60) return 1; // Yellow/amber = warning
		return 0; // Grey/blue/green = info
	}

	/**
	 * Attribute a measured micro-stutter. Only codes with a live probe can be returned.
	 *
	 * Priority: GC pause ({@code frameGcDeltaMs >= 10}, typically previous-frame MXBean sample)
	 * → physical RAM → heap → heap vs UMA → shader/shadow → throttle L1/L2
	 * → Sodium allocator warning → SpikeScope.dominant() → shader fallback → FRAME-001.
	 *
	 * @param sodiumAllocatorWarning {@code true} when ChipPower reports Sodium is not SWAP
	 *                               (sampled off the render thread by SpikeMonitor)
	 */
	public static StutterErrorCode fromSpike(
		long deltaNanos,
		GcProbe gc,
		MemoryPressureProbe mem,
		SpikeScope.Phase phase,
		boolean isShaderActive,
		boolean isShadowPass,
		boolean sodiumAllocatorWarning
	) {
		if (phase == null) {
			phase = SpikeScope.Phase.NONE;
		}
		if (deltaNanos <= 0L) {
			return FRAME_001;
		}

		// 1. GC pause. GcProbe.sampleFrame is async; this is usually the previous frame.
		if (gc != null && gc.frameGcDeltaMs() >= 10L) {
			return GC_001;
		}

		// 2. Physical unified RAM
		if (mem != null) {
			long freePhysicalMb = mem.freePhysicalMb();
			if (freePhysicalMb >= 0L) {
				if (freePhysicalMb < 64L) {
					return MEM_001;
				}
				if (freePhysicalMb < 128L) {
					return MEM_002;
				}
				if (freePhysicalMb < 256L) {
					return MEM_003;
				}
			}
		}

		// 3. Heap occupancy
		if (mem != null && mem.heapMaxMb() > 0L) {
			long heapPct = mem.heapUsedMb() * 100L / mem.heapMaxMb();
			if (heapPct > 92L) {
				return GC_030;
			}
			if (heapPct > 90L) {
				return MEM_026;
			}
			if (heapPct > 80L) {
				return MEM_027;
			}
		}

		// 4. Heap vs physical UMA (8GB+4G-style or heap ≥ physical)
		if (RamDiscipline.get().heapVsPhysicalUnhealthy()) {
			return MEM_041;
		}

		// 5. Shader shadow pass — Iris live query plus mixin latch (valid at render RETURN)
		if (isShadowPass) {
			return GPU_004;
		}

		// 6. ShaderAutoThrottle session overlay (L1/L2 only; L0 is not a stutter cause)
		ShaderAutoThrottle.Level throttle = ShaderAutoThrottle.get().level();
		if (throttle == ShaderAutoThrottle.Level.L2) {
			return GPU_062;
		}
		if (throttle == ShaderAutoThrottle.Level.L1) {
			return GPU_061;
		}

		// 7. Sodium chunk allocator is not SWAP (standing config; blame weak/unscoped phases)
		if (sodiumAllocatorWarning && isSodiumScopedPhase(phase)) {
			return MOD_026;
		}

		// 8. SpikeScope dominant phase from mixin push/pop
		StutterErrorCode scoped = fromDominantPhase(phase);
		if (scoped != FRAME_001) {
			return scoped;
		}

		// 9. Shader pack active but no instrumented phase won
		if (isShaderActive) {
			return GPU_041;
		}

		return FRAME_001;
	}

	private static boolean isSodiumScopedPhase(SpikeScope.Phase phase) {
		return phase == SpikeScope.Phase.NONE
			|| phase == SpikeScope.Phase.CHUNK_UPLOAD
			|| phase == SpikeScope.Phase.WORLD_RENDER
			|| phase == SpikeScope.Phase.RENDER_WAIT;
	}

	private static StutterErrorCode fromDominantPhase(SpikeScope.Phase phase) {
		return switch (phase) {
			case NONE -> FRAME_001;
			case WORLD_RENDER -> RND_001;
			case CHUNK_UPLOAD -> CHK_007;
			case RENDER_WAIT -> THR_001;
			case ENTITY_TICK -> ENT_001;
			case PARTICLE -> PRT_012;
			case SOUND -> SND_001;
			case GLINT -> RND_015;
			case LIGHTMAP -> LIT_014;
			case WEATHER -> PRT_015;
			case HUD -> RND_030;
			case SHADOW_ENTITY -> GPU_031;
			case PARTICLE_TRIM -> ParticleTrim.lastAggressive() ? PRT_036 : PRT_013;
		};
	}
}
