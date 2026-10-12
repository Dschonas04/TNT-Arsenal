package io.github.dschonas04.tntarsenal.nuclear.alarm;

import io.github.dschonas04.tntarsenal.nuclear.Sounds;
import io.github.dschonas04.tntarsenal.nuclear.explosion.Jobs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * The air-raid siren: a tone that rises and falls over four seconds, synthesised
 * by tools/sounds.py. Siren blocks wail while powered;
 * every lit bomb sounds it over the land for as long as its fuse burns.
 */
public final class Siren {
    private Siren() {
    }

    /**
     * To be called every four ticks: starts one four-second rise and fall of the siren at the beginning of
     * every 80-tick cycle, so callers in step stay in tune. Volume 16 carries about 256 blocks.
     */
    public static void wail(ServerLevel level, Vec3 at, float volume) {
        if (level.getGameTime() % 80 >= 4) return;
        level.playSound(null, at.x, at.y, at.z, Sounds.SIREN, SoundSource.BLOCKS, volume, 1.0f);
    }

    /** Wails over a lit bomb until its fuse has burnt down. */
    public static void warn(ServerLevel level, Vec3 at, int ticks) {
        Jobs.add(new Jobs.Job() {
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
