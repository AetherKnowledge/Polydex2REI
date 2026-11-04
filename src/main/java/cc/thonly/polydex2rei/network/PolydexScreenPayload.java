package cc.thonly.polydex2rei.network;

import cc.thonly.polydex2rei.Polydex2REI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PolydexScreenPayload(int containerId, boolean open, StackActionPayload query) implements CustomPacketPayload {
    public static final Identifier ID = Polydex2REI.id("polydex_screen");
    public static final Type<PolydexScreenPayload> PACKET_ID = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, PolydexScreenPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PolydexScreenPayload::containerId,
            ByteBufCodecs.BOOL, PolydexScreenPayload::open,
            StackActionPayload.CODEC, PolydexScreenPayload::query,
            PolydexScreenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}
