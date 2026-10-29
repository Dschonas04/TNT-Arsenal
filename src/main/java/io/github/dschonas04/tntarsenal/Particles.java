// Autor: Dschonas04 (https://github.com/Dschonas04)

package io.github.dschonas04.tntarsenal;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Die eigenen Partikel der Mod, gezeichnet von der Client-Seite (siehe ArsenalParticlesClient), und
 * die Bildschirmeffekte, die eine große Detonation an die Spieler in der Nähe schickt: Die Kamera
 * wackelt, und der Bildschirm nimmt eine Tönung an.
 */
public final class Particles {
    /** Ein heller Funke, der von einer brennenden Lunte fliegt. */
    public static final SimpleParticleType FUSE_SPARK = register("fuse_spark", false);

    /**
     * Ein Ring flach auf dem Boden, der nach außen rast; seine Geschwindigkeit x ist der Radius,
     * den er erreicht.
     */
    public static final SimpleParticleType SHOCKWAVE = register("shockwave", true);

    /** Ein großer Rauchball, der anschwillt, aufsteigt und dünner wird. */
    public static final SimpleParticleType SMOKE_BALL = register("smoke_ball", true);

    /** Eine glühende Glut, die aufsteigt, schwankt und von Gelb zu Dunkelrot abkühlt. */
    public static final SimpleParticleType EMBER = register("ember", false);

    /** Ein Fetzen buntes Papier, der herabtaumelt. */
    public static final SimpleParticleType CONFETTI = register("confetti", false);

    /** Ein eisiges Funkeln. */
    public static final SimpleParticleType FROST_SPARKLE = register("frost_sparkle", false);

    /**
     * Eine dunkle Flocke, die spiralförmig in den Punkt zieht, auf den ihre Geschwindigkeit zeigt.
     */
    public static final SimpleParticleType VOID_SWIRL = register("void_swirl", false);

    /** Eine grüne, pulsierende Strahlungsflocke. */
    public static final SimpleParticleType RADIATION = register("radiation", false);

    /** Ein knisternder elektrischer Bogen. */
    public static final SimpleParticleType ARC = register("arc", false);

    private Particles() {}

    private static SimpleParticleType register(String name, boolean alwaysShow) {
        return Registry.register(
                BuiltInRegistries.PARTICLE_TYPE,
                TntArsenal.id(name),
                FabricParticleTypes.simple(alwaysShow));
    }

    static void init() {
        PayloadTypeRegistry.clientboundPlay().register(ScreenEffect.TYPE, ScreenEffect.CODEC);
    }

    /**
     * Wackeln (0..1) und Tönung (ARGB, Alpha = Stärke) für einen Spieler; der Client blendet beides
     * aus.
     */
    public record ScreenEffect(float shake, int tint, int ticks) implements CustomPacketPayload {
        public static final Type<ScreenEffect> TYPE = new Type<>(TntArsenal.id("screen_effect"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ScreenEffect> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.FLOAT,
                        ScreenEffect::shake,
                        ByteBufCodecs.INT,
                        ScreenEffect::tint,
                        ByteBufCodecs.VAR_INT,
                        ScreenEffect::ticks,
                        ScreenEffect::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Schickt einen Bildschirmeffekt an alle im Umkreis {@code radius}, schwächer, je weiter sie
     * weg sind.
     */
    public static void screen(
            ServerLevel level, Vec3 at, double radius, float shake, int tint, int ticks) {
        for (ServerPlayer player : level.players()) {
            double d = Math.sqrt(player.distanceToSqr(at));
            if (d > radius || !ServerPlayNetworking.canSend(player, ScreenEffect.TYPE)) continue;
            float near = (float) (1 - d / radius);
            int alpha = (int) (((tint >>> 24) & 0xFF) * near);
            ServerPlayNetworking.send(
                    player,
                    new ScreenEffect(shake * near, (alpha << 24) | (tint & 0xFFFFFF), ticks));
        }
    }

    /** Ein Druckwellenring mit dem angegebenen Radius zu Füßen des TNT. */
    public static void ring(ServerLevel level, Vec3 at, double radius) {
        level.sendParticles(SHOCKWAVE, true, true, at.x, at.y + 0.1, at.z, 0, radius, 0, 0, 1);
    }
}
