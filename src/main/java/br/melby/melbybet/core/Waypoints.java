package br.melby.melbybet.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Lista de waypoints de um servidor (guardada no cliente do jogador). Lógica pura: ordenação por distância, rumo para a
 * bússola da tela, expiração dos temporários. Nada de minimapa: só pontos com nome.
 */
public class Waypoints {
    public static final int MAX_PONTOS = 200;
    public static final int MAX_NOME = 32;
    /** Cores (RGB) que o jogador escolhe, na ordem do botão de cor. */
    public static final int[] PALETA = {
        0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x00AA00, 0x55FFFF, 0x00AAAA, 0x5555FF, 0xAA00AA, 0xFF55FF, 0xFFFFFF, 0xAAAAAA
    };

    public static final String NORMAL = "normal", TEMP = "temp", CORPO = "corpo", COMPARTILHADO = "compartilhado", BUSCA = "busca";

    public static class Ponto {
        public String id = UUID.randomUUID().toString();
        public String nome = "";
        /** Letra mostrada no losango; vazia = primeira letra do nome. */
        public String inicial = "";
        /** Só o caminho da dimensão: overworld, the_nether, the_end. */
        public String dim = "overworld";
        public int x, y, z;
        /** Cor RGB. */
        public int cor = PALETA[0];
        public boolean visivel = true;
        public String tipo = NORMAL;
        /** Temporário: some nesse instante (ms desde 1970); 0 = sem prazo. */
        public long expiraEm;
        /** Temporário: some ao chegar a esta distância (blocos); 0 = não some por chegada. */
        public int raio;
        public long criadoEm;

        public String letra() {
            if (inicial != null && !inicial.isBlank()) return inicial.substring(0, inicial.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT);
            return inicialDe(nome);
        }

        public boolean automatico() {
            return CORPO.equals(tipo) || BUSCA.equals(tipo);
        }
    }

    public List<Ponto> pontos = new ArrayList<>();
    /** Liga/desliga tudo de uma vez (tecla H). */
    public boolean tudoVisivel = true;
    /** Mostrar a bússola no topo da tela. */
    public boolean bussola = true;
    /** Mostrar o feixe de luz. */
    public boolean feixe = true;
    /** Distância (blocos) dentro da qual um temporário novo some ao chegar. */
    public int raioPadrao = 5;
    /** Minutos de vida de um temporário criado pela tecla (0 = sem prazo). */
    public int minutosTemp = 0;

    public static String inicialDe(String nome) {
        if (nome == null) return "?";
        String t = nome.trim();
        if (t.isEmpty()) return "?";
        return t.substring(0, t.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT);
    }

