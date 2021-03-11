package pokecube.mobs.moves.attacks.special;

import net.minecraft.entity.LivingEntity;
import pokecube.core.PokecubeCore;
import pokecube.core.interfaces.pokemob.moves.MovePacket;
import pokecube.core.moves.damage.PokemobDamageSource;
import pokecube.core.moves.templates.Move_Basic;

public class Mirrorcoat extends Move_Basic
{

    public Mirrorcoat()
    {
        super("mirrorcoat");
    }

    @Override
    public void postAttack(final MovePacket packet)
    {
        super.postAttack(packet);
        if (packet.canceled || packet.failed) return;
        final LivingEntity attacker = packet.attacker.getEntity();
        if (!packet.attacker.getMoveStats().biding)
        {
            attacker.getPersistentData().putLong("bideTime", attacker.getCommandSenderWorld().getGameTime() + PokecubeCore
                    .getConfig().attackCooldown);
            packet.attacker.getMoveStats().biding = true;
            packet.attacker.getMoveStats().SPECIALDAMAGETAKENCOUNTER = 0;
        }
        else if (attacker.getPersistentData().getLong("bideTime") < attacker.getCommandSenderWorld().getGameTime())
        {
            attacker.getPersistentData().remove("bideTime");
            final int damage = 2 * packet.attacker.getMoveStats().SPECIALDAMAGETAKENCOUNTER;
            packet.attacker.getMoveStats().SPECIALDAMAGETAKENCOUNTER = 0;
            if (packet.attacked != null) packet.attacked.hurt(new PokemobDamageSource(attacker, this),
                    damage);
            packet.attacker.getMoveStats().biding = false;
        }
    }
}
