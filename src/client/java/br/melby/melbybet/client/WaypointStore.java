package br.melby.melbybet.client;

import br.melby.melbybet.WaypointRede;
import br.melby.melbybet.core.Waypoints;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

/** Os waypoints do servidor em que o jogador está (arquivo em config/melbybet/waypoints/), mais o estado recebido do servidor. */
public final class WaypointStore {
    private WaypointStore() {}

    public static Waypoints dados = new Waypoints();
    private static Path arquivo;
    /** O servidor pode desligar os waypoints (o OP decide); sem o mod no servidor, fica liberado. */
    public static boolean liberado = true;
    /** Seleção atual do /marcador, recebida do servidor (null = nada para desenhar). */
    public static WaypointRede.AreaPayload area;

    public static String dimAtual(Minecraft mc) {
        if (mc.level == null) return "overworld";
        String s = mc.level.dimension().toString();
        int a = s.lastIndexOf(':');
        int b = s.lastIndexOf(']');
        return a >= 0 && b > a ? s.substring(a + 1, b) : "overworld";
    }

    public static void carregar(Minecraft mc) {
        String chave = "singleplayer";
        var srv = mc.getCurrentServer();
        if (srv != null && srv.ip != null) chave = srv.ip.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
        arquivo = FabricLoader.getInstance().getConfigDir().resolve("melbybet").resolve("waypoints").resolve(chave + ".json");
        dados = Waypoints.ler(arquivo);
        liberado = true;
        area = null;
        // corpos e compartilhados antigos não valem em outra sessão: o servidor manda de novo os que ainda existem
        dados.pontos.removeIf(p -> p.automatico());
    }

    public static void salvar() {
        if (arquivo == null) return;
        try {
            dados.gravar(arquivo);
        } catch (Exception e) {
            // sem disco: perde só esta gravação
        }
    }

    public static void descarregar() {
        salvar();
        arquivo = null;
        area = null;
    }

    public static boolean ativo() {
        return liberado && arquivo != null;
    }

    /** Mensagem curta acima da hotbar. */
    public static void aviso(String texto) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendOverlayMessage(Component.literal(texto));
    }
}
