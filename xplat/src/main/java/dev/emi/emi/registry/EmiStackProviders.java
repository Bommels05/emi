package dev.emi.emi.registry;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiStackProvider;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.mixin.accessor.CraftingResultSlotAccessor;
import dev.emi.emi.mixin.accessor.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.slot.CraftingResultSlot;
import net.minecraft.inventory.slot.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeDispatcher;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;

public class EmiStackProviders {
	public static Map<Class<?>, List<EmiStackProvider<?>>> fromClass = Maps.newHashMap();
	public static List<EmiStackProvider<?>> generic = Lists.newArrayList();

	public static void clear() {
		fromClass.clear();
		generic.clear();
	}
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static EmiStackInteraction getStackAt(Screen screen, int x, int y, boolean notClick) {
		if (fromClass.containsKey(screen.getClass())) {
			for (EmiStackProvider provider : fromClass.get(screen.getClass())) {
				EmiStackInteraction stack = provider.getStackAt(screen, x, y);
				if (!stack.isEmpty() && (notClick || stack.isClickable())) {
					return stack;
				}
			}
		}
		for (EmiStackProvider handler : generic) {
			EmiStackInteraction stack = handler.getStackAt(screen, x, y);
			if (!stack.isEmpty() && (notClick || stack.isClickable())) {
				return stack;
			}
		}
		if (notClick && screen instanceof HandledScreenAccessor handled) {
			Slot s = handled.getFocusedSlot();
			if (s != null) {
				ItemStack stack = s.getStack();
				if (stack != null) {
					if (s instanceof CraftingResultSlot craf) {
						// Emi be making assumptions
						try {
							Inventory inv = ((CraftingResultSlotAccessor) craf).getInput();
							if (inv instanceof CraftingInventory) {
								MinecraftClient client = MinecraftClient.getInstance();
								List<RecipeType> list = ((List<RecipeType>) RecipeDispatcher.getInstance().getAllRecipes()).stream().filter(
										recipe -> recipe.matches((CraftingInventory) inv, client.world)
								).collect(Collectors.toList());
								if (!list.isEmpty()) {
									Identifier id = EmiPort.getId(list.get(0));
									EmiRecipe recipe = EmiApi.getRecipeManager().getRecipe(id);
									if (recipe != null) {
										return new EmiStackInteraction(EmiStack.of(stack), recipe, false);
									}
								}
							}
						} catch (Exception e) {
						}
					}
					return new EmiStackInteraction(EmiStack.of(stack));
				}
			}
		}
		return EmiStackInteraction.EMPTY;
	}
}
