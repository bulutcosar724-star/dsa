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
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Artı / nokta / özel çizim: keskin dikdörtgenler (DrawContext.fill).
 * Daire ve ok: gerçek geometri + 1 gerçek piksellik yumuşatılmış kenar (pikselsiz görünüm).
 */
public final class CrosshairRenderer {
    private CrosshairRenderer() {}

    /** Yumusak sekiller icin dortgenler: her kose {x, y, alfa} */
    private static final class Mesh {
        final List<float[]> quads = new ArrayList<>();

        boolean isEmpty() { return quads.isEmpty(); }

        void quad(float x1, float y1, float a1, float x2, float y2, float a2,
                  float x3, float y3, float a3, float x4, float y4, float a4) {
            quads.add(new float[]{x1, y1, a1, x2, y2, a2, x3, y3, a3, x4, y4, a4});
        }
    }

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
        Mesh fill = new Mesh();
        Mesh outl = new Mesh();
        float f = 1.0f / (float) MinecraftClient.getInstance().getWindow().getScaleFactor();
        int o = cfg.outline ? cfg.outlineThickness : 0;
        build(cfg, style, cx, cy, rects, fill, outl, f, o);

        boolean targetColor = onTarget && cfg.entityColorEnabled;
        boolean invert = cfg.vanillaColor && !targetColor;
        int color = targetColor ? cfg.entityColorInt() : cfg.colorInt();

