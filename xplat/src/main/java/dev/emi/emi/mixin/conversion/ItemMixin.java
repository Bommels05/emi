package dev.emi.emi.mixin.conversion;

import org.spongepowered.asm.mixin.Mixin;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackConvertible;
import net.minecraft.item.Item;

@Mixin(Item.class)
public class ItemMixin implements EmiStackConvertible {

	@Override
    public EmiStack emi() {
		return EmiStack.of((Item) (Object) this);
	}

	@Override
	public EmiStack emi(long amount) {
		return EmiStack.of((Item) (Object) this, amount);
	}
}
