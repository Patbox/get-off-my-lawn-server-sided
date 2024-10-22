package draylar.goml.block;

import draylar.goml.GetOffMyLawn;
import draylar.goml.api.Claim;
import draylar.goml.api.ClaimUtils;
import draylar.goml.api.event.ClaimEvents;
import draylar.goml.block.entity.ClaimAnchorBlockEntity;
import draylar.goml.item.UpgradeKitItem;
import draylar.goml.registry.GOMLEntities;
import draylar.goml.registry.GOMLTextures;
import eu.pb4.polymer.core.api.block.PolymerHeadBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collections;
import java.util.function.IntSupplier;

@SuppressWarnings({"deprecation"})
public class ClaimAnchorBlock extends Block implements BlockEntityProvider, PolymerHeadBlock {

    private final IntSupplier radius;
    private final String texture;

    @Deprecated
    public ClaimAnchorBlock(Block.Settings settings, int radius) {
        this(settings, () -> radius, GOMLTextures.MISSING_TEXTURE);
    }

    public ClaimAnchorBlock(Block.Settings settings, IntSupplier radius, String texture) {
        super(settings.solid());
        this.radius = radius;
        this.texture = texture;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (world == null) {
            return;
        }

        if (!world.isClient()) {
            var radius = Math.max(this.radius.getAsInt(), 1);

            Claim claimInfo = new Claim(world.getServer(), Collections.singleton(placer.getUuid()), Collections.emptySet(), pos);
            claimInfo.internal_setIcon(new ItemStack(itemStack.getItem()));
            claimInfo.internal_setType(this);
            claimInfo.internal_setWorld(world.getRegistryKey().getValue());
            var box = ClaimUtils.createClaimBox(pos, radius);
            claimInfo.internal_setClaimBox(box);
            GetOffMyLawn.CLAIM.get(world).add(claimInfo);

            // Assign claim to BE
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof ClaimAnchorBlockEntity anchor) {
                anchor.setClaim(claimInfo, box);
            }
            if (world instanceof ServerWorld world1) {
                claimInfo.internal_updateChunkCount(world1);
            }

            ClaimEvents.CLAIM_CREATED.invoker().onEvent(claimInfo);
            claimInfo.internal_enableUpdates();
        }

        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (world == null || world.isClient()) {
            return;
        }
        if (newState.getBlock() != state.getBlock()) {
            ClaimUtils.getClaimsAt(world, pos).forEach(claimedArea -> {
                if (ClaimUtils.canDestroyClaimBlock(claimedArea, null, pos)) {
                    claimedArea.getValue().destroy();
                }
            });
        }


        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world == null || world.isClient()) {
            return state;
        }

        ClaimUtils.getClaimsAt(world, pos).forEach(claimedArea -> {
            if (ClaimUtils.canDestroyClaimBlock(claimedArea, player, pos)) {
                claimedArea.getValue().destroy();
            }
        });

        return super.onBreak(world, pos, state, player);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity playerEntity, BlockHitResult hit) {
        if (playerEntity instanceof ServerPlayerEntity player && !player.isSneaking() && !(player.getMainHandStack().getItem() instanceof UpgradeKitItem)) {
            var blockEntity = world.getBlockEntity(pos, GOMLEntities.CLAIM_ANCHOR);
            blockEntity.ifPresent(claimAnchorBlockEntity -> claimAnchorBlockEntity.getClaim().openUi(player));

            return ActionResult.SUCCESS;
        }
        return super.onUse(state, world, pos, playerEntity, hit);
    }

    public int getRadius() {
        return this.radius.getAsInt();
    }

    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        if (ClaimUtils.isInAdminMode(player) || (world instanceof ServerWorld serverWorld && ClaimUtils.getClaimsAt(serverWorld, pos).anyMatch(x -> x.getValue().isOwner(player)))) {
            return super.calcBlockBreakingDelta(state, player, world, pos);
        } else {
            return 0;
        }
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ClaimAnchorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return ClaimAnchorBlockEntity::tick;
    }

    @Override
    public String getPolymerSkinValue(BlockState state, BlockPos pos, PacketContext player) {
        return this.texture;
    }
}
