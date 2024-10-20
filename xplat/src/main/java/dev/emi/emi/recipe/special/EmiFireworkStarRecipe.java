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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;


public class EmiFireworkStarRecipe extends EmiPatternCraftingRecipe {
	private static final List<ItemStack> DYES = EmiArmorDyeRecipe.DYES;

	private static final List<Item> SHAPES = List.of(Items.FIRE_CHARGE, Items.FEATHER, Items.GOLD_NUGGET, Items.SKULL);

	private static final List<Item> EFFECTS = List.of(Items.DIAMOND, Items.GLOWSTONE_DUST);

	public EmiFireworkStarRecipe(Identifier id) {
		super(List.of(
				EmiIngredient.of(DYES.stream().map(i -> (EmiIngredient) EmiStack.of(i)).collect(Collectors.toList())),
						EmiIngredient.of(SHAPES.stream().map(i -> (EmiIngredient) EmiStack.of(i)).collect(Collectors.toList())),
						EmiIngredient.of(EFFECTS.stream().map(i -> (EmiIngredient) EmiStack.of(i)).collect(Collectors.toList())),
						EmiStack.of(Items.GUNPOWDER)),
				EmiStack.of(Items.FIREWORK_CHARGE), id);
	}

	@Override
	public SlotWidget getInputWidget(int slot, int x, int y) {
		if (slot == 0) {
			return new SlotWidget(EmiStack.of(Items.GUNPOWDER), x, y);
		} else {
			final int s = slot - 1;
			return new GeneratedSlotWidget(r -> {
				List<ItemStack> items = getItems(r);
				if (s < items.size()) {
					return EmiStack.of(items.get(s));
				}
				return EmiStack.EMPTY;
			}, unique, x, y);
		}
	}

	@Override
	public SlotWidget getOutputWidget(int x, int y) {
		return new GeneratedSlotWidget(this::getFireworkStar, unique, x, y);
	}
	private List<ItemStack> getDyes(Random random, int max) {
		List<ItemStack> dyes = Lists.newArrayList();
		int amount = 1 + random.nextInt(max);
		for (int i = 0; i < amount; i++) {
			dyes.add(DYES.get(random.nextInt(DYES.size())));
		}
		return dyes;
	}

	private List<ItemStack> getItems(Random random) {
		List<ItemStack> items = Lists.newArrayList();
		int amount = random.nextInt(4);
		if (amount < 2) {
			items.add(new ItemStack(EFFECTS.get(amount)));
		} else if (amount == 2) {
			items.add(new ItemStack(EFFECTS.get(0)));
			items.add(new ItemStack(EFFECTS.get(1)));
		}
		amount = random.nextInt(5);
		if (amount < 3) {
			Item item = SHAPES.get(amount);
			if (item == Items.SKULL) {
				items.add(new ItemStack(item, 1, random.nextInt(5)));
			} else {
				items.add(new ItemStack(item));
			}
		}

		items.addAll(getDyes(random, 8-items.size()));

		return items;
	}

	private EmiStack getFireworkStar(Random random) {
		ItemStack stack = new ItemStack(Items.FIREWORK_CHARGE);
		NbtCompound tag = new NbtCompound();
		NbtCompound explosion = new NbtCompound();
		boolean hasShape = false;

		List<ItemStack> items = getItems(random);
		byte smallBall = 0;
		byte largeBall = 1;
		byte star = 2;
		byte creeper = 3;
		byte burst = 4;
		List<Integer> colors = Lists.newArrayList();

		for (ItemStack item : items) {
			if (Items.GLOWSTONE_DUST.equals(item.getItem())) {
				explosion.putByte("Flicker", largeBall);
			} else if (Items.DIAMOND.equals(item.getItem())) {
				explosion.putByte("Trail", largeBall);
			} else if (Items.FIRE_CHARGE.equals(item.getItem())) {
				explosion.putByte("Type", largeBall);
				hasShape = true;
			} else if (Items.GOLD_NUGGET.equals(item.getItem())) {
				explosion.putByte("Type", star);
				hasShape = true;
			} else if (Items.FEATHER.equals(item.getItem())) {
				explosion.putByte("Type", burst);
				hasShape = true;
			} else if (SHAPES.contains(item.getItem())) {
				explosion.putByte("Type", creeper);
				hasShape = true;
			} else {
				colors.add(DyeItem.COLORS[item.getData()]);
			}
		}
		if (!hasShape) {
			explosion.putByte("Type", smallBall);
		}

		explosion.putIntArray("Colors", colors.stream().mapToInt(i -> i).toArray());
		tag.put("Explosion", explosion);
		stack.setNbt(tag);
		return EmiStack.of(stack);
	}
}

