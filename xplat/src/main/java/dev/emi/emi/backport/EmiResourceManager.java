package dev.emi.emi.backport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.LanguageRegistry;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.util.Identifier;

public class EmiResourceManager {
	private static final Map<Identifier, URL> files = new HashMap<>();
	public static final EmiResourceManager INSTANCE = new EmiResourceManager();
	
	static {
		File src = Loader.instance().activeModContainer().getSource();
		if (src.isFile()) {
			try (ZipFile zf = new ZipFile(src)) {
				for (ZipEntry en : Collections.list(zf.entries())) {
					if (en.getName().startsWith("assets/emi/") && !en.getName().endsWith("/")) {
						files.put(new Identifier("emi", en.getName().substring(11)), EmiResourceManager.class.getResource("/"+en.getName()));
					}
				}
			} catch (Throwable e) {
				EmiLog.error("Error scanning resources");
				EmiLog.error(e);
			}
		} else {
			throw new IllegalStateException("Mod file is a directory?");
		}
		
		var gson = new Gson();
		for (var fileEn : INSTANCE.findResources("lang", id -> id.getPath().endsWith(".json")).entrySet()) {
			try (var r = new InputStreamReader(fileEn.getValue().getInputStream(), StandardCharsets.UTF_8)) {
				var lang = fileEn.getKey().getPath();
				lang = lang.substring(lang.lastIndexOf('/')+1, lang.length()-5);
				var split = lang.split("_", 2);
				lang = split[0]+"_"+split[1].toUpperCase(Locale.ROOT);
				var obj = gson.fromJson(r, JsonObject.class);
				for (var en : obj.entrySet()) {
					LanguageRegistry.instance().addStringLocalization(en.getKey(), lang, en.getValue().getAsString());
				}
			} catch (IOException e) {
				EmiLog.error("Error loading langage file: " + fileEn.getKey());
				EmiLog.error(e);
			}
		}
	}

	private EmiResourceManager() {}

	public Map<Identifier, EmiResource> findResources(String startingPath, Predicate<Identifier> pathFilter) {
		var map = new HashMap<Identifier, EmiResource>();
		
		files.entrySet().stream()
			.filter(en -> en.getKey().getPath().startsWith(startingPath+"/"))
			.filter(en -> pathFilter.test(en.getKey()))
			.forEach(en -> map.put(en.getKey(), new EmiResource(en.getValue()::openStream)));
		
		var root = new File("config/emi").toPath();
		try (var s = Files.walk(root.resolve(startingPath))) {
			for (var p : (Iterable<Path>)s::iterator) {
				var id = new Identifier("config", p.relativize(root).toString());
				if (pathFilter.test(id))
					map.put(id, new EmiResource(() -> Files.newInputStream(p)));
			}
		} catch (IOException e) {}
		
		return map;
	}

	public List<EmiResource> getAllResources(Identifier id) {
		if (id.getNamespace().equals("config")) {
			var f = new File("config/emi", id.getPath());
			if (f.exists()) {
				return Collections.singletonList(new EmiResource(() -> new FileInputStream(f)));
			}
		} else if (id.getNamespace().equals("emi")) {
			var u = EmiResourceManager.class.getResource("/assets/emi/"+id.getPath());
			if (u != null) {
				return Collections.singletonList(new EmiResource(u::openStream));
			}
		}
		return Collections.emptyList();
	}
	
}
