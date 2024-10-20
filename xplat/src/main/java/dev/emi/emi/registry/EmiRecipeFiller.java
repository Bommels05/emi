package dev.emi.emi.registry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import dev.emi.emi.EmiUtil;
import dev.emi.emi.mixin.accessor.CraftingInventoryAccessor;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.slot.CraftingResultSlot;
import net.minecraft.inventory.slot.Slot;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.handler.CoercedRecipeHandler;
import dev.emi.emi.mixin.accessor.CraftingResultSlotAccessor;
import dev.emi.emi.runtime.EmiSidebars;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;

public class EmiRecipeFiller {
	public static Map<Class<? extends ScreenHandler>, List<EmiRecipeHandler<?>>> handlers = Maps.newHashMap();
	public static BiFunction<ScreenHandler, EmiRecipe, EmiRecipeHandler<?>> extraHandlers = (h, r) -> null;

	public static void clear() {
		handlers.clear();
		extraHandlers = (h, r) -> null;
	}

	public static boolean isSupported(EmiRecipe recipe) {
		for (List<EmiRecipeHandler<?>> list : handlers.values()) {
			for (EmiRecipeHandler<?> handler : list) {
				if (handler.supportsRecipe(recipe) && handler.alwaysDisplaySupport(recipe)) {
					return true;
				}
			}
		}
		HandledScreen hs = EmiApi.getHandledScreen();
		if (hs != null) {
			for (EmiRecipeHandler<?> handler : getAllHandlers(hs)) {
				if (handler.supportsRecipe(recipe)) {
					return true;
				}
			}
			EmiRecipeHandler<?> handler = extraHandlers.apply(hs.screenHandler, recipe);
			if (handler != null && handler.supportsRecipe(recipe)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public static <T extends ScreenHandler> List<EmiRecipeHandler<T>> getAllHandlers(HandledScreen screen) {
		if (screen != null) {
			T screenHandler = (T) screen.screenHandler;
			Class<? extends ScreenHandler> type;
			try {
				type = screenHandler instanceof PlayerScreenHandler ? null : screenHandler.getClass();
			} catch (UnsupportedOperationException e) {
				type = null;
			}
			if ((type != null || screenHandler instanceof PlayerScreenHandler) && handlers.containsKey(type)) {
				return (List<EmiRecipeHandler<T>>) (List<?>) handlers.get(type);
			}
			for (Slot slot : (List<Slot>) screen.screenHandler.slots) {
				if (slot instanceof CraftingResultSlot crs) {
					Inventory inv = ((CraftingResultSlotAccessor) crs).getInput();
					if (inv instanceof CraftingInventory cInv) {
						int width = ((CraftingInventoryAccessor) cInv).getWidth();
						//I know this is redundant...
						int height = cInv.getInvSize() / width;
						if (width > 0 && height > 0) {
							return List.of(new CoercedRecipeHandler<T>(crs, cInv));
						}
					}
				}
			}
		}
		return List.of();
	}

	@SuppressWarnings("unchecked")
	public static <T extends ScreenHandler> @Nullable EmiRecipeHandler<T> getFirstValidHandler(EmiRecipe recipe, HandledScreen screen) {
		EmiRecipeHandler<T> ret = null;
		for (EmiRecipeHandler<T> handler : (List<EmiRecipeHandler<T>>) (List<?>) getAllHandlers(screen)) {
			if (handler.supportsRecipe(recipe)) {
				ret = handler;
				break;
			}
		}
		if (ret == null || (ret instanceof CoercedRecipeHandler && !(screen instanceof InventoryScreen))) {
			EmiRecipeHandler<T> extra = (EmiRecipeHandler<T>) extraHandlers.apply(screen.screenHandler, recipe);
			if (extra != null) {
				ret = extra;
			}
		}
		return ret;
	}

	public static <T extends ScreenHandler> boolean performFill(EmiRecipe recipe, HandledScreen screen,
			EmiCraftContext.Type type, EmiCraftContext.Destination destination, int amount) {
		EmiRecipeHandler<T> handler = getFirstValidHandler(recipe, screen);
		if (handler != null && handler.supportsRecipe(recipe)) {
			EmiPlayerInventory inv = handler.getInventory(screen);
			EmiCraftContext<T> context = new EmiCraftContext<T>(screen, inv, type, destination, amount);
			if (handler.canCraft(recipe, context)) {
				EmiSidebars.craft(recipe);
				boolean crafted = handler.craft(recipe, context);
				if (crafted) {
					MinecraftClient.getInstance().setScreen(screen);
				}
				return crafted;
			}
		}
		return false;
	}

	public static <T extends ScreenHandler> @Nullable List<ItemStack> getStacks(StandardRecipeHandler<T> handler, EmiRecipe recipe, HandledScreen screen, int amount) {
		try {
			T screenHandler = (T) screen.screenHandler;
			if (handler != null) {
				List<Slot> slots = handler.getInputSources(screenHandler);
				List<Slot> craftingSlots = handler.getCraftingSlots(recipe, screenHandler);
				List<EmiIngredient> ingredients = recipe.getInputs();
				List<DiscoveredItem> discovered = Lists.newArrayList();
				Map<EmiStack, Integer> weightDivider = new HashMap<>();
				for (int i = 0; i < ingredients.size(); i++) {
					List<DiscoveredItem> d = Lists.newArrayList();
					EmiIngredient ingredient = ingredients.get(i);
					List<EmiStack> emiStacks = ingredient.getEmiStacks();
					if (ingredient.isEmpty()) {
						discovered.add(null);
						continue;
					}
					for (int e = 0; e < emiStacks.size(); e++) {
						EmiStack stack = emiStacks.get(e);
						slotLoop:
						for (Slot s : slots) {
							ItemStack ss = s.getStack();
							if (EmiStack.of(s.getStack()).isEqual(stack)) {
								for (DiscoveredItem di : d) {
									if (EmiUtil.canCombineIgnoreCount(ss, di.stack)) {
										di.amount += ss.count;
										continue slotLoop;
									}
								}
								d.add(new DiscoveredItem(stack, ss, ss.count, (int) ingredient.getAmount(), ss.getMaxCount()));
							}
						}
					}
					DiscoveredItem biggest = null;
					for (DiscoveredItem di : d) {
						if (biggest == null) {
							biggest = di;
						} else {
							int a = di.amount / (weightDivider.getOrDefault(di.ingredient, 0) + di.consumed);
							int ba = biggest.amount / (weightDivider.getOrDefault(biggest.ingredient, 0) + biggest.consumed);
							if (ba < a) {
								biggest = di;
							}
						}
					}
					if (biggest == null || i >= craftingSlots.size()) {
						return null;
					}
					Slot slot = craftingSlots.get(i);
					if (slot == null) {
						return null;
					}
					weightDivider.put(biggest.ingredient, weightDivider.getOrDefault(biggest.ingredient, 0) + biggest.consumed);
					biggest.max = Math.min(biggest.max, slot.getMaxStackAmount());
					discovered.add(biggest);
				}
				if (discovered.isEmpty()) {
					return null;
				}

				List<DiscoveredItem> unique = Lists.newArrayList();
				outer:
				for (DiscoveredItem di : discovered) {
					if (di == null) {
						continue;
					}
					for (DiscoveredItem ui : unique) {
						if (EmiUtil.canCombineIgnoreCount(di.stack, ui.stack)) {
							ui.consumed += di.consumed;
							continue outer;
						}
					}
					unique.add(new DiscoveredItem(di.ingredient, di.stack, di.amount, di.consumed, di.max));
				}
				int maxAmount = Integer.MAX_VALUE;
				for (DiscoveredItem ui : unique) {
					if (!ui.catalyst()) {
						maxAmount = Math.min(maxAmount, ui.amount / ui.consumed);
						maxAmount = Math.min(maxAmount, ui.max);
					}
				}
				maxAmount = Math.min(maxAmount, amount + batchesAlreadyPresent(recipe, handler, screen));

				if (maxAmount == 0) {
					return null;
				}

				List<ItemStack> desired = Lists.newArrayList();
				for (int i = 0; i < discovered.size(); i++) {
					DiscoveredItem di = discovered.get(i);
					if (di != null) {
						ItemStack is = di.stack.copy();
						int a = di.catalyst() ? di.consumed : di.consumed * maxAmount;
						is.count = a;
						desired.add(is);
					} else {
						desired.add(null);
					}
				}
				return desired;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static <T extends ScreenHandler> int batchesAlreadyPresent(EmiRecipe recipe, StandardRecipeHandler<T> handler, HandledScreen screen) {
		List<EmiIngredient> inputs = recipe.getInputs();
		List<ItemStack> stacks = Lists.newArrayList();
		Slot output = handler.getOutputSlot((T) screen.screenHandler);
		if (output != null && output.getStack() != null && recipe.getOutputs().size() > 0
				&& !output.getStack().getItem().equals(recipe.getOutputs().get(0).getItemStack().getItem())) {
			return 0;
		}
		for (Slot slot : handler.getCraftingSlots(recipe, (T) screen.screenHandler)) {
			if (slot != null) {
				stacks.add(slot.getStack());
			} else {
				stacks.add(null);
			}
		}
		long amount = Long.MAX_VALUE;
		outer:
		for (int i = 0; i < inputs.size(); i++) {
			EmiIngredient input = inputs.get(i);
			if (input.isEmpty()) {
				if (stacks.get(i) == null) {
					continue;
				}
				return 0;
			}
			if (i >= stacks.size()) {
				return 0;
			}
			EmiStack es = EmiStack.of(stacks.get(i));
			for (EmiStack v : input.getEmiStacks()) {
				if (v.isEmpty()) {
					continue;
				}
				if (v.isEqual(es) && es.getAmount() >= v.getAmount()) {
					amount = Math.min(amount, es.getAmount() / v.getAmount());
					continue outer;
				}
			}
			return 0;
		}
		if (amount < Long.MAX_VALUE && amount > 0) {
			return (int) amount;
		}
		return 0;
	}

	public static <T extends ScreenHandler> boolean clientFill(StandardRecipeHandler<T> handler, EmiRecipe recipe,
			HandledScreen screen, List<ItemStack> stacks, EmiCraftContext.Destination destination) {
		T screenHandler = (T) screen.screenHandler;
		MinecraftClient client = MinecraftClient.getInstance();
		PlayerEntity player = client.field_3805;
		if (handler != null && player.inventory.getCursorStack() != null) {
			ClientPlayerInteractionManager manager = client.interactionManager;
			List<Slot> clear = handler.getCraftingSlots(screenHandler);
			for (Slot slot : clear) {
				if (slot != null) {
					manager.clickSlot(screenHandler.syncId, slot.id, 0, 1, player);
				}
			}
			List<Slot> inputs = handler.getInputSources(screenHandler);
			List<Slot> slots = handler.getCraftingSlots(recipe, screenHandler);
			outer:
			for (int i = 0; i < stacks.size(); i++) {
				ItemStack stack = stacks.get(i);
				if (stack == null) {
					continue;
				}
				if (i >= slots.size()) {
					return false;
				}
				Slot crafting = slots.get(i);
				if (crafting == null) {
					return false;
				}
				int needed = stack.count;
				for (Slot input : inputs) {
					if (slots.contains(input)) {
						continue;
					}
					ItemStack is = input.getStack().copy();
					if (EmiUtil.canCombineIgnoreCount(is, stack)) {
						manager.clickSlot(screenHandler.syncId, input.id, 0, 0, player);
						if (is.count <= needed) {
							needed -= is.count;
							manager.clickSlot(screenHandler.syncId, crafting.id, 0, 0, player);
						} else {
							while (needed > 0) {
								manager.clickSlot(screenHandler.syncId, crafting.id, 1, 0, player);
								needed--;
							}
							manager.clickSlot(screenHandler.syncId, input.id, 0, 0, player);
						}
					}
					if (needed == 0) {
						continue outer;
					}
				}
				return false;
			}
			Slot slot = handler.getOutputSlot(screenHandler);
			if (slot != null) {
				if (destination == EmiCraftContext.Destination.CURSOR) {
					manager.clickSlot(screenHandler.syncId, slot.id, 0, 0, player);
				} else if (destination == EmiCraftContext.Destination.INVENTORY) {
					manager.clickSlot(screenHandler.syncId, slot.id, 0, 1, player);
				}
			}
			return true;
		}
		return false;
	}

	private static class DiscoveredItem {
		private static final Comparison COMPARISON = Comparison.DEFAULT_COMPARISON;
		public EmiStack ingredient;
		public ItemStack stack;
		public int consumed;
		public int amount;
		public int max;

		public DiscoveredItem(EmiStack ingredient, ItemStack stack, int amount, int consumed, int max) {
			this.ingredient = ingredient;
			this.stack = stack.copy();
			this.amount = amount;
			this.consumed = consumed;
			this.max = max;
		}

		public boolean catalyst() {
			return ingredient.getRemainder().isEqual(ingredient, COMPARISON);
		}
	}
}
