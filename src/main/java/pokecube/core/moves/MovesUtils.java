package pokecube.core.moves;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import pokecube.core.PokecubeCore;
import pokecube.core.database.moves.MoveEntry;
import pokecube.core.database.moves.MoveEntry.Category;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.IPokemob.Stats;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.interfaces.capabilities.CapabilityAffected;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.interfaces.entity.IOngoingAffected;
import pokecube.core.interfaces.entity.IOngoingAffected.IOngoingEffect;
import pokecube.core.interfaces.entity.impl.NonPersistantStatusEffect;
import pokecube.core.interfaces.entity.impl.NonPersistantStatusEffect.Effect;
import pokecube.core.interfaces.entity.impl.PersistantStatusEffect;
import pokecube.core.interfaces.entity.impl.StatEffect;
import pokecube.core.interfaces.pokemob.moves.MovePacket;
import pokecube.core.interfaces.pokemob.stats.DefaultModifiers;
import pokecube.core.interfaces.pokemob.stats.StatModifiers;
import pokecube.core.network.pokemobs.PacketPokemobMessage;
import pokecube.core.network.pokemobs.PacketSyncModifier;
import pokecube.core.utils.PokeType;
import thut.api.boom.ExplosionCustom;
import thut.api.maths.Vector3;
import thut.api.terrain.TerrainSegment;
import thut.core.common.commands.CommandTools;

public class MovesUtils implements IMoveConstants
{
    public static enum AbleStatus
    {
        ABLE, SLEEP, FREEZE, AIOFF, GENERICUNABLE;
    }

    public static Random rand = new Random();

    public static HashMap<String, Move_Base> moves = Maps.newHashMap();

