package dev.emi.emi.recipe;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public class EmiShapelessOreRecipe extends EmiCraftingRecipe {

	public EmiShapelessOreRecipe(ShapelessOreRecipe recipe) {
		super(recipe.getInput().stream().map(EmiShapedOreRecipe::fromOreInput).toList(),
			EmiStack.of(recipe.getOutput()), EmiPort.getId(recipe));
		EmiShapedRecipe.setRemainders(input, recipe);
	}

	@Override
	public boolean canFit(int width, int height) {
		return input.size() <= width * height;
	}
}
