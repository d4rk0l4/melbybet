package br.melby.melbybet.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Receita de um estandarte: cor da base e camadas (padrão + cor), com as contas de material. Lógica pura. */
public class BannerReceita {
    public static final int MAX_CAMADAS = 6;
    public static final int MAX_FAVORITOS = 20;

    public static class Camada {
        public String padrao = "";
        public String cor = "";

        public Camada() {}

        public Camada(String padrao, String cor) {
            this.padrao = padrao;
            this.cor = cor;
        }
    }

    public String nome = "";
    public String base = "white";
    public List<Camada> camadas = new ArrayList<>();

    /** Cores na ordem do jogo: id (igual ao nome do enum em minúsculas) -> nome em português. */
    public static final Map<String, String> CORES = new LinkedHashMap<>();
    /** Padrões: id do registro -> nome em português. */
    public static final Map<String, String> PADROES = new LinkedHashMap<>();
    /** Padrões que o tear só aceita com um item de padrão de estandarte, e o que se usa para fazê-lo. */
    public static final Map<String, String> ITEM_DO_PADRAO = new LinkedHashMap<>();

    static {
        String[][] c = {{"white", "Branco"}, {"orange", "Laranja"}, {"magenta", "Magenta"}, {"light_blue", "Azul-claro"}, {"yellow", "Amarelo"},
            {"lime", "Verde-limão"}, {"pink", "Rosa"}, {"gray", "Cinza"}, {"light_gray", "Cinza-claro"}, {"cyan", "Ciano"}, {"purple", "Roxo"},
            {"blue", "Azul"}, {"brown", "Marrom"}, {"green", "Verde"}, {"red", "Vermelho"}, {"black", "Preto"}};
        for (String[] x : c) CORES.put(x[0], x[1]);
        String[][] p = {{"stripe_bottom", "Listra inferior"}, {"stripe_top", "Listra superior"}, {"stripe_left", "Listra esquerda"},
            {"stripe_right", "Listra direita"}, {"stripe_center", "Listra vertical central"}, {"stripe_middle", "Listra horizontal central"},
            {"stripe_downright", "Listra diagonal \\"}, {"stripe_downleft", "Listra diagonal /"}, {"small_stripes", "Listras finas"},
            {"cross", "Cruz diagonal (X)"}, {"straight_cross", "Cruz reta (+)"}, {"square_bottom_left", "Quadrado inferior esquerdo"},
            {"square_bottom_right", "Quadrado inferior direito"}, {"square_top_left", "Quadrado superior esquerdo"},
            {"square_top_right", "Quadrado superior direito"}, {"triangle_bottom", "Triângulo inferior"}, {"triangle_top", "Triângulo superior"},
            {"triangles_bottom", "Triângulos inferiores"}, {"triangles_top", "Triângulos superiores"}, {"diagonal_left", "Diagonal esquerda superior"},
            {"diagonal_right", "Diagonal direita inferior"}, {"diagonal_up_left", "Diagonal esquerda inferior"}, {"diagonal_up_right", "Diagonal direita superior"},
            {"circle", "Círculo"}, {"rhombus", "Losango"}, {"half_vertical", "Metade esquerda"}, {"half_vertical_right", "Metade direita"},
            {"half_horizontal", "Metade superior"}, {"half_horizontal_bottom", "Metade inferior"}, {"border", "Borda"}, {"curly_border", "Borda ornamentada"},
            {"gradient", "Degradê (de cima)"}, {"gradient_up", "Degradê (de baixo)"}, {"bricks", "Tijolos"}, {"creeper", "Creeper"}, {"skull", "Caveira"},
            {"flower", "Flor"}, {"mojang", "Coisa"}, {"globe", "Globo"}, {"piglin", "Focinho de piglin"}, {"flow", "Fluxo"}, {"guster", "Rajada"}};
        for (String[] x : p) PADROES.put(x[0], x[1]);
        ITEM_DO_PADRAO.put("creeper", "cabeça de creeper");
        ITEM_DO_PADRAO.put("skull", "cabeça de esqueleto wither");
        ITEM_DO_PADRAO.put("flower", "margarida");
        ITEM_DO_PADRAO.put("mojang", "maçã dourada encantada");
        ITEM_DO_PADRAO.put("globe", "padrão de globo (de aldeão cartógrafo)");
        ITEM_DO_PADRAO.put("piglin", "focinho de piglin (padrão achado em bastião)");
        ITEM_DO_PADRAO.put("flow", "padrão de fluxo (de cofre de provação)");
        ITEM_DO_PADRAO.put("guster", "padrão de rajada (de cofre de provação)");
        ITEM_DO_PADRAO.put("bricks", "bloco de tijolos");
        ITEM_DO_PADRAO.put("curly_border", "vinhas");
    }

    public BannerReceita() {}

    public BannerReceita(String nome, String base, List<Camada> camadas) {
        this.nome = nome;
        this.base = base;
        this.camadas = camadas;
    }

