package dev.autoswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;

public final class AutoSwapClient implements ClientModInitializer {
    private static KeyBinding openMenuKey;
    private static KeyBinding swapKey;
    private static boolean enabled = true;
    private static String swapStatus = "Ready";

    @Override
    public void onInitializeClient() {
        AutoSwapConfig.load();
        enabled = AutoSwapConfig.isEnabled();
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoswap.open_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O,
                "category.autoswap"));
        swapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoswap.swap_head", InputUtil.Type.KEYSYM,
                AutoSwapConfig.getKeyCode(), "category.autoswap"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new AutoSwapScreen());
            }
            while (swapKey.wasPressed()) {
                if (enabled) swapHeadToOffhand(client);
            }
        });
    }

    private static void swapHeadToOffhand(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null || client.currentScreen != null) {
            swapStatus = "Cannot swap right now (screen/player unavailable).";
            return;
        }

        var player = client.player;
        if (player.getOffHandStack().isOf(Items.PLAYER_HEAD)) {
            swapStatus = "A Player Head is already in the offhand.";
            return;
        }

        // Vanilla F swaps the selected main-hand stack with the offhand.
        // Reuse the actual vanilla key binding so Minecraft handles the
        // normal client action and sends its ordinary server packet.
        int selectedSlot = player.getInventory().getSelectedSlot();
        if (!player.getInventory().getStack(selectedSlot).isOf(Items.PLAYER_HEAD)) {
            swapStatus = "Select the hotbar slot containing the Player Head first.";
            return;
        }

        KeyBinding.onKeyPressed(InputUtil.fromKeyCode(GLFW.GLFW_KEY_F, 0));
        swapStatus = "Vanilla offhand swap triggered.";
    }

    static boolean isEnabled() { return enabled; }
    static String getSwapStatus() { return swapStatus; }
    static void setEnabled(boolean value) {
        enabled = value;
        AutoSwapConfig.setEnabled(value);
        AutoSwapConfig.save();
    }
    static int getSwapKeyCode() { return AutoSwapConfig.getKeyCode(); }
    static void setSwapKeyCode(int code) {
        swapKey.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(code));
        KeyBinding.updateKeysByCode();
        AutoSwapConfig.setKeyCode(code);
        AutoSwapConfig.save();
    }
}
