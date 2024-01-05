package draylar.goml.mixin;

import draylar.goml.block.augment.HeavenWingsAugmentBlock;
import draylar.goml.api.event.ServerPlayerUpdateEvents;
import io.github.ladysnake.pal.VanillaAbilities;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void goml_remove(ServerPlayerEntity player, CallbackInfo ci) {
        HeavenWingsAugmentBlock.HEAVEN_WINGS.revokeFrom(player, VanillaAbilities.ALLOW_FLYING);
    }

    @Inject(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/text/Text;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/text/MutableText;"), slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=multiplayer.player.joined.renamed")))
    private void goml_onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        ServerPlayerUpdateEvents.NAME_CHANGED.invoker().onNameChanged(player);
    }
}
