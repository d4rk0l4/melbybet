package br.melby.melbybet.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Leitura e gravação atômica de JSON em disco (arquivo temporário e troca), usada pelos arquivos próprios em config/melbybet. */
public final class Json {
    private Json() {}

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Lê o arquivo; devolve null se não existir, estiver vazio ou ilegível. */
    public static <T> T ler(Path arquivo, Class<T> tipo) {
        try {
            if (Files.isRegularFile(arquivo)) return GSON.fromJson(Files.readString(arquivo, StandardCharsets.UTF_8), tipo);
        } catch (Exception e) {
            // arquivo ilegível: quem chamou decide o que fazer
        }
        return null;
    }

    public static void gravar(Path arquivo, Object obj) throws IOException {
        Files.createDirectories(arquivo.getParent());
        Path tmp = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
        Files.writeString(tmp, GSON.toJson(obj), StandardCharsets.UTF_8);
        Files.move(tmp, arquivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
