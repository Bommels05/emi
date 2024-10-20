package dev.emi.emi;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.bom.BoM;
import dev.emi.emi.data.EmiRecipeCategoryProperties;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.registry.EmiRecipeFiller;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraftforge.fluids.Fluid;

public class EmiUtil {
	public static final Random RANDOM = new Random();
	public static final Identifier UI_BUTTON_CLICK = new Identifier("gui.button.press");

	public static String subId(Identifier id) {
		return id.getNamespace() + "/" + id.getPath();
	}

	public static String subId(String id) {
		return subId(new Identifier(id));
	}

	public static String subId(Block block) {
		return subId(EmiPort.getBlockRegistry().getId((Object) block));
	}

	public static String subId(Item item) {
		return subId(EmiPort.getItemRegistry().getId((Object) item));
	}

	public static String subId(ItemStack stack) {
		return subId(stack.getItem()) + stack.getData();
	}

	public static String subId(Fluid fluid) {
		List<Map.Entry<String, Fluid>> result = EmiPort.getFluidRegistry().entrySet().stream().filter(entry -> entry.getValue() == fluid).collect(Collectors.toList());
		if (result.size() == 1) {
			return subId(result.get(0).getKey());
		} else {
			throw new IllegalStateException("Multiple or no ids associated with fluid: " + fluid + " (" + result.size() + ")");
		}
	}

