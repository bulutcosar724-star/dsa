package com.example.customcrosshair;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.joml.Matrix4f;

import java.util.Arrays;

public class CrosshairRenderer {
    // Dortgen tamponu: her dortgen 8 sayi (x1,y1 ... x4,y4)
    private static float[] q = new float[512];
    private static int qn;

    private static void quad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        if (qn + 8 > q.length) q = Arrays.copyOf(q, q.length * 2);
        // GUI'nin bekledigi sarma yonu (fill ile ayni): isaretli alan <= 0 olmali
        float area = (x1 * y2 - x2 * y1) + (x2 * y3 - x3 * y2) + (x3 * y4 - x4 * y3) + (x4 * y1 - x1 * y4);
        if (area <= 0) {
            q[qn++] = x1; q[qn++] = y1; q[qn++] = x2; q[qn++] = y2;
            q[qn++] = x3; q[qn++] = y3; q[qn++] = x4; q[qn++] = y4;
        } else {
            q[qn++] = x4; q[qn++] = y4; q[qn++] = x3; q[qn++] = y3;
            q[qn++] = x2; q[qn++] = y2; q[qn++] = x1; q[qn++] = y1;
        }
    }

    private static void rect(float x1, float y1, float x2, float y2) {
        quad(x1, y1, x1, y2, x2, y2, x2, y1);
    }

    /** Oyun ici HUD: ekran ortasi + hedef kontrolu */
    public static void render(DrawContext ctx) {
        render(ctx, ctx.getScaledWindowWidth() / 2f, ctx.getScaledWindowHeight() / 2f, isTargetingEntity());
    }

    private static boolean isTargetingEntity() {
        CrosshairConfig c = CrosshairConfig.get();
        if (!c.entityColorEnabled) return false;
        HitResult h = MinecraftClient.getInstance().crosshairTarget;
        if (h == null || h.getType() != HitResult.Type.ENTITY) return false;
        if (!c.targetOnlyLiving) return true;
        return ((EntityHitResult) h).getEntity() instanceof LivingEntity;
    }

    public static void render(DrawContext ctx, float cx, float cy, boolean onTarget) {
        CrosshairConfig c = CrosshairConfig.get();
        ctx.draw(); // onceki bekleyen cizimleri bosalt, sira bozulmasin

        // Orijinal renk: arka plani ters ceviren beyaz (vanilla gibi). Hedefteyken normal renk (kirmizi).
        boolean invert = c.vanillaColor && !onTarget;

        if (!invert && c.outline && c.outlineThickness > 0) {
            build(c, cx, cy, c.outlineThickness);
            flush(ctx, c.outlineColorInt, false);
        }
        build(c, cx, cy, 0);
        flush(ctx, invert ? 0xFFFFFFFF : (onTarget ? c.entityColorInt : c.colorInt), invert);
    }

    /** grow: outline icin sekli her yone ne kadar buyuteecegi (0 = normal sekil) */
    private static void build(CrosshairConfig c, float cx, float cy, float grow) {
        qn = 0;
        int icx = (int) Math.floor(cx);
        int icy = (int) Math.floor(cy);
        int t = c.thickness;
        int lo = -(t / 2);
        int hi = lo + t;

        switch (c.style) {
            case CROSS -> {
                int g = c.gap;
                int s = c.size;
                if (s > 0) {
                    rect(icx - g - s - grow, icy + lo - grow, icx - g + grow, icy + hi + grow);     // sol
                    rect(icx + g - grow, icy + lo - grow, icx + g + s + grow, icy + hi + grow);     // sag
                    if (!c.tShape) {
                        rect(icx + lo - grow, icy - g - s - grow, icx + hi + grow, icy - g + grow); // ust
                    }
                    rect(icx + lo - grow, icy + g - grow, icx + hi + grow, icy + g + s + grow);     // alt
                }
            }
            case DOT -> {
                int d = c.dotSize;
                int dl = -(d / 2);
                rect(icx + dl - grow, icy + dl - grow, icx + dl + d + grow, icy + dl + d + grow);
            }
            case CIRCLE -> {
                float r = Math.max(c.size, t);
                float ro = r + grow;
                float ri = Math.max(0f, r - t - grow);
                int segs = 48;
                for (int k = 0; k < segs; k++) {
                    double a1 = 2 * Math.PI * k / segs;
                    double a2 = 2 * Math.PI * (k + 1) / segs;
                    float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                    float c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);
                    quad(cx + ri * c1, cy + ri * s1,
                         cx + ro * c1, cy + ro * s1,
                         cx + ro * c2, cy + ro * s2,
                         cx + ri * c2, cy + ri * s2);
                }
            }
            case CHEVRON -> {
                if (c.size > 0) {
                    double phi = Math.toRadians(c.angle);
                    float sn = (float) Math.sin(phi);
                    float cs = (float) Math.cos(phi);
                    float w = t + 2 * grow;
                    float half = w / 2f;
                    float len = c.size + grow;

                    float ax = cx;
                    float ay = cy - (c.size * cs) / 2f;     // seklin dikey ortasi crosshair merkezi
                    float m = half / sn;                    // zirvedeki miter uzunlugu

                    float oax = ax, oay = ay - m;           // dis zirve
                    float iax = ax, iay = ay + m;           // ic zirve

                    // sol kol ucu
                    float lx = ax - len * sn, ly = ay + len * cs;
                    float lox = lx - cs * half, loy = ly - sn * half;
                    float lix = lx + cs * half, liy = ly + sn * half;
                    // sag kol ucu
                    float rx = ax + len * sn, ry = ay + len * cs;
                    float rox = rx + cs * half, roy = ry - sn * half;
                    float rix = rx - cs * half, riy = ry + sn * half;

                    quad(oax, oay, iax, iay, lix, liy, lox, loy);
                    quad(iax, iay, oax, oay, rox, roy, rix, riy);
                }
            }
        }

        if (c.centerDot && c.style != CrosshairConfig.Style.DOT) {
            int d = c.dotSize;
            int dl = -(d / 2);
            rect(icx + dl - grow, icy + dl - grow, icx + dl + d + grow, icy + dl + d + grow);
        }
    }

    private static void flush(DrawContext ctx, int color, boolean invert) {
        if (qn == 0) return;
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < qn; i += 8) {
            for (int k = 0; k < 4; k++) {
                bb.vertex(m, q[i + 2 * k], q[i + 2 * k + 1], 0f).color(color);
            }
        }
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        if (invert) {
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR, GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR,
                    GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        BufferRenderer.drawWithGlobalProgram(bb.end());
        RenderSystem.defaultBlendFunc();
    }
}
