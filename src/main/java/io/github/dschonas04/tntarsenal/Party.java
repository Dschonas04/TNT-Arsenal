package io.github.dschonas04.tntarsenal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Die Party-Arten: Sie zerstören nichts und verletzen niemanden, sie sind nur zum Spaß da. */
final class Party {
    private static final int[] COLOURS = {
        0xFF3B3B, 0xFFB12B, 0xFFE94A, 0x4BE36B, 0x3BC8FF, 0x6B5BFF, 0xE35BFF, 0xFFFFFF
    };

    private Party() {}

    /**
     * Ein Ton auf einer Notenblock-Stufe, 0 = Fis3 ... 24 = Fis5, wie ein so oft angeklickter
     * Notenblock.
     */
    private static void note(
            ServerLevel level, Vec3 at, SoundEvent instrument, int step, float volume) {
        level.playSound(
                null,
                at.x,
                at.y,
                at.z,
                instrument,
                net.minecraft.sounds.SoundSource.RECORDS,
                volume,
                (float) Math.pow(2.0, (step - 12) / 12.0));
    }

    private static DustParticleOptions dust(RandomSource random, float size) {
        return new DustParticleOptions(COLOURS[random.nextInt(COLOURS.length)], size);
    }

    /** Drei Wellen Konfetti schießen hoch und regnen herab, mit einer kleinen Fanfare. */
    static void confetti(ServerLevel level, PrimedTnt t) {
        Vec3 c = t.position();
        RandomSource random = level.getRandom();
        int[] fanfare = {6, 10, 13, 18};
        Tasks.start(
                age -> {
                    if (age % 12 == 0) {
                        level.playSound(
                                null,
                                c.x,
                                c.y,
                                c.z,
                                SoundEvents.FIREWORK_ROCKET_BLAST,
                                net.minecraft.sounds.SoundSource.BLOCKS,
                                2.0f,
                                1.2f);
                        for (int i = 0; i < 160; i++) {
                            level.sendParticles(
                                    dust(random, 1.6f),
                                    c.x,
                                    c.y + 1,
                                    c.z,
                                    0,
                                    (random.nextDouble() - 0.5) * 1.2,
                                    1.0 + random.nextDouble(),
                                    (random.nextDouble() - 0.5) * 1.2,
                                    0.6);
                        }
                        level.sendParticles(
                                ParticleTypes.FIREWORK, c.x, c.y + 1, c.z, 40, 0.5, 0.5, 0.5, 0.25);
                    }
                    if (age % 4 == 0 && age / 4 < fanfare.length)
                        note(level, c, SoundEvents.NOTE_BLOCK_BELL.value(), fanfare[age / 4], 2.0f);
                    // das Konfetti treibt über eine weite Fläche herab
                    level.sendParticles(Particles.CONFETTI, c.x, c.y + 8, c.z, 14, 8, 2, 8, 0.02);
                    if (age % 12 == 0)
                        level.sendParticles(
                                Particles.CONFETTI, c.x, c.y + 1, c.z, 200, 0.5, 0.5, 0.5, 0.7);
                    return age >= 60;
                });
    }

