package com.suntide_20210418.dimensiontech.datagen;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import java.util.ArrayList;
import java.util.List;

/**
 * Makes the data-generation JVM actually terminate once generation is over.
 *
 * <p>Vanilla relies on the implicit shutdown rule: {@code net.minecraft.data.Main} returns and,
 * because no non-daemon thread is left, the JVM exits. That rule breaks as soon as any other mod on
 * the development classpath owns a non-daemon thread. KubeJS is a concrete case - its {@code
 * ScriptType} enum creates one single-thread executor per script type ({@code
 * ScriptType.executor}), {@code ConsoleJS.resetFile()} submits to that executor while {@code
 * KubeJS.<init>} unloads the script types, and the pool is never shut down. Its worker thread stays
 * parked in {@code LinkedBlockingQueue.take} forever, so the process survives well past "All
 * providers took" and an IDE launch never reports the run as finished.
 *
 * <p>This cannot be solved from the build script alone. A Gradle-installed IDE run configuration of
 * type "Application" launches the module classpath directly instead of a Gradle task, and the
 * module classpath always contains everything declared as {@code implementation}, so optional
 * development mods stay on the data-generation classpath no matter how a {@code runData} Gradle
 * task would configure it. The watchdog therefore acts from inside the process.
 *
 * <p>It waits for the {@code main} thread - the thread that runs the providers and writes the hash
 * cache - to finish, and only then forces the exit, so it never races the end of {@code
 * DataGenerator.run()}. If nothing is holding the process open, the JVM is left alone.
 *
 * <p>Installed from {@code GatherDataEvent}, which is only fired during a data-generation run, so
 * client, server and game-test launches are never affected.
 */
public final class DatagenExitWatchdog {
    private DatagenExitWatchdog() {}

    /** Installs the watchdog for the current data-generation run. */
    public static void install() {
        Thread generator = mainThread();
        if (generator == null) {
            DimensionTechMod.LOGGER.warn(
                    "Data generation is running, but the main thread could not be located; the"
                            + " process might stay alive after the providers finish");
            return;
        }
        Thread watchdog =
                new Thread(
                        () -> exitWhenGenerationFinished(generator), "dimension-tech-datagen-exit");
        // Daemon on purpose: if the watchdog itself kept the JVM alive, it would be the bug it is
        // supposed to fix.
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private static void exitWhenGenerationFinished(Thread generator) {
        try {
            generator.join();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return;
        }
        List<String> lingering = nonDaemonThreads();
        if (lingering.isEmpty()) {
            // Nothing holds the process open, so let the normal shutdown sequence run.
            return;
        }
        DimensionTechMod.LOGGER.warn(
                "Data generation finished, but {} non-daemon thread(s) are still alive: {}. Exiting"
                        + " the process explicitly, otherwise it would hang indefinitely.",
                lingering.size(),
                String.join(", ", lingering));
        System.exit(0);
    }

    private static List<String> nonDaemonThreads() {
        List<String> names = new ArrayList<>();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.isAlive() && !thread.isDaemon()) names.add(thread.getName());
        }
        return names;
    }

    private static Thread mainThread() {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            // The launcher's main thread is always the first thread the JVM creates.
            if (thread.getId() == 1L) return thread;
        }
        return null;
    }
}
