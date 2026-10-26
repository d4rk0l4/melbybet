package br.melby.melbybet.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estado da casa de apostas e todas as regras. Sem nada do Minecraft aqui:
 * os diamantes são tirados e entregues pela camada do jogo; esta classe só faz as contas.
 */
public class Casa {
    public static class Config {
        /** Parte do pote que a casa fica, em %. */
        public int rakePct = 5;
        public int apostaMin = 1;
        /** Máximo que cada jogador pode pôr num mesmo bolão. */
        public int apostaMax = 64;
        /** A partir de quanto uma aposta vira aviso no chat. */
        public int avisoGrande = 32;
        /** Taxa de quem desiste da aposta (cash out), em %. */
        public int cashoutTaxaPct = 10;
        /** Os nicks da Gangue; viram as opções dos bolões de jogador. */
        public List<String> roster = new ArrayList<>(List.of("d4rk_4g10t4", "vitorcs201012", "saya_neku", "has_edu", "patricksmirnoff", "davigamer58GB"));
        /** Cor do nick no chat: nick (minúsculo) -> degradê. */
        public Map<String, CorNick> cores = new LinkedHashMap<>();
        /** Distância do chat local, em blocos (0 = sem limite). */
        public int chatLocalRaio = 100;
        /** Títulos dados por OP: nick (minúsculo) -> título. */
        public Map<String, Titulo> titulos = new LinkedHashMap<>();
        public List<String> msgEntrar = new ArrayList<>(List.of(
            "{nick} chegou na Gangue!", "Olha quem apareceu: {nick}", "{nick} entrou para perder diamantes",
            "{nick} voltou do além", "A casa agradece a visita de {nick}"));
        public List<String> msgSair = new ArrayList<>(List.of(
            "{nick} foi buscar pão", "{nick} desistiu (por enquanto)", "{nick} saiu com os diamantes... ou sem",
            "Tchau, {nick}! A casa sempre ganha", "{nick} sumiu no Void do mundo real"));
        /** Minutos parado até ficar AFK sozinho (0 = só pelo /afk). */
        public int afkAutoMinutos = 5;
        /** Nicks dos moderadores: usam /inv, /ec e /freeze sem precisar ser OP. */
        public List<String> moderadores = new ArrayList<>();
        /** Opcional: URL de webhook do Discord para resultados. Vazio = desligado. */
        public String discordWebhook = "";
        /** Bônus da combinada, em % do total apostado, por perna além da primeira. Sai do cofre. */
        public int combinadaBonusPct = 25;
        /** Teto do bônus de uma combinada, em diamantes. */
        public int combinadaBonusMax = 64;
        /** Pasta do mundo. Vazio = lê o level-name do server.properties. */
        public String pastaMundo = "";
    }

    public static class Jogador {
        public String nome = "";
        public int apostas;
        public int acertos;
        public long gasto;
        public long ganho;
    }

    /** Título que um OP deu a um jogador (degradê entre duas cores). */
    public static class Titulo {
        public String texto = "";
        public int cor1 = 0xFFAA00;
        public int cor2 = 0xFFE066;
    }

    /** Cor (degradê) do nick no chat, escolhida pelo jogador. */
    public static class CorNick {
        public int cor1 = 0xFFFFFF;
        public int cor2 = 0xFFFFFF;
    }

    /** Uma mensagem privada, guardada para os admins conferirem. */
    public static class Tell {
        public long ts;
        public String de = "";
        public String para = "";
        public String texto = "";
    }

    /** Quando cada jogador entrou e saiu pela última vez. */
    public static class Visto {
        public String nome = "";
        public String uuid = "";
        public long primeira;
        public long entrou;
        public long saiu;
        public boolean online;
        public String dimensao = "";
        public int x, y, z;
    }

    public static class Registro {
        public long ts;
        public String texto = "";

        public Registro() {}

        public Registro(long ts, String texto) {
            this.ts = ts;
            this.texto = texto;
        }
    }

    public static class Pagamento {
        public final String uuid;
        public final String nome;
        public final int valor;

