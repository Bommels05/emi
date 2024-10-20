package dev.emi.emi.screen;

import java.lang.reflect.Field;
import java.util.List;

import dev.emi.emi.backport.ButtonManager;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.com.unascribed.qdcss.QDCSS;
import dev.emi.emi.config.ConfigPresets;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.config.EmiConfig.ConfigGroup;
import dev.emi.emi.config.EmiConfig.ConfigValue;
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

public class ConfigPresetScreen extends Screen {
	private final ConfigScreen last;
	private ListWidget list;
	private ButtonManager buttonManager;
	private EmiNameWidget name;
	public ButtonWidget resetButton;

	public ConfigPresetScreen(ConfigScreen last) {
		super();
		this.last = last;
	}

	@Override
	public void init() {
		super.init();
		buttonManager = new ButtonManager();

		this.name = new EmiNameWidget(width / 2, 16);
		int w = Math.min(400, width - 40);
		int x = (width - w) / 2;
		this.resetButton = EmiPort.newButton(x + 2, height - 30, w / 2 - 2, 20, EmiPort.translatable("gui.done"), button -> {
			EmiConfig.loadConfig(QDCSS.load("revert", last.originalConfig));
			MinecraftClient client = MinecraftClient.getInstance();
			this.init(client, width, height);
		}, buttonManager);
		this.buttons.add(resetButton);
		this.buttons.add(EmiPort.newButton(x + w / 2 + 2, height - 30, w / 2 - 2, 20, EmiPort.translatable("gui.done"), button -> {
			this.close();
		}, buttonManager));
		list = new ListWidget(client, width, height, 40, height - 40);
		try {
			for (Field field : ConfigPresets.class.getFields()) {
				ConfigValue config = field.getDeclaredAnnotation(ConfigValue.class);
				if (config != null) {
					if (field.get(null) instanceof Runnable runnable) {
						ConfigGroup group = field.getDeclaredAnnotation(ConfigGroup.class);
						if (group != null) {
							Text translation = EmiPort.translatable("config.emi." + group.value().replace('-', '_'));
							list.addEntry(new PresetGroupWidget(translation));
						}
						Text translation = EmiPort.translatable("config.emi." + config.value().replace('-', '_'));
						list.addEntry(new PresetWidget(runnable, translation, ConfigScreen.getFieldTooltip(field)));
					}
				}
			}
		} catch (Exception e) {
		}
		updateChanges();
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
		if (list.getHoveredEntry() instanceof PresetWidget widget) {
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

	public void updateChanges() {
		// Split on the blank lines between config options
		String[] oLines = last.originalConfig.split("\n\n");
		String[] cLines = EmiConfig.getSavedConfig().split("\n\n");
		int different = 0;
		for (int i = 0; i < oLines.length; i++) {
			if (i >= cLines.length) {
				break;
			}
			if (!oLines[i].equals(cLines[i])) {
				different++;
			}
		}
		this.resetButton.active = different > 0;
		this.resetButton.message = EmiPort.translatable("screen.emi.config.reset", different).asFormattedString();
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

	public class PresetWidget extends ListWidget.Entry {
		private final ButtonWidget button;
		private final List<TooltipComponent> tooltip;
		private final ButtonManager buttonManager = new ButtonManager();

		public PresetWidget(Runnable runnable, Text name, List<TooltipComponent> tooltip) {
			button = EmiPort.newButton(0, 0, 200, 20, name, t -> {
				runnable.run();
				updateChanges();
			}, buttonManager);
			this.tooltip = tooltip;
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

	public class PresetGroupWidget extends ListWidget.Entry {
		private final Text text;

		public PresetGroupWidget(Text text) {
			this.text = text;
		}

		@Override
		public List<? extends Element> children() {
			return List.of();
		}

		@Override
		public void render(MatrixStack raw, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float delta) {
			EmiDrawContext context = EmiDrawContext.wrap(raw);
			context.drawCenteredTextWithShadow(text, x + width / 2, y + 3, -1);
		}

		@Override
		public int getHeight() {
			return 20;
		}
	}
}
