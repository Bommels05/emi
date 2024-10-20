package dev.emi.emi.registry;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.backport.ItemKey;
import dev.emi.emi.backport.TagKey;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.config.IndexSource;
import dev.emi.emi.data.EmiData;
import dev.emi.emi.data.IndexStackData;
import dev.emi.emi.mixin.accessor.BlockItemAccessor;
import dev.emi.emi.runtime.EmiHidden;
import dev.emi.emi.runtime.EmiLog;
import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.set.hash.TCustomHashSet;
import gnu.trove.strategy.HashingStrategy;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.itemgroup.ItemGroup;
import net.minecraft.nbt.NbtCompound;
import net.minecraftforge.fluids.Fluid;

public class EmiStackList {
	private static final TagKey<ItemKey> ITEM_HIDDEN = TagKey.of(ItemKey.class, EmiTags.HIDDEN_FROM_RECIPE_VIEWERS);
	private static final TagKey<Block> BLOCK_HIDDEN = TagKey.of(Block.class, EmiTags.HIDDEN_FROM_RECIPE_VIEWERS);
	private static final TagKey<Fluid> FLUID_HIDDEN = TagKey.of(Fluid.class, EmiTags.HIDDEN_FROM_RECIPE_VIEWERS);
	public static List<Predicate<EmiStack>> invalidators = Lists.newArrayList();
	public static List<EmiStack> stacks = List.of();
	public static List<EmiStack> filteredStacks = List.of();
	private static Map<EmiStack, Integer> strictIndices = new TCustomHashMap<>(new StrictHashStrategy());
	private static Map<Object, Integer> keyIndices = new HashMap<>();

	public static void clear() {
		invalidators.clear();
		stacks = List.of();
		strictIndices.clear();
		keyIndices.clear();
	}

	public static void reload() {
		List<IndexGroup> groups = Lists.newArrayList();
		Map<String, IndexGroup> namespaceGroups = new LinkedHashMap<>();
		Map<String, IndexGroup> creativeGroups = new LinkedHashMap<>();
		for (Item item : (Iterable<Item>) EmiPort.getItemRegistry()) {
			String itemName = "null";
			try {
				itemName = item.toString();
				EmiStack stack = EmiStack.of(item);
				namespaceGroups.computeIfAbsent(stack.getId().getNamespace(), (k) -> new IndexGroup()).stacks.add(stack);
			} catch (Exception e) {
				EmiLog.error("Item " + itemName + " threw while EMI was attempting to construct the index, items may be missing.");
				EmiLog.error(e);
			}
		}
		for (Item item : (Iterable<Item>) EmiPort.getItemRegistry()) {
			String itemName = "null";
			try {
				itemName = item.toString();
				List<ItemStack> itemStacks = new ArrayList<>();
				item.appendItemStacks(item, ItemGroup.SEARCH, itemStacks);
				List<EmiStack> stacks = itemStacks.stream().filter(s -> s != null && s.getItem() != null).map(EmiStack::of).toList();
				if (!stacks.isEmpty()) {
					creativeGroups.computeIfAbsent(stacks.get(0).getId().getNamespace(), (k) -> new IndexGroup()).stacks.addAll(stacks);
				}
			} catch (Exception e) {
				EmiLog.error("Item " + itemName + " threw while EMI was attempting to construct the index, items may be missing.");
				EmiLog.error(e);
			}
		}
		if (EmiConfig.indexSource == IndexSource.CREATIVE) {
			for (String namespace : namespaceGroups.keySet()) {
				if (creativeGroups.containsKey(namespace)) {
					IndexGroup ng = namespaceGroups.get(namespace);
					IndexGroup cg = creativeGroups.get(namespace);
					if (cg.stacks.size() * 3 >= ng.stacks.size()) {
						ng.suppressedBy.add(cg);
					}
				}
			}
		}
		if (EmiConfig.indexSource != IndexSource.REGISTERED) {
			groups.addAll(creativeGroups.values());
		}
		groups.addAll(namespaceGroups.values());
		IndexGroup fluidGroup = new IndexGroup();
		for (Fluid fluid : EmiPort.getFluidRegistry().values()) {
			String fluidName = null;
			try {
				fluidName = fluid.toString();
				EmiStack fs = EmiStack.of(fluid);
				fluidGroup.stacks.add(fs);
			} catch (Exception e) {
				EmiLog.error("Fluid  " + fluidName + " threw while EMI was attempting to construct the index, stack may be missing.");
				EmiLog.error(e);
			}
		}
		groups.add(fluidGroup);

		Set<EmiStack> added = new TCustomHashSet<>(new StrictHashStrategy());
		
		stacks = Lists.newLinkedList();
		for (IndexGroup group : groups) {
			if (group.shouldDisplay()) {
				for (EmiStack stack : group.stacks) {
					if (!added.contains(stack)) {
						stacks.add(stack);
						added.add(stack);
					}
				}
			}
		}
	}

	@SuppressWarnings({"deprecation", "unchecked"})
	private static <T> boolean isHiddenFromRecipeViewers(T key) {
		if (key instanceof Item i) {
			if (i instanceof BlockItem bi && BLOCK_HIDDEN.contains(((BlockItemAccessor) bi).getBlock())) {
				return true;
			}
		} else if (key instanceof Fluid f) {
			if (FLUID_HIDDEN.contains(f)) {
				return true;
			}
		} else if (key instanceof ItemStack s) {
			if (ITEM_HIDDEN.contains(ItemKey.of(s))) {
				return true;
			}
			if (s.getItem() instanceof BlockItem bi && BLOCK_HIDDEN.contains(((BlockItemAccessor) bi).getBlock())) {
				return true;
			}
		}
		return false;
	}

