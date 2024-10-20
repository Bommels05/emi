package dev.emi.emi.mixin.accessor;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.slot.CraftingResultSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.inventory.CraftingInventory;

@Mixin(CraftingResultSlot.class)
public interface CraftingResultSlotAccessor {
	
	@Accessor("field_4147")
    Inventory getInput();
}
