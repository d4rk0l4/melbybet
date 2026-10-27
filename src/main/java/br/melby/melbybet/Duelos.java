package br.melby.melbybet;

import br.melby.melbybet.core.Casa;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.util.UUID;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** /duelo: dois jogadores apostam o mesmo valor; quem matar o outro primeiro leva o pote. */
public final class Duelos {
    private Duelos() {}

    private static final long CONVITE_MS = 60_000L;
    private static final long DURACAO_MS = 15 * 60_000L;
    private static final long COMBATE_MS = 30_000L;

    /** uuid -> instante do último golpe entre os dois duelistas. Só na memória. */
    private static final java.util.Map<UUID, Long> COMBATE = new java.util.HashMap<>();

    private static long combateRestante(UUID id) {
        Long t = COMBATE.get(id);
        if (t == null) return 0;
        return Math.max(0, COMBATE_MS - (System.currentTimeMillis() - t));
    }

    /** Chamado quando alguém leva dano: se foi do outro duelista, os dois entram em combate por 30 s. */
    public static void aoDano(net.minecraft.world.entity.LivingEntity vitima, DamageSource fonte) {
        if (!(vitima instanceof ServerPlayer v) || !(fonte.getEntity() instanceof ServerPlayer atacante)) return;
        Casa.Duelo x = Jogo.casa.duelAtivoDe(v.getUUID().toString());
        if (x == null || x != Jogo.casa.duelAtivoDe(atacante.getUUID().toString())) return;
        long agora = System.currentTimeMillis();
        COMBATE.put(v.getUUID(), agora);
        COMBATE.put(atacante.getUUID(), agora);
    }

