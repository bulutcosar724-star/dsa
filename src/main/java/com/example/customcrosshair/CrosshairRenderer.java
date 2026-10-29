package com.example.customcrosshair;

import net.minecraft.client.gui.DrawContext;

public class CrosshairRenderer {

    public static void render(DrawContext ctx) {
        CrosshairConfig c = CrosshairConfig.get();
        int cx = ctx.getScaledWindowWidth() / 2;
        int cy = ctx.getScaledWindowHeight() / 2;

        int t = c.thickness;
        int lo = -(t / 2);
        int hi = lo + t;
        int g = c.gap;
        int s = c.size;

        // Her dikdortgen: {x1, y1, x2, y2}
        int[][] rects = new int[5][];
        int n = 0;

        if (s > 0) {
            rects[n++] = new int[]{cx - g - s, cy + lo, cx - g, cy + hi};      // sol
            rects[n++] = new int[]{cx + g, cy + lo, cx + g + s, cy + hi};      // sag
            if (!c.tShape) {
                rects[n++] = new int[]{cx + lo, cy - g - s, cx + hi, cy - g};  // ust
            }
            rects[n++] = new int[]{cx + lo, cy + g, cx + hi, cy + g + s};      // alt
        }

        int[] dot = null;
        if (c.centerDot) {
            int dl = -(c.dotSize / 2);
            dot = new int[]{cx + dl, cy + dl, cx + dl + c.dotSize, cy + dl + c.dotSize};
        }

        // Once outline (hepsi), sonra ana renk; boylece outline ust uste binmez
        if (c.outline && c.outlineThickness > 0) {
            int o = c.outlineThickness;
            for (int i = 0; i < n; i++) {
                int[] r = rects[i];
                ctx.fill(r[0] - o, r[1] - o, r[2] + o, r[3] + o, c.outlineColorInt);
            }
            if (dot != null) {
                ctx.fill(dot[0] - o, dot[1] - o, dot[2] + o, dot[3] + o, c.outlineColorInt);
            }
        }

        for (int i = 0; i < n; i++) {
            int[] r = rects[i];
            ctx.fill(r[0], r[1], r[2], r[3], c.colorInt);
        }
        if (dot != null) {
            ctx.fill(dot[0], dot[1], dot[2], dot[3], c.colorInt);
        }
    }
}
