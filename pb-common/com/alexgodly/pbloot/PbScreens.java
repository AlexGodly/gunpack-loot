package com.alexgodly.pbloot;

import java.util.ArrayList;
import java.util.List;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Settings screen (Mod Menu on Fabric, Config button in the Mods list on Forge/NeoForge). Needs Cloth Config. */
public final class PbScreens {
    private PbScreens() {
    }

    public static Screen create(Screen parent) {
        PbConfig cfg = PbConfig.get();
        ConfigBuilder b = ConfigBuilder.create().setParentScreen(parent)
                .setTitle(Component.literal("Point Blank: Loot")).setSavingRunnable(PbConfig::save);
        ConfigEntryBuilder eb = b.entryBuilder();
        ConfigCategory c = b.getOrCreateCategory(Component.literal("Loot"));
        c.addEntry(eb.startEnumSelector(Component.literal("Loot mode"), PbConfig.Mode.class, cfg.lootMode)
                .setDefaultValue(PbConfig.Mode.DEFAULT)
                .setEnumNameProvider(m -> Component.literal(m == PbConfig.Mode.DEFAULT ? "Default (tiered)" : "Equal (same chance everywhere)"))
                .setTooltip(Component.literal("Default: better guns in harder structures, plus gun pack guns."),
                        Component.literal("Equal: every gun, ammo and attachment has the same chance in every chest."))
                .setSaveConsumer(v -> cfg.lootMode = v).build());
        c.addEntry(eb.startDoubleField(Component.literal("Gun pack chance (Default mode)"), cfg.gunPackChance)
                .setDefaultValue(0.2).setMin(0).setMax(1)
                .setTooltip(Component.literal("Chance that a chest also gets a gun from an installed gun pack."))
                .setSaveConsumer(v -> cfg.gunPackChance = v).build());
        c.addEntry(eb.startBooleanToggle(Component.literal("Ammo with gun pack guns"), cfg.gunPackAmmo)
                .setDefaultValue(true).setSaveConsumer(v -> cfg.gunPackAmmo = v).build());
        c.addEntry(eb.startDoubleField(Component.literal("Chest chance (Equal mode)"), cfg.equalChance)
                .setDefaultValue(0.35).setMin(0).setMax(1).setSaveConsumer(v -> cfg.equalChance = v).build());
        c.addEntry(eb.startIntSlider(Component.literal("Most items per chest (Equal mode)"), cfg.equalMaxItems, 1, 10)
                .setDefaultValue(2).setSaveConsumer(v -> cfg.equalMaxItems = v).build());
        c.addEntry(eb.startStrList(Component.literal("Excluded gun packs"), new ArrayList<>(cfg.excludedPacks))
                .setDefaultValue(List.of())
                .setTooltip(Component.literal("Mod ids (namespaces) of gun packs that never show up in chests."))
                .setSaveConsumer(v -> cfg.excludedPacks = new ArrayList<>(v)).build());
        return b.build();
    }
}
