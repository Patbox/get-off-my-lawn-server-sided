package draylar.goml.block.augment;

import com.mojang.authlib.GameProfile;
import draylar.goml.GetOffMyLawn;
import draylar.goml.api.Claim;
import draylar.goml.api.ClaimUtils;
import draylar.goml.api.DataKey;
import draylar.goml.block.ClaimAugmentBlock;
import draylar.goml.registry.GOMLTextures;
import draylar.goml.ui.GenericPlayerListGui;
import draylar.goml.ui.GenericPlayerSelectionGui;
import draylar.goml.ui.PagedGui;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ForceFieldAugmentBlock extends ClaimAugmentBlock {

    public static final DataKey<Set<UUID>> UUID_KEY = DataKey.ofUuidSet(GetOffMyLawn.id("force_field/uuids"));
    public static final DataKey<Boolean> WHITELIST_KEY = DataKey.ofBoolean(GetOffMyLawn.id("force_field/whitelist"), true);

    public ForceFieldAugmentBlock(Settings settings, String texture) {
        super(settings, texture);
    }

    @Override
    public void onPlayerEnter(Claim claim, PlayerEntity player) {
        if (shouldBlock(claim, player) && claim.getClaimBox().minecraftBox().contains(player.getPos())) {
            Pair<Vec3d, Direction> pair = ClaimUtils.getClosestXZBorder(claim, player.getPos(), 2);
            int distance = 0;
            while (true) {
                var i = shouldBlock(player.getWorld(), pair.getLeft(), player);

                if (i == -1) {
                    break;
                }
                distance += i;
                pair = ClaimUtils.getClosestXZBorder(claim, player.getPos(), 2 + distance);
            }


            var pairPart = ClaimUtils.getClosestXZBorder(claim, player.getPos(), distance);

            var pos = pair.getLeft();
            var dir = pair.getRight();
            var pos2 = pairPart.getLeft();

            var dir2 = pairPart.getRight();

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    ((ServerWorld) player.getWorld()).spawnParticles(
                            (ServerPlayerEntity) player, new BlockStateParticleEffect(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.getDefaultState()), true,
                            pos2.x + dir2.getOffsetZ() * x, player.getEyeY() + y, pos2.z + dir2.getOffsetX() * x,
                            1,
                            0.0, 0.0, 0.0,
                            0.0
                    );
                }
            }

            double y;
            if (player.getWorld().isSpaceEmpty(player, player.getDimensions(player.getPose()).getBoxAt(pos.x, player.getY(), pos.z))) {
                y = player.getY();
            } else {
                y = player.getWorld().getTopY(Heightmap.Type.MOTION_BLOCKING, (int) pos.x, (int) pos.z);
            }

            player.teleport(pos.x, y, pos.z);

            player.setVelocity(Vec3d.of(dir.getVector()).multiply(0.2));

            if (player.hasVehicle()) {
                player.getVehicle().teleport(pos.x, y, pos.z);
                player.getVehicle().setVelocity(Vec3d.of(dir.getVector()).multiply(0.2));
            }


            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayer));

                if (player.hasVehicle()) {
                    serverPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player.getVehicle()));
                }
            }
        }
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public void playerTick(Claim claim, PlayerEntity player) {
        onPlayerEnter(claim, player);
    }

    @Override
    public boolean ticks() {
        return true;
    }

    public boolean shouldBlock(Claim claim, PlayerEntity player) {
        var uuids = claim.getData(UUID_KEY);

        if (claim.hasPermission(player) || ClaimUtils.isInAdminMode(player)) {
            return false;
        }
        var doThing = uuids.contains(player.getUuid());

        if (claim.getData(WHITELIST_KEY)) {
            doThing = !doThing;
        }

        return doThing;
    }


    private int shouldBlock(World world, Vec3d pos, PlayerEntity player) {
        var x = ClaimUtils.getClaimsAt(world, BlockPos.ofFloored(pos))
                .filter(entry -> entry.getValue().hasAugment(this) && shouldBlock(entry.getValue(), player)).collect(Collectors.toList());

        return x.isEmpty() ? -1 : x.get(0).getValue().getRadius();
    }

    @Override
    public void openSettings(Claim claim, ServerPlayerEntity player, @Nullable Runnable closeCallback) {
        var gui = new SimpleGui(ScreenHandlerType.HOPPER, player, false) {
            boolean ingore = false;

            @Override
            public void onClose() {
                if (closeCallback != null && !this.ingore) {
                    closeCallback.run();
                }
            }
        };

        gui.setTitle(this.getGuiName());
        {
            var change = new MutableObject<Runnable>();
            change.setValue(() -> {
                var currentMode = claim.getData(WHITELIST_KEY).booleanValue();
                gui.setSlot(0, new GuiElementBuilder(currentMode ? Items.WHITE_WOOL : Items.BLACK_WOOL)
                        .setName(Text.translatable("text.goml.gui.force_field.whitelist_mode", ScreenTexts.onOrOff(currentMode)))
                        .addLoreLine(Text.translatable("text.goml.mode_toggle.help").formatted(Formatting.GRAY))
                        .setCallback((x, y, z) -> {
                            PagedGui.playClickSound(player);
                            claim.setData(WHITELIST_KEY, !currentMode);
                            change.getValue().run();
                        })
                );
            });

            change.getValue().run();
        }
        {
            var change = new MutableObject<Runnable>();
            change.setValue(() -> {
                gui.setSlot(1, new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setName(Text.translatable("text.goml.gui.force_field.player_list"))
                        .setCallback((x, y, z) -> {
                            PagedGui.playClickSound(player);
                            gui.ingore = true;
                            gui.close(true);
                            gui.ingore = false;
                            new ListGui(player, claim, gui::open);
                        })
                );
            });

            change.getValue().run();
        }

        gui.setSlot(4, new GuiElementBuilder(Items.STRUCTURE_VOID)
                .setName(Text.translatable(closeCallback != null ? "text.goml.gui.back" : "text.goml.gui.close").formatted(Formatting.RED))
                .setCallback((x, y, z) -> {
                    PagedGui.playClickSound(player);
                    gui.close(closeCallback != null);
                })
        );

        while (gui.getFirstEmptySlot() != -1) {
            gui.addSlot(PagedGui.DisplayElement.filler().element());
        }

        gui.open();
    }

    private class ListGui extends GenericPlayerListGui {
        private final Claim claim;

        public ListGui(ServerPlayerEntity player, Claim claim, @Nullable Runnable onClose) {
            super(player, onClose);
            this.setTitle(Text.translatable("text.goml.gui.force_field.player_list"));
            this.claim = claim;
            this.updateDisplay();
            this.open();
        }

        @Override
        protected void updateDisplay() {
            this.uuids.clear();
            this.uuids.addAll(this.claim.getData(UUID_KEY));
            super.updateDisplay();
        }

        @Override
        protected DisplayElement getNavElement(int id) {
            return switch (id) {
                case 5 -> DisplayElement.of(new GuiElementBuilder(Items.PLAYER_HEAD)
                        .setName(Text.translatable("text.goml.gui.player_list.add_player").formatted(Formatting.GREEN))
                        .setSkullOwner(GOMLTextures.GUI_ADD)
                        .setCallback((x, y, z) -> {
                            playClickSound(this.player);
                            this.ignoreCloseCallback = true;
                            this.close(true);
                            this.ignoreCloseCallback = false;

                            new GenericPlayerSelectionGui(
                                    this.player,
                                    Text.translatable("text.goml.gui.force_field.add_player.title"),
                                    (p) -> !this.claim.hasDirectPermission(p.getId()) && !this.claim.getData(UUID_KEY).contains(p.getId()),
                                    (p) -> this.claim.getData(UUID_KEY).add(p.getId()),
                                    this::refreshOpen).updateAndOpen();
                        }));
                default -> super.getNavElement(id);
            };
        }

        @Override
        protected void modifyBuilder(GuiElementBuilder builder, Optional<GameProfile> optional, UUID uuid) {
            builder.addLoreLine(Text.translatable("text.goml.gui.click_to_remove"));
            builder.setCallback((x, y, z) -> {
                playClickSound(player);
                this.claim.getData(UUID_KEY).remove(uuid);
                this.updateDisplay();
            });

        }
    }
}
