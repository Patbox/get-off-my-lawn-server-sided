package draylar.goml.mixin;

import draylar.goml.api.ClaimUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ExplosionImpl.class)
public abstract class ExplosionMixin {

    @Shadow @Nullable public abstract LivingEntity getCausingEntity();

    @Shadow @Final private ServerWorld world;

    @Inject(method = "getBlocksToDestroy", at = @At("TAIL"))
    private void goml_clearBlocks(CallbackInfoReturnable<List<BlockPos>> cir) {
        cir.getReturnValue().removeIf((b) -> !ClaimUtils.canExplosionDestroy(this.world, b, this.getCausingEntity()));
    }

    @ModifyVariable(method = "damageEntities", at = @At("STORE"), ordinal = 0)
    private List<Entity> goml_clearEntities(List<Entity> x) {
        x.removeIf((e) -> !ClaimUtils.canExplosionDestroy(this.world, e.getBlockPos(), this.getCausingEntity()));
        return x;
    }
}
