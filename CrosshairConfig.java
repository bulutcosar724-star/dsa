package com.example.customcrosshair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CrosshairConfig {
    public enum Style {
        CROSS, DOT, CIRCLE,
        @SerializedName(value = "ARROW", alternate = {"CHEVRON"}) ARROW, // eski adi CHEVRON da okunur
        CUSTOM
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("customcrosshair.json");
    private static CrosshairConfig instance = new CrosshairConfig();

    public boolean enabled = true;
    public Style style = Style.CROSS;
    public String color = "FFFFFFFF";
    public int size = 5;
    public int thickness = 1;
    public int gap = 3;
    public boolean outline = true;
    public String outlineColor = "FF000000";
    public int outlineThickness = 1;
    public boolean centerDot = false;
    public int dotSize = 1;
    public boolean tShape = false;
    public boolean hideInThirdPerson = true;
    public int angle = 45;
    public boolean entityColorEnabled = true;
    public String entityColor = "FFFF0000";
    public boolean targetOnlyLiving = true;
    // Orijinal (vanilla) renk: arka plani ters cevirir, renk ayari yok sayilir
    public boolean vanillaColor = false;

    // Kendi cizdigin crosshair (customGrid x customGrid, '1' = dolu piksel)
    public int customGrid = 11;
    public int customScale = 1;
    public String customPixels = "";

    public static CrosshairConfig get() {
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                CrosshairConfig c = GSON.fromJson(Files.readString(PATH), CrosshairConfig.class);
                if (c != null) {
                    instance = c;
                }
            }
        } catch (Exception e) {
            System.err.println("[CustomCrosshair] Config okunamadi, varsayilan kullaniliyor: " + e);
            instance = new CrosshairConfig();
        }
        instance.normalize();
        instance.save();
    }

    public static void reset() {
        instance = new CrosshairConfig();
        instance.normalize();
    }

    public void save() {
        normalize();
        try {
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("[CustomCrosshair] Config yazilamadi: " + e);
        }
    }

    public void normalize() {
        if (style == null) style = Style.CROSS;
        size = clamp(size, 1, 30);
        thickness = clamp(thickness, 1, 10);
        gap = clamp(gap, 0, 20);
        outlineThickness = clamp(outlineThickness, 0, 4);
        dotSize = clamp(dotSize, 1, 10);
        angle = clamp(angle, 10, 80);
        customScale = clamp(customScale, 1, 4);
        customGrid = clamp(customGrid, 5, 31);
        if (customGrid % 2 == 0) customGrid++;
        if (customGrid > 31) customGrid = 31;
        if (customPixels == null || customPixels.length() != customGrid * customGrid) {
            setPixels(plus(customGrid));
        }
    }

    // ---- renkler (AARRGGBB veya RRGGBB) ----
    public int colorInt() { return parse(color, 0xFFFFFFFF); }
    public int outlineColorInt() { return parse(outlineColor, 0xFF000000); }
    public int entityColorInt() { return parse(entityColor, 0xFFFF0000); }

    public static int parse(String s, int fallback) {
        if (s == null) return fallback;
        s = s.trim().replace("#", "");
        if (s.length() == 6) s = "FF" + s;
        if (s.length() != 8) return fallback;
        try {
            return (int) Long.parseLong(s, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ---- ozel piksel verisi ----
    public boolean[] pixels() {
        int len = customGrid * customGrid;
        if (customPixels == null || customPixels.length() != len) {
            return plus(customGrid);
        }
        boolean[] r = new boolean[len];
        for (int i = 0; i < len; i++) {
            r[i] = customPixels.charAt(i) == '1';
        }
        return r;
    }

    public void setPixels(boolean[] p) {
        StringBuilder sb = new StringBuilder(p.length);
        for (boolean b : p) sb.append(b ? '1' : '0');
        customPixels = sb.toString();
    }

    public static boolean[] plus(int n) {
        boolean[] r = new boolean[n * n];
        int c = n / 2;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                boolean onRow = (y == c && Math.abs(x - c) >= 2);
                boolean onCol = (x == c && Math.abs(y - c) >= 2);
                r[y * n + x] = onRow || onCol;
            }
        }
        return r;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
