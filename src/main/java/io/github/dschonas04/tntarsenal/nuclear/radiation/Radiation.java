package io.github.dschonas04.tntarsenal.nuclear.radiation;

import io.github.dschonas04.tntarsenal.nuclear.NuclearTnt;
import io.github.dschonas04.tntarsenal.nuclear.Achievements;
import io.github.dschonas04.tntarsenal.nuclear.config.NukeConfig;
import io.github.dschonas04.tntarsenal.nuclear.protection.Protection;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import com.mojang.serialization.Codec;

/**
 * Radiation: a dose rate per chunk, a dose per player, and what the dose does.
 *
 * <p>Chunks carry their rate as an attachment that is saved with them and sent to
 * clients. A detonation raises the rate of every loaded chunk within three blast
 * radii and is remembered by the level; a chunk that loads later catches up on
 * the blasts it missed. Rates wane steadily and are gone after two Minecraft days
 * ({@code radiationTicks}).
 *
 * <p>Players carry their dose in mSv, saved and sent to their own client. Every
 * second they take in the rate of the chunk they stand in, less what the blocks
 * over their head and water around them shield. Out of the radiation the dose
 * goes down again, two percent a second (at least 1 mSv). What the dose does is
 * one effect, {@link RadiationEffect}, at four levels: from 100, 250, 500 and 1000 mSv.
 */
public final class Radiation {
    private Radiation() {
    }

