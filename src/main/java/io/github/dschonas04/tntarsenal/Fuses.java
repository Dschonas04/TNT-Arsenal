package io.github.dschonas04.tntarsenal;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;

/**
 * A burning fuse that tells what is coming: every kind sparks in its own way — frost TNT snows,
 * fire TNT spits flames, the ender TNT shimmers purple — and the closer it gets to going off, the
 * more it sparks.
 */
final class Fuses {
    private Fuses() {}

    static ParticleOptions spark(Kind kind) {
        return switch (kind) {
            case GIGA, LAVA, VOLCANO -> ParticleTypes.LAVA;
            case FIRE, METEOR, TORCH, CHAIN -> ParticleTypes.FLAME;
            case LIGHTNING, MAGNET -> ParticleTypes.ELECTRIC_SPARK;
            case CLUSTER, FIREWORK, RAINBOW, FLAK -> ParticleTypes.FIREWORK;
            case DRILL, TUNNEL, LEVELER, MINER, STAIRCASE, BUNKER, EARTHQUAKE ->
                    ParticleTypes.DUST_PLUME;
            case WATER, SPONGE -> ParticleTypes.SPLASH;
            case STORM -> ParticleTypes.FALLING_WATER;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case POISON, CURSE -> ParticleTypes.WITCH;
            case GRAVITY, ANTIMATTER -> ParticleTypes.REVERSE_PORTAL;
            case ENDER, END -> ParticleTypes.PORTAL;
            case BOUNCE, SHOCKWAVE -> ParticleTypes.CLOUD;
            case HEALING, WOLF -> ParticleTypes.HEART;
            case BLACK_HOLE -> ParticleTypes.SQUID_INK;
            case ANTIGRAVITY, XRAY, STASIS -> ParticleTypes.END_ROD;
            case BRIDGE, WALL, DOME, PLATFORM, TOWER, LUMBER, GOLEM -> ParticleTypes.POOF;
            case HORDE, HALLOWEEN -> ParticleTypes.SOUL_FIRE_FLAME;
            case SONIC, SCULK -> ParticleTypes.SCULK_SOUL;
            case SMOKE -> ParticleTypes.CAMPFIRE_COSY_SMOKE;
            case NATURE, HARVEST -> ParticleTypes.HAPPY_VILLAGER;
            case NETHER -> ParticleTypes.ASH;
            case ARROW_RAIN -> ParticleTypes.CRIT;
            case COBWEB, DESERT -> ParticleTypes.WHITE_ASH;
            case BLESSING -> ParticleTypes.TOTEM_OF_UNDYING;
            case BEE -> ParticleTypes.DRIPPING_HONEY;
            case MEGA -> ParticleTypes.LARGE_SMOKE;
            case CONFETTI, DISCO, SHEEP -> ParticleTypes.FIREWORK;
            case CAKE -> ParticleTypes.SMALL_FLAME;
            case CHICKEN -> ParticleTypes.CLOUD;
        };
    }

    /**
     * How hard a kind hits, for the size of its shock wave and how much the screen shakes: 0 =
     * harmless, 3 = huge.
     */
    static int weight(Kind kind) {
        return switch (kind) {
            case GIGA, ANTIMATTER, BLACK_HOLE, EARTHQUAKE, VOLCANO, METEOR -> 3;
            case MEGA, CLUSTER, SHOCKWAVE, GRAVITY, STORM, LIGHTNING, NETHER, SONIC -> 2;
            case CONFETTI,
                    CAKE,
                    DISCO,
                    CHICKEN,
                    SHEEP,
                    HEALING,
                    BLESSING,
                    NATURE,
                    FIREWORK,
                    RAINBOW,
                    TORCH,
                    HARVEST,
                    BRIDGE,
                    WALL,
                    DOME,
                    PLATFORM,
                    TOWER,
                    STAIRCASE,
                    BUNKER,
                    WOLF,
                    GOLEM,
                    BEE,
                    XRAY ->
                    0;
            default -> 1;
        };
    }

