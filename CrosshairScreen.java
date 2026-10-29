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
import java.util.regex.Pattern;

public class CrosshairScreen extends Screen {
    private static final Pattern HEX = Pattern.compile("[0-9a-fA-F]{6}|[0-9a-fA-F]{8}");
    private static final int COL_W = 120;
    private static final int GAP = 8;
    private static final int ROW = 22;

    private final Screen parent;
    private final List<Object[]> labels = new ArrayList<>();
    private int previewX, previewY;

    public CrosshairScreen(Screen parent) {
        super(Text.literal("Custom Crosshair"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        labels.clear();
        CrosshairConfig cfg = CrosshairConfig.get();

        int total = COL_W * 3 + GAP * 2;
        int x1 = Math.max(4, (width - total) / 2);
        int x2 = x1 + COL_W + GAP;
        int x3 = x2 + COL_W + GAP;
        int y = 34;

        // --- Sutun 1: sliderlar ---
        addDrawableChild(new IntSlider(x1, y, COL_W, 20, "Boyut", cfg.size, 1, 30, v -> cfg.size = v));
        addDrawableChild(new IntSlider(x1, y + ROW, COL_W, 20, "Kalinlik", cfg.thickness, 1, 10, v -> cfg.thickness = v));
        addDrawableChild(new IntSlider(x1, y + ROW * 2, COL_W, 20, "Bosluk", cfg.gap, 0, 20, v -> cfg.gap = v));
        addDrawableChild(new IntSlider(x1, y + ROW * 3, COL_W, 20, "Outline", cfg.outlineThickness, 0, 4, v -> cfg.outlineThickness = v));
        addDrawableChild(new IntSlider(x1, y + ROW * 4, COL_W, 20, "Nokta boyutu", cfg.dotSize, 1, 10, v -> cfg.dotSize = v));
        addDrawableChild(new IntSlider(x1, y + ROW * 5, COL_W, 20, "Aci (ok ^)", cfg.angle, 10, 80, v -> cfg.angle = v));
        previewX = x1;
        previewY = y + ROW * 6 + 6;

        // --- Sutun 2: acma/kapama ---
        toggle(x2, y, "Crosshair", () -> cfg.enabled, v -> cfg.enabled = v);
        toggle(x2, y + ROW, "Outline", () -> cfg.outline, v -> cfg.outline = v);
        toggle(x2, y + ROW * 2, "Orta nokta", () -> cfg.centerDot, v -> cfg.centerDot = v);
        toggle(x2, y + ROW * 3, "T sekli", () -> cfg.tShape, v -> cfg.tShape = v);
        toggle(x2, y + ROW * 4, "3. sahis gizle", () -> cfg.hideInThirdPerson, v -> cfg.hideInThirdPerson = v);
        toggle(x2, y + ROW * 5, "Hedefte renk", () -> cfg.entityColorEnabled, v -> cfg.entityColorEnabled = v);
        toggle(x2, y + ROW * 6, "Sadece canlilar", () -> cfg.targetOnlyLiving, v -> cfg.targetOnlyLiving = v);

        // --- Sutun 3: stil, renkler, butonlar ---
        addDrawableChild(ButtonWidget.builder(Text.literal("Stil: " + cfg.style.name()), b -> {
            CrosshairConfig.Style[] all = CrosshairConfig.Style.values();
            cfg.style = all[(cfg.style.ordinal() + 1) % all.length];
            b.setMessage(Text.literal("Stil: " + cfg.style.name()));
        }).dimensions(x3, y, COL_W, 20).build());

        colorField(x3, y + 36, "Renk (AARRGGBB)", cfg.color, s -> cfg.color = s);
        colorField(x3, y + 36 + 30, "Outline renk", cfg.outlineColor, s -> cfg.outlineColor = s);
        colorField(x3, y + 36 + 60, "Hedef renk", cfg.entityColor, s -> cfg.entityColor = s);

        int by = y + 36 + 60 + 24;
        addDrawableChild(ButtonWidget.builder(Text.literal("Kendin ciz"), b -> {
            cfg.save();
            client.setScreen(new PixelEditorScreen(this));
        }).dimensions(x3, by, COL_W, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Sifirla"), b -> {
            CrosshairConfig.reset();
            client.setScreen(new CrosshairScreen(parent));
        }).dimensions(x3, by + ROW, COL_W, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Bitti"), b -> close())
                .dimensions(x3, by + ROW * 2, COL_W, 20).build());
    }

    private void toggle(int x, int y, String label, BooleanSupplier get, Consumer<Boolean> set) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label + ": " + (get.getAsBoolean() ? "ACIK" : "KAPALI")), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(Text.literal(label + ": " + (get.getAsBoolean() ? "ACIK" : "KAPALI")));
        }).dimensions(x, y, COL_W, 20).build());
    }

    private void colorField(int x, int y, String label, String initial, Consumer<String> onValid) {
        labels.add(new Object[]{label, x, y - 10});
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, COL_W, 18, Text.literal(label));
        f.setMaxLength(9);
        f.setText(initial);
        f.setChangedListener(s -> {
            String c = s.trim().replace("#", "");
            if (HEX.matcher(c).matches()) {
                onValid.accept(c);
            }
        });
        addDrawableChild(f);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);
        for (Object[] l : labels) {
            ctx.drawTextWithShadow(textRenderer, (String) l[0], (int) l[1], (int) l[2], 0xFFAAAAAA);
        }
        // onizleme kutusu
        int w = COL_W, h = 48;
        ctx.fill(previewX - 1, previewY - 1, previewX + w + 1, previewY + h + 1, 0xFF000000);
        ctx.fill(previewX, previewY, previewX + w, previewY + h, 0xFF3A5F3A);
        ctx.enableScissor(previewX, previewY, previewX + w, previewY + h);
        CrosshairRenderer.draw(ctx, CrosshairConfig.get(), previewX + w / 2, previewY + h / 2, false, null);
        ctx.disableScissor();
    }

    @Override
    public void close() {
        CrosshairConfig.get().save();
        client.setScreen(parent);
    }

    private static final class IntSlider extends SliderWidget {
        private final String label;
        private final int min, max;
        private final IntConsumer setter;

        IntSlider(int x, int y, int w, int h, String label, int cur, int min, int max, IntConsumer setter) {
            super(x, y, w, h, Text.empty(), (cur - min) / (double) (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        private int current() {
            return min + (int) Math.round(value * (max - min));
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
