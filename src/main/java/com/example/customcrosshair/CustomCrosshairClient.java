package com.example.customcrosshair;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class CustomCrosshairClient implements ClientModInitializer {
    private static final String CATEGORY = "key.categories.customcrosshair";

    @Override
    public void onInitializeClient() {
        CrosshairConfig.load();

        KeyBinding open = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.customcrosshair.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
        KeyBinding toggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.customcrosshair.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
        KeyBinding reload = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.customcrosshair.reload", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (open.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new CrosshairScreen());
                }
            }
            while (toggle.wasPressed()) {
                CrosshairConfig cfg = CrosshairConfig.get();
                cfg.enabled = !cfg.enabled;
                CrosshairConfig.save();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Custom Crosshair: " + (cfg.enabled ? "ACIK" : "KAPALI")), true);
                }
            }
            while (reload.wasPressed()) {
                CrosshairConfig.load();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Custom Crosshair config yenilendi"), true);
                }
            }
        });
    }
}
