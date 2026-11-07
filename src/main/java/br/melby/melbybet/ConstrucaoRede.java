package br.melby.melbybet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Pacotes das ferramentas de construção do cliente (lista de materiais, grade de mapa-arte, editores de armor stand e de placa). */
public final class ConstrucaoRede {
    private ConstrucaoRede() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> novoTipo(String nome) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("melbybet", nome));
    }

    private static void lista4(RegistryFriendlyByteBuf b, List<String> l) {
        for (int i = 0; i < 4; i++) b.writeUtf(i < l.size() ? l.get(i) : "", 200);
    }

    private static List<String> le4(RegistryFriendlyByteBuf b) {
        List<String> l = new ArrayList<>();
        for (int i = 0; i < 4; i++) l.add(b.readUtf(200));
        return l;
    }

    /** Servidor -> cliente: um projeto do /catalogo para acompanhar na tela (nome vazio = parar). */
    public record MateriaisPayload(String nome, List<String> blocos, List<String> itens, List<Long> qtds) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MateriaisPayload> TYPE = novoTipo("materiais");
        public static final StreamCodec<RegistryFriendlyByteBuf, MateriaisPayload> CODEC = StreamCodec.of(
            (b, v) -> {
                b.writeUtf(v.nome, 64);
                b.writeVarInt(v.blocos.size());
                for (int i = 0; i < v.blocos.size(); i++) {
                    b.writeUtf(v.blocos.get(i), 100);
                    b.writeUtf(v.itens.get(i), 100);
                    b.writeLong(v.qtds.get(i));
                }
            },
            b -> {
                String nome = b.readUtf(64);
                int n = Math.min(b.readVarInt(), 300);
                List<String> bl = new ArrayList<>(), it = new ArrayList<>();
                List<Long> q = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    bl.add(b.readUtf(100));
                    it.add(b.readUtf(100));
                    q.add(b.readLong());
                }
                return new MateriaisPayload(nome, bl, it, q);
            });

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Servidor -> cliente: a grade de mapas (colunas x linhas, escala) para desenhar numerada no mundo. */
    public record MapArtPayload(boolean ativo, String dim, int escala, int x0, int z0, int cols, int linhas, int y) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MapArtPayload> TYPE = novoTipo("mapart_grade");
        public static final StreamCodec<RegistryFriendlyByteBuf, MapArtPayload> CODEC = StreamCodec.of(
            (b, v) -> {
                b.writeBoolean(v.ativo);
                b.writeUtf(v.dim, 64);
                b.writeInt(v.escala);
                b.writeInt(v.x0);
                b.writeInt(v.z0);
                b.writeInt(v.cols);
                b.writeInt(v.linhas);
                b.writeInt(v.y);
            },
            b -> new MapArtPayload(b.readBoolean(), b.readUtf(64), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readInt()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Cliente -> servidor: mudança num armor stand. tipo 0 = ângulo de uma parte (x, y, z em graus), 1 = braços, 2 = base,
     * 3 = invisível (flag), 4 = giro do corpo (x = graus).
     */
    public record PosePayload(int entidade, int tipo, int parte, float x, float y, float z, boolean flag) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PosePayload> TYPE = novoTipo("armorstand_pose");
        public static final StreamCodec<RegistryFriendlyByteBuf, PosePayload> CODEC = StreamCodec.of(
            (b, v) -> {
                b.writeInt(v.entidade);
                b.writeVarInt(v.tipo);
                b.writeVarInt(v.parte);
                b.writeFloat(v.x);
                b.writeFloat(v.y);
                b.writeFloat(v.z);
                b.writeBoolean(v.flag);
            },
            b -> new PosePayload(b.readInt(), b.readVarInt(), b.readVarInt(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Cliente -> servidor: quero editar a placa nesta posição (mandar o texto e o histórico). */
    public record PlacaPedirPayload(int x, int y, int z) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PlacaPedirPayload> TYPE = novoTipo("placa_pedir");
        public static final StreamCodec<RegistryFriendlyByteBuf, PlacaPedirPayload> CODEC = StreamCodec.of(
            (b, v) -> {
                b.writeInt(v.x);
                b.writeInt(v.y);
                b.writeInt(v.z);
            },
            b -> new PlacaPedirPayload(b.readInt(), b.readInt(), b.readInt()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record VersaoPlaca(int id, String rotulo, List<String> linhas) {}

    /** Servidor -> cliente: o texto atual da placa (com os códigos de cor) e as versões anteriores. */
    public record PlacaDadosPayload(int x, int y, int z, List<String> linhas, boolean encerada, List<VersaoPlaca> versoes) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PlacaDadosPayload> TYPE = novoTipo("placa_dados");
        public static final StreamCodec<RegistryFriendlyByteBuf, PlacaDadosPayload> CODEC = StreamCodec.of(
            (b, v) -> {
                b.writeInt(v.x);
                b.writeInt(v.y);
                b.writeInt(v.z);
                lista4(b, v.linhas);
                b.writeBoolean(v.encerada);
                b.writeVarInt(v.versoes.size());
                for (VersaoPlaca vp : v.versoes) {
                    b.writeInt(vp.id());
                    b.writeUtf(vp.rotulo(), 100);
                    lista4(b, vp.linhas());
                }
            },
            b -> {
                int x = b.readInt(), y = b.readInt(), z = b.readInt();
                List<String> l = le4(b);
                boolean enc = b.readBoolean();
                int n = Math.min(b.readVarInt(), 30);
                List<VersaoPlaca> vs = new ArrayList<>();
                for (int i = 0; i < n; i++) vs.add(new VersaoPlaca(b.readInt(), b.readUtf(100), le4(b)));
                return new PlacaDadosPayload(x, y, z, l, enc, vs);
            });

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Cliente -> servidor: escrever estas 4 linhas (com códigos de cor) na placa. */
    public record PlacaAplicarPayload(int x, int y, int z, List<String> linhas) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PlacaAplicarPayload> TYPE = novoTipo("placa_aplicar");
        public static final StreamCodec<RegistryFriendlyByteBuf, PlacaAplicarPayload> CODEC = StreamCodec.of(
            (b, v) -> {
                b.writeInt(v.x);
                b.writeInt(v.y);
                b.writeInt(v.z);
                lista4(b, v.linhas);
            },
            b -> new PlacaAplicarPayload(b.readInt(), b.readInt(), b.readInt(), le4(b)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Cliente -> servidor: voltar a placa para uma versão do histórico. */
    public record PlacaRestaurarPayload(int x, int y, int z, int id) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PlacaRestaurarPayload> TYPE = novoTipo("placa_restaurar");
        public static final StreamCodec<RegistryFriendlyByteBuf, PlacaRestaurarPayload> CODEC = StreamCodec.of(
            (b, v) -> {
                b.writeInt(v.x);
                b.writeInt(v.y);
                b.writeInt(v.z);
                b.writeInt(v.id);
            },
            b -> new PlacaRestaurarPayload(b.readInt(), b.readInt(), b.readInt(), b.readInt()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
