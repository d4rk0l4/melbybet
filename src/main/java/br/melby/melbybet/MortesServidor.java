package br.melby.melbybet;

import br.melby.melbybet.core.Casa;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Histórico de mortes de cada jogador para a tela do cliente. */
public final class MortesServidor {
    private MortesServidor() {}

    private static final int MAX_POR_JOGADOR = 10;

    public static void registrar() {
        PayloadTypeRegistry.clientboundPlay().register(MortesRede.ListaPayload.TYPE, MortesRede.ListaPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MortesRede.PedirPayload.TYPE, MortesRede.PedirPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(MortesRede.PedirPayload.TYPE, (v, ctx) -> {
            ServerPlayer p = ctx.player();
            if (v.corpoId() <= 0) {
                enviar(p);
                return;
            }
            Casa.Corpo c = Jogo.casa.corpo(v.corpoId());
            if (c == null || !c.uuid.equals(p.getUUID().toString())) return;
            MenuCorpo.abrir(p, c);
        });
    }

    /** Guarda a morte no histórico (chamado quando o corpo é criado). */
    public static void registrarMorte(ServerPlayer p, Casa.Corpo c) {
        Casa.Morte m = new Casa.Morte();
        m.corpoId = c.id;
        m.uuid = c.uuid;
        m.dim = c.dim;
        m.x = (int) Math.round(c.x);
        m.y = (int) Math.round(c.y);
        m.z = (int) Math.round(c.z);
        m.quando = c.quando;
        m.itens = c.itens.size();
        Jogo.casa.mortes.add(m);
        int n = 0;
        for (int i = Jogo.casa.mortes.size() - 1; i >= 0; i--) {
            if (!Jogo.casa.mortes.get(i).uuid.equals(c.uuid)) continue;
            if (++n > MAX_POR_JOGADOR) Jogo.casa.mortes.remove(i);
        }
        enviar(p);
    }

    public static void enviar(ServerPlayer p) {
        if (!ServerPlayNetworking.canSend(p, MortesRede.ListaPayload.TYPE)) return;
        String uuid = p.getUUID().toString();
        List<MortesRede.Morte> l = new ArrayList<>();
        long agora = System.currentTimeMillis();
        for (int i = Jogo.casa.mortes.size() - 1; i >= 0; i--) {
            Casa.Morte m = Jogo.casa.mortes.get(i);
            if (!m.uuid.equals(uuid)) continue;
            Casa.Corpo c = m.recuperado ? null : Jogo.casa.corpo(m.corpoId);
            boolean rec = c == null;
            long prot = rec ? 0 : Math.max(0, Jogo.casa.config.corpoProtecaoSeg * 1000L - (agora - m.quando));
            l.add(new MortesRede.Morte(m.corpoId, m.dim, m.x, m.y, m.z, m.quando, rec ? 0 : c.itens.size(), rec, prot));
        }
        ServerPlayNetworking.send(p, new MortesRede.ListaPayload(p.getGameProfile().name(), l));
    }

    /** O corpo foi esvaziado ou mexido: atualiza a tela de quem estiver com ela aberta. */
    public static void corpoMudou(Casa.Corpo c, boolean vazio) {
        if (vazio) {
            for (Casa.Morte m : Jogo.casa.mortes) if (m.corpoId == c.id && m.uuid.equals(c.uuid)) m.recuperado = true;
        }
        if (Jogo.server == null) return;
        ServerPlayer dono = Jogo.server.getPlayerList().getPlayer(java.util.UUID.fromString(c.uuid));
        if (dono != null) enviar(dono);
    }
}