    /** Ein Ring aus Kuchen mit brennenden Kerzen und „Happy Birthday“ auf der Harfe. */
    static void cake(ServerLevel level, PrimedTnt t) {
        Vec3 c = t.position();
        BlockPos center = t.blockPosition();
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2 * i / 6;
            BlockPos column =
                    center.offset(
                            (int) Math.round(Math.cos(angle) * 3),
                            0,
                            (int) Math.round(Math.sin(angle) * 3));
            BlockPos ground = Detonations.surface(level, column, 3, 4);
            if (ground != null)
                Detonations.place(
                        level,
                        ground.above(),
                        Blocks.CANDLE_CAKE.defaultBlockState().setValue(CandleCakeBlock.LIT, true));
        }
        level.sendParticles(ParticleTypes.FIREWORK, c.x, c.y + 1, c.z, 60, 1.5, 0.5, 1.5, 0.1);
        // Happy Birthday, alle vier Ticks ein Ton, eine Pause ist -1
        int[] song = {
            6, 6, 8, -1, 6, -1, 11, -1, 10, -1, -1, -1, 6, 6, 8, -1, 6, -1, 13, -1, 11, -1, -1, -1,
            6, 6, 18, -1, 15, -1, 11, -1, 10, -1, 8, -1, -1, -1, 16, 16, 15, -1, 11, -1, 13, -1, 11
        };
        Tasks.start(
                age -> {
                    if (age % 4 == 0) {
                        int step = song[age / 4];
                        if (step >= 0)
                            note(level, c, SoundEvents.NOTE_BLOCK_HARP.value(), step, 2.0f);
                    }
                    return age >= (song.length - 1) * 4;
                });
    }

    /** Fünfzehn Sekunden Disco: bunte Scheinwerfer, ein Beat, alle leuchten und hüpfen. */
    static void disco(ServerLevel level, PrimedTnt t) {
        Vec3 c = t.position();
        RandomSource random = level.getRandom();
        int[] bass = {6, 6, 13, 6, 11, 6, 13, 10};
        Tasks.start(
                age -> {
                    if (age % 5 == 0) {
                        for (int beam = 0; beam < 4; beam++) {
                            double angle = (age * 0.15) + beam * Math.PI / 2;
                            double x = c.x + Math.cos(angle) * 5, z = c.z + Math.sin(angle) * 5;
                            DustParticleOptions colour = dust(random, 2.0f);
                            for (int y = 0; y < 8; y++)
                                level.sendParticles(colour, x, c.y + y, z, 2, 0.15, 0.2, 0.15, 0);
                        }
                    }
                    if (age % 10 == 0) {
                        note(
                                level,
                                c,
                                SoundEvents.NOTE_BLOCK_BASS.value(),
                                bass[(age / 10) % bass.length],
                                2.5f);
                        note(level, c, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 12, 2.0f);
                    }
                    if (age % 10 == 5) note(level, c, SoundEvents.NOTE_BLOCK_HAT.value(), 12, 1.5f);
                    if (age % 20 == 0) {
                        for (LivingEntity dancer :
                                Detonations.around(level, c, LivingEntity.class, 10)) {
                            dancer.addEffect(
                                    new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
                            if (dancer.onGround()) {
                                dancer.setDeltaMovement(dancer.getDeltaMovement().add(0, 0.42, 0));
                                dancer.needsSync = true;
                            }
                        }
                    }
                    return age >= 300;
                });
    }

    /** Es regnet Hühner. */
    static void chickens(ServerLevel level, PrimedTnt t) {
        Vec3 c = t.position();
        RandomSource random = level.getRandom();
        Tasks.start(
                age -> {
                    if (age % 3 == 0) {
                        BlockPos at =
                                BlockPos.containing(
                                        c.x + (random.nextDouble() - 0.5) * 12,
                                        c.y + 14,
                                        c.z + (random.nextDouble() - 0.5) * 12);
                        if (level.getBlockState(at).isAir()) {
                            Chicken chicken =
                                    EntityTypes.CHICKEN.spawn(
                                            level, at, EntitySpawnReason.TRIGGERED);
                            if (chicken != null && random.nextInt(5) == 0) chicken.setBaby(true);
                        }
                        level.playSound(
                                null,
                                c.x,
                                c.y + 8,
                                c.z,
                                SoundEvents.CHICKEN_EGG,
                                net.minecraft.sounds.SoundSource.NEUTRAL,
                                1.5f,
                                0.8f + random.nextFloat() * 0.6f);
                    }
                    return age >= 48;
                });
        level.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 14, c.z, 80, 5, 0.5, 5, 0.02);
    }

    /** Jedes Schaf ringsum bekommt eine neue Farbe, und ein paar Regenbogenschafe kommen dazu. */
    static void sheep(ServerLevel level, PrimedTnt t) {
        Vec3 c = t.position();
        RandomSource random = level.getRandom();
        DyeColor[] colours = DyeColor.values();
        List<Sheep> flock = Detonations.around(level, c, Sheep.class, 24);
        for (Sheep sheep : flock) {
            DyeColor colour = colours[random.nextInt(colours.length)];
            sheep.setColor(colour);
            sheep.setSheared(false);
            level.sendParticles(
                    new BlockParticleOption(
                            ParticleTypes.BLOCK, Blocks.WOOL.pick(colour).defaultBlockState()),
                    sheep.getX(),
                    sheep.getY() + 0.8,
                    sheep.getZ(),
                    20,
                    0.4,
                    0.4,
                    0.4,
                    0.1);
        }
        for (int i = 0; i < 3; i++) {
            Sheep sheep =
                    EntityTypes.SHEEP.spawn(
                            level,
                            t.blockPosition().offset(i - 1, 0, 0),
                            EntitySpawnReason.TRIGGERED);
            if (sheep != null) sheep.setCustomName(Component.literal("jeb_"));
        }
        for (int i = 0; i < 4; i++) {
            Sheep sheep =
                    EntityTypes.SHEEP.spawn(
                            level,
                            t.blockPosition().offset(0, 0, i - 2),
                            EntitySpawnReason.TRIGGERED);
            if (sheep != null) sheep.setColor(colours[random.nextInt(colours.length)]);
        }
        level.playSound(
                null,
                c.x,
                c.y,
                c.z,
                SoundEvents.SHEEP_AMBIENT,
                net.minecraft.sounds.SoundSource.NEUTRAL,
                2.0f,
                1.2f);
        note(level, c, SoundEvents.NOTE_BLOCK_CHIME.value(), 18, 2.0f);
        level.sendParticles(ParticleTypes.FIREWORK, c.x, c.y + 1, c.z, 60, 2, 1, 2, 0.1);
    }
}
