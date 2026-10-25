package br.melby.melbybet.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/** Acha os arquivos de stats e advancements do mundo. O lugar mudou entre versões, então tenta os dois. */
public final class Arquivos {
    private Arquivos() {}

    /** Nome da pasta do mundo: config, senão level-name do server.properties, senão "world". */
    public static Path pastaMundo(Path servidor, String configurada) {
        if (configurada != null && !configurada.isBlank()) return servidor.resolve(configurada);
        Path props = servidor.resolve("server.properties");
        if (Files.exists(props)) {
            try {
                Properties p = new Properties();
                p.load(Files.newBufferedReader(props, StandardCharsets.UTF_8));
                String n = p.getProperty("level-name");
                if (n != null && !n.isBlank()) return servidor.resolve(n.trim());
            } catch (IOException ignorado) {
                // cai no padrão
            }
        }
        return servidor.resolve("world");
    }

    /** @param tipo "stats" ou "advancements" */
    public static String ler(Path mundo, String tipo, String uuid) {
        for (Path dir : List.of(mundo.resolve("players").resolve(tipo), mundo.resolve(tipo))) {
            Path f = dir.resolve(uuid + ".json");
            if (Files.isRegularFile(f)) {
                try {
                    return Files.readString(f, StandardCharsets.UTF_8);
                } catch (IOException ignorado) {
                    return null;
                }
            }
        }
        return null;
    }
}
