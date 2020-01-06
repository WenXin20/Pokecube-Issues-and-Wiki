package pokecube.core.database.recipes;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import pokecube.core.PokecubeCore;
import pokecube.core.database.PokedexEntryLoader.Drop;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.utils.Tools;

public class XMLRecipeHandler
{
    @XmlRootElement(name = "Recipe")
    public static class XMLRecipe
    {
        @XmlAttribute
        public boolean              shapeless = false;
        @XmlAttribute
        String                      handler   = "default";
        @XmlAttribute
        public String               map       = "";
        @XmlElement(name = "Output")
        public XMLRecipeOutput      output;
        @XmlElement(name = "Input")
        public List<XMLRecipeInput> inputs    = Lists.newArrayList();
        @XmlAnyAttribute
        public Map<QName, String>   values    = Maps.newHashMap();

        @Override
        public String toString()
        {
            return "output: " + this.output + " inputs: " + this.inputs + " shapeless: " + this.shapeless + " map: "
                    + this.map;
        }
    }

    @XmlRootElement(name = "Input")
    public static class XMLRecipeInput extends Drop
    {
        @XmlAttribute
        public String key = "";

        @Override
        public String toString()
        {
            return "values: " + this.values + " tag: " + this.tag + " key: " + this.key;
        }
    }

    @XmlRootElement(name = "Output")
    public static class XMLRecipeOutput extends Drop
    {
        @Override
        public String toString()
        {
            return "values: " + this.values + " tag: " + this.tag;
        }
    }

    @XmlRootElement(name = "Recipes")
    public static class XMLRecipes
    {
        @XmlElement(name = "Recipe")
        public List<XMLRecipe> recipes = Lists.newArrayList();
    }

    public static Set<ResourceLocation> recipeFiles = Sets.newHashSet();

    public static Map<String, IRecipeParser> recipeParsers = Maps.newHashMap();

    static
    {
        // TODO re-implement the PokemobMoveRecipeParser...
        // recipeParsers.put("move_effect", new PokemobMoveRecipeParser());
    }

    public static void addRecipe(final XMLRecipe recipe)
    {
        final IRecipeParser parser = XMLRecipeHandler.recipeParsers.get(recipe.handler);
        try
        {
            if (PokecubeMod.debug) PokecubeCore.LOGGER.info("Recipe Handler: " + recipe.handler + " Parser: " + parser);
            parser.manageRecipe(recipe);

        }
        catch (final NullPointerException e)
        {
            PokecubeCore.LOGGER.error("Error with a recipe, Error for: " + recipe, e);
        }
    }

    public static ItemStack getStack(final Drop drop)
    {
        final Map<QName, String> values = drop.values;
        if (drop.tag != null)
        {
            final QName name = new QName("tag");
            values.put(name, drop.tag);
        }
        return Tools.getStack(drop.values);
    }
}
