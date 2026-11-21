package com.alexgodly.pbloot;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public final class ScoreSync {
    private ScoreSync() {
    }

    public static void sync(MinecraftServer server) {
        Scoreboard board = server.getScoreboard();
        Objective o = board.getObjective(PbLoot.MODE_OBJECTIVE);
        if (o == null) {
            o = board.addObjective(PbLoot.MODE_OBJECTIVE, ObjectiveCriteria.DUMMY, Component.literal(PbLoot.MODE_OBJECTIVE),
                    ObjectiveCriteria.RenderType.INTEGER);
        }
        board.getOrCreatePlayerScore("#mode", o).setScore(PbLoot.modeScore());
    }
}
