package dev.emi.emi;

import static dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.ANVIL_REPAIRING;
import static dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.BREWING;
import static dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.CRAFTING;
import static dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.FUEL;
import static dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.INFO;
import static dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.SMELTING;
import static dev.emi.emi.api.recipe.VanillaEmiRecipeCategories.WORLD_INTERACTION;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.mojang.realmsclient.util.Pair;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiInitRegistry;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeSorting;
import dev.emi.emi.api.recipe.EmiWorldInteractionRecipe;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.GeneratedSlotWidget;
import dev.emi.emi.backport.CookingRecipe;
import dev.emi.emi.backport.ItemKey;
import dev.emi.emi.backport.TagKey;
import dev.emi.emi.config.EffectLocation;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.config.FluidUnit;
import dev.emi.emi.handler.CookingRecipeHandler;
import dev.emi.emi.handler.CraftingRecipeHandler;
import dev.emi.emi.handler.InventoryRecipeHandler;
import dev.emi.emi.mixin.accessor.*;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.recipe.*;
import dev.emi.emi.recipe.special.EmiAnvilEnchantRecipe;
import dev.emi.emi.recipe.special.EmiAnvilRepairItemRecipe;
import dev.emi.emi.recipe.special.EmiArmorDyeRecipe;
import dev.emi.emi.recipe.special.EmiBookCloningRecipe;
import dev.emi.emi.recipe.special.EmiFireworkRocketRecipe;
import dev.emi.emi.recipe.special.EmiFireworkStarFadeRecipe;
import dev.emi.emi.recipe.special.EmiFireworkStarRecipe;
import dev.emi.emi.recipe.special.EmiMapCloningRecipe;
import dev.emi.emi.recipe.special.EmiRepairItemRecipe;
import dev.emi.emi.registry.EmiTags;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadLog;
import dev.emi.emi.stack.serializer.FluidEmiStackSerializer;
import dev.emi.emi.stack.serializer.ItemEmiStackSerializer;
import dev.emi.emi.stack.serializer.TagEmiIngredientSerializer;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.*;
import net.minecraft.item.itemgroup.ItemGroup;
import net.minecraft.recipe.*;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.Weight;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

@EmiEntrypoint
public class VanillaPlugin implements EmiPlugin {
	public static EmiRecipeCategory TAG = new EmiRecipeCategory(EmiPort.id("emi:tag"),
		EmiStack.of(Items.NAME_TAG), simplifiedRenderer(240, 208), EmiRecipeSorting.none());
	
	public static EmiRecipeCategory INGREDIENT = new EmiRecipeCategory(EmiPort.id("emi:ingredient"),
		EmiStack.of(Items.COMPASS), simplifiedRenderer(240, 208));
	public static EmiRecipeCategory RESOLUTION = new EmiRecipeCategory(EmiPort.id("emi:resolution"),
		EmiStack.of(Items.COMPASS), simplifiedRenderer(240, 208));

	static {
		CRAFTING = new EmiRecipeCategory(EmiPort.id("minecraft:crafting"),
			EmiStack.of(Blocks.CRAFTING_TABLE), simplifiedRenderer(240, 240), EmiRecipeSorting.compareOutputThenInput());
		SMELTING = new EmiRecipeCategory(EmiPort.id("minecraft:smelting"),
			EmiStack.of(Blocks.FURNACE), simplifiedRenderer(224, 240), EmiRecipeSorting.compareOutputThenInput());
		ANVIL_REPAIRING = new EmiRecipeCategory(new Identifier("emi:anvil_repairing"),
			EmiStack.of(Blocks.ANVIL), simplifiedRenderer(240, 224), EmiRecipeSorting.none());
		BREWING = new EmiRecipeCategory(EmiPort.id("minecraft:brewing"),
			EmiStack.of(Items.BREWING_STAND), simplifiedRenderer(224, 224), EmiRecipeSorting.none());
		WORLD_INTERACTION = new EmiRecipeCategory(EmiPort.id("emi:world_interaction"),
			EmiStack.of(Blocks.GRASS), simplifiedRenderer(208, 224), EmiRecipeSorting.none());
		EmiRenderable flame = (matrices, x, y, delta) -> {
			EmiTexture.FULL_FLAME.render(matrices, x + 1, y + 1, delta);
		};
		FUEL = new EmiRecipeCategory(EmiPort.id("emi:fuel"), flame, flame, EmiRecipeSorting.compareInputThenOutput());
		INFO = new EmiRecipeCategory(EmiPort.id("emi:info"),
			EmiStack.of(Items.WRITABLE_BOOK), simplifiedRenderer(208, 224), EmiRecipeSorting.none());
	}


	@Override
	public void initialize(EmiInitRegistry registry) {
		registry.addIngredientSerializer(ItemEmiStack.class, new ItemEmiStackSerializer());
		registry.addIngredientSerializer(FluidEmiStack.class, new FluidEmiStackSerializer());
		registry.addIngredientSerializer(TagEmiIngredient.class, new TagEmiIngredientSerializer());

		registry.addRegistryAdapter(EmiRegistryAdapter.simple(ItemKey.class, TagKey.Type.ITEM, (key, nbt, amount) -> EmiStack.of(key.item(), nbt, amount, key.meta())));
		registry.addRegistryAdapter(EmiRegistryAdapter.simple(Fluid.class, TagKey.Type.FLUID, EmiStack::of));
	}

