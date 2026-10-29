package io.github.dschonas04.tntarsenal.nuclear.explosion;

import io.github.dschonas04.tntarsenal.nuclear.NuclearTnt;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Der elektromagnetische Impuls einer Detonation, bis zum doppelten Explosionsradius: Zünder,
 * Zeitzünder und Geigerzähler im Inventar eines Spielers fallen eine Minute lang aus, die Augen
 * sind kurz geblendet, und leuchtende Redstone-Lampen und Kupferbirnen in Bodennähe gehen aus. Die
 * Lampen werden über viele Ticks abgeschaltet, ein paar Tausend Blöcke auf einmal.
 */
public final class Emp {
    private Emp() {}

    private static final int DEAD_TICKS = 1200;
    private static final int PER_TICK = 6000;
    private static final int HEIGHT = 12;

    public static void pulse(ServerLevel level, BlockPos center, int radius) {
        int reach = radius * 2;
        Component dead = Component.translatable("tnt_arsenal.emp").withStyle(ChatFormatting.AQUA);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(Vec3.atCenterOf(center)) > (double) reach * reach) continue;
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
            Inventory inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.is(NuclearTnt.DETONATOR)
                        || stack.is(NuclearTnt.GEIGER_COUNTER)
                        || stack.is(NuclearTnt.TIMER)) {
                    player.getCooldowns().addCooldown(stack, DEAD_TICKS);
                }
            }
            player.sendOverlayMessage(dead);
        }
        int lampReach = Math.min(reach, 96);
        Jobs.add(
                new Jobs.Job() {
                    private int index;
                    private final int side = lampReach * 2 + 1;
                    private final int total = side * side * (HEIGHT * 2 + 1);
                    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

                    @Override
                    public ServerLevel level() {
                        return level;
                    }

                    @Override
                    public boolean tick() {
                        int end = Math.min(total, index + PER_TICK);
                        for (; index < end; index++) {
                            int x = index % side - lampReach;
                            int z = (index / side) % side - lampReach;
                            int y = index / (side * side) - HEIGHT;
                            pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                            if (!level.isLoaded(pos)) continue;
                            BlockState state = level.getBlockState(pos);
                            if ((state.is(Blocks.REDSTONE_LAMP)
                                            || state.getBlock() instanceof CopperBulbBlock)
                                    && state.hasProperty(BlockStateProperties.LIT)
                                    && state.getValue(BlockStateProperties.LIT)) {
                                level.setBlock(
                                        pos, state.setValue(BlockStateProperties.LIT, false), 2);
                            }
                        }
                        return index >= total;
                    }
                });
    }

    /** Ob ein Gegenstand gerade durch einen Impuls ausgefallen ist. */
    public static boolean dead(ServerPlayer player, ItemStack stack) {
        return player.getCooldowns().isOnCooldown(stack);
    }
}
