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
    private static boolean waitingForHotbarMove;
    private static int pendingHotbarSlot = -1;

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
            finishPendingSwap(client);
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
        if (waitingForHotbarMove) {
            swapStatus = "Moving the Player Head into the selected hotbar slot...";
            return;
        }
        if (player.getOffHandStack().isOf(Items.PLAYER_HEAD)) {
            swapStatus = "A Player Head is already in the offhand.";
            return;
        }

        int selected = player.getInventory().getSelectedSlot();
        if (player.getInventory().getStack(selected).isOf(Items.PLAYER_HEAD)) {
            triggerVanillaOffhandSwap();
            swapStatus = "Vanilla offhand swap triggered.";
            return;
        }

        // Find the head in the main inventory or another hotbar slot.
        for (int slot = 9; slot <= 44; slot++) {
            if (handler.getSlot(slot).getStack().isOf(Items.PLAYER_HEAD)) {
                int selectedHandlerSlot = 36 + selected;
                if (slot == selectedHandlerSlot) {
                    triggerVanillaOffhandSwap();
                    swapStatus = "Vanilla offhand swap triggered.";
                    return;
                }

                // Use the ordinary inventory SWAP action to move it into the
                // selected hotbar slot. Wait for the server-updated inventory
                // before triggering vanilla F on the next client tick.
                client.interactionManager.clickSlot(handler.syncId, slot,
                        selected, SlotActionType.SWAP, player);
                waitingForHotbarMove = true;
                pendingHotbarSlot = selected;
                swapStatus = "Moving Player Head to the selected hotbar slot...";
                return;
            }
        }
        swapStatus = "No Player Head found in inventory.";
    }

    private static void finishPendingSwap(MinecraftClient client) {
        if (!waitingForHotbarMove) return;
        if (client.player == null || client.interactionManager == null) {
            waitingForHotbarMove = false;
            pendingHotbarSlot = -1;
            swapStatus = "Swap cancelled (player unavailable).";
            return;
        }

        var player = client.player;
        if (player.currentScreenHandler != player.playerScreenHandler) {
            waitingForHotbarMove = false;
            pendingHotbarSlot = -1;
            swapStatus = "Swap cancelled (close the container first).";
            return;
        }

        if (player.getInventory().getStack(pendingHotbarSlot).isOf(Items.PLAYER_HEAD)) {
            waitingForHotbarMove = false;
            pendingHotbarSlot = -1;
            if (player.getOffHandStack().isOf(Items.PLAYER_HEAD)) {
                swapStatus = "Player Head is already in the offhand.";
            } else {
                triggerVanillaOffhandSwap();
                swapStatus = "Vanilla offhand swap triggered from inventory.";
            }
        }
    }

    private static void triggerVanillaOffhandSwap() {
        KeyBinding.onKeyPressed(InputUtil.fromKeyCode(GLFW.GLFW_KEY_F, 0));
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
