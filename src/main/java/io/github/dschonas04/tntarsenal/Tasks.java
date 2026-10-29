// Autor: Dschonas04 (https://github.com/Dschonas04)

package io.github.dschonas04.tntarsenal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Detonationen, die sich über Zeit abspielen – ein Tunnel, der Scheibe für Scheibe gegraben wird,
 * ein schwarzes Loch, das fünf Sekunden lang zieht –, melden hier eine Aufgabe an. Der Server führt
 * jede Aufgabe am Ende jedes Server-Ticks einmal aus, bis sie meldet, dass sie fertig ist. Die
 * Arbeit auf Ticks zu verteilen, verhindert außerdem, dass große Aufträge den Server ausbremsen.
 */
final class Tasks {
    private Tasks() {}

    @FunctionalInterface
    interface Task {
        /** Läuft einmal pro Tick; {@code age} zählt ab 0. Gibt true zurück, wenn fertig. */
        boolean tick(int age);
    }

    private static final Logger LOG = LoggerFactory.getLogger(TntArsenal.MOD_ID);
    private static final List<Running> RUNNING = new ArrayList<>();
    private static final List<Running> STARTED = new ArrayList<>();

    private static final class Running {
        final Task task;
        final Runnable onAbort;
        int age;

        Running(Task task, Runnable onAbort) {
            this.task = task;
            this.onAbort = onAbort;
        }

        void abort() {
            try {
                onAbort.run();
            } catch (RuntimeException e) {
                LOG.error("Cleaning up after a TNT Arsenal detonation failed", e);
            }
        }
    }

    static void start(Task task) {
        start(task, () -> {});
    }

    /**
     * Wie {@link #start(Task)}, führt aber {@code onAbort} aus, wenn die Aufgabe scheitert oder der
     * Server stoppt, bevor sie fertig ist.
     */
    static void start(Task task, Runnable onAbort) {
        STARTED.add(new Running(task, onAbort));
    }

    /**
     * Wird aus dem Server-Tick aufgerufen. Aufgaben, die in diesem Tick gestartet werden, laufen ab
     * dem nächsten.
     */
    static void tickAll() {
        RUNNING.addAll(STARTED);
        STARTED.clear();
        for (Iterator<Running> it = RUNNING.iterator(); it.hasNext(); ) {
            Running running = it.next();
            boolean done;
            try {
                done = running.task.tick(running.age++);
            } catch (RuntimeException e) {
                LOG.error("A TNT Arsenal detonation failed and was stopped", e);
                running.abort();
                done = true;
            }
            if (done) it.remove();
        }
    }

    /** Verwirft alles, wenn der Server stoppt, damit keine Aufgabe ihre Welt überlebt. */
    static void clear() {
        RUNNING.forEach(Running::abort);
        STARTED.forEach(Running::abort);
        RUNNING.clear();
        STARTED.clear();
    }
}
