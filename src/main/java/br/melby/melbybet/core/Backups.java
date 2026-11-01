package br.melby.melbybet.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/** Cópias de segurança do casa.json. Sem nada do Minecraft aqui. */
public final class Backups {
    private Backups() {}

    /** Copia o arquivo para pasta/nome. Devolve o destino, ou null se o arquivo de origem não existe. */
    public static Path copiar(Path origem, Path pasta, String nome) throws IOException {
        if (!Files.exists(origem)) return null;
        Files.createDirectories(pasta);
        Path destino = pasta.resolve(nome);
        Files.copy(origem, destino, StandardCopyOption.REPLACE_EXISTING);
        return destino;
    }

    public static boolean existe(Path pasta, String nome) {
        return Files.exists(pasta.resolve(nome));
    }

    /** Mantém só os "manter" arquivos mais recentes que começam com o prefixo (o nome tem a data, então ordenar por nome basta). */
    public static int limpar(Path pasta, String prefixo, int manter) throws IOException {
        if (!Files.isDirectory(pasta)) return 0;
        List<Path> arquivos = new ArrayList<>();
        try (Stream<Path> s = Files.list(pasta)) {
            s.filter(p -> p.getFileName().toString().startsWith(prefixo)).forEach(arquivos::add);
        }
        Collections.sort(arquivos);
        int apagados = 0;
        for (int i = 0; i < arquivos.size() - manter; i++) {
            Files.delete(arquivos.get(i));
            apagados++;
        }
        return apagados;
    }
}
