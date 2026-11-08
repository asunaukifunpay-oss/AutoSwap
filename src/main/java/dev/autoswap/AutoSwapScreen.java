package dev.autoswap;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class AutoSwapScreen extends Screen {
    private boolean waitingForKey = false;
    private ButtonWidget bindButton;

    public AutoSwapScreen() { super(Text.literal("AutoSwap")); }

    @Override
    protected void init() {
        int center = width / 2;
        addDrawableChild(ButtonWidget.builder(toggleText(), button -> {
            AutoSwapClient.setEnabled(!AutoSwapClient.isEnabled());
            button.setMessage(toggleText());
        }).dimensions(center - 100, height / 2 - 42, 200, 20).build());

        bindButton = ButtonWidget.builder(bindText(), button -> {
            waitingForKey = true;
            button.setMessage(Text.literal("Press any key... (Esc to cancel)"));
        }).dimensions(center - 100, height / 2 - 12, 200, 20).build();
        addDrawableChild(bindButton);
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(center - 100, height / 2 + 28, 200, 20).build());
    }

    private Text toggleText() { return Text.literal("AutoSwap: " + (AutoSwapClient.isEnabled() ? "ON" : "OFF")); }
    private Text bindText() { return Text.literal("Swap key: " + keyName(AutoSwapClient.getSwapKeyCode())); }
    private String keyName(int code) {
        String name = GLFW.glfwGetKeyName(code, 0);
        return name == null ? "Key " + code : name.toUpperCase();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingForKey) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                waitingForKey = false;
                bindButton.setMessage(bindText());
            } else if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
                AutoSwapClient.setSwapKeyCode(keyCode);
                waitingForKey = false;
                bindButton.setMessage(bindText());
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 85, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Finds the first Player Head in your inventory and swaps it to offhand."),
                width / 2, height / 2 + 65, 0xAAAAAA);
    }
}
