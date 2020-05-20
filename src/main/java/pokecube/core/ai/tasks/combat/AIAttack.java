package pokecube.core.ai.tasks.combat;

import org.apache.logging.log4j.Level;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.brain.BrainUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.util.FakePlayer;
import pokecube.core.PokecubeCore;
import pokecube.core.ai.brain.BrainUtils;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.pokemob.ai.CombatStates;
import pokecube.core.interfaces.pokemob.ai.GeneralStates;
import pokecube.core.items.pokecubes.EntityPokecubeBase;
import pokecube.core.moves.MovesUtils;
import thut.api.entity.ai.IAICombat;
import thut.api.maths.Matrix3;
import thut.api.maths.Vector3;

/**
 * This is the IAIRunnable for managing which attack is used when. It
 * determines whether the pokemob is in range, manages pathing to account for
 * range issues, and also manage auto selection of moves for wild or hunting
 * pokemobs.<br>
 * <br>
 * It also manages the message to notify the player that a wild pokemob has
 * decided to battle, as well as dealing with combat between rivals over a mate.
 * It is the one to queue the attack for the pokemob to perform.
 */
public class AIAttack extends FightTask implements IAICombat
{
    public static int maxWildBattleDur = 600;

    /** The target being attacked. */
    LivingEntity entityTarget;
    /** IPokemob version of entityTarget. */
    IPokemob     pokemobTarget;

    /** Where the target is/was for attack. */
    Vector3   targetLoc   = Vector3.getNewVector();
    /** Move we are using */
    Move_Base attack;
    Matrix3   targetBox   = new Matrix3();
    Matrix3   attackerBox = new Matrix3();

    /** Temp vectors for checking things. */
    Vector3 v  = Vector3.getNewVector();
    Vector3 v1 = Vector3.getNewVector();
    Vector3 v2 = Vector3.getNewVector();
    /** Speed for pathing. */
    double  movementSpeed;

    /** Used to determine when to give up attacking. */
    protected int chaseTime;
    /** Used for when to execute attacks. */
    protected int delayTime = -1;
    boolean       running   = false;

    int battleTime = 0;

    public AIAttack(final IPokemob mob)
    {
        super(mob);
        this.movementSpeed = this.entity.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getValue() * 1.8;
        this.setMutex(3);
    }

    public boolean continueExecuting()
    {
        final IPokemob mobA = this.pokemob;
        final IPokemob mobB = this.pokemobTarget;

        if (mobB != null)
        {
            if (mobB.getCombatState(CombatStates.FAINTED)) return false;

            final boolean weTame = mobA.getOwnerId() == null;
            final boolean theyTame = mobB.getOwnerId() == null;
            final boolean weHunt = mobA.getCombatState(CombatStates.HUNTING);
            final boolean theyHunt = mobB.getCombatState(CombatStates.HUNTING);
            if (weTame == theyTame && !weTame && weHunt == theyHunt && !theyHunt)
            {
                final float weHealth = mobA.getEntity().getHealth() / mobA.getEntity().getMaxHealth();
                final float theyHealth = mobB.getEntity().getHealth() / mobB.getEntity().getMaxHealth();
                // Wild mobs shouldn't fight to the death unless hunting.
                if (weHealth < 0.5 || theyHealth < 0.5)
                {
                    mobA.setCombatState(CombatStates.MATEFIGHT, false);
                    mobB.setCombatState(CombatStates.MATEFIGHT, false);
                    return false;
                }
                // Give up if we took too long to fight.
                if (this.battleTime > AIAttack.maxWildBattleDur) return false;
            }
        }

        if (mobA.getCombatState(CombatStates.FAINTED)) return false;

        if (!this.entityTarget.isAlive() || this.entityTarget.getHealth() <= 0) return false;
        if (!this.entity.isAlive() || this.entity.getHealth() <= 0) return false;

        return this.pokemob.getCombatState(CombatStates.ANGRY);
    }

    @Override
    public void reset()
    {
        this.battleTime = 0;
        this.clearUseMove();
        AIFindTarget.deagro(this.entity);
    }

    private void setUseMove()
    {
        this.pokemob.setCombatState(CombatStates.EXECUTINGMOVE, true);
        BrainUtils.setMoveUseTarget(this.entity, this.targetLoc);
    }

    private void clearUseMove()
    {
        this.pokemob.setCombatState(CombatStates.EXECUTINGMOVE, false);
        BrainUtils.clearMoveUseTarget(this.entity);
    }

