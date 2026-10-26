package br.melby.melbybet.core;

import com.google.gson.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Testes da lógica pura. Rode: java -cp out:gson.jar br.melby.melbybet.core.Testes */
public class Testes {
    static int falhas, total;

    static void igual(Object esperado, Object real, String nome) {
        total++;
        if (!java.util.Objects.equals(esperado, real)) {
            falhas++;
            System.out.println("FALHOU " + nome + ": esperado " + esperado + " mas veio " + real);
        }
    }

    static Casa casa() {
        Casa c = new Casa();
        c.config.rakePct = 5;
        return c;
    }

    static Bolao aberto(Casa c, String... ops) {
        Bolao b = c.criar("Quem morre primeiro?", Bolao.Gatilho.VOID, List.of(ops), 0);
        igual(null, c.abrir(b), "abrir");
        return b;
    }

    public static void main(String[] args) throws Exception {
        formato();
        degrade();
        tells();
        apostas();
        pagamento();
        semGanhador();
        cashoutECancelar();
        stats();
        conquistas();
        combinadas();
        lembretes();
        persistencia();
        System.out.println(falhas == 0 ? "tudo certo (" + total + " checagens)" : falhas + " falhas de " + total);
        if (falhas > 0) System.exit(1);
    }

    static void formato() {
        igual("2.440", Fmt.inteiro(2440), "milhar");
        igual("1.234.567", Fmt.inteiro(1234567), "milhoes");
        igual("41,42", Fmt.decimal(41.42, 2), "decimal");
        igual("8,90", Fmt.decimal(8.9, 2), "decimal zero");
        igual("0,05", Fmt.decimal(0.05, 2), "decimal pequeno");
        igual("1.000,00", Fmt.decimal(1000, 2), "decimal milhar");
        igual("41,42 km", StatCat.DISTANCIA.formatar(41.42), "km");
        igual("30x", StatCat.MORREU.formatar(30), "mortes");
        igual("1.738 itens", StatCat.CRAFTOU.formatar(1738), "itens");
        igual("161 mobs", StatCat.MATOU.formatar(161), "mobs");
        igual("8,90h", StatCat.TEMPO.formatar(8.9), "horas");
    }

    static void degrade() {
        int[] c = Gradiente.cores(3, 0xFF0000, 0x0000FF);
        igual(3, c.length, "3 cores");
        igual(0xFF0000, c[0], "começa na cor 1");
        igual(0x800080, c[1], "meio");
        igual(0x0000FF, c[2], "termina na cor 2");
        igual(0xFFAA00, Gradiente.cores(1, 0xFFAA00, 0x000000)[0], "uma letra só");
        igual(0, Gradiente.cores(0, 1, 2).length, "vazio");
        igual(0xFF8A24, Gradiente.hex("#ff8a24"), "hex válido");
        igual(-1, Gradiente.hex("ff8a24"), "hex sem #");
        igual(-1, Gradiente.hex("#ff8a2"), "hex curto");
        igual("menos de 1 min", Fmt.duracao(30_000), "duração curta");
        igual("5 min", Fmt.duracao(5 * 60_000), "minutos");
        igual("3h 12min", Fmt.duracao((3 * 60 + 12) * 60_000L), "horas");
        igual("2d 4h", Fmt.duracao((2 * 24 + 4) * 3_600_000L), "dias");
    }

    static void tells() {
        Casa c = casa();
        for (int i = 0; i < 305; i++) c.registrarTell(i % 2 == 0 ? "Davi" : "Kaue", i % 2 == 0 ? "Kaue" : "Edu", "m" + i, i);
        igual(300, c.logTells.size(), "guarda só 300");
        igual("m5", c.logTells.get(0).texto, "descarta as mais antigas");
        var l = c.tellsDe("edu", 3);
        igual(3, l.size(), "limite");
        igual("m299", l.get(0).texto, "mais antiga das 3");
        igual("m303", l.get(2).texto, "mais nova por último");
        igual(0, c.tellsDe("ninguem", 5).size(), "sem mensagens");
        igual(true, Gradiente.preset("FOGO") != null, "preset sem diferenciar maiúscula");
        igual(null, Gradiente.preset("xyz"), "preset inexistente");
    }

