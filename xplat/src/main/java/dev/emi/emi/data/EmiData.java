package dev.emi.emi.data;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.emi.emi.EmiPort;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeSorting;
import dev.emi.emi.api.recipe.EmiWorldInteractionRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import dev.emi.emi.backport.EmiJsonHelper;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class EmiData {
	public static Map<String, EmiRecipeCategoryProperties> categoryPriorities = Map.of();
	public static List<Predicate<EmiRecipe>> recipeFilters = List.of();
	public static List<PrioritySupplier<IndexStackData>> stackData = List.of();
	public static List<Supplier<EmiAlias>> aliases = List.of();
	public static List<Supplier<EmiRecipe>> recipes = List.of();
	
	public static void init(Consumer<EmiResourceReloadListener> register) {
		register.accept(new RecipeDefaultLoader());
		register.accept(new EmiTagExclusionsLoader());
		register.accept(
			new EmiDataLoader<Map<String, EmiRecipeCategoryProperties>>(
				EmiPort.id("emi:category_properties"), "category/properties", Maps::newHashMap,
				(map, json, id) -> {
					for (String k : json.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toSet())) {
						if (EmiJsonHelper.hasJsonObject(json, k)) {
							EmiRecipeCategoryProperties props = map.computeIfAbsent(k, s -> new EmiRecipeCategoryProperties());
							JsonObject val = json.getAsJsonObject(k);
							if (EmiJsonHelper.hasNumber(val, "order")) {
								props.order = val.get("order").getAsInt();
							}
							if (EmiJsonHelper.hasJsonObject(val, "icon")) {
								JsonObject icon = val.getAsJsonObject("icon");
								if (EmiJsonHelper.hasString(icon, "texture")) {
									props.icon = () -> new EmiTexture(EmiPort.id(EmiJsonHelper.getString(icon, "texture")), 0, 0, 16, 16, 16, 16, 16, 16);
								} else if (EmiJsonHelper.hasString(icon, "stack")) {
									props.icon = () -> EmiIngredientSerializer.getDeserialized(icon.get("stack"));
								}
							}
							if (EmiJsonHelper.hasJsonObject(val, "simplified_icon")) {
								JsonObject icon = val.getAsJsonObject("simplified_icon");
								if (EmiJsonHelper.hasString(icon, "texture")) {
									props.simplified = () -> new EmiTexture(EmiPort.id(EmiJsonHelper.getString(icon, "texture")), 0, 0, 16, 16, 16, 16, 16, 16);
								} else if (EmiJsonHelper.hasString(icon, "stack")) {
									props.simplified = () -> EmiIngredientSerializer.getDeserialized(icon.get("stack"));
								}
							}
							if (EmiJsonHelper.hasString(val, "sort")) {
								switch (EmiJsonHelper.getString(val, "sort")) {
									case "none":
										props.sort = EmiRecipeSorting.none();
										break;
									case "input_then_output":
										props.sort = EmiRecipeSorting.compareInputThenOutput();
										break;
									case "output_then_input":
										props.sort = EmiRecipeSorting.compareOutputThenInput();
										break;
									case "identifier":
										props.sort = EmiRecipeSorting.identifier();
										break;
								}
							}
						}
					}
				}, map -> categoryPriorities = map));
		register.accept(
			new EmiDataLoader<List<Predicate<EmiRecipe>>>(
				EmiPort.id("emi:recipe_filters"), "recipe/filters", Lists::newArrayList,
				(list, json, oid) -> {
					JsonArray arr = EmiJsonHelper.getArray(json, "filters", new JsonArray());
					for (JsonElement el : arr) {
						if (el.isJsonObject()) {
							JsonObject obj = el.getAsJsonObject();
							List<Predicate<EmiRecipe>> predicates = Lists.newArrayList();
							if (EmiJsonHelper.hasString(obj, "id")) {
								String id = EmiJsonHelper.getString(obj, "id");
								if (id.startsWith("/") && id.endsWith("/")) {
									Pattern pat = Pattern.compile(id.substring(1, id.length() - 1));
									predicates.add(r -> {
										String rid = r.getId() == null ? "null" : r.getId().toString();
										return pat.matcher(rid).find();
									});
								} else {
									predicates.add(r -> {
										String rid = r.getId() == null ? "null" : r.getId().toString();
										return rid.equals(id);
									});
								}
							}
							if (EmiJsonHelper.hasString(obj, "category")) {
								String id = EmiJsonHelper.getString(obj, "category");
								if (id.startsWith("/") && id.endsWith("/")) {
									Pattern pat = Pattern.compile(id.substring(1, id.length() - 1));
									predicates.add(r -> {
										return pat.matcher(r.getCategory().getId().toString()).find();
									});
								} else {
									predicates.add(r -> {
										return r.getCategory().getId().toString().equals(id);
									});
								}
							}
							if (predicates.size() <= 1) {
								list.addAll(predicates);
							} else {
								list.add(r -> {
									for (Predicate<EmiRecipe> p : predicates) {
										if (!p.test(r)) {
											return false;
										}
									}
									return true;
								});
							}
						}
					}
				}, list -> recipeFilters = list));
		register.accept(
			new EmiDataLoader<List<PrioritySupplier<IndexStackData>>>(
				new Identifier("emi:index_stacks"), "index/stacks", Lists::newArrayList,
				(list, json, oid) -> list.add(new PrioritySupplier<>(EmiJsonHelper.getInt(json, "priority", 0), () -> {
					List<IndexStackData.Added> added = Lists.newArrayList();
					List<EmiIngredient> removed = Lists.newArrayList();
					List<IndexStackData.Filter> filters = Lists.newArrayList();
					if (EmiJsonHelper.hasArray(json, "added")) {
						for (JsonElement el : json.getAsJsonArray("added")) {
							if (el.isJsonObject()) {
								JsonObject obj = el.getAsJsonObject();
								EmiIngredient stack = EmiIngredientSerializer.getDeserialized(obj.get("stack"));
								EmiIngredient after = EmiStack.EMPTY;
								if (obj.has("after")) {
									after = EmiIngredientSerializer.getDeserialized(obj.get("after"));
								}
								added.add(new IndexStackData.Added(stack, after));
							}
						}
					}
					if (EmiJsonHelper.hasArray(json, "removed")) {
						for (JsonElement el : json.getAsJsonArray("removed")) {
							removed.add(EmiIngredientSerializer.getDeserialized(el));
						}
					}
					if (EmiJsonHelper.hasArray(json, "filters")) {
						for (JsonElement el : json.getAsJsonArray("filters")) {
							if (EmiJsonHelper.isString(el)) {
								String id = el.getAsString();
								if (id.startsWith("/") && id.endsWith("/")) {
									Pattern pat = Pattern.compile(id.substring(1, id.length() - 1));
									filters.add(new IndexStackData.Filter(s -> pat.matcher(s).find()));
								} else {
									filters.add(new IndexStackData.Filter(s -> s.equals(id)));
								}
							}
						}
					}
					boolean disable = EmiJsonHelper.getBoolean(json, "disable", false);
					return new IndexStackData(disable, added, removed, filters);
				})),
				list -> {
					list.sort(Comparator.comparingInt(PrioritySupplier::getPriority));
					stackData = list;
				}
				));
		register.accept(
			new EmiDataLoader<List<Supplier<EmiAlias>>>(
				EmiPort.id("emi:aliases"), "aliases", Lists::newArrayList,
				(list, json, id) -> {
					if (EmiJsonHelper.hasArray(json, "aliases")) {
						for (JsonElement el : json.getAsJsonArray("aliases")) {
							if (el.isJsonObject()) {
								JsonObject obj = el.getAsJsonObject();
								list.add(() -> new EmiAlias(
									getArrayOrSingleton(obj, "stacks").map(e -> EmiIngredientSerializer.getDeserialized(e)).toList(),
									getArrayOrSingleton(obj, "text").map(e -> e.getAsString()).toList()));
							}
						}
					}
				}, list -> aliases = list));
		register.accept(
			new EmiDataLoader<List<Supplier<EmiRecipe>>>(
				EmiPort.id("emi:recipe_additions"), "recipe/additions", Lists::newArrayList,
				(list, json, oid) -> {
					String s = EmiJsonHelper.getString(json, "type", "");
					Identifier id = EmiPort.id("emi:/generated/" + oid.getPath());
					if (s.equals("emi:info")) {
						list.add(() -> new EmiInfoRecipe(getArrayOrSingleton(json, "stacks").map(EmiIngredientSerializer::getDeserialized).toList(),
							getArrayOrSingleton(json, "text").map(t -> (Text) EmiPort.translatable(t.getAsString())).toList(),
							id));
					} else if (s.equals("emi:world_interaction")) {
						list.add(() -> {
							EmiWorldInteractionRecipe.Builder builder = EmiWorldInteractionRecipe.builder();
							getArrayOrSingleton(json, "left").map(EmiIngredientSerializer::getDeserialized).forEach(
								i -> builder.leftInput(i)
							);
							getArrayOrSingleton(json, "right").map(EmiIngredientSerializer::getDeserialized).forEach(
								i -> builder.rightInput(i, false)
							);
							getArrayOrSingleton(json, "output").map(EmiIngredientSerializer::getDeserialized).forEach(
								i -> builder.output(i.getEmiStacks().get(0))
							);
							builder.id(id);
							return builder.build();
						});
					}
				}, list -> recipes = list));
	}

	private static Stream<JsonElement> getArrayOrSingleton(JsonObject json, String key) {
		if (EmiJsonHelper.hasArray(json, key)) {
			return StreamSupport.stream(json.getAsJsonArray(key).spliterator(), false);
		}
		return Stream.of(json.get(key));
	}

	public static class PrioritySupplier<T> implements Supplier<T> {
		private final int priority;
		private final Supplier<T> supplier;

		public PrioritySupplier(int priority, Supplier<T> supplier) {
			this.priority = priority;
			this.supplier = supplier;
		}

		/**
		 * Gets a result.
		 * @return a result
		 */
		@Override
		public T get() {
			return this.supplier.get();
		}

		/**
		 * Gets a priority if this Supplier.
		 * @return integer priority of this Supplier.
		 */
		public int getPriority() {
			return this.priority;
		}
	};
}
