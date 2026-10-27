package br.melby.melbybet;

import br.melby.melbybet.core.Arquivos;
import br.melby.melbybet.core.Armazem;
import br.melby.melbybet.core.Bolao;
import br.melby.melbybet.core.Casa;
import br.melby.melbybet.core.Resultados;
import br.melby.melbybet.core.StatCat;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Estado do mod em execução e as ações que mexem no jogo (diamantes, mensagens, salvar). */
public final class Jogo {
    private Jogo() {}

    public static Casa casa = new Casa();
    public static MinecraftServer server;
    private static Path arquivo;
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    // cache das stats para o /top não reler o disco a cada clique
    private static Map<String, JsonObject> cacheStats = new LinkedHashMap<>();
    private static long cacheEm;

    public static void iniciar(MinecraftServer s) {
        server = s;
        arquivo = FabricLoader.getInstance().getConfigDir().resolve("melbybet").resolve("casa.json");
        try {
            casa = Armazem.carregar(arquivo);
        } catch (Exception e) {
            Melbybet.LOGGER.error("Não consegui ler {}; começando uma casa nova (o arquivo antigo não foi apagado).", arquivo, e);
            casa = new Casa();
        }
        Prefixos.limparTudo();
        Duelos.aoLigar();
        Presenca.aoLigar();
        salvar();
    }

    public static void salvar() {
        if (arquivo == null) return;
        try {
            Armazem.salvar(arquivo, casa);
        } catch (Exception e) {
            Melbybet.LOGGER.error("Falha ao salvar {}", arquivo, e);
        }
    }

    // ---------- texto ----------

    public static Component txt(String s, ChatFormatting cor) {
        return Component.literal(s).withStyle(cor);
    }

    public static Component prefixo(String s) {
        return Component.literal("[MelbyBet] ").withStyle(ChatFormatting.GOLD).append(Component.literal(s).withStyle(ChatFormatting.WHITE));
    }

    public static void avisar(String s) {
        if (server != null) server.getPlayerList().broadcastSystemMessage(prefixo(s), false);
    }

    // ---------- diamantes ----------

