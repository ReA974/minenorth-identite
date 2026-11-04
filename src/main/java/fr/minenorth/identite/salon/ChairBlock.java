package fr.minenorth.identite.salon;

import fr.minenorth.identite.character.CharacterManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Fauteuil de salon. Clic droit : si un professionnel est dans le salon, l'éditeur d'apparence s'ouvre chez le client,
 * limité à ce que le salon permet (tatouages et piercings, ou cheveux et pilosité).
 */
public class ChairBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 11, 16);

    private final SalonKind kind;

    public ChairBlock(SalonKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer client)) return InteractionResult.PASS;
        List<Set<String>> nearby = new ArrayList<>();
        double max = SalonKind.RADIUS * SalonKind.RADIUS;
        for (ServerPlayer other : client.server.getPlayerList().getPlayers()) {
            if (other != client && other.level() == level && other.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= max) {
                nearby.add(other.getTags());
            }
        }
        if (!kind.allowed(client.hasPermissions(2), client.getTags(), nearby)) {
            client.displayClientMessage(Component.literal("§c" + kind.missingMessage()), true);
            return InteractionResult.CONSUME;
        }
        if (!CharacterManager.openEditor(client.server, client.getUUID(), kind.sections())) {
            client.displayClientMessage(Component.literal("§cCréez d'abord votre personnage."), true);
        }
        return InteractionResult.CONSUME;
    }
}
