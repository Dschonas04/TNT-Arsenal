package io.github.dschonas04.tntarsenal.nuclear.explosion;

import io.github.dschonas04.tntarsenal.nuclear.Sounds;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Der Knall, spät gehört: Schall legt 343 Blöcke pro Sekunde zurück, jeder Spieler hört ihn also,
 * wenn er ihn erreicht, leiser, je weiter er weg ist. Direkt neben dem Spieler abgespielt, damit er
 * auf jede Entfernung zu hören ist.
 */
public final class Bang implements Jobs.Job {
    private static final double SPEED = 343.0 / 20.0; // Blöcke pro Tick
    private static final double AUDIBLE = 1200;

    private final ServerLevel level;
    private final Vec3 origin;
    private final List<ServerPlayer> waiting;
    private final List<Integer> due = new ArrayList<>();
    private int age;

    public Bang(ServerLevel level, Vec3 origin) {
        this.level = level;
        this.origin = origin;
        this.waiting = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            double dist = Math.sqrt(player.distanceToSqr(origin));
            if (dist > AUDIBLE) continue;
            waiting.add(player);
            due.add((int) (dist / SPEED));
        }
    }

    @Override
    public ServerLevel level() {
        return level;
    }

    @Override
    public boolean tick() {
        for (int i = waiting.size() - 1; i >= 0; i--) {
            if (due.get(i) > age) continue;
            ServerPlayer player = waiting.remove(i);
            due.remove(i);
            double dist = Math.sqrt(player.distanceToSqr(origin));
            float volume = (float) Math.max(0.15, 1.0 - dist / AUDIBLE) * 4.0f;
            Vec3 toward = origin.subtract(player.position());
            Vec3 at =
                    player.getEyePosition()
                            .add(toward.lengthSqr() < 1 ? Vec3.ZERO : toward.normalize().scale(4));
            long seed = level.getRandom().nextLong();
            // weiter weg kommt der Donner tiefer an
            float pitch = (float) Math.max(0.7, 1.0 - dist / AUDIBLE * 0.3);
            player.connection.send(
                    new ClientboundSoundPacket(
                            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(Sounds.NUKE_BLAST),
                            SoundSource.BLOCKS,
                            at.x,
                            at.y,
                            at.z,
                            volume,
                            pitch,
                            seed));
        }
        age++;
        return waiting.isEmpty();
    }
}
