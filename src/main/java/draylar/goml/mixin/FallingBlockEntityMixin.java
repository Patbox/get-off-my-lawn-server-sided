package draylar.goml.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import draylar.goml.api.ClaimUtils;
import draylar.goml.other.OriginOwner;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.UUID;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin extends Entity implements OriginOwner {
    public FallingBlockEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;canPlaceAt(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean cantPlaceOnClaim(boolean bool, @Local(ordinal = 0) BlockPos pos) {
        if (this.getWorld().isClient) {
            return bool;
        }
        if (bool) {
            return ClaimUtils.hasMatchingClaims(this.getWorld(), pos, this.goml$getOriginSafe());
        }

        return false;
    }

    @Inject(method = "handleFallDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;ceil(F)I", ordinal = 0), cancellable = true)
    private void blockFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (this.getWorld().isClient) {
            return;
        }
        if (!ClaimUtils.hasMatchingClaims(this.getWorld(), this.getBlockPos(), this.goml$getOriginSafe())) {
            cir.setReturnValue(false);
        }
    }
}
