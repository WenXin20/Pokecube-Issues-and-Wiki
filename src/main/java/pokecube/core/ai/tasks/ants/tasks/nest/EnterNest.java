package pokecube.core.ai.tasks.ants.tasks.nest;

import java.util.Optional;

import net.minecraft.entity.ai.brain.Brain;
import pokecube.core.ai.tasks.ants.AntTasks;
import pokecube.core.ai.tasks.ants.tasks.AbstractAntTask;
import pokecube.core.interfaces.IInhabitable;
import pokecube.core.interfaces.IPokemob;
import thut.api.maths.Vector3;

public class EnterNest extends AbstractAntTask
{
    final Vector3 homePos    = Vector3.getNewVector();
    int           enterTimer = 0;

    public EnterNest(final IPokemob pokemob)
    {
        super(pokemob);
    }

    @Override
    public void reset()
    {
        this.homePos.clear();
        this.entity.getNavigator().resetRangeMultiplier();
        this.enterTimer = 0;
    }

    @Override
    public void run()
    {
        final Brain<?> brain = this.entity.getBrain();
        this.homePos.set(this.nest.nest.getPos());
        this.entity.getNavigator().setRangeMultiplier(10);

        // Ensures no jobs for 5s after this is decided
        brain.setMemory(AntTasks.NO_WORK_TIME, -100);

        // If we take more than 30s to progress, just tp there
        if (this.enterTimer++ > 600) this.entity.setPosition(this.homePos.x + 0.5, this.homePos.y + 1, this.homePos.z
                + 0.5);

        // If too far, lets path over.
        if (this.homePos.distToEntity(this.entity) > 2 || this.nest == null) this.setWalkTo(this.homePos, 1, 1);
        else
        {
            final IInhabitable habitat = this.nest.hab;

            // if (habitat instanceof HabitatProvider) ((HabitatProvider)
            // habitat).wrapped = new AntHabitat();

            if (habitat.canEnterHabitat(this.entity))
            {
                brain.setMemory(AntTasks.OUT_OF_HIVE_TIMER, 0);
                brain.removeMemory(AntTasks.GOING_HOME);
                habitat.onEnterHabitat(this.entity);
            }
            // Set the out of hive timer, so we don't try to re-enter
            // immediately!
            else brain.setMemory(AntTasks.OUT_OF_HIVE_TIMER, 100);
        }
    }

    @Override
    protected boolean doTask()
    {
        // We were already heading home, so keep doing that.
        if (!this.homePos.isEmpty()) return true;
        final Brain<?> brain = this.entity.getBrain();
        if (brain.hasMemory(AntTasks.GOING_HOME)) return true;
        final Optional<Integer> hiveTimer = brain.getMemory(AntTasks.OUT_OF_HIVE_TIMER);
        final int timer = hiveTimer.orElseGet(() -> 0);
        // This is our counter for if something angered us, and made is leave
        // the hive, if so, we don't return to hive.
        if (timer > 0) return false;
        if (AntTasks.shouldAntBeInNest(this.world, this.nest.nest.getPos()))
        {
            brain.setMemory(AntTasks.GOING_HOME, true);
            return true;
        }
        // Been out too long, we want to return!
        if (timer < -12000 || timer < -2500 && this.entity.getRNG().nextInt(200) == 0) brain.setMemory(
                AntTasks.GOING_HOME, true);
        return brain.hasMemory(AntTasks.GOING_HOME);
    }

}