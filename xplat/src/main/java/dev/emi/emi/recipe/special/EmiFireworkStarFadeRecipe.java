package dev.emi.emi.recipe.special;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import dev.emi.emi.api.recipe.EmiPatternCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.GeneratedSlotWidget;
import dev.emi.emi.api.widget.SlotWidget;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public class EmiFireworkStarFadeRecipe extends EmiPatternCraftingRecipe {
	private static final List<ItemStack> DYES = EmiArmorDyeRecipe.DYES;

	public EmiFireworkStarFadeRecipe(Identifier id) {
		super(List.of(
			EmiIngredient.of(DYES.stream().map(i -> (EmiIngredient) EmiStack.of(i)).collect(Collectors.toList())),
			EmiStack.of(Items.FIREWORK_CHARGE)), EmiStack.of(Items.FIREWORK_CHARGE), id);
	}

	@Override
	public SlotWidget getInputWidget(int slot, int x, int y) {
		return new GeneratedSlotWidget(r -> {
			EmiStack fireworkStar = getFireworkStar(r, false);
			List<ItemStack> dyeItems = getDyes(r, 8);
			final int s = slot - 1;
			if (slot == 0) {
				return fireworkStar;
			}
			if (s < dyeItems.size()) {
				return EmiStack.of(dyeItems.get(s));
			}
			return EmiStack.EMPTY;
		}, unique, x, y);
	}

	@Override
	public SlotWidget getOutputWidget(int x, int y) {
		return new GeneratedSlotWidget(r -> getFireworkStar(r, true), unique, x, y);
	}

	private List<ItemStack> getDyes(Random random, int max) {
		List<ItemStack> dyes = Lists.newArrayList();
		int amount = 1 + random.nextInt(max);
		for (int i = 0; i < amount; i++) {
			dyes.add(DYES.get(random.nextInt(DYES.size())));
		}
		return dyes;
	}

	private EmiStack getFireworkStar(Random random, Boolean faded) {
		ItemStack stack = new ItemStack(Items.FIREWORK_CHARGE);
		NbtCompound tag = new NbtCompound();
		NbtCompound explosion = new NbtCompound();
		int items = 0;

		int amount = random.nextInt(5);
		explosion.putByte("Type", (byte) amount);

		if (!(amount == 0)) {
			items++;
		}

		amount = random.nextInt(4);

		if (amount == 0) {
			explosion.putByte("Flicker", (byte) 1);
			items++;
		} else if (amount == 1) {
			explosion.putByte("Trail", (byte) 1);
			items++;
		} else if (amount == 2){
			explosion.putByte("Trail", (byte) 1);
			explosion.putByte("Flicker", (byte) 1);
			items = items + 2;
		}

		List<ItemStack> dyeItems = getDyes(random, 8 - items);
		List<Integer> colors = Lists.newArrayList();
		for (ItemStack dyeItem : dyeItems) {
			colors.add(DyeItem.COLORS[dyeItem.getData()]);
		}
		explosion.putIntArray("Colors", colors.stream().mapToInt(i -> i).toArray());

		if (faded) {
			List<ItemStack> dyeItemsFaded = getDyes(random, 8);
			List<Integer> fadedColors = Lists.newArrayList();
			for (ItemStack dyeItem : dyeItemsFaded) {
				fadedColors.add(DyeItem.COLORS[dyeItem.getData()]);
			}
			explosion.putIntArray("FadeColors", fadedColors.stream().mapToInt(i -> i).toArray());
		}

		tag.put("Explosion", explosion);
		stack.setNbt(tag);
		return EmiStack.of(stack);
	}
}
