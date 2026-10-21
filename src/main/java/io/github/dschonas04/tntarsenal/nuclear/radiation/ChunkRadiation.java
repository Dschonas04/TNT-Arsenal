package io.github.dschonas04.tntarsenal.nuclear.radiation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The radiation of one chunk: the dose rate in mSv per second as it was at game
 * time {@code since}. It wanes steadily and is gone after the configured lifetime
 * (two Minecraft days); the current value is worked out when someone asks, not every tick.
 */
public record ChunkRadiation(float value, long since) {
    public static final ChunkRadiation NONE = new ChunkRadiation(0f, 0L);

    public static final Codec<ChunkRadiation> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("value").forGetter(ChunkRadiation::value),
            Codec.LONG.fieldOf("since").forGetter(ChunkRadiation::since)
    ).apply(i, ChunkRadiation::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkRadiation> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ChunkRadiation::value,
            ByteBufCodecs.VAR_LONG, ChunkRadiation::since,
            ChunkRadiation::new);

    /** The dose rate at {@code now}: falls in a straight line from {@code value} to nothing over {@code lifetime} ticks. */
    public float at(long now, long lifetime) {
        if (value <= 0) return 0f;
        return value * left(now - since, lifetime);
    }

    /** The share of a rate that is left {@code elapsed} ticks later: 1 at first, 0 once the lifetime is over. */
    public static float left(long elapsed, long lifetime) {
        return (float) Math.max(0.0, 1.0 - (double) Math.max(0, elapsed) / Math.max(1, lifetime));
    }

    /** This chunk with more radiation added on top of what is left now; the new total lasts a full lifetime again. */
    public ChunkRadiation plus(float more, long now, long lifetime) {
        return new ChunkRadiation(at(now, lifetime) + more, now);
    }
}
