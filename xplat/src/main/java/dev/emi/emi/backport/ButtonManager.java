package dev.emi.emi.backport;

import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ButtonManager {
    private final Map<Integer, Consumer<ButtonWidget>> buttons = new HashMap<>();

    public int register(Consumer<ButtonWidget> action) {
        return register(buttons.size(), action);
    }

    public int register(int id, Consumer<ButtonWidget> action) {
        buttons.put(id, action);
        return id;
    }

    public void handleClick(ButtonWidget button) {
        if (button.active) {
            buttons.get(button.id).accept(button);
        }
    }

}
