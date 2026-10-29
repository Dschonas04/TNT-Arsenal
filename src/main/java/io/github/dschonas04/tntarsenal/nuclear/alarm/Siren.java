// Autor: Dschonas04 (https://github.com/Dschonas04)

package io.github.dschonas04.tntarsenal.nuclear.alarm;

import io.github.dschonas04.tntarsenal.nuclear.Sounds;
import io.github.dschonas04.tntarsenal.nuclear.explosion.Jobs;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Die Luftschutzsirene: ein Ton, der über vier Sekunden an- und abschwillt, synthetisiert von
 * tools/sounds.py. Sirenenblöcke heulen, solange sie Strom haben; jede gezündete Bombe lässt sie
 * über das Land heulen, solange ihre Lunte brennt.
 */
public final class Siren {
    private Siren() {}

    /**
     * Alle vier Ticks aufzurufen: startet zu Beginn jedes 80-Tick-Zyklus ein vier Sekunden langes
     * Auf und Ab der Sirene, damit gleichzeitige Aufrufer im Takt bleiben. Lautstärke 16 trägt etwa
     * 256 Blöcke.
     */
    public static void wail(ServerLevel level, Vec3 at, float volume) {
        if (level.getGameTime() % 80 >= 4) return;
        level.playSound(null, at.x, at.y, at.z, Sounds.SIREN, SoundSource.BLOCKS, volume, 1.0f);
    }

    /** Heult über einer gezündeten Bombe, bis ihre Lunte abgebrannt ist. */
    public static void warn(ServerLevel level, Vec3 at, int ticks) {
        Jobs.add(
                new Jobs.Job() {
                    private int age;

                    @Override
                    public ServerLevel level() {
                        return level;
                    }

                    @Override
                    public boolean tick() {
                        if (age % 4 == 0) wail(level, at, 16f);
                        return ++age >= ticks;
                    }
                });
    }
}
