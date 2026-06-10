package com.example.examplemod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class WaterMonsterAltarProtection {
    private WaterMonsterAltarProtection() {
    }

    public static void alertDefenders(World world, BlockPos pos, PlayerEntity player) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        for (WaterMonsterEntity monster : findBoundMonsters(serverWorld, pos)) {
            monster.defendAltarAgainst(player);
        }
    }

    public static void onPlayerBrokenAltarBlock(World world, PlayerEntity player, BlockPos pos) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        for (WaterMonsterEntity monster : findBoundMonsters(serverWorld, pos)) {
            monster.defendAltarAgainst(player);
            monster.onAltarBlockBroken(pos);
        }
    }

    public static void removeProtectedBlocks(ServerWorld world, List<BlockPos> positions) {
        if (positions.isEmpty()) return;

        Set<BlockPos> protectedBlocks = collectProtectedBlocks(world);
        if (protectedBlocks.isEmpty()) return;

        positions.removeIf(protectedBlocks::contains);
    }

    private static List<WaterMonsterEntity> findBoundMonsters(ServerWorld world, BlockPos pos) {
        List<WaterMonsterEntity> monsters = new ArrayList<>();
        for (WaterMonsterEntity monster : world.getEntitiesByType(
                TypeFilter.instanceOf(WaterMonsterEntity.class),
                monster -> monster.isBoundAltarBlock(pos)
        )) {
            monsters.add(monster);
        }
        return monsters;
    }

    private static Set<BlockPos> collectProtectedBlocks(ServerWorld world) {
        Set<BlockPos> protectedBlocks = new HashSet<>();
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof WaterMonsterEntity monster) {
                protectedBlocks.addAll(monster.getProtectedAltarBlocks());
            }
        }
        return protectedBlocks;
    }
}