	@Override
	public void register(EmiRegistry registry) {
		registry.addCategory(CRAFTING);
		registry.addCategory(SMELTING);
		registry.addCategory(ANVIL_REPAIRING);
		registry.addCategory(BREWING);
		registry.addCategory(WORLD_INTERACTION);
		registry.addCategory(FUEL);
		registry.addCategory(INFO);
		registry.addCategory(TAG);
		registry.addCategory(INGREDIENT);
		registry.addCategory(RESOLUTION);

		registry.addWorkstation(CRAFTING, EmiStack.of(Blocks.CRAFTING_TABLE));
		registry.addWorkstation(SMELTING, EmiStack.of(Blocks.FURNACE));
		registry.addWorkstation(ANVIL_REPAIRING, EmiStack.of(Blocks.ANVIL));
		registry.addWorkstation(ANVIL_REPAIRING, EmiStack.of(Blocks.ANVIL, 1, 1)); // damaged 1
		registry.addWorkstation(ANVIL_REPAIRING, EmiStack.of(Blocks.ANVIL, 1, 2)); // damaged 2
		registry.addWorkstation(BREWING, EmiStack.of(Items.BREWING_STAND));

		registry.addRecipeHandler(null, new InventoryRecipeHandler());
		registry.addRecipeHandler(CraftingScreenHandler.class, new CraftingRecipeHandler());
		registry.addRecipeHandler(FurnaceScreenHandler.class, new CookingRecipeHandler<>(SMELTING));

		registry.addExclusionArea(CreativeInventoryScreen.class, (screen, consumer) -> {
			int left = ((HandledScreenAccessor) screen).getX();
			int top = ((HandledScreenAccessor) screen).getY();
			int width = ((HandledScreenAccessor) screen).getBackgroundWidth();
			int bottom = top + ((HandledScreenAccessor) screen).getBackgroundHeight();
			consumer.accept(new Bounds(left, top - 28, width, 28));
			consumer.accept(new Bounds(left, bottom, width, 28));
		});

		registry.addGenericExclusionArea((screen, consumer) -> {
			if (screen instanceof InventoryScreen inv) {
				MinecraftClient client = MinecraftClient.getInstance();
				Collection<StatusEffectInstance> collection = client.field_3805.getStatusEffectInstances();
				if (!collection.isEmpty()) {
					int k = 33;
					if (collection.size() > 5) {
						k = 132 / (collection.size() - 1);
					}
					int right = ((HandledScreenAccessor) inv).getX() + ((HandledScreenAccessor) inv).getBackgroundWidth() + 2;
					int rightWidth = inv.width - right;
					if (rightWidth >= 32) {
						int top = ((HandledScreenAccessor) inv).getY();
						int height = (collection.size() - 1) * k + 32;
						int left, width;
						if (EmiConfig.effectLocation == EffectLocation.TOP) {
							int size = collection.size();
							top = ((HandledScreenAccessor) inv).getY() - 34;
							if (((Object) screen) instanceof CreativeInventoryScreen) {
								top -= 28;
								if (EmiAgnos.isForge()) {
									top -= 22;
								}
							}
							int xOff = 34;
							if (size == 1) {
								xOff = 122;
							} else if (size > 5) {
								xOff = (((HandledScreenAccessor) inv).getBackgroundWidth() - 32) / (size - 1);
							}
							width = Math.max(122, (size - 1) * xOff + 32);
							left = ((HandledScreenAccessor) inv).getX() + (((HandledScreenAccessor) inv).getBackgroundWidth() - width) / 2;
							height = 32;
						} else {
							left = switch (EmiConfig.effectLocation) {
								case LEFT_COMPRESSED -> ((HandledScreenAccessor) inv).getX() - 2 - 32;
								case LEFT -> ((HandledScreenAccessor) inv).getX() - 2 - 120;
								default -> right;
							};
							width = switch (EmiConfig.effectLocation) {
								case LEFT, RIGHT -> 120;
								case LEFT_COMPRESSED, RIGHT_COMPRESSED -> 32;
								default -> 32;
							};
						}
						consumer.accept(new Bounds(left, top, width, height));
					}
				}
			}
		});

		Comparison potionComparison = Comparison.compareData(stack -> ((PotionItem) stack.getItemStack().getItem()).getCustomPotionEffects(stack.getItemStack()));

		registry.setDefaultComparison(Items.POTION, potionComparison);
		registry.setDefaultComparison(Items.ENCHANTED_BOOK, EmiPort.compareStrict());

		Set<Item> hiddenItems = Stream.concat(
			TagKey.of(ItemKey.class, EmiTags.HIDDEN_FROM_RECIPE_VIEWERS).getAll().stream().map(ItemKey::item),
			EmiPort.getDisabledItems()
		).collect(Collectors.toSet());

		//todo check how this is in nei
		List<Item> dyeableItems = EmiPort.getAllItems().stream().filter(i -> i instanceof ArmorItem armor && armor.method_4602() == ArmorMaterial.CLOTH).collect(Collectors.toList());

		for (RecipeType recipe : (List<RecipeType>) RecipeDispatcher.getInstance().getAllRecipes()) {
			Identifier id = EmiPort.getId(recipe);
			if (recipe instanceof MapUpscaleRecipeType map) {
				EmiStack paper = EmiStack.of(Items.PAPER);
				addRecipeSafe(registry, () -> new EmiCraftingRecipe(List.of(
						paper, paper, paper, paper,
						EmiStack.of(Items.FILLED_MAP),
						paper, paper, paper, paper
				), 
						EmiStack.of(Items.FILLED_MAP),
						id, false), recipe);
			} else if (recipe instanceof ShapedRecipeType shaped) {
				ShapedRecipeTypeAccessor accessor = (ShapedRecipeTypeAccessor) shaped;
				if (accessor.getWidth() <= 3 && accessor.getHeight() <= 3) {
					addRecipeSafe(registry, () -> new EmiShapedRecipe(shaped), recipe);
				}
			} else if (recipe instanceof ShapelessRecipeType shapeless && recipe.getSize() <= 9) {
				addRecipeSafe(registry, () -> new EmiShapelessRecipe(shapeless), recipe);
			} else if (recipe instanceof ShapedOreRecipe shaped) {
				int width = EmiShapedOreRecipe.getWidth(shaped);
				if (width <= 3 && shaped.getSize() / width <= 3) {
					addRecipeSafe(registry, () -> new EmiShapedOreRecipe(shaped));
				}
			} else if (recipe instanceof ShapelessOreRecipe shapeless && recipe.getSize() <= 9) {
				addRecipeSafe(registry, () -> new EmiShapelessOreRecipe(shapeless));
			} else if (recipe instanceof ArmorDyeRecipeType dye) {
				for (Item i : dyeableItems) {
					if (!hiddenItems.contains(i)) {
						addRecipeSafe(registry, () -> new EmiArmorDyeRecipe(i, synthetic("crafting/dying", EmiUtil.subId(i))), recipe);
					}
				}
			/*} else if (recipe instanceof SuspiciousStewRecipe stew) {
				addRecipeSafe(registry, () -> new EmiSuspiciousStewRecipe(id), recipe);
			} else if (recipe instanceof ShulkerBoxColoringRecipe shulker) {
				for (DyeColor dye : DyeColor.values()) {
					DyeItem dyeItem = DyeItem.byColor(dye);
					Identifier sid = synthetic("crafting/shulker_box_dying", EmiUtil.subId(dyeItem));
					addRecipeSafe(registry, () -> new EmiCraftingRecipe(
						List.of(EmiStack.of(Items.SHULKER_BOX), EmiStack.of(dyeItem)),
						EmiStack.of(ShulkerBoxBlock.getItemStack(dye)), sid), recipe);
				}*/
			/*} else if (recipe instanceof ShieldDecorationRecipe shield) {
				addRecipeSafe(registry, () -> new EmiBannerShieldRecipe(id), recipe);*/
			} else if (recipe instanceof BookCloningRecipeType book) {
				addRecipeSafe(registry, () -> new EmiBookCloningRecipe(id), recipe);
			} else if (recipe instanceof FireworkRecipeType star) {
				addRecipeSafe(registry, () -> new EmiFireworkStarRecipe(new Identifier("minecraft:firework_star")), recipe);
				addRecipeSafe(registry, () -> new EmiFireworkStarFadeRecipe(new Identifier("minecraft:firework_star_fade")), recipe);
				addRecipeSafe(registry, () -> new EmiFireworkRocketRecipe(new Identifier("minecraft:firework_rocket")), recipe);
			/*} else if (recipe instanceof BannerDuplicateRecipe banner) {
				for (Item i : EmiBannerDuplicateRecipe.BANNERS) {
					if (!hiddenItems.contains(i)) {
						addRecipeSafe(registry, () -> new EmiBannerDuplicateRecipe(i, synthetic("crafting/banner_copying", EmiUtil.subId(i))), recipe);
					}
				}*/
			} else if (recipe instanceof MapCloningRecipeType map) {
				addRecipeSafe(registry, () -> new EmiMapCloningRecipe(id), recipe);
			} else {
				EmiReloadLog.warn("Could not load unknown recipe type: " + recipe);
			}
		}

		for (Item i : EmiRepairItemRecipe.TOOLS) {
			if (!hiddenItems.contains(i)) {
				addRecipeSafe(registry, () -> new EmiRepairItemRecipe(i, synthetic("crafting/repairing", EmiUtil.subId(i))), "Item repairing");
			}
		}

		//Smelting recipes are compressed so things like charcoal don't get split, and they are missing tag support like fuel recipes
		Map<ItemKey, ItemKey> smeltingRecipes = new HashMap<>();
		((Map<ItemStack, ItemStack>) SmeltingRecipeRegistry.getInstance().getRecipeMap()).forEach((in, out) -> {
			for (ItemStack stack : EmiStack.ofPotentialTag(in).getEmiStacks().stream().map(EmiStack::getItemStack).toList()) {
				if (smeltingRecipes.put(ItemKey.of(stack), ItemKey.of(out)) != null) {
					throw new IllegalArgumentException("Duplicate smelting recipe: " + in + "=" + out);
				}
			}
		});

		compressRecipesToTags(smeltingRecipes.keySet(), Comparator.comparingInt(stack -> smeltingRecipes.get(stack).hashCode()), tag -> {
			EmiIngredient input = EmiIngredient.of(tag);
			ItemStack output = smeltingRecipes.get(ItemKey.of(input.getEmiStacks().get(0).getItemStack())).toStack();
			addRecipeSafe(registry, () -> new EmiCookingRecipe(new CookingRecipe(input, output,
					SmeltingRecipeRegistry.getInstance().getXp(output)), SMELTING, 1, false));
		}, key -> {
			ItemStack output = smeltingRecipes.get(key).toStack();
			addRecipeSafe(registry, () -> new EmiCookingRecipe(new CookingRecipe(EmiStack.ofPotentialTag(key.toStack()), output,
					SmeltingRecipeRegistry.getInstance().getXp(output)), SMELTING, 1, false));
		});
		/*for (BlastingRecipe recipe : getRecipes(registry, RecipeType.BLASTING)) {
			addRecipeSafe(registry, () -> new EmiCookingRecipe(recipe, BLASTING, 2, false), recipe);
		}
		for (SmokingRecipe recipe : getRecipes(registry, RecipeType.SMOKING)) {
			addRecipeSafe(registry, () -> new EmiCookingRecipe(recipe, SMOKING, 2, false), recipe);
		}
		for (CampfireCookingRecipe recipe : getRecipes(registry, RecipeType.CAMPFIRE_COOKING)) {
			addRecipeSafe(registry, () -> new EmiCookingRecipe(recipe, CAMPFIRE_COOKING, 1, true), recipe);
		}
		for (SmithingRecipe recipe : getRecipes(registry, RecipeType.SMITHING)) {
			addRecipeSafe(registry, () -> new EmiSmithingRecipe(recipe), recipe);
		}
		for (StonecuttingRecipe recipe : getRecipes(registry, RecipeType.STONECUTTING)) {
			addRecipeSafe(registry, () -> new EmiStonecuttingRecipe(recipe), recipe);
		}*/

		safely("repair", () -> addRepair(registry, hiddenItems));
		safely("brewing", () -> EmiAgnos.addBrewingRecipes(registry));
		safely("world interaction", () -> addWorldInteraction(registry, hiddenItems, dyeableItems));
		safely("fuel", () -> addFuel(registry, hiddenItems));
		//safely("composting", () -> addComposting(registry, hiddenItems));

		for (TagKey<?> key : EmiTags.TAGS) {
			if (new TagEmiIngredient(key, 1).getEmiStacks().size() > 1) {
				addRecipeSafe(registry, () -> new EmiTagRecipe(key));
			}
		}
	}

