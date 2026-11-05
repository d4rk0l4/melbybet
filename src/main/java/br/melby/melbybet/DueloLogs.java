package br.melby.melbybet;

import br.melby.melbybet.core.ArenaRegistro;
import br.melby.melbybet.core.Casa;
import br.melby.melbybet.core.DueloLog;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Log permanente dos duelos (config/melbybet/duelos-log.jsonl) e a anulação feita pela staff. */
public final class DueloLogs {
    private DueloLogs() {}

    static DueloLog log = new DueloLog();
    private static Path arq;
    private static boolean sujo;

    public static void carregar() {
        arq = FabricLoader.getInstance().getConfigDir().resolve("melbybet").resolve("duelos-log.jsonl");
        log = DueloLog.ler(arq);
    }

    public static void salvar() {
        if (arq == null) return;
        try {
            log.gravar(arq);
            sujo = false;
        } catch (Exception e) {
            Melbybet.LOGGER.error("Não consegui gravar o log de duelos em {}", arq, e);
        }
    }

    /** Marca para gravar daqui a pouco (o rastro gera muitos eventos). */
    static void marcarSujo() {
        sujo = true;
    }

    /** Ao ligar: duelo que ficou "em andamento" sem estar mais ativo foi cortado pelo reinício. */
    public static void aoLigar() {
        long agora = System.currentTimeMillis();
        for (DueloLog.Reg r : log.regs) {
            if (!r.estado.equals("EM ANDAMENTO")) continue;
            Casa.Duelo x = Jogo.casa.achaDuelo(r.id);
            if (x != null && x.estado.equals("ATIVO")) continue;
            r.estado = "CANCELADO";
            r.fim = agora;
            r.motivo = "o servidor reiniciou durante o duelo";
            r.evento(agora, "Cancelado: o servidor reiniciou durante o duelo");
        }
        salvar();
    }

    public static void aCadaSegundo() {
        if (sujo) salvar();
    }

    static String fuso() {
        return Jogo.casa.config.fuso;
    }

