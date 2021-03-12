package pokecube.legends.blocks.normalblocks;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DirectionalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import pokecube.legends.init.ItemInit;

public class GrassDistorticBlock extends DirectionalBlock
{
    public GrassDistorticBlock(final AbstractBlock.Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DirectionalBlock.FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder)
    {
        builder.add(DirectionalBlock.FACING);
    }

    @Override
    public BlockState rotate(final BlockState state, final Rotation rot)
    {
        return state.setValue(DirectionalBlock.FACING, rot.rotate(state.getValue(DirectionalBlock.FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(final BlockState state, final Mirror mirrorIn)
    {
        return state.rotate(mirrorIn.getRotation(state.getValue(DirectionalBlock.FACING)));
    }

    @Override
    public BlockState getStateForPlacement(final BlockItemUseContext context)
    {
        return this.defaultBlockState().setValue(DirectionalBlock.FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public boolean canSustainPlant(final BlockState state, final IBlockReader world, final BlockPos pos,
            final Direction direction, final IPlantable plantable)
    {
        return true;
    }

    @Override
    public void stepOn(final World world, final BlockPos pos, final Entity entity)
    {
        super.stepOn(world, pos, entity);
        {
            final java.util.HashMap<String, Object> $_dependencies = new java.util.HashMap<>();
            $_dependencies.put("entity", entity);
            GrassDistorticBlock.executeProcedure($_dependencies);
        }
    }

    public static void executeProcedure(final java.util.HashMap<String, Object> dependencies)
    {
        if (dependencies.get("entity") == null)
        {
            System.err.println("Failed to WalkBlockEffect!");
            return;
        }
        final Entity entity = (Entity) dependencies.get("entity");
        if (entity instanceof ServerPlayerEntity) if (((PlayerEntity) entity).inventory.armor.get(3).getItem() != new ItemStack(
                ItemInit.ULTRA_HELMET.get(), 1).getItem() || ((PlayerEntity) entity).inventory.armor.get(
                        2).getItem() != new ItemStack(ItemInit.ULTRA_CHESTPLATE.get(), 1).getItem()
                || ((PlayerEntity) entity).inventory.armor.get(1).getItem() != new ItemStack(
                        ItemInit.ULTRA_LEGGINGS.get(), 1).getItem()
                || ((PlayerEntity) entity).inventory.armor.get(0).getItem() != new ItemStack(
                        ItemInit.ULTRA_BOOTS.get(), 1).getItem()) ((LivingEntity) entity).addEffect(new EffectInstance(Effects.MOVEMENT_SPEED, 120, 2));
    }
}
