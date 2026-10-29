package com.example.customcrosshair;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Piksel piksel crosshair editoru.
 * Sol tik = ciz, sag tik = sil, surukleyerek de calisir.
 */
public class PixelEditorScreen extends Screen {
    private static boolean mirrorX = false;
    private static boolean mirrorY = false;

    private final Screen parent;
    private final CrosshairConfig cfg = CrosshairConfig.get();

    private int n;
    private boolean[] px;
    private int cell, gx, gy, gridW;
    private int bx;

    private boolean painting;
    private boolean paintVal;
    private int lastC = -1, lastR = -1;

    public PixelEditorScreen(Screen parent) {
        super(Text.literal("Crosshair Cizimi"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        n = cfg.customGrid;
        px = cfg.pixels();

        cell = Math.max(4, Math.min(16, (height - 60) / n));
        gridW = n * cell;
        gx = Math.max(8, width / 2 - gridW - 12);
        gy = Math.max(30, 30 + (height - 60 - gridW) / 2);
        bx = gx + gridW + 16;

        int bw = 70, g = 4, row = 22;
        int y = 30;

        addDrawableChild(ButtonWidget.builder(Text.literal("Grid -"), b -> resize(-2))
                .dimensions(bx, y, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Grid +"), b -> resize(2))
                .dimensions(bx + bw + g, y, bw, 20).build());
        y += row;

        addDrawableChild(ButtonWidget.builder(Text.literal("Sim X: " + onOff(mirrorX)), b -> {
            mirrorX = !mirrorX;
            b.setMessage(Text.literal("Sim X: " + onOff(mirrorX)));
        }).dimensions(bx, y, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Sim Y: " + onOff(mirrorY)), b -> {
            mirrorY = !mirrorY;
            b.setMessage(Text.literal("Sim Y: " + onOff(mirrorY)));
        }).dimensions(bx + bw + g, y, bw, 20).build());
        y += row;

        addDrawableChild(ButtonWidget.builder(Text.literal("Ters cevir"), b -> {
            for (int i = 0; i < px.length; i++) px[i] = !px[i];
            store();
        }).dimensions(bx, y, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Temizle"), b -> {
            px = new boolean[n * n];
            store();
        }).dimensions(bx + bw + g, y, bw, 20).build());
        y += row;

        addDrawableChild(ButtonWidget.builder(Text.literal("Olcek: " + cfg.customScale + "x"), b -> {
            cfg.customScale = cfg.customScale % 4 + 1;
            b.setMessage(Text.literal("Olcek: " + cfg.customScale + "x"));
        }).dimensions(bx, y, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Arti sablon"), b -> {
            px = CrosshairConfig.plus(n);
            store();
        }).dimensions(bx + bw + g, y, bw, 20).build());
        y += row;

        addDrawableChild(ButtonWidget.builder(Text.literal("Kaydet ve kullan"), b -> {
            cfg.style = CrosshairConfig.Style.CUSTOM;
            close();
        }).dimensions(bx, y, bw * 2 + g, 20).build());
        y += row;

        addDrawableChild(ButtonWidget.builder(Text.literal("Geri"), b -> close())
                .dimensions(bx, y, bw * 2 + g, 20).build());
        y += row + 6;

        previewY = y;
    }

    private int previewY;

    private static String onOff(boolean b) {
        return b ? "ACIK" : "KAPALI";
    }

    private void resize(int delta) {
        int newN = n + delta;
        if (newN < 5 || newN > 31) return;
        boolean[] np = new boolean[newN * newN];
        int off = (newN - n) / 2;
        for (int y = 0; y < newN; y++) {
            for (int x = 0; x < newN; x++) {
                int ox = x - off, oy = y - off;
                if (ox >= 0 && oy >= 0 && ox < n && oy < n) {
                    np[y * newN + x] = px[oy * n + ox];
                }
            }
        }
        cfg.customGrid = newN;
        cfg.setPixels(np);
        cfg.save();
        client.setScreen(new PixelEditorScreen(parent));
    }

    private void store() {
        cfg.setPixels(px);
    }

    private void set(int c, int r, boolean v) {
        if (c >= 0 && r >= 0 && c < n && r < n) {
            px[r * n + c] = v;
        }
    }

    private void stamp(int c, int r, boolean v) {
        set(c, r, v);
        if (mirrorX) set(n - 1 - c, r, v);
        if (mirrorY) set(c, n - 1 - r, v);
        if (mirrorX && mirrorY) set(n - 1 - c, n - 1 - r, v);
    }

    private int colAt(double mx) {
        return (int) Math.floor((mx - gx) / cell);
    }

    private int rowAt(double my) {
        return (int) Math.floor((my - gy) / cell);
    }

    private boolean inside(int c, int r) {
        return c >= 0 && r >= 0 && c < n && r < n;
    }

    private void paintTo(int c, int r) {
        if (lastC < 0) {
            stamp(c, r, paintVal);
        } else {
            int steps = Math.max(Math.abs(c - lastC), Math.abs(r - lastR));
            for (int i = 0; i <= steps; i++) {
                double t = steps == 0 ? 0 : i / (double) steps;
                int cc = (int) Math.round(lastC + (c - lastC) * t);
                int rr = (int) Math.round(lastR + (r - lastR) * t);
                stamp(cc, rr, paintVal);
            }
        }
        lastC = c;
        lastR = r;
        store();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button == 0 || button == 1) {
            int c = colAt(mx), r = rowAt(my);
            if (inside(c, r)) {
                painting = true;
                paintVal = (button == 0);
                lastC = -1;
                lastR = -1;
                paintTo(c, r);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (painting) {
            int c = colAt(mx), r = rowAt(my);
            if (inside(c, r)) {
                paintTo(c, r);
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        painting = false;
        lastC = -1;
        lastR = -1;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, "Sol tik: ciz   Sag tik: sil", gx, 20, 0xFFAAAAAA);

        // izgara
        ctx.fill(gx - 1, gy - 1, gx + gridW + 1, gy + gridW + 1, 0xFF000000);
        int on = cfg.colorInt() | 0xFF000000;
        int center = n / 2;
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                int color;
                if (px[r * n + c]) {
                    color = on;
                } else if (r == center && c == center) {
                    color = 0xFF553333;
                } else {
                    color = (((r + c) & 1) == 0) ? 0xFF2A2A2A : 0xFF333333;
                }
                int x = gx + c * cell, y = gy + r * cell;
                ctx.fill(x, y, x + cell - 1, y + cell - 1, color);
            }
        }
        int hc = colAt(mouseX), hr = rowAt(mouseY);
        if (inside(hc, hr)) {
            int x = gx + hc * cell, y = gy + hr * cell;
            ctx.fill(x, y, x + cell - 1, y + cell - 1, 0x66FFFFFF);
        }

        // gercek boyutlu onizleme
        int pw = 144;
        int ph = Math.min(60, height - previewY - 8);
        if (ph >= 20) {
            ctx.fill(bx - 1, previewY - 1, bx + pw + 1, previewY + ph + 1, 0xFF000000);
            ctx.fill(bx, previewY, bx + pw, previewY + ph, 0xFF3A5F3A);
            ctx.enableScissor(bx, previewY, bx + pw, previewY + ph);
            CrosshairRenderer.draw(ctx, cfg, bx + pw / 2, previewY + ph / 2, false, CrosshairConfig.Style.CUSTOM);
            ctx.disableScissor();
        }
    }

    @Override
    public void close() {
        cfg.save();
        client.setScreen(parent);
    }
}
