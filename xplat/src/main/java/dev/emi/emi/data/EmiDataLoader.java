package dev.emi.emi.data;

import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import dev.emi.emi.EmiPort;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.backport.EmiResource;
import dev.emi.emi.backport.EmiResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import dev.emi.emi.backport.EmiJsonHelper;
import net.minecraft.util.profiler.Profiler;

public class EmiDataLoader<T> extends SinglePreparationResourceReloader<T>
		implements EmiResourceReloadListener {
	private static final Gson GSON = new Gson();
	private final Identifier id;
	private final String path;
	private final Supplier<T> baseSupplier;
	private final DataConsumer<T> prepare;
	private final Consumer<T> apply;

	public EmiDataLoader(Identifier id, String path, Supplier<T> baseSupplier,
			DataConsumer<T> prepare, Consumer<T> apply) {
		this.id = id;
		this.path = path;
		this.baseSupplier = baseSupplier;
		this.prepare = prepare;
		this.apply = apply;
	}

	@Override
	public T prepare(EmiResourceManager manager, Profiler profiler) {
		T t = baseSupplier.get();
		for (Identifier id : EmiPort.findResources(manager, path, i -> i.endsWith(".json"))) {
			if (!id.getNamespace().equals("emi")) {
				continue;
			}
			for (EmiResource resource : manager.getAllResources(id)) {
				try {
					InputStreamReader reader = new InputStreamReader(EmiPort.getInputStream(resource));
					JsonObject json = EmiJsonHelper.deserialize(GSON, reader, JsonObject.class);
					prepare.accept(t, json, id);
				} catch (Exception e) {
					EmiLog.error("Error loading data for " + this.id + " in " + id);
					e.printStackTrace();
				}
			}
		}
		return t;
	}

	@Override
	public void apply(T t, EmiResourceManager manager, Profiler profiler) {
		apply.accept(t);
	}

	@Override
	public Identifier getEmiId() {
		return id;
	}

	public static interface DataConsumer<T> {
		void accept(T t, JsonObject json, Identifier id);
	}
}
