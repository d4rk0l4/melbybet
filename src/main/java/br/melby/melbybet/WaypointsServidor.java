package br.melby.melbybet;

import br.melby.melbybet.core.Casa;
import br.melby.melbybet.core.Waypoints;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Lado do servidor dos waypoints do cliente: avisa o cliente do corpo do jogador, repassa waypoints compartilhados no chat e
 * deixa o OP ligar ou desligar tudo. Os waypoints em si ficam no PC de cada jogador; o servidor só manda e recebe avisos.
 */
public final class WaypointsServidor {
    private WaypointsServidor() {}

    private static final int COR_CORPO = 0xFF5555;
    private static final int COR_COMPARTILHADO = 0x55FFFF;
    private static final Map<UUID, Long> ULTIMO_COMPARTILHAR = new HashMap<>();

    public static void registrar() {
        PayloadTypeRegistry.clientboundPlay().register(WaypointRede.PontoPayload.TYPE, WaypointRede.PontoPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WaypointRede.AreaPayload.TYPE, WaypointRede.AreaPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WaypointRede.ConfigPayload.TYPE, WaypointRede.ConfigPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(WaypointRede.CompartilharPayload.TYPE, WaypointRede.CompartilharPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(WaypointRede.CompartilharPayload.TYPE, (payload, ctx) -> compartilhar(ctx.player(), payload));
    }

    private static boolean pode(ServerPlayer p, CustomPacketPayload.Type<?> tipo) {
        return ServerPlayNetworking.canSend(p, tipo);
    }

    private static void enviar(ServerPlayer p, CustomPacketPayload payload) {
        if (pode(p, payload.type())) ServerPlayNetworking.send(p, payload);
    }

    // ---------- entrada e corpo ----------

    /** Ao entrar: diz se os waypoints estão liberados e manda de novo os corpos que ainda têm itens. */
    public static void aoEntrar(ServerPlayer p) {
        enviar(p, new WaypointRede.ConfigPayload(Jogo.casa.config.waypoints));
        for (Casa.Corpo c : Jogo.casa.corposDe(p.getUUID().toString())) enviarCorpo(p, c);
    }

    private static void enviarCorpo(ServerPlayer p, Casa.Corpo c) {
        enviar(p, new WaypointRede.PontoPayload(false, "corpo-" + c.id, "Corpo", c.dim, (int) Math.floor(c.x), (int) Math.floor(c.y), (int) Math.floor(c.z),
            Waypoints.CORPO, COR_CORPO));
    }

    public static void corpoCriado(ServerPlayer p, Casa.Corpo c) {
        enviarCorpo(p, c);
    }

    /** O corpo foi esvaziado: tira o waypoint de quem morreu (se estiver online). */
    public static void corpoRemovido(Casa.Corpo c) {
        if (Jogo.server == null) return;
        ServerPlayer dono = Jogo.server.getPlayerList().getPlayer(UUID.fromString(c.uuid));
        if (dono == null) return;
        enviar(dono, new WaypointRede.PontoPayload(true, "corpo-" + c.id, "", "overworld", 0, 0, 0, Waypoints.CORPO, 0));
    }

    /** Resultado do /bau buscar: marca os baús achados como waypoints temporários (amarelos) para quem tem o jar de cliente. */
    public static void marcarBuscaDeBaus(ServerPlayer p, java.util.List<br.melby.melbybet.core.EtiquetaBau.Etiqueta> achados) {
        if (!Jogo.casa.config.waypoints || !pode(p, WaypointRede.PontoPayload.TYPE)) return;
        enviar(p, new WaypointRede.PontoPayload(true, "busca", "", "overworld", 0, 0, 0, Waypoints.BUSCA, 0));
        int n = 0;
        for (var e : achados) {
            if (n++ >= 8) break;
            enviar(p, new WaypointRede.PontoPayload(false, "bau-" + e.dim + "-" + e.x + "-" + e.y + "-" + e.z, e.nome.isEmpty() ? "Baú" : e.nome,
                e.dim, e.x, e.y, e.z, Waypoints.BUSCA, 0xFFC928));
        }
    }

    public static void aoSair(ServerPlayer p) {
        ULTIMO_COMPARTILHAR.remove(p.getUUID());
    }

    // ---------- compartilhar ----------

    private static boolean dimValida(String dim) {
        return dim.length() >= 1 && dim.length() <= 30 && dim.matches("[a-z0-9_]+");
    }

    private static boolean coordValida(int x, int y, int z) {
        return Math.abs(x) <= 30_000_000 && Math.abs(z) <= 30_000_000 && y >= -2048 && y <= 4096;
    }

    private static String limpar(String nome) {
        String s = nome.replaceAll("\\p{Cntrl}", "").trim();
        return s.length() > Waypoints.MAX_NOME ? s.substring(0, Waypoints.MAX_NOME) : s;
    }

    public static MutableComponent botaoAdicionar(String nome, String dim, int x, int y, int z) {
        String cmd = "/waypoint adicionar " + dim + " " + x + " " + y + " " + z + " " + limpar(nome);
        return Component.literal("[Waypoint]").withStyle(s -> s.withColor(ChatFormatting.YELLOW).withBold(true)
            .withClickEvent(new ClickEvent.RunCommand(cmd))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Adiciona como waypoint (precisa do MelbyCraft no cliente)"))));
    }

    private static void compartilhar(ServerPlayer p, WaypointRede.CompartilharPayload v) {
        if (!Jogo.casa.config.waypoints || Jogo.server == null) return;
        String nome = limpar(v.nome());
        if (Waypoints.validarNome(nome) != null || !dimValida(v.dim()) || !coordValida(v.x(), v.y(), v.z())) return;
        long agora = System.currentTimeMillis();
        Long antes = ULTIMO_COMPARTILHAR.get(p.getUUID());
        if (antes != null && agora - antes < 3000) {
            p.sendSystemMessage(Jogo.prefixo("Calma: espere uns segundos para compartilhar outro waypoint."));
            return;
        }
        ULTIMO_COMPARTILHAR.put(p.getUUID(), agora);
        String xyz = v.x() + " " + v.y() + " " + v.z();
        Component msg = Component.literal(p.getGameProfile().name() + " compartilhou um waypoint: ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal(nome + " " + xyz + " (" + v.dim() + ") ").withStyle(ChatFormatting.WHITE))
            .append(botaoAdicionar(nome, v.dim(), v.x(), v.y(), v.z()))
            .append(Component.literal(" "))
            .append(Component.literal("[Copiar]").withStyle(s -> s.withColor(ChatFormatting.AQUA).withBold(true)
                .withClickEvent(new ClickEvent.CopyToClipboard(xyz))));
        Jogo.server.getPlayerList().broadcastSystemMessage(msg, false);
    }

    // ---------- comandos ----------

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("waypoint")
            // clique em [Waypoint]: /waypoint adicionar <dim> <x> <y> <z> <nome>
            .then(literal("adicionar")
                .then(argument("dim", StringArgumentType.word())
                    .then(argument("x", IntegerArgumentType.integer())
                        .then(argument("y", IntegerArgumentType.integer())
                            .then(argument("z", IntegerArgumentType.integer())
                                .then(argument("nome", StringArgumentType.greedyString()).executes(c -> adicionar(c.getSource().getPlayerOrException(),
                                    StringArgumentType.getString(c, "dim"), IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "y"),
                                    IntegerArgumentType.getInteger(c, "z"), StringArgumentType.getString(c, "nome"))))
                                .executes(c -> adicionar(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "dim"),
                                    IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "y"), IntegerArgumentType.getInteger(c, "z"), "Ponto")))))))
            .then(literal("servidor")
                .then(literal("ligar").executes(c -> ligar(c.getSource().getPlayerOrException(), true)))
                .then(literal("desligar").executes(c -> ligar(c.getSource().getPlayerOrException(), false)))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.sendSystemMessage(Jogo.prefixo("Waypoints no servidor: " + (Jogo.casa.config.waypoints ? "ligados" : "desligados") + ". Use /waypoint servidor ligar ou desligar."));
                    return 1;
                }))
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                p.sendSystemMessage(Jogo.prefixo(pode(p, WaypointRede.ConfigPayload.TYPE)
                    ? "Os waypoints ficam no seu jogo: tecla B abre a lista, N cria um ponto aqui e H liga ou desliga todos."
                    : "Os waypoints precisam do MelbyCraft instalado no seu jogo (o mesmo arquivo .jar do servidor, na pasta mods do cliente)."));
                return 1;
            }));
    }

    private static int adicionar(ServerPlayer p, String dim, int x, int y, int z, String nome) {
        if (!pode(p, WaypointRede.PontoPayload.TYPE)) {
            p.sendSystemMessage(Jogo.prefixo("Você precisa do MelbyCraft instalado no seu jogo para usar waypoints."));
            return 0;
        }
        if (!Jogo.casa.config.waypoints) {
            p.sendSystemMessage(Jogo.prefixo("Os waypoints estão desligados neste servidor."));
            return 0;
        }
        String n = limpar(nome);
        if (Waypoints.validarNome(n) != null || !dimValida(dim) || !coordValida(x, y, z)) {
            p.sendSystemMessage(Jogo.prefixo("Esse waypoint não é válido."));
            return 0;
        }
        enviar(p, new WaypointRede.PontoPayload(false, "comp-" + UUID.randomUUID(), n, dim, x, y, z, Waypoints.COMPARTILHADO, COR_COMPARTILHADO));
        p.sendSystemMessage(Jogo.prefixo("Waypoint \"" + n + "\" adicionado."));
        return 1;
    }

    private static int ligar(ServerPlayer p, boolean ligado) {
        if (!Comandos.admin(p)) {
            p.sendSystemMessage(Jogo.prefixo("Só OP pode mudar isso."));
            return 0;
        }
        Jogo.casa.config.waypoints = ligado;
        Jogo.salvar();
        for (ServerPlayer q : Jogo.server.getPlayerList().getPlayers()) enviar(q, new WaypointRede.ConfigPayload(ligado));
        p.sendSystemMessage(Jogo.prefixo("Waypoints " + (ligado ? "ligados" : "desligados") + " para todos."));
        return 1;
    }
}
