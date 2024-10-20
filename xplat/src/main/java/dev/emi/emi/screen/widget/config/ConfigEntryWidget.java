package dev.emi.emi.screen.widget.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.collect.Lists;

import dev.emi.emi.backport.ButtonManager;
import dev.emi.emi.config.EmiConfig.ConfigGroup;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.widget.config.ListWidget.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public abstract class ConfigEntryWidget extends Entry {
	private final Text name;
	private final List<TooltipComponent> tooltip;
	protected final Supplier<String> search;
	private final int height;
	public ConfigGroup group;
	public boolean endGroup = false;
	private List<? extends Element> children = List.of();
	protected List<ButtonWidget> buttons = new ArrayList<>();
	protected ButtonManager buttonManager = new ButtonManager();
	protected List<ExtendedTextFieldWidget> textFields = new ArrayList<>();
	protected ExtendedTextFieldWidget lastFocused = null;
	public List<GroupNameWidget> parentGroups = Lists.newArrayList();
	
	public ConfigEntryWidget(Text name, List<TooltipComponent> tooltip, Supplier<String> search, int height) {
		this.name = name;
		this.tooltip = tooltip;
		this.search = search;
		this.height = height;
	}

	public void setChildren(List<? extends Element> children) {
		this.children = children;
	}

	public void update(int y, int x, int width, int height) {
	}

	@Override
	public void render(MatrixStack raw, int index, int y, int x, int width, int height, int mouseX, int mouseY,
			boolean hovered, float delta) {
		EmiDrawContext context = EmiDrawContext.wrap(raw);
		if (group != null) {
			context.fill(x + 4, y + height / 2 - 1, 6, 2, 0xffffffff);
			if (endGroup) {
				context.fill(x + 2, y - 4, 2, height / 2 + 5, 0xffffffff);
			} else {
				context.fill(x + 2, y - 4, 2, height + 4, 0xffffffff);
			}
			x += 10;
			width -= 10;
		}
		update(y, x, width, height);
		context.fill(x, y, width, height, 0x66000000);
		context.drawTextWithShadow(this.name, x + 6, y + 10 - parentList.client.textRenderer.fontHeight / 2, 0xFFFFFF);
		for (Element element : children()) {
			if (element instanceof Drawable drawable) {
				drawable.render(context.raw(), mouseX, mouseY, delta);
			}
		}
		for (ButtonWidget button : buttons) {
			button.render(MinecraftClient.getInstance(), mouseX, mouseY);
		}
		for (ExtendedTextFieldWidget textField : textFields) {
			textField.render();
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if (super.mouseClicked(mouseX, mouseY, mouseButton)) {
			return true;
		} else {
			for (ButtonWidget button : buttons) {
				if (button.isMouseOver(MinecraftClient.getInstance(), (int) mouseX, (int) mouseY)) {
					buttonManager.handleClick(button);
					return true;
				}
			}
			for (ExtendedTextFieldWidget textField : textFields) {
				if (textField.isMouseOver((int) mouseX, (int) mouseY)) {
					textField.mouseClicked((int) mouseX, (int) mouseY, mouseButton);
					for (ExtendedTextFieldWidget field : textFields) {
						if (field != textField) {
							field.setFocused(false);
						}
					}
					return true;
				}
			}
			return false;
        }
    }

	@Override
	public boolean keyPressed(char c, int keyCode) {
		for (ExtendedTextFieldWidget textField : textFields) {
			if (textField.keyPressed(c, keyCode)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<TooltipComponent> getTooltip(int mouseX, int mouseY) {
		return tooltip;
	}

	public String getSearchableText() {
		return name.asUnformattedString();
	}

	public boolean isParentVisible() {
		for (GroupNameWidget g : parentGroups) {
			if (g.collapsed) {
				return false;
			}
		}
		return true;
	}

	public boolean isVisible() {
		String s = search.get().toLowerCase();
		if (getSearchableText().toLowerCase().contains(s)) {
			return true;
		}
		for (GroupNameWidget g : parentGroups) {
			if (g.text.asUnformattedString().toLowerCase().contains(s)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getHeight() {
		if (isParentVisible() && isVisible()) {
			return height;
		}
		return 0;
	}

	@Override
	public List<? extends Element> children() {
		return children;
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!focused) {
			for (ExtendedTextFieldWidget textField : textFields) {
				if (textField.isFocused()) {
					textField.setFocused(false);
					lastFocused = textField;
				}
			}
		} else if (this.parentList.getFocused() == this && lastFocused != null){
			lastFocused.setFocused(true);
		}
	}

	public List<ExtendedTextFieldWidget> getTextFields() {
		return textFields;
	}
}
