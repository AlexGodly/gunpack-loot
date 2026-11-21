package com.alexgodly.pbloot.fabric.mixin;

import com.alexgodly.pbloot.PbLoot;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds Point Blank loot when a structure chest is filled. */
@Mixin(LootTable.class)
public abstract class LootTableMixin {
    @Inject(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at = @At("RETURN"))
    private void pointBlankLoot$addLoot(LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        if (((LootTable) (Object) this).getParamSet() == LootContextParamSets.CHEST) {
            PbLoot.fillChest(cir.getReturnValue(), context.getRandom());
        }
    }
}
