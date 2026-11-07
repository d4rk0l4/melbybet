package br.melby.melbybet;

import br.melby.melbybet.core.DueloLog;
import br.melby.melbybet.core.PlacaHistorico;
import br.melby.melbybet.core.PlacaHistorico.Versao;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;

import java.nio.file.Path;
import java.util.List;

/** Guarda quem escreveu o quê em cada placa, e deixa voltar a uma versão anterior (/placa historico e /placa restaurar). */
public final class HistoricoPlacas {
    private HistoricoPlacas() {}

    private static PlacaHistorico dados = new PlacaHistorico();
    private static Path arq;
    private static boolean sujo;
    private static String antesChave;
    private static String[] antesLinhas;

    public static void carregar() {
        arq = FabricLoader.getInstance().getConfigDir().resolve("melbybet").resolve("placas-historico.json");
        dados = PlacaHistorico.ler(arq);
        sujo = false;
    }

    public static void salvarAgora() {
        if (arq == null || !sujo) return;
        try {
            dados.gravar(arq);
            sujo = false;
        } catch (Exception e) {
            Melbybet.LOGGER.error("Falha ao salvar {}", arq, e);
        }
    }

    public static void aCadaSegundo() {
        if (sujo) salvarAgora();
    }

    public static void registrarEventos() {
        PlayerBlockBreakEvents.AFTER.register((n, p, pos, st, be) -> {
            if (be instanceof SignBlockEntity && n instanceof ServerLevel sl) {
                dados.remover(Corpos.dimDe(sl), pos.getX(), pos.getY(), pos.getZ());
                sujo = true;
            }
        });
    }

    static String chave(SignBlockEntity s, SignTextSlot lado) {
        var p = s.getBlockPos();
        return PlacaHistorico.chave(s.getLevel() instanceof ServerLevel sl ? Corpos.dimDe(sl) : "?", p.getX(), p.getY(), p.getZ(), lado.name());
    }

    /** Chamar logo ANTES de mudar o texto da placa. */
    public static void antes(SignBlockEntity s, SignTextSlot lado) {
        if (!(s.getLevel() instanceof ServerLevel)) return;
        antesChave = chave(s, lado);
        antesLinhas = Placas.lerLinhas(s, lado);
    }

    /** Chamar logo DEPOIS de mudar o texto (já com as cores convertidas). */
    public static void depois(Player p, SignBlockEntity s, SignTextSlot lado) {
        if (antesChave == null || !(s.getLevel() instanceof ServerLevel)) return;
        String k = chave(s, lado);
        String[] antes = antesLinhas;
        boolean mesma = k.equals(antesChave);
        antesChave = null;
        antesLinhas = null;
        if (!mesma) return;
        dados.registrar(k, antes, Placas.lerLinhas(s, lado), p.getUUID().toString(), p.getGameProfile().name(), System.currentTimeMillis());
        sujo = true;
    }

