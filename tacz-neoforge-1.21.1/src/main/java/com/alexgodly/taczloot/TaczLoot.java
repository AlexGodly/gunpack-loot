package com.alexgodly.taczloot;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@Mod(TaczLoot.MOD_ID)
public class TaczLoot {
    public static final String MOD_ID = "tacz_loot";
    static final String MODE_OBJECTIVE = "tacz_loot_mode";

    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);

    static {
        MODIFIERS.register("gun_packs", () -> GunPackLoot.CODEC);
    }

    public TaczLoot(IEventBus modBus, ModContainer container) {
        MODIFIERS.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, LootConfig.SPEC);
        modBus.addListener((ModConfigEvent.Reloading e) -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.execute(() -> syncMode(server));
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> syncMode(e.getServer()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> LootCommand.register(e.getDispatcher()));
        if (FMLEnvironment.dist.isClient()) {
            TaczLootClient.init(container);
        }
    }

    /** The data pack loot reads this score: 0 = tiered loot, 2 = the mod handles Equal mode itself. */
    static void syncMode(MinecraftServer server) {
        Scoreboard board = server.getScoreboard();
        Objective objective = board.getObjective(MODE_OBJECTIVE);
        if (objective == null) {
            objective = board.addObjective(MODE_OBJECTIVE, ObjectiveCriteria.DUMMY, Component.literal(MODE_OBJECTIVE),
                    ObjectiveCriteria.RenderType.INTEGER, false, null);
        }
        int mode = LootConfig.MODE.get() == LootConfig.Mode.EQUAL ? 2 : 0;
        board.getOrCreatePlayerScore(ScoreHolder.forNameOnly("#mode"), objective).set(mode);
    }
}
