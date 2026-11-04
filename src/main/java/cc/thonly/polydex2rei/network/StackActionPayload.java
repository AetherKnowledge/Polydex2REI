package cc.thonly.polydex2rei.network;

import cc.thonly.polydex2rei.Polydex2REI;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

@Slf4j
public record StackActionPayload(Action action, String id) implements CustomPacketPayload {
    public static final Identifier ID = Polydex2REI.id("stack_action");
    public static final Type<StackActionPayload> PACKET_ID = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, StackActionPayload> CODEC = StreamCodec.ofMember(
            StackActionPayload::write,
            StackActionPayload::read);

    private static StackActionPayload read(RegistryFriendlyByteBuf buf) {
        try {
            return new StackActionPayload(Action.get(buf.readInt()), buf.readUtf());
        } catch (Exception e) {
            log.error("Can't read Stack Action Payload: ", e);
            return new StackActionPayload(Action.LEFT, "minecraft:stone");
        }
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.action.getId());
        buf.writeUtf(this.id);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}