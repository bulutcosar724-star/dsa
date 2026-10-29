package com.example.customcrosshair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CrosshairConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("customcrosshair.json");
    private static CrosshairConfig instance;

    public enum Style {
        CROSS("Arti (+)"), DOT("Nokta"), CIRCLE("Daire"), CHEVRON("Ok (^)");

        public final String label;

        Style(String label) {
            this.label = label;
        }

        public Style next() {
            Style[] v = values();
            return v[(ordinal() + 1) % v.length];
        }
    }

    // Renkler AARRGGBB hex (ornek: "FFFFFFFF" beyaz, "80FF0000" yari saydam kirmizi)
    public boolean enabled = true;
    public Style style = Style.CROSS;
    public String color = "FFFFFFFF";
    public int size = 5;            // kol uzunlugu / daire yaricapi / ok uzunlugu
    public int thickness = 1;       // cizgi kalinligi
    public int gap = 3;             // merkezden bosluk (sadece arti)
    public boolean outline = true;
    public String outlineColor = "FF000000";
    public int outlineThickness = 1;
    public boolean centerDot = false;
    public int dotSize = 1;
    public boolean tShape = false;  // sadece arti: ustteki kolu kaldirir
    public boolean hideInThirdPerson = false;

    // Hedefe bakinca renk degisimi
    public boolean entityColorEnabled = true;
    public String entityColor = "FFFF0000";
    public boolean targetOnlyLiving = true; // sadece oyuncu/mob/NPC gibi canlilar

    public transient int colorInt = 0xFFFFFFFF;
    public transient int outlineColorInt = 0xFF000000;
    public transient int entityColorInt = 0xFFFF0000;

    public static CrosshairConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        CrosshairConfig cfg = null;
        try {
            if (Files.exists(PATH)) {
                cfg = GSON.fromJson(Files.readString(PATH), CrosshairConfig.class);
            }
        } catch (Exception e) {
            System.err.println("[CustomCrosshair] Config okunamadi, varsayilan kullaniliyor: " + e);
        }
        if (cfg == null) cfg = new CrosshairConfig();
        cfg.normalize();
        instance = cfg;
        save();
    }

    public static void reset() {
        CrosshairConfig cfg = new CrosshairConfig();
        cfg.normalize();
        instance = cfg;
        save();
    }

    public void normalize() {
        if (style == null) style = Style.CROSS;
        size = Math.max(0, size);
        thickness = Math.max(1, thickness);
        gap = Math.max(0, gap);
        outlineThickness = Math.max(0, outlineThickness);
        dotSize = Math.max(1, dotSize);
        if (color == null) color = "FFFFFFFF";
        if (outlineColor == null) outlineColor = "FF000000";
        if (entityColor == null) entityColor = "FFFF0000";
        colorInt = parse(color, 0xFFFFFFFF);
        outlineColorInt = parse(outlineColor, 0xFF000000);
        entityColorInt = parse(entityColor, 0xFFFF0000);
    }

    public static void save() {
        try {
            Files.writeString(PATH, GSON.toJson(instance));
        } catch (IOException e) {
            System.err.println("[CustomCrosshair] Config yazilamadi: " + e);
        }
    }

    public static int parse(String s, int fallback) {
        try {
            String h = s.replace("#", "").trim();
            if (h.length() == 6) h = "FF" + h;
            return (int) Long.parseLong(h, 16);
        } catch (Exception e) {
            return fallback;
        }
    }
}
