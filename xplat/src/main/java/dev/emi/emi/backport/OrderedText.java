package dev.emi.emi.backport;

import net.minecraft.text.Text;

public class OrderedText {
	private final Text text;

	public OrderedText(Text text) {
		this.text = text;
	}

	public static OrderedText of(Text text) {
		return new OrderedText(text);
	}

	public String asString() {
		return text.asFormattedString();
	}

	public Text asText() {
		return text;
	}
}
