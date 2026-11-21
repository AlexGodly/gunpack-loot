package com.alexgodly.taczloot;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Config button in the Mods list, using NeoForge's built-in settings screen. */
final class TaczLootClient {
    private TaczLootClient() {
    }

    static void init(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
