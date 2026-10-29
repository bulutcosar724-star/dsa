package com.example.customcrosshair;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.Arrays;

public class CrosshairRenderer {
    // Dikdortgen tamponu (x1, y1, x2, y2 ...) - her karede yeni nesne olusturmamak icin
    private static int[] buf = new int[1024];
    private static int n;

    private static void add(int x1, int y1, int x2, int y2) {
        if (n + 4 > buf.length) buf = Arrays.copyOf(buf, buf.length * 2);
        buf[n++] = x1;
        buf[n++] = y1;
        buf[n++] = x2;
        buf[n++] = y2;
    }

    /** Oyun ici HUD: ekran ortasi + hedef kontrolu */
    public static void render(DrawContext ctx) {
        render(ctx, ctx.getScaledWindowWidth() / 2, ctx.getScaledWindowHeight() / 2, isTargetingEntity());
    }

    private static boolean isTargetingEntity() {
        CrosshairConfig c = CrosshairConfig.get();
        if (!c.entityColorEnabled) return false;
        HitResult h = MinecraftClient.getInstance().crosshairTarget;
        if (h == null || h.getType() != HitResult.Type.ENTITY) return false;
        if (!c.targetOnlyLiving) return true;
        return ((EntityHitResult) h).getEntity() instanceof LivingEntity;
    }

    public static void render(DrawContext ctx, int cx, int cy, boolean onTarget) {
        CrosshairConfig c = CrosshairConfig.get();
        n = 0;

        int t = c.thickness;
        int lo = -(t / 2);
        int hi = lo + t;

        switch (c.style) {
            case CROSS -> {
                int g = c.gap;
                int s = c.size;
                if (s > 0) {
                    add(cx - g - s, cy + lo, cx - g, cy + hi);      // sol
                    add(cx + g, cy + lo, cx + g + s, cy + hi);      // sag
                    if (!c.tShape) add(cx + lo, cy - g - s, cx + hi, cy - g); // ust
                    add(cx + lo, cy + g, cx + hi, cy + g + s);      // alt
                }
            }
            case DOT -> {
                int d = c.dotSize;
                int dl = -(d / 2);
                add(cx + dl, cy + dl, cx + dl + d, cy + dl + d);
            }
            case CIRCLE -> {
                int r = Math.max(c.size, t + 1);
                int rin = r - t;
                double ro = r + 0.5;
                double ri = rin + 0.5;
                for (int dy = -r; dy <= r; dy++) {
                    int xo = (int) Math.round(Math.sqrt(ro * ro - dy * dy));
                    int xi = 0;
                    if (rin > 0 && dy * dy < ri * ri) {
                        xi = (int) Math.round(Math.sqrt(ri * ri - dy * dy));
                    }
                    if (xi >= 1) {
                        add(cx - xo, cy + dy, cx - xi, cy + dy + 1);
                        add(cx + xi + 1, cy + dy, cx + xo + 1, cy + dy + 1);
                    } else {
                        add(cx - xo, cy + dy, cx + xo + 1, cy + dy + 1);
                    }
                }
            }
            case CHEVRON -> {
                int ay = cy - c.size / 2;
                for (int i = 0; i <= c.size; i++) {
                    int y = ay + i;
                    add(cx - i + lo, y + lo, cx - i + hi, y + hi);
                    if (i > 0) add(cx + i + lo, y + lo, cx + i + hi, y + hi);
                }
            }
        }

        if (c.centerDot && c.style != CrosshairConfig.Style.DOT) {
            int d = c.dotSize;
            int dl = -(d / 2);
            add(cx + dl, cy + dl, cx + dl + d, cy + dl + d);
        }

        if (c.outline && c.outlineThickness > 0) {
            int o = c.outlineThickness;
            for (int i = 0; i < n; i += 4) {
                ctx.fill(buf[i] - o, buf[i + 1] - o, buf[i + 2] + o, buf[i + 3] + o, c.outlineColorInt);
            }
        }

        int col = onTarget ? c.entityColorInt : c.colorInt;
        for (int i = 0; i < n; i += 4) {
            ctx.fill(buf[i], buf[i + 1], buf[i + 2], buf[i + 3], col);
        }
    }
}
