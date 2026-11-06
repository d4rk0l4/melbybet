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
        duelos();
        reinicio();
        morteEPlacar();
        apostaNoDuelo();
        limpeza();
        apelidos();
        placas();
        loteria();
        corpos();
        msgMatar();
        backups();
        busca();
        igual("#3c44aa", Gradiente.paraHex(0x3C44AA), "hex: formata");
        igual("#00000f", Gradiente.paraHex(0xF), "hex: zeros");
        persistencia();
        arena();
        dueloLog();
        marcador();
        catalogo();
        placaCopiar();
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

    static void arena() throws Exception {
        var c = new ArenaGeo.Caixa(100, 300, -50, 15, 15, 10);
        igual(114, c.x1(), "arena: x1");
        igual(309, c.y1(), "arena: y1");
        igual(2250, c.total(), "arena: total de blocos");
        igual(true, c.contem(100, 300, -50), "arena: canto");
        igual(false, c.contem(115, 300, -50), "arena: fora em x");
        igual(true, c.casca(100, 305, -40), "arena: parede");
        igual(true, c.casca(105, 300, -45), "arena: chao");
        igual(true, c.casca(105, 309, -45), "arena: teto");
        igual(true, c.interior(105, 305, -45), "arena: interior");
        igual(false, c.casca(105, 305, -45), "arena: interior nao e casca");
        int interior = 0, casca = 0;
        for (int x = c.x0(); x <= c.x1(); x++) for (int y = c.y0(); y <= c.y1(); y++) for (int z = c.z0(); z <= c.z1(); z++) {
            if (c.interior(x, y, z)) interior++;
            if (c.casca(x, y, z)) casca++;
        }
        igual(13 * 13 * 8, interior, "arena: interior 13x13x8");
        igual(2250 - interior, casca, "arena: casca fecha tudo");
        var m = c.expandida(1);
        igual(99, m.x0(), "arena: margem x0");
        igual(17, m.larg(), "arena: margem larg");
        double[] a = c.posA(), b = c.posB();
        igual(true, c.dentroLivre(a[0], a[1], a[2]), "arena: A dentro");
        igual(true, c.dentroLivre(b[0], b[1], b[2]), "arena: B dentro");
        igual(true, a[2] < b[2] && b[2] - a[2] > 8, "arena: lados opostos");
        igual(false, c.dentroLivre(a[0], a[1], c.z0() + 0.5), "arena: dentro da parede nao vale");
        igual(false, c.dentroLivre(a[0], c.y1() + 0.5, a[2]), "arena: acima do teto nao vale");
        igual(false, c.dentroLivre(c.x1() + 3, a[1], a[2]), "arena: fora");
        // cruzamento e distancia minima
        var longe = new ArenaGeo.Caixa(100 + 15 + 40, 300, -50, 15, 15, 10);
        var colada = new ArenaGeo.Caixa(115, 300, -50, 15, 15, 10);
        var perto = new ArenaGeo.Caixa(100 + 15 + 20, 300, -50, 15, 15, 10);
        igual(false, c.cruza(longe, 32), "arena: 40 blocos de vao com folga 32 nao cruza");
        igual(true, c.cruza(perto, 32), "arena: 20 blocos de vao com folga 32 cruza");
        igual(false, c.cruza(colada, 0), "arena: encostada sem folga nao se sobrepoe");
        igual(true, c.cruza(colada, 1), "arena: encostada com folga 1 cruza");
        var outraAltura = new ArenaGeo.Caixa(100, 100, -50, 15, 15, 10);
        igual(false, c.cruza(outraAltura, 5), "arena: muito abaixo nao cruza");
        // altura
        igual(300, ArenaGeo.alturaBase(100, -64, 319, 10, 200), "altura: terreno baixo 100+200 passa do teto e limita em 308");
        igual(-1, ArenaGeo.alturaBase(310, -64, 319, 10, 32), "altura: nao cabe acima do terreno");
        igual(132, ArenaGeo.alturaBase(100, -64, 319, 10, 32), "altura: terreno + 32");
        igual(-1, ArenaGeo.alturaBase(0, 0, 8, 10, 32), "altura: mundo baixo demais");
        // candidatos
        var cand = ArenaGeo.candidatos(0, 0, 48, 2);
        igual(1 + 8 + 16, cand.size(), "candidatos: 25 pontos em 2 aneis");
        igual(0, cand.get(0)[0], "candidatos: o primeiro e o centro");
        var vistos = new java.util.HashSet<String>();
        for (int[] p : cand) vistos.add(p[0] + "," + p[1]);
        igual(cand.size(), vistos.size(), "candidatos: sem repeticao");
        // registro: reserva, sobreposicao, persistencia
        var reg = new ArenaRegistro();
        var cx = ArenaGeo.centrada(0, 0, 300, 15, 15, 10);
        igual(true, reg.livre("minecraft:overworld", cx, 32), "registro: vazio esta livre");
        var ar = reg.reservar(7, "minecraft:overworld", cx, "minecraft:glass", 1000L);
        ar.uuidA = "aaa";
        ar.origemA = new ArenaRegistro.Origem("minecraft:the_nether", 1.5, 64, -3.5, 90f, 10f);
        igual(false, reg.livre("minecraft:overworld", ArenaGeo.centrada(20, 0, 300, 15, 15, 10), 32), "registro: vizinha proxima recusada");
        igual(true, reg.livre("minecraft:overworld", ArenaGeo.centrada(200, 0, 300, 15, 15, 10), 32), "registro: vizinha longe aceita");
        igual(true, reg.livre("minecraft:the_end", ArenaGeo.centrada(0, 0, 300, 15, 15, 10), 32), "registro: outra dimensao aceita");
        reg.devolver.put("bbb", new ArenaRegistro.Origem("minecraft:overworld", 5, 70, 5, 0f, 0f));
        Path tmp = Files.createTempFile("arenas", ".json");
        reg.gravar(tmp);
        var lido = ArenaRegistro.ler(tmp);
        igual(1, lido.arenas.size(), "registro: leu 1 arena");
        igual(7, lido.arenas.get(0).duelo, "registro: duelo");
        igual("minecraft:the_nether", lido.arenas.get(0).origemA.dim, "registro: origem dim");
        igual(-3.5, lido.arenas.get(0).origemA.z, "registro: origem z");
        igual(cx, lido.arenas.get(0).caixa(), "registro: caixa igual");
        igual(70.0, lido.devolver.get("bbb").y, "registro: devolver");
        lido.liberar(7);
        igual(0, lido.arenas.size(), "registro: liberar");
        igual(true, lido.livre("minecraft:overworld", cx, 32), "registro: depois de liberar, livre de novo");
        Files.writeString(tmp, "{ isso nao e json");
        igual(0, ArenaRegistro.ler(tmp).arenas.size(), "registro: arquivo ruim vira vazio");
        Files.deleteIfExists(tmp);
    }

    static void dueloLog() throws Exception {
        var l = new DueloLog();
        var r = l.obter(5);
        r.nomeA = "Davi";
        r.nomeB = "Kaue";
        r.itensA.add("{\"id\":\"minecraft:diamond\"}");
        r.criadoEm = 1_700_000_000_000L;
        r.evento(r.criadoEm, "comecou");
        r.rastro(r.criadoEm + 5, "abriu bau em 1 2 3");
        r.estado = "RESOLVIDO";
        igual(r, l.obter(5), "log: obter devolve o mesmo");
        l.obter(6).nomeA = "Edu";
        igual(6, l.recentes(5).get(0).id, "log: mais novo primeiro");
        igual(1, l.recentes(1).size(), "log: limite");
        Path tmp = Files.createTempFile("duelos", ".jsonl");
        l.gravar(tmp);
        igual(2, Files.readAllLines(tmp).size(), "log: uma linha por duelo");
        Files.writeString(tmp, Files.readString(tmp) + "isto nao e json\n");
        var lido = DueloLog.ler(tmp);
        igual(2, lido.regs.size(), "log: linha ruim e ignorada");
        igual("Davi", lido.achar(5).nomeA, "log: nome");
        igual("abriu bau em 1 2 3", lido.achar(5).rastro.get(0).texto, "log: rastro");
        igual("RESOLVIDO", lido.achar(5).estado, "log: estado");
        for (int i = 0; i < 400; i++) r.rastro(i, "x" + i);
        igual(300, r.rastro.size(), "log: rastro limitado a 300");
        igual("14/11/2023 19:13:20", DueloLog.data(1_700_000_000_000L, "America/Sao_Paulo"), "log: data no fuso de SP");
        igual("-", DueloLog.data(0, "x"), "log: data vazia");
        Files.deleteIfExists(tmp);

        // anulacao de duelo de diamantes: devolve a aposta, tira do placar, cofre cobre o que nao foi recolhido
        var c = casa();
        c.config.rakePct = 10;
        var d = c.proporDuelo("A", "Davi", "B", "Kaue", 10, 1L);
        c.ativarDuelo(d, 2L);
        int prem = c.resolverDuelo(d, "A", 3L);
        igual(18, prem, "anular: premio com taxa de 10%");
        igual(2L, c.cofre, "anular: cofre ficou com a taxa");
        igual(18, c.creditoDe("A"), "anular: vencedor recebeu o premio");
        igual(1, c.placarDuelos.size(), "anular: placar tem o duelo");
        igual(d.id, c.placarDuelos.get(0).id, "anular: placar guarda o id");
        igual(18, d.premio, "anular: duelo guarda o premio");
        c.baixarCredito("A", 18); // o vencedor ja pegou
        c.anularDuelo(d.id, "A", "Davi", "B", "Kaue", "A", 10, false, d.premio, 8, 4L);
        igual("ANULADO", d.estado, "anular: estado");
        igual(10, c.creditoDe("A"), "anular: A recebeu a aposta de volta");
        igual(10, c.creditoDe("B"), "anular: B recebeu a aposta de volta");
        igual(0, c.placarDuelos.size(), "anular: saiu do placar");
        igual((long) (2 - (20 - 8)), c.cofre, "anular: cofre cobre o que nao foi recuperado");
        igual(0, c.jogador("A", "Davi").duelosGanhos, "anular: vitoria desfeita");
        igual(0, c.jogador("B", "Kaue").duelosPerdidos, "anular: derrota desfeita");
        igual(0L, c.jogador("A", "Davi").gasto, "anular: gasto desfeito");
        igual(0L, c.jogador("A", "Davi").ganho, "anular: ganho desfeito");
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

    static void duelos() {
        Casa c = casa();
        igual("Você não pode duelar contra você mesmo.", c.validarDuelo("u1", "u1", 5), "duelo contra si");
        igual("Valor máximo: 64 diamantes.", c.validarDuelo("u1", "u2", 65), "duelo valor maximo");
        igual(null, c.validarDuelo("u1", "u2", 10), "duelo ok");
        Casa.Duelo d = c.proporDuelo("u1", "Davi", "u2", "Kaue", 10, 0);
        igual("Você já tem um convite pendente.", c.validarDuelo("u1", "u3", 5), "convite pendente");
        c.ativarDuelo(d, 1);
        igual("Você já está em um duelo.", c.validarDuelo("u1", "u3", 5), "ja em duelo");
        igual("O outro jogador já está em um duelo.", c.validarDuelo("u3", "u2", 5), "outro em duelo");
        int premio = c.resolverDuelo(d, "u2", 2);
        igual(19, premio, "premio 20 menos 5%");
        igual(1L, c.cofre, "rake do duelo");
        igual(19, c.creditoDe("u2"), "credito do vencedor");
        igual(0, c.creditoDe("u1"), "perdedor sem credito");
        igual(1, c.jogadores.get("u2").acertos, "acerto do vencedor");
        Casa.Duelo e = c.proporDuelo("u1", "Davi", "u2", "Kaue", 8, 0);
        c.ativarDuelo(e, 1);
        c.cancelarDuelo(e, 2);
        igual(27, c.creditoDe("u2"), "devolvido ao b");
        igual(8, c.creditoDe("u1"), "devolvido ao a");
        igual(null, c.duelAtivoDe("u1"), "sem duelo ativo");
        Casa f = casa();
        Casa.Duelo di = f.proporDuelo("u1", "Davi", "u2", "Kaue", 0, 0);
        di.itens = true;
        di.estado = "ESCOLHENDO";
        igual("Você já está em um duelo.", f.validarDuelo("u1", "u3", 5), "escolhendo conta como ocupado");
        f.ativarDuelo(di, 1);
        f.resolverDueloItens(di, "u1", 2);
        igual(0L, f.cofre, "duelo de itens sem taxa");
        igual(1, f.jogadores.get("u1").acertos, "acerto do duelo de itens");
        Casa.Duelo dj = f.proporDuelo("u1", "Davi", "u2", "Kaue", 0, 0);
        dj.itens = true;
        f.ativarDuelo(dj, 1);
        f.cancelarDuelo(dj, 2);
        igual(0, f.creditoDe("u1"), "duelo de itens cancelado nao devolve diamantes");
    }

    static void reinicio() {
        igual(true, Reinicio.valido("04:30"), "hora valida");
        igual(true, Reinicio.valido("4:30"), "hora sem zero");
        igual(false, Reinicio.valido("24:00"), "hora invalida");
        igual(false, Reinicio.valido("4:7"), "minuto invalido");
        igual("04:30", Reinicio.normalizar("4:30"), "normaliza");
        igual(null, Reinicio.normalizar("abc"), "normaliza invalido");
        java.time.ZoneId z = java.time.ZoneId.of("UTC");
        java.time.Instant ag = java.time.Instant.parse("2026-10-27T03:00:00Z");
        igual(5_400_000L, Reinicio.msAteProximo(List.of("04:30"), z, ag), "ate 04:30");
        igual(5_400_000L, Reinicio.msAteProximo(List.of("20:00", "04:30"), z, ag), "pega o mais proximo");
        igual(23L * 3600_000L, Reinicio.msAteProximo(List.of("02:00"), z, ag), "amanha");
        igual(-1L, Reinicio.msAteProximo(List.of(), z, ag), "sem horarios");
        igual(300L, Reinicio.avisoCruzado(301_000, 299_000), "cruzou 5 min");
        igual(-1L, Reinicio.avisoCruzado(400_000, 350_000), "nao cruzou");
        igual(10L, Reinicio.avisoCruzado(11_000, 10_000), "cruzou 10s");
        igual("5 minutos", Reinicio.texto(300), "texto minutos");
        igual("1 segundo", Reinicio.texto(1), "texto segundo");
        Casa c = casa();
        Casa.Duelo d = c.proporDuelo("u1", "Davi", "u2", "Kaue", 5, 0);
        c.ativarDuelo(d, 1);
        c.resolverDuelo(d, "u1", 2);
        igual(1, c.jogadores.get("u1").duelosGanhos, "duelo ganho");
        igual(1, c.jogadores.get("u2").duelosPerdidos, "duelo perdido");
    }

    static void morteEPlacar() {
        igual("norte", Direcao.cardinal(0, -10), "norte");
        igual("sul", Direcao.cardinal(0, 10), "sul");
        igual("leste", Direcao.cardinal(10, 0), "leste");
        igual("oeste", Direcao.cardinal(-10, 0), "oeste");
        igual("sudoeste", Direcao.cardinal(-10, 10), "sudoeste");
        igual("nordeste", Direcao.cardinal(7, -7), "nordeste");
        // olhando para o sul (yaw 0), um ponto ao sul está à frente
        igual("↑", Direcao.seta(0, 10, 0), "seta a frente");
        // olhando para o sul, o leste fica à esquerda
        igual("←", Direcao.seta(10, 0, 0), "seta esquerda");
        // olhando para o norte (yaw 180), um ponto ao sul está atrás
        igual("↓", Direcao.seta(0, 10, 180), "seta atras");
        // olhando para o oeste (yaw 90), o norte fica à direita
        igual("→", Direcao.seta(0, -10, 90), "seta direita");

        Casa c = casa();
        Casa.Duelo a = c.proporDuelo("u1", "Davi", "u2", "Kaue", 5, 0);
        c.ativarDuelo(a, 1);
        c.resolverDuelo(a, "u1", 10);
        Casa.Duelo b = c.proporDuelo("u2", "Kaue", "u1", "Davi", 5, 0);
        c.ativarDuelo(b, 1);
        c.resolverDuelo(b, "u2", 11);
        Casa.Duelo e = c.proporDuelo("u1", "Davi", "u2", "Kaue", 5, 0);
        c.ativarDuelo(e, 1);
        c.resolverDuelo(e, "u1", 12);
        int[] pl = c.confronto("davi", "KAUE");
        igual(2, pl[0], "Davi venceu 2");
        igual(1, pl[1], "Kaue venceu 1");
        igual(3, c.duelosDe("Davi", 10).size(), "historico do Davi");
        igual(12L, c.duelosDe("Kaue", 10).get(0).ts, "mais novo primeiro");
        igual(1, c.duelosDe("Davi", 1).size(), "limite do historico");
        Casa.Duelo f = c.proporDuelo("u1", "Davi", "u2", "Kaue", 0, 0);
        f.itens = true;
        c.ativarDuelo(f, 1);
        c.resolverDueloItens(f, "u2", 13, "1x Espada");
        igual("1x Espada", c.duelosDe("Kaue", 1).get(0).resumo, "resumo do duelo de itens");
        igual(true, c.duelosDe("Kaue", 1).get(0).itens, "marcado como itens");
    }

    static void apostaNoDuelo() {
        Casa c = casa();
        Bolao b = c.criar("Duelo: Davi x Kaue", Bolao.Gatilho.MANUAL, List.of("Davi", "Kaue"), 0);
        b.dueloId = 7;
        b.excluidos.add("u1");
        b.excluidos.add("u2");
        b.maxAposta = 16;
        c.abrir(b);
        igual("Você está nesse duelo: não pode apostar nele.", c.validarAposta(b, "u1", "Davi", "Kaue", 5), "duelista nao aposta");
        igual("Você está nesse duelo: não pode apostar nele.", c.validarAposta(b, "u2", "Kaue", "Davi", 5), "outro duelista nao aposta");
        igual(null, c.validarAposta(b, "u3", "Edu", "Davi", 16), "espectador no limite");
        igual("Limite por bolão: 16 diamantes (você já pôs 0).", c.validarAposta(b, "u3", "Edu", "Davi", 17), "limite do duelo");
        c.apostar(b, "u3", "Edu", "Davi", 10);
        c.apostar(b, "u4", "Gota", "Kaue", 10);
        c.resolver(b, "Davi", 1);
        igual(19, c.creditoDe("u3"), "espectador que acertou recebe");
        igual(0, c.creditoDe("u4"), "espectador que errou nao recebe");
    }

    static void limpeza() {
        igual(List.of(30L, 10L, 5L, 3L, 2L, 1L), br.melby.melbybet.core.Limpeza.limiares(30), "limiares 30s");
        igual(List.of(8L, 5L, 3L, 2L, 1L), br.melby.melbybet.core.Limpeza.limiares(8), "limiares 8s");
        igual(List.of(), br.melby.melbybet.core.Limpeza.limiares(0), "sem aviso");
        var l = br.melby.melbybet.core.Limpeza.limiares(30);
        igual(30L, br.melby.melbybet.core.Limpeza.cruzado(l, 31_000, 29_000), "cruzou 30s");
        igual(-1L, br.melby.melbybet.core.Limpeza.cruzado(l, 20_000, 19_000), "nao cruzou");
        igual(10L, br.melby.melbybet.core.Limpeza.cruzado(l, 11_000, 10_000), "cruzou 10s");
        igual(0, new Casa().config.limpezaMinutos, "limpeza desligada por padrao");
        igual(true, new Casa().config.limpezaProtege, "protege valiosos por padrao");
    }

    static void apelidos() {
        var r = Apelido.ler("&6Rei&eDoMel");
        igual("ReiDoMel", r.texto, "apelido sem codigos");
        igual(0xFFAA00, r.cor1, "cor 1 do apelido");
        igual(0xFFFF55, r.cor2, "cor 2 do apelido");
        var h = Apelido.ler("&#ff0000Fogo&#0000ffAgua");
        igual("FogoAgua", h.texto, "apelido hex");
        igual(0xFF0000, h.cor1, "hex 1");
        igual(0x0000FF, h.cor2, "hex 2");
        var s = Apelido.ler("Simples");
        igual(-1, s.cor1, "sem cor");
        igual(null, Apelido.validar("Simples"), "valido");
        igual(true, Apelido.validar("a") != null, "curto demais");
        igual(true, Apelido.validar("nome<>") != null, "caractere ruim");
    }

    static void placas() {
        var s = TextoPlaca.ler("&cVermelho &lNegrito");
        igual(2, s.size(), "placa: 2 trechos");
        igual(0xFF5555, s.get(0).cor, "placa: cor &c");
        igual(true, s.get(1).negrito, "placa: negrito");
        igual(0xFF5555, s.get(1).cor, "placa: negrito mantem a cor");
        var h = TextoPlaca.ler("&#00ff00Verde&rNormal");
        igual(0x00FF00, h.get(0).cor, "placa: hex");
        igual(-1, h.get(1).cor, "placa: &r limpa");
        var g = TextoPlaca.ler("<#ff0000-#0000ff>abc</>d");
        igual(4, g.size(), "placa: degrade 3 letras + 1");
        igual(0xFF0000, g.get(0).cor, "placa: degrade comeca");
        igual(0x0000FF, g.get(2).cor, "placa: degrade termina");
        igual(-1, g.get(3).cor, "placa: depois do degrade");
        igual("abcd", TextoPlaca.semCodigos("<#ff0000-#0000ff>abc</>d"), "placa: sem codigos");
        igual(false, TextoPlaca.temCodigo("Loja do Melby"), "placa: sem codigo");
        igual(true, TextoPlaca.temCodigo("&aLoja"), "placa: com codigo");
    }

    static void loteria() {
        Casa casa = new Casa();
        var l = casa.loteria;
        igual(true, l.abrir(0, 10, 10, 0) != null, "loteria: preco invalido");
        igual(null, l.abrir(2, 10, 5, 0L), "loteria: abre");
        igual(true, l.abrir(2, 10, 5, 0L) != null, "loteria: nao abre duas");
        igual(null, l.validarCompra("a", 3, 1000L), "loteria: compra ok");
        l.comprar("a", "Ana", 3);
        igual(true, l.validarCompra("a", 3, 1000L) != null, "loteria: passou do limite");
        igual(true, l.validarCompra("b", 1, 10 * 60_000L + 1) != null, "loteria: tempo acabou");
        l.comprar("b", "Beto", 2);
        igual(10, l.pote(), "loteria: pote");
        var r = l.sortear(casa, new java.util.Random(1), 5L);
        igual(false, r.devolvido, "loteria: sorteou");
        igual(10 - 10 * casa.config.rakePct / 100, r.premio, "loteria: premio com taxa");
        igual(r.premio, casa.creditoDe(r.uuid), "loteria: credito ao vencedor");
        igual(false, l.ativa, "loteria: encerrou");
        l.abrir(1, 5, 10, 0L);
        l.comprar("a", "Ana", 4);
        var d = l.sortear(casa, new java.util.Random(1), 5L);
        igual(true, d.devolvido, "loteria: so uma pessoa devolve");
        igual(4 + r.premio * (r.uuid.equals("a") ? 1 : 0), casa.creditoDe("a"), "loteria: devolveu os bilhetes");
        l.abrir(1, 5, 10, 0L);
        l.comprar("b", "Beto", 2);
        igual(true, l.cancelar(casa, 1L), "loteria: cancela");
        igual(false, l.cancelar(casa, 1L), "loteria: nada a cancelar");
    }

    static void corpos() {
        Casa casa = new Casa();
        var c = casa.novoCorpo("u1", "Ana", "overworld", 1, 64, 2, java.util.List.of("a", "b"), 10L);
        c.boneco = "stand-1";
        var d = casa.novoCorpo("u2", "Beto", "the_nether", 0, 70, 0, java.util.List.of("x"), 11L);
        igual(1, c.id, "corpo: primeiro id");
        igual(2, d.id, "corpo: segundo id");
        igual(1, casa.corposDe("u1").size(), "corpo: do jogador");
        igual(c, casa.corpoDoBoneco("stand-1"), "corpo: pelo boneco");
        igual(null, casa.corpoDoBoneco("nada"), "corpo: boneco inexistente");
        casa.removerCorpo(1);
        igual(null, casa.corpo(1), "corpo: removido");
        igual(1, casa.corpos.size(), "corpo: sobrou um");
    }

    static void msgMatar() {
        Casa casa = new Casa();
        var r = new java.util.Random(3);
        igual(null, casa.escolherMsgMatar("Ana", "Beto", r), "msgmatar: nenhuma");
        casa.config.msgMatar.put("ana", "antiga {vitima}");
        igual("antiga {vitima}", casa.escolherMsgMatar("Ana", "Beto", r), "msgmatar: migra a antiga");
        igual(0, casa.config.msgMatar.size(), "msgmatar: antiga limpa");
        casa.msgsMatar("Ana", null).add("B {vitima}");
        casa.msgsMatar("Ana", "Beto").add("so pro Beto {vitima}");
        igual("so pro Beto {vitima}", casa.escolherMsgMatar("Ana", "Beto", r), "msgmatar: especifica vence");
        igual(true, casa.escolherMsgMatar("Ana", "Caio", r) != null, "msgmatar: padrao para os outros");
        igual(3, casa.todasMsgsMatar("Ana").size(), "msgmatar: lista numerada");
        igual(true, casa.removerMsgMatar("Ana", 3), "msgmatar: remove a especifica");
        igual(2, casa.todasMsgsMatar("Ana").size(), "msgmatar: sobraram 2");
        igual(false, casa.removerMsgMatar("Ana", 9), "msgmatar: numero invalido");
        igual(0, Marcos.maiorAte(4.9), "marcos: nenhum");
        igual(10, Marcos.maiorAte(12), "marcos: 10h");
        igual(2000, Marcos.maiorAte(99999), "marcos: maior");
    }

    static void backups() throws Exception {
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("mbbk");
        java.nio.file.Path orig = dir.resolve("casa.json");
        igual(null, Backups.copiar(orig, dir.resolve("b"), "x.json"), "backup: sem origem");
        java.nio.file.Files.writeString(orig, "{}");
        for (int i = 1; i <= 5; i++) Backups.copiar(orig, dir.resolve("b"), "casa-2026-01-0" + i + ".json");
        Backups.copiar(orig, dir.resolve("b"), "manual-1.json");
        igual(true, Backups.existe(dir.resolve("b"), "casa-2026-01-03.json"), "backup: existe");
        igual(3, Backups.limpar(dir.resolve("b"), "casa-2", 2), "backup: apagou os antigos");
        igual(false, Backups.existe(dir.resolve("b"), "casa-2026-01-01.json"), "backup: o mais antigo saiu");
        igual(true, Backups.existe(dir.resolve("b"), "casa-2026-01-05.json"), "backup: o mais novo ficou");
        igual(true, Backups.existe(dir.resolve("b"), "manual-1.json"), "backup: outro prefixo intacto");
    }

    static void busca() {
        igual(100, Busca.pontuar("cortador de pedras", "Cortador de Pedras"), "busca: igual");
        igual(true, Busca.pontuar("cortador de pedra", "Cortador de Pedras") >= 60, "busca: singular acha plural");
        igual(true, Busca.pontuar("Tabuas", "Tábuas de Carvalho") >= 60, "busca: sem acento");
        igual(0, Busca.pontuar("espada", "Cortador de Pedras"), "busca: nao combina");
        igual(true, Busca.pontuar("diamante", "Diamante") > Busca.pontuar("diamante", "Bloco de Diamante"), "busca: o exato vence");
        igual(true, Busca.melhor("stonecutter", "Cortador de Pedras", "Stonecutter", "stonecutter") == 100, "busca: id em ingles");
        igual(true, Busca.pontuar("pedra", "Cortador de Pedras") > 0, "busca: parte do nome");
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

    static void marcador() throws Exception {
        var d = new MarcadorDados();
        var sel = d.de("u1").atual;
        igual(false, sel.completa(), "marcador: vazio");
        igual(null, sel.area(), "marcador: sem área");
        sel.definir(1, "overworld", 10, 70, -5);
        igual(false, sel.completa(), "marcador: um canto só");
        sel.definir(2, "overworld", 4, 60, 3);
        var a = sel.area();
        igual(4, a.x0, "marcador: x0 normalizado");
        igual(10, a.x1, "marcador: x1 normalizado");
        igual(60, a.y0, "marcador: y0");
        igual(70, a.y1, "marcador: y1");
        igual(-5, a.z0, "marcador: z0");
        igual(3, a.z1, "marcador: z1");
        igual(7, a.larg(), "marcador: largura inclui os dois cantos");
        igual(11, a.alt(), "marcador: altura");
        igual(9, a.comp(), "marcador: comprimento");
        igual(7L * 11 * 9, a.volume(), "marcador: volume");
        igual(true, a.contem(4, 60, -5), "marcador: contém canto");
        igual(true, a.contem(10, 70, 3), "marcador: contém canto oposto");
        igual(false, a.contem(11, 65, 0), "marcador: fora");
        igual("x 4..10, y 60..70, z -5..3", a.resumo(), "marcador: resumo");
        // o volume não estoura int
        var gigante = new MarcadorDados.Selecao();
        gigante.definir(1, "overworld", -30000000, -64, -30000000);
        gigante.definir(2, "overworld", 30000000, 319, 30000000);
        igual(true, gigante.area().volume() > Integer.MAX_VALUE, "marcador: volume em long");
        // trocar de dimensão descarta o outro canto
        sel.definir(1, "the_nether", 1, 1, 1);
        igual(false, sel.completa(), "marcador: troca de dimensão descarta o outro canto");
        igual("the_nether", sel.dim, "marcador: dimensão nova");
        sel.definir(2, "the_nether", 3, 3, 3);
        igual(true, sel.completa(), "marcador: completa de novo");
        // expandir e contrair
        igual(true, sel.ajustar(2, -64, 319), "marcador: expandir");
        igual(-1, sel.area().x0, "marcador: expandiu x0");
        igual(5, sel.area().x1, "marcador: expandiu x1");
        igual(7, sel.area().larg(), "marcador: largura depois de expandir");
        igual(false, sel.ajustar(-4, -64, 319), "marcador: encolher demais é recusado");
        igual(7, sel.area().larg(), "marcador: recusado não mexe");
        igual(true, sel.ajustar(-3, -64, 319), "marcador: encolher até 1 bloco");
        igual(1, sel.area().larg(), "marcador: sobrou 1");
        sel.definir(1, "the_nether", 0, 5, 0);
        sel.definir(2, "the_nether", 2, 6, 2);
        igual(true, sel.ajustar(10, 0, 8), "marcador: expandir limitado pelo mundo");
        igual(0, sel.area().y0, "marcador: y não passa do fundo");
        igual(8, sel.area().y1, "marcador: y não passa do topo");
        igual(true, sel.altura(-64, 319), "marcador: altura total");
        igual(-64, sel.area().y0, "marcador: y fundo");
        igual(319, sel.area().y1, "marcador: y topo");
        sel.limpar();
        igual(false, sel.completa(), "marcador: limpar");
        igual(false, sel.ajustar(1, 0, 10), "marcador: ajustar sem área");

        // nomes
        igual(null, MarcadorDados.validarNome("Casa_1-b"), "marcador: nome ok");
        igual(null, MarcadorDados.validarNome("Praça"), "marcador: acento ok");
        igual(true, MarcadorDados.validarNome("a b") != null, "marcador: espaço recusado");
        igual(true, MarcadorDados.validarNome("a:b") != null, "marcador: dois-pontos recusado");
        igual(true, MarcadorDados.validarNome("") != null, "marcador: vazio recusado");
        igual(true, MarcadorDados.validarNome("x".repeat(25)) != null, "marcador: nome longo recusado");
        igual(null, MarcadorDados.validarNome("x".repeat(24)), "marcador: 24 letras ok");

        // salvar, achar, compartilhar
        var s1 = new MarcadorDados.Selecao();
        s1.definir(1, "overworld", 0, 0, 0);
        s1.definir(2, "overworld", 9, 4, 9);
        igual(null, d.salvar("u1", "Davi", "Casa", s1.area(), 1000L), "marcador: salvar");
        igual("Davi", d.buscar("u1", "casa").donoNome, "marcador: busca sem diferenciar maiúscula");
        igual("u1", d.buscar("u1", "CASA").dono, "marcador: dono");
        igual(1000L, d.buscar("u1", "Casa").criadaEm, "marcador: data");
        igual(true, d.salvar("u1", "Davi", "Casa", null, 1L) != null, "marcador: salvar sem área");
        igual(true, d.salvar("u1", "Davi", "x y", s1.area(), 1L) != null, "marcador: salvar nome ruim");
        igual(null, d.buscar("u2", "Casa"), "marcador: outro jogador não vê");
        igual(0, d.visiveis("u2").size(), "marcador: nada visível para u2");
        igual(null, d.compartilhar("u1", "casa", "u2"), "marcador: compartilhar");
        igual(true, d.compartilhar("u1", "casa", "u2") != null, "marcador: já compartilhada");
        igual(true, d.compartilhar("u1", "casa", "u1") != null, "marcador: não compartilha consigo");
        igual(true, d.compartilhar("u1", "nada", "u2") != null, "marcador: área inexistente");
        igual("Casa", d.buscar("u2", "Davi:casa").nome, "marcador: dono:nome");
        igual("Casa", d.buscar("u2", "casa").nome, "marcador: nome único compartilhado");
        igual(1, d.visiveis("u2").size(), "marcador: u2 vê uma");
        igual(null, d.buscar("u2", "Kaue:casa"), "marcador: dono errado");
        // regravar com o mesmo nome mantém quem já tinha acesso
        igual(null, d.salvar("u1", "Davi", "casa", s1.area(), 2000L), "marcador: regravar");
        igual(1, d.buscar("u1", "casa").partilha.size(), "marcador: regravar mantém o compartilhamento");
        igual(1, d.de("u1").salvas.size(), "marcador: regravar não duplica");
        // nome igual ao de outro dono: a sua vence, e dono:nome desambigua
        igual(null, d.salvar("u2", "Kaue", "Casa", s1.area(), 3000L), "marcador: u2 salva com o mesmo nome");
        igual("Kaue", d.buscar("u2", "casa").donoNome, "marcador: a sua vence");
        igual("Davi", d.buscar("u2", "Davi:casa").donoNome, "marcador: dono:nome desambigua");
        // ambíguo entre duas compartilhadas
        igual(null, d.salvar("u3", "Samuel", "Torre", s1.area(), 1L), "marcador: u3 salva");
        igual(null, d.salvar("u4", "Hugo", "Torre", s1.area(), 1L), "marcador: u4 salva");
        d.compartilhar("u3", "torre", "u5");
        d.compartilhar("u4", "torre", "u5");
        igual(null, d.buscar("u5", "torre"), "marcador: ambíguo devolve null");
        igual("Hugo", d.buscar("u5", "Hugo:torre").donoNome, "marcador: ambíguo se resolve com dono:nome");
        igual(null, d.descompartilhar("u1", "casa", "u2"), "marcador: descompartilhar");
        igual(true, d.descompartilhar("u1", "casa", "u2") != null, "marcador: já tinha tirado");
        igual(null, d.buscar("u5", "Davi:casa"), "marcador: nunca compartilhada com u5");
        igual(true, d.apagar("u1", "CASA"), "marcador: apagar");
        igual(false, d.apagar("u1", "casa"), "marcador: apagar de novo");
        // limite de áreas
        var cheio = new MarcadorDados();
        for (int i = 0; i < MarcadorDados.MAX_AREAS; i++) igual(null, cheio.salvar("u", "U", "a" + i, s1.area(), 1L), "marcador: limite " + i);
        igual(true, cheio.salvar("u", "U", "extra", s1.area(), 1L) != null, "marcador: passou do limite");
        igual(null, cheio.salvar("u", "U", "a0", s1.area(), 1L), "marcador: regravar no limite é permitido");

        // arestas das partículas
        var cubo = new MarcadorDados.Area();
        var unit = MarcadorDados.arestas(cubo, 100);
        igual(8, unit.size(), "marcador: um bloco tem 8 cantos");
        var media = new MarcadorDados.Area();
        media.x1 = 4;
        media.y1 = 4;
        media.z1 = 4;
        var pm = MarcadorDados.arestas(media, 1000);
        igual(true, pm.size() > 8 && pm.size() <= 1000, "marcador: caixa média dentro do limite");
        var grande = new MarcadorDados.Area();
        grande.x1 = 999;
        grande.y1 = 99;
        grande.z1 = 999;
        var pg = MarcadorDados.arestas(grande, 500);
        igual(true, pg.size() <= 600, "marcador: caixa grande é espaçada (" + pg.size() + ")");
        boolean canto = false;
        for (double[] q : pg) if (q[0] == 1000 && q[1] == 100 && q[2] == 1000) canto = true;
        igual(true, canto, "marcador: o canto oposto sempre aparece");
        var vistos = new java.util.HashSet<String>();
        boolean repetido = false;
        for (double[] q : pg) if (!vistos.add(q[0] + "," + q[1] + "," + q[2])) repetido = true;
        igual(false, repetido, "marcador: sem pontos repetidos");

        // disco
        Path tmp = Files.createTempDirectory("melbymarc");
        Path f = tmp.resolve("sub").resolve("marcadores.json");
        d.gravar(f);
        var l = MarcadorDados.ler(f);
        igual(d.de("u2").salvas.size(), l.de("u2").salvas.size(), "marcador: disco guarda áreas");
        igual("Kaue", l.buscar("u2", "casa").donoNome, "marcador: disco guarda dono");
        igual(0, MarcadorDados.ler(tmp.resolve("nao.json")).jogadores.size(), "marcador: arquivo ausente começa vazio");
        Files.writeString(tmp.resolve("ruim.json"), "{ isso nao e json");
        igual(0, MarcadorDados.ler(tmp.resolve("ruim.json")).jogadores.size(), "marcador: arquivo ilegível começa vazio");
        Files.writeString(tmp.resolve("parcial.json"), "{\"jogadores\":{\"x\":{}}}");
        var pj = MarcadorDados.ler(tmp.resolve("parcial.json"));
        igual(false, pj.de("x").atual.completa(), "marcador: campos faltando viram padrão");
        igual(0, pj.de("x").salvas.size(), "marcador: salvas faltando viram vazio");
    }

    static void catalogo() throws Exception {
        var c = new ContagemBlocos.Contagem();
        c.somar("stone", 100);
        c.somar("oak_planks", 50);
        c.somar("stone", 20);
        c.somar("glass", 50);
        c.somar("dirt", 0);
        c.somar("dirt", -3);
        igual(220L, c.total, "catalogo: total");
        igual(3, c.blocos.size(), "catalogo: zero e negativo ignorados");
        var o = c.ordenado();
        igual("stone", o.get(0).getKey(), "catalogo: mais numeroso primeiro");
        igual(120L, o.get(0).getValue(), "catalogo: soma do mesmo tipo");
        igual("glass", o.get(1).getKey(), "catalogo: empate em ordem alfabética");
        igual("oak_planks", o.get(2).getKey(), "catalogo: empate, segundo");
        igual("54,5%", ContagemBlocos.percentual(120, 220), "catalogo: percentual");
        igual("0%", ContagemBlocos.percentual(1, 0), "catalogo: percentual sem total");
        igual("10", ContagemBlocos.pilhas(10, 64), "catalogo: menos de uma pilha");
        igual("1 pilha", ContagemBlocos.pilhas(64, 64), "catalogo: uma pilha");
        igual("2 pilhas + 5", ContagemBlocos.pilhas(133, 64), "catalogo: pilhas e resto");
        igual("1 shulker", ContagemBlocos.pilhas(27 * 64, 64), "catalogo: um shulker");
        igual("2 shulkers + 1 pilha + 3", ContagemBlocos.pilhas(2 * 27 * 64 + 67, 64), "catalogo: shulkers, pilha e resto");
        
        igual("1 pilha + 4", ContagemBlocos.pilhas(20, 16), "catalogo: pilha de 16") ;
        igual("5", ContagemBlocos.pilhas(5, 1), "catalogo: itens que não empilham");
        var gr = new ContagemBlocos.Contagem();
        for (int i = 0; i < 30; i++) gr.somar("b" + (i < 10 ? "0" : "") + i, 30 - i);
        igual(3, gr.paginas(), "catalogo: 30 tipos = 3 páginas");
        igual(12, gr.pagina(1).size(), "catalogo: página cheia");
        igual(6, gr.pagina(3).size(), "catalogo: última página");
        igual(0, gr.pagina(4).size(), "catalogo: página inexistente");
        igual(0, gr.pagina(0).size(), "catalogo: página zero");
        igual(1, new ContagemBlocos.Contagem().paginas(), "catalogo: vazio tem 1 página");
        var d = new ContagemBlocos();
        var p = new ContagemBlocos.Projeto();
        p.contagem = c;
        p.larg = 3;
        igual(null, d.salvar("u1", "Torre", p, 5L), "catalogo: salvar projeto");
        igual(true, d.salvar("u1", "T o", p, 5L) != null, "catalogo: nome ruim");
        igual(220L, d.obter("u1", "torre").contagem.total, "catalogo: obter sem diferenciar maiúscula");
        igual(null, d.obter("u2", "torre"), "catalogo: outro jogador não vê");
        igual(1, d.lista("u1").size(), "catalogo: lista");
        for (int i = 0; i < ContagemBlocos.MAX_PROJETOS - 1; i++) d.salvar("u1", "p" + i, p, 1L);
        igual(true, d.salvar("u1", "extra", p, 1L) != null, "catalogo: limite de projetos");
        igual(null, d.salvar("u1", "torre", p, 9L), "catalogo: regravar no limite");
        igual(9L, d.obter("u1", "torre").criadoEm, "catalogo: regravar atualiza");
        Path tmp = Files.createTempDirectory("melbycat");
        Path f = tmp.resolve("catalogo.json");
        d.gravar(f);
        var l = ContagemBlocos.ler(f);
        igual(220L, l.obter("u1", "torre").contagem.total, "catalogo: disco guarda o total");
        igual(120L, l.obter("u1", "torre").contagem.blocos.get("stone"), "catalogo: disco guarda os tipos");
        igual(true, l.apagar("u1", "torre"), "catalogo: apagar");
        igual(false, l.apagar("u1", "torre"), "catalogo: apagar de novo");
        igual(0, ContagemBlocos.ler(tmp.resolve("x.json")).projetos.size(), "catalogo: arquivo ausente");
    }

    /** Mesma aparência, caractere por caractere. */
    static boolean mesmaAparencia(String a, String b) {
        var x = new StringBuilder();
        var y = new StringBuilder();
        for (var s : TextoPlaca.ler(a)) for (char c : s.texto.toCharArray()) x.append(c).append(s.cor).append(s.negrito).append(s.italico).append(s.sublinhado).append(s.riscado).append(s.embaralhado).append(';');
        for (var s : TextoPlaca.ler(b)) for (char c : s.texto.toCharArray()) y.append(c).append(s.cor).append(s.negrito).append(s.italico).append(s.sublinhado).append(s.riscado).append(s.embaralhado).append(';');
        return x.toString().equals(y.toString());
    }

    static void placaCopiar() {
        String[] amostras = {"", "texto simples", "&cvermelho", "&c&lnegrito vermelho&r normal", "&#ff8a24laranja &9azul",
            "<#ff0000-#0000ff>degradê bonito</>", "&l<#ff0000-#00ff00>negrito e degradê</> fim", "ab<#ffffff-#000000>cde</>fg",
            "&aum &bdois &ctrês", "&n&m&o&ktudo", "&1&2&3&4&5&6&7&8&9&a&b&c&d&e&f", "&e&lA&r&eB", "<#123456-#abcdef>xy</>", "&#010203x&#040506y"};
        for (String a : amostras) {
            String volta = TextoPlaca.paraCodigos(TextoPlaca.ler(a));
            igual(true, mesmaAparencia(a, volta), "placa copiar: ida e volta de [" + a + "] -> [" + volta + "]");
            igual(TextoPlaca.semCodigos(a), TextoPlaca.semCodigos(volta), "placa copiar: mesmo texto de [" + a + "]");
        }
        igual("", TextoPlaca.paraCodigos(TextoPlaca.ler("")), "placa copiar: vazio");
        igual("texto simples", TextoPlaca.paraCodigos(TextoPlaca.ler("texto simples")), "placa copiar: sem formatação fica igual");
        igual("&cvermelho", TextoPlaca.paraCodigos(TextoPlaca.ler("&cvermelho")), "placa copiar: cor da paleta usa letra");
        igual(true, TextoPlaca.paraCodigos(TextoPlaca.ler("&#ff8a24x")).contains("&#ff8a24"), "placa copiar: cor exata usa hex");
        String g = TextoPlaca.paraCodigos(TextoPlaca.ler("<#ff0000-#0000ff>degradê</>"));
        igual(true, g.contains("<#ff0000-#0000ff>") && g.endsWith("</>"), "placa copiar: degradê volta como degradê (" + g + ")");
        igual(true, g.length() < 40, "placa copiar: degradê não vira um código por letra");
        // aparência que não vem de códigos (texto vindo de outro mod ou do /data): cai em cor por letra, sem quebrar
        var sg = new TextoPlaca.Seg();
        sg.texto = "oi";
        sg.cor = 0x123456;
        igual("&#123456oi", TextoPlaca.paraCodigos(java.util.List.of(sg)), "placa copiar: segmento solto");
    }
}
