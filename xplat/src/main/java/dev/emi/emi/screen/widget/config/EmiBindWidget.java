package dev.emi.emi.screen.widget.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.collect.Lists;

import dev.emi.emi.EmiPort;
import dev.emi.emi.input.EmiBind;
import dev.emi.emi.input.EmiBind.ModifiedKey;
import dev.emi.emi.mixin.accessor.ButtonWidgetAccessor;
import dev.emi.emi.screen.ConfigScreen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class EmiBindWidget extends ConfigEntryWidget {
	private final ConfigScreen screen;
	private final Text bindName;
	private EmiBind bind;

	public EmiBindWidget(ConfigScreen screen, List<TooltipComponent> tooltip, Supplier<String> search, EmiBind bind) {
		super(EmiPort.translatable(bind.translationKey), tooltip, search, 0);
		this.screen = screen;
		this.bindName = EmiPort.translatable(bind.translationKey);
		this.bind = bind;
		updateButtons();
	}

	private void updateButtons() {
		buttons.clear();
		for (int i = 0; i < bind.boundKeys.size(); i++) {
			final int j = i;
			ButtonWidget widget = EmiPort.newButton(0, 0, 200, 20, bind.boundKeys.get(i).getKeyText(Formatting.RESET), button -> {
				screen.setActiveBind(bind, j);
			}, buttonManager);
			buttons.add(widget);
		}
	}

	@Override
	public void update(int y, int x, int width, int height) {
		if (buttons.size() != bind.boundKeys.size()) {
			updateButtons();
		}
		int h = 0;
		for (int i = 0; i < buttons.size(); i++) {
			ButtonWidget button = buttons.get(i);
			button.x = x + width - 224;
			button.y = y + h;
			if (screen.activeBind == bind && screen.activeBindOffset == i) {
				((ButtonWidgetAccessor) button).setWidth(200);
				button.x = x + width - 224;
				if (screen.lastModifier == 0) {
					button.message = EmiPort.literal("...", Formatting.YELLOW).asFormattedString();
				} else {
					button.message = new ModifiedKey(InputUtil.Type.KEYSYM
						.createFromCode(screen.lastModifier), screen.activeModifiers)
						.getKeyText(Formatting.YELLOW).asFormattedString();
				}
			} else if (i < bind.boundKeys.size()) {
				if (bind.boundKeys.get(i).isUnbound() && i > 0) {
					((ButtonWidgetAccessor) button).setWidth(20);
					button.x = x + width - 20;
					button.y = y;
					button.message = EmiPort.literal("+", Formatting.AQUA).asFormattedString();
				} else {
					button.message = bind.boundKeys.get(i).getKeyText(Formatting.RESET).asFormattedString();
				}
			}
			h += 24;
		}
	}

	@Override
	public int getHeight() {
		if (!isVisible() || !isParentVisible()) {
			return 0;
		}
		int size = buttons.size() * 24;
		if (buttons.size() > 1 && buttons.size() <= bind.boundKeys.size() && bind.boundKeys.get(buttons.size() - 1).isUnbound()
				&& (screen.activeBind != bind || screen.activeBindOffset != buttons.size() - 1)) {
			size -= 24;
		}
		return size - 4;
	}
}