        public Pagamento(String uuid, String nome, int valor) {
            this.uuid = uuid;
            this.nome = nome;
            this.valor = valor;
        }
    }

    public Config config = new Config();
    public int proximoId = 1;
    public int temporada = 1;
    public long cofre;
    /** Pote acumulado de bolões em que ninguém acertou. Vai para o próximo bolão resolvido. */
    public long jackpot;
    public List<Bolao> bolaos = new ArrayList<>();
    /** uuid -> estatísticas de aposta. */
    public Map<String, Jogador> jogadores = new LinkedHashMap<>();
    /** uuid -> diamantes que ainda faltam entregar (jogador offline ou inventário cheio). */
    public Map<String, Integer> creditos = new LinkedHashMap<>();
    public List<Registro> historico = new ArrayList<>();
    /** uuid -> nick de todo mundo que já entrou (usado para achar os arquivos de stats). */
    public Map<String, String> conhecidos = new LinkedHashMap<>();
    /** uuids de quem já recebeu o livro de boas-vindas. */
    public List<String> tutorial = new ArrayList<>();
    /** Últimas mensagens privadas (até 300). */
    public List<Tell> logTells = new ArrayList<>();
    /** nick (minúsculo) -> última vez visto. */
    public Map<String, Visto> vistos = new LinkedHashMap<>();

    public static class Local {
        public String dim = "overworld";
        public int x, y, z;
    }

    /** Locais salvos por jogador (uuid -> nome -> local). Só anotações: não teleporta. */
    public Map<String, Map<String, Local>> locais = new LinkedHashMap<>();

    // ---------- combinadas ----------

    public static class Perna {
        public int bolao;
        public String opcao = "";

        public Perna() {}

        public Perna(int bolao, String opcao) {
            this.bolao = bolao;
            this.opcao = opcao;
        }
    }

    /** Várias apostas feitas de uma vez; se todas acertarem, a casa paga um bônus. */
    public static class Combinada {
        public int id;
        public String uuid = "";
        public String nome = "";
        public int valorPorPerna;
        public List<Perna> pernas = new ArrayList<>();
        /** PENDENTE, GANHA, PERDIDA ou CANCELADA. */
        public String estado = "PENDENTE";
        public int bonus;
    }

    public int proximaCombinada = 1;
    public List<Combinada> combinadas = new ArrayList<>();
    /** Bônus pagos pela última resolução (só para avisar; não é salvo). */
    public transient List<Pagamento> bonusRecentes = new ArrayList<>();

    /** Bônus se todas as pernas acertarem (antes do limite do cofre). */
    public int bonusDe(int pernas, int totalApostado) {
        long b = (long) totalApostado * config.combinadaBonusPct * Math.max(0, pernas - 1) / 100;
        return (int) Math.min(b, config.combinadaBonusMax);
    }

    /** null = pode; senão o motivo. */
    public String validarCombinada(String uuid, String nome, List<Perna> pernas, int valor) {
        if (pernas.size() < 2) return "Escolha pelo menos 2 bolões.";
        if (pernas.size() > 4) return "No máximo 4 bolões por combinada.";
        List<Integer> vistos = new ArrayList<>();
        for (Perna pn : pernas) {
            if (vistos.contains(pn.bolao)) return "Não repita o mesmo bolão.";
            vistos.add(pn.bolao);
            Bolao b = achar(pn.bolao);
            if (b == null) return "Bolão #" + pn.bolao + " não existe.";
            String e = validarAposta(b, uuid, nome, pn.opcao, valor);
            if (e != null) return "#" + b.id + ": " + e;
        }
        return null;
    }

    /** Registra as apostas de cada perna e a combinada. Chame só depois de tirar valor * pernas diamantes. */
    public Combinada criarCombinada(String uuid, String nome, List<Perna> pernas, int valor) {
        Combinada c = new Combinada();
        c.id = proximaCombinada++;
        c.uuid = uuid;
        c.nome = nome;
        c.valorPorPerna = valor;
        for (Perna pn : pernas) {
            Bolao b = achar(pn.bolao);
            String exata = b.opcaoExata(pn.opcao);
            apostar(b, uuid, nome, exata, valor);
            c.pernas.add(new Perna(pn.bolao, exata));
        }
        combinadas.add(c);
        return c;
    }

