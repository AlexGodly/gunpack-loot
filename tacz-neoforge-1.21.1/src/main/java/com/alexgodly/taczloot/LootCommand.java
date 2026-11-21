package com.alexgodly.taczloot;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** /taczloot: change every setting in game. Changes are saved to the config file. */
final class LootCommand {
    private LootCommand() {
    }

    static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("taczloot")
                .requires(s -> s.hasPermission(2))
                .executes(LootCommand::status)
                .then(Commands.literal("status").executes(LootCommand::status))
                .then(Commands.literal("mode")
                        .then(Commands.literal("default").executes(c -> mode(c, LootConfig.Mode.DEFAULT)))
                        .then(Commands.literal("equal").executes(c -> mode(c, LootConfig.Mode.EQUAL))))
                .then(Commands.literal("gunPackChance")
                        .then(Commands.argument("chance", DoubleArgumentType.doubleArg(0, 1))
                                .executes(c -> {
                                    LootConfig.GUN_PACK_CHANCE.set(DoubleArgumentType.getDouble(c, "chance"));
                                    return saved(c);
                                })))
                .then(Commands.literal("equalChance")
                        .then(Commands.argument("chance", DoubleArgumentType.doubleArg(0, 1))
                                .executes(c -> {
                                    LootConfig.EQUAL_CHANCE.set(DoubleArgumentType.getDouble(c, "chance"));
                                    return saved(c);
                                })))
                .then(Commands.literal("equalMaxItems")
                        .then(Commands.argument("items", IntegerArgumentType.integer(1, 10))
                                .executes(c -> {
                                    LootConfig.EQUAL_MAX_ITEMS.set(IntegerArgumentType.getInteger(c, "items"));
                                    return saved(c);
                                })))
                .then(Commands.literal("gunPackAmmo")
                        .then(Commands.literal("on").executes(c -> {
                            LootConfig.GUN_PACK_AMMO.set(true);
                            return saved(c);
                        }))
                        .then(Commands.literal("off").executes(c -> {
                            LootConfig.GUN_PACK_AMMO.set(false);
                            return saved(c);
                        })))
                .then(Commands.literal("exclude")
                        .then(Commands.literal("add").then(Commands.argument("pack", StringArgumentType.word())
                                .executes(c -> exclude(c, StringArgumentType.getString(c, "pack"), true))))
                        .then(Commands.literal("remove").then(Commands.argument("pack", StringArgumentType.word())
                                .executes(c -> exclude(c, StringArgumentType.getString(c, "pack"), false))))));
    }

    private static int mode(CommandContext<CommandSourceStack> c, LootConfig.Mode mode) {
        LootConfig.MODE.set(mode);
        TaczLoot.syncMode(c.getSource().getServer());
        return saved(c);
    }

    private static int exclude(CommandContext<CommandSourceStack> c, String pack, boolean add) {
        List<String> list = new ArrayList<>(LootConfig.EXCLUDED_PACKS.get());
        list.remove(pack);
        if (add) {
            list.add(pack);
        }
        LootConfig.EXCLUDED_PACKS.set(list);
        return saved(c);
    }

    private static int saved(CommandContext<CommandSourceStack> c) {
        LootConfig.MODE.save();
        return status(c);
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        CommandSourceStack s = c.getSource();
        s.sendSuccess(() -> Component.literal("TACZ: Loot settings").withStyle(ChatFormatting.GOLD), false);
        s.sendSuccess(() -> Component.literal(" Loot mode: " + LootConfig.MODE.get()), false);
        s.sendSuccess(() -> Component.literal(" Gun pack chance: " + LootConfig.GUN_PACK_CHANCE.get()), false);
        s.sendSuccess(() -> Component.literal(" Gun pack ammo: " + (LootConfig.GUN_PACK_AMMO.get() ? "on" : "off")), false);
        s.sendSuccess(() -> Component.literal(" Equal chance: " + LootConfig.EQUAL_CHANCE.get()
                + ", up to " + LootConfig.EQUAL_MAX_ITEMS.get() + " items"), false);
        s.sendSuccess(() -> Component.literal(" Excluded gun packs: " + LootConfig.EXCLUDED_PACKS.get()), false);
        return 1;
    }
}
