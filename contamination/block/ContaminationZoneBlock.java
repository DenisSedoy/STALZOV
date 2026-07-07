package com.czo.contamination.block;

import com.czo.contamination.ContaminationType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ContaminationZoneBlock extends Block {
    private static final VoxelShape MARKER_SHAPE = Block.box(4.0D, 4.0D, 4.0D, 12.0D, 12.0D, 12.0D);

    private final ContaminationType contaminationType;
    private final int contaminationLevel;

    public ContaminationZoneBlock(ContaminationType contaminationType, int contaminationLevel, Properties properties) {
        super(properties);
        this.contaminationType = contaminationType;
        this.contaminationLevel = contaminationLevel;
    }

    public ContaminationType contaminationType() {
        return contaminationType;
    }

    public int contaminationLevel() {
        return contaminationLevel;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();

            if (entity instanceof Player player && player.isCreative() && isHoldingContaminationZone(player)) {
                return MARKER_SHAPE;
            }
        }

        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    private static boolean isHoldingContaminationZone(Player player) {
        return isContaminationZoneItem(player.getMainHandItem()) || isContaminationZoneItem(player.getOffhandItem());
    }

    private static boolean isContaminationZoneItem(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ContaminationZoneBlock;
    }
}
