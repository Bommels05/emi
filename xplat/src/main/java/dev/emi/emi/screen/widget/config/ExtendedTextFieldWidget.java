package dev.emi.emi.screen.widget.config;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.input.Keyboard;

public class ExtendedTextFieldWidget extends TextFieldWidget {
    private boolean repeatEventsEnabled = false;

    public ExtendedTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height) {
        super(textRenderer, x, y, width, height);
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return (mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height);
    }

    @Override
    public void setFocused(boolean focused) {
        if (focused) {
            repeatEventsEnabled = Keyboard.areRepeatEventsEnabled();
            Keyboard.enableRepeatEvents(true);
        } else {
            Keyboard.enableRepeatEvents(repeatEventsEnabled);
        }
        super.setFocused(focused);
    }
}
