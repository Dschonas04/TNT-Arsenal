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
 * The mod's own particles, drawn by the client side (see ArsenalParticlesClient), and the screen
 * effects a big detonation sends to the players near it: the camera shakes and the screen takes on
 * a tint.
 */
public final class Particles {
    /** A bright spark that flies off a burning fuse. */
    public static final SimpleParticleType FUSE_SPARK = register("fuse_spark", false);

    /** A ring flat on the ground that races outwards; its velocity x is the radius it reaches. */
    public static final SimpleParticleType SHOCKWAVE = register("shockwave", true);

    /** A big ball of smoke that swells, rises and thins out. */
    public static final SimpleParticleType SMOKE_BALL = register("smoke_ball", true);

    /** A glowing ember that rises, sways and cools from yellow to dark red. */
    public static final SimpleParticleType EMBER = register("ember", false);

    /** A scrap of coloured paper that tumbles down. */
    public static final SimpleParticleType CONFETTI = register("confetti", false);

    /** An icy twinkle. */
    public static final SimpleParticleType FROST_SPARKLE = register("frost_sparkle", false);

    /** A dark mote that spirals into the point its velocity points at. */
    public static final SimpleParticleType VOID_SWIRL = register("void_swirl", false);

    /** A green, pulsing mote of radiation. */
    public static final SimpleParticleType RADIATION = register("radiation", false);

    /** A crackling electric arc. */
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

    /** Shake (0..1) and tint (ARGB, alpha = strength) for one player; the client fades both out. */
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
     * Sends a screen effect to everyone within {@code radius}, weaker the further away they are.
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

    /** A shock wave ring of the given radius at the TNT's feet. */
    public static void ring(ServerLevel level, Vec3 at, double radius) {
        level.sendParticles(SHOCKWAVE, true, true, at.x, at.y + 0.1, at.z, 0, radius, 0, 0, 1);
    }
}
