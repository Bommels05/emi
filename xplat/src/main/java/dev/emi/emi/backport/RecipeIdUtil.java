package dev.emi.emi.backport;

import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.mixin.accessor.ShapedRecipeTypeAccessor;
import dev.emi.emi.mixin.accessor.ShapelessRecipeTypeAccessor;
import dev.emi.emi.recipe.EmiShapedOreRecipe;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.util.Identifier;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;

public class RecipeIdUtil {

    public static Identifier getId(Object recipe) {
        if (recipe instanceof RecipeType) {
            if (recipe instanceof ShapedRecipeType shaped) {
                return new Identifier("unknown", getJoinedId(EmiUtil.subId(shaped.getOutput()), getJoinedId(((ShapedRecipeTypeAccessor) shaped).getIngredients())));
            } else if (recipe instanceof ShapelessRecipeType shapeless) {
                return new Identifier("unknown", getJoinedId(EmiUtil.subId(shapeless.getOutput()), getJoinedId(((ShapelessRecipeTypeAccessor) shapeless).getStacks().toArray(new ItemStack[0]))));
            } else if (recipe instanceof ShapedOreRecipe shaped) {
                return new Identifier("unknown", getJoinedId(EmiUtil.subId(shaped.getOutput()), getJoinedId(o -> getId(EmiShapedOreRecipe.fromOreInput(o)), shaped.getInput())));
            } else if (recipe instanceof ShapelessOreRecipe shapeless) {
                return new Identifier("unknown", getJoinedId(EmiUtil.subId(shapeless.getOutput()), getJoinedId(o -> getId(EmiShapedOreRecipe.fromOreInput(o)), shapeless.getInput().toArray())));
            } else if (recipe instanceof MapUpscaleRecipeType) {
                return new Identifier("minecraft:map_extending");
            } else if (recipe instanceof MapCloningRecipeType) {
                return new Identifier("minecraft:map_cloning");
            } else if (recipe instanceof BookCloningRecipeType) {
                return new Identifier("minecraft:book_cloning");
            }
        } else if (recipe instanceof CookingRecipe r) {
            return new Identifier("unknown", getJoinedId(EmiUtil.subId(r.getOutput()), getId(r.getInput())));
        }
        return null;
    }

    private static String getJoinedId(ItemStack... stacks) {
        return getJoinedId(stack -> stack != null ? EmiUtil.subId(stack) : "", stacks);
    }

    private static <T> String getJoinedId(Function<T, String> converter, T... inputs) {
        StringJoiner joiner = new StringJoiner("_");
        for (T input : inputs) {
            String converted = converter.apply(input);
            if (!converted.isEmpty()) {
                joiner.add(converted);
            }
        }
        return joiner.toString();
    }

    private static String getJoinedId(String... ids) {
        StringJoiner joiner = new StringJoiner("_");
        for (String id : ids) {
            joiner.add(id);
        }
        return joiner.toString();
    }

    private static String getId(EmiIngredient ingredient) {
        if (ingredient instanceof ItemEmiStack stack) {
            return EmiUtil.subId(stack.getItemStack());
        } else if (ingredient instanceof ListEmiIngredient list) {
            return getJoinedId(RecipeIdUtil::getId, list.getEmiStacks().toArray(new EmiIngredient[0]));
        } else if (ingredient instanceof TagEmiIngredient tag) {
            return tag.key.getTag().toString();
        } else if (ingredient instanceof EmptyEmiStack) {
            return "";
        } else {
            throw new IllegalStateException("Unsupported EmiIngredient for recipe id generation: " + ingredient);
        }
    }

}