	private static void addRepair(EmiRegistry registry, Set<Item> hiddenItems) {
		List<Enchantment> targetedEnchantments = Lists.newArrayList();
		List<Enchantment> universalEnchantments = Lists.newArrayList();
		for (Enchantment enchantment : Arrays.stream(Enchantment.ALL_ENCHANTMENTS).filter(Objects::nonNull).toList()) {
			try {
				if (enchantment.isAcceptableItem(new ItemStack(Blocks.AIR))) {
					universalEnchantments.add(enchantment);
					continue;
				}
			} catch (Throwable t) {
			}
			targetedEnchantments.add(enchantment);
			for (int i = 1; i <= enchantment.getMaximumLevel(); i++) {
				registry.addEmiStack(EmiStack.of(Items.ENCHANTED_BOOK.getAsItemStack(new EnchantmentLevelEntry(enchantment, i))));
			}
		}
		for (Item i : EmiPort.getAllItems()) {
			if (hiddenItems.contains(i)) {
				continue;
			}
			try {
				if (i.getMaxDamage() > 0) {
					if (i instanceof ArmorItem ai && ai.method_4602() != null && ai.method_4602().method_6339() != null) {
						Identifier id = synthetic("anvil/repairing/material", EmiUtil.subId(i) + "/" + EmiUtil.subId(ai.method_4602().method_6339()));
						addRecipeSafe(registry, () -> new EmiAnvilRecipe(EmiStack.of(i), EmiStack.of(ai.method_4602().method_6339()), id));
					} else if (i instanceof ToolItem ti && ti.method_6345().method_6370() != null) {
						Identifier id = synthetic("anvil/repairing/material", EmiUtil.subId(i) + "/" + EmiUtil.subId(ti.method_6345().method_6370()));
						addRecipeSafe(registry, () -> new EmiAnvilRecipe(EmiStack.of(i), EmiStack.of(ti.method_6345().method_6370()), id));
					}
				}
				if (i.isDamageable()) {
					addRecipeSafe(registry, () -> new EmiAnvilRepairItemRecipe(i, synthetic("anvil/repairing/tool", EmiUtil.subId(i))));
					//addRecipeSafe(registry, () -> new EmiGrindstoneRecipe(i, synthetic("grindstone/repairing", EmiUtil.subId(i))));
				}
			} catch (Throwable t) {
				EmiLog.error("Exception thrown registering repair recipes");
				t.printStackTrace();
			}
			try {
				ItemStack defaultStack = new ItemStack(i);
				int acceptableEnchantments = 0;
				Consumer<Enchantment> consumer = e -> {
					int max = e.getMaximumLevel();
					addRecipeSafe(registry, () -> new EmiAnvilEnchantRecipe(i, e, max,
						synthetic("anvil/enchanting", EmiUtil.subId(i) + "/" + e.getTranslationKey() + "/" + max)));
				};
				for (Enchantment e : targetedEnchantments) {
					if (e.isAcceptableItem(defaultStack)) {
						consumer.accept(e);
						acceptableEnchantments++;
					}
				}
				if (acceptableEnchantments > 0) {
					for (Enchantment e : universalEnchantments) {
						if (e.isAcceptableItem(defaultStack)) {
							consumer.accept(e);
							acceptableEnchantments++;
						}
					}
					//addRecipeSafe(registry, () -> new EmiGrindstoneDisenchantingRecipe(i, synthetic("grindstone/disenchanting/tool", EmiUtil.subId(i))));
				}
			} catch (Throwable t) {
				EmiReloadLog.warn("Exception thrown registering enchantment recipes");
				EmiReloadLog.error(t);
			}
		}
		List<ItemStack> stacks = new ArrayList<>();
		Blocks.DOUBLE_PLANT.appendItemStacks(Item.fromBlock(Blocks.DOUBLE_PLANT), ItemGroup.MISC, stacks);
		for (ItemStack stack : stacks) {
			//Not grass or fern
			if (stack.getData() != 2 && stack.getData() != 3) {
				addRecipeSafe(registry, () -> basicWorld(EmiStack.of(stack).setRemainder(EmiStack.of(stack)), EmiStack.of(Items.DYE, 1, 15), EmiStack.of(stack),
						synthetic("world/flower_duping", EmiUtil.subId(stack)), false));
			}
		}

		/*addRecipeSafe(registry, () -> new EmiAnvilRecipe(EmiStack.of(Items.ELYTRA), EmiStack.of(Items.PHANTOM_MEMBRANE),
			synthetic("anvil/repairing/material", EmiUtil.subId(Items.ELYTRA) + "/" + EmiUtil.subId(Items.PHANTOM_MEMBRANE))));
		addRecipeSafe(registry, () -> new EmiAnvilRecipe(EmiStack.of(Items.SHIELD), EmiIngredient.of(ItemTags.PLANKS),
			synthetic("anvil/repairing/material", EmiUtil.subId(Items.SHIELD) + "/" + EmiUtil.subId(Items.OAK_PLANKS))));*/

		/*for (Enchantment e : List.of(Enchantment.ALL_ENCHANTMENTS)) {
			if (!e.isCursed()) {
				int max = Math.min(10, e.getMaxLevel());
				int min = e.getMinLevel();
				while (min <= max) {
					int level = min;
					addRecipeSafe(registry, () -> new EmiGrindstoneDisenchantingBookRecipe(e, level,
						synthetic("grindstone/disenchanting/book", EmiUtil.subId(EmiPort.getEnchantmentRegistry().getId(e)) + "/" + level)));
					min++;
				}
			}
		}*/
	}

