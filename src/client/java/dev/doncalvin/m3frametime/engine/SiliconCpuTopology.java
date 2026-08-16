package dev.doncalvin.m3frametime.engine;

import dev.doncalvin.m3frametime.M3FrametimeMod;
import dev.doncalvin.m3frametime.client.DarwinQos;

/**
 * Apple Silicon CPU Topology & Asymmetric Multiprocessing (AMP) Router.
 * Distributes Minecraft workloads across Performance (P-Core) and Efficiency (E-Core) clusters:
 * - Render thread & main tick: Dedicated to P-Cores (User Interactive, 4+ GHz)
 * - Sodium chunk meshing & lighting: Assigned to P-Cores (User Initiated)
 * - Sound decoding & background I/O: Routed to E-Cores (Utility) to reserve 100% of P-Core horsepower for maximum FPS.
 */
public final class SiliconCpuTopology {
	private static final SiliconCpuTopology INSTANCE = new SiliconCpuTopology();

	private final int totalCores;
	private final int estimatedPCores;
	private final int estimatedECores;
	private final String chipName;

	private SiliconCpuTopology() {
		this.totalCores = Runtime.getRuntime().availableProcessors();

		// Apple Silicon Core Distribution Estimation
		if (totalCores <= 8) { // M1/M2/M3 Base
			this.estimatedPCores = 4;
			this.estimatedECores = totalCores - 4;
			this.chipName = "Apple Silicon (Base / 4P + " + estimatedECores + "E)";
		} else if (totalCores <= 12) { // M1/M2/M3 Pro
			this.estimatedPCores = totalCores - 4;
			this.estimatedECores = 4;
			this.chipName = "Apple Silicon Pro (" + estimatedPCores + "P + 4E)";
		} else if (totalCores <= 16) { // M1/M2/M3 Max
			this.estimatedPCores = totalCores - 4;
			this.estimatedECores = 4;
			this.chipName = "Apple Silicon Max (" + estimatedPCores + "P + 4E)";
		} else { // Ultra (Dual Die)
			this.estimatedPCores = totalCores - 8;
			this.estimatedECores = 8;
			this.chipName = "Apple Silicon Ultra (" + estimatedPCores + "P + 8E)";
		}
	}

	public static SiliconCpuTopology get() {
		return INSTANCE;
	}

	public int getTotalCores() {
		return totalCores;
	}

	public int getEstimatedPCores() {
		return estimatedPCores;
	}

	public int getEstimatedECores() {
		return estimatedECores;
	}

	public String getChipName() {
		return chipName;
	}

	/** Assigns the calling worker thread to efficiency cores to free P-cores for rendering. */
	public void routeToEfficiencyCores() {
		DarwinQos.setQosSelf(DarwinQos.QOS_CLASS_UTILITY);
	}

	/** Assigns the calling worker thread to performance cores for high-speed chunk meshing. */
	public void routeToPerformanceCores() {
		DarwinQos.setQosSelf(DarwinQos.QOS_CLASS_USER_INITIATED);
	}
}
