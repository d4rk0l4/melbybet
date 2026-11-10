package br.melby.melbybet;

import br.melby.melbybet.core.Bolao;
import br.melby.melbybet.core.Casa;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Telas do cliente (apostas, gestos, duelo, cor), avisos no canto da tela e painel do HUD. */
public final class TelasServidor {
    private TelasServidor() {}

    private static final Map<UUID, Long> ULTIMA_APOSTA = new HashMap<>();
    private static final Map<UUID, List<String>> ULTIMO_PAINEL = new HashMap<>();
    private static int contador;

    public static void registrar() {
        PayloadTypeRegistry.clientboundPlay().register(TelasRede.AbrirPayload.TYPE, TelasRede.AbrirPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TelasRede.BoloesPayload.TYPE, TelasRede.BoloesPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TelasRede.AvisoPayload.TYPE, TelasRede.AvisoPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TelasRede.PainelPayload.TYPE, TelasRede.PainelPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TelasRede.PedirPayload.TYPE, TelasRede.PedirPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TelasRede.ApostarPayload.TYPE, TelasRede.ApostarPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TelasRede.PedirPayload.TYPE, (v, ctx) -> {
            if (v.o().equals("boloes")) enviarBoloes(ctx.player());
        });
        ServerPlayNetworking.registerGlobalReceiver(TelasRede.ApostarPayload.TYPE, (v, ctx) -> {
            ServerPlayer p = ctx.player();
            long agora = System.currentTimeMillis();
            Long antes = ULTIMA_APOSTA.get(p.getUUID());
            if (antes != null && agora - antes < 700) return;
            ULTIMA_APOSTA.put(p.getUUID(), agora);
            Bolao b = Jogo.casa.achar(v.id());
            if (b == null) {
                p.sendSystemMessage(Jogo.prefixo("Esse bolão não existe mais."));
            } else {
                String erro = Jogo.apostar(p, b, v.opcao(), v.valor());
                if (erro != null) p.sendSystemMessage(Jogo.prefixo(erro));
            }
            enviarBoloes(p);
        });
    }

    public static void aoSair(ServerPlayer p) {
        ULTIMA_APOSTA.remove(p.getUUID());
        ULTIMO_PAINEL.remove(p.getUUID());
    }

    public static boolean temJar(ServerPlayer p) {
        return ServerPlayNetworking.canSend(p, TelasRede.AbrirPayload.TYPE);
    }

    /** Abre a tela no cliente. Devolve false se o jogador não tem o jar (aí o servidor abre o menu de baú). */
    public static boolean abrir(ServerPlayer p, String tela) {
        if (!temJar(p)) return false;
        if (tela.equals("bet")) enviarBoloes(p);
        ServerPlayNetworking.send(p, new TelasRede.AbrirPayload(tela));
        return true;
    }

    public static void enviarBoloes(ServerPlayer p) {
        if (!temJar(p)) return;
        long agora = System.currentTimeMillis();
        String uuid = p.getUUID().toString();
        List<TelasRede.BolaoInfo> l = new ArrayList<>();
        for (Bolao b : Jogo.casa.ativos()) {
            List<Integer> potes = new ArrayList<>();
            for (String o : b.opcoes) potes.add(b.poteDa(o));
            long resta = b.expiraEm > 0 ? Math.max(0, b.expiraEm - agora) : -1;
            l.add(new TelasRede.BolaoInfo(b.id, b.titulo, b.status.name(), new ArrayList<>(b.opcoes), potes, b.apostadoPor(uuid), b.maxAposta, resta));
        }
        ServerPlayNetworking.send(p, new TelasRede.BoloesPayload(Jogo.contar(p), (int) Jogo.casa.cofre, (int) Jogo.casa.jackpot, l));
    }

    /** Aviso no canto da tela (só para quem tem o jar). */
    public static void aviso(ServerPlayer p, String titulo, String texto) {
        if (p == null || !temJar(p)) return;
        ServerPlayNetworking.send(p, new TelasRede.AvisoPayload(titulo, texto));
    }

    /** A cada segundo: a cada 5, atualiza o painel do HUD de quem tem o jar. */
    public static void aCadaSegundo() {
        if (Jogo.server == null || ++contador % 5 != 0) return;
        long agora = System.currentTimeMillis();
        for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) {
            if (!temJar(p)) continue;
            String uuid = p.getUUID().toString();
            List<String> linhas = new ArrayList<>();
            for (Bolao b : Jogo.casa.ativos()) {
                int meu = b.apostadoPor(uuid);
                if (meu <= 0) continue;
                String resta = b.expiraEm > 0 ? " (" + Math.max(0, (b.expiraEm - agora) / 60000) + " min)" : "";
                linhas.add("Bolão #" + b.id + ": " + meu + " 💎" + resta);
                if (linhas.size() >= 3) break;
            }
            Casa.Duelo x = Jogo.casa.duelAtivoDe(uuid);
            if (x != null) linhas.add("Duelo: " + x.nomeA + " x " + x.nomeB);
            for (Casa.Corpo c : Jogo.casa.corposDe(uuid)) {
                long resta = Jogo.casa.config.corpoProtecaoSeg * 1000L - (agora - c.quando);
                if (resta > 0) {
                    linhas.add("Seu corpo: protegido " + (resta + 999) / 1000 + "s");
                    break;
                }
            }
            List<String> antes = ULTIMO_PAINEL.get(p.getUUID());
            if (linhas.equals(antes) || (linhas.isEmpty() && antes == null)) continue;
            ULTIMO_PAINEL.put(p.getUUID(), linhas);
            ServerPlayNetworking.send(p, new TelasRede.PainelPayload(linhas));
        }
    }
}
