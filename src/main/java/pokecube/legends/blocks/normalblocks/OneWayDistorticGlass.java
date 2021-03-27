package pokecube.legends.blocks.normalblocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DirectionalBlock;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.DyeColor;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IWorldReader;

public class OneWayDistorticGlass extends StainedGlassBlock
{
    protected static final DirectionProperty FACING = DirectionalBlock.FACING;

    public OneWayDistorticGlass(final String name, DyeColor color, final Properties props)
    {
        super(color, props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }
    @Override
    public BlockState getStateForPlacement(final BlockItemUseContext context)
    {
        Direction direction = context.getNearestLookingDirection().getOpposite();
        return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, direction));
    }

	@Override
	public boolean shouldDisplayFluidOverlay(final BlockState state, final IBlockDisplayReader world, final BlockPos pos, final FluidState fluidstate) {
		return true;
	}

    @Override
    public float[] getBeaconColorMultiplier(BlockState state, IWorldReader world, BlockPos pos, BlockPos beaconPos) {
        return new float[]{0.97f, 0.45f, 0.24f};
    }
}