    public static int contar(ServerPlayer p) {
        Inventory inv = p.getInventory();
        int t = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.is(Items.DIAMOND)) t += st.getCount();
        }
        return t;
    }

    /** Tira n diamantes do inventário. false (sem tirar nada) se não tiver o bastante. */
    public static boolean tirar(ServerPlayer p, int n) {
        if (n <= 0 || contar(p) < n) return false;
        Inventory inv = p.getInventory();
        int falta = n;
        for (int i = 0; i < inv.getContainerSize() && falta > 0; i++) {
            ItemStack st = inv.getItem(i);
            if (!st.is(Items.DIAMOND)) continue;
            int x = Math.min(falta, st.getCount());
            st.shrink(x);
            falta -= x;
        }
        inv.setChanged();
        p.containerMenu.broadcastChanges();
        return true;
    }

    /** Entrega n diamantes. Devolve quantos não couberam no inventário. */
    public static int dar(ServerPlayer p, int n) {
        int resto = n;
        while (resto > 0) {
            int lote = Math.min(64, resto);
            ItemStack st = new ItemStack(Items.DIAMOND, lote);
            p.getInventory().add(st);
            int sobrou = st.getCount();
            resto -= lote - sobrou;
            if (sobrou > 0) break;
        }
        p.containerMenu.broadcastChanges();
        return resto;
    }

    /** Entrega os créditos pendentes do jogador (prêmios, devoluções). */
    public static void entregar(ServerPlayer p) {
        String id = p.getUUID().toString();
        int devido = casa.creditoDe(id);
        if (devido <= 0) return;
        int sobrou = dar(p, devido);
        int entregue = devido - sobrou;
        if (entregue > 0) {
            casa.baixarCredito(id, entregue);
            p.sendSystemMessage(prefixo("Você recebeu " + entregue + " diamante(s)."));
            salvar();
        }
        if (sobrou > 0) p.sendSystemMessage(prefixo("Inventário cheio: faltam " + sobrou + " diamante(s). Libere espaço que eles chegam."));
    }

    // ---------- apostas ----------

    /** Devolve null se apostou, ou o motivo da recusa. */
    public static String apostar(ServerPlayer p, Bolao b, String opcao, int valor) {
        String id = p.getUUID().toString();
        String nome = p.getGameProfile().name();
        String erro = casa.validarAposta(b, id, nome, opcao, valor);
        if (erro != null) return erro;
        if (!tirar(p, valor)) return "Você só tem " + contar(p) + " diamante(s) no inventário.";
        casa.apostar(b, id, nome, opcao, valor);
        salvar();
        p.sendSystemMessage(prefixo("Aposta feita: " + valor + " diamante(s) em " + b.opcaoExata(opcao) + " (bolão #" + b.id + ")."));
        if (valor >= casa.config.avisoGrande)
            avisar(nome + " apostou " + valor + " diamantes em " + b.opcaoExata(opcao) + " no bolão \"" + b.titulo + "\"!");
        return null;
    }

    /** Devolve null se a combinada foi feita, ou o motivo da recusa. */
    public static String apostarCombinada(ServerPlayer p, List<Casa.Perna> pernas, int valor) {
        String id = p.getUUID().toString();
        String nome = p.getGameProfile().name();
        String erro = casa.validarCombinada(id, nome, pernas, valor);
        if (erro != null) return erro;
        int total = valor * pernas.size();
        if (!tirar(p, total)) return "Você precisa de " + total + " diamantes (tem " + contar(p) + ").";
        casa.criarCombinada(id, nome, pernas, valor);
        salvar();
        p.sendSystemMessage(prefixo("Combinada feita: " + pernas.size() + " bolões, " + valor + " diamante(s) em cada. Acertou tudo, a casa paga até "
            + casa.bonusDe(pernas.size(), total) + " de bônus."));
        avisar(nome + " montou uma combinada de " + pernas.size() + " bolões!");
        return null;
    }

    public static void resolver(Bolao b, String vencedor) {
        List<Casa.Pagamento> pag = casa.resolver(b, vencedor, System.currentTimeMillis());
        if (pag.isEmpty()) {
            avisar("Bolão \"" + b.titulo + "\": " + vencedor + " ganhou, mas ninguém acertou. O pote vai pro jackpot (" + casa.jackpot + ").");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Casa.Pagamento x : pag) sb.append(x.nome).append(" +").append(x.valor).append("  ");
            avisar("Bolão \"" + b.titulo + "\": " + vencedor + " ganhou! " + sb.toString().trim());
        }
        for (Casa.Pagamento x : casa.bonusRecentes) avisar("Combinada de " + x.nome + " acertou tudo! Bônus de " + x.valor + " diamante(s).");
        entregarTodos();
        salvar();
        StringBuilder w = new StringBuilder("🏆 **Bolão #" + b.id + " — " + b.titulo + "**\nResultado: **" + vencedor + "**\n");
        if (pag.isEmpty()) {
            w.append("Ninguém acertou. Jackpot acumulado: **").append(casa.jackpot).append("** 💎");
        } else {
            w.append("Quem acertou:\n");
            for (Casa.Pagamento x : pag) w.append("• ").append(x.nome).append(" ganhou **").append(x.valor).append("** 💎\n");
        }
        webhook(w.toString());
    }

    public static void entregarTodos() {
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) entregar(p);
    }

    // ---------- stats e conquistas ----------

    public static Path mundo() {
        return Arquivos.pastaMundo(FabricLoader.getInstance().getGameDir(), casa.config.pastaMundo);
    }

    private static void gravarDados() {
        try {
            server.getPlayerList().saveAll();
        } catch (RuntimeException e) {
            Melbybet.LOGGER.warn("Não consegui forçar o salvamento das stats", e);
        }
    }

    /** nick -> stats do jogador, de todo mundo que já entrou. Cache de 10 s. */
    public static Map<String, JsonObject> statsTodos() {
        long agora = System.currentTimeMillis();
        if (agora - cacheEm < 10_000 && !cacheStats.isEmpty()) return cacheStats;
        gravarDados();
        Map<String, JsonObject> r = new LinkedHashMap<>();
        Path mundo = mundo();
        for (var e : casa.conhecidos.entrySet()) {
            String txt = Arquivos.ler(mundo, "stats", e.getKey());
            if (txt != null) r.put(e.getValue(), StatCat.lerStats(txt));
        }
        cacheStats = r;
        cacheEm = agora;
        return r;
    }

    /** Verifica os bolões resolvidos por conquista. Chamado a cada ~30 s. */
    public static void conferirConquistas() {
        boolean algum = false;
        for (Bolao.Gatilho g : Bolao.Gatilho.values()) {
            if (g.porConquista() && !casa.comGatilho(g).isEmpty()) algum = true;
        }
        if (!algum) return;
        gravarDados();
        Path mundo = mundo();
        for (Bolao.Gatilho g : Bolao.Gatilho.values()) {
            if (!g.porConquista()) continue;
            for (Bolao b : casa.comGatilho(g)) {
                if (b.status == Bolao.Status.AGUARDANDO) continue;
                var d = Resultados.porConquista(g.conquista, casa.conhecidos, u -> Arquivos.ler(mundo, "advancements", u), b.criadoEm);
                if (d == null) continue;
                if (d.claro() && b.opcaoExata(d.vencedores.get(0)) != null) {
                    resolver(b, b.opcaoExata(d.vencedores.get(0)));
                } else if (!d.claro()) {
                    casa.aguardar(b, d.vencedores);
                    avisar("Bolão #" + b.id + " empatou entre " + String.join(", ", d.vencedores) + ". Um OP precisa confirmar: /melbybet resolver " + b.id + " <nick>");
                    salvar();
                }
            }
        }
    }

    // ---------- Discord ----------

    public static void webhook(String texto) {
        String url = casa.config.discordWebhook;
        if (url == null || url.isBlank()) return;
        try {
            JsonObject o = new JsonObject();
            o.addProperty("content", texto);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(o.toString()))
                .build();
            HTTP.sendAsync(req, HttpResponse.BodyHandlers.discarding());
        } catch (RuntimeException e) {
            Melbybet.LOGGER.warn("Webhook do Discord falhou", e);
        }
    }
}
