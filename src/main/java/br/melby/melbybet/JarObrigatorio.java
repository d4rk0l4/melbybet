package br.melby.melbybet;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Exige o jar do MelbyCraft no cliente, na mesma versão do servidor. Modos (casa.json, jarModo): off, avisar (padrão,
 * só avisa o jogador e o log) e barrar (tira do servidor depois de alguns segundos). OP muda com /jar modo.
 */
public final class JarObrigatorio {
    private JarObrigatorio() {}

    private static final int SEGUNDOS_DE_TOLERANCIA = 12;
    private static final Map<UUID, Integer> SEGUNDOS = new HashMap<>();
    private static final Map<UUID, String> VERSAO = new HashMap<>();
    private static final Map<UUID, Boolean> AVISADO = new HashMap<>();

    public static String versaoDoServidor() {
        return FabricLoader.getInstance().getModContainer("melbybet").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }

    /** Uma vez só, na inicialização. */
    public static void registrarRede() {
        PayloadTypeRegistry.serverboundPlay().register(JarRede.OlaPayload.TYPE, JarRede.OlaPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(JarRede.OlaPayload.TYPE, (v, ctx) -> VERSAO.put(ctx.player().getUUID(), v.versao()));
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("jar").requires(s -> s.getPlayer() != null && Comandos.admin(s.getPlayer()))
            .then(literal("modo").then(argument("modo", StringArgumentType.word()).suggests((c, b) -> {
                b.suggest("off");
                b.suggest("avisar");
                b.suggest("barrar");
                return b.buildFuture();
            }).executes(c -> {
                String m = StringArgumentType.getString(c, "modo");
                if (!m.equals("off") && !m.equals("avisar") && !m.equals("barrar")) {
                    c.getSource().sendFailure(Component.literal("Use off, avisar ou barrar."));
                    return 0;
                }
                Jogo.casa.config.jarModo = m;
                Jogo.salvar();
                c.getSource().sendSuccess(() -> Component.literal("Jar do MelbyCraft: modo " + m + "."), false);
                return 1;
            })))
            .then(literal("quem").executes(c -> {
                String sv = versaoDoServidor();
                for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) {
                    String v = VERSAO.get(p.getUUID());
                    String nome = p.getGameProfile().name();
                    String linha = nome + ": " + (v == null ? "SEM jar" : v.equals(sv) ? "ok (" + v + ")" : "versão " + v + " (servidor " + sv + ")");
                    c.getSource().sendSuccess(() -> Component.literal(linha), false);
                }
                return 1;
            }))
            .executes(c -> {
                c.getSource().sendSuccess(() -> Component.literal("Modo: " + Jogo.casa.config.jarModo + ". Versão do servidor: " + versaoDoServidor() + ". Use /jar modo off|avisar|barrar e /jar quem."), false);
                return 1;
            }));
    }

    /** Chamado a cada segundo. */
    public static void aCadaSegundo() {
        if (Jogo.server == null) return;
        String modo = Jogo.casa.config.jarModo;
        String sv = versaoDoServidor();
        var online = new java.util.HashSet<UUID>();
        for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) {
            UUID id = p.getUUID();
            online.add(id);
            int s = SEGUNDOS.merge(id, 1, Integer::sum);
            if (modo.equals("off") || s < SEGUNDOS_DE_TOLERANCIA) continue;
            String v = VERSAO.get(id);
            if (v != null && v.equals(sv)) continue;
            Component msg = Component.literal(v == null
                ? "Este servidor exige o mod MelbyCraft (versão " + sv + "). Baixe o jar no canal de atualizações do Discord e coloque na pasta mods."
                : "Seu MelbyCraft é a versão " + v + " e o servidor usa a " + sv + ". Baixe o jar novo no canal de atualizações do Discord.");
            if (modo.equals("barrar")) {
                p.connection.disconnect(msg);
            } else if (AVISADO.put(id, true) == null) {
                p.sendSystemMessage(msg.copy().withStyle(ChatFormatting.RED));
            }
        }
        SEGUNDOS.keySet().removeIf(id -> !online.contains(id));
        VERSAO.keySet().removeIf(id -> !online.contains(id));
        AVISADO.keySet().removeIf(id -> !online.contains(id));
    }
}
