package draylar.goml.mixin.augment;

import draylar.goml.api.Claim;
import draylar.goml.api.ClaimUtils;
import draylar.goml.block.entity.ClaimAnchorBlockEntity;
import draylar.goml.registry.GOMLBlocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends LivingEntity {

    private VillagerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean isInvulnerableTo(ServerWorld world, DamageSource source) {
        if(source.getAttacker() instanceof HostileEntity) {
            boolean b = ClaimUtils.getClaimsAt(getWorld(), getBlockPos()).anyMatch(claim -> claim.getValue().hasAugment(GOMLBlocks.VILLAGE_CORE.getFirst()));

            if(b) return true;
        }

        return super.isInvulnerableTo(world, source);
    }
}
