package br.melby.melbybet;

import br.melby.melbybet.core.ContagemBlocos;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import java.util.ArrayList;
import java.util.List;

/** Servidor das ferramentas de construção do cliente: registra os pacotes e valida tudo que o cliente pedir. */
public final class ConstrucaoServidor {
    private ConstrucaoServidor() {}

    public static void registrar() {
        PayloadTypeRegistry.clientboundPlay().register(ConstrucaoRede.MateriaisPayload.TYPE, ConstrucaoRede.MateriaisPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ConstrucaoRede.MapArtPayload.TYPE, ConstrucaoRede.MapArtPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ConstrucaoRede.PlacaDadosPayload.TYPE, ConstrucaoRede.PlacaDadosPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ConstrucaoRede.PosePayload.TYPE, ConstrucaoRede.PosePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ConstrucaoRede.PlacaPedirPayload.TYPE, ConstrucaoRede.PlacaPedirPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ConstrucaoRede.PlacaAplicarPayload.TYPE, ConstrucaoRede.PlacaAplicarPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ConstrucaoRede.PlacaRestaurarPayload.TYPE, ConstrucaoRede.PlacaRestaurarPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ConstrucaoRede.FotoPartePayload.TYPE, ConstrucaoRede.FotoPartePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ConstrucaoRede.FotoPartePayload.TYPE, (v, ctx) -> FotoMoldura.receberParte(ctx.player(), v));
        ServerPlayNetworking.registerGlobalReceiver(ConstrucaoRede.PosePayload.TYPE, (v, ctx) -> ArmorStands.aplicarDoCliente(ctx.player(), v));
        ServerPlayNetworking.registerGlobalReceiver(ConstrucaoRede.PlacaPedirPayload.TYPE, (v, ctx) -> HistoricoPlacas.enviarAoCliente(ctx.player(), new BlockPos(v.x(), v.y(), v.z())));
        ServerPlayNetworking.registerGlobalReceiver(ConstrucaoRede.PlacaAplicarPayload.TYPE, (v, ctx) -> HistoricoPlacas.aplicarDoCliente(ctx.player(), new BlockPos(v.x(), v.y(), v.z()), v.linhas()));
        ServerPlayNetworking.registerGlobalReceiver(ConstrucaoRede.PlacaRestaurarPayload.TYPE, (v, ctx) -> HistoricoPlacas.restaurarDoCliente(ctx.player(), new BlockPos(v.x(), v.y(), v.z()), v.id()));
    }

    public static boolean pode(ServerPlayer p, CustomPacketPayload.Type<?> tipo) {
        return ServerPlayNetworking.canSend(p, tipo);
    }

    public static void enviar(ServerPlayer p, CustomPacketPayload payload) {
        if (pode(p, payload.type())) ServerPlayNetworking.send(p, payload);
    }

    /** A placa nesta posição, se o jogador está perto dela. */
    static SignBlockEntity placaPerto(ServerPlayer p, BlockPos pos) {
        if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 8.0 * 8.0) return null;
        return p.level().getBlockEntity(pos) instanceof SignBlockEntity s ? s : null;
    }

    /** Manda a lista de blocos de um projeto para o cliente acompanhar na tela. Devolve false se ele não tem o jar de cliente. */
    public static boolean enviarMateriais(ServerPlayer p, ContagemBlocos.Projeto pr) {
        if (!pode(p, ConstrucaoRede.MateriaisPayload.TYPE)) return false;
        List<String> blocos = new ArrayList<>(), itens = new ArrayList<>();
        List<Long> qtds = new ArrayList<>();
        for (var e : pr.contagem.ordenado()) {
            if (blocos.size() >= 150) break;
            blocos.add(e.getKey());
            itens.add(Catalogo.itemDe(e.getKey()));
            qtds.add(e.getValue());
        }
        ServerPlayNetworking.send(p, new ConstrucaoRede.MateriaisPayload(pr.nome.isEmpty() ? "Contagem" : pr.nome, blocos, itens, qtds));
        return true;
    }

    public static void limparMateriais(ServerPlayer p) {
        enviar(p, new ConstrucaoRede.MateriaisPayload("", List.of(), List.of(), List.of()));
    }
}
