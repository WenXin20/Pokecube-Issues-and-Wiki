package pokecube.legends.spawns;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import pokecube.core.PokecubeCore;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.database.stats.ISpecialCaptureCondition;
import pokecube.core.database.stats.ISpecialSpawnCondition;
import pokecube.core.handlers.PokecubePlayerDataHandler;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.utils.Tools;
import thut.api.maths.Vector3;

public class LegendarySpawn
{
    private static enum SpawnResult
    {
        SUCCESS, WRONGITEM, NOCAPTURE, NOSPAWN, FAIL;
    }

    private static List<LegendarySpawn> spawns = Lists.newArrayList();

    private static List<LegendarySpawn> getForBlock(final BlockState state)
    {
        final List<LegendarySpawn> matches = Lists.newArrayList();
        LegendarySpawn.spawns.forEach(s ->
        {
            if (s.targetBlockChecker.test(state)) matches.add(s);
        });
        return matches;
    }

    private static SpawnResult trySpawn(final LegendarySpawn spawn, final ItemStack stack,
            final PlayerInteractEvent.RightClickBlock evt)
    {
        final PlayerEntity playerIn = evt.getPlayer();
        final ServerWorld worldIn = (ServerWorld) evt.getWorld();
        final PokedexEntry entry = spawn.entry;
        if (!spawn.heldItemChecker.test(stack)) return SpawnResult.WRONGITEM;

        final ISpecialSpawnCondition spawnCondition = ISpecialSpawnCondition.spawnMap.get(entry);
        final ISpecialCaptureCondition captureCondition = ISpecialCaptureCondition.captureMap.get(entry);
        if (spawnCondition != null)
        {
            final Vector3 location = Vector3.getNewVector().set(evt.getPos());
            if (spawnCondition.canSpawn(playerIn, location, false))
            {
                MobEntity entity = PokecubeCore.createPokemob(entry, worldIn);
                final IPokemob pokemob = CapabilityPokemob.getPokemobFor(entity);
                if (captureCondition != null && !captureCondition.canCapture(playerIn, pokemob))
                {
                    evt.setCanceled(true);
                    return SpawnResult.NOCAPTURE;
                }
                PokecubePlayerDataHandler.getCustomDataTag(playerIn).putBoolean("spwn:" + entry.getTrimmedName(), true);
                entity.getPersistentData().putUniqueId("spwnedby:", playerIn.getUniqueID());
                entity.setHealth(entity.getMaxHealth());
                location.add(0, 1, 0).moveEntity(entity);
                spawnCondition.onSpawn(pokemob);
                playerIn.getHeldItemMainhand().setCount(0);
                worldIn.setBlockState(evt.getPos(), Blocks.AIR.getDefaultState());
                if (pokemob.getExp() < 100) entity = pokemob.setForSpawn(Tools.levelToXp(entry.getEvolutionMode(), 50))
                        .getEntity();
                worldIn.addEntity(entity);
                evt.setCanceled(true);
                return SpawnResult.SUCCESS;
            }
            else return SpawnResult.NOSPAWN;
        }
        return SpawnResult.FAIL;
    }

    @SubscribeEvent
    public static void livingDeath(final LivingDeathEvent evt)
    {
        if (!(evt.getEntity().getEntityWorld() instanceof ServerWorld)) return;
        // // Recall if it is a pokemob.
        final IPokemob attacked = CapabilityPokemob.getPokemobFor(evt.getEntity());
        if (attacked != null && attacked.getOwnerId() == null && evt.getEntity().getPersistentData().contains(
                "spwnedby:"))
        {
            ServerWorld world = (ServerWorld) evt.getEntity().getEntityWorld();
            world = world.getServer().getWorld(DimensionType.OVERWORLD);
            final UUID id = evt.getEntity().getPersistentData().getUniqueId("spwnedby:");
            PokecubePlayerDataHandler.getCustomDataTag(id).putLong("spwn_ded:" + attacked.getPokedexEntry()
                    .getTrimmedName(), world.getGameTime());

        }
    }

