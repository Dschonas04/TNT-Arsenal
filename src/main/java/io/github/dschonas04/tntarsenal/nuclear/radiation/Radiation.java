package io.github.dschonas04.tntarsenal.nuclear.radiation;

import com.mojang.serialization.Codec;

import io.github.dschonas04.tntarsenal.nuclear.Achievements;
import io.github.dschonas04.tntarsenal.nuclear.NuclearTnt;
import io.github.dschonas04.tntarsenal.nuclear.config.NukeConfig;
import io.github.dschonas04.tntarsenal.nuclear.protection.Protection;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Strahlung: eine Dosisleistung je Chunk, eine Dosis je Spieler und was die Dosis bewirkt.
 *
 * <p>Chunks tragen ihre Leistung als Attachment, das mit ihnen gespeichert und an die Clients
 * geschickt wird. Eine Detonation erhöht die Leistung jedes geladenen Chunks im Umkreis von drei
 * Explosionsradien und wird von der Welt gemerkt; ein später geladener Chunk holt die verpassten
 * Explosionen nach. Die Leistung nimmt gleichmäßig ab und ist nach zwei Minecraft-Tagen weg ({@code
 * radiationTicks}).
 *
 * <p>Spieler tragen ihre Dosis in mSv, gespeichert und an ihren eigenen Client geschickt. Jede
 * Sekunde nehmen sie die Leistung des Chunks auf, in dem sie stehen, abzüglich dessen, was die
 * Blöcke über ihrem Kopf und Wasser ringsum abschirmen. Außerhalb der Strahlung sinkt die Dosis
 * wieder, zwei Prozent pro Sekunde (mindestens 1 mSv). Was die Dosis bewirkt, ist ein Effekt,
 * {@link RadiationEffect}, in vier Stufen: ab 100, 250, 500 und 1000 mSv.
 */
public final class Radiation {
    private Radiation() {}

    public static final AttachmentType<ChunkRadiation> CHUNK =
            AttachmentRegistry.create(
                    NuclearTnt.id("chunk_radiation"),
                    b ->
                            b.persistent(ChunkRadiation.CODEC)
                                    .syncWith(
                                            ChunkRadiation.STREAM_CODEC,
                                            AttachmentSyncPredicate.all()));
    public static final AttachmentType<Float> DOSE =
            AttachmentRegistry.create(
                    NuclearTnt.id("dose"),
                    b ->
                            b.persistent(Codec.FLOAT)
                                    .syncWith(
                                            ByteBufCodecs.FLOAT,
                                            AttachmentSyncPredicate.targetOnly()));
    public static final AttachmentType<List<Blast>> BLASTS =
            AttachmentRegistry.create(
                    NuclearTnt.id("blasts"), b -> b.persistent(Blast.CODEC.listOf()));

