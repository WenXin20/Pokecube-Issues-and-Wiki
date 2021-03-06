package pokecube.core.ai.tasks.utility;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;
import pokecube.core.PokecubeCore;
import pokecube.core.ai.tasks.AIBase;
import pokecube.core.interfaces.IMoveConstants.AIRoutine;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;
import pokecube.core.items.berries.ItemBerry;
import thut.lib.ItemStackTools;

/**
 * This IAIRunnable will result in the mob occasionally emptying its inventory
 * into an inventory near its home location. This, along with AIGatherStuff
 * allows using pokemobs for automatic harvesting and storage of berries and
 * dropped items.
 */
public class AIStoreStuff extends AIBase implements INBTSerializable<CompoundNBT>
{
    public static int COOLDOWN = 10;
    public static int MAXSIZE  = 100;

    // Inventory to store stuff in.
    public BlockPos storageLoc = null;
    // Inventory to check for berries
    public BlockPos berryLoc = null;
    // Inventory to pull stuff out of
    public BlockPos emptyInventory = null;
    // Side to store stuff in.
    public Direction storageFace = Direction.UP;
    // Side to emtpy things from.
    public Direction emptyFace               = Direction.UP;
    int              searchInventoryCooldown = 0;
    int              doStorageCooldown       = 0;

    public AIStoreStuff(IPokemob entity)
    {
        super(entity);
    }

    private BlockPos checkDir(IBlockReader world, Direction dir, BlockPos centre, Direction side)
    {
        if (centre == null) return null;
        if (dir != null) centre = centre.offset(dir);
        if (this.getInventory(world, centre, side) != null) return centre;
        return null;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt)
    {
        final CompoundNBT berry = nbt.getCompound("b");
        final CompoundNBT storage = nbt.getCompound("s");
        final CompoundNBT empty = nbt.getCompound("e");
        if (!berry.isEmpty()) this.berryLoc = new BlockPos(berry.getInt("x"), berry.getInt("y"), berry.getInt("z"));
        else this.berryLoc = null;
        if (!storage.isEmpty())
        {
            this.storageLoc = new BlockPos(storage.getInt("x"), storage.getInt("y"), storage.getInt("z"));
            this.storageFace = Direction.values()[storage.getByte("f")];
        }
        else this.storageLoc = null;
        if (!empty.isEmpty())
        {
            this.emptyInventory = new BlockPos(empty.getInt("x"), empty.getInt("y"), empty.getInt("z"));
            this.emptyFace = Direction.values()[empty.getByte("f")];
        }
        else this.emptyInventory = null;
    }

    private boolean doBerryCheck(IItemHandlerModifiable pokemobInv)
    {
        ItemStack stack = pokemobInv.getStackInSlot(2);
        boolean hasBerry = stack.getItem() instanceof ItemBerry;
        // Has a berry already, can pass through to storage check.
        if (hasBerry) return false;
        boolean freeSlot = false;
        int berrySlot = -1;
        int firstFreeSlot = -1;
        // Search inventory for free slots or berries.
        for (int i = 2; i < pokemobInv.getSlots(); i++)
        {
            final boolean test = (stack = pokemobInv.getStackInSlot(i)).isEmpty();
            if (test && firstFreeSlot == -1 && i != 2) firstFreeSlot = i;
            freeSlot = freeSlot || test;
            if (!hasBerry)
            {
                hasBerry = !test && stack.getItem() instanceof ItemBerry;
                if (hasBerry) berrySlot = i;
            }
        }
        // If you have a berry stack elsewhere, swap it into first slot in
        // inventory.
        if (berrySlot != -1)
        {
            final ItemStack stack1 = pokemobInv.getStackInSlot(berrySlot);
            pokemobInv.setStackInSlot(berrySlot, pokemobInv.getStackInSlot(2));
            pokemobInv.setStackInSlot(2, stack1);
            // Retrun false to allow storage check.
            return false;
        }
        // No room to pick up a berry if it wanted to, so can pass through
        // to
        // storage check.
        if (!freeSlot || firstFreeSlot == -1) return false;
        // No Berry Storage, so move to normal storage checks.
        if (!this.findBerryStorage(false)) return false;
        IItemHandlerModifiable berries = this.getInventory(this.world, this.berryLoc, null);
        // No Storage at berryLoc.
        if (berries == null && !this.findBerryStorage(true))
        {
            this.berryLoc = null;
            return false;
        }
        // Second pass to find storage.
        if (berries == null) berries = this.getInventory(this.world, this.berryLoc, null);
        if (berries == null) return false;
        // No Berries in storage.
        if (!this.hasItem(ItemBerry.class, berries)) return false;
        if (this.pokemob.getEntity().getPosition().distanceSq(this.berryLoc) > 9)
        {
            final double speed = this.entity.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getValue();
            this.pokemob.getEntity().getNavigator().tryMoveToXYZ(this.berryLoc.getX() + 0.5, this.berryLoc.getY() + 0.5,
                    this.berryLoc.getZ() + 0.5, speed);
            // We should be pathing to berries, so return true to stop other
            // storage tasks.
            if (PokecubeMod.debug) PokecubeCore.LOGGER.info(this.pokemob.getDisplayName()
                    .getUnformattedComponentText() + " Pathing to Berries at " + this.berryLoc);
            return true;
        }
        for (int i = 0; i < Math.min(berries.getSlots(), AIStoreStuff.MAXSIZE); i++)
        {
            stack = berries.getStackInSlot(i);
            if (stack.getItem() instanceof ItemBerry)
            {
                berries.setStackInSlot(i, ItemStack.EMPTY);
                pokemobInv.setStackInSlot(firstFreeSlot, pokemobInv.getStackInSlot(2));
                pokemobInv.setStackInSlot(2, stack);
                // Collected our berry, Can pass to storage now.
                if (PokecubeMod.debug) PokecubeCore.LOGGER.info(this.pokemob.getDisplayName()
                        .getUnformattedComponentText() + " Took " + stack);
                return false;
            }
        }
        return false;
    }

