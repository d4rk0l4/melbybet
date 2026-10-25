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
        public List<String> roster = new ArrayList<>(List.of("d4rk_4g10t4", "vitorcs201012", "saya_neku", "has_edu", "patricksmirnoff"));
        /** Opcional: URL de webhook do Discord para resultados. Vazio = desligado. */
        public String discordWebhook = "";
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
