package dev.emi.emi.recipe;

import java.util.List;

import com.google.common.collect.Lists;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.mixin.accessor.ShapedRecipeTypeAccessor;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipeType;

public class EmiShapedRecipe extends EmiCraftingRecipe {

	public EmiShapedRecipe(ShapedRecipeType recipe) {
		super(padIngredients((ShapedRecipeTypeAccessor) recipe), EmiStack.of(recipe.getOutput()), EmiPort.getId(recipe), false);
		setRemainders(input, recipe);
	}

	public static void setRemainders(List<EmiIngredient> input, RecipeType recipe) {
		try {
			CraftingInventory inv = EmiUtil.getCraftingInventory();
			for (int i = 0; i < input.size(); i++) {
				if (input.get(i).isEmpty()) {
					continue;
				}
				for (int j = 0; j < input.size(); j++) {
					if (j == i) {
						continue;
					}
					if (!input.get(j).isEmpty()) {
						inv.setInvStack(j, input.get(j).getEmiStacks().get(0).getItemStack().copy());
					}
				}
				List<EmiStack> stacks = input.get(i).getEmiStacks();
				for (EmiStack stack : stacks) {
					inv.setInvStack(i, stack.getItemStack().copy());
					Item remainder = stack.getItemStack().getItem().getRecipeRemainder();
					if (remainder != null) {
						stack.setRemainder(EmiStack.of(remainder));
					}
				}
			}
		} catch (Exception e) {
			EmiLog.error("Exception thrown setting remainders for " + EmiPort.getId(recipe));
			e.printStackTrace();
		}
	}

	private static List<EmiIngredient> padIngredients(ShapedRecipeTypeAccessor recipe) {
		List<EmiIngredient> list = Lists.newArrayList();
		int i = 0;
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				if (x >= recipe.getWidth() || y >= recipe.getHeight() || i >= recipe.getIngredients().length) {
					list.add(EmiStack.EMPTY);
				} else {
					list.add(EmiStack.ofPotentialTag(recipe.getIngredients()[i++], 1));
				}
			}
		}
		return list;
	}
}
