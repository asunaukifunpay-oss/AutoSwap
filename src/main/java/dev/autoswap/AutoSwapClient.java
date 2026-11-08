package dev.autoswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
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
        var handler = player.playerScreenHandler;
        if (player.currentScreenHandler != handler) {
            swapStatus = "Close the container before swapping.";
            return;
        }

        if (player.getOffHandStack().isOf(Items.PLAYER_HEAD)) {
            swapStatus = "A Player Head is already in the offhand.";
            return;
        }

        // Vanilla F swaps the selected hotbar slot with the offhand.
        // If the head is elsewhere, first use the ordinary hotbar SWAP action
        // to put it in the selected slot, then perform the vanilla offhand SWAP.
        int selectedHotbar = player.getInventory().selectedSlot;
        int selectedHandlerSlot = 36 + selectedHotbar;

        for (int slot = 9; slot <= 44; slot++) {
            if (!handler.getSlot(slot).getStack().isOf(Items.PLAYER_HEAD)) continue;

            if (slot != selectedHandlerSlot) {
                // Buttons 0-8 exchange the clicked slot with the corresponding hotbar slot.
                client.interactionManager.clickSlot(handler.syncId,
                        slot, selectedHotbar, SlotActionType.SWAP, player);
            }

            // Button 40 exchanges the selected hotbar slot with the offhand (vanilla F action).
            client.interactionManager.clickSlot(handler.syncId,
                    selectedHandlerSlot, 40, SlotActionType.SWAP, player);
            swapStatus = "Vanilla-style offhand swap sent; server inventory update will decide the result.";
            return;
        }
        swapStatus = "No Player Head found in inventory slots.";
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