    /** Devolve o motivo se o nome não serve, ou null. */
    public static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) return "Dê um nome ao waypoint.";
        if (nome.length() > MAX_NOME) return "Nome muito longo (máximo " + MAX_NOME + " letras).";
        for (int i = 0; i < nome.length(); i++) if (Character.isISOControl(nome.charAt(i))) return "O nome tem caracteres inválidos.";
        return null;
    }

    public Ponto porId(String id) {
        for (Ponto p : pontos) if (p.id.equals(id)) return p;
        return null;
    }

    /** Adiciona. Devolve o erro, ou null. Pontos automáticos (corpo) não contam no limite. */
    public String adicionar(Ponto p) {
        String erro = validarNome(p.nome);
        if (erro != null) return erro;
        if (!p.automatico() && manuais() >= MAX_PONTOS) return "Você já tem " + MAX_PONTOS + " waypoints. Apague algum.";
        pontos.add(p);
        return null;
    }

    public int manuais() {
        int n = 0;
        for (Ponto p : pontos) if (!p.automatico()) n++;
        return n;
    }

    public boolean remover(String id) {
        return pontos.removeIf(p -> p.id.equals(id));
    }

    /** Entra ou atualiza um ponto vindo do servidor (corpo, compartilhado), identificado por uma chave. */
    public Ponto upsertDoServidor(String chave, String nome, String dim, int x, int y, int z, String tipo, int cor, long agora) {
        Ponto p = porId(chave);
        if (p == null) {
            p = new Ponto();
            p.id = chave;
            p.criadoEm = agora;
            pontos.add(p);
        }
        p.nome = nome.length() > MAX_NOME ? nome.substring(0, MAX_NOME) : nome;
        p.dim = dim;
        p.x = x;
        p.y = y;
        p.z = z;
        p.tipo = tipo;
        p.cor = cor;
        return p;
    }

    public static double distancia(Ponto p, double x, double y, double z) {
        double dx = p.x + 0.5 - x, dy = p.y + 0.5 - y, dz = p.z + 0.5 - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static double distanciaHorizontal(Ponto p, double x, double z) {
        double dx = p.x + 0.5 - x, dz = p.z + 0.5 - z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** "850 m" ou "1,2 km". */
    public static String formatarDistancia(double m) {
        if (m < 1000) return Math.round(m) + " m";
        return String.format(Locale.ROOT, "%.1f km", m / 1000.0).replace('.', ',');
    }

    /** Rumo (yaw do Minecraft: 0 = sul, 90 = oeste, -90 = leste) de quem está em (x,z) até o ponto. */
    public static double rumo(Ponto p, double x, double z) {
        double dx = p.x + 0.5 - x, dz = p.z + 0.5 - z;
        return Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** Diferença entre dois ângulos, entre -180 e 180. */
    public static double diferenca(double paraOnde, double olhando) {
        double d = (paraOnde - olhando) % 360.0;
        if (d > 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }

    /** Posição do marcador na bússola de -1 (esquerda) a 1 (direita), ou NaN se está fora da faixa visível. */
    public static double fracaoBussola(double diferenca, double meiaFaixa) {
        double abs = Math.abs(diferenca);
        // yaw cresce para a esquerda no Minecraft, então o sinal inverte: alvo à direita tem diferença negativa
        return abs > meiaFaixa ? Double.NaN : -diferenca / meiaFaixa;
    }

    /** Os da dimensão atual por distância, depois os de outras dimensões por nome. */
    public List<Ponto> ordenados(String dim, double x, double y, double z) {
        List<Ponto> r = new ArrayList<>(pontos);
        r.sort(Comparator.<Ponto, Boolean>comparing(p -> !p.dim.equals(dim))
            .thenComparingDouble(p -> p.dim.equals(dim) ? distancia(p, x, y, z) : 0)
            .thenComparing(p -> p.nome.toLowerCase(Locale.ROOT)));
        return r;
    }

    /** Remove e devolve os temporários vencidos (por prazo) ou alcançados (por distância, na mesma dimensão). */
    public List<Ponto> limparTemporarios(long agora, String dim, double x, double y, double z) {
        List<Ponto> saiu = new ArrayList<>();
        for (Ponto p : pontos) {
            if (!TEMP.equals(p.tipo) && !BUSCA.equals(p.tipo)) continue;
            boolean venceu = p.expiraEm > 0 && agora >= p.expiraEm;
            boolean chegou = p.raio > 0 && p.dim.equals(dim) && distancia(p, x, y, z) <= p.raio;
            if (venceu || chegou) saiu.add(p);
        }
        pontos.removeAll(saiu);
        return saiu;
    }

    /** Nome automático para um ponto criado na hora: "Ponto 1", "Ponto 2"... */
    public String nomeNovo() {
        int n = 1;
        while (true) {
            String cand = "Ponto " + n;
            boolean existe = false;
            for (Ponto p : pontos) if (p.nome.equalsIgnoreCase(cand)) existe = true;
            if (!existe) return cand;
            n++;
        }
    }

    public static int corDoIndice(int i) {
        return PALETA[Math.floorMod(i, PALETA.length)];
    }

    /** A cor seguinte da paleta (a primeira se a atual não estiver nela). */
    public static int proximaCor(int atual) {
        for (int i = 0; i < PALETA.length; i++) if (PALETA[i] == atual) return PALETA[(i + 1) % PALETA.length];
        return PALETA[0];
    }

    public static Waypoints ler(Path arquivo) {
        Waypoints w = Json.ler(arquivo, Waypoints.class);
        if (w == null) return new Waypoints();
        if (w.pontos == null) w.pontos = new ArrayList<>();
        w.pontos.removeIf(p -> p == null || p.nome == null || p.dim == null || p.id == null);
        return w;
    }

    public void gravar(Path arquivo) throws IOException {
        Json.gravar(arquivo, this);
    }
}
