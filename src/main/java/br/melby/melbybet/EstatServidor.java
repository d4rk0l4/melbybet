package br.melby.melbybet;

import br.melby.melbybet.core.Casa;
import br.melby.melbybet.core.Fmt;
import br.melby.melbybet.core.Ranking;
import br.melby.melbybet.core.StatCat;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Manda as estatísticas do jogador e o ranking da Gangue para a tela do cliente. */
public final class EstatServidor {
    private EstatServidor() {}

    private static final int LINHAS = 10;
    private static final Map<java.util.UUID, Long> ULTIMO = new java.util.HashMap<>();

    public static void registrar() {
        PayloadTypeRegistry.clientboundPlay().register(EstatRede.DadosPayload.TYPE, EstatRede.DadosPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(EstatRede.PedirPayload.TYPE, EstatRede.PedirPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(EstatRede.PedirPayload.TYPE, (v, ctx) -> enviar(ctx.player()));
    }

    public static void aoSair(ServerPlayer p) {
        ULTIMO.remove(p.getUUID());
    }

    private static void enviar(ServerPlayer p) {
        long agora = System.currentTimeMillis();
        Long antes = ULTIMO.get(p.getUUID());
        if (antes != null && agora - antes < 2000) return;
        ULTIMO.put(p.getUUID(), agora);
        String nome = p.getGameProfile().name();
        Map<String, JsonObject> stats = Jogo.statsTodos();
        JsonObject meu = stats.get(nome);
        List<EstatRede.Cat> cats = new ArrayList<>();
        for (StatCat c : StatCat.values()) {
            List<Ranking.Linha> rank = Ranking.montar(c, stats);
            int pos = 0;
            for (int i = 0; i < rank.size(); i++) if (rank.get(i).nome.equalsIgnoreCase(nome)) pos = i + 1;
            List<String> nomes = new ArrayList<>(), vals = new ArrayList<>();
            for (int i = 0; i < Math.min(LINHAS, rank.size()); i++) {
                nomes.add(rank.get(i).nome);
                vals.add(c.formatar(rank.get(i).valor));
            }
            cats.add(new EstatRede.Cat(c.rotulo, icone(c), meu == null ? "0" : c.formatar(c.valor(meu)), pos, rank.size(), nomes, vals));
        }
        Casa.Visto v = Jogo.casa.vistos.get(nome.toLowerCase());
        String gangue = v != null && v.primeira > 0 ? Fmt.duracao(agora - v.primeira) : "";
        String online = meu == null ? "" : StatCat.TEMPO.formatar(StatCat.TEMPO.valor(meu));
        ServerPlayNetworking.send(p, new EstatRede.DadosPayload(nome, gangue, online, cats));
    }

    private static String icone(StatCat c) {
        return BuiltInRegistries.ITEM.getKey(MenuTop.icone(c)).getPath();
    }
}
