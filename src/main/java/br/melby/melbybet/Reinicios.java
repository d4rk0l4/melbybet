package br.melby.melbybet;

import br.melby.melbybet.core.Reinicio;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.time.ZoneId;

/** Avisos automáticos de reinício e a contagem manual (/melbybet reiniciar). */
public final class Reinicios {
    private Reinicios() {}

    private static long ultimoAgendado = -1;
    private static long ultimoManual = -1;
    /** Instante (ms) em que o reinício manual acontece; 0 = nenhum. */
    private static long alvoManual;

    private static ZoneId zona() {
        try {
            return ZoneId.of(Jogo.casa.config.fuso);
        } catch (RuntimeException e) {
            return ZoneId.of("America/Sao_Paulo");
        }
    }

    public static long msAteAgendado() {
        return Reinicio.msAteProximo(Jogo.casa.config.reinicios, zona(), Instant.now());
    }

    public static boolean manualAtivo() {
        return alvoManual > 0;
    }

    public static void iniciarManual(int minutos) {
        alvoManual = System.currentTimeMillis() + minutos * 60_000L;
        ultimoManual = minutos * 60_000L + 1;
        Jogo.avisar("O servidor vai reiniciar em " + Reinicio.texto(minutos * 60L) + ".");
    }

    public static void cancelarManual() {
        alvoManual = 0;
        ultimoManual = -1;
        Jogo.avisar("O reinício foi cancelado.");
    }

    private static void anunciar(long segundos, boolean manual) {
        String t = Reinicio.texto(segundos);
        Component msg = Component.literal("[Reinício] ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
            .append(Component.literal("O servidor " + "reinicia" + " em " + t + ". Guarde seus itens e termine o que está fazendo.")
                .withStyle(ChatFormatting.WHITE));
        Component barra = Component.literal("Reinício em " + t).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        for (ServerPlayer p : Jogo.server.getPlayerList().getPlayers()) {
            if (segundos > 10) p.sendSystemMessage(msg);
            else p.sendSystemMessage(barra, true);
            Jogo.som(p, segundos <= 10 ? "minecraft:block.note_block.pling" : "minecraft:block.bell.use", segundos <= 10 ? 1.5f : 1.0f);
        }
    }

    /** Chamado a cada segundo. */
    public static void aCadaSegundo() {
        if (Jogo.server == null) return;
        long ag = msAteAgendado();
        if (ag >= 0) {
            if (ultimoAgendado >= 0) {
                long aviso = Reinicio.avisoCruzado(ultimoAgendado, ag);
                if (aviso > 0) anunciar(aviso, false);
            }
            ultimoAgendado = ag;
        } else {
            ultimoAgendado = -1;
        }
        if (alvoManual > 0) {
            long resta = alvoManual - System.currentTimeMillis();
            if (resta <= 0) {
                alvoManual = 0;
                Jogo.avisar("Reiniciando agora. Até já!");
                Jogo.salvar();
                Jogo.server.halt(false);
                return;
            }
            long aviso = Reinicio.avisoCruzado(ultimoManual, resta);
            if (aviso > 0) anunciar(aviso, true);
            ultimoManual = resta;
        }
    }
}
