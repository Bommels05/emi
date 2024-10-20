package dev.emi.emi.api.stack;

import java.util.List;
import java.util.stream.Collectors;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import dev.emi.emi.api.render.EmiRender;
import dev.emi.emi.api.render.EmiTooltipComponents;
import dev.emi.emi.platform.EmiAgnos;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@ApiStatus.Internal
public class FluidEmiStack extends EmiStack {
	private final Fluid fluid;
	private final NbtCompound nbt;

	public FluidEmiStack(Fluid fluid) {
		this(fluid, null);
	}

	public FluidEmiStack(Fluid fluid, @Nullable NbtCompound nbt) {
		this(fluid, nbt, 0);
	}

	public FluidEmiStack(Fluid fluid, @Nullable NbtCompound nbt, long amount) {
		this.fluid = fluid;
		this.nbt = nbt;
		this.amount = amount;
	}

	@Override
	public EmiStack copy() {
		EmiStack e = new FluidEmiStack(fluid, nbt, amount);
		e.setChance(chance);
		e.setRemainder(getRemainder().copy());
		e.comparison = comparison;
		return e;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public NbtCompound getNbt() {
		return nbt;
	}

	@Override
	public Object getKey() {
		return fluid;
	}

	@Override
	public Identifier getId() {
		return new Identifier(FluidRegistry.getFluidName(fluid));
	}

	@Override
	public void render(MatrixStack matrices, int x, int y, float delta, int flags) {
		if ((flags & RENDER_ICON) != 0) {
			RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEX);
			MinecraftClient.getInstance().currentScreen.method_4944(x, y, fluid.getIcon(), 16, 16);
		}
		if ((flags & RENDER_REMAINDER) != 0) {
			EmiRender.renderRemainderIcon(this, matrices, x, y);
		}
	}

	@Override
	public List<Text> getTooltipText() {
		return EmiAgnos.getFluidTooltip(fluid, nbt);
	}

	@Override
	public List<TooltipComponent> getTooltip() {
		List<TooltipComponent> list = getTooltipText().stream().map(EmiTooltipComponents::of).collect(Collectors.toList());
		if (amount > 1) {
			list.add(EmiTooltipComponents.getAmount(this));
		}
		String namespace = getId().getNamespace();
		EmiTooltipComponents.appendModName(list, namespace);
		list.addAll(super.getTooltip());
		return list;
	}

	@Override
	public Text getName() {
		return EmiAgnos.getFluidName(fluid, nbt);
	}

	static class FluidEntry {
	}
}
