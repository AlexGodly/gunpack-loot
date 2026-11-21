package com.alexgodly.taczloot;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * Talks to TACZ through its public API classes, looked up at runtime so this mod works with any TACZ 1.x build.
 * TACZ lists every gun, ammo and attachment it has loaded, including the ones from gun packs.
 */
final class Tacz {
    private static final Logger LOG = LogUtils.getLogger();
    private static boolean checked;
    private static boolean ok;
    private static Method allGuns, allAmmo, allAttachments, gunIndex;

    record Entry(ResourceLocation id, String kind) {
        ItemStack make(RandomSource random) {
            return switch (kind) {
                case "gun" -> gun(id);
                case "ammo" -> ammo(id, 8 + random.nextInt(17));
                default -> attachment(id);
            };
        }
    }

    private Tacz() {
    }

    static synchronized boolean available() {
        if (!checked) {
            checked = true;
            try {
                Class<?> api = Class.forName("com.tacz.guns.api.TimelessAPI");
                allGuns = api.getMethod("getAllCommonGunIndex");
                allAmmo = api.getMethod("getAllCommonAmmoIndex");
                allAttachments = api.getMethod("getAllCommonAttachmentIndex");
                gunIndex = api.getMethod("getCommonGunIndex", ResourceLocation.class);
                ok = true;
            } catch (ReflectiveOperationException e) {
                LOG.warn("TACZ: Loot could not find the TACZ API, gun pack loot is off", e);
            }
        }
        return ok;
    }

    private static List<ResourceLocation> ids(Method m) {
        List<ResourceLocation> out = new ArrayList<>();
        try {
            for (Object o : (Collection<?>) m.invoke(null)) {
                if (o instanceof Map.Entry<?, ?> e && e.getKey() instanceof ResourceLocation id) {
                    out.add(id);
                }
            }
        } catch (ReflectiveOperationException | ClassCastException e) {
            LOG.warn("TACZ: Loot could not read the TACZ list", e);
        }
        return out;
    }

    static List<ResourceLocation> gunIds() {
        return ids(allGuns);
    }

    static List<Entry> everything() {
        List<Entry> out = new ArrayList<>();
        for (ResourceLocation id : ids(allGuns)) out.add(new Entry(id, "gun"));
        for (ResourceLocation id : ids(allAmmo)) out.add(new Entry(id, "ammo"));
        for (ResourceLocation id : ids(allAttachments)) out.add(new Entry(id, "attachment"));
        return out;
    }

    private static ItemStack stack(String item, String key, ResourceLocation id, int count) {
        Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation("tacz", item));
        if (it == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(it, count);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(key, id.toString());
        return stack;
    }

    static ItemStack gun(ResourceLocation id) {
        ItemStack built = build("com.tacz.guns.api.item.builder.GunItemBuilder", id, 1);
        if (!built.isEmpty()) {
            return built;
        }
        ItemStack stack = stack("modern_kinetic_gun", "GunId", id, 1);
        if (!stack.isEmpty()) {
            stack.getOrCreateTag().putString("GunFireMode", "SEMI");
        }
        return stack;
    }

    static ItemStack ammo(ResourceLocation id, int count) {
        ItemStack built = build("com.tacz.guns.api.item.builder.AmmoItemBuilder", id, count);
        return built.isEmpty() ? stack("ammo", "AmmoId", id, count) : built;
    }

    static ItemStack attachment(ResourceLocation id) {
        ItemStack built = build("com.tacz.guns.api.item.builder.AttachmentItemBuilder", id, 1);
        return built.isEmpty() ? stack("attachment", "AttachmentId", id, 1) : built;
    }

    /** TACZ's own item builders fill in everything a fresh item needs (fire mode, ammo count and so on). */
    private static ItemStack build(String builderClass, ResourceLocation id, int count) {
        try {
            Class<?> c = Class.forName(builderClass);
            Object b = c.getMethod("create").invoke(null);
            b = c.getMethod("setId", ResourceLocation.class).invoke(b, id);
            if (count > 1) {
                try {
                    b = c.getMethod("setCount", int.class).invoke(b, count);
                } catch (NoSuchMethodException ignored) {
                }
            }
            Object out = c.getMethod("build").invoke(b);
            if (out instanceof ItemStack s) {
                if (count > 1 && s.getCount() != count) {
                    s.setCount(count);
                }
                return s;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return ItemStack.EMPTY;
    }

    /** The ammo a gun uses, read from its gun data. */
    static ItemStack ammoFor(ResourceLocation gun, int count) {
        try {
            Object opt = gunIndex.invoke(null, gun);
            Object index = opt instanceof java.util.Optional<?> o ? o.orElse(null) : opt;
            if (index == null) {
                return ItemStack.EMPTY;
            }
            Object data = index.getClass().getMethod("getGunData").invoke(index);
            Object ammoId = data.getClass().getMethod("getAmmoId").invoke(data);
            if (ammoId instanceof ResourceLocation id) {
                return ammo(id, count);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return ItemStack.EMPTY;
    }
}
