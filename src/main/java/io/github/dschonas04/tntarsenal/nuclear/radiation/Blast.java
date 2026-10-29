package io.github.dschonas04.tntarsenal.nuclear.radiation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;

/**
 * Eine Detonation, wie die Welt sie sich merkt, damit Chunks, die damals nicht geladen waren, beim
 * Laden trotzdem ihren Anteil an der Strahlung bekommen.
 */
public record Blast(BlockPos center, int radius, float peak, long time) {
    public static final Codec<Blast> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            BlockPos.CODEC
                                                    .fieldOf("center")
                                                    .forGetter(Blast::center),
                                            Codec.INT.fieldOf("radius").forGetter(Blast::radius),
                                            Codec.FLOAT.fieldOf("peak").forGetter(Blast::peak),
                                            Codec.LONG.fieldOf("time").forGetter(Blast::time))
                                    .apply(i, Blast::new));

    /** Die Verseuchung reicht bis zum dreifachen Explosionsradius. */
    public int reach() {
        return radius * 3;
    }

    /**
     * Die Dosisleistung, die diese Explosion in der Mitte eines Chunks hinterlassen hat, zum
     * Zeitpunkt der Explosion.
     */
    public float rateAt(int chunkX, int chunkZ) {
        double dx = (chunkX << 4) + 8 - center.getX();
        double dz = (chunkZ << 4) + 8 - center.getZ();
        double fall = 1.0 - Math.sqrt(dx * dx + dz * dz) / reach();
        return fall <= 0 ? 0f : (float) (peak * fall * fall);
    }
}
