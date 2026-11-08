package br.melby.melbybet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** O cliente avisa o servidor que tem o jar e qual versão. */
public final class JarRede {
    private JarRede() {}

    public record OlaPayload(String versao) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OlaPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("melbybet", "ola"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OlaPayload> CODEC = StreamCodec.of((b, v) -> b.writeUtf(v.versao, 40), b -> new OlaPayload(b.readUtf(40)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