	private static void addWorldInteraction(EmiRegistry registry, Set<Item> hiddenItems, List<Item> dyeableItems) {
		/*EmiStack concreteWater = EmiStack.of(Fluids.WATER);
		concreteWater.setRemainder(concreteWater);
		addConcreteRecipe(registry, Blocks.WHITE_CONCRETE_POWDER, concreteWater, Blocks.WHITE_CONCRETE);
		addConcreteRecipe(registry, Blocks.ORANGE_CONCRETE_POWDER, concreteWater, Blocks.ORANGE_CONCRETE);
		addConcreteRecipe(registry, Blocks.MAGENTA_CONCRETE_POWDER, concreteWater, Blocks.MAGENTA_CONCRETE);
		addConcreteRecipe(registry, Blocks.LIGHT_BLUE_CONCRETE_POWDER, concreteWater, Blocks.LIGHT_BLUE_CONCRETE);
		addConcreteRecipe(registry, Blocks.YELLOW_CONCRETE_POWDER, concreteWater, Blocks.YELLOW_CONCRETE);
		addConcreteRecipe(registry, Blocks.LIME_CONCRETE_POWDER, concreteWater, Blocks.LIME_CONCRETE);
		addConcreteRecipe(registry, Blocks.PINK_CONCRETE_POWDER, concreteWater, Blocks.PINK_CONCRETE);
		addConcreteRecipe(registry, Blocks.GRAY_CONCRETE_POWDER, concreteWater, Blocks.GRAY_CONCRETE);
		addConcreteRecipe(registry, Blocks.LIGHT_GRAY_CONCRETE_POWDER, concreteWater, Blocks.LIGHT_GRAY_CONCRETE);
		addConcreteRecipe(registry, Blocks.CYAN_CONCRETE_POWDER, concreteWater, Blocks.CYAN_CONCRETE);
		addConcreteRecipe(registry, Blocks.PURPLE_CONCRETE_POWDER, concreteWater, Blocks.PURPLE_CONCRETE);
		addConcreteRecipe(registry, Blocks.BLUE_CONCRETE_POWDER, concreteWater, Blocks.BLUE_CONCRETE);
		addConcreteRecipe(registry, Blocks.BROWN_CONCRETE_POWDER, concreteWater, Blocks.BROWN_CONCRETE);
		addConcreteRecipe(registry, Blocks.GREEN_CONCRETE_POWDER, concreteWater, Blocks.GREEN_CONCRETE);
		addConcreteRecipe(registry, Blocks.RED_CONCRETE_POWDER, concreteWater, Blocks.RED_CONCRETE);
		addConcreteRecipe(registry, Blocks.BLACK_CONCRETE_POWDER, concreteWater, Blocks.BLACK_CONCRETE);*/

		/*EmiIngredient axes = damagedTool(getPreferredTag(List.of(
				"minecraft:axes", "c:axes", "c:tools/axes", "fabric:axes", "forge:tools/axes"
			), EmiStack.of(Items.IRON_AXE)), 1);
		for (Map.Entry<Block, Block> entry : AxeItemAccessor.getStrippedBlocks().entrySet()) {
			Identifier id = synthetic("world/stripping", EmiUtil.subId(entry.getKey()));
			addRecipeSafe(registry, () -> basicWorld(EmiStack.of(entry.getKey()), axes, EmiStack.of(entry.getValue()), id));
		}
		for (Map.Entry<Block, Block> entry : Oxidizable.OXIDATION_LEVEL_DECREASES.get().entrySet()) {
			Identifier id = synthetic("world/stripping", EmiUtil.subId(entry.getKey()));
			addRecipeSafe(registry, () -> basicWorld(EmiStack.of(entry.getKey()), axes, EmiStack.of(entry.getValue()), id));
		}
		for (Map.Entry<Block, Block> entry : HoneycombItem.WAXED_TO_UNWAXED_BLOCKS.get().entrySet()) {
			Identifier id = synthetic("world/stripping", EmiUtil.subId(entry.getKey()));
			addRecipeSafe(registry, () -> basicWorld(EmiStack.of(entry.getKey()), axes, EmiStack.of(entry.getValue()), id));
		}*/
		
		/*EmiIngredient shears = damagedTool(EmiStack.of(Items.SHEARS), 1);
		addRecipeSafe(registry, () -> EmiWorldInteractionRecipe.builder()
			.id(synthetic("world/shearing", "minecraft/pumpkin"))
			.leftInput(EmiStack.of(Items.PUMPKIN))
			.rightInput(shears, true)
			.output(EmiStack.of(Items.PUMPKIN_SEEDS, 4))
			.output(EmiStack.of(Items.CARVED_PUMPKIN))
			.build());*/
		//todo right tags
		EmiIngredient hoes = damagedTool(getPreferredTag(List.of(
				"minecraft:hoes", "c:hoes", "c:tools/hoes", "fabric:hoes", "forge:tools/hoes"
			), EmiStack.of(Items.IRON_HOE)), 1);
		EmiIngredient dirt = EmiIngredient.of(List.of(EmiStack.of(Blocks.DIRT), EmiStack.of(Blocks.GRASS)));
		Identifier id = synthetic("world/tilling", EmiUtil.subId(Blocks.DIRT));
		addRecipeSafe(registry, () -> basicWorld(dirt, hoes, EmiStack.of(Blocks.FARMLAND), id));

		EmiIngredient shovels = damagedTool(getPreferredTag(List.of(
				"minecraft:shovels", "c:shovels", "c:tools/shovels", "fabric:shovels", "forge:tools/shovels"
			), EmiStack.of(Items.IRON_SHOVEL)), 1);
		/*for (Map.Entry<Block, BlockState> entry : ShovelItemAccessor.getPathStates().entrySet()) {
			Block result = entry.getValue().getBlock();
			Identifier id = synthetic("world/flattening", EmiUtil.subId(entry.getKey()));
			addRecipeSafe(registry, () -> basicWorld(EmiStack.of(entry.getKey()), shovels, EmiStack.of(result), id));
		}*/

		/*EmiIngredient honeycomb = EmiStack.of(Items.HONEYCOMB);
		for (Map.Entry<Block, Block> entry : HoneycombItem.UNWAXED_TO_WAXED_BLOCKS.get().entrySet()) {
			Identifier id = synthetic("world/waxing", EmiUtil.subId(entry.getKey()));
			addRecipeSafe(registry, () -> basicWorld(EmiStack.of(entry.getKey()), honeycomb, EmiStack.of(entry.getValue()), id, false));
		}*/

		for (Item i : dyeableItems) {
			if (hiddenItems.contains(i)) {
				continue;
			}
			EmiStack cauldron = EmiStack.of(Items.CAULDRON);
			EmiStack waterThird = EmiStack.of(FluidRegistry.WATER, FluidUnit.BOTTLE);
			int uniq = EmiUtil.RANDOM.nextInt();
			addRecipeSafe(registry, () -> EmiWorldInteractionRecipe.builder()
				.id(synthetic("world/cauldron_washing", EmiUtil.subId(i)))
				.leftInput(EmiStack.EMPTY, s -> new GeneratedSlotWidget(r -> {
					ItemStack stack = new ItemStack(i);
					((ArmorItem) i).setColor(stack, r.nextInt(0xFFFFFF + 1));
					return EmiStack.of(stack);
				}, uniq, s.getBounds().x(), s.getBounds().y()))
				.rightInput(cauldron, true)
				.rightInput(waterThird, false)
				.output(EmiStack.of(i))
				.supportsRecipeTree(false)
				.build());
		}

		EmiStack water = EmiStack.of(FluidRegistry.WATER, FluidUnit.BUCKET);
		EmiStack lava = EmiStack.of(FluidRegistry.LAVA, FluidUnit.BUCKET);
		EmiStack waterCatalyst = water.copy().setRemainder(water);
		EmiStack lavaCatalyst = lava.copy().setRemainder(lava);

		addRecipeSafe(registry, () -> EmiWorldInteractionRecipe.builder()
			.id(synthetic("world/fluid_spring", "minecraft/water"))
			.leftInput(waterCatalyst)
			.rightInput(waterCatalyst, false)
			.output(EmiStack.of(FluidRegistry.WATER, FluidUnit.BUCKET))
			.build());
		addRecipeSafe(registry, () -> EmiWorldInteractionRecipe.builder()
			.id(synthetic("world/fluid_interaction", "minecraft/cobblestone"))
			.leftInput(waterCatalyst)
			.rightInput(lavaCatalyst, false)
			.output(EmiStack.of(Blocks.COBBLESTONE))
			.build());
		addRecipeSafe(registry, () -> EmiWorldInteractionRecipe.builder()
			.id(synthetic("world/fluid_interaction", "minecraft/stone"))
			.leftInput(waterCatalyst)
			.rightInput(lavaCatalyst, false)
			.output(EmiStack.of(Blocks.STONE))
			.build());
		addRecipeSafe(registry, () -> EmiWorldInteractionRecipe.builder()
			.id(synthetic("world/fluid_interaction", "minecraft/obsidian"))
			.leftInput(lava)
			.rightInput(waterCatalyst, false)
			.output(EmiStack.of(Blocks.OBSIDIAN))
			.build());
	
		/*EmiStack soulSoil = EmiStack.of(Items.SOUL_SOIL);
		soulSoil.setRemainder(soulSoil);
		EmiStack blueIce = EmiStack.of(Blocks.BLUE_ICE);
		blueIce.setRemainder(blueIce);

		addRecipeSafe(registry, () -> EmiWorldInteractionRecipe.builder()
			.id(synthetic("world/fluid_interaction", "minecraft/basalt"))
			.leftInput(lavaCatalyst)
			.rightInput(soulSoil, false, s -> s.appendTooltip(EmiPort
				.translatable("tooltip.emi.fluid_interaction.basalt.soul_soil", Formatting.GREEN)))
			.rightInput(blueIce, false, s -> s.appendTooltip(EmiPort
				.translatable("tooltip.emi.fluid_interaction.basalt.blue_ice", Formatting.GREEN)))
			.output(EmiStack.of(Items.BASALT))
			.build());*/

		EmiPort.getAllFluids().forEach(fluid -> {
			if (fluid.getBlock() != null) {
				Arrays.stream(FluidContainerRegistry.getRegisteredFluidContainerData()).filter(data ->
						data.fluid.getFluid() == fluid).forEach(container -> {
					addRecipeSafe(registry, () -> basicWorld(EmiStack.of(container.emptyContainer), EmiStack.of(container.fluid), EmiStack.of(container.filledContainer),
							synthetic("emi", EmiUtil.subId(container.emptyContainer) + "_filling/" + EmiUtil.subId(fluid)), false));
				});
			}
		});

		//Already added through the container registry
		/*addRecipeSafe(registry, () -> basicWorld(EmiStack.of(Items.GLASS_BOTTLE), water,
			EmiStack.of(new ItemStack(Items.POTION)),
			synthetic("world/unique", "minecraft/water_bottle")));

		/*EmiStack waterBottle = EmiStack.of(EmiPort.setPotion(new ItemStack(Items.POTION), Potions.WATER))
			.setRemainder(EmiStack.of(Items.GLASS_BOTTLE));
		EmiStack mud = EmiStack.of(Items.MUD);
		addRecipeSafe(registry, () -> basicWorld(EmiStack.of(Items.DIRT), waterBottle, mud, synthetic("world/unique", "minecraft/mud"), false));*/
		//todo check if this is actually used by mods
		List<Pair<ItemStack, Integer>> seedDrops = getSeedDrops();
		EmiIngredient sword = damagedTool(EmiStack.of(Items.IRON_SWORD), 1);
		for (Pair<ItemStack, Integer> seed : seedDrops) {
			addRecipeSafe(registry, () -> basicWorld(EmiStack.of(Blocks.TALLGRASS, 1, 1), sword,
					EmiStack.of(seed.first()).setChance(getSeedChance(seedDrops.stream().map(Pair::second).toList(), seed.second())),
					synthetic("emi", "seeds/" + EmiUtil.subId(seed.first())), true));
		}
	}

