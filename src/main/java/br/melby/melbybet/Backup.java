package br.melby.melbybet;

import br.melby.melbybet.core.Backups;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static net.minecraft.commands.Commands.literal;

/** Backup diário do casa.json (bolões, corpos, loteria...). Ficam em config/melbybet/backups. */
public final class Backup {
    private Backup() {}

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static Path pasta() {
        return FabricLoader.getInstance().getConfigDir().resolve("melbybet").resolve("backups");
    }

    private static Path arquivo() {
        return FabricLoader.getInstance().getConfigDir().resolve("melbybet").resolve("casa.json");
    }

    private static ZoneId zona() {
        try {
            return ZoneId.of(Jogo.casa.config.fuso);
        } catch (Exception e) {
            return ZoneId.of("America/Sao_Paulo");
        }
    }

    /** Guarda uma cópia do arquivo que está no disco antes de qualquer coisa sobrescrevê-lo (usado quando ele não abre). */
    public static void guardarCorrompido() {
        try {
            Path p = Backups.copiar(arquivo(), pasta(), "corrompido-" + LocalDateTime.now(zona()).format(HORA) + ".json");
            if (p != null) Melbybet.LOGGER.warn("casa.json não abriu; a cópia ficou em {}", p);
        } catch (Exception e) {
            Melbybet.LOGGER.error("Não consegui guardar o casa.json que não abriu", e);
        }
    }

    /** Chamado a cada segundo; uma vez por dia cria o backup. */
    private static int contador;

    public static void aCadaSegundo() {
        if (Jogo.server == null || ++contador % 60 != 0) return;
        if (Jogo.casa.config.backupDias <= 0) return;
        String nome = "casa-" + LocalDate.now(zona()) + ".json";
        if (Backups.existe(pasta(), nome)) return;
        try {
            Jogo.salvar();
            Backups.copiar(arquivo(), pasta(), nome);
            Backups.limpar(pasta(), "casa-2", Jogo.casa.config.backupDias);
            Melbybet.LOGGER.info("Backup diário criado: {}", nome);
        } catch (Exception e) {
            Melbybet.LOGGER.error("Falha no backup diário", e);
        }
    }

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("backup")
            .requires(s -> s.getPlayer() == null || Comandos.admin(s.getPlayer()))
            .executes(c -> {
                try {
                    Jogo.salvar();
                    String nome = "manual-" + LocalDateTime.now(zona()).format(HORA) + ".json";
                    Backups.copiar(arquivo(), pasta(), nome);
                    Backups.limpar(pasta(), "manual-", 10);
                    c.getSource().sendSuccess(() -> Component.literal("Backup criado: config/melbybet/backups/" + nome), false);
                    return 1;
                } catch (Exception e) {
                    c.getSource().sendFailure(Component.literal("Não consegui criar o backup: " + e.getMessage()));
                    return 0;
                }
            }));
    }
}