    private boolean doEmptyCheck(IItemHandlerModifiable pokemobInv)
    {
        // Return true here to make the cooldown not 5x, this means we don't
        // have a setting for empty, so no need to run this AI.
        if (!this.findEmptyStorage(false)) return false;
        boolean freeSlot = false;
        // Search inventory for free slots, ignore first slot, as berry storage
        // deals with it.
        int firstFreeSlot = -1;
        for (int i = 3; i < pokemobInv.getSlots() && !freeSlot; i++)
        {
            freeSlot = pokemobInv.getStackInSlot(i).isEmpty();
            if (freeSlot) firstFreeSlot = i;
        }
        // Can only pick up item if we have a free slot for it.
        if (!freeSlot || firstFreeSlot == -1) return false;
        IItemHandlerModifiable inventory = this.getInventory(this.world, this.emptyInventory, this.emptyFace);
        // No inventory to empty
        if (inventory == null && !this.findEmptyStorage(true))
        {
            this.emptyInventory = null;
            return false;
        }
        // Second pass to find storage.
        if (inventory == null) inventory = this.getInventory(this.world, this.emptyInventory, this.emptyFace);
        if (inventory == null) return false;
        // No items to empty
        if (!this.hasItem(Item.class, inventory)) return false;
        // Path to the inventory.
        if (this.pokemob.getEntity().getPosition().distanceSq(this.emptyInventory) > 9)
        {
            final double speed = this.entity.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getValue();
            this.pokemob.getEntity().getNavigator().tryMoveToXYZ(this.emptyInventory.getX() + 0.5, this.emptyInventory
                    .getY() + 0.5, this.emptyInventory.getZ() + 0.5, speed);
            // We should be pathing, so return true.
            if (PokecubeMod.debug) PokecubeCore.LOGGER.info(this.pokemob.getDisplayName()
                    .getUnformattedComponentText() + " Pathing to Pick Up at " + this.emptyInventory);
            return true;
        }
        boolean collected = false;
        int start = 0;
        inv:
        for (int slot = firstFreeSlot; slot < pokemobInv.getSlots(); slot++)
            if (pokemobInv.getStackInSlot(slot).isEmpty()) for (int i = start; i < Math.min(inventory.getSlots(),
                    AIStoreStuff.MAXSIZE); i++)
            {
                final ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty())
                {
                    inventory.setStackInSlot(i, ItemStack.EMPTY);
                    pokemobInv.setStackInSlot(slot, stack);
                    // Collected our item successfully
                    if (PokecubeMod.debug) PokecubeCore.LOGGER.info(this.pokemob.getDisplayName()
                            .getUnformattedComponentText() + " Took " + stack);
                    collected = true;
                    start = i + 1;
                    continue inv;
                }
            }
        return collected;
    }

    private boolean doStorageCheck(IItemHandlerModifiable pokemobInv)
    {
        boolean freeSlot = false;
        // Search inventory for free slots, ignore first slot, as berry storage
        // deals with it.
        for (int i = 3; i < pokemobInv.getSlots() && !freeSlot; i++)
            freeSlot = pokemobInv.getStackInSlot(i).isEmpty();
        // Only dump inventory if no free slots.
        if (freeSlot) return false;
        // No ItemStorage
        if (!this.findItemStorage(false)) return false;
        // check if should path to storage.
        if (this.pokemob.getEntity().getPosition().distanceSq(this.storageLoc) > 9)
        {
            final double speed = this.entity.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getValue();
            this.pokemob.getEntity().getNavigator().tryMoveToXYZ(this.storageLoc.getX() + 0.5, this.storageLoc.getY()
                    + 0.5, this.storageLoc.getZ() + 0.5, speed);
            // We should be pathing to storage here, so return true.
            if (PokecubeMod.debug) PokecubeCore.LOGGER.info(this.pokemob.getDisplayName()
                    .getUnformattedComponentText() + " Pathing to Storage at " + this.storageLoc);
            return true;
        }
        IItemHandlerModifiable storage = this.getInventory(this.world, this.storageLoc, this.storageFace);
        // if Somehow have no storage, should return false.
        if (storage == null && !this.findItemStorage(true)) return false;
        // Second pass to find storage.
        if (storage == null) storage = this.getInventory(this.world, this.storageLoc, this.storageFace);
        if (storage == null) return false;
        // Store every item after berry slot
        for (int i = 3; i < pokemobInv.getSlots(); i++)
        {
            ItemStack stack = pokemobInv.getStackInSlot(i);
            if (PokecubeMod.debug) PokecubeCore.LOGGER.info(this.pokemob.getDisplayName()
                    .getUnformattedComponentText() + " Storing " + stack);
            if (ItemStackTools.addItemStackToInventory(stack, storage, 0))
            {
                if (stack.isEmpty()) stack = ItemStack.EMPTY;
                pokemobInv.setStackInSlot(i, stack);
            }
        }
        return false;
    }

    private boolean findBerryStorage(boolean refresh)
    {
        if (!refresh && this.berryLoc != null && this.pokemob.getGeneralState(GeneralStates.TAMED)) return true;
        if (this.berryLoc != null && refresh)
        {
            BlockPos found = this.checkDir(this.world, null, this.berryLoc, null);
            if (found == null) for (final Direction dir : Direction.Plane.HORIZONTAL)
            {
                found = this.checkDir(this.world, dir, this.berryLoc, null);
                if (found != null) break;
            }
            if (found == null) found = this.checkDir(this.world, Direction.DOWN, this.berryLoc, null);
            if (found == null) found = this.checkDir(this.world, Direction.UP, this.berryLoc, null);
            this.berryLoc = found;
        }
        return this.berryLoc != null;
    }

    private boolean findEmptyStorage(boolean refresh)
    {
        if (this.emptyInventory != null && refresh)
        {
            BlockPos found = this.checkDir(this.world, null, this.emptyInventory, this.emptyFace);
            if (found == null) for (final Direction dir : Direction.Plane.HORIZONTAL)
            {
                found = this.checkDir(this.world, dir, this.emptyInventory, this.emptyFace);
                if (found != null) break;
            }
            if (found == null) found = this.checkDir(this.world, Direction.DOWN, this.emptyInventory, this.emptyFace);
            if (found == null) found = this.checkDir(this.world, Direction.UP, this.emptyInventory, this.emptyFace);
            this.emptyInventory = found;
        }
        return this.emptyInventory != null && this.emptyInventory.distanceSq(this.pokemob.getHome()) < 256;
    }

    private boolean findItemStorage(boolean refresh)
    {
        if (!refresh && this.storageLoc != null && this.pokemob.getGeneralState(GeneralStates.TAMED)) return true;
        if (this.storageLoc != null && refresh)
        {
            BlockPos found = this.checkDir(this.world, null, this.storageLoc, this.storageFace);
            if (found == null) for (final Direction dir : Direction.Plane.HORIZONTAL)
            {
                found = this.checkDir(this.world, dir, this.storageLoc, this.storageFace);
                if (found != null) break;
            }
            if (found == null) found = this.checkDir(this.world, Direction.DOWN, this.storageLoc, this.storageFace);
            if (found == null) found = this.checkDir(this.world, Direction.UP, this.storageLoc, this.storageFace);
            this.storageLoc = found;
        }
        return this.storageLoc != null;
    }

    @Override
    public String getIdentifier()
    {
        return "store_stuff";
    }

    private IItemHandlerModifiable getInventory(IBlockReader world, BlockPos pos, Direction side)
    {
        if (pos == null) return null;
        if (this.pokemob.getOwner() instanceof PlayerEntity)
        {
            final PlayerEntity player = (PlayerEntity) this.pokemob.getOwner();
            final BreakEvent evt = new BreakEvent(player.getEntityWorld(), pos, world.getBlockState(pos), player);
            MinecraftForge.EVENT_BUS.post(evt);
            if (evt.isCanceled()) return null;
        }
        final TileEntity tile = world.getTileEntity(pos);
        if (tile == null) return null;
        if (tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side) instanceof IItemHandlerModifiable)
            return (IItemHandlerModifiable) tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
        return null;
    }

    private boolean hasItem(Class<? extends Item> tocheck, IItemHandlerModifiable inventory)
    {
        for (int i = 0; i < Math.min(inventory.getSlots(), AIStoreStuff.MAXSIZE); i++)
        {
            final ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && tocheck.isInstance(stack.getItem())) return true;
        }
        return false;
    }

    @Override
    public void reset()
    {
    }

    @Override
    public void run()
    {
    }

    @Override
    public CompoundNBT serializeNBT()
    {
        final CompoundNBT tag = new CompoundNBT();
        final CompoundNBT berry = new CompoundNBT();
        final CompoundNBT storage = new CompoundNBT();
        final CompoundNBT empty = new CompoundNBT();
        if (this.berryLoc != null)
        {
            berry.putInt("x", this.berryLoc.getX());
            berry.putInt("y", this.berryLoc.getY());
            berry.putInt("z", this.berryLoc.getZ());
        }
        if (this.storageLoc != null)
        {
            storage.putInt("x", this.storageLoc.getX());
            storage.putInt("y", this.storageLoc.getY());
            storage.putInt("z", this.storageLoc.getZ());
            storage.putByte("f", (byte) this.storageFace.ordinal());
        }
        if (this.emptyInventory != null)
        {
            empty.putInt("x", this.emptyInventory.getX());
            empty.putInt("y", this.emptyInventory.getY());
            empty.putInt("z", this.emptyInventory.getZ());
            empty.putByte("f", (byte) this.emptyFace.ordinal());
        }
        tag.put("b", berry);
        tag.put("s", storage);
        tag.put("e", empty);
        return tag;
    }

    @Override
    public boolean shouldRun()
    {
        if (!this.pokemob.isRoutineEnabled(AIRoutine.STORE) || this.pokemob.getHome() == null) return false;
        return true;
    }

    @Override
    public boolean sync()
    {
        return true;
    }

    /**
     * Only tame pokemobs set to "stay" should run this AI.
     *
     * @return
     */
    private boolean tameCheck()
    {
        return this.pokemob.getGeneralState(GeneralStates.TAMED) && !this.pokemob.getGeneralState(
                GeneralStates.STAYING);
    }

    @Override
    public void tick()
    {

        if (this.tameCheck() || !this.shouldRun()) return;
        boolean stuff = false;
        if (this.searchInventoryCooldown-- < 0)
        {
            this.searchInventoryCooldown = AIStoreStuff.COOLDOWN;
            this.findBerryStorage(false);
            stuff = this.findItemStorage(false);
            if (!stuff) this.searchInventoryCooldown = 50 * AIStoreStuff.COOLDOWN;
        }
        if (!stuff || this.doStorageCooldown-- > 0 || this.entity.getPosition().distanceSq(this.pokemob
                .getHome()) > 256) return;
        final IItemHandlerModifiable itemhandler = new InvWrapper(this.pokemob.getInventory());
        if (this.doBerryCheck(itemhandler) || this.doStorageCheck(itemhandler) || this.doEmptyCheck(itemhandler))
            this.doStorageCooldown = 5;
        else this.doStorageCooldown = 10 * AIStoreStuff.COOLDOWN;
    }
}
