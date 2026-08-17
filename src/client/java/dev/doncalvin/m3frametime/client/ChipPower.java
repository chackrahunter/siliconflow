package dev.doncalvin.m3frametime.client;

import dev.doncalvin.m3frametime.M3FrametimeMod;

/** Reports runtime architecture facts without changing scheduler or dependency-owned settings. */
public final class ChipPower {
    private static boolean reported;
    private ChipPower() {}

    public static void applyOnce() {
        if (reported) return;
        reported = true;
        M3FrametimeMod.LOGGER.info("Runtime facts: os={} arch={} vm={} logicalProcessors={} (scheduler ownership unchanged)",
            System.getProperty("os.name", "?"), System.getProperty("os.arch", "?"),
            System.getProperty("java.vm.name", "?"), Runtime.getRuntime().availableProcessors());
    }

    public static void reinforceRenderPriority() {}
    public static void tryBoostSodiumWorkers() {}
}