    public static int historico(ServerPlayer p, SignBlockEntity s) {
        SignTextSlot lado = s.getSlotPlayerIsFacing(p);
        List<Versao> l = dados.lista(chave(s, lado));
        if (l.isEmpty()) {
            p.sendSystemMessage(Component.literal("Essa placa não tem histórico (só registro edições feitas depois que o recurso entrou).").withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }
        p.sendSystemMessage(Component.literal("Histórico da placa").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            .append(Component.literal("  (clique em [restaurar])").withStyle(ChatFormatting.GRAY)), false);
        int n = 0;
        for (Versao v : l) {
            if (++n > 10) break;
            String quando = DueloLog.data(v.quando, Jogo.casa.config.fuso);
            final int id = v.id;
            MutableComponent previa = Component.empty();
            for (int i = 0; i < 4; i++) {
                if (i > 0) previa.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY));
                previa.append(Placas.componente(v.linhas[i]));
            }
            MutableComponent botao = Component.literal("[restaurar]").withStyle(st -> st.withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent.RunCommand("/placa restaurar " + id))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Volta a placa para esta versão"))));
            p.sendSystemMessage(Component.literal("#" + id + " " + quando + " " + v.nome + " ").withStyle(ChatFormatting.AQUA).append(botao), false);
            p.sendSystemMessage(Component.literal("   ").append(previa), false);
        }
        return l.size();
    }

    public static int restaurar(ServerPlayer p, SignBlockEntity s, int id) {
        Versao v = dados.achar(id);
        if (v == null) return Placas.avisoErro(p, "Não existe a versão #" + id + " (talvez já tenha saído do limite de " + PlacaHistorico.MAX_VERSOES + ").");
        SignTextSlot lado = s.getSlotPlayerIsFacing(p);
        if (!chave(s, lado).equals(dados.chaveDe(id))) return Placas.avisoErro(p, "Essa versão é de outra placa (ou do outro lado). Olhe para a placa certa.");
        String err = PlacaHistorico.podeRestaurar(v, p.getUUID().toString(), Comandos.admin(p));
        if (err != null) return Placas.avisoErro(p, err);
        if (s.isWaxed()) return Placas.avisoErro(p, "Essa placa está encerada, não dá para mudar.");
        if (!p.level().mayInteract(p, s.getBlockPos())) return Placas.avisoErro(p, "Você não pode mexer nessa placa.");
        antes(s, lado);
        for (int i = 0; i < 4; i++) Placas.editar(s, p, i, v.linhas[i]);
        depois(p, s, lado);
        p.sendSystemMessage(Component.literal("Placa restaurada para a versão #" + id + ".").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    // ---------- editor do cliente ----------

    /** Manda ao cliente o texto atual da placa e as versões do histórico (até 10). */
    public static void enviarAoCliente(ServerPlayer p, net.minecraft.core.BlockPos pos) {
        SignBlockEntity s = ConstrucaoServidor.placaPerto(p, pos);
        if (s == null) return;
        SignTextSlot lado = s.getSlotPlayerIsFacing(p);
        String[] atual = Placas.lerLinhas(s, lado);
        List<ConstrucaoRede.VersaoPlaca> vs = new java.util.ArrayList<>();
        for (Versao v : dados.lista(chave(s, lado))) {
            if (vs.size() >= 10) break;
            vs.add(new ConstrucaoRede.VersaoPlaca(v.id, DueloLog.data(v.quando, Jogo.casa.config.fuso) + " " + v.nome, List.of(v.linhas)));
        }
        ConstrucaoServidor.enviar(p, new ConstrucaoRede.PlacaDadosPayload(pos.getX(), pos.getY(), pos.getZ(), List.of(atual), s.isWaxed(), vs));
    }

    public static void aplicarDoCliente(ServerPlayer p, net.minecraft.core.BlockPos pos, List<String> linhas) {
        SignBlockEntity s = ConstrucaoServidor.placaPerto(p, pos);
        if (s == null) return;
        if (s.isWaxed()) {
            Placas.avisoErro(p, "Essa placa está encerada, não dá para mudar.");
            return;
        }
        if (!p.level().mayInteract(p, pos)) {
            Placas.avisoErro(p, "Você não pode mexer nessa placa.");
            return;
        }
        for (String l : linhas) {
            if (br.melby.melbybet.core.TextoPlaca.semCodigos(l).length() > 40) {
                Placas.avisoErro(p, "Linha grande demais.");
                return;
            }
        }
        SignTextSlot lado = s.getSlotPlayerIsFacing(p);
        antes(s, lado);
        for (int i = 0; i < 4; i++) Placas.editar(s, p, i, i < linhas.size() ? linhas.get(i) : "");
        depois(p, s, lado);
        p.sendSystemMessage(Component.literal("Placa atualizada.").withStyle(ChatFormatting.GREEN), true);
        enviarAoCliente(p, pos);
    }

    public static void restaurarDoCliente(ServerPlayer p, net.minecraft.core.BlockPos pos, int id) {
        SignBlockEntity s = ConstrucaoServidor.placaPerto(p, pos);
        if (s == null) return;
        restaurar(p, s, id);
        enviarAoCliente(p, pos);
    }
}
