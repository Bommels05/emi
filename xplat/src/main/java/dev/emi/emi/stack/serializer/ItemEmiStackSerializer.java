package dev.emi.emi.stack.serializer;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import dev.emi.emi.api.stack.serializer.EmiStackSerializer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public class ItemEmiStackSerializer implements EmiStackSerializer<ItemEmiStack> {

	@Override
	public String getType() {
		return "item";
	}

	@Override
	public EmiStack create(Identifier id, NbtCompound nbt, long amount) {
		Item item = EmiPort.getItemRegistry().get(id.toString());
		ItemStack stack;
		if (item == null) {
			item = EmiPort.getItemRegistry().get(id.toString().substring(0, id.toString().lastIndexOf('_')));
			String[] sections = id.toString().split("_");
			stack = new ItemStack(item, 1, Integer.parseInt(sections[sections.length - 1]));
		} else {
			stack = new ItemStack(item);
		}
		stack.setNbt(nbt);
		return EmiStack.of(stack, amount);
	}
}
