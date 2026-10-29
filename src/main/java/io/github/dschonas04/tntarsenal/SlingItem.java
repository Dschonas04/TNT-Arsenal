// Autor: Dschonas04 (https://github.com/Dschonas04)

package io.github.dschonas04.tntarsenal;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Die TNT-Schleuder. Rechtsklick wirft ein gezündetes TNT – das in der anderen Hand, sonst das
 * erste im Inventar, jede Arsenal-Art oder einfaches TNT, nie eine Atombombe. Es fliegt im Bogen
 * und geht hoch, wenn seine Lunte abgebrannt ist. Schleichend wirft man es klebend: Es haftet an
 * der ersten Kreatur, die es berührt, oder am Boden, wo es landet.
 */
public class SlingItem extends Item {
    private static final String STICKY_TAG = "tnt_arsenal.sticky";

    public SlingItem(Properties properties) {
        super(properties);
    }

    private static boolean throwable(ItemStack stack) {
        return stack.getItem() instanceof BlockItem block
                && block.getBlock() instanceof TntBlock
                && !(block.getBlock()
                        instanceof io.github.dschonas04.tntarsenal.nuclear.block.NukeBlock);
    }

    /** Das TNT zum Werfen: zuerst die andere Hand, dann das Inventar ab der Hotbar. */
    private static ItemStack ammo(Player player, InteractionHand hand) {
        ItemStack other =
                player.getItemInHand(
                        hand == InteractionHand.MAIN_HAND
                                ? InteractionHand.OFF_HAND
                                : InteractionHand.MAIN_HAND);
        if (throwable(other)) return other;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (throwable(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack sling = player.getItemInHand(hand);
        ItemStack ammo = ammo(player, hand);
        if (ammo.isEmpty()) {
            if (level instanceof ServerLevel)
                player.sendOverlayMessage(
                        Component.translatable("item.tnt_arsenal.tnt_sling.empty"));
            return InteractionResult.FAIL;
        }
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;

        BlockState state = ((BlockItem) ammo.getItem()).getBlock().defaultBlockState();
        Vec3 eye = player.getEyePosition();
        PrimedTnt tnt = new PrimedTnt(level, eye.x, eye.y - 0.3, eye.z, player);
        if (state.getBlock() instanceof ArsenalTntBlock arsenal) {
            tnt.setBlockState(state.setValue(ArsenalTntBlock.FACING, player.getDirection()));
            tnt.setFuse(Math.max(30, arsenal.kind().fuse()));
        } else {
            tnt.setBlockState(Blocks.TNT.defaultBlockState());
            tnt.setFuse(50);
        }
        tnt.setDeltaMovement(player.getLookAngle().scale(1.3).add(player.getDeltaMovement()));
        boolean sticky = player.isShiftKeyDown();
        if (sticky) tnt.addTag(STICKY_TAG);
        level.addFreshEntity(tnt);
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.TRIDENT_THROW.value(),
                SoundSource.PLAYERS,
                1.0f,
                0.7f);
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.TNT_PRIMED,
                SoundSource.PLAYERS,
                0.8f,
                1.4f);
        if (sticky) stick(server, tnt, player);

        if (!player.isCreative()) ammo.shrink(1);
        sling.hurtAndBreak(1, player, hand);
        player.getCooldowns().addCooldown(sling, 15);
        if (player instanceof ServerPlayer sp) sp.awardStat(Gimmicks.THROWN);
        return InteractionResult.SUCCESS;
    }

    /**
     * Klebendes TNT: reitet auf der ersten Kreatur, die es berührt, oder bleibt schlagartig liegen,
     * wo es auf den Boden oder eine Wand trifft.
     */
    private static void stick(ServerLevel level, PrimedTnt tnt, Player thrower) {
        Tasks.start(
                age -> {
                    if (!tnt.isAlive() || tnt.isPassenger()) return true;
                    AABB reach = tnt.getBoundingBox().inflate(0.4);
                    List<LivingEntity> hit =
                            level.getEntitiesOfClass(
                                    LivingEntity.class,
                                    reach,
                                    e -> e != thrower && e.isAlive() && !e.isSpectator());
                    if (!hit.isEmpty() && age > 2) {
                        Entity target = hit.get(0);
                        if (tnt.startRiding(target, true, true)) {
                            level.playSound(
                                    null,
                                    target.getX(),
                                    target.getY(),
                                    target.getZ(),
                                    SoundEvents.SLIME_SQUISH,
                                    SoundSource.PLAYERS,
                                    1.0f,
                                    1.0f);
                            level.sendParticles(
                                    ParticleTypes.ITEM_SLIME,
                                    target.getX(),
                                    target.getY() + 1,
                                    target.getZ(),
                                    12,
                                    0.3,
                                    0.3,
                                    0.3,
                                    0.05);
                            if (thrower instanceof ServerPlayer player)
                                Gimmicks.award(player, "sticky");
                        }
                        return true;
                    }
                    if (tnt.onGround() || tnt.horizontalCollision) {
                        tnt.setDeltaMovement(Vec3.ZERO);
                        tnt.setNoGravity(true);
                        level.playSound(
                                null,
                                tnt.getX(),
                                tnt.getY(),
                                tnt.getZ(),
                                SoundEvents.SLIME_SQUISH_SMALL,
                                SoundSource.PLAYERS,
                                1.0f,
                                1.0f);
                        return true;
                    }
                    return age > 200;
                });
    }
}
