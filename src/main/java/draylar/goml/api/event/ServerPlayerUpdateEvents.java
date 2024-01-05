package draylar.goml.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;

public class ServerPlayerUpdateEvents {
    public static final Event<PlayerNameChangedEvent> NAME_CHANGED = EventFactory.createArrayBacked(PlayerNameChangedEvent.class,
        (listeners) -> player -> {
            for (var event : listeners) {
                event.onNameChanged(player);
            }
        }
    );

    @FunctionalInterface
    public interface PlayerNameChangedEvent {
        void onNameChanged(ServerPlayerEntity player);
    }
}