	public static void bake() {
		stacks.removeIf(s -> {
			try {
				if (s.isEmpty()) {
					return true;
				}
				for (Predicate<EmiStack> invalidator : invalidators) {
					if (invalidator.test(s)) {
						return true;
					}
				}
				if (isHiddenFromRecipeViewers(s.getKey()) || isHiddenFromRecipeViewers(s.getItemStack())) {
					return true;
				}
				return false;
			} catch (Throwable t) {
				EmiLog.error("Stack threw error while baking");
				t.printStackTrace();
				return true;
			}
		});
		for (Supplier<IndexStackData> supplier : EmiData.stackData) {
			IndexStackData ssd = supplier.get();
			if (!ssd.removed().isEmpty()) {
				Set<EmiStack> removed = Sets.newHashSet();
				for (EmiIngredient invalidator : ssd.removed()) {
					for (EmiStack stack : invalidator.getEmiStacks()) {
						removed.add(stack.copy().comparison(c -> EmiPort.compareStrict()));
					}
				}
				stacks.removeAll(removed);
			}
			if (!ssd.filters().isEmpty()) {
				stacks.removeIf(s -> {
					String id = "" + s.getId();
					for (IndexStackData.Filter filter : ssd.filters()) {
						if (filter.filter().test(id)) {
							return true;
						}
					}
					return false;
				});
			}
			for (IndexStackData.Added added : ssd.added()) {
				if (added.added().isEmpty()) {
					continue;
				}
				if (added.after().isEmpty()) {
					stacks.add(added.added().getEmiStacks().get(0));
				} else {
					int i = stacks.indexOf(added.after());
					if (i == -1) {
						i = stacks.size() - 1;
					}
					stacks.add(i + 1, added.added().getEmiStacks().get(0));
				}
			}
		}
		stacks = stacks.stream().filter(stack -> {
			String name = "Unknown";
			String id = "unknown";
			try {
				if (stack.isEmpty()) {
					return false;
				}
				name = stack.toString();
				id = stack.getId().toString();
				if (name != null && stack.getKey() != null && stack.getName() != null) {
					return true;
				}
				EmiLog.warn("Hiding stack " + name + " with id " + id + " from index due to returning dangerous values");
				return false;
			} catch (Throwable t) {
				EmiLog.warn("Hiding stack " + name + " with id " + id + " from index due to throwing errors");
				t.printStackTrace();
				return false;
			}
		}).toList();
		for (int i = 0; i < stacks.size(); i++) {
			EmiStack stack = stacks.get(i);
			strictIndices.put(stack, i);
			keyIndices.put(stack.getKey(), i);
		}
		bakeFiltered();
	}

	public static void bakeFiltered() {
		filteredStacks = stacks.stream().filter(s -> !EmiHidden.isDisabled(s) && !EmiHidden.isHidden(s)).toList();
	}

	public static int getIndex(EmiIngredient ingredient) {
		EmiStack stack = ingredient.getEmiStacks().get(0);
		int ret = strictIndices.getOrDefault(stack, Integer.MAX_VALUE);
		if (ret == Integer.MAX_VALUE) {
			ret = keyIndices.getOrDefault(stack, ret);
		}
		return ret;
	}

	public static class IndexGroup {
		public List<EmiStack> stacks = Lists.newArrayList();
		public Set<IndexGroup> suppressedBy = Sets.newHashSet();

		public boolean shouldDisplay() {
			for (IndexGroup suppressor : suppressedBy) {
				if (suppressor.shouldDisplay()) {
					return false;
				}
			}
			return true;
		}
	}

	public static class StrictHashStrategy implements HashingStrategy<EmiStack> {

		@Override
		public boolean equals(EmiStack a, EmiStack b) {
			if (a == b) {
				return true;
			} else if (a == null || b == null) {
				return false;
			} else if (a.isEmpty() && b.isEmpty()) {
				return true;
			}
			return a.isEqual(b, EmiPort.compareStrict());
		}

		@Override
		public int computeHashCode(EmiStack stack) {
			if (stack != null) {
				NbtCompound nbtCompound = stack.getNbt();
				int i = 31 + stack.getKey().hashCode();
				return 31 * i + (nbtCompound == null ? 0 : nbtCompound.hashCode());
			}
			return 0;
		}
	}

	public static class ComparisonHashStrategy implements HashingStrategy<EmiStack> {

		@Override
		public boolean equals(EmiStack a, EmiStack b) {
			if (a == b) {
				return true;
			} else if (a == null || b == null) {
				return false;
			} else if (a.isEmpty() && b.isEmpty()) {
				return true;
			}
			return a.isEqual(b, EmiComparisonDefaults.get(a.getKey()));
		}

		@Override
		public int computeHashCode(EmiStack stack) {
			if (stack != null) {
				int i = 31 + stack.getKey().hashCode();
				return 31 * i + EmiComparisonDefaults.get(stack.getKey()).getHash(stack);
			}
			return 0;
		}
	}
}
