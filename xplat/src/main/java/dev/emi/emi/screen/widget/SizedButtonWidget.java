package dev.emi.emi.screen.widget;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.backport.ButtonManager;
import dev.emi.emi.runtime.EmiDrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

public class SizedButtonWidget extends ButtonWidget {
	private final BooleanSupplier isActive;
	private final IntSupplier vOffset;
	protected Identifier texture = EmiRenderHelper.BUTTONS;
	protected Supplier<List<Text>> text;
	protected int u, v;

	public SizedButtonWidget(int x, int y, int width, int height, int u, int v, BooleanSupplier isActive, Consumer<ButtonWidget> action, ButtonManager manager) {
		this(x, y, width, height, u, v, isActive, action, manager, () -> 0);
	}

	public SizedButtonWidget(int x, int y, int width, int height, int u, int v, BooleanSupplier isActive, Consumer<ButtonWidget> action, ButtonManager manager,
			List<Text> text) {
		this(x, y, width, height, u, v, isActive, action, manager, () -> 0, () -> text);
	}

	public SizedButtonWidget(int x, int y, int width, int height, int u, int v, BooleanSupplier isActive, Consumer<ButtonWidget> action, ButtonManager manager,
			IntSupplier vOffset) {
		this(x, y, width, height, u, v, isActive, action, manager, vOffset, null);
	}

	public SizedButtonWidget(int x, int y, int width, int height, int u, int v, BooleanSupplier isActive, Consumer<ButtonWidget> action, ButtonManager manager,
			IntSupplier vOffset, Supplier<List<Text>> text) {
		super(manager.register(action), x, y, width, height, "");
		this.u = u;
		this.v = v;
		this.isActive = isActive;
		this.vOffset = vOffset;
		this.text = text;
	}

	protected int getU(int mouseX, int mouseY) {
		return this.u;
	}

	protected int getV(int mouseX, int mouseY) {
		int v = this.v + vOffset.getAsInt();
		this.active = this.isActive.getAsBoolean();
		if (!this.active) {
			v += this.height * 2;
		} else if (this.isMouseOver(MinecraftClient.getInstance(), mouseX, mouseY)) {
			v += this.height;
		}
		return v;
	}
	
	@Override
	public void render(MinecraftClient client, int mouseX, int mouseY) {
		EmiDrawContext context = EmiDrawContext.wrap(MatrixStack.INSTANCE);
		boolean mouseOver = this.isMouseOver(client, mouseX, mouseY);
		if (!mouseOver) {
			context.resetColor();
		}
		RenderSystem.enableDepthTest();
		context.drawTexture(texture, this.x, this.y, getU(mouseX, mouseY), getV(mouseX, mouseY), this.width, this.height);
		if (mouseOver && text != null && this.active) {
			context.push();
			RenderSystem.disableDepthTest();
			EmiRenderHelper.drawTooltip(client.currentScreen, context, text.get().stream().map(EmiPort::ordered).map(TooltipComponent::of).toList(), mouseX, mouseY);
			context.pop();
		}
	}
}