    public static final ResourceKey<DamageType> DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, NuclearTnt.id("radiation"));
    public static Holder<MobEffect> EFFECT;

    public static void register() {
        EFFECT =
                Registry.registerForHolder(
                        BuiltInRegistries.MOB_EFFECT,
                        NuclearTnt.id("radiation"),
                        new RadiationEffect());
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> catchUp(level, chunk));
        ServerTickEvents.END_SERVER_TICK.register(
                server -> {
                    if (server.getTickCount() % 20 != 0) return;
                    for (ServerLevel level : server.getAllLevels()) {
                        for (ServerPlayer player : level.players()) expose(level, player);
                    }
                });
    }

    /**
     * Fabric fügt AttachmentTarget zur Laufzeit in diese Klassen ein; ohne Loom weiß der Compiler
     * das nicht.
     */
    private static AttachmentTarget at(Object target) {
        return (AttachmentTarget) target;
    }

    private static long lifetime() {
        return NukeConfig.get().radiationTicks;
    }

    /** Die aktuelle Dosisleistung im Chunk an einer Position, in mSv pro Sekunde. */
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

    /** Verseucht das Land um eine Detonation und merkt sie sich für später geladene Chunks. */
    public static void contaminate(ServerLevel level, BlockPos center, int radius, float peak) {
        long now = level.getGameTime();
        Blast blast = new Blast(center, radius, peak, now);
        List<Blast> blasts = new ArrayList<>(at(level).getAttachedOrElse(BLASTS, List.of()));
        // Explosionen, deren Strahlung vorbei ist, werden vergessen
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
     * Entfernt alle Strahlung im Umkreis von {@code radius} Blöcken um eine Position: Jeder Chunk
     * dort wird geladen und auf null gesetzt, und Explosionen dort werden vergessen, damit nichts
     * zurückkommt, wenn ein Chunk später lädt. Gibt die bereinigten Chunks zurück.
     */
    public static int clear(ServerLevel level, BlockPos center, int radius) {
        long now = level.getGameTime();
        List<Blast> blasts = new ArrayList<>(at(level).getAttachedOrElse(BLASTS, List.of()));
        long r2 = (long) radius * radius;
        if (blasts.removeIf(b -> b.center().distSqr(center) <= r2))
            at(level).setAttached(BLASTS, List.copyOf(blasts));
        int cleared = 0;
        for (int cx = (center.getX() - radius) >> 4; cx <= (center.getX() + radius) >> 4; cx++) {
            for (int cz = (center.getZ() - radius) >> 4;
                    cz <= (center.getZ() + radius) >> 4;
                    cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                ChunkRadiation old = at(chunk).getAttached(CHUNK);
                if (old == null || old.at(now, lifetime()) <= 0f) continue;
                // null ab jetzt, damit catchUp ältere Explosionen nicht erneut abspielt
                at(chunk).setAttached(CHUNK, new ChunkRadiation(0f, now));
                chunk.markUnsaved();
                cleared++;
            }
        }
        return cleared;
    }

    /**
     * Ein Chunk, der während einer Explosion entladen war, bekommt jetzt ihre Strahlung, abzüglich
     * dessen, was seither abgeklungen ist.
     */
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

    /** Wie viel der Strahlung von außen einen Spieler erreicht: Dächer und Wasser schirmen ab. */
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
        float rate =
                rateAt(level, player.blockPosition())
                        * shielding(level, player)
                        * Protection.factor(player);
        // Fallout-Regen wäscht den Staub auf jeden herab, der darin steht.
        if (rate > 0.001f && level.isRainingAt(player.blockPosition().above())) rate *= 2f;
        float dose = dose(player);
        if (rate > 0.001f) dose += rate;
        else dose = Math.max(0f, dose - Math.max(1f, dose * 0.02f));
        setDose(player, dose);
        if (dose >= 500) Achievements.award(player, "survivor");
        if (dose >= 1000) Achievements.award(player, "glowing");
        int stage = stage(dose);
        // Jede Sekunde aufgefrischt, solange die Dosis hoch genug ist; läuft drei Sekunden danach
        // von selbst
        // aus.
        if (stage >= 0)
            player.addEffect(new MobEffectInstance(EFFECT, 60, stage, false, true, true));
    }

    /** Die Effektstärke der Strahlung für eine Dosis, oder -1 unter 100 mSv. */
    public static int stage(float dose) {
        return dose >= 1000 ? 3 : dose >= 500 ? 2 : dose >= 250 ? 1 : dose >= 100 ? 0 : -1;
    }

    /**
     * Strahlenkrankheit für einen Mob oder Spieler, ohne über die Dosis zu gehen, z. B. im Fallout.
     */
    public static void sicken(LivingEntity entity, int amplifier, int ticks) {
        MobEffectInstance current = entity.getEffect(EFFECT);
        if (current != null && current.getAmplifier() > amplifier) return;
        entity.addEffect(new MobEffectInstance(EFFECT, ticks, amplifier, false, true, true));
    }

    public static DamageSource damage(ServerLevel level) {
        return new DamageSource(
                level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DAMAGE));
    }
}
