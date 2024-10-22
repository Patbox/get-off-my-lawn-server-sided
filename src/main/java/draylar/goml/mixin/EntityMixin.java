package draylar.goml.mixin;

import draylar.goml.api.ClaimUtils;
import draylar.goml.other.LegacyNbtHelper;
import draylar.goml.other.OriginOwner;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements OriginOwner {
    @Shadow private World world;

    @Shadow public abstract BlockPos getBlockPos();

    @Unique
    private BlockPos originPos;

    @Inject(method = "isAlwaysInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void goml$isInvulnerable(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (!ClaimUtils.canDamageEntity(this.world, (Entity) (Object) this, damageSource)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void writeGomlNbt(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        if (this.originPos != null) {
            nbt.put("goml:origin", LegacyNbtHelper.fromBlockPos(this.originPos));
        }
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void readGomlNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("goml:origin")) {
            this.originPos = LegacyNbtHelper.toBlockPos(nbt.getCompound("goml:origin"));
        }
    }

    @Override
    public BlockPos goml$getOrigin() {
        return this.originPos;
    }

    @Override
    public void goml$setOrigin(BlockPos pos) {
        this.originPos = pos;
    }

    @Override
    public void goml$tryFilling() {
        this.originPos = this.getBlockPos();
    }
}
