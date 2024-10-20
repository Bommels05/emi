package dev.emi.emi.recipe.special;

import java.util.List;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraftforge.common.util.Constants;

public class EmiAnvilEnchantRecipe implements EmiRecipe {
	private final Item tool;
	private final Enchantment enchantment;
	private final int level;
	private final Identifier id;

	public EmiAnvilEnchantRecipe(Item tool, Enchantment enchantment, int level, Identifier id) {
		this.tool = tool;
		this.enchantment = enchantment;
		this.level = level;
		this.id = id;
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return VanillaEmiRecipeCategories.ANVIL_REPAIRING;
	}

	@Override
	public Identifier getId() {
		return id;
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return List.of(EmiStack.of(tool), getBook());
	}

	@Override
	public List<EmiStack> getOutputs() {
		return List.of(EmiStack.of(tool));
	}

	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public int getDisplayWidth() {
		return 125;
	}

	@Override
	public int getDisplayHeight() {
		return 18;
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		widgets.addTexture(EmiTexture.PLUS, 27, 3);
		widgets.addTexture(EmiTexture.EMPTY_ARROW, 75, 1);
		widgets.addSlot(EmiStack.of(tool), 0, 0);
		widgets.addSlot(getBook(), 49, 0);
		widgets.addSlot(EmiStack.of(getTool()), 107, 0).recipeContext(this);
	}

	private ItemStack getTool() {
		ItemStack itemStack = new ItemStack(tool);
		itemStack.addEnchantment(enchantment, level);
		return itemStack;
	}

	private EmiStack getBook() {
		ItemStack item = new ItemStack(Items.ENCHANTED_BOOK);
		NbtCompound tag = new NbtCompound();
		NbtList StoredEnchantments = new NbtList();
		NbtCompound enchant = new NbtCompound();
		int id = getTool().getNbt().getList("ench", Constants.NBT.TAG_COMPOUND).getCompound(0).getInt("id");

		enchant.putInt("id", id);
		enchant.putShort("lvl", (short) level);
		StoredEnchantments.add(enchant);
		tag.put("StoredEnchantments", StoredEnchantments);
		item.setNbt(tag);
		return EmiStack.of(item);
	}
}
