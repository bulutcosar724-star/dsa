package com.example.customcrosshair;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class CrosshairScreen extends Screen {
    private static final int COL_W = 130;
    private static final int GAP = 6;
    private static final int ROW = 22;
    private static final int BOX = 62;

    private int x0;
    private int boxY;
    private final List<String> labelTexts = new ArrayList<>();
    private final List<int[]> labelPos = new ArrayList<>();

    public CrosshairScreen() {
        super(Text.literal("Custom Crosshair"));
    }

    @Override
    protected void init() {
        labelTexts.clear();
        labelPos.clear();
        CrosshairConfig cfg = CrosshairConfig.get();

        int totalW = COL_W * 3 + GAP * 2;
        x0 = Math.max(5, (this.width - totalW) / 2);
        int x1 = x0 + COL_W + GAP;
        int x2 = x1 + COL_W + GAP;

        // ---- Sutun 1: slider'lar ----
        int y = 30;
        addDrawableChild(new IntSlider(x0, y, COL_W, 20, "Boyut", 0, 20, cfg.size, v -> cfg.size = v));
        y += ROW;
        addDrawableChild(new IntSlider(x0, y, COL_W, 20, "Kalinlik", 1, 10, cfg.thickness, v -> cfg.thickness = v));
        y += ROW;
        addDrawableChild(new IntSlider(x0, y, COL_W, 20, "Bosluk (arti)", 0, 20, cfg.gap, v -> cfg.gap = v));
        y += ROW;
        addDrawableChild(new IntSlider(x0, y, COL_W, 20, "Outline", 0, 3, cfg.outlineThickness, v -> cfg.outlineThickness = v));
        y += ROW;
        addDrawableChild(new IntSlider(x0, y, COL_W, 20, "Nokta boyutu", 1, 8, cfg.dotSize, v -> cfg.dotSize = v));
        boxY = y + 36;

        // ---- Sutun 2: ac/kapat ----
        int ry = 30;
        addDrawableChild(toggle(x1, ry, "Crosshair", () -> cfg.enabled, v -> cfg.enabled = v));
        ry += ROW;
        addDrawableChild(toggle(x1, ry, "Outline", () -> cfg.outline, v -> cfg.outline = v));
        ry += ROW;
        addDrawableChild(toggle(x1, ry, "Orta nokta", () -> cfg.centerDot, v -> cfg.centerDot = v));
        ry += ROW;
        addDrawableChild(toggle(x1, ry, "T sekli", () -> cfg.tShape, v -> cfg.tShape = v));
        ry += ROW;
        addDrawableChild(toggle(x1, ry, "3. sahis gizle", () -> cfg.hideInThirdPerson, v -> cfg.hideInThirdPerson = v));
        ry += ROW;
        addDrawableChild(toggle(x1, ry, "Hedefte renk", () -> cfg.entityColorEnabled, v -> cfg.entityColorEnabled = v));
        ry += ROW;
        addDrawableChild(toggle(x1, ry, "Sadece canlilar", () -> cfg.targetOnlyLiving, v -> cfg.targetOnlyLiving = v));

        // ---- Sutun 3: stil, renkler, butonlar ----
        int cy = 30;
        addDrawableChild(ButtonWidget.builder(Text.literal("Stil: " + cfg.style.label), b -> {
            cfg.style = cfg.style.next();
            b.setMessage(Text.literal("Stil: " + cfg.style.label));
        }).dimensions(x2, cy, COL_W, 20).build());
        cy += ROW;

        cy = colorRow(x2, cy, "Renk (AARRGGBB)", cfg.color, s -> {
            cfg.color = s;
            cfg.colorInt = CrosshairConfig.parse(s, cfg.colorInt);
        });
        cy = colorRow(x2, cy, "Outline renk", cfg.outlineColor, s -> {
            cfg.outlineColor = s;
            cfg.outlineColorInt = CrosshairConfig.parse(s, cfg.outlineColorInt);
        });
        cy = colorRow(x2, cy, "Hedef renk", cfg.entityColor, s -> {
            cfg.entityColor = s;
            cfg.entityColorInt = CrosshairConfig.parse(s, cfg.entityColorInt);
        });

        addDrawableChild(ButtonWidget.builder(Text.literal("Sifirla"), b -> {
            CrosshairConfig.reset();
            this.client.setScreen(new CrosshairScreen());
        }).dimensions(x2, cy + 4, COL_W, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Bitti"), b -> this.close())
                .dimensions(x2, cy + 4 + ROW, COL_W, 20).build());
    }

    private int colorRow(int x, int y, String label, String initial, Consumer<String> onValid) {
        labelTexts.add(label);
        labelPos.add(new int[]{x, y + 1});
        TextFieldWidget f = new TextFieldWidget(this.textRenderer, x, y + 11, COL_W, 20, Text.literal(label));
        f.setMaxLength(8);
        f.setText(initial);
        f.setChangedListener(s -> {
            String h = s.replace("#", "").trim();
            if (h.length() == 6 || h.length() == 8) {
                onValid.accept(h.toUpperCase());
            }
        });
        addDrawableChild(f);
        return y + 33;
    }

    private ButtonWidget toggle(int x, int y, String label, BooleanSupplier get, Consumer<Boolean> set) {
        return ButtonWidget.builder(toggleText(label, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(toggleText(label, get.getAsBoolean()));
        }).dimensions(x, y, COL_W, 20).build();
    }

    private static Text toggleText(String label, boolean on) {
        return Text.literal(label + ": " + (on ? "ACIK" : "KAPALI"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        for (int i = 0; i < labelTexts.size(); i++) {
            int[] p = labelPos.get(i);
            context.drawTextWithShadow(this.textRenderer, labelTexts.get(i), p[0], p[1], 0xA0A0A0);
        }

        // Onizleme: solda normal, sagda hedefe bakarken
        context.drawTextWithShadow(this.textRenderer, "Normal", x0, boxY - 11, 0xA0A0A0);
        context.drawTextWithShadow(this.textRenderer, "Hedefte", x0 + BOX + 6, boxY - 11, 0xA0A0A0);
        context.fill(x0, boxY, x0 + BOX, boxY + BOX, 0xFF3C6B2E);
        context.fill(x0 + BOX + 6, boxY, x0 + BOX * 2 + 6, boxY + BOX, 0xFFB8B8B8);
        CrosshairRenderer.render(context, x0 + BOX / 2, boxY + BOX / 2, false);
        CrosshairRenderer.render(context, x0 + BOX + 6 + BOX / 2, boxY + BOX / 2, true);
    }

    @Override
    public void removed() {
        CrosshairConfig.save();
    }

    private static class IntSlider extends SliderWidget {
        private final String label;
        private final int min, max;
        private final IntConsumer setter;

        IntSlider(int x, int y, int w, int h, String label, int min, int max, int value, IntConsumer setter) {
            super(x, y, w, h, Text.empty(), (Math.min(Math.max(value, min), max) - min) / (double) (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        private int current() {
            return min + (int) Math.round(this.value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label + ": " + current()));
        }

        @Override
        protected void applyValue() {
            setter.accept(current());
        }
    }
}
