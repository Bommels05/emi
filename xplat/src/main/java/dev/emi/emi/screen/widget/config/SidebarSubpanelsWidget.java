package dev.emi.emi.screen.widget.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.collect.Lists;

import dev.emi.emi.EmiPort;
import dev.emi.emi.backport.ButtonManager;
import dev.emi.emi.config.SidebarSubpanels;
import dev.emi.emi.config.SidebarType;
import dev.emi.emi.screen.ConfigScreen.Mutator;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class SidebarSubpanelsWidget extends ConfigEntryWidget {
	private List<IntEdit> rows = Lists.newArrayList();
	private Mutator<SidebarSubpanels> mutator;
	private int buttonCount;

	public SidebarSubpanelsWidget(Text name, List<TooltipComponent> tooltip, Supplier<String> search, Mutator<SidebarSubpanels> mutator) {
		super(name, tooltip, search, 0);
		this.mutator = mutator;
		updateButtons();
	}

	public void updateButtons() {
		buttonManager = new ButtonManager();
		textFields.clear();
		buttons.clear();
		buttonCount = 0;
		SidebarSubpanels pages = mutator.get();
		for (int i = 0; i < pages.subpanels.size(); i++) {
			final int j = i;
			SidebarSubpanels.Subpanel page = pages.subpanels.get(i);
			buttons.add(EmiPort.newButton(0, 0, 150, 20, page.type.getText(), b -> {
				EnumWidget.page(page.type, t -> t != SidebarType.CHESS, t -> {
					pages.subpanels.get(j).type = (SidebarType) t;
					pages.unique();
				});
			}, buttonManager));
			buttonCount++;
			IntEdit edit = new IntEdit(40, () -> page.rows(), (ni) -> page.rows = Math.max(1, ni), buttonManager);
			rows.add(edit);
			buttons.add(edit.up);
			buttons.add(edit.down);
			textFields.add(edit.text);
		}
		buttons.add(EmiPort.newButton(0, 0, 20, 20, EmiPort.literal("+"), b -> {
			EnumWidget.page(SidebarType.INDEX, t -> t != SidebarType.CHESS, t -> {
				pages.subpanels.add(new SidebarSubpanels.Subpanel((SidebarType) t, 1));
				pages.unique();
			});
		}, buttonManager));
		buttonCount++;
	}

	@Override
	public List<TooltipComponent> getTooltip(int mouseX, int mouseY) {
		for (IntEdit edit : rows) {
			if (edit.contains(mouseX, mouseY)) {
				return List.of(TooltipComponent.of(EmiPort.ordered(
					EmiPort.translatable("emi.sidebar.size.rows"))));
			}
		}
		return super.getTooltip(mouseX, mouseY);
	}

	@Override
	public void update(int y, int x, int width, int height) {
		int h = 0;
		for (int i = 0; i < buttons.size() - 1; i++) {
			ButtonWidget button = buttons.get(i);
			button.x = x + width - 174;
			button.y = y + h;
			h += 24;
		}
		h = 0;
		for (int i = 0; i < rows.size(); i++) {
			IntEdit edit = rows.get(i);
			edit.setPosition(x + width - 218, y + h);
			h += 24;
		}
		ButtonWidget button = buttons.get(buttons.size() - 1);
		button.x = x + width - 20;
		button.y = y;
	}

	@Override
	public int getHeight() {
		if (!isVisible() || !isParentVisible()) {
			return 0;
		}
		if (buttonCount == 1) {
			return 20;
		}
		return buttonCount * 24 - 28;
	}
}
