package com.alexgodly.pbloot.neoforge;

import com.alexgodly.pbloot.PbLoot;
import com.alexgodly.pbloot.PbScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = PbLoot.MOD_ID, dist = Dist.CLIENT)
public class PbLootNeoForgeClient {
    public PbLootNeoForgeClient(IEventBus modBus, ModContainer container) {
        if (ModList.get().isLoaded("cloth_config")) {
            container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> PbScreens.create(parent));
        }
    }
}
