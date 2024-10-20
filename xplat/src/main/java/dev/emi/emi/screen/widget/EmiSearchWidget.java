package dev.emi.emi.screen.widget;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.realmsclient.util.Pair;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.backport.OrderedText;
import dev.emi.emi.screen.widget.config.ExtendedTextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.emi.emi.EmiPort;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.search.EmiSearch;
import dev.emi.emi.search.QueryType;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

public class EmiSearchWidget extends ExtendedTextFieldWidget {
	private static final Pattern ESCAPE = Pattern.compile("\\\\.");
	private List<String> searchHistory = Lists.newArrayList();
	private int searchHistoryIndex = 0;
	private List<Pair<Integer, Style>> styles = new ArrayList<>();
	private long lastClick = 0;
	private String last = "";
	private long lastRender = System.currentTimeMillis();
	private long accumulatedSpin = 0;
	public boolean highlight = false;
	// Reimplement focus because other mods keep breaking it
	public boolean isFocused;

	public EmiSearchWidget(TextRenderer textRenderer, int x, int y, int width, int height) {
		super(textRenderer, x, y, width, height);
		this.setFocusUnlocked(true);
		this.setEditableColor(-1);
		this.setUneditableColor(-1);
		this.setMaxLength(256);
	}

	public void update() {
		setText(getText());
	}

	public void swap() {
		String last = this.getText();
		this.setText(this.last);
		this.last = last;
	}

	@Override
	public void setFocused(boolean focused) {
		if (!focused) {
			searchHistoryIndex = 0;
			String currentSearch = getText();
			if (!currentSearch.trim().isEmpty() && !currentSearch.isEmpty()) {
				searchHistory.removeIf(s -> s.trim().isEmpty());
				searchHistory.remove(currentSearch);
				searchHistory.add(0, currentSearch);
				if (searchHistory.size() > 36) {
					searchHistory.remove(searchHistory.size() - 1);
				}
			}
		}
		isFocused = focused;
		super.setFocused(focused);
	}

	@Override
	public void write(String s) {
		super.write(s);
		handleText();
	}

	@Override
	public void eraseCharacters(int characterOffset) {
		super.eraseCharacters(characterOffset);
		handleText();
	}

	public void handleText() {
		if (getText().isEmpty()) {
			EmiScreenManager.focusSearchSidebarType(EmiConfig.emptySearchSidebarFocus);
		} else {
			EmiScreenManager.focusSearchSidebarType(EmiConfig.searchSidebarFocus);
		}
		Matcher matcher = EmiSearch.TOKENS.matcher(getText());
		List<Pair<Integer, Style>> styles = Lists.newArrayList();
		int last = 0;
		while (matcher.find()) {
			int start = matcher.start();
			int end = matcher.end();
			if (last < start) {
				styles.add(Pair.of(start, new Style().setFormatting(Formatting.WHITE)));
			}
			String group = matcher.group();
			if (group.startsWith("-")) {
				styles.add(Pair.of(start + 1, new Style().setFormatting(Formatting.RED)));
				start++;
				group = group.substring(1);
			}
			QueryType type = QueryType.fromString(group);
			int subStart = type.prefix.length();
			if (group.length() > 1 + subStart && group.substring(subStart).startsWith("/") && group.endsWith("/")) {
				int rOff = start + subStart + 1;
				styles.add(Pair.of(rOff, type.slashColor));
				Matcher rMatcher = ESCAPE.matcher(getText().substring(rOff, end - 1));
				int rLast = 0;
				while (rMatcher.find()) {
					int rStart = rMatcher.start();
					int rEnd = rMatcher.end();
					if (rLast < rStart) {
						styles.add(Pair.of(rStart + rOff, type.regexColor));
					}
					styles.add(Pair.of(rEnd + rOff, type.escapeColor));
					rLast = rEnd;
				}
				if (rLast < end - 1) {
					styles.add(Pair.of(end - 1, type.regexColor));
				}
				styles.add(Pair.of(end, type.slashColor));
			} else {
				styles.add(Pair.of(end, type.color));
			}

			last = end;
		}
		if (last < getText().length()) {
			styles.add(Pair.of(getText().length(), new Style().setFormatting(Formatting.WHITE)));
		}
		this.styles = styles;
		EmiSearch.search(getText());
	}

	@Override
	public boolean isFocused() {
		return isFocused;
	}
	