    @Override
    public void run()
    {
        this.battleTime++;
        // Check if the pokemob has an active move being used, if so return
        if (this.pokemob.getActiveMove() != null) return;

        this.attack = MovesUtils.getMoveFromName(this.pokemob.getMove(this.pokemob.getMoveIndex()));
        if (this.attack == null) this.attack = MovesUtils.getMoveFromName(IMoveConstants.DEFAULT_MOVE);

        if (!this.running)
        {

            if (!((this.attack.getAttackCategory() & IMoveConstants.CATEGORY_SELF) != 0) && !this.pokemob
                    .getGeneralState(GeneralStates.CONTROLLED)) this.setWalkTo(this.entityTarget.getPositionVec(),
                            this.movementSpeed, 0);
            this.targetLoc.set(this.entityTarget);
            this.chaseTime = 0;
            this.running = true;
            /**
             * Don't want to notify if the pokemob just broke out of a
             * pokecube.
             */
            final boolean previousCaptureAttempt = !EntityPokecubeBase.canCaptureBasedOnConfigs(this.pokemob);

            /**
             * Check if it should notify the player of agression, and do so if
             * it should.
             */
            if (!previousCaptureAttempt && PokecubeCore.getConfig().pokemobagresswarning
                    && this.entityTarget instanceof ServerPlayerEntity && !(this.entityTarget instanceof FakePlayer)
                    && !this.pokemob.getGeneralState(GeneralStates.TAMED) && ((PlayerEntity) this.entityTarget)
                            .getRevengeTarget() != this.entity && ((PlayerEntity) this.entityTarget)
                                    .getLastAttackedEntity() != this.entity)
            {
                final ITextComponent message = new TranslationTextComponent("pokemob.agress", this.pokemob
                        .getDisplayName().getFormattedText());
                try
                {
                    // Only send this once.
                    if (this.pokemob.getAttackCooldown() == 0) this.entityTarget.sendMessage(message);
                }
                catch (final Exception e)
                {
                    PokecubeCore.LOGGER.log(Level.WARN, "Error with message for " + this.entityTarget, e);
                }
                this.pokemob.setAttackCooldown(PokecubeCore.getConfig().pokemobagressticks);
            }
            return;
        }

        // Look at the target
        BrainUtil.lookAt(this.entity, this.entityTarget);

        // No executing move state with no target location.
        if (this.pokemob.getCombatState(CombatStates.EXECUTINGMOVE) && this.targetLoc.isEmpty()) this.clearUseMove();

        // If it has been too long since last seen the target, give up.
        if (this.chaseTime > 200)
        {
            this.pokemob.setCombatState(CombatStates.ANGRY, false);
            this.chaseTime = 0;
            if (PokecubeMod.debug) PokecubeCore.LOGGER.log(Level.INFO, "Too Long Chase, Forgetting Target: "
                    + this.entity + " " + this.entityTarget);
            // Send deagress message and put mob on cooldown.
            final ITextComponent message = new TranslationTextComponent("pokemob.deagress.timeout", this.pokemob
                    .getDisplayName().getFormattedText());
            try
            {
                this.entityTarget.sendMessage(message);
            }
            catch (final Exception e)
            {
                PokecubeCore.LOGGER.log(Level.WARN, "Error with message for " + this.entityTarget, e);
            }
            this.pokemob.setAttackCooldown(PokecubeCore.getConfig().pokemobagressticks);
            return;
        }

        Move_Base move = null;
        move = MovesUtils.getMoveFromName(this.pokemob.getMove(this.pokemob.getMoveIndex()));
        if (move == null) move = MovesUtils.getMoveFromName(IMoveConstants.DEFAULT_MOVE);
        double var1 = (double) (this.entity.getWidth() * 2.0F) * (this.entity.getWidth() * 2.0F);
        boolean distanced = false;
        final boolean self = (move.getAttackCategory() & IMoveConstants.CATEGORY_SELF) > 0;
        final double dist = this.entity.getDistanceSq(this.entityTarget.posX, this.entityTarget.posY,
                this.entityTarget.posZ);

        distanced = (move.getAttackCategory() & IMoveConstants.CATEGORY_DISTANCE) > 0;
        // Check to see if the move is ranged, contact or self.
        if (distanced) var1 = PokecubeCore.getConfig().rangedAttackDistance * PokecubeCore
                .getConfig().rangedAttackDistance;
        else if (PokecubeCore.getConfig().contactAttackDistance > 0)
        {
            var1 = PokecubeCore.getConfig().contactAttackDistance * PokecubeCore.getConfig().contactAttackDistance;
            distanced = true;
        }

        this.delayTime = this.pokemob.getAttackCooldown();
        final boolean canUseMove = MovesUtils.canUseMove(this.pokemob);
        if (!canUseMove) return;
        boolean shouldPath = this.delayTime <= 0;
        boolean inRange = false;

        // Checks to see if the target is in range.
        if (distanced) inRange = dist < var1;
        else inRange = MovesUtils.contactAttack(this.pokemob, this.entityTarget);

        if (self)
        {
            inRange = true;
            this.targetLoc.set(this.entity);
        }

        final boolean canSee = BrainUtil.canSee(this.entity.getBrain(), this.entityTarget);

        // If can't see, increment the timer for giving up later.
        if (!canSee)
        {
            this.chaseTime++;
            if (!this.pokemob.getCombatState(CombatStates.EXECUTINGMOVE)) this.targetLoc.set(this.entityTarget).addTo(0,
                    this.entityTarget.getHeight() / 2, 0);
            // Try to path to target if you can't see it, regardless of what
            // move you have selected.
            shouldPath = true;
        }
        else
        {
            // Otherwise set timer to 0, and if newly executing the move, set
            // the target location as a "aim". This aiming is done so that when
            // the move is fired, it is fired at the location, not the target,
            // giving option to dodge.
            this.chaseTime = 0;
            if (!this.pokemob.getCombatState(CombatStates.EXECUTINGMOVE)) this.targetLoc.set(this.entityTarget).addTo(0,
                    this.entityTarget.getHeight() / 2, 0);
        }

        final boolean isTargetDodging = this.pokemobTarget != null && this.pokemobTarget.getCombatState(
                CombatStates.DODGING);

        // If the target is not trying to dodge, and the move allows it,
        // then
        // set target location to where the target is now. This is so that
        // it can use the older postion set above, lowering the accuracy of
        // move use, allowing easier dodging.
        if (!isTargetDodging) this.targetLoc.set(this.entityTarget).addTo(0, this.entityTarget.getHeight() / 2, 0);

        boolean delay = false;
        // Check if the attack should, applying a new delay if this is the
        // case..
        if (inRange && canSee || self)
        {
            if (this.delayTime <= 0 && this.entity.addedToChunk)
            {
                this.delayTime = this.pokemob.getAttackCooldown();
                delay = canUseMove;
            }
            shouldPath = false;
            if (!self) this.setUseMove();
            else this.clearUseMove();
        }

        if (!(distanced || self))
        {
            this.setUseMove();
            this.pokemob.setCombatState(CombatStates.LEAPING, true);
        }

        // If all the conditions match, queue up an attack.
        if (!this.targetLoc.isEmpty() && delay && inRange)
        {
            // Tell the target no need to try to dodge anymore, move is fired.
            if (this.pokemobTarget != null) this.pokemobTarget.setCombatState(CombatStates.DODGING, false);
            // Swing arm for effect.
            if (this.entity.getHeldItemMainhand() != null) this.entity.swingArm(Hand.MAIN_HAND);
            // Apply the move.
            final float f = (float) this.targetLoc.distToEntity(this.entity);
            if (this.entity.addedToChunk)
            {
                if (this.entityTarget.isAlive()) this.pokemob.executeMove(this.entityTarget, this.targetLoc.copy(), f);
                // Reset executing move and no item use status now that we have
                // used a move.
                this.clearUseMove();
                this.pokemob.setCombatState(CombatStates.NOITEMUSE, false);
                this.targetLoc.clear();
                shouldPath = false;
                this.delayTime = this.pokemob.getAttackCooldown();
            }
        }
        // If there is a target location, and it should path to it, queue a path
        // for the mob.
        if (!this.targetLoc.isEmpty() && shouldPath) this.setWalkTo(this.entityTarget.getPositionVec(),
                this.movementSpeed, 0);
    }

    @Override
    public boolean shouldRun()
    {
        // If we do have the target, but are not angry, return false.
        if (!this.pokemob.getCombatState(CombatStates.ANGRY)) return false;

        final LivingEntity target = BrainUtils.getAttackTarget(this.entity);
        // No target, we can't do anything, so return false
        if (target == null) return false;
        // If either us, or target is dead, or about to be so (0 health) return
        // false
        if (!target.isAlive() || target.getHealth() <= 0 || this.pokemob.getHealth() <= 0 || !this.entity.isAlive())
            return false;

        if (target != this.entityTarget) this.pokemobTarget = CapabilityPokemob.getPokemobFor(target);
        this.entityTarget = target;

        if (!this.continueExecuting())
        {
            AIFindTarget.deagro(this.entity);
            return false;
        }

        return true;
    }

    @Override
    public void tick()
    {
        this.entity.getPersistentData().putLong("lastAttackTick", this.entity.getEntityWorld().getGameTime());
    }
}
