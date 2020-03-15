package pokecube.legends.blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import org.antlr.v4.runtime.misc.NotNull;

import javax.annotation.Nullable;

import static net.minecraft.util.math.shapes.VoxelShapes.combineAndSimplify;

public class SpaceCoreBlock extends Rotates implements IWaterLoggable
{
    private static final EnumProperty<TimeSpaceCorePart> HALF = EnumProperty.create("half", TimeSpaceCorePart.class);
    private static final Map<Direction, VoxelShape> TIME_SPACE_TOP = new HashMap<>();
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final DirectionProperty FACING = HorizontalBlock.HORIZONTAL_FACING;

    //Precise selection box
    private static final VoxelShape TIME_SPACE_BOTTOM = VoxelShapes.or(
      makeCuboidShape(4, 0, 4, 12, 15.5, 12),
      makeCuboidShape(0, 0, 4, 4, 2, 12),
      makeCuboidShape(2, 2, 4, 4, 4, 12),
      makeCuboidShape(4, 0, 0, 12, 2, 4),
      makeCuboidShape(4, 2, 12, 12, 4, 14),
      makeCuboidShape(4, 0, 12, 12, 2, 16),
      makeCuboidShape(4, 2, 2, 12, 4, 4),
      makeCuboidShape(12, 2, 4, 14, 4, 12),
      makeCuboidShape(12, 0, 4, 16, 2, 12))
      .simplify();

