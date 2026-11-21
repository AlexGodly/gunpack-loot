package com.alexgodly.pbloot;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Settings, saved to config/point_blank_loot.json. */
public final class PbConfig {
    public enum Mode { DEFAULT, EQUAL }

    public Mode lootMode = Mode.DEFAULT;
    public double gunPackChance = 0.2;
    public boolean gunPackAmmo = true;
    public double equalChance = 0.35;
    public int equalMaxItems = 2;
    public List<String> excludedPacks = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PbConfig current = new PbConfig();
    private static Path file;
    /** Set by the loader code: runs after every save (updates the score the data pack reads). */
    public static Runnable onSave = () -> { };

    public static PbConfig get() {
        return current;
    }

    public static void load(Path configDir) {
        file = configDir.resolve("point_blank_loot.json");
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    PbConfig c = GSON.fromJson(r, PbConfig.class);
                    if (c != null) {
                        current = c;
                    }
                }
            }
        } catch (Exception e) {
            PbLoot.LOG.warn("Point Blank: Loot could not read its config, using defaults", e);
        }
        current.clean();
        save();
    }

    public static void save() {
        if (file == null) {
            return;
        }
        current.clean();
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(current, w);
            }
            onSave.run();
        } catch (Exception e) {
            PbLoot.LOG.warn("Point Blank: Loot could not save its config", e);
        }
    }

    private void clean() {
        if (lootMode == null) lootMode = Mode.DEFAULT;
        if (excludedPacks == null) excludedPacks = new ArrayList<>();
        gunPackChance = Math.max(0, Math.min(1, gunPackChance));
        equalChance = Math.max(0, Math.min(1, equalChance));
        equalMaxItems = Math.max(1, Math.min(10, equalMaxItems));
    }
}