    static void apostas() {
        Casa c = casa();
        Bolao b = aberto(c, "Davi", "Kaue", "Edu");
        igual("Não pode apostar em você mesmo.", c.validarAposta(b, "u1", "Davi", "davi", 5), "em si mesmo");
        igual(null, c.validarAposta(b, "u1", "Samuel", "davi", 5), "aposta ok");
        igual("Opção inválida.", c.validarAposta(b, "u1", "Samuel", "Zé", 5), "opcao invalida");
        igual("Aposta mínima: 1 diamante(s).", c.validarAposta(b, "u1", "Samuel", "Davi", 0), "minimo");
        c.apostar(b, "u1", "Samuel", "Davi", 60);
        igual("Limite por bolão: 64 diamantes (você já pôs 60).", c.validarAposta(b, "u1", "Samuel", "Kaue", 5), "limite");
        igual(null, c.validarAposta(b, "u1", "Samuel", "Kaue", 4), "limite exato");
        c.fechar(b);
        igual("Esse bolão não está aberto.", c.validarAposta(b, "u2", "Edu", "Davi", 1), "fechado");
        Bolao so1 = c.criar("x", Bolao.Gatilho.MANUAL, List.of("a"), 0);
        igual("O bolão precisa de pelo menos 2 opções.", c.abrir(so1), "2 opcoes");
    }

    static void pagamento() {
        Casa c = casa();
        Bolao b = aberto(c, "Davi", "Kaue");
        c.apostar(b, "u1", "Samuel", "Davi", 10);
        c.apostar(b, "u2", "Edu", "Davi", 30);
        c.apostar(b, "u3", "Gota", "Kaue", 60);
        // pote 100, rake 5 -> 95 para dividir entre 40 apostados em Davi
        igual(2.375, c.odd(b, "Davi"), "odd");
        List<Casa.Pagamento> p = c.resolver(b, "davi", 1);
        igual(2, p.size(), "dois vencedores");
        igual(23, p.get(0).valor, "pagamento 1");
        igual(71, p.get(1).valor, "pagamento 2");
        igual(5L + 1, c.cofre, "cofre = rake + sobra");
        igual(23, c.creditoDe("u1"), "credito u1");
        igual(0, c.creditoDe("u3"), "perdedor");
        igual("Davi", b.vencedor, "vencedor com grafia certa");
        igual(Bolao.Status.RESOLVIDO, b.status, "status");
        igual(1, c.jogadores.get("u1").acertos, "acerto contado");
        c.baixarCredito("u1", 20);
        igual(3, c.creditoDe("u1"), "baixa parcial");
        c.baixarCredito("u1", 3);
        igual(false, c.creditos.containsKey("u1"), "credito zerado some");
    }

    static void semGanhador() {
        Casa c = casa();
        Bolao b = aberto(c, "Davi", "Kaue");
        c.apostar(b, "u1", "Samuel", "Kaue", 20);
        List<Casa.Pagamento> p = c.resolver(b, "Davi", 1);
        igual(0, p.size(), "ninguém acertou");
        igual(1L, c.cofre, "rake de 20 = 1");
        igual(19L, c.jackpot, "jackpot guarda o resto");
        Bolao b2 = aberto(c, "Davi", "Kaue");
        c.apostar(b2, "u2", "Edu", "Davi", 10);
        c.apostar(b2, "u1", "Samuel", "Kaue", 10);
        List<Casa.Pagamento> p2 = c.resolver(b2, "Davi", 2);
        // pote 20, rake 1, +19 de jackpot = 38 para u2
        igual(38, p2.get(0).valor, "jackpot vai para o próximo");
        igual(0L, c.jackpot, "jackpot zerado");
    }

    static void cashoutECancelar() {
        Casa c = casa();
        Bolao b = aberto(c, "Davi", "Kaue");
        c.apostar(b, "u1", "Samuel", "Davi", 50);
        igual(45, c.cashout(b, "u1"), "cashout com 10% de taxa");
        igual(5L, c.cofre, "taxa vai para a casa");
        igual(0, b.pote(), "apostas removidas");
        igual(-1, c.cashout(b, "u1"), "sem aposta");
        c.apostar(b, "u1", "Samuel", "Davi", 10);
        c.apostar(b, "u2", "Edu", "Kaue", 7);
        c.cancelar(b, 3);
        igual(Bolao.Status.CANCELADO, b.status, "cancelado");
        igual(55, c.creditoDe("u1"), "devolve 45 + 10");
        igual(7, c.creditoDe("u2"), "devolve inteiro");
    }

