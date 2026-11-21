package com.alexgodly.taczloot;

import java.util.ArrayList;
import java.util.List;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Settings screen behind the Config button in the Mods list (needs Cloth Config). */
final class ConfigScreen {
    private ConfigScreen() {
    }

    static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent)
                .setTitle(Component.literal("TACZ: Loot"))
                .setSavingRunnable(LootConfig.SPEC::save);
        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory c = builder.getOrCreateCategory(Component.literal("Loot"));
        c.addEntry(eb.startEnumSelector(Component.literal("Loot mode"), LootConfig.Mode.class, LootConfig.MODE.get())
                .setDefaultValue(LootConfig.Mode.DEFAULT)
                .setEnumNameProvider(m -> Component.literal(m == LootConfig.Mode.DEFAULT ? "Default (tiered)" : "Equal (same chance everywhere)"))
                .setTooltip(Component.literal("Default: better guns in harder structures, plus gun pack guns."),
                        Component.literal("Equal: every gun, ammo and attachment has the same chance in every chest."))
                .setSaveConsumer(LootConfig.MODE::set).build());
        c.addEntry(eb.startDoubleField(Component.literal("Gun pack chance (Default mode)"), LootConfig.GUN_PACK_CHANCE.get())
                .setDefaultValue(0.2).setMin(0).setMax(1)
                .setTooltip(Component.literal("Chance that a chest also gets a gun from an installed gun pack."))
                .setSaveConsumer(LootConfig.GUN_PACK_CHANCE::set).build());
        c.addEntry(eb.startBooleanToggle(Component.literal("Ammo with gun pack guns"), LootConfig.GUN_PACK_AMMO.get())
                .setDefaultValue(true).setSaveConsumer(LootConfig.GUN_PACK_AMMO::set).build());
        c.addEntry(eb.startDoubleField(Component.literal("Chest chance (Equal mode)"), LootConfig.EQUAL_CHANCE.get())
                .setDefaultValue(0.35).setMin(0).setMax(1)
                .setSaveConsumer(LootConfig.EQUAL_CHANCE::set).build());
        c.addEntry(eb.startIntSlider(Component.literal("Most items per chest (Equal mode)"), LootConfig.EQUAL_MAX_ITEMS.get(), 1, 10)
                .setDefaultValue(2).setSaveConsumer(LootConfig.EQUAL_MAX_ITEMS::set).build());
        c.addEntry(eb.startStrList(Component.literal("Excluded gun packs"), new ArrayList<>(LootConfig.EXCLUDED_PACKS.get()))
                .setDefaultValue(List.of())
                .setTooltip(Component.literal("Gun pack namespaces that never show up in chests."))
                .setSaveConsumer(LootConfig.EXCLUDED_PACKS::set).build());
        return builder.build();
    }
}
