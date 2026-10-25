package br.melby.melbybet.core;

import java.util.ArrayList;
import java.util.List;

/** Um bolão. Só dados simples, para o Gson salvar e ler direto. */
public class Bolao {
    public enum Status { FECHADO, ABERTO, AGUARDANDO, RESOLVIDO, CANCELADO }

    /** O que resolve o bolão sozinho. MANUAL = um OP declara o vencedor. */
    public enum Gatilho {
        MANUAL("Manual (OP decide)", null),
        VOID("Morrer no Void", null),
        WITHER("Matar o Wither", null),
        DRAGAO("Matar o Dragão", null),
        DIAMANTE("Primeiro diamante", "minecraft:story/mine_diamond"),
        NETHER("Primeiro no Nether", "minecraft:story/enter_the_nether"),
        END("Primeiro no End", "minecraft:story/enter_the_end"),
        ELYTRA("Primeira elytra", "minecraft:end/elytra"),
        NETHERITE("Primeira armadura de netherite", "minecraft:nether/netherite_armor");

        public final String rotulo;
        /** Conquista que resolve (null = outro tipo de gatilho). */
        public final String conquista;

        Gatilho(String rotulo, String conquista) {
            this.rotulo = rotulo;
            this.conquista = conquista;
        }

        public boolean porConquista() { return conquista != null; }

        public static Gatilho de(String s) {
            if (s == null) return null;
            for (Gatilho g : values()) if (g.name().equalsIgnoreCase(s.trim())) return g;
            return null;
        }
    }

    public static class Aposta {
        public String uuid;
        public String nome;
        public String opcao;
        public int valor;

        public Aposta() {}

        public Aposta(String uuid, String nome, String opcao, int valor) {
            this.uuid = uuid;
            this.nome = nome;
            this.opcao = opcao;
            this.valor = valor;
        }
    }

    public int id;
    public String titulo = "";
    public Gatilho gatilho = Gatilho.MANUAL;
    public Status status = Status.FECHADO;
    public List<String> opcoes = new ArrayList<>();
    public List<Aposta> apostas = new ArrayList<>();
    public String vencedor;
    /** Candidatos quando o resultado ficou ambíguo (status AGUARDANDO). */
    public List<String> candidatos = new ArrayList<>();
    public long criadoEm;
    /** Instante (ms) em que fecha sozinho; 0 = sem prazo. */
    public long expiraEm;
    public int pagoTotal;

    public int pote() {
        int t = 0;
        for (Aposta a : apostas) t += a.valor;
        return t;
    }

    public int poteDa(String opcao) {
        int t = 0;
        for (Aposta a : apostas) if (a.opcao.equalsIgnoreCase(opcao)) t += a.valor;
        return t;
    }

    public int apostadoPor(String uuid) {
        int t = 0;
        for (Aposta a : apostas) if (a.uuid.equals(uuid)) t += a.valor;
        return t;
    }

    public boolean aberto() { return status == Status.ABERTO; }

    public boolean emAndamento() { return status == Status.ABERTO || status == Status.FECHADO || status == Status.AGUARDANDO; }

    /** Devolve a opção com a grafia cadastrada, ou null se não existir. */
    public String opcaoExata(String texto) {
        if (texto == null) return null;
        for (String o : opcoes) if (o.equalsIgnoreCase(texto.trim())) return o;
        return null;
    }
}
