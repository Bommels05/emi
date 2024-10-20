package dev.emi.emi.platform.forge;

import java.nio.file.Path;
import java.util.*;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.discovery.ITypeDiscoverer;
import cpw.mods.fml.common.discovery.asm.ASMModParser;
import cpw.mods.fml.common.discovery.asm.ModAnnotation;
import dev.emi.emi.backport.ItemKey;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.entity.effect.StatusEffectStrings;
import net.minecraft.item.Items;
import net.minecraft.item.itemgroup.ItemGroup;
import net.minecraftforge.fluids.Fluid;
import org.objectweb.asm.Type;

import com.google.common.collect.Lists;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.FluidEmiStack;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.recipe.EmiBrewingRecipe;
import dev.emi.emi.registry.EmiPluginContainer;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.text.WordUtils;

public class EmiAgnosForge extends EmiAgnos {
	static {
		EmiAgnos.delegate = new EmiAgnosForge();
	}

	@Override
	protected boolean isForgeAgnos() {
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected String getModNameAgnos(String namespace) {
		if (namespace.equals("c")) {
			return "Common";
		}
		Optional<? extends ModContainer> container = Optional.ofNullable(Loader.instance().getIndexedModList().get(namespace));
		if (container.isPresent()) {
			return container.get().getName();
		}
		container = Optional.ofNullable(Loader.instance().getIndexedModList().get(namespace.replace('_', '-')));
		if (container.isPresent()) {
			return container.get().getName();
		}
		return WordUtils.capitalizeFully(namespace.replace('_', ' '));
	}

	@Override
	protected Path getConfigDirectoryAgnos() {
		return Loader.instance().getConfigDir().toPath();
	}

	@Override
	protected boolean isDevelopmentEnvironmentAgnos() {
		return false;
	}

	@Override
	protected boolean isModLoadedAgnos(String id) {
		return Loader.isModLoaded(id);
	}

	@Override
	protected List<String> getAllModNamesAgnos() {
		return Loader.instance().getModList().stream().map(ModContainer::getName).collect(Collectors.toList());
	}

	@Override
	protected List<String> getAllModIdsAgnos() {
		return Loader.instance().getModList().stream().map(ModContainer::getModId).collect(Collectors.toList());
	}

	@Override
	protected List<EmiPluginContainer> getPluginsAgnos() {
		List<EmiPluginContainer> containers = Lists.newArrayList();
		Type entrypointType = Type.getType(EmiEntrypoint.class);
		for (ModContainer mod : Loader.instance().getActiveModList()) {
			try {
				if (mod instanceof DummyModContainer || (mod instanceof InjectedModContainer container && container.wrappedContainer instanceof DummyModContainer)) {
					continue;
				}
				JarFile jar = new JarFile(mod.getSource());
				for (JarEntry entry : Collections.list(jar.entries())) {
					if (ITypeDiscoverer.classFile.matcher(entry.getName()).matches()) {
						ASMModParser parser = new ASMModParser(jar.getInputStream(entry));
						for (ModAnnotation annot : parser.getAnnotations().stream().filter(annot -> annot.getASMType().equals(entrypointType)).collect(Collectors.toList())) {
							Class<?> clazz = Class.forName(annot.getMember());
							if (EmiPlugin.class.isAssignableFrom(clazz)) {
								Class<? extends EmiPlugin> pluginClass = clazz.asSubclass(EmiPlugin.class);
								EmiPlugin plugin = pluginClass.getConstructor().newInstance();
								String id = mod.getModId();
								containers.add(new EmiPluginContainer(plugin, id));
							} else {
								EmiLog.error("EmiEntrypoint " + annot.getMember() + " does not implement EmiPlugin");
							}
						}
					}
				}
			} catch (Throwable t) {
				EmiLog.error("Exception constructing entrypoint:");
				t.printStackTrace();
			}
		}
		return containers;
	}

	@Override
	protected void addBrewingRecipesAgnos(EmiRegistry registry) {
		List<ItemStack> potions = new ArrayList<>();
		for (Item item : EmiPort.getAllItems()) {
			List<ItemStack> stacks = new ArrayList<>();
			item.appendItemStacks(item, ItemGroup.MISC, stacks);
			stacks.stream().filter(stack -> stack.getItem() instanceof PotionItem).forEach(potions::add);
		}
		//Hopefully this doesn't take too long
		findBrewingRecipes(registry, potions, new ArrayList<>());
	}

	private static void findBrewingRecipes(EmiRegistry registry, List<ItemStack> potions, List<EmiBrewingRecipe> foundRecipes) {
		List<ItemStack> newPotions = new ArrayList<>();
		for (Item item : EmiPort.getAllItems()) {
			List<ItemStack> stacks = new ArrayList<>();
			item.appendItemStacks(item, ItemGroup.MISC, stacks);
			for (ItemStack stack : stacks) {
				if (stack.getItem().hasStatusEffectString(stack)) {
					for (ItemStack potion : potions) {
						int data = StatusEffectStrings.getStatusEffectData(potion.getData(), stack.getItem().getStatusEffectString(stack));

						//BrewingStand canBrew logic
						List<?> potionEffects = Items.POTION.getPotionEffects(potion.getData());
						List<?> resultPotionEffects = Items.POTION.getPotionEffects(data);
						if ((!PotionItem.isThrowable(potion.getData()) && PotionItem.isThrowable(data)) || (
								(potion.getData() <= 0 || potionEffects != resultPotionEffects) &&
								(potionEffects == null || !potionEffects.equals(resultPotionEffects) && resultPotionEffects != null) && potion.getData() != data)) {
							ItemStack result = new ItemStack(potion.getItem(), potion.count, data);
							String pid = EmiUtil.subId(result.getItem());
							try {
								Identifier id = EmiPort.id("emi", "/brewing/" + pid
										+ "/" + EmiUtil.subId(stack)
										+ "/" + EmiUtil.subId(potion)
										+ "/" + EmiUtil.subId(result));
								EmiBrewingRecipe recipe = new EmiBrewingRecipe(
										EmiStack.of(potion), EmiStack.of(stack),
										EmiStack.of(result), id);
								if (!foundRecipes.contains(recipe)) {
									registry.addRecipe(recipe);
									foundRecipes.add(recipe);
									if (!potions.stream().anyMatch(found -> ItemStack.equalsAll(found, result))) {
										newPotions.add(result);
									}
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		if (!newPotions.isEmpty()) {
			potions.addAll(newPotions);
			findBrewingRecipes(registry, potions, foundRecipes);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<String> getAllModAuthorsAgnos() {
		return Loader.instance().getModList().stream().flatMap(m -> {
			List<String> authors = m.getMetadata().authorList;
			if (authors != null && authors.size() > 0 && authors.get(0) instanceof String) {
				return authors.stream();
			}
			return Stream.empty();
		}).distinct().collect(Collectors.toList());
	}

	@Override
	protected List<TooltipComponent> getItemTooltipAgnos(ItemStack stack) {
		MinecraftClient client = MinecraftClient.getInstance();
		List<TooltipComponent> text = ((List<String>) stack.getTooltip(client.field_3805, client.options.advancedItemTooltips)).stream().map(EmiPort::literal).map(TooltipComponent::of).collect(Collectors.toList());
		return text;
	}

	@Override
	protected Text getFluidNameAgnos(Fluid fluid, NbtCompound nbt) {
		return EmiPort.literal(new FluidStack(fluid, 1000, nbt).getLocalizedName());
	}

	@Override
	protected List<Text> getFluidTooltipAgnos(Fluid fluid, NbtCompound nbt) {
		return dev.emi.emi.backport.java.List.of(getFluidName(fluid, nbt));
	}

	@Override
	protected boolean isFloatyFluidAgnos(FluidEmiStack stack) {
		FluidStack fs = new FluidStack(stack.getKeyOfType(Fluid.class), 1000, stack.getNbt());
		return fs.getFluid().getDensity() <= 0;
	}

	@Override
	protected EmiStack createFluidStackAgnos(Object object) {
		if (object instanceof FluidStack f) {
			return EmiStack.of(f.getFluid(), f.tag, f.amount);
		}
		return EmiStack.EMPTY;
	}

	/*@Override
	protected boolean canBatchAgnos(ItemStack stack) {
		MinecraftClient client = MinecraftClient.getInstance();
		ItemRenderer ir = client.getItemRenderer();
		BakedModel model = ir.getModel(stack, client.world, null, 0);
		return model != null && model.getClass() == BasicBakedModel.class;
	}*/

	@Override
	protected Map<ItemKey, Integer> getFuelMapAgnos() {
		Map<ItemKey, Integer> fuelMap = new HashMap<>();
		for (Item item : EmiPort.getAllItems()) {
			List<ItemStack> stacks = new ArrayList<>();
			item.appendItemStacks(item, ItemGroup.MISC, stacks);
			for (ItemStack stack : stacks) {
				int time = FurnaceBlockEntity.getBurnTime(stack);
				if (time > 0) {
					fuelMap.put(ItemKey.of(stack), time);
				}
			}
		}
		return fuelMap;
	}
}
