package com.alexgodly.pbloot.fabric;

import com.alexgodly.pbloot.PbCommands;
import com.alexgodly.pbloot.PbConfig;
import com.alexgodly.pbloot.ScoreSync;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;

public class PbLootFabric implements ModInitializer {
    private static MinecraftServer server;

    @Override
    public void onInitialize() {
        PbConfig.onSave = () -> {
            MinecraftServer s = server;
            if (s != null) {
                s.execute(() -> ScoreSync.sync(s));
            }
        };
        PbConfig.load(FabricLoader.getInstance().getConfigDir());
        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s;
            ScoreSync.sync(s);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, env) -> PbCommands.register(dispatcher,
                Commands.hasPermission(Commands.LEVEL_GAMEMASTERS), s -> ScoreSync.sync(s.getServer())));
    }
}
