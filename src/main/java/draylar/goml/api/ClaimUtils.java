package draylar.goml.api;

import com.jamieswhiteshirt.rtree3i.Box;
import com.jamieswhiteshirt.rtree3i.Entry;
import com.jamieswhiteshirt.rtree3i.Selection;
import draylar.goml.EventHandlers;
import draylar.goml.GetOffMyLawn;
import draylar.goml.api.event.ClaimEvents;
import draylar.goml.block.augment.ExplosionControllerAugmentBlock;
import draylar.goml.block.entity.ClaimAnchorBlockEntity;
import draylar.goml.other.GomlPlayer;
import draylar.goml.other.StatusEnum;
import draylar.goml.registry.GOMLBlocks;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class ClaimUtils {

    /**
     * Returns all claims at the given position in the given world.
     *
     * @param world world to check for claim in
     * @param pos   position to check at
     * @return claims at the given position in the given world
     */
    public static Selection<Entry<ClaimBox, Claim>> getClaimsAt(WorldView world, BlockPos pos) {
        Box checkBox = Box.create(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries(box -> box.contains(checkBox));
    }

    /**
     * Returns all claims with the given origin in the given world.
     *
     * @param world world to check for claim in
     * @param pos   position to check at
     * @return claims at the given position in the given world
     */
    public static Selection<Entry<ClaimBox, Claim>> getClaimsWithOrigin(WorldView world, BlockPos pos) {
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries().filter(x -> x.getValue().getOrigin().equals(pos));
    }

    /**
     * Returns all claims in the given world where player is owner.
     *
     * <p>Under normal circumstances, only 1 claim will exist at a location, but multiple may still be returned.
     *
     * @param world  world to check for claim in
     * @param player player's uuid to find by
     * @return claims at the given position in the given world
     */
    public static Selection<Entry<ClaimBox, Claim>> getClaimsOwnedBy(WorldView world, UUID player) {
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries().filter(entry -> entry.getValue().isOwner(player));
    }

    /**
     * Returns all claims in the given world where player is trusted.
     *
     * <p>Under normal circumstances, only 1 claim will exist at a location, but multiple may still be returned.
     *
     * @param world  world to check for claim in
     * @param player player's uuid to find by
     * @return claims at the given position in the given world
     */
    public static Selection<Entry<ClaimBox, Claim>> getClaimsTrusted(WorldView world, UUID player) {
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries().filter(entry -> entry.getValue().getTrusted().contains(player));
    }

    /**
     * Returns all claims in the given world where player has access.
     *
     * <p>Under normal circumstances, only 1 claim will exist at a location, but multiple may still be returned.
     *
     * @param world  world to check for claim in
     * @param player player's uuid to find by
     * @return claims at the given position in the given world
     */
    public static Selection<Entry<ClaimBox, Claim>> getClaimsWithAccess(WorldView world, UUID player) {
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries().filter(entry -> entry.getValue().hasPermission(player));
    }

    /**
     * Returns all claims that intersect with a box created by the 2 given positions.
     *
     * @param world world to check for claim in
     * @param lower lower corner of claim
     * @param upper upper corner of claim
     * @return claims that intersect with a box created by the 2 positions in the given world
     */
    public static Selection<Entry<ClaimBox, Claim>> getClaimsInBox(WorldView world, BlockPos lower, BlockPos upper) {
        Box checkBox = createBox(lower, upper);
        return getClaimsInBox(world, checkBox);
    }

    public static Selection<Entry<ClaimBox, Claim>> getClaimsInBox(WorldView world, Box checkBox) {
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries(box -> box.intersectsClosed(checkBox));
    }

    public static Selection<Entry<ClaimBox, Claim>> getClaimsInOpenBox(WorldView world, Box checkBox) {
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries(box -> box.intersectsOpen(checkBox));
    }

    public static Box createBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        return Box.create(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    public static Box createBox(BlockPos pos1, BlockPos pos2) {
        return Box.create(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()), Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()));
    }

    /**
     * Returns all claims that intersect with a box created by the 2 given positions.
     * If the found box is equal to the ignore box, it is not included.
     *
     * @param world  world to check for claim in
     * @param lower  lower corner of claim
     * @param upper  upper corner of claim
     * @param ignore box to ignore
     * @return claims that intersect with a box created by the 2 positions in the given world
     */
    public static Selection<Entry<ClaimBox, Claim>> getClaimsInBox(WorldView world, BlockPos lower, BlockPos upper, Box ignore) {
        Box checkBox = Box.create(lower.getX(), lower.getY(), lower.getZ(), upper.getX(), upper.getY(), upper.getZ());
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries(box -> box.intersectsClosed(checkBox) && !box.equals(ignore));
    }

    public static Selection<Entry<ClaimBox, Claim>> getClaimsInBox(WorldView world, Box checkBox, Box ignore) {
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries(box -> box.intersectsClosed(checkBox) && !box.equals(ignore));
    }

    /**
     * Returns whether or not the information about a claim matches with a {@link PlayerEntity} and {@link BlockPos}.
     *
     * @param claim       claim to check
     * @param checkPlayer player to check against
     * @param checkPos    position to check against
     * @return whether or not the claim information matches up with the player and position
     */
    public static boolean canDestroyClaimBlock(Entry<ClaimBox, Claim> claim, @Nullable PlayerEntity checkPlayer, BlockPos checkPos) {
        return (checkPlayer == null || playerHasPermission(claim, checkPlayer)) && claim.getValue().getOrigin().equals(checkPos);
    }

    public static boolean canModifyClaimAt(World world, BlockPos pos, Entry<ClaimBox, Claim> claim, PlayerEntity player) {
        return claim.getValue().hasPermission(player)
                || isInAdminMode(player)
                || ClaimEvents.PERMISSION_DENIED.invoker().check(player, world, Hand.MAIN_HAND, pos, PermissionReason.AREA_PROTECTED) == ActionResult.SUCCESS;
    }

    public static boolean isInAdminMode(PlayerEntity player) {
        return Permissions.check(player, "goml.modify_others", 3) && (player instanceof GomlPlayer adminModePlayer && adminModePlayer.goml_getAdminMode());
    }

    public static boolean canFireDestroy(World world, BlockPos pos) {
        return ClaimUtils.getClaimsAt(world, pos).isEmpty();
    }

    public static boolean canFluidFlow(World world, BlockPos cur, BlockPos dest) {
        var claimsDest = ClaimUtils.getClaimsAt(world, dest);
        var claimsCur = ClaimUtils.getClaimsAt(world, cur);
        return claimsDest.isEmpty() || claimsCur.anyMatch(x -> claimsCur.anyMatch(y -> x.equals(y)));
    }

    public static boolean canExplosionDestroy(World world, BlockPos pos, @Nullable Entity causingEntity) {
        Selection<Entry<ClaimBox, Claim>> claimsFound = ClaimUtils.getClaimsAt(world, pos);

        PlayerEntity player;

        if (causingEntity instanceof PlayerEntity playerEntity) {
            player = playerEntity;
        } else if (!GetOffMyLawn.CONFIG.protectAgainstHostileExplosionsActivatedByTrustedPlayers && causingEntity instanceof MobEntity creeperEntity && creeperEntity.getTarget() instanceof PlayerEntity playerEntity) {
            player = playerEntity;
        } else {
            player = null;
        }

        if (player != null && claimsFound.isNotEmpty()) {
            return !claimsFound.anyMatch((Entry<ClaimBox, Claim> boxInfo) -> !canModifyClaimAt(world, pos, boxInfo, player));
        }

        return claimsFound.isEmpty() || claimsFound.anyMatch((c) -> {
            if (world.getServer() != null) {
                if (c.getValue().hasAugment(GOMLBlocks.EXPLOSION_CONTROLLER.getFirst())) {
                    return c.getValue().getData(ExplosionControllerAugmentBlock.KEY) == StatusEnum.Toggle.DISABLED;
                }
            }

            return false;
        });
    }

    public static boolean canDamageEntity(World world, Entity entity, DamageSource source) {
        if (entity == source.getAttacker()) {
            return true;
        }

        PlayerEntity player;

        if (source.getAttacker() instanceof PlayerEntity playerEntity) {
            player = playerEntity;
        } else if (!GetOffMyLawn.CONFIG.protectAgainstHostileExplosionsActivatedByTrustedPlayers && source.getAttacker() instanceof MobEntity creeperEntity && creeperEntity.getTarget() instanceof PlayerEntity playerEntity) {
            player = playerEntity;
        } else if (source.getAttacker() instanceof ProjectileEntity projectileEntity && projectileEntity.getOwner() instanceof PlayerEntity playerEntity) {
            player = playerEntity;
        } else if (source.getAttacker() instanceof AreaEffectCloudEntity projectileEntity && projectileEntity.getOwner() instanceof PlayerEntity playerEntity) {
            player = playerEntity;
        } else if (source.getAttacker() instanceof TameableEntity projectileEntity && projectileEntity.getOwner() instanceof PlayerEntity playerEntity) {
            player = playerEntity;
        } else {
            return true;
        }

        if (ClaimUtils.isInAdminMode(player) || entity == player) {
            return true;
        }

        if ((GetOffMyLawn.CONFIG.allowDamagingNamedHostileMobs
                || (GetOffMyLawn.CONFIG.allowDamagingUnnamedHostileMobs && entity.getCustomName() == null))
                && entity instanceof HostileEntity
        ) {
            return true;
        }
        var claims = ClaimUtils.getClaimsAt(world, entity.getBlockPos());

        if (claims.isEmpty()) {
            return true;
        }

        if (entity instanceof PlayerEntity attackedPlayer) {
            claims = claims.filter((e) -> e.getValue().hasAugment(GOMLBlocks.PVP_ARENA.getFirst()));

            if (claims.isEmpty()) {
                return GetOffMyLawn.CONFIG.enablePvPinClaims;
            } else {
                var obj = new MutableBoolean(true);
                claims.forEach((e) -> {
                    if (!obj.getValue()) {
                        return;
                    }
                    var claim = e.getValue();

                    obj.setValue(switch (claim.getData(GOMLBlocks.PVP_ARENA.getFirst().key)) {
                        case EVERYONE -> true;
                        case DISABLED -> false;
                        case TRUSTED -> claim.hasPermission(player) && claim.hasPermission(attackedPlayer);
                        case UNTRUSTED -> !claim.hasPermission(player) && !claim.hasPermission(attackedPlayer);
                    });
                });

                return obj.getValue();
            }
        }

        return EventHandlers.testPermission(claims, player, Hand.MAIN_HAND, entity.getBlockPos(), PermissionReason.ENTITY_PROTECTED) != ActionResult.FAIL;
    }

    public static boolean canModify(World world, BlockPos pos, @Nullable PlayerEntity player) {
        if (GetOffMyLawn.CONFIG.allowFakePlayersToModify && player != null && player.getClass() != ServerPlayerEntity.class && !world.isClient) {
            return true;
        }

        Selection<Entry<ClaimBox, Claim>> claimsFound = ClaimUtils.getClaimsAt(world, pos);
        if (player != null && claimsFound.isNotEmpty()) {
            return !claimsFound.anyMatch((Entry<ClaimBox, Claim> boxInfo) -> !canModifyClaimAt(world, pos, boxInfo, player));
        }

        return claimsFound.isEmpty();
    }

    @Nullable
    public static ClaimAnchorBlockEntity getAnchor(World world, Claim claim) {
        ClaimAnchorBlockEntity claimAnchor = (ClaimAnchorBlockEntity) world.getBlockEntity(claim.getOrigin());

        if (claimAnchor == null) {
            GetOffMyLawn.LOGGER.warn(String.format("A claim anchor was requested at %s, but no Claim Anchor BE was found! Was the claim not properly removed? Removing the claim now.", claim.getOrigin().toString()));

            // Remove claim
            GetOffMyLawn.CLAIM.get(world).getClaims().entries().forEach(entry -> {
                if (entry.getValue() == claim) {
                    GetOffMyLawn.CLAIM.get(world).remove(entry.getValue());
                }
            });

            return null;
        }

        return claimAnchor;
    }

    public static List<Text> getClaimText(MinecraftServer server, Claim claim) {
        var owners = getPlayerNames(server, claim.getOwners());
        var trusted = getPlayerNames(server, claim.getTrusted());

        var texts = new ArrayList<Text>();

        texts.add(Text.translatable("text.goml.position",
                Text.literal(claim.getOrigin().toShortString())
                        .append(Text.literal(" (" + claim.getWorld().toString() + ")").formatted(Formatting.GRAY)).formatted(Formatting.WHITE)
        ).formatted(Formatting.BLUE));

        texts.add(Text.translatable("text.goml.radius",
                Text.literal("" + claim.getRadius()).formatted(Formatting.WHITE)
        ).formatted(Formatting.YELLOW));

        if (!owners.isEmpty()) {
            texts.add(Text.translatable("text.goml.owners", owners.remove(0)).formatted(Formatting.GOLD));

            for (var text : owners) {
                texts.add(Text.literal("   ").append(text));
            }
        }

        if (!trusted.isEmpty()) {
            texts.add(Text.translatable("text.goml.trusted", trusted.remove(0)).formatted(Formatting.GREEN));

            for (var text : trusted) {
                texts.add(Text.literal("   ").append(text));
            }
        }

        return texts;
    }

    protected static final List<Text> getPlayerNames(MinecraftServer server, Collection<UUID> uuids) {
        var list = new ArrayList<Text>();

        var builder = new StringBuilder();
        var iterator = uuids.iterator();
        while (iterator.hasNext()) {
            var gameProfile = server.getUserCache().getByUuid(iterator.next());
            if (gameProfile.isPresent()) {
                builder.append(gameProfile.get().getName());

                if (iterator.hasNext()) {
                    builder.append(", ");
                }

                if (builder.length() > 32) {
                    list.add(Text.literal(builder.toString()).formatted(Formatting.WHITE));
                    builder = new StringBuilder();
                }
            }
        }
        if (!builder.isEmpty()) {
            list.add(Text.literal(builder.toString()).formatted(Formatting.WHITE));
        }

        return list;
    }

    @Deprecated
    public static boolean claimMatchesWith(Entry<ClaimBox, Claim> claim, @Nullable PlayerEntity checkPlayer, BlockPos checkPos) {
        return canDestroyClaimBlock(claim, checkPlayer, checkPos);
    }

    @Deprecated
    public static boolean playerHasPermission(Entry<ClaimBox, Claim> claim, PlayerEntity checkPlayer) {
        return claim.getValue().getOwners().contains(checkPlayer.getUuid()) || isInAdminMode(checkPlayer);
    }

    public static ClaimBox createClaimBox(BlockPos pos, int radius) {
        if (GetOffMyLawn.CONFIG.makeClaimAreaChunkBound) {
            var chunkPos = ChunkSectionPos.from(pos);

            radius = (int) ((Math.ceil(radius / 16d) - 1) * 16) + 8;
            var radiusY = (int) ((Math.ceil((radius * GetOffMyLawn.CONFIG.claimAreaHeightMultiplier) / 16d) - 1) * 16) + 8;

            return new ClaimBox(chunkPos.getCenterPos(), radius, GetOffMyLawn.CONFIG.claimProtectsFullWorldHeight ? Short.MAX_VALUE : radiusY, true);
        }

        return new ClaimBox(pos, radius, GetOffMyLawn.CONFIG.claimProtectsFullWorldHeight ? Short.MAX_VALUE : (int) (radius * GetOffMyLawn.CONFIG.claimAreaHeightMultiplier));
    }

    public static Pair<Vec3d, Direction> getClosestXZBorder(Claim claim, Vec3d curPos) {
        return getClosestXZBorder(claim, curPos, 0);
    }

    public static Pair<Vec3d, Direction> getClosestXZBorder(Claim claim, Vec3d curPos, double extraDistance) {
        var box = claim.getClaimBox();

        var center = box.noShift() ? Vec3d.of(box.origin()) : Vec3d.ofCenter(box.getOrigin());
        var vec = curPos.subtract(center);
        var r = (box.noShift() ? box.radius() - 0.5 : box.radius()) + extraDistance;
        var angle = MathHelper.atan2(vec.z, vec.x);

        var tan = Math.tan(angle);

        if (Double.isNaN(tan)) {
            tan = 1;
        }

        double x, z;

        Direction dir = null;

        if (angle >= -MathHelper.HALF_PI / 2 && angle <= MathHelper.HALF_PI / 2) {
            x = r;
            z = tan * r;
            dir = Direction.EAST;
        } else if (angle >= MathHelper.HALF_PI / 2 && angle <= MathHelper.HALF_PI * 3 / 2) {
            x = 1 / tan * r;
            z = r;
            dir = Direction.SOUTH;
        } else if (angle <= -MathHelper.HALF_PI / 2 && angle >= -MathHelper.HALF_PI * 3 / 2) {
            x = -1 / tan * r;
            z = -r;
            dir = Direction.NORTH;
        } else {
            x = -r;
            z = -Math.tan(angle) * r;
            dir = Direction.WEST;
        }


        return new Pair<>(center.add(x, 0, z), dir);
    }
}
