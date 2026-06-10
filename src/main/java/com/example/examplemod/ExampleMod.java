package com.example.examplemod;

import com.example.examplemod.entity.NoBlockDamageTntEntity;
import com.example.examplemod.entity.WaterMonsterEntity;
import com.example.examplemod.item.TntFishingRodItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "examplemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Item EXAMPLE_ITEM = new Item(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "example_item"))));
    public static final Item TNT_FISHING_ROD = new TntFishingRodItem(new Item.Settings()
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "tnt_fishing_rod")))
            .maxCount(1)
            .maxDamage(64)
            .rarity(Rarity.RARE)
    );
    public static final RegistryKey<ItemGroup> EXAMPLE_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, "example_group"));

    private static final RegistryKey<EntityType<?>> WATER_MONSTER_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "water_monster"));
    private static final RegistryKey<EntityType<?>> NO_BLOCK_DAMAGE_TNT_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "no_block_damage_tnt"));

    public static final EntityType<WaterMonsterEntity> WATER_MONSTER = Registry.register(
            Registries.ENTITY_TYPE,
            WATER_MONSTER_KEY,
            EntityType.Builder.create(WaterMonsterEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.8f, 1.8f)
                    .maxTrackingRange(10)
                    .trackingTickInterval(3)
                    .build(WATER_MONSTER_KEY)
    );

    public static final EntityType<NoBlockDamageTntEntity> NO_BLOCK_DAMAGE_TNT = Registry.register(
            Registries.ENTITY_TYPE,
            NO_BLOCK_DAMAGE_TNT_KEY,
            EntityType.Builder.create(NoBlockDamageTntEntity::new, SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f)
                    .maxTrackingRange(10)
                    .trackingTickInterval(10)
                    .build(NO_BLOCK_DAMAGE_TNT_KEY)
    );

    @Override
    public void onInitialize() {
        LOGGER.info("ExampleMod initializing!");

        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "example_item"), EXAMPLE_ITEM);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "tnt_fishing_rod"), TNT_FISHING_ROD);
        Registry.register(Registries.ITEM_GROUP, EXAMPLE_GROUP_KEY,
                FabricItemGroup.builder()
                        .icon(() -> new ItemStack(EXAMPLE_ITEM))
                        .displayName(Text.translatable("itemGroup.examplemod.example_group"))
                        .build());
        ItemGroupEvents.modifyEntriesEvent(EXAMPLE_GROUP_KEY).register(entries -> {
            entries.add(EXAMPLE_ITEM);
            entries.add(TNT_FISHING_ROD);
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (hand != Hand.MAIN_HAND || !player.getStackInHand(hand).isOf(TNT_FISHING_ROD)) {
                return ActionResult.PASS;
            }
            if (!world.isClient()) {
                boolean breakBlocks = TntFishingRodItem.toggleBreakBlocks(player.getStackInHand(hand));
                player.sendMessage(TntFishingRodItem.modeText(breakBlocks), true);
            }
            player.swingHand(hand);
            return ActionResult.SUCCESS;
        });

        FabricDefaultAttributeRegistry.register(WATER_MONSTER, WaterMonsterEntity.createAttributes());
        UseBlockCallback.EVENT.register(ExampleMod::trySummonWaterMonster);

        LOGGER.info("ExampleMod initialized successfully!");
    }

    private static ActionResult trySummonWaterMonster(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (hand != Hand.MAIN_HAND || !player.getStackInHand(hand).isEmpty()) {
            return ActionResult.PASS;
        }

        BlockPos topSand = hitResult.getBlockPos();
        if (!isWaterMonsterAltar(world, topSand)) {
            return ActionResult.PASS;
        }

        if (world instanceof ServerWorld serverWorld) {
            consumeWaterMonsterAltar(serverWorld, topSand);
            WaterMonsterEntity waterMonster = new WaterMonsterEntity(WATER_MONSTER, serverWorld);
            waterMonster.refreshPositionAndAngles(topSand.getX() + 0.5, topSand.getY() + 1.0, topSand.getZ() + 0.5, player.getYaw(), 0.0f);
            serverWorld.spawnEntity(waterMonster);
        }

        player.swingHand(hand);
        return ActionResult.SUCCESS;
    }

    private static boolean isWaterMonsterAltar(World world, BlockPos topSand) {
        if (!world.getBlockState(topSand).isOf(Blocks.SAND) || !world.getBlockState(topSand.down()).isOf(Blocks.SAND)) {
            return false;
        }

        return hasSandBaseLine(world, topSand, Direction.Axis.X) || hasSandBaseLine(world, topSand, Direction.Axis.Z);
    }

    private static boolean hasSandBaseLine(World world, BlockPos topSand, Direction.Axis axis) {
        BlockPos center = topSand.down();
        Direction positive = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction negative = positive.getOpposite();
        return world.getBlockState(center.offset(positive)).isOf(Blocks.SAND)
                && world.getBlockState(center.offset(negative)).isOf(Blocks.SAND);
    }

    private static void consumeWaterMonsterAltar(ServerWorld world, BlockPos topSand) {
        world.breakBlock(topSand, false);
        world.breakBlock(topSand.down(), false);
        if (hasSandBaseLine(world, topSand, Direction.Axis.X)) {
            world.breakBlock(topSand.down().east(), false);
            world.breakBlock(topSand.down().west(), false);
        } else {
            world.breakBlock(topSand.down().south(), false);
            world.breakBlock(topSand.down().north(), false);
        }
    }
}
