package cc.thonly.polydex2rei.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.architectury.networking.NetworkManager;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import me.shedaniel.rei.RoughlyEnoughItemsNetwork;
import me.shedaniel.rei.impl.common.networking.REIPackets;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RoughlyEnoughItemsNetwork.class)
public abstract class RoughlyEnoughItemsNetworkMixin {
    // Convert only after REI has checked the player's cheat permission.
    @ModifyExpressionValue(
            method = "lambda$onInitialize$1",
            at = @At(value = "INVOKE", target = "Lme/shedaniel/rei/impl/common/networking/REIPackets$CreateItems;stack()Lnet/minecraft/world/item/ItemStack;")
    )
    private static ItemStack decodePolymerStack(ItemStack stack, REIPackets.CreateItems payload,
                                               NetworkManager.PacketContext context) {
        return PolymerItemUtils.getRealItemStack(stack, PacketContext.get(), context.registryAccess());
    }
}
