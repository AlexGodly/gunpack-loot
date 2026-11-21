package com.alexgodly.pbloot.neoforge;

import com.alexgodly.pbloot.PbCommands;
import com.alexgodly.pbloot.PbConfig;
import com.alexgodly.pbloot.PbLoot;
import com.alexgodly.pbloot.ScoreSync;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@Mod(PbLoot.MOD_ID)
public class PbLootNeoForge {
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, PbLoot.MOD_ID);

    static {
        MODIFIERS.register("chests", () -> ChestLoot.CODEC);
    }

    public PbLootNeoForge(IEventBus modBus, ModContainer container) {
        MODIFIERS.register(modBus);
        PbConfig.onSave = () -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.execute(() -> ScoreSync.sync(server));
            }
        };
        PbConfig.load(FMLPaths.CONFIGDIR.get());
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, e -> ScoreSync.sync(e.getServer()));
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e -> PbCommands.register(e.getDispatcher(),
                Commands.hasPermission(Commands.LEVEL_GAMEMASTERS), s -> ScoreSync.sync(s.getServer())));
    }

    /** Adds Point Blank loot to every structure chest. */
    public static class ChestLoot extends LootModifier {
        static final MapCodec<ChestLoot> CODEC = MapCodec.unit(() -> new ChestLoot(new LootItemCondition[0]));

        ChestLoot(LootItemCondition[] conditions) {
            super(conditions, 0);
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
            var id = context.getQueriedLootTableId();
            if (id != null && id.getPath().startsWith("chests/")) {
                PbLoot.fillChest(loot, context.getRandom());
            }
            return loot;
        }

        @Override
        public MapCodec<? extends IGlobalLootModifier> codec() {
            return CODEC;
        }
    }
}