    /** Devolve o motivo se a receita não vale, ou null. */
    public String validar() {
        if (!CORES.containsKey(base)) return "Cor de base desconhecida: " + base;
        if (camadas.size() > MAX_CAMADAS) return "No tear dá para ter no máximo " + MAX_CAMADAS + " camadas.";
        for (Camada c : camadas) {
            if (!PADROES.containsKey(c.padrao)) return "Padrão desconhecido: " + c.padrao;
            if (!CORES.containsKey(c.cor)) return "Cor desconhecida: " + c.cor;
        }
        return null;
    }

    /** Corantes necessários: cor -> quantidade (uma por camada). */
    public Map<String, Integer> corantes() {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (Camada c : camadas) m.merge(c.cor, 1, Integer::sum);
        return m;
    }

    /** Itens de padrão necessários (sem repetir), na ordem das camadas. */
    public List<String> itensDePadrao() {
        Set<String> s = new LinkedHashSet<>();
        for (Camada c : camadas) if (ITEM_DO_PADRAO.containsKey(c.padrao)) s.add(c.padrao);
        return new ArrayList<>(s);
    }

    /** O passo a passo em português, uma linha por passo. */
    public List<String> passos() {
        List<String> l = new ArrayList<>();
        l.add("Base: estandarte " + CORES.getOrDefault(base, base).toLowerCase() + " (6 lãs " + CORES.getOrDefault(base, base).toLowerCase() + " + 1 graveto)");
        int n = 1;
        for (Camada c : camadas) {
            String item = ITEM_DO_PADRAO.get(c.padrao);
            l.add(n++ + "ª camada no tear: " + PADROES.get(c.padrao) + ", " + CORES.getOrDefault(c.cor, c.cor).toLowerCase() + " (1 corante)"
                + (item != null ? "  [precisa do item de padrão: " + item + "]" : ""));
        }
        return l;
    }

    public String resumoMateriais() {
        StringBuilder sb = new StringBuilder("Materiais: 6 lãs + 1 graveto");
        Map<String, Integer> co = corantes();
        for (Map.Entry<String, Integer> e : co.entrySet()) sb.append(", ").append(e.getValue()).append(" corante ").append(CORES.get(e.getKey()).toLowerCase());
        List<String> it = itensDePadrao();
        if (!it.isEmpty()) sb.append(" + itens de padrão (").append(it.size()).append(")");
        return sb.toString();
    }

    public BannerReceita copia() {
        BannerReceita r = new BannerReceita(nome, base, new ArrayList<>());
        for (Camada c : camadas) r.camadas.add(new Camada(c.padrao, c.cor));
        return r;
    }

    private static final class Cru {
        String nome, base;
        List<List<String>> camadas;
    }

    /** Lê o catálogo (JSON de [{nome, base, camadas: [[padrao, cor], ...]}]). Receitas inválidas são descartadas. */
    public static List<BannerReceita> lerCatalogo(String json) {
        List<BannerReceita> r = new ArrayList<>();
        List<Cru> cru;
        try {
            cru = new Gson().fromJson(json, new TypeToken<List<Cru>>() {}.getType());
        } catch (Exception e) {
            return r;
        }
        if (cru == null) return r;
        for (Cru c : cru) {
            if (c == null || c.nome == null || c.base == null) continue;
            BannerReceita b = new BannerReceita(c.nome, c.base, new ArrayList<>());
            if (c.camadas != null) for (List<String> x : c.camadas) if (x != null && x.size() == 2) b.camadas.add(new Camada(x.get(0), x.get(1)));
            if (b.validar() == null) r.add(b);
        }
        return r;
    }

    /** Favoritos de cada jogador, em config/melbybet/banners-favoritos.json. */
    public static class Favoritos {
        public Map<String, List<BannerReceita>> jogadores = new LinkedHashMap<>();

        public List<BannerReceita> de(String uuid) {
            return jogadores.computeIfAbsent(uuid, k -> new ArrayList<>());
        }

        /** Guarda (ou substitui o de mesmo nome). Devolve o erro, ou null. */
        public String salvar(String uuid, String nome, BannerReceita r) {
            String err = MarcadorDados.validarNome(nome);
            if (err != null) return err;
            err = r.validar();
            if (err != null) return err;
            if (r.camadas.isEmpty()) return "Adicione pelo menos uma camada.";
            List<BannerReceita> l = de(uuid);
            BannerReceita c = r.copia();
            c.nome = nome;
            for (int i = 0; i < l.size(); i++) {
                if (l.get(i).nome.equalsIgnoreCase(nome)) {
                    l.set(i, c);
                    return null;
                }
            }
            if (l.size() >= MAX_FAVORITOS) return "Você já tem " + MAX_FAVORITOS + " favoritos. Apague algum.";
            l.add(c);
            return null;
        }

        public boolean apagar(String uuid, String nome) {
            return de(uuid).removeIf(x -> x.nome.equalsIgnoreCase(nome));
        }

        public static Favoritos ler(Path arquivo) {
            Favoritos f = Json.ler(arquivo, Favoritos.class);
            if (f == null) return new Favoritos();
            if (f.jogadores == null) f.jogadores = new LinkedHashMap<>();
            f.jogadores.values().removeIf(l -> l == null);
            for (List<BannerReceita> l : f.jogadores.values()) l.removeIf(r -> r == null || r.validar() != null);
            return f;
        }

        public void gravar(Path arquivo) throws IOException {
            Json.gravar(arquivo, this);
        }
    }
}
