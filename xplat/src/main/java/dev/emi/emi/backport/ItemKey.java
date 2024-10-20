package dev.emi.emi.backport;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public record ItemKey(Item item, int meta) {

    public static ItemKey of(ItemStack stack) {
        return new ItemKey(stack.getItem(), stack.getData());
    }

    public ItemStack toStack() {
        return new ItemStack(item, 1, meta);
    }

}
