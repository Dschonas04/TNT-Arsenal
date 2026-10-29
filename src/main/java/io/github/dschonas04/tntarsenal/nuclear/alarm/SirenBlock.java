package io.github.dschonas04.tntarsenal.nuclear.alarm;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.Vec3;

/** Eine Sirene, die heult, solange sie ein Redstone-Signal bekommt. */
public class SirenBlock extends Block {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public SirenBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        update(state, level, pos);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighbor,
            Orientation orientation,
            boolean movedByPiston) {
        update(state, level, pos);
    }

    private void update(BlockState state, Level level, BlockPos pos) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;
        level.setBlock(pos, state.setValue(POWERED, powered), UPDATE_ALL);
        if (powered) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERED)) return;
        Siren.wail(level, Vec3.atCenterOf(pos), 8f);
        level.scheduleTick(pos, this, 4);
    }
}
