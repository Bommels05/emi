package dev.emi.emi.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;

public class EmiRecipeSorter {
	private static Map<EmiRecipe, List<Integer>> inputCache = Maps.newIdentityHashMap();
	private static Map<EmiRecipe, List<Integer>> outputCache = Maps.newIdentityHashMap();

	public static List<Integer> getInput(EmiRecipe recipe) {
		List<Integer> list = inputCache.get(recipe);
		if (list == null) {
			list = bakedList(recipe.getInputs());
			inputCache.put(recipe, list);
		}
		return list;
	}

	public static List<Integer> getOutput(EmiRecipe recipe) {
		List<Integer> list = outputCache.get(recipe);
		if (list == null) {
			list = bakedList(recipe.getOutputs());
			outputCache.put(recipe, list);
		}
		return list;
	}

	public static void clear() {
		inputCache.clear();
		outputCache.clear();
	}

	private static List<Integer> bakedList(List<? extends EmiIngredient> stacks) {
		List<Integer> list = new ArrayList<>(stacks.size());
		for (EmiIngredient stack : stacks) {
			if (stack.isEmpty()) {
				continue;
			}
			int value = EmiStackList.getIndex(stack.getEmiStacks().get(0));
			list.add(value);
		}
		return list;
	}
}
