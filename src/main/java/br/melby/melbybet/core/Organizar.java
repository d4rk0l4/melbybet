package br.melby.melbybet.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Organização de inventário no cliente: cada jogador diz em que slots gosta de cada tipo de item (espada, comida...) e o
 * botão "Organizar" calcula as trocas para chegar lá. Lógica pura: planeja uma lista de trocas que o cliente executa com
 * três cliques (pegar A, clicar B, clicar A), o que junta pilhas iguais, troca de lugar ou move para um slot vazio.
 */
public final class Organizar {
    private Organizar() {}

    public static final int SLOTS = 36;

    /** Tipos de item que o jogador pode posicionar. A ordem é a de arrumação do que não tem posição fixa. */
    public enum Categoria {
        ESPADA("Espada"), MACHADO("Machado"), PICARETA("Picareta"), PA("Pá"), ENXADA("Enxada"), ARCO("Arco"), BESTA("Besta"),
        TRIDENTE("Tridente"), MACA("Maça"), VARA("Vara de pesca"), FERRAMENTA("Ferramenta"), FLECHA("Flecha"), POCAO("Poção"),
        PEROLA("Pérola"), FOGUETE("Foguete"), TOTEM("Totem"), TOCHA("Tocha"), BALDE("Balde"), COMIDA("Comida"), BLOCO("Bloco"),
        MATERIAL("Material"), OUTRO("Outro");

        public final String rotulo;

        Categoria(String rotulo) {
            this.rotulo = rotulo;
        }
    }

    /** Pilha de itens para o planejamento. {@code chave} identifica o que pode empilhar junto (item + componentes). */
    public record Pilha(String id, String chave, int qtd, int max, Categoria cat) {
        Pilha com(int novaQtd) {
            return new Pilha(id, chave, novaQtd, max, cat);
        }

        boolean cheia() {
            return qtd >= max;
        }
    }

    /** Onde o jogador quer cada tipo (slots 0 a 8 = hotbar, 9 a 35 = mochila) e as opções de uso. */
    public static class Perfil {
        public Map<String, List<Integer>> slots = new LinkedHashMap<>();
        /** "Organizar" também mexe nos slots da hotbar. */
        public boolean organizarHotbar = true;
        /** "Guardar iguais" deixa a hotbar de fora. */
        public boolean guardarIgnoraHotbar = true;
        /** Quando a pilha na mão acaba, puxa a próxima igual do inventário. */
        public boolean reporBloco = true;
        /** Slots travados: o Organizar e o Guardar iguais não mexem neles. */
        public List<Integer> travados = new ArrayList<>();

        public boolean travado(int slot) {
            return travados != null && travados.contains(slot);
        }

        public void alternarTrava(int slot) {
            if (slot < 0 || slot >= SLOTS) return;
            if (travados == null) travados = new ArrayList<>();
            if (!travados.remove(Integer.valueOf(slot))) travados.add(slot);
        }

        public static Perfil padrao() {
            Perfil p = new Perfil();
            // espada 0, picareta 1, machado 2, pá 3, arco 4, comida 5, bloco 6 e 7
            Categoria[] cats = {Categoria.ESPADA, Categoria.PICARETA, Categoria.MACHADO, Categoria.PA, Categoria.ARCO, Categoria.COMIDA, Categoria.BLOCO};
            int[] slots = {0, 1, 2, 3, 4, 5, 6};
            for (int i = 0; i < cats.length; i++) p.slots.computeIfAbsent(cats[i].name(), k -> new ArrayList<>()).add(slots[i]);
            p.slots.get(Categoria.BLOCO.name()).add(7);
            return p;
        }

        public List<Integer> de(Categoria c) {
            List<Integer> l = slots == null ? null : slots.get(c.name());
            return l == null ? List.of() : l;
        }

        /** Põe ou tira o slot da categoria; um slot só pertence a uma categoria. */
        public void alternar(Categoria c, int slot) {
            if (slot < 0 || slot >= SLOTS) return;
            List<Integer> atual = slots.computeIfAbsent(c.name(), k -> new ArrayList<>());
            if (atual.contains(slot)) {
                atual.remove(Integer.valueOf(slot));
            } else {
                for (List<Integer> outra : slots.values()) outra.remove(Integer.valueOf(slot));
                atual.add(slot);
            }
            slots.values().removeIf(List::isEmpty);
        }

        public Categoria dono(int slot) {
            for (Categoria c : Categoria.values()) if (de(c).contains(slot)) return c;
            return null;
        }

        public void limpar() {
            slots.clear();
        }
    }

