package draylar.goml.mixin;

import draylar.goml.api.ClaimUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin extends Block {
    public FarmlandBlockMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "setToDirt", at = @At("HEAD"), cancellable = true)
    private static void goml$protectFarmland(Entity entity, BlockState state, World world, BlockPos pos, CallbackInfo ci) {
        if (world.isClient) {
            return;
        }
        if (!ClaimUtils.canModify(world, pos, entity instanceof PlayerEntity player ? player : null)) {
            ci.cancel();
        }
    }
}