    static JsonObject stat(String j) {
        return StatCat.lerStats(j);
    }

    static void stats() {
        JsonObject s = stat("{\"stats\":{\"minecraft:custom\":{\"minecraft:walk_one_cm\":4142000,"
            + "\"minecraft:sprint_one_cm\":1000000,\"minecraft:aviate_one_cm\":500000,\"minecraft:swim_one_cm\":200000,"
            + "\"minecraft:horse_one_cm\":300000,\"minecraft:deaths\":30,\"minecraft:mob_kills\":161,"
            + "\"minecraft:play_time\":641520},\"minecraft:mined\":{\"minecraft:stone\":2000,\"minecraft:dirt\":440},"
            + "\"minecraft:crafted\":{\"minecraft:stick\":1700,\"minecraft:torch\":38},"
            + "\"minecraft:used\":{\"minecraft:stone\":100,\"minecraft:diamond_pickaxe\":900,\"minecraft:bow\":10,\"minecraft:dirt\":5}},"
            + "\"DataVersion\":4500}");
        igual("41,42 km", StatCat.ANDOU.formatar(StatCat.ANDOU.valor(s)), "andou");
        igual("10,00 km", StatCat.CORREU.formatar(StatCat.CORREU.valor(s)), "correu");
        igual("5,00 km", StatCat.VOOU.formatar(StatCat.VOOU.valor(s)), "voou");
        igual("2,00 km", StatCat.NADOU.formatar(StatCat.NADOU.valor(s)), "nadou");
        igual("3,00 km", StatCat.CAVALGOU.formatar(StatCat.CAVALGOU.valor(s)), "cavalgou");
        igual("61,42 km", StatCat.DISTANCIA.formatar(StatCat.DISTANCIA.valor(s)), "distância soma tudo");
        igual("2.440 blocos", StatCat.QUEBROU.formatar(StatCat.QUEBROU.valor(s)), "quebrou");
        igual("1.738 itens", StatCat.CRAFTOU.formatar(StatCat.CRAFTOU.valor(s)), "craftou");
        igual("161 mobs", StatCat.MATOU.formatar(StatCat.MATOU.valor(s)), "mobs");
        igual("30x", StatCat.MORREU.formatar(StatCat.MORREU.valor(s)), "mortes");
        igual("8,91h", StatCat.TEMPO.formatar(StatCat.TEMPO.valor(s)), "tempo");
        igual("105 blocos", StatCat.COLOCOU.formatar(StatCat.COLOCOU.valor(s)), "colocou ignora ferramenta e arco");
        igual(0.0, StatCat.MORREU.valor(stat("lixo{")), "json quebrado não explode");
        igual(0.0, StatCat.ANDOU.valor(stat("{}")), "vazio");

        Map<String, JsonObject> todos = new LinkedHashMap<>();
        todos.put("Davi", stat("{\"stats\":{\"minecraft:custom\":{\"minecraft:deaths\":5}}}"));
        todos.put("Kaue", stat("{\"stats\":{\"minecraft:custom\":{\"minecraft:deaths\":9}}}"));
        todos.put("Edu", stat("{\"stats\":{\"minecraft:custom\":{\"minecraft:deaths\":9}}}"));
        todos.put("Gota", stat("{\"stats\":{}}"));
        var r = Ranking.montar(StatCat.MORREU, todos);
        igual(3, r.size(), "zerado fica fora");
        igual("Edu", r.get(0).nome, "empate desempata por nome");
        igual("Kaue", r.get(1).nome, "segundo");
        igual("Davi", r.get(2).nome, "terceiro");
        igual(12, StatCat.values().length, "12 categorias");
    }

    static String adv(String id, String when, boolean done) {
        return "{\"" + id + "\":{\"criteria\":{\"x\":\"" + when + "\"},\"done\":" + done + "},\"DataVersion\":1}";
    }

