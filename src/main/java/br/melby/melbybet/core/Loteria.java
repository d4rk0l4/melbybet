package br.melby.melbybet.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/** Loteria da Gangue: bilhetes em diamantes, tempo configurável, um sorteado leva o pote (menos a taxa da casa). */
public class Loteria {
    public boolean ativa;
    public int preco = 1;
    public long fimMs;
    public int maxPorPessoa = 10;
    public int minutos;
    public Map<String, Integer> bilhetes = new LinkedHashMap<>();
    public Map<String, String> nomes = new LinkedHashMap<>();

    public static class Resultado {
        public String uuid = "", nome = "";
        public int premio, bilhetesDoVencedor, totalBilhetes, participantes;
        /** true = menos de 2 pessoas entraram: todos os bilhetes foram devolvidos. */
        public boolean devolvido;
    }

    public int total() {
        int t = 0;
        for (int v : bilhetes.values()) t += v;
        return t;
    }

    public int pote() {
        return total() * preco;
    }

    public int bilhetesDe(String uuid) {
        return bilhetes.getOrDefault(uuid, 0);
    }

    public long restanteMs(long agora) {
        return ativa ? Math.max(0, fimMs - agora) : 0;
    }

    /** Abre uma loteria. Devolve o motivo do erro, ou null. */
    public String abrir(int preco, int minutos, int maxPorPessoa, long agora) {
        if (ativa) return "Já tem uma loteria rolando. Sorteie ou cancele antes.";
        if (preco < 1 || preco > 64) return "O bilhete custa de 1 a 64 diamantes.";
        if (minutos < 1 || minutos > 10080) return "O tempo vai de 1 minuto a 7 dias (10080 minutos).";
        if (maxPorPessoa < 1 || maxPorPessoa > 100) return "O limite de bilhetes por pessoa vai de 1 a 100.";
        this.preco = preco;
        this.minutos = minutos;
        this.maxPorPessoa = maxPorPessoa;
        this.fimMs = agora + minutos * 60_000L;
        this.ativa = true;
        bilhetes.clear();
        nomes.clear();
        return null;
    }

    public String validarCompra(String uuid, int qtd, long agora) {
        if (!ativa || agora >= fimMs) return "Não tem loteria aberta agora.";
        if (qtd < 1) return "Compre pelo menos 1 bilhete.";
        if (bilhetesDe(uuid) + qtd > maxPorPessoa)
            return "O limite é " + maxPorPessoa + " bilhete(s) por pessoa (você tem " + bilhetesDe(uuid) + ").";
        return null;
    }

    public void comprar(String uuid, String nome, int qtd) {
        bilhetes.merge(uuid, qtd, Integer::sum);
        nomes.put(uuid, nome);
    }

    private void devolverTudo(Casa casa) {
        for (var e : bilhetes.entrySet()) casa.credito(e.getKey(), e.getValue() * preco);
    }

    private void zerar() {
        ativa = false;
        bilhetes.clear();
        nomes.clear();
    }

    /** Sorteia (ou devolve, se entrou menos de 2 pessoas). Null se não havia loteria. */
    public Resultado sortear(Casa casa, Random r, long agora) {
        if (!ativa) return null;
        Resultado res = new Resultado();
        res.totalBilhetes = total();
        res.participantes = bilhetes.size();
        if (res.participantes < 2) {
            res.devolvido = true;
            devolverTudo(casa);
            casa.historico.add(new Casa.Registro(agora, "Loteria: menos de 2 pessoas, bilhetes devolvidos"));
            zerar();
            return res;
        }
        int pote = pote();
        int rake = pote * casa.config.rakePct / 100;
        res.premio = pote - rake;
        casa.cofre += rake;
        int alvo = r.nextInt(res.totalBilhetes);
        for (var e : bilhetes.entrySet()) {
            alvo -= e.getValue();
            if (alvo < 0) {
                res.uuid = e.getKey();
                res.nome = nomes.getOrDefault(e.getKey(), "?");
                res.bilhetesDoVencedor = e.getValue();
                break;
            }
        }
        casa.credito(res.uuid, res.premio);
        casa.historico.add(new Casa.Registro(agora, "Loteria: " + res.nome + " levou " + res.premio + " diamantes (" + res.totalBilhetes + " bilhetes)"));
        zerar();
        return res;
    }

    /** Cancela e devolve todos os bilhetes. */
    public boolean cancelar(Casa casa, long agora) {
        if (!ativa) return false;
        devolverTudo(casa);
        casa.historico.add(new Casa.Registro(agora, "Loteria cancelada, bilhetes devolvidos"));
        zerar();
        return true;
    }
}