	private static EmiIngredient damagedTool(EmiIngredient tool, int damage) {
		for (EmiStack stack : tool.getEmiStacks()) {
			ItemStack is = stack.getItemStack().copy();
			is.setDamage(1);
			stack.setRemainder(EmiStack.of(is));
		}
		return tool;
	}

	private static EmiIngredient getPreferredTag(List<String> candidates, EmiIngredient fallback) {
		for (String id : candidates) {
			EmiIngredient potential = EmiIngredient.of(TagKey.of(ItemKey.class, EmiPort.id(id)));
			if (!potential.isEmpty()) {
				return potential;
			}
		}
		return fallback;
	}

	private static void addFuel(EmiRegistry registry, Set<Item> hiddenItems) {
		Map<ItemKey, Integer> fuelMap = EmiAgnos.getFuelMap();
		compressRecipesToTags(fuelMap.keySet().stream().collect(Collectors.toSet()), (a, b) -> {
				return Integer.compare(fuelMap.get(a), fuelMap.get(b));
			}, tag -> {
				EmiIngredient stack = EmiIngredient.of(tag);
				ItemStack item = stack.getEmiStacks().get(0).getItemStack();
				int time = fuelMap.get(ItemKey.of(item));
				registry.addRecipe(new EmiFuelRecipe(stack, time, synthetic("fuel/tag", EmiUtil.subId(tag.id()))));
			}, item -> {
				if (!hiddenItems.contains(item.item())) {
					int time = fuelMap.get(item);
					registry.addRecipe(new EmiFuelRecipe(EmiStack.of(item.toStack()), time, synthetic("fuel/item", EmiUtil.subId(item.toStack()))));
				}
			});
	}

