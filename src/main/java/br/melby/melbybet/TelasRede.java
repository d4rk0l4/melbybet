package br.melby.melbybet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Pacotes das telas do cliente: apostas, avisos no canto da tela e painel do HUD. */
public final class TelasRede {
    private TelasRede() {}

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> tipo(String nome) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("melbybet", nome));
    }

    /** Servidor -> cliente: abra esta tela (bet, gestos, duelo, cor). */
    public record AbrirPayload(String tela) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AbrirPayload> TYPE = tipo("abrir_tela");
        public static final StreamCodec<RegistryFriendlyByteBuf, AbrirPayload> CODEC = StreamCodec.of((b, v) -> b.writeUtf(v.tela, 20), b -> new AbrirPayload(b.readUtf(20)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record BolaoInfo(int id, String titulo, String status, List<String> opcoes, List<Integer> potes, int meu, int maxAposta, long restanteMs) {}

    public record BoloesPayload(int diamantes, int cofre, int jackpot, List<BolaoInfo> boloes) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BoloesPayload> TYPE = tipo("boloes");
        public static final StreamCodec<RegistryFriendlyByteBuf, BoloesPayload> CODEC = StreamCodec.of(
            (b, v) -> {
                b.writeVarInt(v.diamantes);
                b.writeVarInt(v.cofre);
                b.writeVarInt(v.jackpot);
                b.writeVarInt(v.boloes.size());
                for (BolaoInfo i : v.boloes) {
                    b.writeVarInt(i.id);
                    b.writeUtf(i.titulo, 100);
                    b.writeUtf(i.status, 20);
                    b.writeVarInt(i.opcoes.size());
                    for (int k = 0; k < i.opcoes.size(); k++) {
                        b.writeUtf(i.opcoes.get(k), 60);
                        b.writeVarInt(i.potes.get(k));
                    }
                    b.writeVarInt(i.meu);
                    b.writeVarInt(i.maxAposta);
                    b.writeLong(i.restanteMs);
                }
            },
            b -> {
                int dia = b.readVarInt(), cofre = b.readVarInt(), jack = b.readVarInt();
                int n = Math.min(b.readVarInt(), 30);
                List<BolaoInfo> l = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    int id = b.readVarInt();
                    String t = b.readUtf(100), st = b.readUtf(20);
                    int m = Math.min(b.readVarInt(), 20);
                    List<String> ops = new ArrayList<>();
                    List<Integer> potes = new ArrayList<>();
                    for (int k = 0; k < m; k++) {
                        ops.add(b.readUtf(60));
                        potes.add(b.readVarInt());
                    }
                    l.add(new BolaoInfo(id, t, st, ops, potes, b.readVarInt(), b.readVarInt(), b.readLong()));
                }
                return new BoloesPayload(dia, cofre, jack, l);
            });

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Cliente -> servidor: mande os bolões (o que é "boloes"). */
    public record PedirPayload(String o) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PedirPayload> TYPE = tipo("telas_pedir");
        public static final StreamCodec<RegistryFriendlyByteBuf, PedirPayload> CODEC = StreamCodec.of((b, v) -> b.writeUtf(v.o, 20), b -> new PedirPayload(b.readUtf(20)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ApostarPayload(int id, String opcao, int valor) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ApostarPayload> TYPE = tipo("apostar");
        public static final StreamCodec<RegistryFriendlyByteBuf, ApostarPayload> CODEC = StreamCodec.of(
            (b, v) -> {
                b.writeVarInt(v.id);
                b.writeUtf(v.opcao, 60);
                b.writeVarInt(v.valor);
            },
            b -> new ApostarPayload(b.readVarInt(), b.readUtf(60), b.readVarInt()));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Servidor -> cliente: aviso no canto da tela. */
    public record AvisoPayload(String titulo, String texto) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AvisoPayload> TYPE = tipo("aviso");
        public static final StreamCodec<RegistryFriendlyByteBuf, AvisoPayload> CODEC = StreamCodec.of(
            (b, v) -> {
                b.writeUtf(v.titulo, 60);
                b.writeUtf(v.texto, 200);
            },
            b -> new AvisoPayload(b.readUtf(60), b.readUtf(200)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Servidor -> cliente: linhas do painel do HUD (vazio esconde). */
    public record PainelPayload(List<String> linhas) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PainelPayload> TYPE = tipo("painel");
        public static final StreamCodec<RegistryFriendlyByteBuf, PainelPayload> CODEC = StreamCodec.of(
            (b, v) -> {
                b.writeVarInt(v.linhas.size());
                for (String s : v.linhas) b.writeUtf(s, 120);
            },
            b -> {
                int n = Math.min(b.readVarInt(), 8);
                List<String> l = new ArrayList<>();
                for (int i = 0; i < n; i++) l.add(b.readUtf(120));
                return new PainelPayload(l);
            });

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
