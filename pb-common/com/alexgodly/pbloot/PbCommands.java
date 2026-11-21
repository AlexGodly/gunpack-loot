package com.alexgodly.pbloot;

import java.util.function.Consumer;
import java.util.function.Predicate;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** /pbloot: change every setting in game. Changes are saved to the config file. */
public final class PbCommands {
    private PbCommands() {
    }

    /**
     * @param op         who may use the command (operators)
     * @param modeChange called after the loot mode changes, to update the score the data pack reads
     */
    public static void register(CommandDispatcher<CommandSourceStack> d, Predicate<CommandSourceStack> op,
                                Consumer<CommandSourceStack> modeChange) {
        d.register(Commands.literal("pbloot").requires(op)
                .executes(PbCommands::status)
                .then(Commands.literal("status").executes(PbCommands::status))
                .then(Commands.literal("mode")
                        .then(Commands.literal("default").executes(c -> mode(c, PbConfig.Mode.DEFAULT, modeChange)))
                        .then(Commands.literal("equal").executes(c -> mode(c, PbConfig.Mode.EQUAL, modeChange))))
                .then(Commands.literal("gunPackChance").then(Commands.argument("chance", DoubleArgumentType.doubleArg(0, 1))
                        .executes(c -> set(c, () -> PbConfig.get().gunPackChance = DoubleArgumentType.getDouble(c, "chance")))))
                .then(Commands.literal("equalChance").then(Commands.argument("chance", DoubleArgumentType.doubleArg(0, 1))
                        .executes(c -> set(c, () -> PbConfig.get().equalChance = DoubleArgumentType.getDouble(c, "chance")))))
                .then(Commands.literal("equalMaxItems").then(Commands.argument("items", IntegerArgumentType.integer(1, 10))
                        .executes(c -> set(c, () -> PbConfig.get().equalMaxItems = IntegerArgumentType.getInteger(c, "items")))))
                .then(Commands.literal("gunPackAmmo")
                        .then(Commands.literal("on").executes(c -> set(c, () -> PbConfig.get().gunPackAmmo = true)))
                        .then(Commands.literal("off").executes(c -> set(c, () -> PbConfig.get().gunPackAmmo = false))))
                .then(Commands.literal("exclude")
                        .then(Commands.literal("add").then(Commands.argument("pack", StringArgumentType.word())
                                .executes(c -> set(c, () -> {
                                    String p = StringArgumentType.getString(c, "pack");
                                    PbConfig.get().excludedPacks.remove(p);
                                    PbConfig.get().excludedPacks.add(p);
                                }))))
                        .then(Commands.literal("remove").then(Commands.argument("pack", StringArgumentType.word())
                                .executes(c -> set(c, () -> PbConfig.get().excludedPacks.remove(StringArgumentType.getString(c, "pack"))))))));
    }

    private static int mode(CommandContext<CommandSourceStack> c, PbConfig.Mode mode, Consumer<CommandSourceStack> modeChange) {
        PbConfig.get().lootMode = mode;
        PbConfig.save();
        modeChange.accept(c.getSource());
        return status(c);
    }

    private static int set(CommandContext<CommandSourceStack> c, Runnable change) {
        change.run();
        PbConfig.save();
        return status(c);
    }

    private static void say(CommandSourceStack s, Component text) {
        s.sendSuccess(() -> text, false);
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        CommandSourceStack s = c.getSource();
        PbConfig cfg = PbConfig.get();
        say(s, Component.literal("Point Blank: Loot settings").withStyle(ChatFormatting.GOLD));
        say(s, Component.literal(" Loot mode: " + cfg.lootMode));
        say(s, Component.literal(" Gun pack chance: " + cfg.gunPackChance));
        say(s, Component.literal(" Gun pack ammo: " + (cfg.gunPackAmmo ? "on" : "off")));
        say(s, Component.literal(" Equal chance: " + cfg.equalChance + ", up to " + cfg.equalMaxItems + " items"));
        say(s, Component.literal(" Excluded gun packs: " + cfg.excludedPacks));
        return 1;
    }
}