	/*private static void addComposting(EmiRegistry registry, Set<Item> hiddenItems) {
		compressRecipesToTags(ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.keySet().stream()
			.map(ItemConvertible::asItem).collect(Collectors.toSet()), (a, b) -> {
				return Float.compare(ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.getFloat(a), ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.getFloat(b));
			}, tag -> {
				EmiIngredient stack = EmiIngredient.of(tag);
				Item item = stack.getEmiStacks().get(0).getItemStack().getItem();
				float chance = ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.getFloat(item);
				registry.addRecipe(new EmiCompostingRecipe(stack, chance, synthetic("composting/tag", EmiUtil.subId(tag.id()))));
			}, item -> {
				if (!hiddenItems.contains(item)) {
					float chance = ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.getFloat(item);
					registry.addRecipe(new EmiCompostingRecipe(EmiStack.of(item), chance, synthetic("composting/item", EmiUtil.subId(item))));
				}
			});
	}*/

	private static void compressRecipesToTags(Set<ItemKey> stacks, Comparator<ItemKey> comparator, Consumer<TagKey<ItemKey>> tagConsumer, Consumer<ItemKey> itemConsumer) {
		Set<ItemKey> handled = Sets.newHashSet();
		outer:
		for (TagKey<ItemKey> key : (List<TagKey<ItemKey>>) (List<?>) EmiTags.getTags(TagKey.Type.ITEM)) {
			List<ItemKey> items = key.getAll();
			if (items.size() < 2) {
				continue;
			}
			ItemKey base = items.get(0);
			if (!stacks.contains(base)) {
				continue;
			}
			for (int i = 1; i < items.size(); i++) {
				ItemKey item = items.get(i);
				if (!stacks.contains(item) || comparator.compare(base, item) != 0) {
					continue outer;
				}
			}
			if (handled.containsAll(items)) {
				continue;
			}
			handled.addAll(items);
			tagConsumer.accept(key);
		}
		for (ItemKey item : stacks) {
			if (handled.contains(item)) {
				continue;
			}
			itemConsumer.accept(item);
		}
	}

