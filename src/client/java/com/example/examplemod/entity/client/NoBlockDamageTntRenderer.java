package com.example.examplemod.entity.client;

import com.example.examplemod.entity.NoBlockDamageTntEntity;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.block.MovingBlockRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.FallingBlockEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;

public class NoBlockDamageTntRenderer extends EntityRenderer<NoBlockDamageTntEntity, FallingBlockEntityRenderState> {
    public NoBlockDamageTntRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
    }

    @Override
    public FallingBlockEntityRenderState createRenderState() {
        return new FallingBlockEntityRenderState();
    }

    @Override
    public void updateRenderState(NoBlockDamageTntEntity entity, FallingBlockEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        BlockPos blockPos = BlockPos.ofFloored(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
        MovingBlockRenderState blockState = state.movingBlockRenderState;
        blockState.fallingBlockPos = entity.getBlockPos();
        blockState.entityBlockPos = blockPos;
        blockState.blockState = Blocks.TNT.getDefaultState();
        blockState.biome = entity.getEntityWorld().getBiome(blockPos);
        blockState.world = entity.getEntityWorld();
    }

    @Override
    public void render(FallingBlockEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState) {
        matrices.push();
        matrices.translate(-0.5, 0.0, -0.5);
        queue.submitMovingBlock(matrices, state.movingBlockRenderState);
        matrices.pop();
        super.render(state, matrices, queue, cameraRenderState);
    }
}
