package br.melby.melbybet.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Quem é o dono de cada coisa colocada no mundo (armor stands, molduras de foto, ...). Lógica pura, em config/melbybet/donos.json. */
public class Donos {
    public static final int MAX = 20000;

    public static class Dono {
        public String uuid = "", nome = "";
        public long desde;
    }

    /** chave (por exemplo "stand:<uuid da entidade>") -> dono. */
    public Map<String, Dono> donos = new LinkedHashMap<>();

    public Dono de(String chave) {
        return donos.get(chave);
    }

    /** Define o dono se ainda não tinha. Devolve true se passou a ser dele. */
    public boolean reivindicar(String chave, String uuid, String nome, long agora) {
        if (donos.containsKey(chave)) return false;
        Dono d = new Dono();
        d.uuid = uuid;
        d.nome = nome;
        d.desde = agora;
        donos.put(chave, d);
        while (donos.size() > MAX) donos.remove(donos.keySet().iterator().next());
        return true;
    }

    /** Devolve o motivo se o jogador não pode mexer, ou null. Sem dono: só OP. */
    public String pode(String chave, String uuid, boolean op) {
        if (op) return null;
        Dono d = donos.get(chave);
        if (d == null) return "Isso não tem dono registrado (foi colocado antes do mod), então só um OP mexe. Se estiver vazio, use /armorstand reivindicar.";
        if (!d.uuid.equals(uuid)) return "Isso pertence a " + d.nome + ".";
        return null;
    }

    /** Passa a posse. Só o dono atual ou um OP. Devolve o erro, ou null. */
    public String transferir(String chave, String porUuid, boolean op, String novoUuid, String novoNome, long agora) {
        Dono d = donos.get(chave);
        if (d == null && !op) return "Isso não tem dono registrado; só um OP passa a posse.";
        if (d != null && !d.uuid.equals(porUuid) && !op) return "Isso pertence a " + d.nome + ".";
        Dono n = new Dono();
        n.uuid = novoUuid;
        n.nome = novoNome;
        n.desde = agora;
        donos.put(chave, n);
        return null;
    }

    public void remover(String chave) {
        donos.remove(chave);
    }

    public int contar(String uuid) {
        int n = 0;
        for (Dono d : donos.values()) if (d.uuid.equals(uuid)) n++;
        return n;
    }

    public static Donos ler(Path arquivo) {
        Donos d = Json.ler(arquivo, Donos.class);
        if (d == null) return new Donos();
        if (d.donos == null) d.donos = new LinkedHashMap<>();
        d.donos.values().removeIf(x -> x == null || x.uuid == null);
        return d;
    }

    public void gravar(Path arquivo) throws IOException {
        Json.gravar(arquivo, this);
    }
}