	private static Identifier synthetic(String type, String name) {
		return EmiPort.id("emi", "/" + type + "/" + name);
	}

	private static void safely(String name, Runnable runnable) {
		try {
			runnable.run();
		} catch (Throwable t) {
			EmiReloadLog.warn("Exception thrown when reloading " + name  + " step in vanilla EMI plugin");
			EmiReloadLog.error(t);
		}
	}

	private static void addRecipeSafe(EmiRegistry registry, Supplier<EmiRecipe> supplier) {
		try {
			registry.addRecipe(supplier.get());
		} catch (Throwable e) {
			EmiReloadLog.warn("Exception thrown when parsing EMI recipe (no ID available)");
			EmiReloadLog.error(e);
		}
	}

	private static void addRecipeSafe(EmiRegistry registry, Supplier<EmiRecipe> supplier, Object recipe) {
		try {
			registry.addRecipe(supplier.get());
		} catch (Throwable e) {
			EmiReloadLog.warn("Exception thrown when parsing vanilla recipe " + recipe);
			EmiReloadLog.error(e);
		}
	}

	private static EmiRenderable simplifiedRenderer(int u, int v) {
		return (raw, x, y, delta) -> {
			EmiDrawContext context = EmiDrawContext.wrap(raw);
			context.drawTexture(EmiRenderHelper.WIDGETS, x, y, u, v, 16, 16);
		};
	}