    static
    {
        TIME_SPACE_TOP.put(Direction.NORTH,
          combineAndSimplify(makeCuboidShape(2.5, 7, 7.75, 13.5, 8, 8.25),
            combineAndSimplify(makeCuboidShape(3, 6, 7.75, 13, 7, 8.25),
              combineAndSimplify(makeCuboidShape(3, 8, 7.75, 13, 9, 8.25),
                combineAndSimplify(makeCuboidShape(3.5, 4.5, 7.75, 12.5, 6, 8.25),
                  combineAndSimplify(makeCuboidShape(3.5, 9, 7.75, 12.5, 10.5, 8.25),
                    combineAndSimplify(makeCuboidShape(4, 3, 7.75, 12, 4.5, 8.25),
                      combineAndSimplify(makeCuboidShape(4, 10.5, 7.75, 12, 11.5, 8.25),
                        combineAndSimplify(makeCuboidShape(4.5, 2.5, 7.75, 11.5, 3, 8.25),
                          combineAndSimplify(makeCuboidShape(4.5, 11.5, 7.75, 11.5, 12.5, 8.25),
                            combineAndSimplify(makeCuboidShape(5, 2, 7.75, 11, 2.5, 8.25),
                              combineAndSimplify(makeCuboidShape(5, 12.5, 7.75, 11, 13, 8.25),
                                combineAndSimplify(makeCuboidShape(5.5, 1.5, 7.75, 10.5, 2, 8.25),
                                  combineAndSimplify(makeCuboidShape(5.5, 13, 7.75, 10.5, 13.5, 8.25),
                                    combineAndSimplify(makeCuboidShape(6, 1, 7.75, 10, 1.5, 8.25),
                                      combineAndSimplify(makeCuboidShape(6, 13.5, 7.75, 10, 14, 8.25),
                                        combineAndSimplify(makeCuboidShape(7, 0.5, 7.75, 9, 1, 8.25),
                                          makeCuboidShape(7, 14, 7.75, 9, 14.5, 8.25),
                                          IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
                                    IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
                              IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
                        IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
                  IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
            IBooleanFunction.OR)
        );
        TIME_SPACE_TOP.put(Direction.SOUTH,
          combineAndSimplify(makeCuboidShape(2.5, 7, 7.75, 13.5, 8, 8.25),
            combineAndSimplify(makeCuboidShape(3, 6, 7.75, 13, 7, 8.25),
              combineAndSimplify(makeCuboidShape(3, 8, 7.75, 13, 9, 8.25),
                combineAndSimplify(makeCuboidShape(3.5, 4.5, 7.75, 12.5, 6, 8.25),
                  combineAndSimplify(makeCuboidShape(3.5, 9, 7.75, 12.5, 10.5, 8.25),
                    combineAndSimplify(makeCuboidShape(4, 3, 7.75, 12, 4.5, 8.25),
                      combineAndSimplify(makeCuboidShape(4, 10.5, 7.75, 12, 11.5, 8.25),
                        combineAndSimplify(makeCuboidShape(4.5, 2.5, 7.75, 11.5, 3, 8.25),
                          combineAndSimplify(makeCuboidShape(4.5, 11.5, 7.75, 11.5, 12.5, 8.25),
                            combineAndSimplify(makeCuboidShape(5, 2, 7.75, 11, 2.5, 8.25),
                              combineAndSimplify(makeCuboidShape(5, 12.5, 7.75, 11, 13, 8.25),
                                combineAndSimplify(makeCuboidShape(5.5, 1.5, 7.75, 10.5, 2, 8.25),
                                  combineAndSimplify(makeCuboidShape(5.5, 13, 7.75, 10.5, 13.5, 8.25),
                                    combineAndSimplify(makeCuboidShape(6, 1, 7.75, 10, 1.5, 8.25),
                                      combineAndSimplify(makeCuboidShape(6, 13.5, 7.75, 10, 14, 8.25),
                                        combineAndSimplify(makeCuboidShape(7, 0.5, 7.75, 9, 1, 8.25),
                                          makeCuboidShape(7, 14, 7.75, 9, 14.5, 8.25),
                                          IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
                                    IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
                              IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
                        IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
                  IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
            IBooleanFunction.OR)
        );
        TIME_SPACE_TOP.put(Direction.EAST,
          combineAndSimplify(makeCuboidShape(7.75, 7, 2.5, 8.25, 8, 13.5),
            combineAndSimplify(makeCuboidShape(7.75, 6, 3, 8.25, 7, 13),
              combineAndSimplify(makeCuboidShape(7.75, 8, 3, 8.25, 9, 13),
                combineAndSimplify(makeCuboidShape(7.75, 4.5, 3.5, 8.25, 6, 12.5),
                  combineAndSimplify(makeCuboidShape(7.75, 9, 3.5, 8.25, 10.5, 12.5),
                    combineAndSimplify(makeCuboidShape(7.75, 3, 4, 8.25, 4.5, 12),
                      combineAndSimplify(makeCuboidShape(7.75, 10.5, 4, 8.25, 11.5, 12),
                        combineAndSimplify(makeCuboidShape(7.75, 2.5, 4.5, 8.25, 3, 11.5),
                          combineAndSimplify(makeCuboidShape(7.75, 11.5, 4.5, 8.25, 12.5, 11.5),
                            combineAndSimplify(makeCuboidShape(7.75, 2, 5, 8.25, 2.5, 11),
                              combineAndSimplify(makeCuboidShape(7.75, 12.5, 5, 8.25, 13, 11),
                                combineAndSimplify(makeCuboidShape(7.75, 1.5, 5.5, 8.25, 2, 10.5),
                                  combineAndSimplify(makeCuboidShape(7.75, 13, 5.5, 8.25, 13.5, 10.5),
                                    combineAndSimplify(makeCuboidShape(7.75, 1, 6, 8.25, 1.5, 10),
                                      combineAndSimplify(makeCuboidShape(7.75, 13.5, 6, 8.25, 14, 10),
                                        combineAndSimplify(makeCuboidShape(7.75, 0.5, 7, 8.25, 1, 9),
                                          makeCuboidShape(7.75, 14, 7, 8.25, 14.5, 9),
                                          IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
                                    IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
                              IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
                        IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
                  IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
            IBooleanFunction.OR)
        );
        TIME_SPACE_TOP.put(Direction.WEST,
          combineAndSimplify(makeCuboidShape(7.75, 7, 2.5, 8.25, 8, 13.5),
            combineAndSimplify(makeCuboidShape(7.75, 6, 3, 8.25, 7, 13),
              combineAndSimplify(makeCuboidShape(7.75, 8, 3, 8.25, 9, 13),
                combineAndSimplify(makeCuboidShape(7.75, 4.5, 3.5, 8.25, 6, 12.5),
                  combineAndSimplify(makeCuboidShape(7.75, 9, 3.5, 8.25, 10.5, 12.5),
                    combineAndSimplify(makeCuboidShape(7.75, 3, 4, 8.25, 4.5, 12),
                      combineAndSimplify(makeCuboidShape(7.75, 10.5, 4, 8.25, 11.5, 12),
                        combineAndSimplify(makeCuboidShape(7.75, 2.5, 4.5, 8.25, 3, 11.5),
                          combineAndSimplify(makeCuboidShape(7.75, 11.5, 4.5, 8.25, 12.5, 11.5),
                            combineAndSimplify(makeCuboidShape(7.75, 2, 5, 8.25, 2.5, 11),
                              combineAndSimplify(makeCuboidShape(7.75, 12.5, 5, 8.25, 13, 11),
                                combineAndSimplify(makeCuboidShape(7.75, 1.5, 5.5, 8.25, 2, 10.5),
                                  combineAndSimplify(makeCuboidShape(7.75, 13, 5.5, 8.25, 13.5, 10.5),
                                    combineAndSimplify(makeCuboidShape(7.75, 1, 6, 8.25, 1.5, 10),
                                      combineAndSimplify(makeCuboidShape(7.75, 13.5, 6, 8.25, 14, 10),
                                        combineAndSimplify(makeCuboidShape(7.75, 0.5, 7, 8.25, 1, 9),
                                          makeCuboidShape(7.75, 14, 7, 8.25, 14.5, 9),
                                          IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
                                    IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
                              IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
                        IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
                  IBooleanFunction.OR), IBooleanFunction.OR), IBooleanFunction.OR),
            IBooleanFunction.OR)
        );
    }

    //Precise selection box
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context)
    {
        TimeSpaceCorePart half = state.get(HALF);
        if (half == TimeSpaceCorePart.BOTTOM)
        {
            return TIME_SPACE_BOTTOM;
        }
        else
        {
            return TIME_SPACE_TOP.get(state.get(FACING));
        }
    }

    //Default States
    public SpaceCoreBlock(final String name, final Material material, final float hardness, final float resistance,
                          final SoundType sound)
    {
        super(name, material, hardness, resistance, sound);
        this.setDefaultState(this.stateContainer.getBaseState()
          .with(HALF, TimeSpaceCorePart.BOTTOM)
          .with(FACING, Direction.NORTH)
          .with(WATERLOGGED, false));
    }

    //Places Time & Space Spawner with both top and bottom pieces
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack)
    {
        if (entity != null)
        {
            IFluidState fluidState = world.getFluidState(pos.up());
            world.setBlockState(pos.up(),
              state.with(HALF, TimeSpaceCorePart.TOP)
                .with(WATERLOGGED,  fluidState.getFluid() == Fluids.WATER), 3);
        }
    }

    //Breaking Time & Space Spawner breaks both parts and returns one item only
    public void onBlockHarvested(World world, @NotNull BlockPos pos, BlockState state, @NotNull PlayerEntity player)
    {
        Direction facing = state.get(FACING);
        BlockPos timeSpacePos = getTimeSpacePos(pos, state.get(HALF), facing);
        BlockState TimeSpaceBlockState = world.getBlockState(timeSpacePos);
        if (TimeSpaceBlockState.getBlock() == this && !pos.equals(timeSpacePos))
        {
            removeHalf(world, timeSpacePos, TimeSpaceBlockState);
        }
        BlockPos timeSpacePartPos = getTimeSpaceTopPos(timeSpacePos, facing);
        TimeSpaceBlockState = world.getBlockState(timeSpacePartPos);
        if (TimeSpaceBlockState.getBlock() == this && !pos.equals(timeSpacePartPos))
        {
            removeHalf(world, timeSpacePartPos, TimeSpaceBlockState);
        }
        super.onBlockHarvested(world, pos, state, player);
    }

    private BlockPos getTimeSpaceTopPos(BlockPos base, Direction facing)
    {
        switch (facing) {
            default:
                return base.up();
        }
    }

    private BlockPos getTimeSpacePos(BlockPos pos, TimeSpaceCorePart part, Direction facing)
    {
        if (part == TimeSpaceCorePart.BOTTOM) return pos;
        switch (facing) {
            default:
                return pos.down();
        }
    }

    //Breaking the Time & Space Spawner leaves water if underwater
    private void removeHalf(World world, BlockPos pos, BlockState state)
    {
        IFluidState fluidState = world.getFluidState(pos);
        if (fluidState.getFluid() == Fluids.WATER)
        {
            world.setBlockState(pos, fluidState.getBlockState(), 35);
        }
        else {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 35);
        }
    }

    //Prevents the Time & Space Spawner from replacing blocks above it and checks for water
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {
        IFluidState ifluidstate = context.getWorld().getFluidState(context.getPos());
        BlockPos pos = context.getPos();

        BlockPos timeSpacePos = getTimeSpaceTopPos(pos, context.getPlacementHorizontalFacing().getOpposite());
        if (pos.getY() < 255 &&
          timeSpacePos.getY() < 255 && context.getWorld().getBlockState(pos.up()).isReplaceable(context))
        {
            return  this.getDefaultState()
              .with(FACING, context.getPlacementHorizontalFacing().getOpposite())
              .with(HALF, TimeSpaceCorePart.BOTTOM)
              .with(WATERLOGGED, ifluidstate.isTagged(FluidTags.WATER) && ifluidstate.getLevel() == 8);
        }
        return null;
    }

    @Override
    protected void fillStateContainer(final StateContainer.Builder<Block, BlockState> builder)
    {
        builder.add(HALF, HorizontalBlock.HORIZONTAL_FACING, WATERLOGGED);
    }

    public SpaceCoreBlock(final String name, final Properties props)
    {
        super(name, props.tickRandomly());
    }

    @Override
    public void randomTick(final BlockState state, final World worldIn, final BlockPos pos, final Random random)
    {
        if (random.nextInt(100) == 0) worldIn.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
          SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 0.5F, random.nextFloat() * 0.4F + 0.8F,
          false);
    }
}
