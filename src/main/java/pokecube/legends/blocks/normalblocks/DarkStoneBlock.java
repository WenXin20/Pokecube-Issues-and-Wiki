package pokecube.legends.blocks.normalblocks;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import pokecube.legends.blocks.BlockBase;
import pokecube.legends.init.ItemInit;

public class DarkStoneBlock extends BlockBase
{
    public DarkStoneBlock(final String name, final Material material, MaterialColor color)
    {
        super(name, Properties.of(material).sound(SoundType.STONE).strength(3, 8).harvestTool(
                ToolType.PICKAXE).harvestLevel(1));
    }

    @Override
    public void stepOn(final World world, final BlockPos pos, final Entity entity)
    {
        super.stepOn(world, pos, entity);
        {
            final java.util.HashMap<String, Object> $_dependencies = new java.util.HashMap<>();
            $_dependencies.put("entity", entity);
            DarkStoneBlock.executeProcedure($_dependencies);
        }
    }

    public static void executeProcedure(final java.util.HashMap<String, Object> dependencies)
    {
        if (dependencies.get("entity") == null)
        {
            System.err.println("Failed to WalkEffect!");
            return;
        }
        final Entity entity = (Entity) dependencies.get("entity");
        if (entity instanceof ServerPlayerEntity) {
        	if ((((PlayerEntity) entity).inventory.armor.get(3).getItem() != new ItemStack(ItemInit.ULTRA_HELMET.get(), 1).getItem()) ||
                (((PlayerEntity) entity).inventory.armor.get(2).getItem() != new ItemStack(ItemInit.ULTRA_CHESTPLATE.get(), 1).getItem()) ||
                (((PlayerEntity) entity).inventory.armor.get(1).getItem() != new ItemStack(ItemInit.ULTRA_LEGGINGS.get(), 1).getItem()) || 
                (((PlayerEntity) entity).inventory.armor.get(0).getItem() != new ItemStack(ItemInit.ULTRA_BOOTS.get(), 1).getItem())) 
            {
            	((LivingEntity) entity).addEffect(new EffectInstance(Effects.BLINDNESS, 120, 1));
           }
        }
    }
}