	public static boolean showAdvancedTooltips() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.options.advancedItemTooltips;
	}

	public static String translateId(String prefix, Identifier id) {
		return prefix + id.getNamespace() + "." + id.getPath().replace('/', '.');
	}

	public static String getModName(String namespace) {
		return EmiAgnos.getModName(namespace);
	}

	public static List<String> getStackTrace(Throwable t) {
		StringWriter writer = new StringWriter();
		t.printStackTrace(new PrintWriter(writer, true));
		return Arrays.asList(writer.getBuffer().toString().split("\n"));
	}

	public static CraftingInventory getCraftingInventory() {
		return new CraftingInventory(new ScreenHandler() {

			@Override
			public boolean canUse(PlayerEntity player) {
				return false;
			}

			@Override
			public ItemStack transferSlot(PlayerEntity player, int index) {
				return null;
			}

			@Override
			public void onContentChanged(Inventory inventory) {
			}
		}, 3, 3);
	}

	public static int getOutputCount(EmiRecipe recipe, EmiIngredient stack) {
		int count = 0;
		for (EmiStack o : recipe.getOutputs()) {
			if (stack.getEmiStacks().contains(o)) {
				count += o.getAmount();
			}
		}
		return count;
	}

	public static EmiRecipe getPreferredRecipe(EmiIngredient ingredient, EmiPlayerInventory inventory, boolean requireCraftable) {
		if (ingredient.getEmiStacks().size() == 1 && !ingredient.isEmpty()) {
			HandledScreen hs = EmiApi.getHandledScreen();
			EmiStack stack = ingredient.getEmiStacks().get(0);
			return getPreferredRecipe(EmiApi.getRecipeManager().getRecipesByOutput(stack).stream().filter(r -> {
				@SuppressWarnings("rawtypes")
				EmiRecipeHandler handler = EmiRecipeFiller.getFirstValidHandler(r, hs);
				return handler != null && handler.supportsRecipe(r);
			}).toList(), inventory, requireCraftable);
		}
		return null;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static EmiRecipe getPreferredRecipe(List<EmiRecipe> recipes, EmiPlayerInventory inventory, boolean requireCraftable) {
		EmiRecipe preferred = null;
		int preferredWeight = -1;
		HandledScreen hs = EmiApi.getHandledScreen();
		EmiCraftContext context = new EmiCraftContext<>(hs, inventory, EmiCraftContext.Type.CRAFTABLE);
		for (EmiRecipe recipe : recipes) {
			if (!recipe.supportsRecipeTree()) {
				continue;
			}
			int weight = 0;
			EmiRecipeHandler handler = EmiRecipeFiller.getFirstValidHandler(recipe, hs);
			if (handler != null && handler.canCraft(recipe, context)) {
				weight += 16;
			} else if (requireCraftable) {
				continue;
			} else if (inventory.canCraft(recipe)) {
				weight += 8;
			}
			if (BoM.isRecipeEnabled(recipe)) {
				weight += 4;
			}
			if (recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING) {
				weight += 2;
			}
			if (weight > preferredWeight) {
				preferredWeight = weight;
				preferred = recipe;
			} else if (weight == preferredWeight) {
				if (EmiRecipeCategoryProperties.getOrder(recipe.getCategory()) < EmiRecipeCategoryProperties.getOrder(preferred.getCategory())) {
					preferredWeight = weight;
					preferred = recipe;
				}
			}
		}
		return preferred;
	}

	public static EmiRecipe getRecipeResolution(EmiIngredient ingredient, EmiPlayerInventory inventory) {
		if (ingredient.getEmiStacks().size() == 1 && !ingredient.isEmpty()) {
			EmiStack stack = ingredient.getEmiStacks().get(0);
			return getPreferredRecipe(EmiApi.getRecipeManager().getRecipesByOutput(stack).stream().filter(r -> {
					return r.getOutputs().stream().anyMatch(i -> i.isEqual(stack));
				}).toList(), inventory, false);
		}
		return null;
	}

	public static String replaceCharAt(String s, int index, char c) {
		return s.substring(0, index) + c + s.substring(index + 1);
	}

	public static boolean canCombineIgnoreCount(ItemStack stack, ItemStack other) {
		if (stack != null && stack.isStackable()) {
			if (other != null && other.isStackable()
					&& stack.getItem() == other.getItem()
					&& (!stack.isUnbreakable() || stack.getData() == other.getData())
					&& ItemStack.equalsIgnoreDamage(stack, other)) {
				return true;
			}
		}
		return false;
	}

	public static void offerOrDrop(PlayerEntity player, ItemStack stack) {
		if (!player.inventory.insertStack(stack)) {
			player.dropItem(stack, false);
		}
	}

	public static List<Element> gatherChildren(AbstractParentElement widget) {
		List<Element> children = new ArrayList<>();
		for (Element child : widget.children()) {
			children.add(child);
			if (child instanceof AbstractParentElement parent) {
				children.addAll(gatherChildren(parent));
			}
		}
		return children;
	}

	public static boolean hasTranslation(String key) {
		return I18n.storage.translations.containsKey(key);
	}

	public static int getColorValue(Formatting formatting) {
		return switch (formatting) {
			case BLACK -> 0x000000;
			case DARK_BLUE -> 0x0000AA;
			case DARK_GREEN -> 0x00AA00;
			case DARK_AQUA -> 0x00AAAA;
			case DARK_RED -> 0xAA0000;
			case DARK_PURPLE -> 0xAA00AA;
			case GOLD -> 0xFFAA00;
			case GRAY -> 0xAAAAAA;
			case DARK_GRAY -> 0x555555;
			case BLUE -> 0x5555FF;
			case GREEN -> 0x55FF55;
			case AQUA -> 0x55FFFF;
			case RED -> 0xFF5555;
			case LIGHT_PURPLE -> 0xFF55FF;
			case YELLOW -> 0x55FFFF;
			case WHITE -> 0xFFFFFF;
			default -> -1;
		};
	}

	public static int compareIdentifier(Identifier first, Identifier second) {
		int i = first.getPath().compareTo(second.getPath());
		if (i != 0) return i;
		return first.getNamespace().compareTo(second.getNamespace());
	}

	public static int mapScrollAmount(int amount) {
		return Integer.compare(amount, 0);
	}

	public static void deParentText(Text text) {
		for (Text sibling : (List<Text>) text.getSiblings()) {
			sibling.getStyle().setParent(null);
			deParentText(sibling);
		}
	}
}
