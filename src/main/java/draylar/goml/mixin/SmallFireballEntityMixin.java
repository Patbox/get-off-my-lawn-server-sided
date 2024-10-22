package draylar.goml.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import draylar.goml.api.ClaimUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(SmallFireballEntity.class)
public abstract class SmallFireballEntityMixin extends AbstractFireballEntity {

    public SmallFireballEntityMixin(EntityType<? extends AbstractFireballEntity> entityType, World world) {
        super(entityType, world);
    }

    @WrapWithCondition(method = "onBlockHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private boolean safeSetBlock(World world, BlockPos pos, BlockState state) {
        return ClaimUtils.canExplosionDestroy(world, pos, this);
    }
}
