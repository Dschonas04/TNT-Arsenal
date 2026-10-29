// Autor: Dschonas04 (https://github.com/Dschonas04)

package io.github.dschonas04.tntarsenal.nuclear.explosion;

import io.github.dschonas04.tntarsenal.nuclear.Achievements;
import io.github.dschonas04.tntarsenal.nuclear.Sounds;
import io.github.dschonas04.tntarsenal.nuclear.block.Tier;
import io.github.dschonas04.tntarsenal.nuclear.config.NukeConfig;
import io.github.dschonas04.tntarsenal.nuclear.entity.PrimedNuke;
import io.github.dschonas04.tntarsenal.nuclear.network.NukeDetonatedPayload;
import io.github.dschonas04.tntarsenal.nuclear.radiation.Radiation;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.phys.Vec3;

/**
 * Was passiert, wenn die Lunte einer Bombe abgebrannt ist: der Explosionsauftrag, der wandernde
 * Knall und die Nachricht an die Clients.
 */
public final class Detonation {
    private Detonation() {}

    public static void start(ServerLevel level, PrimedNuke nuke) {
        int radius = NukeConfig.get().radius(nuke.tier());
        Vec3 origin = nuke.position();
        level.playSound(
                null,
                origin.x,
                origin.y,
                origin.z,
                Sounds.NUKE_BLAST,
                SoundSource.BLOCKS,
                8.0f,
                1.0f);
        Jobs.add(new NukeExplosion(level, nuke.blockPosition(), radius, nuke, nuke.igniter()));
        Jobs.add(new Bang(level, origin));
        Jobs.add(new Fallout(level, nuke.blockPosition(), radius));
        Radiation.contaminate(
                level, nuke.blockPosition(), radius, NukeConfig.get().peakRate(nuke.tier()));
        Emp.pulse(level, nuke.blockPosition(), radius);
        falloutRain(level, nuke.tier());
        if (nuke.tier() == Tier.TSAR) {
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(origin) < 1000 * 1000)
                    Achievements.award(player, "destroyer");
            }
        }
        io.github.dschonas04.tntarsenal.Particles.ring(level, origin, radius * 1.6);
        level.sendParticles(
                io.github.dschonas04.tntarsenal.Particles.RADIATION,
                true,
                true,
                origin.x,
                origin.y + 2,
                origin.z,
                300,
                radius * 0.5,
                radius * 0.25,
                radius * 0.5,
                0.05);
        level.sendParticles(
                io.github.dschonas04.tntarsenal.Particles.EMBER,
                true,
                true,
                origin.x,
                origin.y + 2,
                origin.z,
                400,
                radius * 0.3,
                radius * 0.2,
                radius * 0.3,
                0.4);
        NukeDetonatedPayload payload =
                new NukeDetonatedPayload(nuke.blockPosition(), nuke.tier().ordinal(), radius);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(origin) < 1000 * 1000
                    && ServerPlayNetworking.canSend(player, NukeDetonatedPayload.TYPE)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    /** Schwarzer Regen: Der Himmel öffnet sich über der Dimension, länger, je größer die Bombe. */
    private static void falloutRain(ServerLevel level, Tier tier) {
        if (!level.dimensionType().hasSkyLight()) return;
        int ticks =
                switch (tier) {
                    case MINI -> 2400;
                    case NUKE -> 6000;
                    case TSAR -> 12000;
                };
        WeatherData weather = level.getWeatherData();
        weather.setClearWeatherTime(0);
        weather.setRaining(true);
        weather.setRainTime(Math.max(weather.getRainTime(), ticks));
        weather.setDirty();
    }
}
