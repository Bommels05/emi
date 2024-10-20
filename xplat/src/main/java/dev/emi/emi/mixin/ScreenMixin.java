package dev.emi.emi.mixin;

import dev.emi.emi.EmiUtil;
import dev.emi.emi.mixin.accessor.DrawableHelperAccessor;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.Window;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

@Mixin(Screen.class)
public class ScreenMixin {

	@Shadow
	protected static ItemRenderer itemRenderer;

	@Inject(at = @At("HEAD"), method = "renderTooltip(Lnet/minecraft/item/ItemStack;II)V")
	private void renderTooltip(ItemStack stack, int x, int y, CallbackInfo info) {
		EmiScreenManager.lastStackTooltipRendered = stack;
	}

	@Inject(at = @At("RETURN"), method = "init(Lnet/minecraft/client/MinecraftClient;II)V")
	private void init(MinecraftClient client, int width, int height, CallbackInfo info) {
		if ((Object) this instanceof HandledScreen hs) {
			Keyboard.enableRepeatEvents(true);
			EmiScreenManager.addWidgets(hs);
		}
	}

	@Inject(at = @At("HEAD"),
			method = "handleKeyboard", cancellable = true)
	public void onKey(CallbackInfo info) {
		if ((Object) this instanceof HandledScreen) {
			try {
				if (Keyboard.getEventKeyState()) {
					if (EmiScreenManager.keyPressed(Keyboard.getEventCharacter(), Keyboard.getEventKey())) {
						info.cancel();
					}
				}
			} catch (Exception e) {
				EmiLog.error("Error while handling key press");
				e.printStackTrace();
			}
		}
	}

	@Inject(at = @At("HEAD"),
			method = "handleMouse", cancellable = true)
	public void onMouseDown(CallbackInfo info) {
		if ((Object) this instanceof HandledScreen screen) {
			try {
				MinecraftClient client = MinecraftClient.getInstance();
				double mx = (double) (Mouse.getEventX() * screen.width) / client.width;
				double my = screen.height - (double) (Mouse.getEventY() * screen.height) / client.height - 1;
				int eventButton = Mouse.getEventButton();
				if (Mouse.getEventButtonState()) {
					if (EmiScreenManager.mouseClicked(mx, my, eventButton)) {
						screen.pressedMouseButton = eventButton;
						screen.lastClicked = MinecraftClient.getTime();
						info.cancel();
					}
				} else if (eventButton != -1) {
					if (EmiScreenManager.mouseReleased(mx, my, eventButton)) {
						screen.pressedMouseButton = -1;
						info.cancel();
					}
				} else if (screen.pressedMouseButton != -1 && screen.lastClicked > 0L) {
					EmiScreenManager.mouseDragged(mx, my, screen.pressedMouseButton, MinecraftClient.getTime() - screen.lastClicked);
				}
				if (Mouse.getEventDWheel() != 0) {
					if (EmiScreenManager.mouseScrolled(mx, my, EmiUtil.mapScrollAmount(Mouse.getEventDWheel()))) {
						info.cancel();
					}
				}
			} catch (Exception e) {
				EmiLog.error("Error while handling mouse press");
				e.printStackTrace();
			}
		}
	}

	//This is patched by forge somewhere somehow...
	@Dynamic
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;func_73733_a(IIIIII)V", ordinal = 8, shift = At.Shift.AFTER, remap = false),
			method = "Lnet/minecraft/client/gui/GuiScreen;drawHoveringText(Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;)V", remap = false)
	public void onRenderTooltipStart(CallbackInfo info) {
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glPushMatrix();
		GL11.glTranslatef(0, 0, 300);
	}

	@Dynamic
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;func_73733_a(IIIIII)V", ordinal = 0, remap = false),
			method = "Lnet/minecraft/client/gui/GuiScreen;drawHoveringText(Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;)V", remap = false)
	public void onRenderTooltipMiddle(CallbackInfo info) {
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glTranslatef(0, 0, 350);
	}

	@Dynamic
	@Inject(at = @At(value = "RETURN"),
			method = "Lnet/minecraft/client/gui/GuiScreen;drawHoveringText(Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;)V")
	public void onRenderTooltipEnd(CallbackInfo info) {
		GL11.glPopMatrix();
	}
}
