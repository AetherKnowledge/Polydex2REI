package cc.thonly.polydex2rei.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.shedaniel.rei.impl.common.registry.displays.ServerDisplayRegistryImpl;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(ServerDisplayRegistryImpl.class)
public abstract class ServerDisplayRegistryImplMixin {
    @WrapMethod(method = "updatePlayers")
    private void encodeRecipesForPlayer(RegistryAccess registries, List<ServerPlayer> players, Operation<Void> original) {
        // REI pre-encodes split packets. Each player needs their own Polymer transformations.
        for (ServerPlayer player : players) {
            PacketContext.runWithContext((PacketContextProvider) player.connection,
                    () -> original.call(registries, List.of(player)));
        }
    }
}