    public List<Combinada> combinadasDe(String uuid) {
        List<Combinada> r = new ArrayList<>();
        for (Combinada c : combinadas) if (c.uuid.equals(uuid)) r.add(c);
        return r;
    }

    private static boolean temAposta(Bolao b, String uuid, String opcao) {
        for (Bolao.Aposta a : b.apostas) if (a.uuid.equals(uuid) && a.opcao.equalsIgnoreCase(opcao)) return true;
        return false;
    }

    /** Confere as combinadas pendentes depois que um bolão mudou. Devolve os bônus pagos (já lançados nos créditos). */
    public List<Pagamento> avaliarCombinadas(long agora) {
        List<Pagamento> pagos = new ArrayList<>();
        for (Combinada c : combinadas) {
            if (!c.estado.equals("PENDENTE")) continue;
            boolean todasGanhas = true;
            String novo = null;
            for (Perna pn : c.pernas) {
                Bolao b = achar(pn.bolao);
                if (b == null || b.status == Bolao.Status.CANCELADO || !temAposta(b, c.uuid, pn.opcao)) {
                    novo = "CANCELADA";
                    break;
                }
                if (b.status == Bolao.Status.RESOLVIDO) {
                    if (!pn.opcao.equalsIgnoreCase(b.vencedor)) {
                        novo = "PERDIDA";
                        break;
                    }
                } else {
                    todasGanhas = false;
                }
            }
            if (novo == null && todasGanhas) novo = "GANHA";
            if (novo == null) continue;
            c.estado = novo;
            if (novo.equals("GANHA")) {
                int total = c.valorPorPerna * c.pernas.size();
                int bonus = (int) Math.min(bonusDe(c.pernas.size(), total), Math.max(0, cofre));
                c.bonus = bonus;
                if (bonus > 0) {
                    cofre -= bonus;
                    credito(c.uuid, bonus);
                    jogador(c.uuid, c.nome).ganho += bonus;
                    pagos.add(new Pagamento(c.uuid, c.nome, bonus));
                }
                historico.add(new Registro(agora, "Combinada de " + c.nome + " (" + c.pernas.size() + " bolões) acertou tudo, bônus " + bonus));
            }
        }
        return pagos;
    }

    // ---------- mensagens privadas ----------

    public void registrarTell(String de, String para, String texto, long agora) {
        Tell t = new Tell();
        t.ts = agora;
        t.de = de;
        t.para = para;
        t.texto = texto;
        logTells.add(t);
        while (logTells.size() > 300) logTells.remove(0);
    }

    /** As últimas mensagens em que o jogador falou ou recebeu, da mais antiga para a mais nova. */
    public List<Tell> tellsDe(String nick, int max) {
        List<Tell> r = new ArrayList<>();
        for (int i = logTells.size() - 1; i >= 0 && r.size() < max; i--) {
            Tell t = logTells.get(i);
            if (t.de.equalsIgnoreCase(nick) || t.para.equalsIgnoreCase(nick)) r.add(0, t);
        }
        return r;
    }

    // ---------- consultas ----------

    public Bolao achar(int id) {
        for (Bolao b : bolaos) if (b.id == id) return b;
        return null;
    }

    public List<Bolao> ativos() {
        List<Bolao> r = new ArrayList<>();
        for (Bolao b : bolaos) if (b.emAndamento()) r.add(b);
        return r;
    }

    public List<Bolao> comGatilho(Bolao.Gatilho g) {
        List<Bolao> r = new ArrayList<>();
        for (Bolao b : bolaos) if (b.emAndamento() && b.gatilho == g) r.add(b);
        return r;
    }

