package com.alexgodly.pbloot.forge;

import com.alexgodly.pbloot.PbCommands;
import com.alexgodly.pbloot.PbConfig;
import com.alexgodly.pbloot.PbLoot;
import com.alexgodly.pbloot.PbScreens;
import com.alexgodly.pbloot.ScoreSync;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod(PbLoot.MOD_ID)
public class PbLootForge {
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, PbLoot.MOD_ID);

    static {
        MODIFIERS.register("chests", () -> ChestLoot.CODEC);
    }

    public PbLootForge() {
        MODIFIERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        PbConfig.onSave = () -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.execute(() -> ScoreSync.sync(server));
            }
        };
        PbConfig.load(FMLPaths.CONFIGDIR.get());
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent e) -> ScoreSync.sync(e.getServer()));
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> PbCommands.register(e.getDispatcher(),
                s -> s.hasPermission(2), s -> ScoreSync.sync(s.getServer())));
        if (FMLEnvironment.dist == Dist.CLIENT && ModList.get().isLoaded("cloth_config")) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> PbScreens.create(parent)));
        }
    }

    /** Adds Point Blank loot to every structure chest. */
    public static class ChestLoot extends LootModifier {
        static final Codec<ChestLoot> CODEC = Codec.unit(() -> new ChestLoot(new LootItemCondition[0]));

        ChestLoot(LootItemCondition[] conditions) {
            super(conditions);
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
            ResourceLocation id = context.getQueriedLootTableId();
            if (id != null && id.getPath().startsWith("chests/")) {
                PbLoot.fillChest(loot, context.getRandom());
            }
            return loot;
        }

        @Override
        public Codec<? extends IGlobalLootModifier> codec() {
            return CODEC;
        }
    }
}