    static void conquistas() {
        String id = "minecraft:story/mine_diamond";
        long t0 = Resultados.quando(adv(id, "2026-10-24 12:00:00 -0300", true), id);
        igual(java.time.OffsetDateTime.parse("2026-10-24T12:00:00-03:00").toInstant().toEpochMilli(), t0, "timestamp");
        igual(-1L, Resultados.quando(adv(id, "2026-10-24 12:00:00 -0300", false), id), "não concluída");
        igual(-1L, Resultados.quando("{}", id), "sem a conquista");
        igual(-1L, Resultados.quando("lixo", id), "json quebrado");

        Map<String, String> nomes = new LinkedHashMap<>();
        nomes.put("u1", "Davi");
        nomes.put("u2", "Kaue");
        nomes.put("u3", "Edu");
        Map<String, String> arq = new LinkedHashMap<>();
        arq.put("u1", adv(id, "2026-10-24 12:05:00 -0300", true));
        arq.put("u2", adv(id, "2026-10-24 12:01:00 -0300", true));
        var d = Resultados.porConquista(id, nomes, arq::get, 0);
        igual("Kaue", d.vencedores.get(0), "o mais cedo ganha");
        igual(true, d.claro(), "claro");

        long depoisDeKaue = t0 + 2 * 60_000;
        var d2 = Resultados.porConquista(id, nomes, arq::get, depoisDeKaue);
        igual("Davi", d2.vencedores.get(0), "ignora conquista anterior ao bolão");
        igual(null, Resultados.porConquista(id, nomes, arq::get, t0 + 3_600_000), "ninguém ainda");

        arq.put("u3", adv(id, "2026-10-24 12:01:00 -0300", true));
        var d3 = Resultados.porConquista(id, nomes, arq::get, 0);
        igual(2, d3.vencedores.size(), "empate no mesmo segundo");
        igual(false, d3.claro(), "ambíguo");
    }

    static void combinadas() {
        Casa c = casa();
        c.cofre = 100;
        Bolao b1 = aberto(c, "Davi", "Kaue");
        Bolao b2 = aberto(c, "Edu", "Gota");
        List<Casa.Perna> ps = List.of(new Casa.Perna(b1.id, "Davi"), new Casa.Perna(b2.id, "Edu"));
        igual(null, c.validarCombinada("u1", "Samuel", ps, 10), "combinada ok");
        igual("Escolha pelo menos 2 bolões.", c.validarCombinada("u1", "Samuel", List.of(ps.get(0)), 10), "uma perna");
        igual("Não repita o mesmo bolão.", c.validarCombinada("u1", "Samuel", List.of(ps.get(0), ps.get(0)), 10), "repetida");
        igual(true, c.validarCombinada("u1", "Davi", ps, 10).contains("você mesmo"), "em si mesmo");
        igual(5, c.bonusDe(2, 20), "bonus 2 pernas");
        igual(15, c.bonusDe(4, 20), "bonus 4 pernas");
        igual(64, c.bonusDe(4, 1000), "bonus com teto");
        c.criarCombinada("u1", "Samuel", ps, 10);
        igual(10, b1.pote(), "perna 1 apostada");
        c.apostar(b1, "u2", "Edu", "Kaue", 10);
        c.apostar(b2, "u2", "Edu", "Gota", 10);
        c.resolver(b1, "Davi", 1);
        igual("PENDENTE", c.combinadas.get(0).estado, "ainda pendente");
        c.resolver(b2, "Edu", 2);
        igual("GANHA", c.combinadas.get(0).estado, "ganhou tudo");
        igual(5, c.combinadas.get(0).bonus, "bonus pago");
        igual(1, c.bonusRecentes.size(), "bonus avisado");
        igual(95L, c.cofre >= 95 ? 95L : c.cofre, "cofre pagou o bonus");

        Casa d = casa();
        d.cofre = 2;
        Bolao x1 = aberto(d, "Davi", "Kaue");
        Bolao x2 = aberto(d, "Edu", "Gota");
        d.criarCombinada("u1", "Samuel", List.of(new Casa.Perna(x1.id, "Davi"), new Casa.Perna(x2.id, "Edu")), 10);
        d.resolver(x1, "Kaue", 1);
        igual("PERDIDA", d.combinadas.get(0).estado, "perdeu uma perna");
        d.resolver(x2, "Edu", 2);
        igual(0, d.combinadas.get(0).bonus, "perdida sem bonus");

        Casa e = casa();
        e.cofre = 2;
        Bolao y1 = aberto(e, "Davi", "Kaue");
        Bolao y2 = aberto(e, "Edu", "Gota");
        e.criarCombinada("u1", "Samuel", List.of(new Casa.Perna(y1.id, "Davi"), new Casa.Perna(y2.id, "Edu")), 10);
        e.resolver(y1, "Davi", 1);
        e.resolver(y2, "Edu", 2);
        igual(2, e.combinadas.get(0).bonus, "bonus limitado pelo cofre");
        igual(0L, e.cofre, "cofre zerou");

        Casa f = casa();
        Bolao z1 = aberto(f, "Davi", "Kaue");
        Bolao z2 = aberto(f, "Edu", "Gota");
        f.criarCombinada("u1", "Samuel", List.of(new Casa.Perna(z1.id, "Davi"), new Casa.Perna(z2.id, "Edu")), 10);
        f.cashout(z1, "u1");
        f.resolver(z2, "Edu", 2);
        igual("CANCELADA", f.combinadas.get(0).estado, "desistiu de uma perna");
        igual(0, f.combinadas.get(0).bonus, "cancelada sem bonus");
    }