    public static Perfil lerPerfil(Path arquivo) {
        Perfil p = Json.ler(arquivo, Perfil.class);
        if (p == null) return Perfil.padrao();
        if (p.slots == null) p.slots = new LinkedHashMap<>();
        Map<String, List<Integer>> limpo = new LinkedHashMap<>();
        for (Map.Entry<String, List<Integer>> e : p.slots.entrySet()) {
            try {
                Categoria.valueOf(e.getKey());
            } catch (IllegalArgumentException ex) {
                continue;
            }
            List<Integer> ok = new ArrayList<>();
            if (e.getValue() != null) for (Integer s : e.getValue()) if (s != null && s >= 0 && s < SLOTS && !ok.contains(s)) ok.add(s);
            if (!ok.isEmpty()) limpo.put(e.getKey(), ok);
        }
        p.slots = limpo;
        List<Integer> trav = new ArrayList<>();
        if (p.travados != null) for (Integer t : p.travados) if (t != null && t >= 0 && t < SLOTS && !trav.contains(t)) trav.add(t);
        p.travados = trav;
        return p;
    }

    public static void gravarPerfil(Path arquivo, Perfil p) throws IOException {
        Json.gravar(arquivo, p);
    }

    // ---------- categorias ----------

    /** Categoria de um item pelo id (só o caminho, "iron_sword"). {@code comida} e {@code bloco} vêm do jogo. */
    public static Categoria categoria(String id, boolean comida, boolean bloco) {
        String s = id.toLowerCase(Locale.ROOT);
        if (s.endsWith("_sword")) return Categoria.ESPADA;
        if (s.endsWith("_pickaxe")) return Categoria.PICARETA;
        if (s.endsWith("_axe")) return Categoria.MACHADO;
        if (s.endsWith("_shovel")) return Categoria.PA;
        if (s.endsWith("_hoe")) return Categoria.ENXADA;
        switch (s) {
            case "bow": return Categoria.ARCO;
            case "crossbow": return Categoria.BESTA;
            case "trident": return Categoria.TRIDENTE;
            case "mace": return Categoria.MACA;
            case "fishing_rod": case "carrot_on_a_stick": case "warped_fungus_on_a_stick": return Categoria.VARA;
            case "shears": case "flint_and_steel": case "brush": case "spyglass": case "compass": case "clock": case "lead": return Categoria.FERRAMENTA;
            case "arrow": case "spectral_arrow": case "tipped_arrow": return Categoria.FLECHA;
            case "potion": case "splash_potion": case "lingering_potion": return Categoria.POCAO;
            case "ender_pearl": return Categoria.PEROLA;
            case "firework_rocket": return Categoria.FOGUETE;
            case "totem_of_undying": return Categoria.TOTEM;
            case "torch": case "soul_torch": case "redstone_torch": case "lantern": case "soul_lantern": return Categoria.TOCHA;
            default: break;
        }
        if (s.endsWith("_bucket") || s.equals("bucket")) return Categoria.BALDE;
        if (comida) return Categoria.COMIDA;
        if (s.endsWith("_ingot") || s.endsWith("_nugget") || s.startsWith("raw_") || s.endsWith("_scrap")
            || s.equals("diamond") || s.equals("emerald") || s.equals("coal") || s.equals("charcoal") || s.equals("redstone")
            || s.equals("lapis_lazuli") || s.equals("quartz") || s.equals("amethyst_shard") || s.equals("copper_ingot")) return Categoria.MATERIAL;
        if (bloco) return Categoria.BLOCO;
        return Categoria.OUTRO;
    }

    // ---------- simulação de um clique de troca ----------

    /** Faz "pegar A, clicar B, clicar A" no vetor (como o jogo faria). */
    public static void aplicarTroca(Pilha[] inv, int a, int b) {
        Pilha cursor = inv[a];
        inv[a] = null;
        if (cursor == null) {
            cursor = inv[b];
            inv[b] = null;
        } else if (inv[b] == null) {
            inv[b] = cursor;
            cursor = null;
        } else if (inv[b].chave().equals(cursor.chave())) {
            int espaco = inv[b].max() - inv[b].qtd();
            int move = Math.min(espaco, cursor.qtd());
            inv[b] = inv[b].com(inv[b].qtd() + move);
            cursor = cursor.qtd() - move == 0 ? null : cursor.com(cursor.qtd() - move);
        } else {
            Pilha tmp = inv[b];
            inv[b] = cursor;
            cursor = tmp;
        }
        if (cursor != null) inv[a] = cursor;
    }

    public static Pilha[] aplicar(Pilha[] inicial, List<int[]> trocas) {
        Pilha[] inv = inicial.clone();
        for (int[] t : trocas) aplicarTroca(inv, t[0], t[1]);
        return inv;
    }

    private static boolean igual(Pilha x, Pilha y) {
        if (x == null || y == null) return x == y;
        return x.chave().equals(y.chave()) && x.qtd() == y.qtd();
    }

    // ---------- planejamento ----------

