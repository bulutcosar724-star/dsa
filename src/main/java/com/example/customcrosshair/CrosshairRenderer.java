package com.example.customcrosshair;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Sadece DrawContext.fill ile dikdortgen cizer, FPS'e etkisi yok denecek kadar az. */
public final class CrosshairRenderer {
    private CrosshairRenderer() {}

    public static void render(DrawContext ctx) {
        CrosshairConfig cfg = CrosshairConfig.get();
        int cx = (ctx.getScaledWindowWidth() - 1) / 2;
        int cy = (ctx.getScaledWindowHeight() - 1) / 2;
        draw(ctx, cfg, cx, cy, isTargetingEntity(cfg), null);
    }

    /** cx, cy = merkez pikseli. override null degilse o stil cizilir (onizleme icin). */
    public static void draw(DrawContext ctx, CrosshairConfig cfg, int cx, int cy,
                            boolean onTarget, CrosshairConfig.Style override) {
        CrosshairConfig.Style style = override != null ? override : cfg.style;
        List<int[]> rects = new ArrayList<>();
        build(cfg, style, cx, cy, rects);

        boolean targetColor = onTarget && cfg.entityColorEnabled;
        boolean invert = cfg.vanillaColor && !targetColor;
        int color = targetColor ? cfg.entityColorInt() : cfg.colorInt();
        int o = cfg.outline ? cfg.outlineThickness : 0;
        if (o > 0) {
            int oc = cfg.outlineColorInt();
            for (int[] r : rects) {
                ctx.fill(r[0] - o, r[1] - o, r[2] + o, r[3] + o, oc);
            }
        }
        if (invert) {
            drawInverted(ctx, rects);
        } else {
            for (int[] r : rects) {
                ctx.fill(r[0], r[1], r[2], r[3], color);
            }
        }
    }

    /** Vanilla crosshair gibi arka plani ters ceviren blend ile cizer. */
    private static void drawInverted(DrawContext ctx, List<int[]> rects) {
        if (rects.isEmpty()) return;
        ctx.draw(); // bekleyen cizimleri (outline) once gonder
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR, GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int[] r : rects) {
            buf.vertex(m, r[0], r[3], 0f).color(0xFFFFFFFF);
            buf.vertex(m, r[2], r[3], 0f).color(0xFFFFFFFF);
            buf.vertex(m, r[2], r[1], 0f).color(0xFFFFFFFF);
            buf.vertex(m, r[0], r[1], 0f).color(0xFFFFFFFF);
        }
        BuiltBuffer built = buf.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }
        RenderSystem.defaultBlendFunc();
    }

    private static boolean isTargetingEntity(CrosshairConfig cfg) {
        if (!cfg.entityColorEnabled) return false;
        HitResult h = MinecraftClient.getInstance().crosshairTarget;
        if (h == null || h.getType() != HitResult.Type.ENTITY) return false;
        if (!cfg.targetOnlyLiving) return true;
        return ((EntityHitResult) h).getEntity() instanceof LivingEntity;
    }

    // dikdortgenler: {x1, y1, x2, y2}  (x2, y2 dahil degil)
    private static void build(CrosshairConfig cfg, CrosshairConfig.Style style, int cx, int cy, List<int[]> out) {
        switch (style) {
            case CROSS: {
                int t = cfg.thickness, g = cfg.gap, s = cfg.size;
                int x0 = cx - (t - 1) / 2, y0 = cy - (t - 1) / 2;
                int x1 = x0 + t, y1 = y0 + t;
                out.add(new int[]{x0 - g - s, y0, x0 - g, y1});        // sol
                out.add(new int[]{x1 + g, y0, x1 + g + s, y1});        // sag
                if (!cfg.tShape) {
                    out.add(new int[]{x0, y0 - g - s, x1, y0 - g});    // ust
                }
                out.add(new int[]{x0, y1 + g, x1, y1 + g + s});        // alt
                if (g == 0) {
                    out.add(new int[]{x0, y0, x1, y1});                // merkez dolgu
                }
                if (cfg.centerDot) dot(out, cx, cy, cfg.dotSize);
                break;
            }
            case DOT:
                dot(out, cx, cy, cfg.dotSize);
                break;
            case CIRCLE: {
                int r = cfg.size;
                double outer = r + 0.5;
                double inner = outer - cfg.thickness;
                for (int dy = -r; dy <= r; dy++) {
                    int start = Integer.MIN_VALUE;
                    for (int dx = -r; dx <= r + 1; dx++) {
                        boolean on = false;
                        if (dx <= r) {
                            double d = Math.sqrt(dx * dx + dy * dy);
                            on = d <= outer && d > inner;
                        }
                        if (on && start == Integer.MIN_VALUE) {
                            start = dx;
                        } else if (!on && start != Integer.MIN_VALUE) {
                            out.add(new int[]{cx + start, cy + dy, cx + dx, cy + dy + 1});
                            start = Integer.MIN_VALUE;
                        }
                    }
                }
                if (cfg.centerDot) dot(out, cx, cy, cfg.dotSize);
                break;
            }
            case ARROW: {
                int t = cfg.thickness;
                double a = Math.toRadians(cfg.angle);
                Set<Long> seen = new HashSet<>();
                for (int i = cfg.gap; i <= cfg.gap + cfg.size; i++) {
                    int dx = (int) Math.round(i * Math.sin(a));
                    int dy = (int) Math.round(i * Math.cos(a));
                    addBox(out, seen, cx + dx - (t - 1) / 2, cy + dy - (t - 1) / 2, t);
                    addBox(out, seen, cx - dx - (t - 1) / 2, cy + dy - (t - 1) / 2, t);
                }
                if (cfg.centerDot) dot(out, cx, cy, cfg.dotSize);
                break;
            }
            case CUSTOM: {
                int n = cfg.customGrid, sc = cfg.customScale;
                boolean[] p = cfg.pixels();
                int half = n / 2;
                int left = cx - half * sc;
                int top = cy - half * sc;
                for (int row = 0; row < n; row++) {
                    int start = -1;
                    for (int col = 0; col <= n; col++) {
                        boolean on = col < n && p[row * n + col];
                        if (on && start < 0) {
                            start = col;
                        } else if (!on && start >= 0) {
                            out.add(new int[]{left + start * sc, top + row * sc,
                                    left + col * sc, top + (row + 1) * sc});
                            start = -1;
                        }
                    }
                }
                break;
            }
        }
    }

    private static void dot(List<int[]> out, int cx, int cy, int d) {
        int x0 = cx - (d - 1) / 2, y0 = cy - (d - 1) / 2;
        out.add(new int[]{x0, y0, x0 + d, y0 + d});
    }

    private static void addBox(List<int[]> out, Set<Long> seen, int x, int y, int t) {
        long key = ((long) x << 32) ^ (y & 0xffffffffL);
        if (seen.add(key)) {
            out.add(new int[]{x, y, x + t, y + t});
        }
    }
}
