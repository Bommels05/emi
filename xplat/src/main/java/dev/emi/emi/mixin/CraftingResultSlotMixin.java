package dev.emi.emi.mixin;

import java.util.List;
import java.util.Optional;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.slot.CraftingResultSlot;
import net.minecraft.recipe.RecipeDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.runtime.EmiSidebars;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeType;

@Mixin(CraftingResultSlot.class)
public class CraftingResultSlotMixin {
	@Shadow @Final
	private Inventory field_4147;
	@Shadow @Final
	private PlayerEntity player;
	
	@Inject(at = @At("HEAD"), method = "onCrafted(Lnet/minecraft/item/ItemStack;)V")
	private void onCrafted(ItemStack stack, CallbackInfo info) {
		if (player.world.isClient && field_4147 instanceof CraftingInventory) {
			Optional<RecipeType> opt = ((List<RecipeType>) RecipeDispatcher.getInstance().getAllRecipes()).stream().filter(recipe -> recipe.matches((CraftingInventory) field_4147, player.world)).findFirst();
			if (opt.isPresent()) {
				EmiRecipe recipe = EmiApi.getRecipeManager().getRecipe(EmiPort.getId(opt.get()));
				if (recipe != null) {
					EmiSidebars.craft(recipe);
				}
			}
		}
	}
}
