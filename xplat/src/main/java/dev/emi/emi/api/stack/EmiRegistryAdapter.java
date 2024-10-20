package dev.emi.emi.api.stack;

import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import dev.emi.emi.backport.TagKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.registry.Registry;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides EMI context for a {@link Registry} to construct stacks from the objects in the registry.
 * This allows EMI to construct tag ingredients from stacks from the given registry.
 * Limited by {@link dev.emi.emi.backport.TagKey.Type} in 1.7.10 so effectively not extendable
 */
@ApiStatus.Experimental
public interface EmiRegistryAdapter<T> {

	/**
	 * @return The base class for objects in the registry.
	 */
	Class<T> getBaseClass();

	/**
	 * @return
	 */
	TagKey.Type getRegistry();

	/**
	 * Constructs an {@link EmiStack} from a given object from the registry, or {@link EmiStack#EMPTY} if somehow invalid.
	 */
	EmiStack of(T t, NbtCompound nbt, long amount);

	/**
	 * Convenience method for creating an {@link EmiRegistryAdapter}.
	 */
	public static <T> EmiRegistryAdapter<T> simple(Class<T> clazz, TagKey.Type type, StackConstructor<T> constructor) {
		return new EmiRegistryAdapter<T>() {

			@Override
			public Class<T> getBaseClass() {
				return clazz;
			}

			@Override
			public TagKey.Type getRegistry() {
				return type;
			}

			@Override
			public EmiStack of(T t, NbtCompound nbt, long amount) {
				return constructor.of(t, nbt, amount);
			}
		};
	}

	public static interface StackConstructor<T> {
		EmiStack of(T t, NbtCompound nbt, long amount);
	}
}
