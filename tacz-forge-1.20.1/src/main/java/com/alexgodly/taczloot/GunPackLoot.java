package com.alexgodly.taczloot;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/** Adds TACZ guns from every installed gun pack to structure chests. */
public class GunPackLoot extends LootModifier {
    public static final Codec<GunPackLoot> CODEC = Codec.unit(() -> new GunPackLoot(new LootItemCondition[0]));

    public GunPackLoot(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        ResourceLocation table = context.getQueriedLootTableId();
        if (table == null || !table.getPath().startsWith("chests/") || !Tacz.available()) {
            return loot;
        }
        RandomSource random = context.getRandom();
        List<? extends String> excluded = LootConfig.EXCLUDED_PACKS.get();
        if (LootConfig.MODE.get() == LootConfig.Mode.EQUAL) {
            if (random.nextDouble() >= LootConfig.EQUAL_CHANCE.get()) {
                return loot;
            }
            List<Tacz.Entry> all = new ArrayList<>();
            for (Tacz.Entry e : Tacz.everything()) {
                if (!excluded.contains(e.id().getNamespace())) {
                    all.add(e);
                }
            }
            int count = 1 + random.nextInt(LootConfig.EQUAL_MAX_ITEMS.get());
            for (int i = 0; i < count && !all.isEmpty(); i++) {
                ItemStack stack = all.get(random.nextInt(all.size())).make(random);
                if (!stack.isEmpty()) {
                    loot.add(stack);
                }
            }
            return loot;
        }
        if (random.nextDouble() >= LootConfig.GUN_PACK_CHANCE.get()) {
            return loot;
        }
        List<ResourceLocation> guns = new ArrayList<>();
        for (ResourceLocation id : Tacz.gunIds()) {
            if (!"tacz".equals(id.getNamespace()) && !excluded.contains(id.getNamespace())) {
                guns.add(id);
            }
        }
        if (guns.isEmpty()) {
            return loot;
        }
        ResourceLocation gun = guns.get(random.nextInt(guns.size()));
        ItemStack stack = Tacz.gun(gun);
        if (!stack.isEmpty()) {
            loot.add(stack);
            if (LootConfig.GUN_PACK_AMMO.get()) {
                ItemStack ammo = Tacz.ammoFor(gun, 8 + random.nextInt(17));
                if (!ammo.isEmpty()) {
                    loot.add(ammo);
                }
            }
        }
        return loot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
