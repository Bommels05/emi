package dev.emi.emi.data;

import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import dev.emi.emi.EmiPort;
import dev.emi.emi.bom.BoM;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.backport.EmiResource;
import dev.emi.emi.backport.EmiResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import dev.emi.emi.backport.EmiJsonHelper;
import net.minecraft.util.profiler.Profiler;

public class RecipeDefaultLoader extends SinglePreparationResourceReloader<RecipeDefaults>
		implements EmiResourceReloadListener {
	private static final Gson GSON = new Gson();
	public static final Identifier ID = EmiPort.id("emi:recipe_defaults");

	@Override
	protected RecipeDefaults prepare(EmiResourceManager manager, Profiler profiler) {
		RecipeDefaults defaults = new RecipeDefaults();
		for (Identifier id : EmiPort.findResources(manager, "recipe/defaults", i -> i.endsWith(".json"))) {
			if (!id.getNamespace().equals("emi")) {
				continue;
			}
			try {
				for (EmiResource resource : manager.getAllResources(id)) {
					InputStreamReader reader = new InputStreamReader(EmiPort.getInputStream(resource));
					JsonObject json = EmiJsonHelper.deserialize(GSON, reader, JsonObject.class);
					loadDefaults(defaults, json);
				}
			} catch (Exception e) {
				EmiLog.error("Error loading recipe default file " + id);
				e.printStackTrace();
			}
		}
		return defaults;
	}

	@Override
	protected void apply(RecipeDefaults prepared, EmiResourceManager manager, Profiler profiler) {
		BoM.setDefaults(prepared);
	}
	
	@Override
	public Identifier getEmiId() {
		return ID;
	}

	public static void loadDefaults(RecipeDefaults defaults, JsonObject json) {
		if (EmiJsonHelper.getBoolean(json, "replace", false)) {
			defaults.clear();
		}
		JsonArray disabled = EmiJsonHelper.getArray(json, "disabled", new JsonArray());
		for (JsonElement el : disabled) {
			Identifier id = EmiPort.id(el.getAsString());
			defaults.remove(id);
		}
		JsonArray added = EmiJsonHelper.getArray(json, "added", new JsonArray());
		if (EmiJsonHelper.hasArray(json, "recipes")) {
			added.addAll(EmiJsonHelper.getArray(json, "recipes"));
		}
		for (JsonElement el : added) {
			Identifier id = EmiPort.id(el.getAsString());
			defaults.add(id);
		}
		JsonObject resolutions = EmiJsonHelper.getObject(json, "resolutions", new JsonObject());
		for (String key : resolutions.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toSet())) {
			Identifier id = EmiPort.id(key);
			if (EmiJsonHelper.hasArray(resolutions, key)) {
				defaults.add(id, EmiJsonHelper.getArray(resolutions, key));
			}
		}
		JsonObject addedTags = EmiJsonHelper.getObject(json, "tags", new JsonObject());
		for (String key : addedTags.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toSet())) {
			defaults.addTag(new JsonPrimitive(key), addedTags.get(key));
		}
	}
}
