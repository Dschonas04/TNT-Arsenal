package io.github.dschonas04.tntarsenal;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;

/**
 * A burning fuse that tells what is coming: every kind sparks in its own way —
 * frost TNT snows, fire TNT spits flames, the ender TNT shimmers purple — and
 * the closer it gets to going off, the more it sparks.
 */
final class Fuses {
    private Fuses() {
    }

    static ParticleOptions spark(Kind kind) {
        return switch (kind) {
            case GIGA, LAVA, VOLCANO -> ParticleTypes.LAVA;
            case FIRE, METEOR, TORCH, CHAIN -> ParticleTypes.FLAME;
            case LIGHTNING, MAGNET -> ParticleTypes.ELECTRIC_SPARK;
            case CLUSTER, FIREWORK, RAINBOW, FLAK -> ParticleTypes.FIREWORK;
            case DRILL, TUNNEL, LEVELER, MINER, STAIRCASE, BUNKER, EARTHQUAKE -> ParticleTypes.DUST_PLUME;
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
        };
    }

    /** The moment it goes off: a flash and a burst of the kind's own sparks, whatever it does next. */
    static void burst(ServerLevel level, PrimedTnt primed, Kind kind) {
        double x = primed.getX(), y = primed.getY() + 0.5, z = primed.getZ();
        level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), x, y, z, 1, 0, 0, 0, 0);
        level.sendParticles(spark(kind), x, y, z, 80, 0.6, 0.6, 0.6, 0.35);
        level.sendParticles(ParticleTypes.POOF, x, y, z, 20, 0.4, 0.3, 0.4, 0.08);
    }

    /** Sparks over the TNT for as long as it burns. */
    static void burn(ServerLevel level, PrimedTnt primed, Kind kind) {
        ParticleOptions spark = spark(kind);
        Tasks.start(age -> {
            if (!primed.isAlive()) return true;
            if (age % 2 == 0) {
                int count = primed.getFuse() < 20 ? 4 : 1;
                level.sendParticles(spark, primed.getX(), primed.getY() + 1.05, primed.getZ(), count, 0.2, 0.1, 0.2, 0.02);
            }
            return age > 2400;
        });
    }
}