    /**
     * Check if we match a spawn condition, and if so, give appropriate
     * messages.
     *
     * @param evt
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void interactRightClickBlock(final PlayerInteractEvent.RightClickBlock evt)
    {
        if (!(evt.getWorld() instanceof ServerWorld)) return;
        final BlockState state = evt.getWorld().getBlockState(evt.getPos());
        final List<LegendarySpawn> matches = LegendarySpawn.getForBlock(state);
        if (matches.isEmpty()) return;
        final boolean repeated = evt.getPlayer().getPersistentData().getLong("pokecube_legends:msgtick") != evt
                .getWorld().getGameTime();
        if (!repeated || evt.getPlayer().isCrouching()) return;
        evt.getPlayer().getPersistentData().putLong("pokecube_legends:msgtick", evt.getWorld().getGameTime());

        evt.setCanceled(true);
        evt.setUseItem(Result.DENY);
        evt.setUseBlock(Result.DENY);
        ItemStack stack = evt.getItemStack();
        // Try both hands just incase.
        if (stack.isEmpty()) stack = evt.getPlayer().getHeldItemMainhand();
        if (stack.isEmpty()) stack = evt.getPlayer().getHeldItemOffhand();

        if (evt.getItemStack().isEmpty())
        {
            Collections.shuffle(matches);
            LegendarySpawn match = matches.get(0);
            PokedexEntry entry = match.entry;
            for (final LegendarySpawn match1 : matches)
            {
                match = match1;
                entry = match1.entry;
                final ISpecialSpawnCondition spawnCondition = ISpecialSpawnCondition.spawnMap.get(entry);
                final Vector3 location = Vector3.getNewVector().set(evt.getPos());
                if (spawnCondition.canSpawn(evt.getPlayer(), location, false)) break;
            }
            evt.getPlayer().sendMessage(new TranslationTextComponent("msg.noitem.info", new TranslationTextComponent(
                    match.entry.getUnlocalizedName())));
            evt.getPlayer().getPersistentData().putLong("pokecube_legends:msgtick", evt.getWorld().getGameTime());
            return;
        }

        boolean worked = false;
        SpawnResult result = SpawnResult.FAIL;
        final List<PokedexEntry> wrong_items = Lists.newArrayList();
        final List<PokedexEntry> wrong_biomes = Lists.newArrayList();
        for (final LegendarySpawn match : matches)
        {
            result = LegendarySpawn.trySpawn(match, stack, evt);
            worked = result == SpawnResult.SUCCESS;
            if (worked) break;

            if (result == SpawnResult.WRONGITEM) wrong_items.add(match.entry);
            if (result == SpawnResult.NOSPAWN) wrong_biomes.add(match.entry);
        }

        if (worked) return;

        if (wrong_items.size() == matches.size())
        {
            Collections.shuffle(wrong_items);
            evt.getPlayer().sendMessage(new TranslationTextComponent("msg.wrongitem.info", new TranslationTextComponent(
                    wrong_items.get(0).getUnlocalizedName())));
        }
        if (wrong_biomes.size() == matches.size())
        {
            Collections.shuffle(wrong_biomes);
            evt.getPlayer().sendMessage(new TranslationTextComponent("msg.nohere.info", new TranslationTextComponent(
                    wrong_items.get(0).getUnlocalizedName())));
        }

    }

    private final Predicate<ItemStack>  heldItemChecker;
    private final Predicate<BlockState> targetBlockChecker;
    private final PokedexEntry          entry;

    public LegendarySpawn(final String entry, final Item held, final Block target)
    {
        this(Database.getEntry(entry), (c) -> c.getItem() == held, (b) -> b.getBlock() == target);
    }

    public LegendarySpawn(final PokedexEntry entry, final Item held, final Block target)
    {
        this(entry, (c) -> c.getItem() == held, (b) -> b.getBlock() == target);
    }

    public LegendarySpawn(final PokedexEntry entry, final Predicate<ItemStack> heldItemChecker,
            final Predicate<BlockState> targetBlockChecker)
    {
        this.heldItemChecker = heldItemChecker;
        this.targetBlockChecker = targetBlockChecker;
        this.entry = entry;
        LegendarySpawn.spawns.add(this);
    }
}
