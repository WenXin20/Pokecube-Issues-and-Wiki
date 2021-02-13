package pokecube.core.database.pokedex;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import pokecube.core.PokecubeCore;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.database.pokedex.PokedexEntryLoader.XMLPokedexEntry;
import thut.core.common.ThutCore;

public class PokemobsDatabases
{
    public static final List<String> DATABASES = Lists.newArrayList("database/pokemobs/");

    public static final PokemobsJson compound = new PokemobsJson();

    public static void load()
    {
        final List<PokemobsJson> allFound = Lists.newArrayList();

        PokemobsDatabases.compound.pokemon.clear();

        for (final String path : PokemobsDatabases.DATABASES)
        {
            final Collection<ResourceLocation> resources = Database.resourceManager.getAllResourceLocations(path, s -> s
                    .endsWith(".json"));

            resources.forEach(l ->
            {
                try
                {
                    final PokemobsJson database = PokedexEntryLoader.loadDatabase(Database.resourceManager.getResource(
                            l).getInputStream(), true);
                    if (database != null)
                    {
                        database.pokemon.forEach(e -> e.name = ThutCore.trim(e.name));
                        database.init();
                        database._file = l;
                        if (!database.requiredMods.isEmpty())
                        {
                            boolean allHere = true;
                            for (final String s : database.requiredMods)
                                allHere = allHere || ModList.get().isLoaded(s);
                            if (!allHere) return;
                        }
                        PokecubeCore.LOGGER.info(l);
                        allFound.add(database);
                    }
                }
                catch (final Exception e)
                {
                    PokecubeCore.LOGGER.error("Error with database file {}", l, e);
                }
            });
        }

        Collections.sort(allFound);
        int n = 0;

        for (final PokemobsJson json : allFound)
        {
            for (final XMLPokedexEntry e : json.pokemon)
                if (PokemobsDatabases.compound.__map__.containsKey(e.name))
                {
                    final XMLPokedexEntry old = PokemobsDatabases.compound.__map__.get(e.name);
                    if (old.stats != null && e.stats != null) old.stats.mergeFrom(e.stats);
                    PokedexEntryLoader.mergeNonDefaults(PokedexEntryLoader.missingno, e, old);
                }
                else
                {
                    if (n != 0) PokecubeCore.LOGGER.info("Adding entry again? {} {}", e.name, json._file);
                    PokemobsDatabases.compound.addEntry(e);
                }
            n++;
        }
    }

    public static void preInitLoad()
    {
        PokemobsDatabases.load();

        // Make all of the pokedex entries added by the database.
        for (final XMLPokedexEntry entry : PokemobsDatabases.compound.pokemon)
            if (Database.getEntry(entry.name) == null)
            {
                final PokedexEntry pentry = new PokedexEntry(entry.number, entry.name);
                pentry.dummy = entry.dummy;
                pentry.stock = entry.stock;
                if (entry.base)
                {
                    pentry.base = entry.base;
                    Database.baseFormes.put(entry.number, pentry);
                    Database.addEntry(pentry);
                }
            }
        // Init all of the evolutions, this is so that the relations between the
        // mobs can be known later.
        for (final XMLPokedexEntry entry : PokemobsDatabases.compound.pokemon)
            PokedexEntryLoader.preCheckEvolutions(entry);
    }

}