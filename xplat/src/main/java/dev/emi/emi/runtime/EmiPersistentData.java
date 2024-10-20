package dev.emi.emi.runtime;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import dev.emi.emi.bom.BoM;
import dev.emi.emi.backport.EmiJsonHelper;

public class EmiPersistentData {
	public static final File FILE = new File("emi.json");
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	
	public static void save() {
		try {
			JsonObject json = new JsonObject();
			json.add("favorites", EmiFavorites.save());
			EmiSidebars.save(json);
			json.add("recipe_defaults", BoM.saveAdded());
			json.add("hidden_stacks", EmiHidden.save());
			FileWriter writer = new FileWriter(FILE);
			GSON.toJson(json, writer);
			writer.close();
		} catch (Exception e) {
			EmiLog.error("Failed to write persistent data");
			e.printStackTrace();
		}
	}

	public static void load() {
		if (!FILE.exists()) {
			return;
		}
		try {
			JsonObject json = GSON.fromJson(new FileReader(FILE), JsonObject.class);
			if (EmiJsonHelper.hasArray(json, "favorites")) {
				EmiFavorites.load(EmiJsonHelper.getArray(json, "favorites"));
			}
			EmiSidebars.load(json);
			if (EmiJsonHelper.hasJsonObject(json, "recipe_defaults")) {
				BoM.loadAdded(EmiJsonHelper.getObject(json, "recipe_defaults"));
			}
			if (EmiJsonHelper.hasArray(json, "hidden_stacks")) {
				EmiHidden.load(EmiJsonHelper.getArray(json, "hidden_stacks"));
			}
		} catch (Exception e) {
			EmiLog.error("Failed to parse persistent data");
			e.printStackTrace();
		}
	}
}
