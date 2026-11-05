package br.melby.melbybet.core;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Log permanente dos duelos (config/melbybet/duelos-log.jsonl, uma linha por duelo): quem duelou, quando, o que foi
 * apostado, como terminou, se a staff anulou e, depois do fim, o rastro do prêmio do vencedor. Lógica pura.
 */
public class DueloLog {
    public static class Evento {
        public long ts;
        public String texto = "";

        public Evento() {}

        public Evento(long ts, String texto) {
            this.ts = ts;
            this.texto = texto;
        }
    }

    public static class Reg {
        public int id;
        public String uuidA = "", nomeA = "", uuidB = "", nomeB = "";
        public boolean itens;
        public int valor;
        /** O que cada um apostou, em texto (e em JSON dos itens, para poder recriar numa anulação). */
        public String apostaA = "", apostaB = "";
        public List<String> itensA = new ArrayList<>(), itensB = new ArrayList<>();
        public long criadoEm, inicio, fim;
        /** EM ANDAMENTO, RESOLVIDO, CANCELADO ou ANULADO. */
        public String estado = "EM ANDAMENTO";
        public String vencedorUuid = "", vencedorNome = "", motivo = "";
        public int premio, taxa;
        public String resultado = "";
        /** JSON dos itens que passaram do perdedor ao vencedor (duelo de itens). */
        public List<String> transferidos = new ArrayList<>();
        public String arena = "";
        public ArenaRegistro.Origem origemA, origemB;
        public String anuladoPor = "", anuladoMotivo = "", anuladoResumo = "";
        public long anuladoEm;
        /** Até quando o rastro do prêmio do vencedor é registrado (0 = não rastreia). */
        public long rastroAte;
        public List<Evento> eventos = new ArrayList<>();
        public List<Evento> rastro = new ArrayList<>();

        public void evento(long ts, String texto) {
            eventos.add(new Evento(ts, texto));
            while (eventos.size() > 60) eventos.remove(0);
        }

        public void rastro(long ts, String texto) {
            rastro.add(new Evento(ts, texto));
            while (rastro.size() > 300) rastro.remove(0);
        }
    }

    public List<Reg> regs = new ArrayList<>();

    private static final Gson GSON = new Gson();

    public Reg achar(int id) {
        for (Reg r : regs) if (r.id == id) return r;
        return null;
    }

    /** O registro do duelo, criado (vazio) se ainda não existir. */
    public Reg obter(int id) {
        Reg r = achar(id);
        if (r == null) {
            r = new Reg();
            r.id = id;
            regs.add(r);
        }
        return r;
    }

    /** Do mais novo para o mais antigo. */
    public List<Reg> recentes(int max) {
        List<Reg> r = new ArrayList<>();
        for (int i = regs.size() - 1; i >= 0 && r.size() < max; i--) r.add(regs.get(i));
        return r;
    }

    public static String data(long ms, String fuso) {
        if (ms <= 0) return "-";
        ZoneId z;
        try {
            z = ZoneId.of(fuso == null || fuso.isBlank() ? "America/Sao_Paulo" : fuso);
        } catch (Exception e) {
            z = ZoneId.of("America/Sao_Paulo");
        }
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(z).format(Instant.ofEpochMilli(ms));
    }

    public static DueloLog ler(Path arquivo) {
        DueloLog l = new DueloLog();
        try {
            if (!Files.isRegularFile(arquivo)) return l;
            for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                if (linha.isBlank()) continue;
                try {
                    Reg r = GSON.fromJson(linha, Reg.class);
                    if (r != null) {
                        if (r.itensA == null) r.itensA = new ArrayList<>();
                        if (r.itensB == null) r.itensB = new ArrayList<>();
                        if (r.transferidos == null) r.transferidos = new ArrayList<>();
                        if (r.eventos == null) r.eventos = new ArrayList<>();
                        if (r.rastro == null) r.rastro = new ArrayList<>();
                        l.regs.add(r);
                    }
                } catch (Exception ignorada) {
                    // linha estragada: pula só ela
                }
            }
        } catch (IOException e) {
            // sem arquivo legível: começa vazio
        }
        return l;
    }

    /** Grava tudo de forma atômica, uma linha por duelo. */
    public void gravar(Path arquivo) throws IOException {
        Files.createDirectories(arquivo.getParent());
        StringBuilder sb = new StringBuilder();
        for (Reg r : regs) sb.append(GSON.toJson(r)).append('\n');
        Path tmp = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
        Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8);
        Files.move(tmp, arquivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
