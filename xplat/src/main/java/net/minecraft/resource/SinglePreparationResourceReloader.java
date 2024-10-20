package net.minecraft.resource;

import dev.emi.emi.backport.EmiResourceManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.profiler.Profiler;

public abstract class SinglePreparationResourceReloader<T> implements ResourceReloader {

	@Override
	public void reload() {
		apply(prepare(EmiResourceManager.INSTANCE, MinecraftClient.getInstance().profiler), EmiResourceManager.INSTANCE, MinecraftClient.getInstance().profiler);
	}
	
	protected abstract T prepare(EmiResourceManager manager, Profiler profiler);
	protected abstract void apply(T t, EmiResourceManager manager, Profiler profiler);

}
