package dev.emi.emi.registry;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import dev.emi.emi.backport.TagKey;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiRegistryAdapter;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ListEmiIngredient;
import dev.emi.emi.api.stack.TagEmiIngredient;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.data.TagExclusions;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.runtime.EmiHidden;
import dev.emi.emi.runtime.EmiReloadLog;
import dev.emi.emi.util.InheritanceMap;
import net.minecraft.block.Block;
import dev.emi.emi.backport.EmiResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class EmiTags {
	public static final InheritanceMap<EmiRegistryAdapter<?>> ADAPTERS_BY_CLASS = new InheritanceMap<>(Maps.newHashMap());
	public static final Map<TagKey.Type, EmiRegistryAdapter<?>> ADAPTERS_BY_REGISTRY = Maps.newHashMap();
	public static final Identifier HIDDEN_FROM_RECIPE_VIEWERS = EmiPort.id("c", "hidden_from_recipe_viewers");
	private static final Map<TagKey<?>, Identifier> MODELED_TAGS = Maps.newHashMap();
	private static final Map<Set<?>, List<TagKey<?>>> CACHED_TAGS = Maps.newHashMap();
	private static final Map<TagKey<?>, List<?>> TAG_CONTENTS = Maps.newHashMap();
	private static final Map<TagKey<?>, List<?>> TAG_VALUES = Maps.newHashMap();
	private static final Map<TagKey.Type, List<TagKey<?>>> SORTED_TAGS = Maps.newHashMap();
	public static final List<TagKey<?>> TAGS = Lists.newArrayList();
	public static TagExclusions exclusions = new TagExclusions();

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <T> List<EmiStack> getValues(TagKey<T> key) {
		if (TAG_VALUES.containsKey(key)) {
			EmiRegistryAdapter adapter = ADAPTERS_BY_REGISTRY.get(key.getType());
			if (adapter != null) {
				List<T> values = (List<T>) TAG_VALUES.getOrDefault(key, List.of());
				return values.stream().map(t -> adapter.of(t, EmiPort.emptyExtraData(), 1)).toList();
			}
		}
		return List.of();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <T> List<EmiStack> getRawValues(TagKey<T> key) {
		if (key.getType() == TagKey.Type.BLOCK) {
			return key.getAll().stream().map(e -> EmiStack.of((Block) e)).toList();
		}
		EmiRegistryAdapter adapter = ADAPTERS_BY_REGISTRY.get(key.getType());
		if (adapter != null) {
			List<T> values = key.getAll();
			return values.stream().map(t -> adapter.of(t, EmiPort.emptyExtraData(), 1)).toList();
		}
		return List.of();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <T> EmiIngredient getIngredient(Class<T> clazz, List<EmiStack> stacks, long amount) {
		Map<T, EmiStack> map = Maps.newHashMap();
		for (EmiStack stack : stacks) {
			if (!stack.isEmpty()) {
				EmiStack existing = map.getOrDefault(stack.getKey(), null);
				if (existing != null && !stack.equals(existing)) {
					return new ListEmiIngredient(stacks, amount);
				}
				map.put((T) stack.getKey(), stack);
			}
		}
		if (map.size() == 0) {
			return EmiStack.EMPTY;
		} else if (map.size() == 1) {
			return map.values().stream().toList().get(0).copy().setAmount(amount);
		}
		EmiRegistryAdapter<T> adapter = (EmiRegistryAdapter<T>) ADAPTERS_BY_CLASS.get(clazz);
		if (adapter == null) {
			return new ListEmiIngredient(stacks, amount);
		}
		TagKey.Type registry = adapter.getRegistry();
		List<TagKey<T>> keys = (List<TagKey<T>>) (List) CACHED_TAGS.get(map.keySet());

		if (keys != null) {
			for (TagKey<T> key : keys) {
				List<T> values = (List<T>) TAG_CONTENTS.get(key);
				map.keySet().removeAll(values);
			}
		} else {
			keys = Lists.newArrayList();
			Set<T> original = new HashSet<>(map.keySet());
			for (TagKey<T> key : (List<TagKey<T>>) (List<?>) getTags(registry)) {
				List<T> values = (List<T>) TAG_CONTENTS.get(key);
				if (values.size() < 2) {
					continue;
				}
				if (map.keySet().containsAll(values)) {
					map.keySet().removeAll(values);
					keys.add(key);
				}
				if (map.isEmpty()) {
					break;
				}
			}
			CACHED_TAGS.put((Set) original, (List) keys);
		}

		if (keys == null || keys.isEmpty()) {
			return new ListEmiIngredient(stacks.stream().toList(), amount);
		} else if (map.isEmpty()) {
			if (keys.size() == 1) {
				return tagIngredient(keys.get(0), amount);
			} else {
				return new ListEmiIngredient(keys.stream().map(k -> tagIngredient(k, 1)).toList(), amount);
			}
		} else {
			return new ListEmiIngredient(List.of(map.values().stream().map(i -> i.copy().setAmount(1)).toList(),
					keys.stream().map(k -> tagIngredient(k, 1)).toList())
				.stream().flatMap(a -> a.stream()).toList(), amount);
		}
	}

	private static EmiIngredient tagIngredient(TagKey<?> key, long amount) {
		List<?> list = TAG_VALUES.get(key);
		if (list == null || list.isEmpty()) {
			return EmiStack.EMPTY;
		} else if (list.size() == 1) {
			return new TagEmiIngredient(key, amount).getEmiStacks().get(0).copy().setAmount(amount);
		} else {
			return new TagEmiIngredient(key, amount);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <T> List<TagKey<T>> getTags(TagKey.Type type) {
		return (List<TagKey<T>>) (List) SORTED_TAGS.getOrDefault(type, List.of());
	}

	public static Text getTagName(TagKey<?> key) {
		String s = getTagTranslationKey(key);
		if (s == null) {
			return EmiPort.literal("#" + key.id());
		} else {
			return EmiPort.translatable(s);
		}
	}

	public static boolean hasTranslation(TagKey<?> key) {
		return getTagTranslationKey(key) != null;
	}

	private static @Nullable String getTagTranslationKey(TagKey<?> key) {
		Identifier registry = key.getType().getRegistryName();
		if (registry.getNamespace().equals("minecraft")) {
			String s = translatePrefix("tag." + registry.getPath().replace("/", ".") + ".", key.id());
			if (s != null) {
				return s;
			}
		} else {
			String s = translatePrefix("tag." + registry.getNamespace() + "." + registry.getPath().replace("/", ".") + ".", key.id());
			if (s != null) {
				return s;
			}
		}
		return translatePrefix("tag.", key.id());
	}

	private static @Nullable String translatePrefix(String prefix, Identifier id) {
		String s = EmiUtil.translateId(prefix, id);
		if (EmiUtil.hasTranslation(s)) {
			return s;
		}
		if (id.getNamespace().equals("forge")) {
			String key = findTagTranslation(EmiUtil.translateId(prefix, EmiPort.id("c", id.getPath())));
			if (key == null) {
				return findTagTranslation(EmiUtil.translateId(prefix, EmiPort.id("minecraft", id.getPath())));
			}
			return key;
		}
		return null;
	}

	private static @Nullable String findTagTranslation(String s) {
		if (EmiUtil.hasTranslation(s)) {
			return s;
		} else if (EmiUtil.hasTranslation(s + "s")) {
			//forge:dye -> forge:dyes
			return s + "s";
		} else {
			String replaced = s.replace("pane.glass", "glass_panes").replace("block.glass", "glass");
			if (EmiUtil.hasTranslation(replaced)) {
				//forge:pane/glass/white -> forge:glass_panes/white
				return replaced;
			} else {
				String[] split = s.split("\\.");
				if (split.length >= 2) {
					replaced = s.replace(split[split.length - 2], split[split.length - 2] + "s");
					if (EmiUtil.hasTranslation(replaced)) {
						//forge:ore/iron -> forge:ores/iron
						return replaced;
					}
					//forge:stair/wood -> forge:wooden_stairs
					replaced = findTagSwitchedTranslation(s, split, "wood", "wooden");
					if (replaced != null) {
						return replaced;
					} else if (split[split.length - 2].equals("block")) {
						replaced = s.replace(split[split.length - 2] + "." + split[split.length - 1], split[split.length - 1] + "_blocks");
						if (EmiUtil.hasTranslation(replaced)) {
							//forge:block/gold -> forge:gold_blocks
							return replaced;
						}
					}
				}
			}
		}
		return null;
	}

	private static @Nullable String findTagSwitchedTranslation(String s, String[] split, String key, String replacement) {
		if (s.endsWith(key)) {
			String replaced = s.replace(split[split.length - 2] + "." + key, replacement + "_" + split[split.length - 2]);
			if (EmiUtil.hasTranslation(replaced)) {
				//forge:stair/wood -> forge:wooden_stair
				return replaced;
			} else if (EmiUtil.hasTranslation(replaced + "s")) {
				return replaced + "s";
			}
		}
		return null;
	}

	public static @Nullable Identifier getCustomModel(TagKey<?> key) {
		Identifier rid = key.id();
		if (rid.getNamespace().equals("forge") && !EmiTags.MODELED_TAGS.containsKey(key)) {
			key = TagKey.of(key.getType(), EmiPort.id("c", rid.getPath()));
		}
		return EmiTags.MODELED_TAGS.get(key);
	}

	public static boolean hasCustomModel(TagKey<?> key) {
		return getCustomModel(key) != null;
	}

	public static void registerTagModels(EmiResourceManager manager, Consumer<Identifier> consumer) {
		EmiTags.MODELED_TAGS.clear();
		for (Identifier id : EmiPort.findResources(manager, "models/tag", s -> s.endsWith(".json"))) {
			String path = id.getPath();
			path = path.substring(11, path.length() - 5);
			String[] parts = path.split("/");
			if (parts.length > 1) {
				TagKey<?> key = TagKey.of(TagKey.Type.of(EmiPort.id("minecraft", parts[0])), EmiPort.id(id.getNamespace(), path.substring(1 + parts[0].length())));
				Identifier mid = EmiPort.id(id.getNamespace(), "tag/" + path);
				EmiTags.MODELED_TAGS.put(key, mid);
				consumer.accept(mid);
			}
		}
		for (Identifier id : EmiPort.findResources(manager, "models/item/tags", s -> s.endsWith(".json"))) {
			String path = id.getPath();
			path = path.substring(0, path.length() - 5);
			String[] parts = path.substring(17).split("/");
			if (id.getNamespace().equals("emi") && parts.length > 1) {
				Identifier mid = new Identifier(id.getNamespace(), path.substring(12));
				EmiTags.MODELED_TAGS.put(TagKey.of(TagKey.Type.ITEM, EmiPort.id(parts[0], path.substring(18 + parts[0].length()))), mid);
				consumer.accept(mid);
			}
		}
	}
	
	public static void reload() {
		TAGS.clear();
		SORTED_TAGS.clear();
		TAG_CONTENTS.clear();
		TAG_VALUES.clear();
		CACHED_TAGS.clear();
		for (TagKey.Type type : ADAPTERS_BY_REGISTRY.keySet()) {
			reloadTags(type);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static <T> void reloadTags(TagKey.Type type) {
		Set<T> hidden = Set.copyOf(((TagKey<T>) TagKey.of(type, HIDDEN_FROM_RECIPE_VIEWERS)).getAll());
		Identifier rid = type.getRegistryName();
		List<TagKey<T>> tags = type.getAll().stream().map(key -> (TagKey<T>) key)
			.filter(key -> !exclusions.contains(rid, key.id()) && !hidden.containsAll(key.getAll()))
			.toList();
		logUntranslatedTags(tags);
		tags = consolodateTags(tags);
		for (TagKey<T> key : tags) {
			List<T> contents = key.getAll();
			TAG_CONTENTS.put(key, contents);
			List<T> values = contents.stream().filter(s -> !EmiHidden.isDisabled(stackFromKey(key, s))).toList();
			if (values.isEmpty()) {
				TAG_VALUES.put(key, contents);
			} else {
				TAG_VALUES.put(key, values);
			}
		}
		EmiTags.TAGS.addAll(tags.stream().sorted((a, b) -> a.toString().compareTo(b.toString())).toList());
		tags = tags.stream()
			.sorted((a, b) -> Integer.compare(b.getAll().size(), a.getAll().size()))
			.toList();
		EmiTags.SORTED_TAGS.put(type, (List) tags);
	}

	@SuppressWarnings("unchecked")
	private static <T> EmiStack stackFromKey(TagKey<T> key, T t) {
		EmiRegistryAdapter<T> adapter = (EmiRegistryAdapter<T>) ADAPTERS_BY_REGISTRY.get(key.getType());
		if (adapter != null) {
			return adapter.of(t, EmiPort.emptyExtraData(), 1);
		}
		throw new UnsupportedOperationException("Unsupported tag registry " + key);
	}

	private static <T> void logUntranslatedTags(List<TagKey<T>> tags) {
		if (EmiConfig.logUntranslatedTags) {
			List<String> untranslated = Lists.newArrayList();
			for (TagKey<T> tag : tags) {
				if (!hasTranslation(tag)) {
					untranslated.add(tag.id().toString());
				}
			}
			if (!untranslated.isEmpty()) {
				for (String tag : untranslated.stream().sorted().toList()) {
					EmiReloadLog.warn("Untranslated tag #" + tag);
				}
				EmiReloadLog.info(" Tag warning can be disabled in the config, EMI docs describe how to add a translation or exclude tags.");
			}
		}
	}

	private static <T> List<TagKey<T>> consolodateTags(List<TagKey<T>> tags) {
		Map<Set<T>, TagKey<T>> map = Maps.newHashMap();
		for (int i = 0; i < tags.size(); i++) {
			TagKey<T> key = tags.get(i);
			Set<T> values = Set.copyOf(key.getAll());
			TagKey<T> original = map.get(values);
			if (original != null) {
				map.put(values, betterTag(key, original));
			} else {
				map.put(values, key);
			}
		}
		return map.values().stream().toList();
	}

	private static<T> TagKey<T> betterTag(TagKey<T> a, TagKey<T> b) {
		if (hasTranslation(a) != hasTranslation(b)) {
			return hasTranslation(a) ? a : b;
		}
		if (hasCustomModel(a) != hasCustomModel(b)) {
			return hasCustomModel(a) ? a : b;
		}
		String an = a.id().getNamespace();
		String bn = b.id().getNamespace();
		if (!an.equals(bn)) {
			if (an.equals("minecraft")) {
				return a;
			} else if (bn.equals("minecraft")) {
				return b;
			} else if (an.equals("c")) {
				return a;
			} else if (bn.equals("c")) {
				return b;
			} else if (an.equals("fabric")) {
				return EmiAgnos.isModLoaded("forge") ? b : a;
			} else if (bn.equals("fabric")) {
				return EmiAgnos.isModLoaded("forge") ? a : b;
			} else if (an.equals("forge")) {
				return EmiAgnos.isModLoaded("forge") ? a : b;
			} else if (bn.equals("forge")) {
				return EmiAgnos.isModLoaded("forge") ? b : a;
			}
		}
		return a.id().toString().length() <= b.id().toString().length() ? a : b;
	}
}
