package com.alexgodly.taczloot;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod(TaczLoot.MOD_ID)
public class TaczLoot {
    public static final String MOD_ID = "tacz_loot";
    static final String MODE_OBJECTIVE = "tacz_loot_mode";

    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);

    static {
        MODIFIERS.register("gun_packs", () -> GunPackLoot.CODEC);
    }

    public TaczLoot() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        MODIFIERS.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, LootConfig.SPEC);
        modBus.addListener((ModConfigEvent.Reloading e) -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.execute(() -> syncMode(server));
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent e) -> syncMode(e.getServer()));
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> LootCommand.register(e.getDispatcher()));
        if (FMLEnvironment.dist == Dist.CLIENT && ModList.get().isLoaded("cloth_config")) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> ConfigScreen.create(parent)));
        }
    }

    /** The data pack loot reads this score: 0 = tiered loot, 2 = the mod handles Equal mode itself. */
    static void syncMode(MinecraftServer server) {
        Scoreboard board = server.getScoreboard();
        Objective objective = board.getObjective(MODE_OBJECTIVE);
        if (objective == null) {
            objective = board.addObjective(MODE_OBJECTIVE, ObjectiveCriteria.DUMMY,
                    Component.literal(MODE_OBJECTIVE), ObjectiveCriteria.RenderType.INTEGER);
        }
        int mode = LootConfig.MODE.get() == LootConfig.Mode.EQUAL ? 2 : 0;
        board.getOrCreatePlayerScore("#mode", objective).setScore(mode);
    }
}
