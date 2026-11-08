package br.melby.melbybet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Pacotes da tela de estatísticas (jar de cliente). */
public final class EstatRede {
    private EstatRede() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> novoTipo(String nome) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("melbybet", nome));
    }

    /** Uma categoria: o seu valor, a sua posição e as primeiras linhas do ranking (nome e valor já formatados). */
    public record Cat(String rotulo, String icone, String meu, int pos, int total, List<String> nomes, List<String> valores) {}

    public record DadosPayload(String nome, String naGangueHa, String tempoOnline, List<Cat> cats) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<DadosPayload> TYPE = novoTipo("estat_dados");
        public static final StreamCodec<RegistryFriendlyByteBuf, DadosPayload> CODEC = StreamCodec.of(
            (b, v) -> {
                b.writeUtf(v.nome, 40);
                b.writeUtf(v.naGangueHa, 60);
                b.writeUtf(v.tempoOnline, 60);
                b.writeVarInt(v.cats.size());
                for (Cat c : v.cats) {
                    b.writeUtf(c.rotulo, 60);
                    b.writeUtf(c.icone, 60);
                    b.writeUtf(c.meu, 60);
                    b.writeVarInt(c.pos);
                    b.writeVarInt(c.total);
                    b.writeVarInt(c.nomes.size());
                    for (int i = 0; i < c.nomes.size(); i++) {
                        b.writeUtf(c.nomes.get(i), 40);
                        b.writeUtf(c.valores.get(i), 60);
                    }
                }
            },
            b -> {
                String nome = b.readUtf(40), gangue = b.readUtf(60), online = b.readUtf(60);
                int n = Math.min(b.readVarInt(), 40);
                List<Cat> cats = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    String rot = b.readUtf(60), ic = b.readUtf(60), meu = b.readUtf(60);
                    int pos = b.readVarInt(), total = b.readVarInt();
                    int m = Math.min(b.readVarInt(), 20);
                    List<String> nomes = new ArrayList<>(), vals = new ArrayList<>();
                    for (int k = 0; k < m; k++) {
                        nomes.add(b.readUtf(40));
                        vals.add(b.readUtf(60));
                    }
                    cats.add(new Cat(rot, ic, meu, pos, total, nomes, vals));
                }
                return new DadosPayload(nome, gangue, online, cats);
            });

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Cliente -> servidor: mande as minhas estatísticas. */
    public record PedirPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PedirPayload> TYPE = novoTipo("estat_pedir");
        public static final StreamCodec<RegistryFriendlyByteBuf, PedirPayload> CODEC = StreamCodec.of((b, v) -> {}, b -> new PedirPayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