        if (o > 0) {
            int oc = cfg.outlineColorInt();
            for (int[] r : rects) {
                ctx.fill(r[0] - o, r[1] - o, r[2] + o, r[3] + o, oc);
            }
            if (!outl.isEmpty()) {
                flush(ctx, Collections.emptyList(), outl, oc, false);
            }
        }
        if (invert) {
            flush(ctx, rects, fill, 0xFFFFFFFF, true);
        } else {
            for (int[] r : rects) {
                ctx.fill(r[0], r[1], r[2], r[3], color);
            }
            if (!fill.isEmpty()) {
                flush(ctx, Collections.emptyList(), fill, color, false);
            }
        }
    }

    /** Dortgenleri dogrudan cizer. invert=true: vanilla gibi arka plani ters cevirir. */
    private static void flush(DrawContext ctx, List<int[]> rects, Mesh mesh, int color, boolean invert) {
        if (rects.isEmpty() && mesh.isEmpty()) return;
        ctx.draw(); // bekleyen cizimleri once gonder, sira bozulmasin
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        if (invert) {
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR, GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR,
                    GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        } else {
            RenderSystem.defaultBlendFunc();
        }

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int[] r : rects) {
            int c = vertexColor(color, 1f, invert);
            buf.vertex(m, r[0], r[3], 0f).color(c);
            buf.vertex(m, r[2], r[3], 0f).color(c);
            buf.vertex(m, r[2], r[1], 0f).color(c);
            buf.vertex(m, r[0], r[1], 0f).color(c);
        }
        for (float[] q : mesh.quads) {
            for (int i = 0; i < 4; i++) {
                buf.vertex(m, q[i * 3], q[i * 3 + 1], 0f).color(vertexColor(color, q[i * 3 + 2], invert));
            }
        }
        BuiltBuffer built = buf.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
    }

    private static int vertexColor(int color, float a, boolean invert) {
        a = Math.max(0f, Math.min(1f, a));
        if (invert) {
            // beyaz * a: ters cevirme ile normal arasinda yumusak gecis
            int g = Math.round(a * 255f);
            return 0xFF000000 | (g << 16) | (g << 8) | g;
        }
        int alpha = Math.round(((color >>> 24) & 0xFF) * a);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static boolean isTargetingEntity(CrosshairConfig cfg) {
        if (!cfg.entityColorEnabled) return false;
        HitResult h = MinecraftClient.getInstance().crosshairTarget;
        if (h == null || h.getType() != HitResult.Type.ENTITY) return false;
        if (!cfg.targetOnlyLiving) return true;
        return ((EntityHitResult) h).getEntity() instanceof LivingEntity;
    }

    // dikdortgenler: {x1, y1, x2, y2}  (x2, y2 dahil degil)
    private static void build(CrosshairConfig cfg, CrosshairConfig.Style style, int cx, int cy,
                              List<int[]> out, Mesh fill, Mesh outl, float f, int o) {
        float fcx = cx + 0.5f, fcy = cy + 0.5f; // merkez pikselin tam ortasi
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
                float ro = cfg.size + 0.5f;
                float ri = Math.max(0f, ro - cfg.thickness);
                ring(fill, fcx, fcy, ri, ro, f);
                if (o > 0) ring(outl, fcx, fcy, Math.max(0f, ri - o), ro + o, f);
                if (cfg.centerDot) dot(out, cx, cy, cfg.dotSize);
                break;
            }
            case ARROW: {
                // Ince "^" (chevron): olculer GERCEK ekran pikseli cinsinden, GUI olcegine bagli degil
                float u = f;
                float a = (float) Math.toRadians(cfg.angle);
                float sn = (float) Math.sin(a), cs = (float) Math.cos(a);
                float s0 = cfg.gap * u, s1 = (cfg.gap + cfg.size) * u;
                float w = (0.5f + 0.5f * cfg.thickness) * u;   // kalinlik 1 = 1 gercek piksel
                float oo = o * u;
                float ay = fcy - (s0 + s1) / 2f * cs;           // dikeyde ortala
                for (int side = -1; side <= 1; side += 2) {
                    float dx = side * sn, dy = cs;
                    float st = s0 - w / 2f;                      // tepe noktasini doldur
                    line(fill, fcx + dx * st, ay + dy * st, fcx + dx * s1, ay + dy * s1, w, f);
                    if (o > 0) {
                        line(outl, fcx + dx * (st - oo), ay + dy * (st - oo),
                                fcx + dx * (s1 + oo), ay + dy * (s1 + oo), w + 2 * oo, f);
                    }
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

    /** Yumusak kenarli halka (veya dolu daire, rIn = 0). */
    private static void ring(Mesh m, float cx, float cy, float rIn, float rOut, float f) {
        float[] r = {Math.max(0f, rIn - f), rIn, rOut, rOut + f};
        float edgeIn = rIn <= 0f ? 1f : (rIn >= f ? 0f : 1f - rIn / f);
        float[] al = {edgeIn, 1f, 1f, 0f};
        int segs = Math.max(48, Math.min(256, (int) (rOut * 10)));
        double step = Math.PI * 2 / segs;
        for (int k = 0; k < segs; k++) {
            double t1 = k * step, t2 = (k + 1) * step;
            float c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
            float c2 = (float) Math.cos(t2), s2 = (float) Math.sin(t2);
            for (int l = 0; l < 3; l++) {
                m.quad(cx + c1 * r[l], cy + s1 * r[l], al[l],
                        cx + c2 * r[l], cy + s2 * r[l], al[l],
                        cx + c2 * r[l + 1], cy + s2 * r[l + 1], al[l + 1],
                        cx + c1 * r[l + 1], cy + s1 * r[l + 1], al[l + 1]);
            }
        }
    }

    /** Yumusak kenarli kalin cizgi. */
    private static void line(Mesh m, float x0, float y0, float x1, float y1, float w, float f) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) return;
        float nx = -dy / len, ny = dx / len;
        float h = w / 2f;
        float[] off = {-(h + f), -h, h, h + f};
        float[] al = {0f, 1f, 1f, 0f};
        for (int i = 0; i < 3; i++) {
            m.quad(x0 + nx * off[i], y0 + ny * off[i], al[i],
                    x1 + nx * off[i], y1 + ny * off[i], al[i],
                    x1 + nx * off[i + 1], y1 + ny * off[i + 1], al[i + 1],
                    x0 + nx * off[i + 1], y0 + ny * off[i + 1], al[i + 1]);
        }
    }

    private static void dot(List<int[]> out, int cx, int cy, int d) {
        int x0 = cx - (d - 1) / 2, y0 = cy - (d - 1) / 2;
        out.add(new int[]{x0, y0, x0 + d, y0 + d});
    }
}