    public static final AttachmentType<ChunkRadiation> CHUNK = AttachmentRegistry.create(NuclearTnt.id("chunk_radiation"),
            b -> b.persistent(ChunkRadiation.CODEC).syncWith(ChunkRadiation.STREAM_CODEC, AttachmentSyncPredicate.all()));
    public static final AttachmentType<Float> DOSE = AttachmentRegistry.create(NuclearTnt.id("dose"),
            b -> b.persistent(Codec.FLOAT).syncWith(ByteBufCodecs.FLOAT, AttachmentSyncPredicate.targetOnly()));
    public static final AttachmentType<List<Blast>> BLASTS = AttachmentRegistry.create(NuclearTnt.id("blasts"),
            b -> b.persistent(Blast.CODEC.listOf()));

    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, NuclearTnt.id("radiation"));
    public static Holder<MobEffect> EFFECT;

    public static void register() {
        EFFECT = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, NuclearTnt.id("radiation"), new RadiationEffect());
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> catchUp(level, chunk));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (ServerLevel level : server.getAllLevels()) {
                for (ServerPlayer player : level.players()) expose(level, player);
            }
        });
    }

    /** Fabric injects AttachmentTarget into these classes at runtime; without Loom the compiler does not know. */
    private static AttachmentTarget at(Object target) {
        return (AttachmentTarget) target;
    }

    private static long lifetime() {
        return NukeConfig.get().radiationTicks;
    }

    /** The current dose rate in the chunk at a position, in mSv per second. */
    public static float rateAt(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return 0f;
        ChunkRadiation radiation = at(level.getChunkAt(pos)).getAttached(CHUNK);
        return radiation == null ? 0f : radiation.at(level.getGameTime(), lifetime());
    }

    public static float dose(ServerPlayer player) {
        Float dose = at(player).getAttached(DOSE);
        return dose == null ? 0f : dose;
    }

    public static void setDose(ServerPlayer player, float dose) {
        if (dose <= 0) at(player).removeAttached(DOSE);
        else at(player).setAttached(DOSE, dose);
    }

    /** Contaminates the land around a detonation and remembers it for chunks loaded later. */
    public static void contaminate(ServerLevel level, BlockPos center, int radius, float peak) {
        long now = level.getGameTime();
        Blast blast = new Blast(center, radius, peak, now);
        List<Blast> blasts = new ArrayList<>(at(level).getAttachedOrElse(BLASTS, List.of()));
        // blasts whose radiation is over are forgotten
        blasts.removeIf(old -> ChunkRadiation.left(now - old.time(), lifetime()) <= 0);
        blasts.add(blast);
        at(level).setAttached(BLASTS, List.copyOf(blasts));
        int reach = blast.reach();
        for (int cx = (center.getX() - reach) >> 4; cx <= (center.getX() + reach) >> 4; cx++) {
            for (int cz = (center.getZ() - reach) >> 4; cz <= (center.getZ() + reach) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk != null) add(chunk, blast.rateAt(cx, cz), now);
            }
        }
    }

    private static void add(LevelChunk chunk, float rate, long now) {
        if (rate <= 0.001f) return;
        ChunkRadiation old = at(chunk).getAttachedOrElse(CHUNK, ChunkRadiation.NONE);
        at(chunk).setAttached(CHUNK, old.plus(rate, now, lifetime()));
        chunk.markUnsaved();
    }

    /**
     * Removes all radiation within {@code radius} blocks of a position: every chunk
     * there is loaded and set to zero, and blasts that went off there are forgotten
     * so that nothing comes back when a chunk loads later. Returns the chunks cleared.
     */
    public static int clear(ServerLevel level, BlockPos center, int radius) {
        long now = level.getGameTime();
        List<Blast> blasts = new ArrayList<>(at(level).getAttachedOrElse(BLASTS, List.of()));
        long r2 = (long) radius * radius;
        if (blasts.removeIf(b -> b.center().distSqr(center) <= r2)) at(level).setAttached(BLASTS, List.copyOf(blasts));
        int cleared = 0;
        for (int cx = (center.getX() - radius) >> 4; cx <= (center.getX() + radius) >> 4; cx++) {
            for (int cz = (center.getZ() - radius) >> 4; cz <= (center.getZ() + radius) >> 4; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                ChunkRadiation old = at(chunk).getAttached(CHUNK);
                if (old == null || old.at(now, lifetime()) <= 0f) continue;
                // zero as of now, so that catchUp does not replay older blasts
                at(chunk).setAttached(CHUNK, new ChunkRadiation(0f, now));
                chunk.markUnsaved();
                cleared++;
            }
        }
        return cleared;
    }

    /** A chunk that was unloaded during a blast gets that blast's radiation now, less what has waned since. */
    private static void catchUp(ServerLevel level, LevelChunk chunk) {
        List<Blast> blasts = at(level).getAttached(BLASTS);
        if (blasts == null || blasts.isEmpty()) return;
        ChunkRadiation current = at(chunk).getAttachedOrElse(CHUNK, ChunkRadiation.NONE);
        long now = level.getGameTime();
        float missed = 0f;
        for (Blast blast : blasts) {
            if (blast.time() <= current.since()) continue;
            float rate = blast.rateAt(chunk.getPos().x(), chunk.getPos().z());
            missed += rate * ChunkRadiation.left(now - blast.time(), lifetime());
        }
        if (missed > 0.001f) add(chunk, missed, now);
    }

    /** How much of the outside radiation reaches a player: roofs and water shield. */
    public static float shielding(ServerLevel level, ServerPlayer player) {
        BlockPos head = BlockPos.containing(player.getEyePosition());
        float factor = 1f;
        for (int dy = 1; dy <= 12; dy++) {
            BlockPos pos = head.above(dy);
            BlockState state = level.getBlockState(pos);
            if (state.is(Protection.LEAD_BLOCK)) factor *= 0.25f;
            else if (state.isCollisionShapeFullBlock(level, pos)) factor *= 0.7f;
        }
        if (player.isUnderWater()) factor *= 0.5f;
        return factor;
    }

    private static void expose(ServerLevel level, ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) return;
        float rate = rateAt(level, player.blockPosition()) * shielding(level, player) * Protection.factor(player);
        // Fallout rain washes the dust down on whoever stands in it.
        if (rate > 0.001f && level.isRainingAt(player.blockPosition().above())) rate *= 2f;
        float dose = dose(player);
        if (rate > 0.001f) dose += rate;
        else dose = Math.max(0f, dose - Math.max(1f, dose * 0.02f));
        setDose(player, dose);
        if (dose >= 500) Achievements.award(player, "survivor");
        if (dose >= 1000) Achievements.award(player, "glowing");
        int stage = stage(dose);
        // Refreshed every second while the dose is high enough; runs out by itself three seconds after.
        if (stage >= 0) player.addEffect(new MobEffectInstance(EFFECT, 60, stage, false, true, true));
    }

    /** The radiation effect's amplifier for a dose, or -1 below 100 mSv. */
    public static int stage(float dose) {
        return dose >= 1000 ? 3 : dose >= 500 ? 2 : dose >= 250 ? 1 : dose >= 100 ? 0 : -1;
    }

    /** Radiation sickness for a mob or a player without going through the dose, e.g. in fallout. */
    public static void sicken(LivingEntity entity, int amplifier, int ticks) {
        MobEffectInstance current = entity.getEffect(EFFECT);
        if (current != null && current.getAmplifier() > amplifier) return;
        entity.addEffect(new MobEffectInstance(EFFECT, ticks, amplifier, false, true, true));
    }

    public static DamageSource damage(ServerLevel level) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE));
    }
}
