// Autor: Dschonas04 (https://github.com/Dschonas04)

package io.github.dschonas04.tntarsenal.nuclear.network;

import io.github.dschonas04.tntarsenal.nuclear.NuclearTnt;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sagt einem Client, dass eine Bombe hochgegangen ist, damit er Blitz, Beben und Atompilz zeichnen
 * kann.
 */
public record NukeDetonatedPayload(BlockPos pos, int tier, int radius)
        implements CustomPacketPayload {
    public static final Type<NukeDetonatedPayload> TYPE = new Type<>(NuclearTnt.id("detonated"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NukeDetonatedPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    NukeDetonatedPayload::pos,
                    ByteBufCodecs.VAR_INT,
                    NukeDetonatedPayload::tier,
                    ByteBufCodecs.VAR_INT,
                    NukeDetonatedPayload::radius,
                    NukeDetonatedPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
