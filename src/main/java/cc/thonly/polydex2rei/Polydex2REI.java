package cc.thonly.polydex2rei;

import cc.thonly.polydex2rei.network.Action;
import cc.thonly.polydex2rei.network.NoticeOpenInventory;
import cc.thonly.polydex2rei.network.PolydexScreenPayload;
import cc.thonly.polydex2rei.network.StackActionPayload;
import cc.thonly.polydex2rei.util.ServerTimeEvent;
import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexPageUtils;
import eu.pb4.polydex.impl.PolydexImpl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Polydex2REI
        implements ModInitializer {
    public static final String MOD_ID = "polydex2rei";
    public static final Logger LOGGER = LoggerFactory.getLogger("polydex2rei");

    public void onInitialize() {
        LOGGER.info("Loading Polydex2REI!");
        PayloadTypeRegistry.clientboundPlay().register(StackActionPayload.PACKET_ID, StackActionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(StackActionPayload.PACKET_ID, StackActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NoticeOpenInventory.PACKET_ID, NoticeOpenInventory.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(NoticeOpenInventory.PACKET_ID, NoticeOpenInventory.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PolydexScreenPayload.PACKET_ID, PolydexScreenPayload.CODEC);

        ServerLifecycleEvents.SERVER_STARTING.register(minecraftServer -> ServerTimeEvent.tick());
        ServerTickEvents.END_SERVER_TICK.register(minecraftServer -> {
            ServerTimeEvent.tick();
        });

        ServerPlayNetworking.registerGlobalReceiver(StackActionPayload.PACKET_ID, (payload, context) -> {
            ServerPlayer player = context.player();
            Action action = payload.action();
            String stringId = payload.id();
            Identifier id = Identifier.parse(stringId);
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item == Items.AIR) {
                rejectPolydexQuery(player, payload);
                return;
            }
            ItemStack itemStack = item.getDefaultInstance();
            PolydexEntry entry = PolydexImpl.getEntry(itemStack);
            if (entry == null) {
                entry = PolydexImpl.ITEM_ENTRIES.nonEmptyById().get(id);
            }
            if (entry != null) {
                PolydexEntry finalEntry = entry;
                if (action == Action.RIGHT || action == Action.LEFT) {
                    ServerTimeEvent.add(new ServerTimeEvent(() -> openPolydex(player, finalEntry, payload), 1));
                } else {
                    rejectPolydexQuery(player, payload);
                }
            } else {
                rejectPolydexQuery(player, payload);
            }
        });

    }

    private static void rejectPolydexQuery(ServerPlayer player, StackActionPayload query) {
        if (ServerPlayNetworking.canSend(player, PolydexScreenPayload.ID)) {
            ServerPlayNetworking.send(player, new PolydexScreenPayload(-1, false, query));
        }
    }

    private static void openPolydex(ServerPlayer player, PolydexEntry entry, StackActionPayload query) {
        int[] openedContainer = {-1};
        Runnable back = () -> {
            player.closeContainer();
            if (ServerPlayNetworking.canSend(player, PolydexScreenPayload.ID)) {
                ServerPlayNetworking.send(player, new PolydexScreenPayload(openedContainer[0], false, query));
            } else if (ServerPlayNetworking.canSend(player, NoticeOpenInventory.ID)) {
                ServerPlayNetworking.send(player, new NoticeOpenInventory());
            }
        };
        var previousMenu = player.containerMenu;
        if (query.action() == Action.RIGHT) {
            PolydexPageUtils.openUsagesListUi(player, entry, back);
        } else {
            PolydexPageUtils.openRecipeListUi(player, entry, back);
        }
        if (player.containerMenu != previousMenu && player.containerMenu != player.inventoryMenu) {
            openedContainer[0] = player.containerMenu.containerId;
            if (ServerPlayNetworking.canSend(player, PolydexScreenPayload.ID)) {
                ServerPlayNetworking.send(player, new PolydexScreenPayload(openedContainer[0], true, query));
            }
        } else {
            rejectPolydexQuery(player, query);
        }
    }

    public static void sendQueryPolydexPagePacket(Action action, String realItemId) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return;
        }
        if (realItemId == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            LOGGER.warn("Cannot send payload: client not connected.");
            return;
        }
        StackActionPayload payload = new StackActionPayload(action, realItemId);
        ClientPlayNetworking.send(payload);
    }

    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }
}
