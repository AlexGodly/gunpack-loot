package com.alexgodly.pbloot;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds every Point Blank gun, ammo and attachment that is registered, whichever mod or gun pack added it,
 * and puts them in structure chests.
 */
public final class PbLoot {
    public static final String MOD_ID = "point_blank_loot";
    public static final String MODE_OBJECTIVE = "pb_loot_mode";
    static final Logger LOG = LoggerFactory.getLogger("Point Blank: Loot");

    private static Set<String> defaults;
    private static List<Item> guns, everything;
    private static Class<?> gunClass, ammoClass, attachmentClass;
    private static Method compatibleAmmo, initStack;

    private PbLoot() {
    }

    /** The score the data pack reads: 0 = tiered loot, 2 = the mod handles Equal mode itself. */
    public static int modeScore() {
        return PbConfig.get().lootMode == PbConfig.Mode.EQUAL ? 2 : 0;
    }

    private static Set<String> defaults() {
        if (defaults == null) {
            Set<String> s = new HashSet<>();
            try (InputStream in = PbLoot.class.getResourceAsStream("/point_blank_loot_defaults.txt")) {
                if (in != null) {
                    new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                            .map(String::trim).filter(l -> !l.isEmpty()).forEach(s::add);
                }
            } catch (Exception e) {
                LOG.warn("Could not read the default item list", e);
            }
            defaults = s;
        }
        return defaults;
    }

    private static synchronized void scan() {
        if (guns != null) {
            return;
        }
        guns = new ArrayList<>();
        everything = new ArrayList<>();
        try {
            gunClass = Class.forName("com.vicmatskiv.pointblank.item.GunItem");
            ammoClass = Class.forName("com.vicmatskiv.pointblank.item.AmmoItem");
            attachmentClass = Class.forName("com.vicmatskiv.pointblank.item.AttachmentItem");
            try {
                compatibleAmmo = gunClass.getMethod("getCompatibleAmmo");
            } catch (NoSuchMethodException ignored) {
            }
            try {
                initStack = gunClass.getMethod("initStackForCrafting", ItemStack.class);
            } catch (NoSuchMethodException ignored) {
            }
        } catch (ClassNotFoundException e) {
            LOG.warn("Point Blank not found, gun pack loot is off");
            return;
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (gunClass.isInstance(item)) {
                guns.add(item);
                everything.add(item);
            } else if (ammoClass.isInstance(item) || attachmentClass.isInstance(item)) {
                everything.add(item);
            }
        }
        LOG.info("Found {} guns and {} guns, ammo and attachments in total", guns.size(), everything.size());
    }

    private static String id(Item item) {
        var key = BuiltInRegistries.ITEM.getKey(item);
        return key.getNamespace() + ":" + key.getPath();
    }

    private static boolean excluded(Item item) {
        var key = BuiltInRegistries.ITEM.getKey(item);
        return PbConfig.get().excludedPacks.contains(key.getNamespace());
    }

    /** Adds loot to a structure chest that is being filled. */
    public static void fillChest(List<ItemStack> loot, RandomSource random) {
        scan();
        PbConfig cfg = PbConfig.get();
        if (cfg.lootMode == PbConfig.Mode.EQUAL) {
            if (random.nextDouble() >= cfg.equalChance) {
                return;
            }
            List<Item> pool = new ArrayList<>();
            for (Item i : everything) {
                if (!excluded(i)) pool.add(i);
            }
            int n = 1 + random.nextInt(cfg.equalMaxItems);
            for (int k = 0; k < n && !pool.isEmpty(); k++) {
                Item item = pool.get(random.nextInt(pool.size()));
                loot.add(stack(item, ammoClass.isInstance(item) ? 8 + random.nextInt(17) : 1));
            }
            return;
        }
        if (random.nextDouble() >= cfg.gunPackChance) {
            return;
        }
        Set<String> base = defaults();
        List<Item> extra = new ArrayList<>();
        for (Item g : guns) {
            if (!base.contains(id(g)) && !excluded(g)) extra.add(g);
        }
        if (extra.isEmpty()) {
            return;
        }
        Item gun = extra.get(random.nextInt(extra.size()));
        loot.add(stack(gun, 1));
        if (cfg.gunPackAmmo && compatibleAmmo != null) {
            try {
                List<?> ammo = (List<?>) compatibleAmmo.invoke(gun);
                if (ammo != null && !ammo.isEmpty() && ammo.get(random.nextInt(ammo.size())) instanceof Item a) {
                    loot.add(stack(a, 8 + random.nextInt(17)));
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
    }

    private static ItemStack stack(Item item, int count) {
        ItemStack s = new ItemStack(item, Math.min(count, new ItemStack(item).getMaxStackSize()));
        if (initStack != null && gunClass.isInstance(item)) {
            try {
                initStack.invoke(null, s);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return s;
    }
}
