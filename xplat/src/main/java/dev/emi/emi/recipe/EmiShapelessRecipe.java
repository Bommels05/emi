package dev.emi.emi.recipe;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.mixin.accessor.ShapelessRecipeTypeAccessor;
import net.minecraft.recipe.ShapelessRecipeType;

public class EmiShapelessRecipe extends EmiCraftingRecipe {
	
	public EmiShapelessRecipe(ShapelessRecipeType recipe) {
		super(((ShapelessRecipeTypeAccessor) recipe).getStacks().stream().map(i -> EmiStack.ofPotentialTag(i, 1)).toList(),
			EmiStack.of(recipe.getOutput()), EmiPort.getId(recipe));
		EmiShapedRecipe.setRemainders(input, recipe);
	}

	@Override
	public boolean canFit(int width, int height) {
		return input.size() <= width * height;
	}
}