    /** Multiplicador estimado se a opção ganhar agora (ex.: 2.4 = recebe 2,4x). 0 se ninguém apostou nela. */
    public double odd(Bolao b, String opcao) {
        int naOpcao = b.poteDa(opcao);
        if (naOpcao <= 0) return 0;
        double liquido = b.pote() * (100 - config.rakePct) / 100.0 + jackpot;
        return liquido / naOpcao;
    }

    // ---------- administração ----------

    public Bolao criar(String titulo, Bolao.Gatilho gatilho, List<String> opcoes, long agora) {
        Bolao b = new Bolao();
        b.id = proximoId++;
        b.titulo = titulo;
        b.gatilho = gatilho == null ? Bolao.Gatilho.MANUAL : gatilho;
        b.opcoes = new ArrayList<>(opcoes);
        b.criadoEm = agora;
        b.status = Bolao.Status.FECHADO;
        bolaos.add(b);
        return b;
    }

    /** Devolve null se deu certo, ou o motivo. */
    public String abrir(Bolao b) {
        if (b.status != Bolao.Status.FECHADO) return "Só dá para abrir um bolão fechado.";
        if (b.opcoes.size() < 2) return "O bolão precisa de pelo menos 2 opções.";
        b.status = Bolao.Status.ABERTO;
        return null;
    }

    public String fechar(Bolao b) {
        if (b.status != Bolao.Status.ABERTO) return "O bolão não está aberto.";
        b.status = Bolao.Status.FECHADO;
        return null;
    }

    // ---------- apostas ----------

    /** Valida a aposta. null = pode apostar; senão, o motivo. */
    public String validarAposta(Bolao b, String uuid, String nome, String opcao, int valor) {
        if (!b.aberto()) return "Esse bolão não está aberto.";
        if (b.opcaoExata(opcao) == null) return "Opção inválida.";
        if (b.opcaoExata(opcao).equalsIgnoreCase(nome)) return "Não pode apostar em você mesmo.";
        if (valor < config.apostaMin) return "Aposta mínima: " + config.apostaMin + " diamante(s).";
        if (b.apostadoPor(uuid) + valor > config.apostaMax)
            return "Limite por bolão: " + config.apostaMax + " diamantes (você já pôs " + b.apostadoPor(uuid) + ").";
        return null;
    }

    /** Registra a aposta. Chame só depois de tirar os diamantes. */
    public void apostar(Bolao b, String uuid, String nome, String opcao, int valor) {
        String exata = b.opcaoExata(opcao);
        b.apostas.add(new Bolao.Aposta(uuid, nome, exata, valor));
        Jogador j = jogador(uuid, nome);
        j.apostas++;
        j.gasto += valor;
    }

    /** Desiste das apostas do jogador no bolão aberto. Devolve quanto volta (já sem a taxa), ou -1. */
    public int cashout(Bolao b, String uuid) {
        if (!b.aberto()) return -1;
        int total = b.apostadoPor(uuid);
        if (total <= 0) return -1;
        int volta = total * (100 - config.cashoutTaxaPct) / 100;
        cofre += total - volta;
        b.apostas.removeIf(a -> a.uuid.equals(uuid));
        Jogador j = jogadores.get(uuid);
        if (j != null) {
            j.gasto -= total;
            j.apostas = Math.max(0, j.apostas - 1);
        }
        credito(uuid, volta);
        return volta;
    }

    // ---------- resultado ----------

