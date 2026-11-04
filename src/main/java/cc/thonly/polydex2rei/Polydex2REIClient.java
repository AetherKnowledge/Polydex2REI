package cc.thonly.polydex2rei;

import cc.thonly.polydex2rei.network.NoticeOpenInventory;
import cc.thonly.polydex2rei.network.PolydexScreenPayload;
import cc.thonly.polydex2rei.client.PolydexTabNavigation;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;

public class Polydex2REIClient
        implements ClientModInitializer {
    @SuppressWarnings("resource")
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PolydexScreenPayload.PACKET_ID,
                (payload, context) -> context.client().execute(() -> PolydexTabNavigation.receive(payload)));
        ScreenEvents.AFTER_INIT.register(PolydexTabNavigation::afterInit);
        ClientGuiEvent.RENDER_CONTAINER_BACKGROUND.register(PolydexTabNavigation::render);
        ClientTickEvents.END_CLIENT_TICK.register(PolydexTabNavigation::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PolydexTabNavigation.reset());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->{
            ClientPlayNetworking.registerReceiver(NoticeOpenInventory.PACKET_ID, (payload, context) -> {
                context.client().execute(() -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc == null) {
                        return;
                    }

                    LocalPlayer player = mc.player;
                    if (player == null) {
                        return;
                    }
                    mc.player.closeContainer();
                    mc.gui.setScreen(
                            new InventoryScreen(player)
                    );
                });
            });
        });
    }
}
