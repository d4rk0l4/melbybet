package br.melby.melbybet.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Poses de armor stand guardadas por jogador, e as contas de ângulo do editor. Lógica pura. */
public class PosePreset {
    public static final int MAX_PRESETS = 20;
    public static final int PARTES = 6;
    public static final String[] NOMES = {"Cabeça", "Corpo", "Braço esquerdo", "Braço direito", "Perna esquerda", "Perna direita"};

    public static class Pose {
        public String nome = "";
        /** 6 partes (cabeça, corpo, braço esq., braço dir., perna esq., perna dir.) x 3 eixos, em graus. */
        public float[][] angulos = new float[PARTES][3];
        public boolean bracos;
        public boolean base = true;
        public long criadoEm;

        public Pose copia() {
            Pose p = new Pose();
            p.nome = nome;
            for (int i = 0; i < PARTES; i++) p.angulos[i] = angulos[i].clone();
            p.bracos = bracos;
            p.base = base;
            p.criadoEm = criadoEm;
            return p;
        }
    }

    /** Soma um passo e mantém o ângulo entre -180 e 180. */
    public static float girar(float atual, float passo) {
        float v = (atual + passo) % 360f;
        if (v > 180f) v -= 360f;
        if (v <= -180f) v += 360f;
        return v;
    }

    public static final int[] PASSOS = {1, 5, 15, 45};

    public static int proximoPasso(int atual) {
        for (int i = 0; i < PASSOS.length; i++) if (PASSOS[i] == atual) return PASSOS[(i + 1) % PASSOS.length];
        return PASSOS[0];
    }

    /** Pose "espelhada": troca esquerda e direita e inverte os eixos Y e Z (que giram para o lado). */
    public static Pose espelhar(Pose p) {
        Pose r = p.copia();
        r.angulos[2] = espelho(p.angulos[3]);
        r.angulos[3] = espelho(p.angulos[2]);
        r.angulos[4] = espelho(p.angulos[5]);
        r.angulos[5] = espelho(p.angulos[4]);
        r.angulos[0] = espelho(p.angulos[0]);
        r.angulos[1] = espelho(p.angulos[1]);
        return r;
    }

    private static float[] espelho(float[] a) {
        return new float[] {a[0], girar(0, -a[1]), girar(0, -a[2])};
    }

    /** uuid -> poses guardadas. */
    public Map<String, List<Pose>> jogadores = new LinkedHashMap<>();

    public List<Pose> de(String uuid) {
        return jogadores.computeIfAbsent(uuid, k -> new ArrayList<>());
    }

    /** Guarda (ou substitui a de mesmo nome). Devolve o erro, ou null. */
    public String salvar(String uuid, String nome, Pose pose, long agora) {
        String err = MarcadorDados.validarNome(nome);
        if (err != null) return err;
        List<Pose> l = de(uuid);
        Pose c = pose.copia();
        c.nome = nome;
        c.criadoEm = agora;
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).nome.equalsIgnoreCase(nome)) {
                l.set(i, c);
                return null;
            }
        }
        if (l.size() >= MAX_PRESETS) return "Você já tem " + MAX_PRESETS + " poses guardadas. Apague alguma.";
        l.add(c);
        return null;
    }

    public Pose obter(String uuid, String nome) {
        for (Pose p : de(uuid)) if (p.nome.equalsIgnoreCase(nome)) return p;
        return null;
    }

    public boolean apagar(String uuid, String nome) {
        return de(uuid).removeIf(p -> p.nome.equalsIgnoreCase(nome));
    }

    public static PosePreset ler(Path arquivo) {
        PosePreset d = Json.ler(arquivo, PosePreset.class);
        if (d == null) return new PosePreset();
        if (d.jogadores == null) d.jogadores = new LinkedHashMap<>();
        d.jogadores.values().removeIf(l -> l == null);
        for (List<Pose> l : d.jogadores.values()) {
            l.removeIf(p -> p == null);
            for (Pose p : l) {
                if (p.angulos == null || p.angulos.length != PARTES) p.angulos = new float[PARTES][3];
                for (int i = 0; i < PARTES; i++) if (p.angulos[i] == null || p.angulos[i].length != 3) p.angulos[i] = new float[3];
            }
        }
        return d;
    }

    public void gravar(Path arquivo) throws IOException {
        Json.gravar(arquivo, this);
    }
}
