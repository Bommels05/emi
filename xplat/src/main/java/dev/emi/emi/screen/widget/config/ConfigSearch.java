package dev.emi.emi.screen.widget.config;

import dev.emi.emi.EmiPort;
import dev.emi.emi.runtime.EmiDrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class ConfigSearch {
	public final ConfigSearchWidgetField field;

	public ConfigSearch(int x, int y, int width, int height) {
		MinecraftClient client = MinecraftClient.getInstance();

		field = new ConfigSearchWidgetField(client.textRenderer, x, y, width, height, EmiPort.literal(""));
	}

	public void setText(String query) {
		field.setText(query);
	}

	public String getSearch() {
		return field.getText();
	}
	
	public class ConfigSearchWidgetField extends ExtendedTextFieldWidget {

		public ConfigSearchWidgetField(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
			super(textRenderer, x, y, width, height);
			this.setText(text.asFormattedString());
		}

		@Override
		public void render() {
			super.render();
			if (this.isVisible() && !this.isFocused() && getText().isEmpty()) {
				EmiDrawContext.wrap(MatrixStack.INSTANCE).drawText(EmiPort.translatable("emi.search_config"), this.x + 4, this.y + (this.height - 8) / 2, -8355712);
			}
		}

		@Override
		public void mouseClicked(int mouseX, int mouseY, int button) {
			if (button == 1 && isMouseOver(mouseX, mouseY)) {
				this.setText("");
				EmiPort.focus(this, true);
				return;
			}
			super.mouseClicked(mouseX, mouseY, button);
		}
	}
}