	@Override
	public void mouseClicked(int mouseX, int mouseY, int button) {
		if (!isMouseOver(mouseX, mouseY) || !EmiConfig.enabled) {
			setFocused(false);
        } else {
			super.mouseClicked(mouseX, mouseY, button == 1 ? 0 : button);
			if (this.isFocused()) {
				if (button == 0) {
					if (System.currentTimeMillis() - lastClick < 500) {
						highlight = !highlight;
						lastClick = 0;
					} else {
						lastClick = System.currentTimeMillis();
					}
				} else if (button == 1) {
					this.setText("");
					this.setFocused(true);
				}
			}
		}
	}

	@Override
	public boolean keyPressed(char c, int keyCode) {
		if (this.isFocused()) {
			if (EmiConfig.clearSearch.matchesKey(keyCode, keyCode)) {
				setText("");
				return true;
			}
			if ((EmiConfig.focusSearch.matchesKey(keyCode, keyCode)
					|| keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE)) {
				this.setFocused(false);
				this.setFocused(false);
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
				int offset = keyCode == GLFW.GLFW_KEY_UP ? 1 : -1;
				if (searchHistoryIndex + offset >= 0 && searchHistoryIndex + offset < searchHistory.size()) {
					if (searchHistoryIndex >= 0 && searchHistoryIndex < searchHistory.size()) {
						searchHistory.set(searchHistoryIndex, getText());
					}
					searchHistoryIndex += offset;
					setText(searchHistory.get(searchHistoryIndex));
				}
			}
		}
		return super.keyPressed(c, keyCode);
	}

	@Override
	public void render() {
		EmiDrawContext context = EmiDrawContext.wrap(MatrixStack.INSTANCE);
		this.setEditable(EmiConfig.enabled);
		String lower = getText().toLowerCase();

		boolean dinnerbone = lower.contains("dinnerbone");
		accumulatedSpin += (dinnerbone ? 1 : -1) * Math.abs(System.currentTimeMillis() - lastRender);
		if (accumulatedSpin < 0) {
			accumulatedSpin = 0;
		} else if (accumulatedSpin > 500) {
			accumulatedSpin = 500;
		}
		lastRender = System.currentTimeMillis();
		long deg = accumulatedSpin * -180 / 500;
		MatrixStack view = RenderSystem.getModelViewStack();
		view.push();
		if (deg != 0) {
			view.translate(this.x + this.width / 2, this.y + this.height / 2, 0);
			view.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(deg));
			view.translate(-(this.x + this.width / 2), -(this.y + this.height / 2), 0);
			RenderSystem.applyModelViewMatrix();
		}

		if (lower.contains("jeb_")) {
			int amount = 0x3FF;
			float h = ((lastRender & amount) % (float) amount) / (float) amount;
			int rgb = Color.HSBtoRGB(h, 1, 1);
			context.setColor(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, ((rgb >> 0) & 0xFF) / 255f);
		}

		if (EmiConfig.enabled) {
			String old = getText();
			setText(getRenderText(this.getText(), this.firstCharacterIndex).asString());
			super.render();
			setText(old);
			if (!this.isFocused() && getText().isEmpty()) {
				context.drawText(EmiPort.translatable("emi.search"), this.x + 4, this.y + (this.height - 8) / 2, -8355712);
			}
			if (highlight) {
				int border = 0xffeeee00;
				context.fill(this.x - 1, this.y - 1, this.width + 2, 1, border);
				context.fill(this.x - 1, this.y + this.height, this.width + 2, 1, border);
				context.fill(this.x - 1, this.y - 1, 1, this.height + 2, border);
				context.fill(this.x + this.width, this.y - 1, 1, this.height + 2, border);
			}
		}
		context.resetColor();
		view.pop();
		RenderSystem.applyModelViewMatrix();
	}

	private OrderedText getRenderText(String string, int stringStart) {
		Text text = EmiPort.literal("");
		int s = 0;
		int last = 0;
		for (; s < styles.size(); s++) {
			Pair<Integer, Style> style = styles.get(s);
			int end = style.first();
			if (end > stringStart) {
				if (end - stringStart >= string.length()) {
					text = EmiPort.literal(string.substring(0, string.length()), style.second());
					// Skip second loop
					s = styles.size();
					break;
				}
				text = EmiPort.literal(string.substring(0, end - stringStart), style.second());
				last = end - stringStart;
				s++;
				break;
			}
		}
		for (; s < styles.size(); s++) {
			Pair<Integer, Style> style = styles.get(s);
			int end = style.first();
			if (end - stringStart >= string.length()) {
				EmiPort.append(text, EmiPort.literal(string.substring(last, string.length()), style.second()));
				break;
			}
			EmiPort.append(text, EmiPort.literal(string.substring(last, end - stringStart), style.second()));
			last = end - stringStart;
		}
		EmiUtil.deParentText(text);
		return EmiPort.ordered(text);
	}
}