	private static void addConcreteRecipe(EmiRegistry registry, Block powder, EmiStack water, Block result) {
		addRecipeSafe(registry, () -> basicWorld(EmiStack.of(powder), water, EmiStack.of(result),
			synthetic("world/concrete", EmiUtil.subId(result))));
	}

	private static EmiRecipe basicWorld(EmiIngredient left, EmiIngredient right, EmiStack output, Identifier id) {
		return basicWorld(left, right, output, id, true);
	}

	private static EmiRecipe basicWorld(EmiIngredient left, EmiIngredient right, EmiStack output, Identifier id, boolean catalyst) {
		return EmiWorldInteractionRecipe.builder()
			.id(id)
			.leftInput(left)
			.rightInput(right, catalyst)
			.output(output)
			.build();
	}

	private static List<Pair<ItemStack, Integer>> getSeedDrops() {
		try {
			Field seedList = ForgeHooks.class.getDeclaredField("seedList");
			seedList.setAccessible(true);
			List<Object> seedEntries = (List<Object>) seedList.get(null);
			List<Pair<ItemStack, Integer>> seeds = new ArrayList<>();
			Class<?> entryClass = Arrays.stream(ForgeHooks.class.getDeclaredClasses()).filter(clazz -> clazz.getName().contains("SeedEntry")).findFirst().orElseThrow(NoSuchElementException::new);
			Field stackField = entryClass.getDeclaredField("seed");
			stackField.setAccessible(true);

			for (Object entry : seedEntries) {
				int weight = ObfuscationReflectionHelper.getPrivateValue(Weight.class, (Weight) entry, "a");
				ItemStack stack = (ItemStack) stackField.get(entry);
				seeds.add(Pair.of(stack, weight));
			}
			return seeds;
		} catch (NoSuchFieldException | IllegalAccessException | NoSuchElementException e) {
			EmiReloadLog.warn("Error while getting seed drops in vanilla plugin");
			EmiReloadLog.error(e);
			return new ArrayList<>();
		}
    }

	private static float getSeedChance(List<Integer> all, int weight) {
		int totalWeight = 0;
		for (Integer i : all) {
			totalWeight	+= weight;
		}
		float relativeWeight = (float) weight / totalWeight;
		return relativeWeight / 8; //Base chance that anything is dropped is 1/8
	}
}