    private static MutableComponent botao(String rotulo, ChatFormatting cor, String comando, String dica) {
        return Component.literal("[" + rotulo + "]").withStyle(s -> s.withColor(cor).withBold(true)
            .withClickEvent(new ClickEvent.RunCommand(comando))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal(dica))));
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("duelo")
            .executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Casa.Duelo x = Jogo.casa.duelAtivoDe(p.getUUID().toString());
                if (x == null) {
                    p.sendSystemMessage(Jogo.prefixo("Use /duelo <nick> <valor>. Quem matar o outro primeiro leva o pote; /duelo desistir entrega a vitória."));
                } else {
                    p.sendSystemMessage(Jogo.prefixo("Duelo: " + x.nomeA + " x " + x.nomeB + ", " + x.valor + " diamante(s) cada. Vence quem matar o outro."));
                }
                return 1;
            })
            .then(literal("aceitar").then(argument("id", integer(1)).executes(c -> {
                aceitar(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "id"));
                return 1;
            })))
            .then(literal("recusar").then(argument("id", integer(1)).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Casa.Duelo x = Jogo.casa.achaDuelo(IntegerArgumentType.getInteger(c, "id"));
                if (x != null && x.estado.equals("PROPOSTO") && x.uuidB.equals(p.getUUID().toString())) {
                    x.estado = "CANCELADO";
                    Jogo.salvar();
                    p.sendSystemMessage(Jogo.prefixo("Você recusou o duelo."));
                    ServerPlayer a = Jogo.server.getPlayerList().getPlayer(UUID.fromString(x.uuidA));
                    if (a != null) a.sendSystemMessage(Jogo.prefixo(p.getGameProfile().name() + " recusou o duelo."));
                }
                return 1;
            })))
            .then(literal("desistir").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Casa.Duelo x = Jogo.casa.duelAtivoDe(p.getUUID().toString());
                if (x == null) {
                    p.sendSystemMessage(Jogo.prefixo("Você não está em um duelo."));
                    return 0;
                }
                long resta = combateRestante(p.getUUID());
                if (resta > 0) {
                    p.sendSystemMessage(Jogo.prefixo("Você está em combate. Espere " + (resta / 1000 + 1) + "s para desistir."));
                    return 0;
                }
                String outro = x.uuidA.equals(p.getUUID().toString()) ? x.uuidB : x.uuidA;
                vencer(x, outro, p.getGameProfile().name() + " desistiu.");
                return 1;
            }))
            .then(argument("alvo", EntityArgument.player()).then(argument("valor", integer(1)).executes(c -> {
                ServerPlayer a = c.getSource().getPlayerOrException();
                propor(a, EntityArgument.getPlayer(c, "alvo"), IntegerArgumentType.getInteger(c, "valor"));
                return 1;
            }))));
    }

    private static void propor(ServerPlayer a, ServerPlayer b, int valor) {
        String erro = Jogo.casa.validarDuelo(a.getUUID().toString(), b.getUUID().toString(), valor);
        if (erro != null) {
            a.sendSystemMessage(Jogo.prefixo(erro));
            return;
        }
        if (Jogo.contar(a) < valor) {
            a.sendSystemMessage(Jogo.prefixo("Você só tem " + Jogo.contar(a) + " diamante(s)."));
            return;
        }
        Casa.Duelo x = Jogo.casa.proporDuelo(a.getUUID().toString(), a.getGameProfile().name(), b.getUUID().toString(), b.getGameProfile().name(),
            valor, System.currentTimeMillis());
        Jogo.salvar();
        a.sendSystemMessage(Jogo.prefixo("Convite enviado para " + b.getGameProfile().name() + " (vale 60 segundos)."));
        b.sendSystemMessage(Jogo.prefixo(a.getGameProfile().name() + " te chamou para um duelo de " + valor + " diamante(s) cada. Quem matar o outro primeiro leva tudo. ")
            .copy().append(botao("Aceitar", ChatFormatting.GREEN, "/duelo aceitar " + x.id, "Põe " + valor + " diamante(s) e começa"))
            .append(Component.literal(" "))
            .append(botao("Recusar", ChatFormatting.RED, "/duelo recusar " + x.id, "Não duelar")));
    }

    private static void aceitar(ServerPlayer b, int id) {
        Casa.Duelo x = Jogo.casa.achaDuelo(id);
        if (x == null || !x.estado.equals("PROPOSTO") || !x.uuidB.equals(b.getUUID().toString())) {
            b.sendSystemMessage(Jogo.prefixo("Esse convite não existe mais."));
            return;
        }
        ServerPlayer a = Jogo.server.getPlayerList().getPlayer(UUID.fromString(x.uuidA));
        if (a == null) {
            x.estado = "CANCELADO";
            b.sendSystemMessage(Jogo.prefixo(x.nomeA + " não está mais online."));
            return;
        }
        String erro = Jogo.casa.validarDuelo(x.uuidA, x.uuidB, x.valor);
        // o convite do próprio jogador conta como pendente; ignora esse aviso específico
        if (erro != null && !erro.equals("Você já tem um convite pendente.")) {
            b.sendSystemMessage(Jogo.prefixo(erro));
            return;
        }
        if (Jogo.contar(a) < x.valor) {
            b.sendSystemMessage(Jogo.prefixo(x.nomeA + " não tem " + x.valor + " diamante(s) agora."));
            return;
        }
        if (Jogo.contar(b) < x.valor) {
            b.sendSystemMessage(Jogo.prefixo("Você só tem " + Jogo.contar(b) + " diamante(s)."));
            return;
        }
        Jogo.tirar(a, x.valor);
        Jogo.tirar(b, x.valor);
        Jogo.casa.ativarDuelo(x, System.currentTimeMillis());
        Jogo.salvar();
        Jogo.avisar("Duelo valendo! " + x.nomeA + " x " + x.nomeB + ", " + x.valor + " diamante(s) cada. Quem matar o outro primeiro leva o pote.");
    }

    private static void vencer(Casa.Duelo x, String uuidVencedor, String motivo) {
        int premio = Jogo.casa.resolverDuelo(x, uuidVencedor, System.currentTimeMillis());
        String nome = uuidVencedor.equals(x.uuidA) ? x.nomeA : x.nomeB;
        Jogo.avisar("Fim do duelo! " + motivo + " " + nome + " leva " + premio + " diamante(s).");
        Jogo.webhook("⚔️ **Duelo** — " + nome + " venceu " + (uuidVencedor.equals(x.uuidA) ? x.nomeB : x.nomeA) + " e levou **" + premio + "** 💎");
        Jogo.entregarTodos();
        Jogo.salvar();
    }

    private static void cancelar(Casa.Duelo x, String motivo) {
        Jogo.casa.cancelarDuelo(x, System.currentTimeMillis());
        Jogo.avisar("Duelo " + x.nomeA + " x " + x.nomeB + " cancelado: " + motivo + " Os diamantes foram devolvidos.");
        Jogo.entregarTodos();
        Jogo.salvar();
    }

    /** Chamado quando um jogador morre. */
    public static void aoMorrer(ServerPlayer morto, DamageSource fonte) {
        Casa.Duelo x = Jogo.casa.duelAtivoDe(morto.getUUID().toString());
        if (x == null) return;
        String outro = x.uuidA.equals(morto.getUUID().toString()) ? x.uuidB : x.uuidA;
        if (fonte.getEntity() instanceof ServerPlayer matador && matador.getUUID().toString().equals(outro)) {
            vencer(x, outro, morto.getGameProfile().name() + " caiu.");
        } else {
            cancelar(x, morto.getGameProfile().name() + " morreu por outro motivo.");
        }
    }

    /** Chamado a cada segundo: expira convites, cancela duelos longos e dá a vitória a quem ficou se o outro saiu. */
    public static void aCadaSegundo() {
        if (Jogo.server == null || Jogo.casa.duelos.isEmpty()) return;
        long agora = System.currentTimeMillis();
        boolean limpar = false;
        for (Casa.Duelo x : new java.util.ArrayList<>(Jogo.casa.duelos)) {
            if (x.estado.equals("PROPOSTO")) {
                if (agora - x.criadoEm > CONVITE_MS) {
                    x.estado = "CANCELADO";
                    limpar = true;
                }
            } else if (x.estado.equals("ATIVO")) {
                boolean aOn = Jogo.server.getPlayerList().getPlayer(UUID.fromString(x.uuidA)) != null;
                boolean bOn = Jogo.server.getPlayerList().getPlayer(UUID.fromString(x.uuidB)) != null;
                if (!aOn && !bOn) cancelar(x, "os dois saíram.");
                else if (!aOn) vencer(x, x.uuidB, x.nomeA + " saiu do jogo" + (emCombate(x.uuidA) ? " em combate." : "."));
                else if (!bOn) vencer(x, x.uuidA, x.nomeB + " saiu do jogo" + (emCombate(x.uuidB) ? " em combate." : "."));
                else if (agora - x.inicio > DURACAO_MS) cancelar(x, "passou de 15 minutos sem vencedor.");
                else {
                    for (String u : new String[] {x.uuidA, x.uuidB}) {
                        long resta = combateRestante(UUID.fromString(u));
                        if (resta > 0) {
                            ServerPlayer jp = Jogo.server.getPlayerList().getPlayer(UUID.fromString(u));
                            if (jp != null) jp.sendSystemMessage(Component.literal("Em combate: " + (resta / 1000 + 1) + "s").withStyle(ChatFormatting.RED), true);
                        }
                    }
                }
            }
        }
        if (limpar) {
            Jogo.casa.duelos.removeIf(x -> x.estado.equals("CANCELADO") && x.criadoEm < agora - CONVITE_MS);
            Jogo.salvar();
        }
    }

    private static boolean emCombate(String uuid) {
        return combateRestante(UUID.fromString(uuid)) > 0;
    }

    /** Ao ligar o servidor, convites pendentes (sem diamantes tirados) somem. */
    public static void aoLigar() {
        Jogo.casa.duelos.removeIf(x -> x.estado.equals("PROPOSTO"));
    }
}