    /** Lista de trocas {a, b} que arruma {@code inicial} (36 posições, null = vazio) segundo o perfil. */
    public static List<int[]> plano(Pilha[] inicial, Perfil perfil) {
        Pilha[] inv = inicial.clone();
        boolean[] mexe = new boolean[SLOTS];
        for (int i = 0; i < SLOTS; i++) mexe[i] = (perfil.organizarHotbar || i >= 9) && !perfil.travado(i);
        List<int[]> ops = new ArrayList<>();

        // 1. junta as pilhas incompletas do mesmo item
        boolean mudou = true;
        while (mudou) {
            mudou = false;
            Map<String, List<Integer>> incompletas = new LinkedHashMap<>();
            for (int i = 0; i < SLOTS; i++) if (mexe[i] && inv[i] != null && !inv[i].cheia()) incompletas.computeIfAbsent(inv[i].chave(), k -> new ArrayList<>()).add(i);
            for (List<Integer> l : incompletas.values()) {
                if (l.size() < 2) continue;
                List<Integer> por = new ArrayList<>(l);
                por.sort((x, y) -> Integer.compare(inv[x].qtd(), inv[y].qtd()));
                int menor = por.get(0), maior = por.get(por.size() - 1);
                ops.add(new int[] {menor, maior});
                aplicarTroca(inv, menor, maior);
                mudou = true;
                break;
            }
        }

        // 2. destino de cada pilha
        List<Integer> ordem = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) if (mexe[i] && inv[i] != null) ordem.add(i);
        final Pilha[] ref = inv.clone();
        ordem.sort((x, y) -> {
            Pilha p = ref[x], q = ref[y];
            int c = Integer.compare(p.cat().ordinal(), q.cat().ordinal());
            if (c != 0) return c;
            c = p.id().compareTo(q.id());
            if (c != 0) return c;
            c = Integer.compare(q.qtd(), p.qtd());
            return c != 0 ? c : p.chave().compareTo(q.chave());
        });
        Pilha[] alvo = new Pilha[SLOTS];
        boolean[] ocupado = new boolean[SLOTS];
        boolean[] reservado = new boolean[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            if (!mexe[i]) {
                alvo[i] = inv[i];
                ocupado[i] = true;
            }
            if (perfil.dono(i) != null) reservado[i] = true;
        }
        List<Integer> semLugar = new ArrayList<>();
        for (int s : ordem) {
            boolean achou = false;
            for (int d : perfil.de(inv[s].cat())) {
                if (!mexe[d] || ocupado[d]) continue;
                alvo[d] = inv[s];
                ocupado[d] = true;
                achou = true;
                break;
            }
            if (!achou) semLugar.add(s);
        }
        for (int s : semLugar) {
            int d = -1;
            // primeiro a mochila e a hotbar livres de perfil, depois os slots reservados que sobraram
            for (int i = 9; i < SLOTS && d < 0; i++) if (!ocupado[i] && !reservado[i] && mexe[i]) d = i;
            for (int i = 0; i < 9 && d < 0; i++) if (!ocupado[i] && !reservado[i] && mexe[i]) d = i;
            for (int i = 9; i < SLOTS && d < 0; i++) if (!ocupado[i] && mexe[i]) d = i;
            for (int i = 0; i < 9 && d < 0; i++) if (!ocupado[i] && mexe[i]) d = i;
            if (d < 0) continue;
            alvo[d] = inv[s];
            ocupado[d] = true;
        }

        // 3. leva cada pilha ao destino
        for (int s = 0; s < SLOTS; s++) {
            if (!mexe[s] || alvo[s] == null || igual(inv[s], alvo[s])) continue;
            int j = -1;
            for (int k = 0; k < SLOTS && j < 0; k++) if (k != s && mexe[k] && igual(inv[k], alvo[s]) && !igual(inv[k], alvo[k])) j = k;
            if (j < 0) continue;
            if (inv[s] != null && inv[s].chave().equals(inv[j].chave())) {
                // o jogo juntaria as duas em vez de trocar: passa por um slot vazio
                int e = -1;
                for (int k = 0; k < SLOTS && e < 0; k++) if (mexe[k] && inv[k] == null && k != s && k != j) e = k;
                if (e < 0) continue;
                for (int[] t : new int[][] {{s, e}, {s, j}, {j, e}}) {
                    ops.add(t);
                    aplicarTroca(inv, t[0], t[1]);
                }
            } else {
                ops.add(new int[] {s, j});
                aplicarTroca(inv, s, j);
            }
        }
        return ops;
    }

    /** O vetor final tem as pilhas do perfil nos slots certos? (usado nos testes e para avisar "já está organizado") */
    public static boolean semTrocas(List<int[]> ops) {
        return ops.isEmpty();
    }

    public static String resumo(Pilha[] inv) {
        StringBuilder sb = new StringBuilder();
        for (Pilha p : inv) sb.append(p == null ? "_" : p.id() + "x" + p.qtd()).append(' ');
        return sb.toString().trim() + " " + Arrays.hashCode(inv);
    }
}
