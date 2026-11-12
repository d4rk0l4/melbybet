package br.melby.melbybet;

import br.melby.melbybet.core.Marcos;
import br.melby.melbybet.core.StatCat;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/** Comemora no chat quando alguém completa 5, 10, 25, 50, 100... horas de servidor. */
public final class MarcosOnline {
    private MarcosOnline() {}

    /** Chamado a cada alguns minutos. */
    public static void conferir() {
        if (Jogo.server == null) return;
        Map<String, JsonObject> stats = Jogo.statsTodos();
        boolean mudou = false;
        for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) {
            JsonObject s = stats.get(p.getGameProfile().name());
            if (s == null) continue;
            int marco = Marcos.maiorAte(StatCat.TEMPO.valor(s));
            String id = p.getUUID().toString();
            Integer ultimo = Jogo.casa.marcos.get(id);
            if (ultimo == null) {
                // primeira vez que o mod vê esse jogador: só registra, sem comemorar o passado
                Jogo.casa.marcos.put(id, marco);
                mudou = true;
                continue;
            }
            if (marco > ultimo) {
                Jogo.casa.marcos.put(id, marco);
                mudou = true;
                String nome = p.getGameProfile().name();
                Component msg = Component.literal("★ ").withStyle(st -> st.withColor(0xFFD700))
                    .append(Prefixos.degrade(nome + " completou " + marco + " horas de servidor! Parabéns!", 0xFFD700, 0xFF8A24));
                Jogo.server.getPlayerList().broadcastSystemMessage(msg, false);
                Jogo.som(p, "minecraft:entity.player.levelup", 1.0f);
                TelasServidor.avisoTodos("Marco de horas", nome + " completou " + marco + " horas!");
                Jogo.webhook("⏱️ **" + nome + "** completou **" + marco + " horas** de servidor!");
            }
        }
        if (mudou) Jogo.salvar();
    }
}
