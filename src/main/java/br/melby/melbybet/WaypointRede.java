package br.melby.melbybet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Pacotes entre o servidor e o jar de cliente do MelbyCraft (waypoints e contorno do /marcador). */
public final class WaypointRede {
    private WaypointRede() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> tipo(String nome) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("melbybet", nome));
    }

    /** Servidor -> cliente: cria, atualiza ou remove um waypoint (corpo, compartilhado). */
    public record PontoPayload(boolean remover, String chave, String nome, String dim, int x, int y, int z, String tipo, int cor) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PontoPayload> TYPE = tipo("waypoint");
        public static final StreamCodec<RegistryFriendlyByteBuf, PontoPayload> CODEC = StreamCodec.of(
            (b, v) -> {
                b.writeBoolean(v.remover);
                b.writeUtf(v.chave, 64);
                b.writeUtf(v.nome, 64);
                b.writeUtf(v.dim, 64);
                b.writeInt(v.x);
                b.writeInt(v.y);
                b.writeInt(v.z);
                b.writeUtf(v.tipo, 32);
                b.writeInt(v.cor);
            },
            b -> new PontoPayload(b.readBoolean(), b.readUtf(64), b.readUtf(64), b.readUtf(64), b.readInt(), b.readInt(), b.readInt(), b.readUtf(32), b.readInt()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Servidor -> cliente: a seleção atual do /marcador (para o cliente desenhar linhas em vez de partículas). */
    public record AreaPayload(String dim, boolean tem1, int ax, int ay, int az, boolean tem2, int bx, int by, int bz) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AreaPayload> TYPE = tipo("marcador_area");
        public static final StreamCodec<RegistryFriendlyByteBuf, AreaPayload> CODEC = StreamCodec.of(
            (b, v) -> {
                b.writeUtf(v.dim, 64);
                b.writeBoolean(v.tem1);
                b.writeInt(v.ax);
                b.writeInt(v.ay);
                b.writeInt(v.az);
                b.writeBoolean(v.tem2);
                b.writeInt(v.bx);
                b.writeInt(v.by);
                b.writeInt(v.bz);
            },
            b -> new AreaPayload(b.readUtf(64), b.readBoolean(), b.readInt(), b.readInt(), b.readInt(), b.readBoolean(), b.readInt(), b.readInt(), b.readInt()));

        public static AreaPayload vazia() {
            return new AreaPayload("overworld", false, 0, 0, 0, false, 0, 0, 0);
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Servidor -> cliente: se os waypoints estão liberados neste servidor. */
    public record ConfigPayload(boolean waypoints) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ConfigPayload> TYPE = tipo("waypoint_config");
        public static final StreamCodec<RegistryFriendlyByteBuf, ConfigPayload> CODEC = StreamCodec.of(
            (b, v) -> b.writeBoolean(v.waypoints),
            b -> new ConfigPayload(b.readBoolean()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Cliente -> servidor: compartilhar um waypoint no chat (todo mundo vê um botão para adicionar). */
    public record CompartilharPayload(String nome, String dim, int x, int y, int z) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CompartilharPayload> TYPE = tipo("waypoint_compartilhar");
        public static final StreamCodec<RegistryFriendlyByteBuf, CompartilharPayload> CODEC = StreamCodec.of(
            (b, v) -> {
                b.writeUtf(v.nome, 64);
                b.writeUtf(v.dim, 64);
                b.writeInt(v.x);
                b.writeInt(v.y);
                b.writeInt(v.z);
            },
            b -> new CompartilharPayload(b.readUtf(64), b.readUtf(64), b.readInt(), b.readInt(), b.readInt()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
