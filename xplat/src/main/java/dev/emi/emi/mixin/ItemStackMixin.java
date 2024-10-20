package dev.emi.emi.mixin;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.search.EmiSearch;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;

@Mixin(value = ItemStack.class, priority = 500)
public class ItemStackMixin {
	
	@Inject(at = @At("RETURN"), method = "getTooltip")
	private void getTooltip(PlayerEntity player, boolean advanced, CallbackInfoReturnable<List<String>> info) {
		List<String> text = info.getReturnValue();
		if (EmiConfig.appendItemModId && EmiConfig.appendModId && Thread.currentThread() != EmiSearch.searchThread && text != null && !text.isEmpty()) {
			String namespace = new Identifier(EmiPort.getItemRegistry().getId((Object) ((ItemStack) (Object) this).getItem())).getNamespace();
			String mod = EmiUtil.getModName(namespace);
			text.add(EmiPort.literal(mod, Formatting.BLUE, Formatting.ITALIC).asFormattedString());
		}
	}
}
