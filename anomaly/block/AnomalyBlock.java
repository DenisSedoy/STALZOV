package com.czo.anomaly.block;

import com.czo.anomaly.AnomalyType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AnomalyBlock extends Block {
    public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 0, 15);

    private static final VoxelShape CARPET_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
    private static final VoxelShape CORE_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 13.0D, 13.0D);
    private static final VoxelShape AIR_CORE_SHAPE = Block.box(4.0D, 4.0D, 4.0D, 12.0D, 12.0D, 12.0D);

    private final AnomalyType anomalyType;

    public AnomalyBlock(AnomalyType anomalyType, Properties properties) {
        super(properties);
        this.anomalyType = anomalyType;
        this.registerDefaultState(this.stateDefinition.any().setValue(LIGHT_LEVEL, defaultLightLevel(anomalyType)));
    }

    public AnomalyType anomalyType() {
        return anomalyType;
    }

    public static int getCzoLightLevel(BlockState state) {
        if (state.hasProperty(LIGHT_LEVEL)) {
            return state.getValue(LIGHT_LEVEL);
        }

        return 0;
    }

    public static int defaultLightLevel(AnomalyType type) {
        return switch (type) {
            // Термоаномалии в покое теперь дают слабое, но заметное свечение.
            case BURNER, STEAM -> 5;
            case INFERNO -> 6;
            // Газовик копит яркость от 5 до 11, вспышка при разряде — 15.
            case GAS -> 5;
            // Электра в состоянии заряда тоже светится слабым синим светом.
            case ELECTRO -> 5;
            // Кисель теперь не рисуется блоком, но даёт слабое кислотно-зелёное свечение.
            case ACID -> 5;
            default -> 0;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIGHT_LEVEL);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // В survival/adventure тех-блоки аномалий не дают обычный блоковый аутлайн.
        // В creative форма остаётся, чтобы аномалии можно было ставить/ломать/редактировать.
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();

            if (entity instanceof Player player && player.isCreative()) {
                return editorShape();
            }
        }

        return Shapes.empty();
    }

    private VoxelShape editorShape() {
        return switch (anomalyType) {
            case ACID -> CARPET_SHAPE;
            case ELECTRO, SPRINGBOARD, HYDRAULIC -> AIR_CORE_SHAPE;
            default -> CORE_SHAPE;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return switch (anomalyType) {
            // Видимость этих аномалий идёт частицами, сам тех-блок не рисуем.
            case BURNER, INFERNO, STEAM, GAS, VORTEX, CAROUSEL, HYDRAULIC, ELECTRO, ELECTROSTATIC, ACID, SPRINGBOARD -> RenderShape.INVISIBLE;
        };
    }
}