    public static void addChange(Entity target, IPokemob attacker, byte change)
    {
        final IPokemob attacked = CapabilityPokemob.getPokemobFor(target);
        final boolean effect = CapabilityAffected.addEffect(target, new NonPersistantStatusEffect(Effect.getStatus(
                change)));
        if (attacked != null) if (change == IMoveConstants.CHANGE_CONFUSED) if (effect)
        {
            ITextComponent text;
            final String message = "pokemob.status.confuse.add";
            text = CommandTools.makeTranslatedMessage(message, "green", attacked.getDisplayName());
            if (attacked != attacker) attacker.displayMessageToOwner(text);
            text = CommandTools.makeTranslatedMessage(message, "red", attacked.getDisplayName());
            attacked.displayMessageToOwner(text);
            attacked.getEntity().playSound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1, 1);
        }
        else
        {
            ITextComponent text;
            final String message = "pokemob.move.stat.fail";
            text = CommandTools.makeTranslatedMessage(message, "red", attacked.getDisplayName());
            if (attacked != attacker) attacker.displayMessageToOwner(text);
            text = CommandTools.makeTranslatedMessage(message, "green", attacked.getDisplayName());
            attacked.displayMessageToOwner(text);
            attacked.getEntity().playSound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1, 1);
        }
    }

    /**
     * @param attacker
     * @return can attacker use its currently selected move.
     */
    public static boolean canUseMove(IPokemob attacker)
    {
        if (MovesUtils.isAbleToUseMoves(attacker) != AbleStatus.ABLE) return false;
        if (attacker.getAttackCooldown() <= 0)
        {
            final int index = attacker.getMoveIndex();
            if (index < 4 && index >= 0) if (attacker.getDisableTimer(index) <= 0) return true;
            else
            {
                for (int i = 0; i < 4; i++)
                    if (attacker.getDisableTimer(i) <= 0) return false;
                return true;
            }
            return true;
        }
        return false;
    }

    /**
     * For contact moves like tackle. The mob gets close to its target and
     * hits.
     *
     * @return whether the mob can attack
     */
    public static boolean contactAttack(IPokemob attacker, Entity attacked)
    {
        if (attacked == null || attacker == null) return false;
        double range = PokecubeCore.getConfig().contactAttackDistance;
        range = Math.max(attacker.getMobSizes().x, range);
        range = Math.max(1, range);
        return attacker.getEntity().getDistance(attacked) <= range;
    }

    /**
     * @param attacker
     * @param attacked
     * @param efficiency
     *            -1 = missed, -2 = failed, 0 = no effect, <1 = not effective, 1
     *            = normal effecive, >1 = supereffective
     * @param criticalRatio
     *            >1 = critical hit.
     */
    public static void displayEfficiencyMessages(IPokemob attacker, Entity attacked, float efficiency,
            float criticalRatio)
    {
        ITextComponent text;
        final IPokemob attackedPokemob = CapabilityPokemob.getPokemobFor(attacked);
        final Entity attackerMob = attacker.getEntity();
        if (efficiency == -1) if (attackedPokemob != null)
        {
            text = new TranslationTextComponent("pokemob.move.missed.theirs", attackedPokemob.getDisplayName());
            if (attacked != attackerMob) attacker.displayMessageToOwner(text);
            text = new TranslationTextComponent("pokemob.move.missed.ours", attackedPokemob.getDisplayName());
            attackedPokemob.displayMessageToOwner(text);
            return;
        }
        else if (attacked == null) if (attacker.getEntity().getAttackTarget() != null)
        {
            attacked = attacker.getEntity().getAttackTarget();
            final ITextComponent name = attacked.getName();
            text = new TranslationTextComponent("pokemob.move.missed.ours", name);
            attacker.displayMessageToOwner(text);
        }
        if (efficiency == -2) if (attackedPokemob != null)
        {
            final String message = "pokemob.move.failed";
            text = new TranslationTextComponent(message + ".theirs", attacker.getDisplayName());
            if (attacked != attackerMob) attacker.displayMessageToOwner(text);
            text = new TranslationTextComponent(message + ".ours", attacker.getDisplayName());
            attackedPokemob.displayMessageToOwner(text);
            return;
        }
        if (efficiency == 0)
        {
            if (attackedPokemob != null)
            {
                final String message = "pokemob.move.doesnt.affect";
                text = CommandTools.makeTranslatedMessage(message, "green", attackedPokemob.getDisplayName());
                if (attacked != attackerMob) attacker.displayMessageToOwner(text);
                text = CommandTools.makeTranslatedMessage(message, "red", attackedPokemob.getDisplayName());
                attackedPokemob.displayMessageToOwner(text);
                attacked.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 1, 1);
                return;
            }
        }
        else if (efficiency < 1)
        {
            if (attackedPokemob != null)
            {
                final String message = "pokemob.move.not.very.effective";
                text = CommandTools.makeTranslatedMessage(message, "green", attackedPokemob.getDisplayName());
                if (attacked != attackerMob) attacker.displayMessageToOwner(text);
                text = CommandTools.makeTranslatedMessage(message, "red", attackedPokemob.getDisplayName());
                attackedPokemob.displayMessageToOwner(text);
                attacked.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, 1, 1);
            }
        }
        else if (efficiency > 1) if (attackedPokemob != null)
        {
            final String message = "pokemob.move.super.effective";
            text = CommandTools.makeTranslatedMessage(message, "green", attackedPokemob.getDisplayName());
            if (attacked != attackerMob) attacker.displayMessageToOwner(text);
            text = CommandTools.makeTranslatedMessage(message, "red", attackedPokemob.getDisplayName());
            attackedPokemob.displayMessageToOwner(text);
            attacked.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 1, 1);
        }

        if (criticalRatio > 1) if (attackedPokemob != null)
        {
            text = CommandTools.makeTranslatedMessage("pokemob.move.critical.hit", "green", attackedPokemob
                    .getDisplayName());
            if (attacked != attackerMob) attacker.displayMessageToOwner(text);
            text = CommandTools.makeTranslatedMessage("pokemob.move.critical.hit", "red", attackedPokemob
                    .getDisplayName());
            attackedPokemob.displayMessageToOwner(text);
            attacked.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1, 1);
        }
    }

    public static void displayMoveMessages(IPokemob attacker, Entity attacked, String attack)
    {
        ITextComponent text;

        final IPokemob attackedPokemob = CapabilityPokemob.getPokemobFor(attacked);
        final Entity attackerMob = attacker.getEntity();
        if (attack.equals("pokemob.status.confusion"))
        {
            if (attackedPokemob != null)
            {
                text = CommandTools.makeTranslatedMessage("pokemob.status.confusion", "red", attackedPokemob
                        .getDisplayName());
                attackedPokemob.displayMessageToOwner(text);
            }
            return;
        }
        final ITextComponent attackName = new TranslationTextComponent(MovesUtils.getUnlocalizedMove(attack));
        text = CommandTools.makeTranslatedMessage("pokemob.move.used", "green", attacker.getDisplayName(),
                attackName);
        attacker.displayMessageToOwner(text);
        if (attackerMob == attacked) return;

        if (attackedPokemob != null)
        {
            text = CommandTools.makeTranslatedMessage("pokemob.move.enemyUsed", "red", attacker.getDisplayName(),
                    attackName);
            attackedPokemob.displayMessageToOwner(text);
        }
        else if (attacked instanceof PlayerEntity && !attacked.getEntityWorld().isRemote)
        {
            text = CommandTools.makeTranslatedMessage("pokemob.move.enemyUsed", "red", attacker.getDisplayName(),
                    attackName);
            PacketPokemobMessage.sendMessage((PlayerEntity) attacked, attacked.getEntityId(), text);
        }
    }

    public static void displayStatsMessage(IPokemob attacker, Entity attacked, float efficiency, byte stat, byte amount)
    {
        ITextComponent text;
        final IPokemob attackedPokemob = CapabilityPokemob.getPokemobFor(attacked);
        final Entity attackerMob = attacker.getEntity();
        if (efficiency == -2)
        {
            if (attackedPokemob != null)
            {
                if ((attacker == null || attackerMob == attacked) && attacked instanceof MobEntity)
                {
                    final IPokemob mob = CapabilityPokemob.getPokemobFor(((MobEntity) attacked).getAttackTarget());
                    if (mob != null) attacker = mob;
                }
                final String message = "pokemob.move.stat.fail";
                text = CommandTools.makeTranslatedMessage(message, "green", attackedPokemob.getDisplayName());
                if (attacked != attackerMob) attacker.displayMessageToOwner(text);
                text = CommandTools.makeTranslatedMessage(message, "red", attackedPokemob.getDisplayName());
                attackedPokemob.displayMessageToOwner(text);
            }
        }
        else
        {
            String message = "pokemob.move.stat";
            boolean fell = false;
            if (amount > 0)
            {
                message += ".fall" + amount;
                fell = true;
            }
            else message += ".rise" + -amount;
            final String statName = "pokemob.move.stat" + stat;

            if ((attacker == null || attackerMob == attacked) && attacked instanceof MobEntity)
            {
                final IPokemob mob = CapabilityPokemob.getPokemobFor(((MobEntity) attacked).getAttackTarget());
                if (mob != null) attacker = mob;
            }
            if (attackedPokemob != null && attacker != null)
            {
                if (attackerMob == attacked)
                {
                    final String colour = fell ? "red" : "green";
                    text = CommandTools.makeTranslatedMessage(message, colour, attackedPokemob.getDisplayName(),
                            statName);
                    attackedPokemob.displayMessageToOwner(text);
                }
                else
                {
                    String colour = fell ? "red" : "green";
                    text = CommandTools.makeTranslatedMessage(message, colour, attackedPokemob.getDisplayName(),
                            statName);
                    attackedPokemob.displayMessageToOwner(text);
                    colour = fell ? "green" : "red";
                    text = CommandTools.makeTranslatedMessage(message, colour, attackedPokemob.getDisplayName(),
                            statName);
                    attacker.displayMessageToOwner(text);
                }
            }
            else if (attacker == null && attackedPokemob != null)
            {
                text = CommandTools.makeTranslatedMessage(message, "red", attackedPokemob.getDisplayName(),
                        statName);
                attackedPokemob.displayMessageToOwner(text);
            }
            else
            {
                final String colour = fell ? "green" : "red";
                text = CommandTools.makeTranslatedMessage(message, colour, attacker.getDisplayName(), statName);
                attacker.displayMessageToOwner(text);
            }
        }
    }

    public static void displayStatusMessages(IPokemob attacker, Entity attacked, byte status, boolean onMove)
    {
        final String message = MovesUtils.getStatusMessage(status, onMove);
        ITextComponent text;
        final IPokemob attackedPokemob = CapabilityPokemob.getPokemobFor(attacked);
        if (message != null)
        {
            if (attacker != null)
            {
                text = CommandTools.makeTranslatedMessage(message, "green", attacked.getDisplayName());
                attacker.displayMessageToOwner(text);
            }
            if (attackedPokemob != null && attackedPokemob.getOwnerId() != null)
            {
                text = CommandTools.makeTranslatedMessage(message, "red", attackedPokemob.getDisplayName());
                attackedPokemob.displayMessageToOwner(text);
            }
            else if (attacked instanceof PlayerEntity && attacker != null)
            {
                text = CommandTools.makeTranslatedMessage(message, "red", attacked.getDisplayName());
                PacketPokemobMessage.sendMessage((PlayerEntity) attacked, attacked.getEntityId(), text);
            }
        }
    }

    public static void doAttack(String attackName, IPokemob attacker, Entity attacked)
    {
        final Move_Base move = MovesUtils.moves.get(attackName);
        if (move != null) move.attack(attacker, attacked);
        else
        {
            if (attackName != null) System.err.println("The Move \"" + attackName + "\" does not exist.");
            MovesUtils.doAttack(IMoveConstants.DEFAULT_MOVE, attacker, attacked);
        }
    }

    public static void doAttack(String attackName, IPokemob attacker, Vector3 attacked)
    {
        final Move_Base move = MovesUtils.moves.get(attackName);

        if (move != null) move.attack(attacker, attacked);
        else
        {
            if (attackName != null) System.err.println("The Move \"" + attackName + "\" does not exist.");
            MovesUtils.doAttack(IMoveConstants.DEFAULT_MOVE, attacker, attacked);
        }
    }

    public static int getAttackDelay(IPokemob attacker, String moveName, boolean distanced, boolean playerTarget)
    {
        int cd = PokecubeCore.getConfig().attackCooldown;
        if (playerTarget) cd *= 2;
        final double accuracyMod = attacker.getModifiers().getDefaultMods().getModifier(Stats.ACCURACY);
        final double moveMod = MovesUtils.getDelayMultiplier(attacker, moveName);
        return (int) (cd * moveMod / accuracyMod);
    }

    public static float getAttackStrength(IPokemob attacker, IPokemob attacked, Category type, int PWR,
            MovePacket movePacket)
    {
        final Move_Base move = movePacket.getMove();
        if (move.fixedDamage) return move.getPWR(attacker, attacked.getEntity());

        if (PWR <= 0) return 0;

        float statusMultiplier = 1F;
        if (attacker.getStatus() == IMoveConstants.STATUS_PAR || attacker.getStatus() == IMoveConstants.STATUS_BRN)
            statusMultiplier = 0.5F;

        final int level = attacker.getLevel();
        int ATT;
        int DEF;

        if (type == Category.SPECIAL)
        {
            ATT = (int) (attacker.getStat(Stats.SPATTACK, true) * movePacket.statMults[Stats.SPATTACK.ordinal()]);
            DEF = attacked.getStat(Stats.SPDEFENSE, true);
        }
        else
        {
            ATT = (int) (attacker.getStat(Stats.ATTACK, true) * movePacket.statMults[Stats.ATTACK.ordinal()]);
            DEF = attacked.getStat(Stats.DEFENSE, true);
        }

        ATT = (int) (statusMultiplier * ATT);

        return (level * 0.4F + 2F) * ATT * PWR / (DEF * 50F) + 2;
    }

    /**
     * Computes the delay between two moves in a fight from move and status
     * effects.
     *
     * @return muliplier on attack delay
     */
    public static float getDelayMultiplier(IPokemob attacker, String moveName)
    {
        float statusMultiplier = PokecubeCore.getConfig().attackCooldown / 20F;
        if (attacker.getStatus() == IMoveConstants.STATUS_PAR) statusMultiplier *= 4F;
        final Move_Base move = MovesUtils.getMoveFromName(moveName);
        if (move == null) return 1;
        statusMultiplier *= move.getPostDelayFactor(attacker);
        return statusMultiplier;
    }

    public static Move_Base getMoveFromName(String moveName)
    {
        if (moveName == null) return null;
        final Move_Base ret = MovesUtils.moves.get(moveName);
        return ret;
    }

    public static ITextComponent getMoveName(String attack)
    {
        return new TranslationTextComponent("pokemob.move." + attack);
    }

    protected static String getStatusMessage(byte status, boolean onMove)
    {
        String message = null;
        if (status == IMoveConstants.STATUS_FRZ) message = "pokemob.move.isfrozen";
        if (status == IMoveConstants.STATUS_SLP) message = "pokemob.move.issleeping";
        if (status == IMoveConstants.STATUS_PAR && onMove) message = "pokemob.move.paralyzed";
        else if (status == IMoveConstants.STATUS_PAR) message = "pokemob.move.isfullyparalyzed";
        if (status == IMoveConstants.STATUS_BRN) message = "pokemob.move.isburned";
        if (status == IMoveConstants.STATUS_PSN) message = "pokemob.move.ispoisoned";
        if (status == IMoveConstants.STATUS_PSN2) message = "pokemob.move.isbadlypoisoned";
        return message;
    }

    public static float getTerrainDamageModifier(PokeType type, Entity attacker, TerrainSegment terrain)
    {
        float ret = 1;
        long terrainDuration = 0;
        final PokemobTerrainEffects effect = (PokemobTerrainEffects) terrain.geTerrainEffect("pokemobEffects");
        if (type == PokeType.getType("dragon"))
        {
            terrainDuration = effect.getEffect(PokemobTerrainEffects.EFFECT_TERRAIN_MISTY);
            if (terrainDuration > 0) ret = 0.5f;
        }
        if (type == PokeType.getType("electric") && (attacker.onGround || attacker.fallDistance < 0.5))
        {
            terrainDuration = effect.getEffect(PokemobTerrainEffects.EFFECT_TERRAIN_ELECTRIC);
            if (terrainDuration > 0) ret = 1.5f;
            terrainDuration = effect.getEffect(PokemobTerrainEffects.EFFECT_SPORT_MUD);
            if (terrainDuration > 0) ret *= 0.33f;
        }
        if (type == PokeType.getType("grass") && (attacker.onGround || attacker.fallDistance < 0.5))
        {
            terrainDuration = effect.getEffect(PokemobTerrainEffects.EFFECT_TERRAIN_GRASS);
            if (terrainDuration > 0) ret = 1.5f;
        }
        if (type == PokeType.getType("water"))
        {
            terrainDuration = effect.getEffect(PokemobTerrainEffects.EFFECT_WEATHER_RAIN);
            if (terrainDuration > 0) ret = 1.5f;
            terrainDuration = effect.getEffect(PokemobTerrainEffects.EFFECT_WEATHER_SUN);
            if (terrainDuration > 0) ret = 0.5f;

        }
        if (type == PokeType.getType("fire"))
        {
            terrainDuration = effect.getEffect(PokemobTerrainEffects.EFFECT_WEATHER_SUN);
            if (terrainDuration > 0) ret = 1.5f;
            terrainDuration = effect.getEffect(PokemobTerrainEffects.EFFECT_WEATHER_RAIN);
            if (terrainDuration > 0) ret = 0.5f;
            terrainDuration = effect.getEffect(PokemobTerrainEffects.EFFECT_SPORT_WATER);
            if (terrainDuration > 0) ret *= 0.33f;
        }
        return ret;
    }

    public static String getUnlocalizedMove(String attack)
    {
        return "pokemob.move." + attack;
    }

    /**
     * Handles stats modifications of the move
     *
     * @param attacker
     *            the pokemob being affected
     * @param atk
     *            the move being used
     * @param attacked
     *            whether the mob is the attacked mob, or the attacker
     * @return
     */
    public static boolean handleStats(IPokemob attacker, Entity target, MovePacket atk, boolean attacked)
    {
        final int[] stats = attacked ? atk.attackedStatModification : atk.attackerStatModification;
        final IPokemob affected = attacked ? CapabilityPokemob.getPokemobFor(target) : attacker;
        float[] mods;
        float[] old;
        if (affected != null)
        {
            final DefaultModifiers modifiers = affected.getModifiers().getDefaultMods();
            mods = modifiers.values;
            old = mods.clone();
        }
        else
        {
            mods = new float[7];
            old = mods.clone();
        }
        if (attacked ? atk.attackedStatModProb > Math.random() : atk.attackerStatModProb > Math.random())
            mods[1] = (byte) Math.max(-6, Math.min(6, mods[1] + stats[1]));
        if (attacked ? atk.attackedStatModProb > Math.random() : atk.attackerStatModProb > Math.random())
            mods[2] = (byte) Math.max(-6, Math.min(6, mods[2] + stats[2]));
        if (attacked ? atk.attackedStatModProb > Math.random() : atk.attackerStatModProb > Math.random())
            mods[3] = (byte) Math.max(-6, Math.min(6, mods[3] + stats[3]));
        if (attacked ? atk.attackedStatModProb > Math.random() : atk.attackerStatModProb > Math.random())
            mods[4] = (byte) Math.max(-6, Math.min(6, mods[4] + stats[4]));
        if (attacked ? atk.attackedStatModProb > Math.random() : atk.attackerStatModProb > Math.random())
            mods[5] = (byte) Math.max(-6, Math.min(6, mods[5] + stats[5]));
        if (attacked ? atk.attackedStatModProb > Math.random() : atk.attackerStatModProb > Math.random())
            mods[6] = (byte) Math.max(-6, Math.min(6, mods[6] + stats[6]));
        if (attacked ? atk.attackedStatModProb > Math.random() : atk.attackerStatModProb > Math.random())
            mods[7] = (byte) Math.max(-6, Math.min(6, mods[7] + stats[7]));
        boolean ret = false;
        final byte[] diff = new byte[old.length];
        for (int i = 0; i < old.length; i++)
        {
            diff[i] = (byte) (old[i] - mods[i]);
            if (old[i] != mods[i]) ret = true;
        }
        if (ret)
        {
            final IOngoingAffected affect = CapabilityAffected.getAffected(target);
            final IPokemob targetMob = affected;
            for (byte i = 0; i < diff.length; i++)
                if (diff[i] != 0)
                {
                    if (!attacked) MovesUtils.displayStatsMessage(attacker, target, 0, i, diff[i]);
                    if (affected != null)
                    {
                        if (attacked) MovesUtils.displayStatsMessage(targetMob, attacker.getEntity(), 0, i, diff[i]);
                    }
                    else if (affect != null) affect.addEffect(new StatEffect(Stats.values()[i], diff[i]));
                }
            PacketSyncModifier.sendUpdate(StatModifiers.DEFAULTMODIFIERS, affected);
        }
        return ret;
    }

    public static boolean handleStats2(IPokemob targetPokemob, Entity attacker, int statEffect, int statEffectAmount)
    {
        final DefaultModifiers modifiers = targetPokemob.getModifiers().getDefaultMods();
        final float[] mods = modifiers.values;
        final float[] old = mods.clone();
        mods[1] = (byte) Math.max(-6, Math.min(6, mods[1] + statEffectAmount * (statEffect & 1)));
        mods[2] = (byte) Math.max(-6, Math.min(6, mods[2] + statEffectAmount * (statEffect & 2) / 2));
        mods[3] = (byte) Math.max(-6, Math.min(6, mods[3] + statEffectAmount * (statEffect & 4) / 4));
        mods[4] = (byte) Math.max(-6, Math.min(6, mods[4] + statEffectAmount * (statEffect & 8) / 8));
        mods[5] = (byte) Math.max(-6, Math.min(6, mods[5] + statEffectAmount * (statEffect & 16) / 16));
        mods[6] = (byte) Math.max(-6, Math.min(6, mods[6] + statEffectAmount * (statEffect & 32) / 32));
        mods[7] = (byte) Math.max(-6, Math.min(6, mods[7] + statEffectAmount * (statEffect & 64) / 64));
        boolean ret = false;
        final byte[] diff = new byte[old.length];
        for (int i = 0; i < old.length; i++)
        {
            diff[i] = (byte) (old[i] - mods[i]);
            if (old[i] != mods[i]) ret = true;
        }
        if (ret)
        {
            final IPokemob pokemob = CapabilityPokemob.getPokemobFor(attacker);
            for (byte i = 0; i < diff.length; i++)
                if (diff[i] != 0 && pokemob != null) MovesUtils.displayStatsMessage(pokemob, targetPokemob.getEntity(),
                        0, i, diff[i]);
            PacketSyncModifier.sendUpdate(StatModifiers.DEFAULTMODIFIERS, targetPokemob);
        }
        return ret;
    }

    /**
     * @param attacker
     * @return is attacker able to use any moves, this doesn't check attack
     *         cooldown, instead checks things like status or ai
     */
    public static AbleStatus isAbleToUseMoves(IPokemob attacker)
    {
        if (!attacker.isRoutineEnabled(AIRoutine.AGRESSIVE)) return AbleStatus.AIOFF;
        if ((attacker.getStatus() & IMoveConstants.STATUS_SLP) > 0) return AbleStatus.SLEEP;
        if ((attacker.getStatus() & IMoveConstants.STATUS_FRZ) > 0) return AbleStatus.FREEZE;
        return AbleStatus.ABLE;
    }

    public static boolean isMoveImplemented(String attackName)
    {
        if (attackName == null) return false;
        final Move_Base move = MovesUtils.moves.get(attackName);
        if (move == null) for (final String s : MovesUtils.moves.keySet())
            if (s.toLowerCase(java.util.Locale.ENGLISH).contentEquals(attackName.toLowerCase(java.util.Locale.ENGLISH)))
            {
                attackName = s;
                return true;
            }
        if (move != null) return true;
        return false;
    }

    /** creats and ExplosionCustom */
    public static ExplosionCustom newExplosion(Entity entity, double par2, double par4, double par6, float par8,
            boolean par9, boolean par10)
    {
        final ExplosionCustom var11 = new ExplosionCustom(entity.getEntityWorld(), entity, par2, par4, par6, par8)
                .setMaxRadius(PokecubeCore.getConfig().blastRadius);
        final IPokemob poke = CapabilityPokemob.getPokemobFor(entity);
        if (poke != null) if (poke.getOwner() instanceof PlayerEntity) var11.owner = (PlayerEntity) poke
                .getOwner();
        else var11.owner = null;
        return var11;
    }

    public static void registerMove(Move_Base move_Base)
    {
        MovesUtils.moves.put(move_Base.name, move_Base);
        if (move_Base.move.baseEntry.ohko) MoveEntry.oneHitKos.add(move_Base.name);
        if (move_Base.move.baseEntry.protectionMoves) MoveEntry.protectionMoves.add(move_Base.name);
    }

    public static boolean setStatus(Entity attacked, byte status)
    {
        final IPokemob attackedPokemob = CapabilityPokemob.getPokemobFor(attacked);
        if (attackedPokemob != null)
        {
            final boolean apply = attackedPokemob.setStatus(status);
            if (!apply) return false;
            else attackedPokemob.getEntity().getNavigator().clearPath();
            return true;
        }
        else if (attacked instanceof LivingEntity)
        {
            final IOngoingAffected affected = CapabilityAffected.getAffected(attacked);
            if (affected != null)
            {
                final IOngoingEffect effect = new PersistantStatusEffect(status, 5);
                affected.addEffect(effect);
            }
        }
        return true;
    }

    public static Entity targetHit(final Entity attacker, Vector3 dest)
    {
        final Vector3 source = Vector3.getNewVector().set(attacker, true);
        source.y += attacker.getHeight() / 4;
        final boolean ignoreAllies = false;
        final IPokemob pokemob = CapabilityPokemob.getPokemobFor(attacker);
        final Predicate<Entity> matcher = e ->
        {
            if (attacker == e.getRidingEntity()) return false;
            if (attacker == e) return false;
            if (!PokecubeCore.getConfig().pokemobsDamagePlayers && e instanceof PlayerEntity) return false;
            if (!PokecubeCore.getConfig().pokemobsDamageOwner && e.getUniqueID().equals(pokemob.getOwnerId()))
                return false;
            if (PokecubeCore.getEntityProvider().getEntity(attacker.getEntityWorld(), e.getEntityId(),
                    true) == attacker) return false;
            return true;
        };
        return MovesUtils.targetHit(source, dest.subtract(source), 16, attacker.getEntityWorld(), attacker,
                ignoreAllies, matcher);
    }

    public static Entity targetHit(Vector3 source, Vector3 dir, int distance, World world, Entity attacker,
            boolean ignoreAllies, Predicate<? super Entity> matcher)
    {
        // Vector3 dest = Vector3.getVector().set(target, true);
        Entity target = null;

        final List<Entity> targets = source.allEntityLocationExcluding(distance, 0.5, dir, source, world, attacker);
        double closest = 16;

        if (targets != null) for (final Entity e : targets)
        {
            if (!matcher.apply(e)) continue;
            if (attacker.getDistance(e) < closest)
            {
                closest = attacker.getDistance(e);
                target = e;
            }
        }
        return target;
    }

    public static List<LivingEntity> targetsHit(final Entity attacker, Vector3 dest)
    {
        final Vector3 source = Vector3.getNewVector().set(attacker, true);

        source.y += attacker.getHeight() / 4;
        final List<Entity> targets = source.allEntityLocationExcluding(16, 0.5, dest.subtract(source), source, attacker
                .getEntityWorld(), attacker);
        final List<LivingEntity> ret = new ArrayList<>();
        final IPokemob pokemob = CapabilityPokemob.getPokemobFor(attacker);

        final Predicate<Entity> matcher = e ->
        {
            if (attacker == e.getRidingEntity()) return false;
            if (attacker == e) return false;
            if (!PokecubeCore.getConfig().pokemobsDamagePlayers && e instanceof PlayerEntity) return false;
            if (pokemob != null && !PokecubeCore.getConfig().pokemobsDamageOwner && e.getUniqueID().equals(pokemob
                    .getOwnerId())) return false;
            if (PokecubeCore.getEntityProvider().getEntity(attacker.getEntityWorld(), e.getEntityId(),
                    true) == attacker) return false;
            return true;
        };

        if (targets != null) for (final Entity e : targets)
            if (e instanceof LivingEntity)
            {
                if (!matcher.apply(e)) continue;
                ret.add((LivingEntity) e);
            }
        return ret;
    }

    public static List<LivingEntity> targetsHit(Entity attacker, Vector3 dest, int range, double area)
    {
        final Vector3 source = Vector3.getNewVector().set(attacker);

        final List<Entity> targets = source.allEntityLocationExcluding(range, area, dest.subtract(source), source,
                attacker.getEntityWorld(), attacker);
        final List<LivingEntity> ret = new ArrayList<>();
        if (targets != null) for (final Entity e : targets)
            if (e instanceof LivingEntity) ret.add((LivingEntity) e);

        return ret;
    }

    public static void useMove(@Nonnull Move_Base move, @Nonnull Entity user, @Nullable Entity target,
            @Nonnull Vector3 start, @Nonnull Vector3 end)
    {
        move.ActualMoveUse(user, target, start, end);
    }
}
