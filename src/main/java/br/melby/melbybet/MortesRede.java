package br.melby.melbybet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Pacotes do histórico de mortes (jar de cliente). */
public final class MortesRede {
    private MortesRede() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> novoTipo(String nome) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("melbybet", nome));
    }

    /** Uma morte. corpoId só vale se o corpo ainda existe (recuperado = false). */
    public record Morte(int corpoId, String dim, int x, int y, int z, long quando, int itens, boolean recuperado, long protegidoMs) {}

    /** Servidor -> cliente: suas últimas mortes, da mais nova para a mais antiga. */
    public record ListaPayload(String nome, List<Morte> mortes) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ListaPayload> TYPE = novoTipo("mortes_lista");
        public static final StreamCodec<RegistryFriendlyByteBuf, ListaPayload> CODEC = StreamCodec.of(
            (b, v) -> {
                b.writeUtf(v.nome, 40);
                b.writeVarInt(v.mortes.size());
                for (Morte m : v.mortes) {
                    b.writeVarInt(m.corpoId);
                    b.writeUtf(m.dim, 60);
                    b.writeInt(m.x);
                    b.writeInt(m.y);
                    b.writeInt(m.z);
                    b.writeLong(m.quando);
                    b.writeVarInt(m.itens);
                    b.writeBoolean(m.recuperado);
                    b.writeLong(m.protegidoMs);
                }
            },
            b -> {
                String nome = b.readUtf(40);
                int n = Math.min(b.readVarInt(), 30);
                List<Morte> l = new ArrayList<>();
                for (int i = 0; i < n; i++) l.add(new Morte(b.readVarInt(), b.readUtf(60), b.readInt(), b.readInt(), b.readInt(), b.readLong(), b.readVarInt(), b.readBoolean(), b.readLong()));
                return new ListaPayload(nome, l);
            });

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Cliente -> servidor: mande minha lista (corpoId 0) ou abra os itens deste corpo (corpoId > 0). */
    public record PedirPayload(int corpoId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PedirPayload> TYPE = novoTipo("mortes_pedir");
        public static final StreamCodec<RegistryFriendlyByteBuf, PedirPayload> CODEC = StreamCodec.of(
            (b, v) -> b.writeVarInt(v.corpoId),
            b -> new PedirPayload(b.readVarInt()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
