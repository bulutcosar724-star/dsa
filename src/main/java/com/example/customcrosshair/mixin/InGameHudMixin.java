package com.example.customcrosshair.mixin;

import com.example.customcrosshair.CrosshairConfig;
import com.example.customcrosshair.CrosshairRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void customcrosshair$render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CrosshairConfig cfg = CrosshairConfig.get();
        if (!cfg.enabled) return; // kapaliysa vanilla crosshair cizilsin

        ci.cancel(); // vanilla crosshair'i iptal et
        MinecraftClient mc = MinecraftClient.getInstance();
        if (cfg.hideInThirdPerson && !mc.options.getPerspective().isFirstPerson()) return;

        CrosshairRenderer.render(context);
    }
}
