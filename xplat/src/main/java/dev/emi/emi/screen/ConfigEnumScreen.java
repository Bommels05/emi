package dev.emi.emi.screen;

import java.util.List;
import java.util.function.Consumer;

import dev.emi.emi.backport.ButtonManager;
import net.minecraft.client.resource.language.I18n;
import org.lwjgl.glfw.GLFW;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.widget.config.EmiNameWidget;
import dev.emi.emi.screen.widget.config.ListWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class ConfigEnumScreen<T> extends Screen {
	private final ConfigScreen last;
	private final List<Entry<T>> entries;
	private final Consumer<T> selection;
	private EmiNameWidget name;
	private ButtonManager buttonManager;
	private ListWidget list;

	public ConfigEnumScreen(ConfigScreen last, List<Entry<T>> entries, Consumer<T> selection) {
		super();
		this.last = last;
		this.entries = entries;
		this.selection = selection;
	}

	@Override
	public void init() {
		super.init();
		this.buttonManager = new ButtonManager();

		this.name = new EmiNameWidget(width / 2, 16);
		int w = 200;
		int x = (width - w) / 2;
		this.buttons.add(EmiPort.newButton(x, height - 30, w, 20, EmiPort.translatable("gui.done"), button -> {
			close();
		}, buttonManager));
		list = new ListWidget(client, width, height, 40, height - 40);
		for (Entry<T> e : entries) {
			list.addEntry(new SelectionWidget<T>(this, e));
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		EmiDrawContext context = EmiDrawContext.wrap(MatrixStack.INSTANCE);
		list.setScrollAmount(list.getScrollAmount());
		if (list.isMouseOver(mouseX, mouseY)) {
			list.render(context.raw(), mouseX, mouseY, delta);
		} else {
			list.render(context.raw(), 0, 0, delta);
		}
		EmiRenderHelper.renderSplitBackground(this, list);
		name.render(context.raw(), mouseX, mouseY, delta);
		super.render(mouseX, mouseY, delta);
		ListWidget.Entry entry = list.getHoveredEntry();
		if (entry instanceof SelectionWidget<?> widget) {
			if (widget.button.isHovered()) {
				EmiRenderHelper.drawTooltip(this, context, widget.tooltip, mouseX, mouseY);
			}
		}
	}

	public void close() {
		MinecraftClient.getInstance().setScreen(last);
	}
	
	@Override
	public void keyPressed(char c, int keyCode) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			this.close();
			return;
		} else if (keyCode == this.client.options.inventoryKey.getCode()) {
			this.close();
			return;
		} else if (keyCode == GLFW.GLFW_KEY_TAB) {
			return;
		}
		super.keyPressed(c, keyCode);
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		buttonManager.handleClick(button);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		super.mouseClicked(mouseX, mouseY, button);
		this.list.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int button) {
		super.mouseReleased(mouseX, mouseY, button);
		this.list.mouseReleased(mouseX, mouseY, button);
	}

	public static record Entry<T>(T value, Text name, List<TooltipComponent> tooltip) {
	}

	public static class SelectionWidget<T> extends ListWidget.Entry {
		private final ButtonWidget button;
		private final List<TooltipComponent> tooltip;
		private final ButtonManager buttonManager = new ButtonManager();

		public SelectionWidget(ConfigEnumScreen<T> screen, Entry<T> e) {
			button = EmiPort.newButton(0, 0, 200, 20, e.name(), t -> {
				screen.selection.accept(e.value());
				screen.close();
			}, buttonManager);
			tooltip = e.tooltip();
		}

		@Override
		public List<? extends Element> children() {
			return List.of();
		}

		@Override
		public void render(MatrixStack raw, int index, int y, int x, int width, int height, int mouseX, int mouseY,
				boolean hovered, float delta) {
			button.y = y;
			button.x = x + width / 2 - button.getWidth() / 2;
			button.render(MinecraftClient.getInstance(), mouseX, mouseY);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
			if (button.isMouseOver(MinecraftClient.getInstance(), (int) mouseX, (int) mouseY)) {
				buttonManager.handleClick(button);
				return true;
			}
			return false;
		}

		@Override
		public int getHeight() {
			return 20;
		}
	}
}
