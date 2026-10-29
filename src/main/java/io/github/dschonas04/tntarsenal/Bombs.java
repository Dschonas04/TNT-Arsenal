package io.github.dschonas04.tntarsenal;

import io.github.dschonas04.tntarsenal.nuclear.block.NukeBlock;
import io.github.dschonas04.tntarsenal.nuclear.config.NukeConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ein Weg, alles zu zünden, was in dieser Mod explodiert: die Atombomben, jedes Arsenal-TNT und
 * einfaches Vanilla-TNT. Fernzünder und Zeitzünder laufen hier durch, damit sie bei allen
 * funktionieren.
 */
public final class Bombs {
    private Bombs() {}

    public static boolean isBomb(BlockState state) {
        return state.getBlock() instanceof NukeBlock || state.getBlock() instanceof TntBlock;
    }

    /**
     * Zündet die Bombe bei {@code pos}; {@code fuse} unter 1 heißt die eigene Lunte der Bombe. Gibt
     * false zurück, wenn dort keine ist.
     */
    public static boolean ignite(ServerLevel level, BlockPos pos, LivingEntity igniter, int fuse) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof NukeBlock bomb) {
            return bomb.ignite(
                    level, pos, igniter, fuse > 0 ? fuse : NukeConfig.get().fuse(bomb.tier()));
        }
        if (state.getBlock() instanceof ArsenalTntBlock tnt) {
            tnt.ignite(level, pos, state, igniter, fuse > 0 ? fuse : tnt.kind().fuse());
            level.removeBlock(pos, false);
            return true;
        }
        if (state.getBlock() instanceof TntBlock && TntBlock.prime(level, pos)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return true;
        }
        return false;
    }
}