    /** Resolve o bolão. Devolve os pagamentos (já lançados nos créditos). */
    public List<Pagamento> resolver(Bolao b, String vencedor, long agora) {
        String venc = b.opcaoExata(vencedor);
        if (venc == null) venc = vencedor;
        b.vencedor = venc;
        b.status = Bolao.Status.RESOLVIDO;
        b.candidatos.clear();

        int pote = b.pote();
        int rake = pote * config.rakePct / 100;
        long distribuir = (long) pote - rake + jackpot;
        cofre += rake;

        int vencedoresTotal = b.poteDa(venc);
        List<Pagamento> pag = new ArrayList<>();
        if (vencedoresTotal <= 0) {
            jackpot = distribuir;
            historico.add(new Registro(agora, "#" + b.id + " " + b.titulo + ": " + venc + " ganhou, ninguém acertou (jackpot " + jackpot + ")"));
            bonusRecentes = avaliarCombinadas(agora);
            return pag;
        }
        jackpot = 0;
        long pago = 0;
        for (Bolao.Aposta a : b.apostas) {
            if (!a.opcao.equalsIgnoreCase(venc)) continue;
            int v = (int) (distribuir * a.valor / vencedoresTotal);
            pago += v;
            pag.add(new Pagamento(a.uuid, a.nome, v));
            credito(a.uuid, v);
            Jogador j = jogador(a.uuid, a.nome);
            j.acertos++;
            j.ganho += v;
        }
        cofre += distribuir - pago;
        b.pagoTotal = (int) pago;
        historico.add(new Registro(agora, "#" + b.id + " " + b.titulo + ": " + venc + " ganhou, pagou " + pago + " diamantes"));
        bonusRecentes = avaliarCombinadas(agora);
        return pag;
    }

    /** Marca como aguardando confirmação de um OP, quando o resultado é ambíguo. */
    public void aguardar(Bolao b, List<String> candidatos) {
        b.status = Bolao.Status.AGUARDANDO;
        b.candidatos = new ArrayList<>(candidatos);
    }

    /** Cancela e devolve todas as apostas inteiras. */
    public void cancelar(Bolao b, long agora) {
        for (Bolao.Aposta a : b.apostas) {
            credito(a.uuid, a.valor);
            Jogador j = jogador(a.uuid, a.nome);
            j.gasto -= a.valor;
            j.apostas = Math.max(0, j.apostas - 1);
        }
        b.status = Bolao.Status.CANCELADO;
        historico.add(new Registro(agora, "#" + b.id + " " + b.titulo + ": cancelado, apostas devolvidas"));
        avaliarCombinadas(agora);
    }

    // ---------- créditos, jogadores, ranking ----------

    public void credito(String uuid, int valor) {
        if (valor <= 0) return;
        creditos.merge(uuid, valor, Integer::sum);
    }

    public int creditoDe(String uuid) {
        return creditos.getOrDefault(uuid, 0);
    }

    public void baixarCredito(String uuid, int entregue) {
        int resto = creditoDe(uuid) - entregue;
        if (resto <= 0) creditos.remove(uuid);
        else creditos.put(uuid, resto);
    }

    public Jogador jogador(String uuid, String nome) {
        Jogador j = jogadores.computeIfAbsent(uuid, k -> new Jogador());
        if (nome != null && !nome.isEmpty()) j.nome = nome;
        return j;
    }

    /** Ranking por lucro (ganho - gasto), depois acertos. */
    public List<Jogador> rankingApostadores() {
        List<Jogador> l = new ArrayList<>(jogadores.values());
        l.removeIf(j -> j.apostas == 0 && j.ganho == 0);
        l.sort((a, b) -> {
            int c = Long.compare(b.ganho - b.gasto, a.ganho - a.gasto);
            return c != 0 ? c : Integer.compare(b.acertos, a.acertos);
        });
        return l;
    }

    /** Título de sátira conforme a posição e o histórico. */
    public static String titulo(int posicao, Jogador j, int totalJogadores) {
        if (posicao == 1 && j.ganho > j.gasto) return "Rei da Banca";
        if (j.apostas >= 3 && j.acertos == 0) return "Pé-Frio Oficial";
        if (posicao == totalJogadores && j.ganho < j.gasto) return "Doador da Casa";
        if (j.apostas >= 5 && j.acertos * 2 >= j.apostas) return "Profeta";
        return "Apostador";
    }

    /** Nova temporada: zera as estatísticas de apostas (bolões e créditos pendentes ficam). */
    public void novaTemporada(long agora) {
        List<Jogador> r = rankingApostadores();
        String campeao = r.isEmpty() ? "ninguém" : r.get(0).nome;
        historico.add(new Registro(agora, "Fim da temporada " + temporada + ". Campeão: " + campeao));
        temporada++;
        jogadores.clear();
    }
}
