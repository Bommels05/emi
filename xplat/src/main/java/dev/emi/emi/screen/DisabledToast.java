package dev.emi.emi.screen;

import dev.emi.emi.EmiPort;
import dev.emi.emi.config.EmiConfig;
import net.minecraft.advancement.Achievement;
import net.minecraft.block.Blocks;
import net.minecraft.text.Text;

//Disabling Emi is now an achievement I guess?
public class DisabledToast extends Achievement {

    public DisabledToast() {
        super(EmiPort.translatable("emi.disabled").asFormattedString(), "emi_disabled", 0, 0, Blocks.FIRE, null);
    }

    @Override
    public Text getText() {
        return EmiPort.translatable("emi.disabled");
    }

    @Override
    public String getDescription() {
        return EmiConfig.toggleVisibility.getBindText().asFormattedString();
    }
}
