package com.alexgodly.taczloot;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class LootConfig {
    public enum Mode { DEFAULT, EQUAL }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Mode> MODE;
    public static final ModConfigSpec.DoubleValue GUN_PACK_CHANCE;
    public static final ModConfigSpec.DoubleValue EQUAL_CHANCE;
    public static final ModConfigSpec.IntValue EQUAL_MAX_ITEMS;
    public static final ModConfigSpec.BooleanValue GUN_PACK_AMMO;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_PACKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        MODE = b.comment("DEFAULT: tiered loot like before (better guns in harder structures), plus guns from installed gun packs.",
                        "EQUAL: every gun, ammo and attachment from TACZ and all gun packs has the same chance in every chest.")
                .defineEnum("lootMode", Mode.DEFAULT);
        GUN_PACK_CHANCE = b.comment("Default mode: chance that a structure chest also gets one gun from an installed gun pack.")
                .defineInRange("gunPackChance", 0.2, 0.0, 1.0);
        GUN_PACK_AMMO = b.comment("Give gun pack guns some of their own ammo.")
                .define("gunPackAmmo", true);
        EQUAL_CHANCE = b.comment("Equal mode: chance that a structure chest gets TACZ loot.")
                .defineInRange("equalChance", 0.35, 0.0, 1.0);
        EQUAL_MAX_ITEMS = b.comment("Equal mode: most items added to one chest.")
                .defineInRange("equalMaxItems", 2, 1, 10);
        EXCLUDED_PACKS = b.comment("Gun packs (namespaces) that never show up in chests, for example [\"some_pack\"].",
                        "Use \"tacz\" to leave out TACZ's own guns in Equal mode.")
                .defineListAllowEmpty("excludedPacks", List.of(), () -> "", o -> o instanceof String);
        SPEC = b.build();
    }

    private LootConfig() {
    }
}
