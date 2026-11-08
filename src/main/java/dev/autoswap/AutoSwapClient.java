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

    // Wait for the server to process the click and send any authoritative inventory correction.
    private static int verificationTicks = 0;
    private static boolean awaitingServerUpdate = false;
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
            if (awaitingServerUpdate && client.player != null) {
                if (--verificationTicks <= 0) {
                    // This reads the inventory after a short server-response window.
                    // Never resend automatically: a rejected action must not be spammed.
                    boolean confirmed = client.player.getOffHandStack().isOf(Items.PLAYER_HEAD);
                    swapStatus = confirmed
                            ? "Head is still in offhand after server update window."
                            : "Head was not confirmed; the server may have rejected the swap.";
                    awaitingServerUpdate = false;
                }
            }

            while (openMenuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new AutoSwapScreen());
            }
            while (swapKey.wasPressed()) {
                if (enabled) swapHeadToOffhand(client);
            }
        });
    }

    private static void swapHeadToOffhand(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null
                || client.currentScreen != null || awaitingServerUpdate) return;

        var player = client.player;
        var handler = player.currentScreenHandler;
        if (handler != player.playerScreenHandler) return;

        // Do not issue a click if the offhand already contains a player head.
        if (player.getOffHandStack().isOf(Items.PLAYER_HEAD)) return;

        // Slot 40 is the offhand swap button; slots 9-44 are inventory + hotbar.
        for (int slot = 9; slot <= 44; slot++) {
            if (handler.getSlot(slot).getStack().isOf(Items.PLAYER_HEAD)) {
                client.interactionManager.clickSlot(handler.syncId,
                        slot, 40, SlotActionType.SWAP, player);
                awaitingServerUpdate = true;
                verificationTicks = 10;
                swapStatus = "Waiting for server inventory update...";
                return;
            }
        }
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
