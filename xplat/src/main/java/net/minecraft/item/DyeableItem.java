package net.minecraft.item;

import java.util.List;

import net.minecraft.block.WoolBlock;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.nbt.NbtCompound;

public class DyeableItem {

	public static ItemStack blendAndSetColor(ItemStack stack, List<ItemStack> colors) {
		ItemStack itemStack = null;
		int[] is = new int[3];
		int i = 0;
		int j = 0;
		Item item = stack.getItem();
		itemStack = stack.copy();
		itemStack.count = 1;
		if (itemStack.nbt != null && itemStack.nbt.contains("display") && itemStack.nbt.getCompound("display").contains("color")) {
			int k = itemStack.nbt.getCompound("display").getInt("color");
			float f = (float)(k >> 16 & 0xFF) / 255.0F;
			float g = (float)(k >> 8 & 0xFF) / 255.0F;
			float h = (float)(k & 0xFF) / 255.0F;
			i += (int)(Math.max(f, Math.max(g, h)) * 255.0F);
			is[0] += (int)(f * 255.0F);
			is[1] += (int)(g * 255.0F);
			is[2] += (int)(h * 255.0F);
			++j;
		}

		for(ItemStack dyeItem : colors) {
			float[] fs = SheepEntity.field_3706[WoolBlock.method_6467(dyeItem.getData())];
			int l = (int)(fs[0] * 255.0F);
			int m = (int)(fs[1] * 255.0F);
			int n = (int)(fs[2] * 255.0F);
			i += Math.max(l, Math.max(m, n));
			is[0] += l;
			is[1] += m;
			is[2] += n;
			++j;
		}

		if (item == null) {
			return null;
		} else {
			int k = is[0] / j;
			int o = is[1] / j;
			int p = is[2] / j;
			float h = (float)i / (float)j;
			float q = (float)Math.max(k, Math.max(o, p));
			k = (int)((float)k * h / q);
			o = (int)((float)o * h / q);
			p = (int)((float)p * h / q);
			int var26 = (k << 8) + o;
			var26 = (var26 << 8) + p;
			if (itemStack.nbt == null) itemStack.setNbt(new NbtCompound());
			if (!itemStack.nbt.contains("display")) itemStack.nbt.put("display", new NbtCompound());
			itemStack.nbt.getCompound("display").putInt("color", var26);
			return itemStack;
		}
	}

}
