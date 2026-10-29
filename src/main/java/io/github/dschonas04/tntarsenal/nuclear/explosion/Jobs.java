package io.github.dschonas04.tntarsenal.nuclear.explosion;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Die Auftragswarteschlange: Arbeit, die auf viele Ticks verteilt ist – die Explosion, verzögerte
 * Geräusche –, läuft hier am Ende jedes Welt-Ticks, bis sie meldet, dass sie fertig ist.
 */
public final class Jobs {
    private Jobs() {}

    public interface Job {
        ServerLevel level();

        /** Ein Tick Arbeit. Gibt true zurück, wenn fertig. */
        boolean tick();

        /**
         * Ein Auftrag, der Blöcke in großen Mengen ändert; davon laufen nur wenige gleichzeitig.
         */
        default boolean heavy() {
            return false;
        }

        /** Der Server stoppt; freigeben, was der Auftrag hält (erzwungene Chunks). */
        default void abort() {}
    }

    private static final List<Job> RUNNING = new ArrayList<>();

    /**
     * Wie viele schwere Aufträge pro Welt und Tick laufen. Jeder hat sein eigenes Blockbudget pro
     * Tick, ohne Obergrenze ergäbe eine Reihe gleichzeitig gezündeter Bomben also einen
     * eingefrorenen Server; der Rest wartet in der Reihenfolge, in der gezündet wurde.
     */
    private static final int MAX_HEAVY = 2;

    public static void add(Job job) {
        RUNNING.add(job);
    }

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(
                level -> {
                    // eine Kopie, weil ein Auftrag einen weiteren hinzufügen kann
                    int heavy = 0;
                    for (Job job : List.copyOf(RUNNING)) {
                        if (job.level() != level) continue;
                        if (job.heavy() && heavy++ >= MAX_HEAVY) continue;
                        boolean done;
                        try {
                            done = job.tick();
                        } catch (RuntimeException e) {
                            io.github.dschonas04.tntarsenal.nuclear.NuclearTnt.LOG.error(
                                    "A nuclear TNT job failed and was stopped", e);
                            job.abort();
                            done = true;
                        }
                        if (done) RUNNING.remove(job);
                    }
                });
        ServerLifecycleEvents.SERVER_STOPPING.register(
                server -> {
                    RUNNING.forEach(Job::abort);
                    RUNNING.clear();
                });
    }
}
