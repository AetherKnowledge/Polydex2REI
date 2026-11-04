package cc.thonly.polydex2rei.network;

import cc.thonly.polydex2rei.Polydex2REI;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

@Slf4j
public record NoticeOpenInventory() implements CustomPacketPayload {
    public static final Identifier ID = Polydex2REI.id("open_inventory");
    public static final Type<NoticeOpenInventory> PACKET_ID = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, NoticeOpenInventory> CODEC = StreamCodec.ofMember(
            NoticeOpenInventory::write,
            NoticeOpenInventory::read);

    private static NoticeOpenInventory read(RegistryFriendlyByteBuf buf) {
        return new NoticeOpenInventory();
    }

    private void write(RegistryFriendlyByteBuf buf) {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}