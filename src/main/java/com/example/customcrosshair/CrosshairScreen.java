package com.example.customcrosshair;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class CrosshairScreen extends Screen {
    private static final int COL_W = 150;
    private static final int GAP = 10;
    private static final int PREVIEW = 120;
    private static final int ROW = 22;

    private int previewX, previewY;
    private int labelY1, labelY2;
    private int colX;

    public CrosshairScreen() {
        super(Text.literal("Custom Crosshair"));
    }

    @Override
    protected void init() {
        CrosshairConfig cfg = CrosshairConfig.get();

        int totalW = COL_W + GAP + COL_W + GAP + PREVIEW;
        int x0 = Math.max(5, (this.width - totalW) / 2);
        int x1 = x0 + COL_W + GAP;
        colX = x0;
        previewX = x1 + COL_W + GAP;
        int y = 30;
        previewY = y;

        // Sol sutun: slider'lar + renk kutulari
        addDrawableChild(new IntSlider(x0, y, COL_W, 20, "Boyut", 0, 20, cfg.size, v -> cfg.size = v));
        y += ROW;
        addDrawableChild(new IntSlider(x0, y, COL_W, 20, "Kalinlik", 1, 10, cfg.thickness, v -> cfg.thickness = v));
        y += ROW;
        addDrawableChild(new IntSlider(x0, y, COL_W, 20, "Bosluk", 0, 20, cfg.gap, v -> cfg.gap = v));
        y += ROW;
        addDrawableChild(new IntSlider(x0, y, COL_W, 20, "Outline kalinlik", 0, 3, cfg.outlineThickness, v -> cfg.outlineThickness = v));
        y += ROW;
        addDrawableChild(new IntSlider(x0, y, COL_W, 20, "Nokta boyutu", 1, 8, cfg.dotSize, v -> cfg.dotSize = v));
        y += ROW + 11;

        labelY1 = y - 10;
        TextFieldWidget colorField = new TextFieldWidget(this.textRenderer, x0, y, COL_W, 20, Text.literal("Renk"));
        colorField.setMaxLength(8);
        colorField.setText(cfg.color);
        colorField.setChangedListener(s -> {
            String h = s.replace("#", "").trim();
            if (h.length() == 6 || h.length() == 8) {
                cfg.color = h.toUpperCase();
                cfg.colorInt = CrosshairConfig.parse(h, cfg.colorInt);
            }
        });
        addDrawableChild(colorField);
        y += ROW + 11;

        labelY2 = y - 10;
        TextFieldWidget outlineField = new TextFieldWidget(this.textRenderer, x0, y, COL_W, 20, Text.literal("Outline renk"));
        outlineField.setMaxLength(8);
        outlineField.setText(cfg.outlineColor);
        outlineField.setChangedListener(s -> {
            String h = s.replace("#", "").trim();
            if (h.length() == 6 || h.length() == 8) {
                cfg.outlineColor = h.toUpperCase();
                cfg.outlineColorInt = CrosshairConfig.parse(h, cfg.outlineColorInt);
            }
        });
        addDrawableChild(outlineField);

        // Sag sutun: ac/kapat butonlari
        int ry = 30;
        addDrawableChild(toggle(x1, ry, "Crosshair", () -> cfg.enabled, v -> cfg.enabled = v));
        ry += ROW;
        addDrawableChild(toggle(x1, ry, "Outline", () -> cfg.outline, v -> cfg.outline = v));
        ry += ROW;
        addDrawableChild(toggle(x1, ry, "Orta nokta", () -> cfg.centerDot, v -> cfg.centerDot = v));
        ry += ROW;
        addDrawableChild(toggle(x1, ry, "T sekli", () -> cfg.tShape, v -> cfg.tShape = v));
        ry += ROW;
        addDrawableChild(toggle(x1, ry, "3. sahista gizle", () -> cfg.hideInThirdPerson, v -> cfg.hideInThirdPerson = v));
        ry += ROW + 10;

        addDrawableChild(ButtonWidget.builder(Text.literal("Sifirla"), b -> {
            CrosshairConfig.reset();
            this.client.setScreen(new CrosshairScreen());
        }).dimensions(x1, ry, COL_W, 20).build());
        ry += ROW;
        addDrawableChild(ButtonWidget.builder(Text.literal("Bitti"), b -> this.close())
                .dimensions(x1, ry, COL_W, 20).build());
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
        context.drawTextWithShadow(this.textRenderer, "Renk (AARRGGBB)", colX, labelY1, 0xA0A0A0);
        context.drawTextWithShadow(this.textRenderer, "Outline renk (AARRGGBB)", colX, labelY2, 0xA0A0A0);

        // Onizleme: yari koyu yesil, yari acik gri zemin
        context.drawTextWithShadow(this.textRenderer, "Onizleme", previewX, previewY - 10, 0xA0A0A0);
        int half = PREVIEW / 2;
        context.fill(previewX, previewY, previewX + half, previewY + PREVIEW, 0xFF3C6B2E);
        context.fill(previewX + half, previewY, previewX + PREVIEW, previewY + PREVIEW, 0xFFB8B8B8);
        CrosshairRenderer.render(context, previewX + half, previewY + half);
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
            super(x, y, w, h, Text.empty(), (value - min) / (double) (max - min));
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