    /** A screen tint for kinds with a mood of their own; 0 = none. */
    private static int tint(Kind kind) {
        return switch (kind) {
            case FROST, STASIS -> 0x5A9ED8FF;
            case POISON -> 0x5040C020;
            case CURSE -> 0x60401060;
            case HEALING, BLESSING -> 0x40FFD860;
            case FIRE, LAVA, VOLCANO, NETHER -> 0x50FF6010;
            case BLACK_HOLE, ANTIMATTER, ENDER, END -> 0x60200030;
            case SCULK -> 0x50002830;
            case LIGHTNING, STORM -> 0x60E0F0FF;
            default -> 0;
        };
    }

    /**
     * The moment it goes off: a flash, the kind's own sparks, a shock wave and smoke, whatever it
     * does next.
     */
    static void burst(ServerLevel level, PrimedTnt primed, Kind kind) {
        double x = primed.getX(), y = primed.getY() + 0.5, z = primed.getZ();
        Vec3 at = primed.position();
        int weight = weight(kind);
        level.sendParticles(
                ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF),
                x,
                y,
                z,
                1,
                0,
                0,
                0,
                0);
        level.sendParticles(spark(kind), x, y, z, 80, 0.6, 0.6, 0.6, 0.35);
        level.sendParticles(Particles.FUSE_SPARK, x, y, z, 30 + weight * 20, 0.3, 0.3, 0.3, 0.6);
        if (kind.party()) {
            level.sendParticles(Particles.CONFETTI, x, y + 0.5, z, 120, 0.6, 0.4, 0.6, 0.45);
        } else {
            Particles.ring(level, at, 3 + weight * 3.5);
            level.sendParticles(
                    Particles.SMOKE_BALL,
                    x,
                    y,
                    z,
                    4 + weight * 6,
                    0.8 + weight,
                    0.4 + weight * 0.4,
                    0.8 + weight,
                    0.02);
        }
        switch (kind) {
            case FIRE, LAVA, VOLCANO, METEOR, NETHER, GIGA, MEGA, CLUSTER ->
                    level.sendParticles(
                            Particles.EMBER, x, y, z, 80 + weight * 50, 1.2, 0.6, 1.2, 0.35);
            case FROST, STASIS ->
                    level.sendParticles(Particles.FROST_SPARKLE, x, y, z, 120, 3, 1.5, 3, 0.02);
            case LIGHTNING, STORM, MAGNET, SHOCKWAVE, SONIC ->
                    level.sendParticles(Particles.ARC, x, y, z, 40, 2.5, 1.5, 2.5, 0);
            case ENDER, END, BLACK_HOLE, ANTIMATTER, GRAVITY, ANTIGRAVITY, CURSE -> {
                // motes all around that spiral into the TNT: velocity = the way to the centre
                RandomSource random = level.getRandom();
                for (int i = 0; i < 80; i++) {
                    double dx = (random.nextDouble() - 0.5) * 10,
                            dy = random.nextDouble() * 4,
                            dz = (random.nextDouble() - 0.5) * 10;
                    level.sendParticles(
                            Particles.VOID_SWIRL, x + dx, y + dy, z + dz, 0, -dx, -dy, -dz, 1);
                }
            }
            default -> {}
        }
        if (weight >= 2)
            Particles.screen(level, at, 24 + weight * 16, 0.25f * weight, tint(kind), 20);
        else if (tint(kind) != 0) Particles.screen(level, at, 16, 0, tint(kind), 30);
    }

    /** Sparks over the TNT for as long as it burns. */
    static void burn(ServerLevel level, PrimedTnt primed, Kind kind) {
        ParticleOptions spark = spark(kind);
        Tasks.start(
                age -> {
                    if (!primed.isAlive()) return true;
                    if (age % 2 == 0) {
                        int count = primed.getFuse() < 20 ? 4 : 1;
                        level.sendParticles(
                                spark,
                                primed.getX(),
                                primed.getY() + 1.05,
                                primed.getZ(),
                                count,
                                0.2,
                                0.1,
                                0.2,
                                0.02);
                        level.sendParticles(
                                Particles.FUSE_SPARK,
                                primed.getX(),
                                primed.getY() + 1.05,
                                primed.getZ(),
                                count,
                                0.05,
                                0.02,
                                0.05,
                                0.12);
                    }
                    return age > 2400;
                });
    }
}
