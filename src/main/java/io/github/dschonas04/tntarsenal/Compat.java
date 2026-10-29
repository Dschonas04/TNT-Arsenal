package io.github.dschonas04.tntarsenal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;

/** What 26.2 offers directly but 26.1 does not: coloured block sets and the short TNT fuse. */
public final class Compat {

    private Compat() {
    }

    /** The wool block of a colour, like 26.2's {@code Blocks.WOOL.pick(colour)}. */
    public static Block wool(DyeColor colour) {
        return coloured(colour, "wool");
    }

    /** The concrete block of a colour, like 26.2's {@code Blocks.CONCRETE.pick(colour)}. */
    public static Block concrete(DyeColor colour) {
        return coloured(colour, "concrete");
    }

    private static Block coloured(DyeColor colour, String kind) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(colour.getName() + "_" + kind));
    }

    /** The fuse a TNT gets when another explosion sets it off: an eighth to three eighths of the normal one. */
    public static int randomShortFuse(int fuse, RandomSource random) {
        return random.nextInt(Math.max(1, fuse / 4)) + fuse / 8;
    }
}
