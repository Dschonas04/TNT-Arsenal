// Autor: Dschonas04 (https://github.com/Dschonas04)

package io.github.dschonas04.tntarsenal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Ein TNT-Block einer {@link Kind}. Er wird gezündet wie Vanilla-TNT – Redstone, Feuerzeug,
 * Feuerkugeln, brennende Pfeile, andere Explosionen –, aber die gezündete Entity trägt den Zustand
 * dieses Blocks. Deshalb wird sie mit der Textur dieses Blocks gezeichnet, und das Mixin erkennt
 * die Art, wenn die Lunte abgebrannt ist.
 */
public class ArsenalTntBlock extends TntBlock {
    /** Wohin der Spieler beim Setzen geschaut hat; nur die Tunnel-Art nutzt das. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final Kind kind;

    public ArsenalTntBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    /** Ersetzt den Block durch eine gezündete Kopie seiner selbst. */
    public void ignite(
            Level level, BlockPos pos, BlockState state, LivingEntity igniter, int fuse) {
        if (!(level instanceof ServerLevel)) return;
        PrimedTnt primed =
                new PrimedTnt(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, igniter);
        primed.setBlockState(state);
        primed.setFuse(fuse);
        level.addFreshEntity(primed);
        level.playSound(
                null,
                primed.getX(),
                primed.getY(),
                primed.getZ(),
                SoundEvents.TNT_PRIMED,
                SoundSource.BLOCKS,
                1.0f,
                1.0f);
        level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
        // Nur die Atombomben lösen die Sirene aus; jedes TNT hier sprüht stattdessen auf seine
        // eigene Art
        // Funken.
        Fuses.burn((ServerLevel) level, primed, kind);
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        if (!oldState.is(state.getBlock()) && level.hasNeighborSignal(pos)) {
            ignite(level, pos, state, null, kind.fuse());
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighbor,
            Orientation orientation,
            boolean movedByPiston) {
        if (level.hasNeighborSignal(pos)) {
            ignite(level, pos, state, null, kind.fuse());
            level.removeBlock(pos, false);
        }
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        // Der Block ist schon weg; ein neues mit kurzer Lunte zünden, wie Vanilla es tut.
        int fuse = PrimedTnt.getRandomShortFuse(kind.fuse(), level.getRandom());
        ignite(level, pos, defaultBlockState(), explosion.getIndirectSourceEntity(), fuse);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        ignite(level, pos, state, player, kind.fuse());
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        if (!player.isCreative()) {
            if (stack.is(Items.FLINT_AND_STEEL)) {
                stack.hurtAndBreak(1, player, hand);
            } else {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onProjectileHit(
            Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level instanceof ServerLevel && projectile.isOnFire()) {
            BlockPos pos = hit.getBlockPos();
            Entity owner = projectile.getOwner();
            ignite(
                    level,
                    pos,
                    state,
                    owner instanceof LivingEntity living ? living : null,
                    kind.fuse());
            level.removeBlock(pos, false);
        }
    }
}
