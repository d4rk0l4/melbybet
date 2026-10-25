package br.melby.melbybet.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Set;

/** As categorias do /top e como cada uma sai das estatísticas do vanilla. */
public enum StatCat {
    DISTANCIA("Distância total", "dist"),
    ANDOU("Mais andou", "km"),
    CORREU("Mais correu", "km"),
    VOOU("Mais voou", "km"),
    NADOU("Mais nadou", "km"),
    CAVALGOU("Mais cavalgou", "km"),
    QUEBROU("Mais quebrou blocos", "blocos"),
    COLOCOU("Mais colocou blocos", "blocos"),
    MATOU("Mais matou mobs", "mobs"),
    MORREU("Mais morreu", "x"),
    TEMPO("Mais tempo online", "h"),
    CRAFTOU("Mais craftou", "itens");

    public final String rotulo;
    private final String unidade;

    StatCat(String rotulo, String unidade) {
        this.rotulo = rotulo;
        this.unidade = unidade;
    }

    private static final String[] MOVIMENTO = {
        "walk", "sprint", "crouch", "swim", "walk_on_water", "walk_under_water", "fly", "aviate",
        "boat", "horse", "minecart", "pig", "strider", "climb", "nautilus", "happy_ghast"
    };

    /** Itens usados que não são blocos; o vanilla não separa, então isto é aproximado. */
    private static final Set<String> NAO_BLOCO = Set.of(
        "bow", "crossbow", "trident", "shield", "fishing_rod", "flint_and_steel", "shears", "elytra",
        "ender_pearl", "ender_eye", "snowball", "egg", "firework_rocket", "carrot_on_a_stick",
        "warped_fungus_on_a_stick", "brush", "mace", "spyglass", "compass", "clock", "totem_of_undying",
        "bone_meal", "lead", "name_tag", "saddle", "wind_charge", "fire_charge", "ender_chest_dummy",
        "water_bucket", "lava_bucket", "milk_bucket", "bucket", "powder_snow_bucket");

    private static boolean ehBloco(String id) {
        String n = id.startsWith("minecraft:") ? id.substring(10) : id;
        if (NAO_BLOCO.contains(n)) return false;
        for (String suf : new String[] {"_sword", "_axe", "_pickaxe", "_shovel", "_hoe", "_spear", "_bucket",
                "_helmet", "_chestplate", "_leggings", "_boots", "_potion", "_arrow", "_spawn_egg", "_boat", "_raft"}) {
            if (n.endsWith(suf)) return false;
        }
        for (String pre : new String[] {"potion", "splash_", "lingering_", "tipped_", "enchanted_", "music_disc"}) {
            if (n.startsWith(pre)) return false;
        }
        return true;
    }

    private static double somaMapa(JsonObject stats, String grupo) {
        JsonElement e = stats.get(grupo);
        if (e == null || !e.isJsonObject()) return 0;
        double t = 0;
        for (var en : e.getAsJsonObject().entrySet()) t += numero(en.getValue());
        return t;
    }

    private static double numero(JsonElement e) {
        try {
            return e.getAsDouble();
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private static double custom(JsonObject stats, String... nomes) {
        JsonElement e = stats.get("minecraft:custom");
        if (e == null || !e.isJsonObject()) return 0;
        JsonObject c = e.getAsJsonObject();
        double t = 0;
        for (String n : nomes) {
            JsonElement v = c.get(n.startsWith("minecraft:") ? n : "minecraft:" + n);
            if (v != null) t += numero(v);
        }
        return t;
    }

    private static String[] mov(String... base) {
        String[] r = new String[base.length];
        for (int i = 0; i < base.length; i++) r[i] = base[i] + "_one_cm";
        return r;
    }

    /** Valor bruto: cm para distâncias, ticks para tempo, contagem nas demais. */
    public double bruto(JsonObject stats) {
        switch (this) {
            case DISTANCIA: return custom(stats, mov(MOVIMENTO));
            case ANDOU: return custom(stats, mov("walk"));
            case CORREU: return custom(stats, mov("sprint"));
            case VOOU: return custom(stats, mov("aviate", "fly"));
            case NADOU: return custom(stats, mov("swim", "walk_on_water", "walk_under_water"));
            case CAVALGOU: return custom(stats, mov("horse"));
            case QUEBROU: return somaMapa(stats, "minecraft:mined");
            case COLOCOU: {
                JsonElement e = stats.get("minecraft:used");
                if (e == null || !e.isJsonObject()) return 0;
                double t = 0;
                for (var en : e.getAsJsonObject().entrySet()) if (ehBloco(en.getKey())) t += numero(en.getValue());
                return t;
            }
            case MATOU: return custom(stats, "mob_kills");
            case MORREU: return custom(stats, "deaths");
            case TEMPO: return custom(stats, "play_time") + custom(stats, "play_one_minute");
            case CRAFTOU: return somaMapa(stats, "minecraft:crafted");
            default: return 0;
        }
    }

    /** Valor usado para ordenar: km, horas ou contagem. */
    public double valor(JsonObject stats) {
        double b = bruto(stats);
        switch (unidade) {
            case "dist": case "km": return b / 100000.0;
            case "h": return b / 72000.0;
            default: return b;
        }
    }

    public String formatar(double v) {
        switch (unidade) {
            case "dist": case "km": return Fmt.decimal(v, 2) + " km";
            case "h": return Fmt.decimal(v, 2) + "h";
            case "blocos": return Fmt.inteiro(Math.round(v)) + " blocos";
            case "mobs": return Fmt.inteiro(Math.round(v)) + " mobs";
            case "itens": return Fmt.inteiro(Math.round(v)) + " itens";
            default: return Fmt.inteiro(Math.round(v)) + "x";
        }
    }

    /** Lê o JSON de estatísticas do jogador (world/.../stats/uuid.json) e devolve o objeto "stats". */
    public static JsonObject lerStats(String json) {
        try {
            JsonObject raiz = JsonParser.parseString(json).getAsJsonObject();
            JsonElement s = raiz.get("stats");
            return s != null && s.isJsonObject() ? s.getAsJsonObject() : raiz;
        } catch (RuntimeException e) {
            return new JsonObject();
        }
    }
}
