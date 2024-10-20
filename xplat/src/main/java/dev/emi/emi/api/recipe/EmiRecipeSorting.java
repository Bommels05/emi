package dev.emi.emi.api.recipe;

import java.util.Comparator;
import java.util.List;

import dev.emi.emi.EmiUtil;
import dev.emi.emi.registry.EmiRecipeSorter;
import net.minecraft.util.Identifier;

public class EmiRecipeSorting {
	private static final Comparator<EmiRecipe> NONE = (a, b) -> 0;

	public static Comparator<EmiRecipe> none() {
		return NONE;
	}

	public static Comparator<EmiRecipe> identifier() {
		return (ar, br) -> {
			Identifier a = ar.getId(); 
			Identifier b = br.getId(); 
			if (a == null) {
				if (b == null) {
					return 0;
				} else {
					return 1;
				}
			} else if (b == null) {
				return -1;
			}
			return EmiUtil.compareIdentifier(a, b);
		};
	}
	
	public static Comparator<EmiRecipe> compareOutputThenInput() {
		return (a, b) -> {
			int comp = compareStacks(EmiRecipeSorter.getOutput(a), EmiRecipeSorter.getOutput(b));
			if (comp != 0) {
				return comp;
			}
			return compareStacks(EmiRecipeSorter.getInput(a), EmiRecipeSorter.getInput(b));
		};
	}

	public static Comparator<EmiRecipe> compareInputThenOutput() {
		return (a, b) -> {
			int comp = compareStacks(EmiRecipeSorter.getInput(a), EmiRecipeSorter.getInput(b));
			if (comp != 0) {
				return comp;
			}
			return compareStacks(EmiRecipeSorter.getOutput(a), EmiRecipeSorter.getOutput(b));
		};
	}

	private static int compareStacks(List<Integer> a, List<Integer> b) {
		int min = Math.min(a.size(), b.size());
		for (int i = 0; i < min; i++) {
			int comparison = Integer.compare(a.get(i), b.get(i));
			if (comparison != 0) {
				return comparison;
			}
		}
		return Integer.compare(a.size(), b.size());
	}
}