    static void lembretes() {
        Lembrete.Resultado r = Lembrete.ler("\"Arrumar baús\" 2 horas");
        igual("Arrumar baús", r.texto, "texto entre aspas");
        igual(7_200_000L, r.ms, "2 horas");
        r = Lembrete.ler("Arrumar baús 30min");
        igual("Arrumar baús", r.texto, "texto sem aspas");
        igual(1_800_000L, r.ms, "30min junto");
        igual(3_600_000L, Lembrete.ler("tomar agua 1 hora").ms, "1 hora");
        igual(172_800_000L, Lembrete.ler("buscar 2d").ms, "2d");
        igual(90_000L, Lembrete.ler("x 90 segundos").ms, "90 segundos");
        igual(null, Lembrete.ler("Arrumar baús"), "sem tempo");
        igual(null, Lembrete.ler("2 horas"), "sem texto");
        igual(null, Lembrete.ler("oi 0 horas"), "zero");
        igual(null, Lembrete.ler("oi 5 bananas"), "unidade invalida");
        Casa c = casa();
        Casa.Capsula x = c.novaCapsula("u1", "Samuel", "Arrumar baús", 1000, 0);
        igual(1, c.capsulasPendentesDe("u1"), "pendentes");
        igual(true, c.removerCapsula(x.id), "remover");
        igual(0, c.capsulasPendentesDe("u1"), "removida");
    }

    static void persistencia() throws Exception {
        Path tmp = Files.createTempDirectory("melby");
        Path f = tmp.resolve("sub").resolve("casa.json");
        Casa c = casa();
        Bolao b = aberto(c, "Davi", "Kaue");
        c.apostar(b, "u1", "Samuel", "Davi", 10);
        c.cofre = 7;
        Armazem.salvar(f, c);
        Casa l = Armazem.carregar(f);
        igual(1, l.bolaos.size(), "bolão salvo");
        igual(Bolao.Status.ABERTO, l.bolaos.get(0).status, "status salvo");
        igual(10, l.bolaos.get(0).pote(), "aposta salva");
        igual(7L, l.cofre, "cofre salvo");
        igual(Bolao.Gatilho.VOID, l.bolaos.get(0).gatilho, "gatilho salvo");
        igual(0, Armazem.carregar(tmp.resolve("nao-existe.json")).bolaos.size(), "arquivo ausente = casa nova");

        Path mundo = tmp.resolve("world");
        Files.createDirectories(mundo.resolve("players").resolve("stats"));
        Files.writeString(mundo.resolve("players").resolve("stats").resolve("u1.json"), "{}");
        Files.createDirectories(mundo.resolve("advancements"));
        Files.writeString(mundo.resolve("advancements").resolve("u2.json"), "{\"a\":1}");
        igual("{}", Arquivos.ler(mundo, "stats", "u1"), "stats na pasta players");
        igual("{\"a\":1}", Arquivos.ler(mundo, "advancements", "u2"), "advancements na pasta antiga");
        igual(null, Arquivos.ler(mundo, "stats", "u9"), "inexistente");
        Files.writeString(tmp.resolve("server.properties"), "level-name=Melby\n");
        igual(tmp.resolve("Melby"), Arquivos.pastaMundo(tmp, ""), "level-name");
        igual(tmp.resolve("outra"), Arquivos.pastaMundo(tmp, "outra"), "pasta configurada");
    }
}
