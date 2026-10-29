package com.example.customcrosshair;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class CustomCrosshairClient implements ClientModInitializer {
    private static final String CATEGORY = "key.categories.customcrosshair";

    @Override
    public void onInitializeClient() {
        CrosshairConfig.load();

        int unbound = InputUtil.UNKNOWN_KEY.getCode();
        KeyBinding open = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.customcrosshair.open", InputUtil.Type.KEYSYM, unbound, CATEGORY));
        KeyBinding toggle = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.customcrosshair.toggle", InputUtil.Type.KEYSYM, unbound, CATEGORY));
        KeyBinding reload = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.customcrosshair.reload", InputUtil.Type.KEYSYM, unbound, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (open.wasPressed()) {
                client.setScreen(new CrosshairScreen(null));
            }
            while (toggle.wasPressed()) {
                CrosshairConfig cfg = CrosshairConfig.get();
                cfg.enabled = !cfg.enabled;
                cfg.save();
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("Custom Crosshair: " + (cfg.enabled ? "ACIK" : "KAPALI")), true);
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
