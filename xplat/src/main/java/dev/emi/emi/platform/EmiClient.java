package dev.emi.emi.platform;

import java.util.List;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.network.EmiNetwork;
import dev.emi.emi.network.FillRecipeC2SPacket;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.inventory.slot.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

public class EmiClient {
	public static boolean onServer = false;

	public static void init() {
		EmiConfig.loadConfig();
	}

	public static <T extends ScreenHandler> void sendFillRecipe(StandardRecipeHandler<T> handler, HandledScreen screen,
			int syncId, int action, List<ItemStack> stacks, EmiRecipe recipe) {
		T screenHandler = (T) screen.screenHandler;
		List<Slot> crafting = handler.getCraftingSlots(recipe, screenHandler);
		Slot output = handler.getOutputSlot(screenHandler);
		EmiNetwork.sendToServer(new FillRecipeC2SPacket(screenHandler, action, handler.getInputSources(screenHandler), crafting, output, stacks));
	}
}
