package dev.emi.emi.backport;

import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.item.ItemStack;

public class CookingRecipe {
    private final EmiIngredient input;
    private final ItemStack output;
    private final float experience;

    public CookingRecipe(EmiIngredient input, ItemStack output, float experience) {
        this.input = input;
        this.output = output;
        this.experience = experience;
    }

    public EmiIngredient getInput() {
        return input;
    }

    public ItemStack getOutput() {
        return output;
    }

    public float getExperience() {
        return experience;
    }
}
