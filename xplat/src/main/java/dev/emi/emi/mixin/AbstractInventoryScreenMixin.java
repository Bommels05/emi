package dev.emi.emi.mixin;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Ordering;

import dev.emi.emi.EmiPort;
import dev.emi.emi.config.EffectLocation;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.runtime.EmiDrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;

@Mixin(InventoryScreen.class)
public abstract class AbstractInventoryScreenMixin extends HandledScreen {
	@Unique
	private static boolean hasInventoryTabs = EmiAgnos.isModLoaded("inventorytabs");
	
	private AbstractInventoryScreenMixin() { super(null); }

	private String getStatusEffectDescription(StatusEffectInstance effectInstance) {
		return getEffectName(effectInstance, StatusEffect.STATUS_EFFECTS[effectInstance.getEffectId()]);
	}

	private void drawStatusEffectBackgrounds(MatrixStack matrices, int x, int height, Iterable<StatusEffectInstance> statusEffects, boolean wide) {
		int y = this.y;
		this.client.getTextureManager().bindTexture(INVENTORY_TEXTURE);
		for (StatusEffectInstance effectInstance : statusEffects) {
			this.drawTexture(x, y, 0, 166, 140, 32);
			y += height;
		}
	}

	private void drawStatusEffectSprites(MatrixStack matrices, int x, int height, Iterable<StatusEffectInstance> statusEffects, boolean wide) {
		int y = this.y + 7;
		for (StatusEffectInstance effectInstance : statusEffects) {
			StatusEffect effect = StatusEffect.STATUS_EFFECTS[effectInstance.getEffectId()];
			if (effect.hasIcon()) {
				int iconLevel = effect.getIconLevel();
				this.drawTexture(x, y, 0 + iconLevel % 8 * 18, 198 + iconLevel / 8 * 18, 18, 18);
			}
			y += height;
		}
	}

	private void drawStatusEffectDescriptions(MatrixStack matrices, int x, int height, Iterable<StatusEffectInstance> statusEffects) {
		int y = this.y + 6;
		for (StatusEffectInstance effectInstance : statusEffects) {
			StatusEffect effect = StatusEffect.STATUS_EFFECTS[effectInstance.getEffectId()];
			String name = getEffectName(effectInstance, effect);
			this.textRenderer.method_956(name, x, y, 16777215);
			String var10 = StatusEffect.getFormattedDuration(effectInstance);
			this.textRenderer.method_956(var10, x, y + 4, 8355711);
			y += height;
		}
	}

	private static String getEffectName(StatusEffectInstance effectInstance, StatusEffect effect) {
		String name = I18n.translate(effect.getTranslationKey());
		if (effectInstance.getAmplifier() == 1) {
			name = name + " " + I18n.translate("enchantment.level.2");
		} else if (effectInstance.getAmplifier() == 2) {
			name = name + " " + I18n.translate("enchantment.level.3");
		} else if (effectInstance.getAmplifier() == 3) {
			name = name + " " + I18n.translate("enchantment.level.4");
		}
		return name;
	}

	@Inject(at = @At(value = "INVOKE",
			target = "Lnet/minecraft/entity/player/ControllablePlayerEntity;getStatusEffectInstances()Ljava/util/Collection;"),
		method = "drawStatusEffects")
	private void drawStatusEffects(CallbackInfo info) {
		if (EmiConfig.effectLocation == EffectLocation.TOP) {
			emi$drawCenteredEffects(MatrixStack.INSTANCE, Mouse.getX(), Mouse.getY());
		}
	}

	@ModifyVariable(at = @At(value = "INVOKE", target = "Ljava/util/Collection;isEmpty()Z"),
		method = "drawStatusEffects", ordinal = 0)
	private Collection<StatusEffectInstance> drawStatusEffects(Collection<StatusEffectInstance> original) {
		if (EmiConfig.effectLocation == EffectLocation.TOP) {
			return List.of();
		}
		return original;
	}

	//todo fix compressed/wide
	private void emi$drawCenteredEffects(MatrixStack raw, int mouseX, int mouseY) {
		EmiDrawContext context = EmiDrawContext.wrap(raw);
		context.resetColor();
		Collection<StatusEffectInstance> effects = Ordering.natural().sortedCopy(this.client.field_3805.getStatusEffectInstances());
		int size = effects.size();
		if (size == 0) {
			return;
		}
		boolean wide = size == 1;
		int y = this.y - 34;
		if (((Object) this) instanceof CreativeInventoryScreen || hasInventoryTabs) {
			y -= 28;
			if (((Object) this) instanceof CreativeInventoryScreen && EmiAgnos.isForge()) {
				y -= 22;
			}
		}
		int xOff = 34;
		if (wide) {
			xOff = 122;
		} else if (size > 5) {
			xOff = (this.backgroundWidth - 32) / (size - 1);
		}
		int width = (size - 1) * xOff + (wide ? 120 : 32);
		int x = this.x + (this.backgroundWidth - width) / 2;
		StatusEffectInstance hovered = null;
		int restoreY = this.y;
		try {
			this.y = y;
			for (StatusEffectInstance inst : effects) {
				int ew = wide ? 120 : 32;
				List<StatusEffectInstance> single = List.of(inst);
				this.drawStatusEffectBackgrounds(context.raw(), x, 32, single, wide);
				this.drawStatusEffectSprites(context.raw(), x, 32, single, wide);
				if (wide) {
					this.drawStatusEffectDescriptions(context.raw(), x, 32, single);
				}
				if (mouseX >= x && mouseX < x + ew && mouseY >= y && mouseY < y + 32) {
					hovered = inst;
				}
				x += xOff;
			}
		} finally {
			this.y = restoreY;
		}
		if (hovered != null && size > 1) {
			List<String> list = List.of(this.getStatusEffectDescription(hovered),
				StatusEffect.getFormattedDuration(hovered));
			this.renderTooltip(list, mouseX, Math.max(mouseY, 16));
		}
	}

	//todo fix
	/*@ModifyVariable(at = @At(value = "STORE", ordinal = 0),
		method = "drawStatusEffects", ordinal = 0)
	private boolean squishEffects(boolean original) {
		return !EmiConfig.effectLocation.compressed;
	}*/

	@ModifyVariable(at = @At(value = "STORE", ordinal = 0),
		method = "drawStatusEffects", ordinal = 2)
	private int changeEffectSpace(int original) {
		return switch (EmiConfig.effectLocation) {
			case RIGHT, RIGHT_COMPRESSED -> original;
			case TOP -> this.x;
			case LEFT_COMPRESSED -> this.x - 2- 32;
			case LEFT -> this.x - 2 - 120;
		};
	}
}
