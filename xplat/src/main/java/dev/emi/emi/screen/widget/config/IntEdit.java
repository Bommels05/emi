package dev.emi.emi.screen.widget.config;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.regex.Pattern;

import dev.emi.emi.EmiPort;
import dev.emi.emi.backport.ButtonManager;
import dev.emi.emi.input.EmiInput;
import dev.emi.emi.screen.widget.SizedButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;

public class IntEdit {
	private static final Pattern NUMBER = Pattern.compile("^-?[0-9]*$");
	public final ExtendedTextFieldWidget text;
	public final ButtonWidget up, down;
	
	public IntEdit(int width, IntSupplier getter, IntConsumer setter, ButtonManager manager) {
		MinecraftClient client = MinecraftClient.getInstance();
		text = new TextField(client.textRenderer, 0, 0, width - 14, 18, getter, setter);
		text.setText("" + getter.getAsInt());
		text.setEditable(true);

		up = new SizedButtonWidget(150, 0, 12, 10, 232, 48, () -> true, button -> {
			setter.accept(getter.getAsInt() + getInc());
			text.setText("" + getter.getAsInt());
		}, manager);
		down = new SizedButtonWidget(150, 10, 12, 10, 244, 48, () -> true, button -> {
			setter.accept(getter.getAsInt() - getInc());
			text.setText("" + getter.getAsInt());
		}, manager);
	}

	public boolean contains(int x, int y) {
		return x > text.x && x < up.x + up.getWidth() && y > text.y && y < text.y + text.height;
	}

	public int getInc() {
		if (EmiInput.isShiftDown()) {
			return 10;
		} else if (EmiInput.isControlDown()) {
			return 5;
		}
		return 1;
	}

	public void setPosition(int x, int y) {
		text.x = x + 1;
		text.y = y + 1;
		up.x = x + text.width + 2;
		up.y = y;
		down.x = up.x;
		down.y = y + 10;
	}

	public static class TextField extends ExtendedTextFieldWidget {
		private final IntSupplier getter;
		private final IntConsumer setter;
		private String lastValid = "";

		public TextField(TextRenderer textRenderer, int x, int y, int width, int height, IntSupplier getter, IntConsumer setter) {
			super(textRenderer, x, y, width, height);
			this.getter = getter;
			this.setter = setter;
		}

		public void handleText(String text) {
			if (NUMBER.matcher(getText()).matches()) {
				lastValid = getText();
				try {
					if (getText().trim().isEmpty()) {
						setter.accept(0);
					} else {
						setter.accept(Integer.parseInt(getText()));
					}
				} catch (Exception e) {
				}
			} else {
				setText(lastValid);
			}
		}

		@Override
		public void write(String text) {
			super.write(text);
			handleText(text);
		}

		@Override
		public void eraseCharacters(int characterOffset) {
			super.eraseCharacters(characterOffset);
			handleText(getText());
		}
	}
}
