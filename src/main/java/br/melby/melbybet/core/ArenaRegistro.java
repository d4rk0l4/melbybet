package br.melby.melbybet.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Arenas de duelo reservadas ou ativas, guardadas em disco (config/melbybet/arenas.json) para que o servidor consiga
 * desfazer tudo se reiniciar no meio. Lógica pura.
 */
public class ArenaRegistro {
    /** De onde o jogador veio (e para onde volta). */
    public static class Origem {
        public String dim = "minecraft:overworld";
        public double x, y, z;
        public float yaw, pitch;

        public Origem() {}

        public Origem(String dim, double x, double y, double z, float yaw, float pitch) {
            this.dim = dim;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    public static class Arena {
        public int duelo;
        public String dim = "minecraft:overworld";
        public int x0, y0, z0, larg, comp, alt;
        /** RESERVADA, CONSTRUINDO, CONTAGEM, LUTA ou LIMPANDO. */
        public String estado = "RESERVADA";
        public String material = "minecraft:glass";
        public String uuidA = "", uuidB = "";
        public Origem origemA = new Origem(), origemB = new Origem();
        public long criadaEm;

        public ArenaGeo.Caixa caixa() {
            return new ArenaGeo.Caixa(x0, y0, z0, larg, comp, alt);
        }
    }

    public List<Arena> arenas = new ArrayList<>();
    /** Jogadores que ficaram na arena quando ela foi desfeita sem eles (offline): uuid -> para onde levar quando entrarem. */
    public Map<String, Origem> devolver = new LinkedHashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Arena achar(int duelo) {
        for (Arena a : arenas) if (a.duelo == duelo) return a;
        return null;
    }

    public Arena daDimensao(String dim, int duelo) {
        for (Arena a : arenas) if (a.duelo == duelo && a.dim.equals(dim)) return a;
        return null;
    }

    /** A caixa (já com margem) pode ser reservada sem encostar em outra arena, respeitando a distância mínima? */
    public boolean livre(String dim, ArenaGeo.Caixa c, int distanciaMinima) {
        for (Arena a : arenas) {
            if (!a.dim.equals(dim)) continue;
            if (a.caixa().expandida(1).cruza(c.expandida(1), distanciaMinima)) return false;
        }
        return true;
    }

    public Arena reservar(int duelo, String dim, ArenaGeo.Caixa c, String material, long agora) {
        Arena a = new Arena();
        a.duelo = duelo;
        a.dim = dim;
        a.x0 = c.x0();
        a.y0 = c.y0();
        a.z0 = c.z0();
        a.larg = c.larg();
        a.comp = c.comp();
        a.alt = c.alt();
        a.material = material;
        a.criadaEm = agora;
        arenas.add(a);
        return a;
    }

    public void liberar(int duelo) {
        arenas.removeIf(a -> a.duelo == duelo);
    }

    public static ArenaRegistro ler(Path arquivo) {
        try {
            if (Files.isRegularFile(arquivo)) {
                ArenaRegistro r = GSON.fromJson(Files.readString(arquivo, StandardCharsets.UTF_8), ArenaRegistro.class);
                if (r != null) {
                    if (r.arenas == null) r.arenas = new ArrayList<>();
                    if (r.devolver == null) r.devolver = new LinkedHashMap<>();
                    return r;
                }
            }
        } catch (Exception e) {
            // arquivo ilegível: começa vazio (as arenas desse arquivo ficam no céu; ver Arena.aoLigar)
        }
        return new ArenaRegistro();
    }

    /** Grava de forma atômica (arquivo temporário e troca), para não corromper se o servidor cair no meio. */
    public void gravar(Path arquivo) throws IOException {
        Files.createDirectories(arquivo.getParent());
        Path tmp = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
        Files.writeString(tmp, GSON.toJson(this), StandardCharsets.UTF_8);
        Files.move(tmp, arquivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
