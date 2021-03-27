package pokecube.legends.blocks.normalblocks;

import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public class TallCrystallizedBush extends DoublePlantBlock implements IWaterLoggable
{
	private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

	public TallCrystallizedBush(final String name, final Properties props)
    {
        super(props);
		this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(WATERLOGGED, false)
			.setValue(HALF, DoubleBlockHalf.LOWER));
    }

	@Override
	public void entityInside(BlockState state, World world, BlockPos pos, Entity entity) {
		if (entity instanceof LivingEntity) {
			entity.makeStuckInBlock(state, new Vector3d(0.9D, 0.75D, 0.9D));
			if (!world.isClientSide && (entity.xOld != entity.getX() || entity.zOld != entity.getZ())) {
				double d0 = Math.abs(entity.getX() - entity.xOld);
				double d1 = Math.abs(entity.getZ() - entity.zOld);
				if (d0 >= 0.003000000026077032D || d1 >= 0.003000000026077032D) {
					entity.hurt(DamageSource.CACTUS, 1.0F);
				}
			}
		}
	}

	public boolean isPathfindable(BlockState state, IBlockReader worldIn, BlockPos pos, PathType path) {
		return false;
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder)
	{
		builder.add(HALF, WATERLOGGED);
	}

	@Override
	public void setPlacedBy(final World world, final BlockPos pos, final BlockState state,
							final LivingEntity placer, final ItemStack stack)
	{
		if (placer != null)
		{
			final FluidState fluidState = world.getFluidState(pos.above());
			world.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER)
				.setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER), 1);
		}
	}

	@Override
	public BlockState getStateForPlacement(final BlockItemUseContext context)
	{
		final FluidState ifluidstate = context.getLevel().getFluidState(context.getClickedPos());
		final BlockPos pos = context.getClickedPos();

		final BlockPos tallBushPos = this.getTallBushTopPos(pos);
		if (pos.getY() < 255 && tallBushPos.getY() < 255 && context.getLevel().getBlockState(pos.above()).canBeReplaced(context))
			return this.defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER)
			.setValue(WATERLOGGED, ifluidstate.is(FluidTags.WATER) && ifluidstate.getAmount() == 8);

		return null;
	}

	// Breaking leaves water if underwater
	private void removeHalf(final World world, final BlockPos pos, final BlockState state, PlayerEntity player)
	{
		BlockState blockstate = world.getBlockState(pos);
		final FluidState fluidState = world.getFluidState(pos);
		if (fluidState.getType() == Fluids.WATER) world.setBlock(pos, fluidState.createLegacyBlock(), 35);
		else
		{
			world.setBlock(pos, Blocks.AIR.defaultBlockState(), 35);
			world.levelEvent(player, 2001, pos, Block.getId(blockstate));
		}
	}
	@Override
	public void playerWillDestroy(final World world, final BlockPos pos, final BlockState state,
								  final PlayerEntity player)
	{
		final BlockPos tallBushPos = this.getTallBushPos(pos, state.getValue(HALF));
		BlockState tallBushBlockState = world.getBlockState(tallBushPos);
		if (tallBushBlockState.getBlock() == this && !pos.equals(tallBushPos)) this.removeHalf(world, tallBushPos,
			tallBushBlockState, player);
		final BlockPos tallBushPartPos = this.getTallBushTopPos(tallBushPos);
		tallBushBlockState = world.getBlockState(tallBushPartPos);
		if (tallBushBlockState.getBlock() == this && !pos.equals(tallBushPartPos)) this.removeHalf(world, tallBushPartPos,
			tallBushBlockState, player);
		super.playerWillDestroy(world, pos, state, player);
	}

	private BlockPos getTallBushTopPos(final BlockPos pos)
	{
		return pos.above();
	}

	private BlockPos getTallBushPos(final BlockPos pos, final DoubleBlockHalf part)
	{
		if (part == DoubleBlockHalf.LOWER) return pos;
		else return pos.below();
	}

	@Override
	public boolean mayPlaceOn(BlockState state, IBlockReader worldIn, BlockPos pos) {
		return state.isFaceSturdy(worldIn, pos, Direction.UP);
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState updateShape(final BlockState state, final Direction facing, final BlockState facingState, final IWorld world, final BlockPos currentPos,
								  final BlockPos facingPos)
	{
		if (state.getValue(WATERLOGGED)) world.getLiquidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
		return super.updateShape(state, facing, facingState, world, currentPos, facingPos);
	}

	// Adds Waterlogging
	@SuppressWarnings("deprecation")
	@Override
	public FluidState getFluidState(final BlockState state)
	{
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}
}