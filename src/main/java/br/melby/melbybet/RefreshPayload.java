package br.melby.melbybet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Pedido do cliente (botão da tela do villager) para trocar as ofertas, como quebrar e repor a bancada. */
public record RefreshPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RefreshPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("melbybet", "refresh_villager"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RefreshPayload> CODEC = StreamCodec.unit(new RefreshPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
