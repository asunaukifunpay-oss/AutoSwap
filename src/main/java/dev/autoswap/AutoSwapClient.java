package dev.autoswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

public final class AutoSwapClient implements ClientModInitializer {
    private static KeyBinding openMenuKey;
    private static KeyBinding swapKey;
    private static boolean enabled = true;

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

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new AutoSwapScreen());
            }
            while (swapKey.wasPressed()) {
                if (enabled) swapHeadToOffhand(client);
            }
        });
    }

    private static void swapHeadToOffhand(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null || client.currentScreen != null) return;
        var player = client.player;
        // Use the active handler and only swap from the player's own inventory.
        // START_CLIENT_TICK queues the inventory action before this tick's movement update.
        var handler = player.currentScreenHandler;
        if (handler != player.playerScreenHandler) return;
        // Slot 40 is the offhand swap button; slots 9-44 are inventory + hotbar.
        for (int slot = 9; slot <= 44; slot++) {
            var stack = handler.getSlot(slot).getStack();
            if (stack.isOf(Items.PLAYER_HEAD)) {
                client.interactionManager.clickSlot(handler.syncId,
                        slot, 40, SlotActionType.SWAP, player);
                return;
            }
        }
    }

    static boolean isEnabled() { return enabled; }
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
