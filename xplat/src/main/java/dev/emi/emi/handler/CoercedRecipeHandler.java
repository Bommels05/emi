package dev.emi.emi.handler;

import java.util.List;

import com.google.common.collect.Lists;

import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.mixin.accessor.CraftingInventoryAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.slot.CraftingResultSlot;
import net.minecraft.inventory.slot.Slot;
import net.minecraft.screen.ScreenHandler;

public class CoercedRecipeHandler<T extends ScreenHandler> implements StandardRecipeHandler<T> {
	private CraftingResultSlot output;
	private CraftingInventory inv;

	public CoercedRecipeHandler(CraftingResultSlot output, CraftingInventory inv) {
		this.output = output;
		this.inv = inv;
	}

	@Override
	public Slot getOutputSlot(ScreenHandler handler) {
		return output;
	}

	@Override
	public List<Slot> getInputSources(ScreenHandler handler) {
		MinecraftClient client = MinecraftClient.getInstance();
		List<Slot> slots = Lists.newArrayList();
		if (output != null) {
			for (Slot slot : (List<Slot>) handler.slots) {
				if (slot.canTakeItems(client.field_3805) && slot != output) {
					slots.add(slot);
				}
			}
		}
		return slots;
	}

	@Override
	public List<Slot> getCraftingSlots(ScreenHandler handler) {
		List<Slot> slots = Lists.newArrayList();
		int width = ((CraftingInventoryAccessor) inv).getWidth();
		int height = inv.getInvSize() / width;
		for (int i = 0; i < 9; i++) {
			slots.add(null);
		}
		for (Slot slot : (List<Slot>) handler.slots) {
			if (slot.inventory == inv && slot.id < width * height && slot.id >= 0) {
				int index = slot.id;
				index = index * 3 / width;
				slots.set(index, slot);
			}
		}
		return slots;
	}

	@Override
	public boolean supportsRecipe(EmiRecipe recipe) {
		if (recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING && recipe.supportsRecipeTree()) {
			if (recipe instanceof EmiCraftingRecipe crafting) {
				int width = ((CraftingInventoryAccessor) inv).getWidth();
				return crafting.canFit(width, inv.getInvSize() / width);
			}
			return true;
		}
		return false;
	}
}
