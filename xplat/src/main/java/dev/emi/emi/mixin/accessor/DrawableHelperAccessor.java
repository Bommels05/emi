package dev.emi.emi.mixin.accessor;

import net.minecraft.client.gui.DrawableHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DrawableHelper.class)
public interface DrawableHelperAccessor {
	
	@Invoker("fillGradient")
	void fillGradient(int x1, int y1, int x2, int y2, int color1, int color2);

	@Accessor("zOffset")
	void setZOffset(float zOffset);

	@Accessor("zOffset")
	float getZOffset();

	/*
	@Invoker("method_32635")
	static void emi$addTooltipComponent(List<TooltipComponent> components, TooltipData data) {
		throw new AbstractMethodError();
	}*/
}
