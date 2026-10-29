// Autor: Dschonas04 (https://github.com/Dschonas04)

package io.github.dschonas04.tntarsenal.nuclear.radiation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Die Strahlung eines Chunks: die Dosisleistung in mSv pro Sekunde, wie sie zur Spielzeit {@code
 * since} war. Sie nimmt gleichmäßig ab und ist nach der eingestellten Lebensdauer (zwei
 * Minecraft-Tage) weg; der aktuelle Wert wird berechnet, wenn jemand fragt, nicht jeden Tick.
 */
public record ChunkRadiation(float value, long since) {
    public static final ChunkRadiation NONE = new ChunkRadiation(0f, 0L);

    public static final Codec<ChunkRadiation> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.FLOAT
                                                    .fieldOf("value")
                                                    .forGetter(ChunkRadiation::value),
                                            Codec.LONG
                                                    .fieldOf("since")
                                                    .forGetter(ChunkRadiation::since))
                                    .apply(i, ChunkRadiation::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkRadiation> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    ChunkRadiation::value,
                    ByteBufCodecs.VAR_LONG,
                    ChunkRadiation::since,
                    ChunkRadiation::new);

    /**
     * Die Dosisleistung bei {@code now}: fällt geradlinig von {@code value} auf null über {@code
     * lifetime} Ticks.
     */
    public float at(long now, long lifetime) {
        if (value <= 0) return 0f;
        return value * left(now - since, lifetime);
    }

    /**
     * Der Anteil einer Leistung, der {@code elapsed} Ticks später übrig ist: anfangs 1, 0, sobald
     * die Lebensdauer vorbei ist.
     */
    public static float left(long elapsed, long lifetime) {
        return (float) Math.max(0.0, 1.0 - (double) Math.max(0, elapsed) / Math.max(1, lifetime));
    }

    /**
     * Dieser Chunk mit zusätzlicher Strahlung auf dem, was jetzt übrig ist; die neue Summe hält
     * wieder eine volle Lebensdauer.
     */
    public ChunkRadiation plus(float more, long now, long lifetime) {
        return new ChunkRadiation(at(now, lifetime) + more, now);
    }
}
