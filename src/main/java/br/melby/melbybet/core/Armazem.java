package br.melby.melbybet.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Salva e lê a Casa em JSON. Escreve num arquivo temporário e troca, para não corromper se o servidor cair. */
public final class Armazem {
    private Armazem() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Casa carregar(Path arquivo) throws IOException {
        if (!Files.exists(arquivo)) return new Casa();
        String txt = Files.readString(arquivo, StandardCharsets.UTF_8);
        Casa c = GSON.fromJson(txt, Casa.class);
        return c == null ? new Casa() : c;
    }

    public static void salvar(Path arquivo, Casa casa) throws IOException {
        Files.createDirectories(arquivo.toAbsolutePath().getParent());
        Path tmp = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
        Files.writeString(tmp, GSON.toJson(casa), StandardCharsets.UTF_8);
        Files.move(tmp, arquivo, StandardCopyOption.REPLACE_EXISTING);
    }
}