    private static String desc(List<ItemStack> l) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(l.get(i).getCount()).append("x ").append(l.get(i).getHoverName().getString());
        }
        return sb.toString();
    }

    /** O registro do duelo, com nomes e valores preenchidos. */
    static DueloLog.Reg obter(Casa.Duelo x) {
        DueloLog.Reg r = log.obter(x.id);
        if (r.criadoEm == 0) {
            r.criadoEm = System.currentTimeMillis();
            r.uuidA = x.uuidA;
            r.nomeA = x.nomeA;
            r.uuidB = x.uuidB;
            r.nomeB = x.nomeB;
            r.itens = x.itens;
            r.valor = x.valor;
        }
        return r;
    }

    public static void evento(Casa.Duelo x, String texto) {
        obter(x).evento(System.currentTimeMillis(), texto);
        Melbybet.LOGGER.info("[duelo {}] {}", x.id, texto);
        marcarSujo();
    }

    /** O duelo entrou no ar (diamantes tirados / itens definidos). daA/daB só no duelo de itens. */
    static void inicio(Casa.Duelo x, List<ItemStack> daA, List<ItemStack> daB) {
        DueloLog.Reg r = obter(x);
        r.inicio = System.currentTimeMillis();
        r.estado = "EM ANDAMENTO";
        if (x.itens) {
            r.apostaA = daA == null ? "" : desc(daA);
            r.apostaB = daB == null ? "" : desc(daB);
            r.itensA.clear();
            r.itensB.clear();
            if (daA != null) for (ItemStack s : daA) r.itensA.add(Corpos.paraJson(s));
            if (daB != null) for (ItemStack s : daB) r.itensB.add(Corpos.paraJson(s));
        } else {
            r.apostaA = x.valor + " diamante(s)";
            r.apostaB = r.apostaA;
        }
        ArenaRegistro.Arena a = Arena.registroDe(x.id);
        if (a != null) {
            r.arena = a.x0 + " " + a.y0 + " " + a.z0 + " (" + a.larg + "x" + a.comp + "x" + a.alt + ", " + a.dim + ")";
            r.origemA = a.origemA;
            r.origemB = a.origemB;
        }
        r.evento(r.inicio, "Duelo começou" + (r.arena.isEmpty() ? "" : " na arena " + r.arena));
        salvar();
    }

    static void fimVitoria(Casa.Duelo x, String uuidVenc, String motivo, int premio, int taxa, String resultado, List<ItemStack> passou) {
        DueloLog.Reg r = obter(x);
        long agora = System.currentTimeMillis();
        r.fim = agora;
        r.estado = "RESOLVIDO";
        r.vencedorUuid = uuidVenc;
        r.vencedorNome = uuidVenc.equals(x.uuidA) ? x.nomeA : x.nomeB;
        r.motivo = motivo;
        r.premio = premio;
        r.taxa = taxa;
        r.resultado = resultado;
        r.transferidos.clear();
        if (passou != null) for (ItemStack s : passou) r.transferidos.add(Corpos.paraJson(s));
        r.evento(agora, "Fim: " + motivo + " Vencedor: " + r.vencedorNome + ". " + resultado);
        Rastro.iniciar(r);
        salvar();
    }

    static void fimCancelado(Casa.Duelo x, String motivo) {
        DueloLog.Reg r = obter(x);
        if (r.estado.equals("RESOLVIDO") || r.estado.equals("ANULADO")) return;
        r.fim = System.currentTimeMillis();
        r.estado = "CANCELADO";
        r.motivo = motivo;
        r.evento(r.fim, "Cancelado: " + motivo);
        salvar();
    }

    // ---------- texto ----------

    public static String linha(DueloLog.Reg r) {
        String quando = DueloLog.data(r.inicio > 0 ? r.inicio : r.criadoEm, fuso());
        String res = switch (r.estado) {
            case "RESOLVIDO" -> "venceu " + r.vencedorNome;
            case "ANULADO" -> "ANULADO";
            case "CANCELADO" -> "cancelado";
            default -> "em andamento";
        };
        return "#" + r.id + " " + r.nomeA + " x " + r.nomeB + " — " + quando + " — " + res;
    }

    public static List<String> detalhe(DueloLog.Reg r, boolean comRastro) {
        String f = fuso();
        List<String> l = new ArrayList<>();
        l.add("Duelo #" + r.id + ": " + r.nomeA + " x " + r.nomeB + (r.itens ? " (itens)" : " (" + r.valor + " diamante(s) cada)"));
        l.add("Início: " + DueloLog.data(r.inicio > 0 ? r.inicio : r.criadoEm, f) + "   Fim: " + DueloLog.data(r.fim, f));
        l.add("Estado: " + r.estado + (r.vencedorNome.isEmpty() ? "" : " — vencedor " + r.vencedorNome) + (r.motivo.isEmpty() ? "" : " (" + r.motivo + ")"));
        if (!r.apostaA.isEmpty()) l.add("Aposta de " + r.nomeA + ": " + r.apostaA + " | de " + r.nomeB + ": " + r.apostaB);
        if (!r.resultado.isEmpty()) l.add("Resultado: " + r.resultado);
        if (!r.arena.isEmpty()) l.add("Arena: " + r.arena);
        if (r.origemA != null) l.add("Origem " + r.nomeA + ": " + origem(r.origemA) + " | " + r.nomeB + ": " + origem(r.origemB));
        if (!r.anuladoPor.isEmpty()) l.add("ANULADO por " + r.anuladoPor + " em " + DueloLog.data(r.anuladoEm, f) + ": " + r.anuladoMotivo + " — " + r.anuladoResumo);
        l.add("Eventos:");
        for (DueloLog.Evento e : r.eventos) l.add("  " + DueloLog.data(e.ts, f) + "  " + e.texto);
        if (comRastro) {
            l.add("Rastro do prêmio de " + r.vencedorNome + (r.rastroAte > 0 ? " (até " + DueloLog.data(r.rastroAte, f) + ")" : "") + ":");
            if (r.rastro.isEmpty()) l.add("  (nada registrado)");
            int de = Math.max(0, r.rastro.size() - 40);
            for (int i = de; i < r.rastro.size(); i++) l.add("  " + DueloLog.data(r.rastro.get(i).ts, f) + "  " + r.rastro.get(i).texto);
        }
        return l;
    }

    private static String origem(ArenaRegistro.Origem o) {
        return o == null ? "-" : (int) o.x + " " + (int) o.y + " " + (int) o.z + " (" + o.dim + ")";
    }

    // ---------- anulação ----------

    /** Anula um duelo. Devolve o texto do resultado (começa com "Não" se não deu). */
    public static String anular(String por, int id, String motivo) {
        DueloLog.Reg r = log.achar(id);
        if (r == null) return "Não existe o duelo #" + id + " no log.";
        Casa.Duelo x = Jogo.casa.achaDuelo(id);
        long agora = System.currentTimeMillis();
        String resumo;
        switch (r.estado) {
            case "ANULADO" -> {
                return "Não dá: esse duelo já foi anulado.";
            }
            case "CANCELADO" -> {
                return "Não dá: esse duelo foi cancelado e o que foi cobrado já voltou.";
            }
            case "EM ANDAMENTO" -> {
                if (x == null || !x.estado.equals("ATIVO")) return "Não dá: esse duelo não está ativo.";
                Duelos.cancelarPorArena(x, "anulado pela staff (" + motivo + ").");
                resumo = r.itens ? "duelo cancelado; nada tinha sido transferido" : "duelo cancelado; diamantes devolvidos";
            }
            case "RESOLVIDO" -> {
                String erro = r.itens ? anularItens(r) : null;
                if (erro != null) return erro;
                resumo = r.itens ? r.anuladoResumo : anularDiamantes(r, agora);
            }
            default -> {
                return "Não dá para anular esse duelo (estado " + r.estado + ").";
            }
        }
        r.estado = "ANULADO";
        r.anuladoPor = por;
        r.anuladoMotivo = motivo;
        r.anuladoEm = agora;
        r.anuladoResumo = resumo;
        r.rastroAte = 0;
        r.evento(agora, "ANULADO por " + por + ": " + motivo + " — " + resumo);
        salvar();
        Jogo.salvar();
        for (String u : new String[] {r.uuidA, r.uuidB}) {
            ServerPlayer p = Jogo.server.getPlayerList().getPlayer(UUID.fromString(u));
            if (p != null) p.sendSystemMessage(Jogo.prefixo("A staff anulou o duelo #" + id + " (" + r.nomeA + " x " + r.nomeB + "): " + motivo + ". " + resumo));
        }
        Jogo.webhook("↩️ **Duelo #" + id + " anulado** por " + por + " (" + r.nomeA + " x " + r.nomeB + "): " + motivo + " — " + resumo);
        Melbybet.LOGGER.info("[duelo {}] anulado por {}: {} — {}", id, por, motivo, resumo);
        return "Duelo #" + id + " anulado. " + resumo;
    }

    private static String anularDiamantes(DueloLog.Reg r, long agora) {
        int premio = r.premio;
        int recuperado = recolherDiamantes(r.vencedorUuid, premio);
        Jogo.casa.anularDuelo(r.id, r.uuidA, r.nomeA, r.uuidB, r.nomeB, r.vencedorUuid, r.valor, false, premio, recuperado, agora);
        Jogo.entregarTodos();
        return "Cada um recebe " + r.valor + " diamante(s) de volta. Do prêmio de " + premio + ", recolhi " + recuperado + " do vencedor"
            + (recuperado < premio ? "; os outros " + (premio - recuperado) + " saíram do cofre." : ".");
    }

    /** Recolhe até n diamantes do vencedor: primeiro o crédito pendente, depois o inventário e o ender chest (se online). */
    private static int recolherDiamantes(String uuid, int n) {
        int falta = n;
        int pend = Math.min(falta, Jogo.casa.creditoDe(uuid));
        if (pend > 0) {
            Jogo.casa.baixarCredito(uuid, pend);
            falta -= pend;
        }
        ServerPlayer p = Jogo.server.getPlayerList().getPlayer(UUID.fromString(uuid));
        if (p != null && falta > 0) {
            int tem = Math.min(falta, Jogo.contar(p));
            if (tem > 0 && Jogo.tirar(p, tem)) falta -= tem;
            if (falta > 0) falta -= tirarDe(p.getEnderChestInventory(), null, falta);
            p.containerMenu.broadcastChanges();
        }
        return n - falta;
    }

    /** Tira até n itens iguais ao modelo (ou diamantes, se modelo é null) de um container. Devolve quantos tirou. */
    private static int tirarDe(Container c, ItemStack modelo, int n) {
        int tirou = 0;
        for (int i = 0; i < c.getContainerSize() && tirou < n; i++) {
            ItemStack st = c.getItem(i);
            boolean bate = modelo == null ? st.is(Items.DIAMOND) : (!st.isEmpty() && Duelos.mesmoItem(modelo, st));
            if (!bate) continue;
            int x = Math.min(n - tirou, st.getCount());
            st.shrink(x);
            tirou += x;
        }
        c.setChanged();
        return tirou;
    }

    /** Devolve null se deu certo (e deixa o resumo em r.anuladoResumo) ou o motivo de não dar. */
    private static String anularItens(DueloLog.Reg r) {
        ServerPlayer perd = Jogo.server.getPlayerList().getPlayer(UUID.fromString(r.vencedorUuid.equals(r.uuidA) ? r.uuidB : r.uuidA));
        if (perd == null) return "Não dá agora: o perdedor precisa estar online para receber os itens de volta.";
        ServerPlayer venc = Jogo.server.getPlayerList().getPlayer(UUID.fromString(r.vencedorUuid));
        int recolhidos = 0, recriados = 0;
        List<String> nomes = new ArrayList<>();
        for (String j : r.transferidos) {
            ItemStack modelo = Corpos.deJson(j);
            if (modelo.isEmpty()) continue;
            int quer = modelo.getCount();
            int tirou = 0;
            if (venc != null) {
                tirou += tirarDe(venc.getInventory(), modelo, quer);
                if (tirou < quer) tirou += tirarDe(venc.getEnderChestInventory(), modelo, quer - tirou);
            }
            recolhidos += tirou;
            recriados += quer - tirou;
            ItemStack dar = modelo.copy();
            perd.getInventory().add(dar);
            if (!dar.isEmpty()) perd.drop(dar, false, net.minecraft.util.Prediction.SERVER_ONLY);
            nomes.add(quer + "x " + modelo.getHoverName().getString());
        }
        if (venc != null) venc.containerMenu.broadcastChanges();
        perd.containerMenu.broadcastChanges();
        Jogo.casa.anularDuelo(r.id, r.uuidA, r.nomeA, r.uuidB, r.nomeB, r.vencedorUuid, 0, true, 0, 0, System.currentTimeMillis());
        r.anuladoResumo = "Itens devolvidos ao perdedor (" + String.join(", ", nomes) + "): " + recolhidos + " recolhidos do vencedor e " + recriados
            + " recriados" + (venc == null ? " (o vencedor estava offline)" : "") + ".";
        return null;
    }
}
