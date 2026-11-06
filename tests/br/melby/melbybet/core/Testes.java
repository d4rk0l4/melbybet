package br.melby.melbybet.core;

import com.google.gson.JsonObject;

import java.awt.image.BufferedImage;
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
        placaHistorico();
        etiquetaBau();
        mapArt();
        banner();
        armorStand();
        fotoMoldura();
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

    static void placaHistorico() throws Exception {
        var h = new PlacaHistorico();
        String k = PlacaHistorico.chave("overworld", 1, 64, 2, "FRONT");
        String[] vazio = {"", "", "", ""};
        String[] a = {"&cLoja", "do Davi", "", ""};
        String[] b = {"&cLoja", "do Kaue", "", ""};
        igual(null, h.registrar(k, vazio, vazio, "u1", "Davi", 1L), "placa hist: sem mudança não registra");
        igual(0, h.placas.size(), "placa hist: sem mudança não cria entrada");
        var v1 = h.registrar(k, vazio, a, "u1", "Davi", 10L);
        igual(1, v1.id, "placa hist: primeiro id");
        igual(1, h.lista(k).size(), "placa hist: placa nova em branco não tem versão original");
        var v2 = h.registrar(k, a, b, "u2", "Kaue", 20L);
        igual(2, v2.id, "placa hist: segundo id");
        igual(null, h.registrar(k, b, b.clone(), "u2", "Kaue", 21L), "placa hist: igual não registra");
        igual("Kaue", h.lista(k).get(0).nome, "placa hist: mais nova primeiro");
        igual("Davi", h.lista(k).get(1).nome, "placa hist: mais antiga por último");
        igual(k, h.chaveDe(1), "placa hist: chave da versão");
        igual(null, h.chaveDe(99), "placa hist: id inexistente");
        igual("do Davi", h.achar(1).linhas[1], "placa hist: achar versão");
        // placa que já tinha texto antes do histórico: guarda o original
        String k2 = PlacaHistorico.chave("overworld", 5, 70, 5, "BACK");
        var v3 = h.registrar(k2, a, b, "u1", "Davi", 30L);
        igual(2, h.lista(k2).size(), "placa hist: original + edição");
        igual("(original)", h.lista(k2).get(1).nome, "placa hist: original marcado");
        igual("", h.lista(k2).get(1).uuid, "placa hist: original sem autor");
        igual(true, v3.id > v2.id, "placa hist: ids sempre crescem");
        // permissões de restaurar
        igual(null, PlacaHistorico.podeRestaurar(v1, "u1", false), "placa hist: autor restaura a sua");
        igual(true, PlacaHistorico.podeRestaurar(v1, "u2", false) != null, "placa hist: outro jogador não restaura");
        igual(null, PlacaHistorico.podeRestaurar(v1, "u2", true), "placa hist: OP restaura qualquer uma");
        igual(true, PlacaHistorico.podeRestaurar(h.lista(k2).get(1), "u1", false) != null, "placa hist: original só OP");
        igual(null, PlacaHistorico.podeRestaurar(h.lista(k2).get(1), "u1", true), "placa hist: OP restaura original");
        // limite de versões
        String k3 = PlacaHistorico.chave("overworld", 9, 9, 9, "FRONT");
        for (int i = 0; i < PlacaHistorico.MAX_VERSOES + 5; i++) h.registrar(k3, new String[] {"v" + i, "", "", ""}, new String[] {"v" + (i + 1), "", "", ""}, "u1", "Davi", i);
        igual(PlacaHistorico.MAX_VERSOES, h.lista(k3).size(), "placa hist: limite de versões por placa");
        igual("v" + (PlacaHistorico.MAX_VERSOES + 5), h.lista(k3).get(0).linhas[0], "placa hist: fica a mais nova");
        // quebrar a placa esquece os dois lados
        h.registrar(PlacaHistorico.chave("overworld", 1, 64, 2, "BACK"), vazio, a, "u1", "Davi", 40L);
        h.remover("overworld", 1, 64, 2);
        igual(0, h.lista(k).size(), "placa hist: quebrar esquece a frente");
        igual(0, h.lista(PlacaHistorico.chave("overworld", 1, 64, 2, "BACK")).size(), "placa hist: quebrar esquece o verso");
        igual(true, h.lista(k2).size() > 0, "placa hist: outras placas ficam");
        // limite de placas
        var g = new PlacaHistorico();
        for (int i = 0; i < PlacaHistorico.MAX_PLACAS + 10; i++) g.registrar(PlacaHistorico.chave("overworld", i, 0, 0, "FRONT"), vazio, a, "u", "U", i);
        igual(PlacaHistorico.MAX_PLACAS, g.placas.size(), "placa hist: limite de placas");
        igual(0, g.lista(PlacaHistorico.chave("overworld", 0, 0, 0, "FRONT")).size(), "placa hist: sai a mais antiga");
        // disco
        Path tmp = Files.createTempDirectory("melbyplh");
        Path f = tmp.resolve("ph.json");
        h.gravar(f);
        var l = PlacaHistorico.ler(f);
        igual(h.proximoId, l.proximoId, "placa hist: disco guarda o próximo id");
        igual("do Kaue", l.lista(k2).get(0).linhas[1], "placa hist: disco guarda as linhas");
        igual(0, PlacaHistorico.ler(tmp.resolve("x.json")).placas.size(), "placa hist: arquivo ausente");
        Files.writeString(tmp.resolve("p.json"), "{\"placas\":{\"k\":[{\"id\":3}]}}");
        var pj = PlacaHistorico.ler(tmp.resolve("p.json"));
        igual(4, pj.placas.get("k").get(0).linhas.length, "placa hist: linhas faltando viram 4 vazias");
        igual("", pj.placas.get("k").get(0).uuid, "placa hist: uuid faltando vira vazio");
    }

    static void etiquetaBau() throws Exception {
        var d = new EtiquetaBau();
        String k = EtiquetaBau.chave("overworld", 10, 64, 10);
        igual(null, d.definir(k, "overworld", 10, 64, 10, EtiquetaBau.Campo.NOME, "  Minérios   raros ", "u1", "Davi", false, 1L), "bau: etiquetar");
        igual("Minérios raros", d.baus.get(k).nome, "bau: espaços arrumados");
        igual("u1", d.baus.get(k).dono, "bau: primeiro vira dono");
        igual(null, d.definir(k, "overworld", 10, 64, 10, EtiquetaBau.Campo.CATEGORIA, "Minas", "u1", "Davi", false, 2L), "bau: categoria");
        igual(null, d.definir(k, "overworld", 10, 64, 10, EtiquetaBau.Campo.DESCRICAO, "ferro, ouro e diamante", "u1", "Davi", false, 3L), "bau: descrição");
        igual(true, d.definir(k, "overworld", 10, 64, 10, EtiquetaBau.Campo.NOME, "roubado", "u2", "Kaue", false, 4L) != null, "bau: outro jogador não muda");
        igual("Minérios raros", d.baus.get(k).nome, "bau: tentativa não altera");
        igual(null, d.definir(k, "overworld", 10, 64, 10, EtiquetaBau.Campo.NOME, "Minérios", "u2", "Kaue", true, 5L), "bau: OP muda de qualquer um");
        igual("u1", d.baus.get(k).dono, "bau: OP não rouba a posse");
        igual(true, d.definir(k, "overworld", 10, 64, 10, EtiquetaBau.Campo.NOME, "x".repeat(33), "u1", "Davi", false, 6L) != null, "bau: nome longo");
        igual(true, d.definir(k, "overworld", 10, 64, 10, EtiquetaBau.Campo.NOME, "a§cb", "u1", "Davi", false, 6L) != null, "bau: sem §");
        igual(true, d.definir("outro", "overworld", 1, 1, 1, EtiquetaBau.Campo.NOME, "", "u1", "Davi", false, 6L) != null, "bau: apagar o que não existe");
        // esvaziar todos os campos remove a etiqueta
        igual(null, d.definir(k, "overworld", 10, 64, 10, EtiquetaBau.Campo.NOME, "", "u1", "Davi", false, 7L), "bau: apagar nome");
        igual(null, d.definir(k, "overworld", 10, 64, 10, EtiquetaBau.Campo.CATEGORIA, "", "u1", "Davi", false, 8L), "bau: apagar categoria");
        igual(true, d.baus.containsKey(k), "bau: ainda tem descrição");
        igual(null, d.definir(k, "overworld", 10, 64, 10, EtiquetaBau.Campo.DESCRICAO, "", "u1", "Davi", false, 9L), "bau: apagar descrição");
        igual(false, d.baus.containsKey(k), "bau: sem campos some");
        // limpar e posse
        d.definir(k, "overworld", 10, 64, 10, EtiquetaBau.Campo.NOME, "Comida", "u1", "Davi", false, 10L);
        igual(true, d.remover(k, "u2", false) != null, "bau: outro não limpa");
        igual(null, d.remover(k, "u1", false), "bau: dono limpa");
        igual(true, d.remover(k, "u1", false) != null, "bau: limpar de novo");
        // baú duplo: metade canônica
        igual(10, EtiquetaBau.canonico(10, 64, 10, 11, 64, 10)[0], "bau: duplo, menor x");
        igual(10, EtiquetaBau.canonico(11, 64, 10, 10, 64, 10)[0], "bau: duplo, ordem tanto faz");
        igual(9, EtiquetaBau.canonico(5, 64, 10, 5, 64, 9)[2], "bau: duplo no eixo z");
        igual(63, EtiquetaBau.canonico(5, 64, 5, 5, 63, 5)[1], "bau: empilhado, menor y");
        // quebrar metade: etiqueta passa para a outra
        d.definir(EtiquetaBau.chave("overworld", 1, 1, 1), "overworld", 1, 1, 1, EtiquetaBau.Campo.NOME, "Duplo", "u1", "Davi", false, 11L);
        d.mover(EtiquetaBau.chave("overworld", 1, 1, 1), EtiquetaBau.chave("overworld", 2, 1, 1), "overworld", 2, 1, 1);
        igual(false, d.baus.containsKey(EtiquetaBau.chave("overworld", 1, 1, 1)), "bau: saiu do lugar antigo");
        igual(2, d.baus.get(EtiquetaBau.chave("overworld", 2, 1, 1)).x, "bau: coordenada atualizada");
        d.mover("nada", "qualquer", "overworld", 0, 0, 0);
        igual(false, d.baus.containsKey("qualquer"), "bau: mover o que não existe");
        // busca e lista
        d.definir(EtiquetaBau.chave("overworld", 5, 5, 5), "overworld", 5, 5, 5, EtiquetaBau.Campo.NOME, "Ferramentas", "u1", "Davi", false, 12L);
        d.definir(EtiquetaBau.chave("overworld", 5, 5, 5), "overworld", 5, 5, 5, EtiquetaBau.Campo.CATEGORIA, "Oficina", "u1", "Davi", false, 12L);
        d.definir(EtiquetaBau.chave("nether", 6, 6, 6), "nether", 6, 6, 6, EtiquetaBau.Campo.NOME, "Pepitas de ouro", "u2", "Kaue", false, 13L);
        igual(1, d.buscar("FERRAMENTA").size(), "bau: busca sem diferenciar maiúscula");
        igual(1, d.buscar("oficina").size(), "bau: busca na categoria");
        igual(1, d.buscar("pepita").size(), "bau: busca de outro jogador");
        d.definir(EtiquetaBau.chave("overworld", 7, 7, 7), "overworld", 7, 7, 7, EtiquetaBau.Campo.NOME, "Mineração", "u3", "Hugo", false, 13L);
        igual(1, d.buscar("mineracao").size(), "bau: busca sem acento acha com acento");
        igual(1, d.buscar("duplo").size(), "bau: busca acha o duplo");
        igual(0, d.buscar("diamante").size(), "bau: busca sem resultado");
        igual(2, d.doJogador("u1", "").size(), "bau: lista do jogador");
        igual(1, d.doJogador("u1", "oficina").size(), "bau: lista por categoria");
        igual(0, d.doJogador("u1", "nada").size(), "bau: categoria inexistente");
        igual("Ferramentas", d.doJogador("u1", "").get(1).nome, "bau: ordenado por categoria e nome");
        // limite por jogador
        var g = new EtiquetaBau();
        for (int i = 0; i < EtiquetaBau.MAX_POR_JOGADOR; i++) g.definir("k" + i, "o", i, 0, 0, EtiquetaBau.Campo.NOME, "b" + i, "u", "U", false, 1L);
        igual(true, g.definir("extra", "o", 0, 0, 0, EtiquetaBau.Campo.NOME, "b", "u", "U", false, 1L) != null, "bau: limite por jogador");
        igual(null, g.definir("extra", "o", 0, 0, 0, EtiquetaBau.Campo.NOME, "b", "u", "U", true, 1L), "bau: OP não tem limite");
        igual(null, g.definir("k0", "o", 0, 0, 0, EtiquetaBau.Campo.NOME, "novo", "u", "U", false, 1L), "bau: editar no limite");
        // disco
        Path tmp = Files.createTempDirectory("melbybau");
        Path f = tmp.resolve("baus.json");
        d.gravar(f);
        var l = EtiquetaBau.ler(f);
        igual(d.baus.size(), l.baus.size(), "bau: disco guarda");
        igual("Pepitas de ouro", l.baus.get(EtiquetaBau.chave("nether", 6, 6, 6)).nome, "bau: disco guarda o nome");
        igual(0, EtiquetaBau.ler(tmp.resolve("x.json")).baus.size(), "bau: arquivo ausente");
    }

    static void mapArt() throws Exception {
        igual(128, MapArtGeo.lado(0), "mapart: lado escala 0");
        igual(256, MapArtGeo.lado(1), "mapart: lado escala 1");
        igual(2048, MapArtGeo.lado(4), "mapart: lado escala 4");
        // a grade do jogo: o mapa 0,0 em escala 0 vai de -64 a 63 e tem centro 0
        igual(0, MapArtGeo.celula(0, 0), "mapart: origem na célula 0");
        igual(0, MapArtGeo.celula(63.9, 0), "mapart: 63 ainda é a célula 0");
        igual(1, MapArtGeo.celula(64, 0), "mapart: 64 começa a célula 1");
        igual(-1, MapArtGeo.celula(-65, 0), "mapart: -65 é a célula -1");
        igual(0, MapArtGeo.celula(-64, 0), "mapart: -64 é a célula 0");
        igual(-64, MapArtGeo.inicio(0, 0), "mapart: início da célula 0");
        igual(63, MapArtGeo.fim(0, 0), "mapart: fim da célula 0");
        igual(64, MapArtGeo.inicio(1, 0), "mapart: início da célula 1");
        igual(0, MapArtGeo.centro(0, 0), "mapart: centro da célula 0");
        igual(128, MapArtGeo.centro(1, 0), "mapart: centro da célula 1");
        igual(-128, MapArtGeo.centro(-1, 0), "mapart: centro da célula -1");
        igual(64, MapArtGeo.centro(0, 1), "mapart: centro em escala 1 (célula 0 vai de -64 a 191)");
        igual(0, MapArtGeo.celula(191, 1), "mapart: escala 1, 191 na célula 0");
        igual(1, MapArtGeo.celula(192, 1), "mapart: escala 1, 192 na célula 1");
        // todo ponto cai dentro dos limites da sua célula
        boolean ok = true;
        for (int e = 0; e <= 4; e++) for (int x = -3000; x <= 3000; x += 37) {
            int i = MapArtGeo.celula(x, e);
            if (x < MapArtGeo.inicio(i, e) || x > MapArtGeo.fim(i, e)) ok = false;
        }
        igual(true, ok, "mapart: todo ponto cai nos limites da sua célula");
        // validação
        igual(null, MapArtGeo.validar(1, 1, 0), "mapart: 1x1 ok");
        igual(null, MapArtGeo.validar(8, 8, 4), "mapart: 8x8 ok");
        igual(true, MapArtGeo.validar(0, 1, 0) != null, "mapart: largura 0");
        igual(true, MapArtGeo.validar(1, 1, 5) != null, "mapart: escala 5");
        igual(true, MapArtGeo.validar(1, 1, -1) != null, "mapart: escala negativa");
        igual(true, MapArtGeo.validar(17, 1, 0) != null, "mapart: lado grande demais");
        igual(true, MapArtGeo.validar(9, 8, 0) != null, "mapart: mapas demais");
        // contas
        var c = MapArtGeo.calcular(2, 3, 0);
        igual(6, c.mapas, "mapart: mapas");
        igual(256, c.largBlocos, "mapart: largura em blocos");
        igual(384, c.altBlocos, "mapart: altura em blocos");
        igual(6L * 16384, c.pixels, "mapart: pixels");
        igual(256L * 384, c.areaBlocos, "mapart: área");
        igual(6L * 16384, c.blocosPlano, "mapart: blocos sem relevo");
        igual(2L * 128 * (3 * 128 + 1), c.blocosRelevo, "mapart: blocos com relevo");
        igual(true, c.blocosRelevo > c.blocosPlano, "mapart: relevo pede mais");
        var c2 = MapArtGeo.calcular(1, 1, 2);
        igual(512, c2.largBlocos, "mapart: escala 2 cobre 512");
        igual(16384L, c2.pixels, "mapart: pixels não mudam com a escala");
        igual(512L * 512, c2.areaBlocos, "mapart: área cresce com a escala");
        // projetos
        var d = new MapArtGeo();
        var p = new MapArtGeo.Projeto();
        p.nome = "Mural";
        p.dim = "overworld";
        p.escala = 0;
        p.gx = 2;
        p.gz = -1;
        p.largM = 3;
        p.altM = 2;
        igual(null, d.salvar("u1", p, 7L), "mapart: salvar projeto");
        igual(192, p.x0(), "mapart: x0 do projeto");
        igual(-192, p.z0(), "mapart: z0 do projeto");
        igual(575, p.x1(), "mapart: x1 cobre 3 mapas");
        igual(63, p.z1(), "mapart: z1 cobre 2 mapas");
        igual(7L, d.obter("u1", "mural").criadoEm, "mapart: obter sem diferenciar maiúscula");
        igual(null, d.obter("u2", "mural"), "mapart: outro jogador não vê");
        var ruim = new MapArtGeo.Projeto();
        ruim.nome = "a b";
        ruim.largM = 1;
        ruim.altM = 1;
        igual(true, d.salvar("u1", ruim, 1L) != null, "mapart: nome ruim");
        var grande = new MapArtGeo.Projeto();
        grande.nome = "Grande";
        grande.largM = 20;
        grande.altM = 1;
        igual(true, d.salvar("u1", grande, 1L) != null, "mapart: tamanho ruim");
        for (int i = 0; i < MapArtGeo.MAX_PROJETOS - 1; i++) {
            var q = new MapArtGeo.Projeto();
            q.nome = "p" + i;
            q.largM = 1;
            q.altM = 1;
            d.salvar("u1", q, 1L);
        }
        var extra = new MapArtGeo.Projeto();
        extra.nome = "extra";
        extra.largM = 1;
        extra.altM = 1;
        igual(true, d.salvar("u1", extra, 1L) != null, "mapart: limite de projetos");
        Path tmp = Files.createTempDirectory("melbymap");
        Path f = tmp.resolve("mapart.json");
        d.gravar(f);
        var l = MapArtGeo.ler(f);
        igual(575, l.obter("u1", "Mural").x1(), "mapart: disco guarda o projeto");
        igual(true, l.apagar("u1", "mural"), "mapart: apagar");
        igual(false, l.apagar("u1", "mural"), "mapart: apagar de novo");
        igual(0, MapArtGeo.ler(tmp.resolve("x.json")).projetos.size(), "mapart: arquivo ausente");
    }

    static void banner() throws Exception {
        igual(16, BannerReceita.CORES.size(), "banner: 16 cores");
        igual("Azul-claro", BannerReceita.CORES.get("light_blue"), "banner: nome da cor");
        igual(true, BannerReceita.PADROES.size() >= 40, "banner: padrões conhecidos");
        for (String id : BannerReceita.ITEM_DO_PADRAO.keySet()) igual(true, BannerReceita.PADROES.containsKey(id), "banner: item de padrão existe como padrão: " + id);
        var r = new BannerReceita("T", "white", new java.util.ArrayList<>());
        igual(null, r.validar(), "banner: sem camadas é válido (só a base)");
        r.camadas.add(new BannerReceita.Camada("creeper", "black"));
        r.camadas.add(new BannerReceita.Camada("border", "red"));
        r.camadas.add(new BannerReceita.Camada("creeper", "green"));
        igual(null, r.validar(), "banner: receita válida");
        igual(3, r.corantes().size(), "banner: três cores diferentes");
        igual(1, r.itensDePadrao().size(), "banner: item de padrão sem repetir");
        igual("creeper", r.itensDePadrao().get(0), "banner: item de padrão certo");
        igual(4, r.passos().size(), "banner: passos = base + camadas");
        igual(true, r.passos().get(1).contains("Creeper") && r.passos().get(1).contains("preto"), "banner: passo cita padrão e cor");
        igual(true, r.passos().get(1).contains("cabeça de creeper"), "banner: passo cita o item");
        igual(true, r.resumoMateriais().contains("3 corante") || r.resumoMateriais().contains("1 corante"), "banner: materiais citam corantes");
        igual(true, new BannerReceita("x", "azul", new java.util.ArrayList<>()).validar() != null, "banner: base inválida");
        var ruim = new BannerReceita("x", "red", new java.util.ArrayList<>(java.util.List.of(new BannerReceita.Camada("inexistente", "red"))));
        igual(true, ruim.validar() != null, "banner: padrão inválido");
        var ruim2 = new BannerReceita("x", "red", new java.util.ArrayList<>(java.util.List.of(new BannerReceita.Camada("border", "roxo"))));
        igual(true, ruim2.validar() != null, "banner: cor inválida");
        var sete = new BannerReceita("x", "red", new java.util.ArrayList<>());
        for (int i = 0; i < 7; i++) sete.camadas.add(new BannerReceita.Camada("border", "black"));
        igual(true, sete.validar() != null, "banner: mais de 6 camadas");
        sete.camadas.remove(0);
        igual(null, sete.validar(), "banner: 6 camadas");
        igual(6, sete.corantes().get("black"), "banner: corantes somam");
        // catálogo embutido
        String json = Files.readString(Path.of("src/main/resources/melbybet/banners.json"));
        var cat = BannerReceita.lerCatalogo(json);
        igual(true, cat.size() >= 25, "banner: catálogo tem receitas (" + cat.size() + ")");
        int quantas = new com.google.gson.Gson().fromJson(json, com.google.gson.JsonArray.class).size();
        igual(quantas, cat.size(), "banner: nenhuma receita do catálogo é inválida");
        var nomes = new java.util.HashSet<String>();
        boolean repetido = false;
        for (var b : cat) if (!nomes.add(b.nome)) repetido = true;
        igual(false, repetido, "banner: nomes do catálogo não repetem");
        igual(0, BannerReceita.lerCatalogo("isto não é json").size(), "banner: JSON ruim = catálogo vazio");
        igual(0, BannerReceita.lerCatalogo("null").size(), "banner: JSON nulo");
        igual(1, BannerReceita.lerCatalogo("[{\"nome\":\"a\",\"base\":\"red\",\"camadas\":[[\"border\",\"black\"]]},{\"nome\":\"b\",\"base\":\"x\",\"camadas\":[]}]").size(), "banner: descarta a inválida");
        // favoritos
        var f = new BannerReceita.Favoritos();
        igual(null, f.salvar("u1", "Meu", r), "banner: favoritar");
        igual("Meu", f.de("u1").get(0).nome, "banner: nome guardado");
        igual(true, f.salvar("u1", "a b", r) != null, "banner: nome ruim");
        igual(true, f.salvar("u1", "Vazio", new BannerReceita("x", "red", new java.util.ArrayList<>())) != null, "banner: sem camadas não favorita");
        r.camadas.remove(0);
        igual(null, f.salvar("u1", "meu", r), "banner: mesmo nome substitui");
        igual(1, f.de("u1").size(), "banner: não duplica");
        igual(2, f.de("u1").get(0).camadas.size(), "banner: substituiu o conteúdo");
        igual(2, r.camadas.size(), "banner: o favorito é uma cópia");
        for (int i = 0; i < BannerReceita.MAX_FAVORITOS; i++) f.salvar("u2", "f" + i, r);
        igual(true, f.salvar("u2", "extra", r) != null, "banner: limite de favoritos");
        igual(true, f.apagar("u2", "F0"), "banner: apagar sem diferenciar maiúscula");
        igual(false, f.apagar("u2", "f0"), "banner: apagar de novo");
        Path tmp = Files.createTempDirectory("melbybn");
        Path arq = tmp.resolve("f.json");
        f.gravar(arq);
        var l = BannerReceita.Favoritos.ler(arq);
        igual(2, l.de("u1").get(0).camadas.size(), "banner: disco guarda favoritos");
        igual("border", l.de("u1").get(0).camadas.get(0).padrao, "banner: disco guarda camadas");
        igual(0, BannerReceita.Favoritos.ler(tmp.resolve("x.json")).jogadores.size(), "banner: arquivo ausente");
    }

    static void armorStand() throws Exception {
        // donos
        var d = new Donos();
        igual(true, d.reivindicar("stand:a", "u1", "Davi", 1L), "dono: reivindicar");
        igual(false, d.reivindicar("stand:a", "u2", "Kaue", 2L), "dono: já tem dono");
        igual("Davi", d.de("stand:a").nome, "dono: primeiro fica");
        igual(null, d.pode("stand:a", "u1", false), "dono: o dono mexe");
        igual(true, d.pode("stand:a", "u2", false) != null, "dono: outro não mexe");
        igual(null, d.pode("stand:a", "u2", true), "dono: OP mexe");
        igual(true, d.pode("stand:b", "u1", false) != null, "dono: sem dono, jogador não mexe");
        igual(null, d.pode("stand:b", "u1", true), "dono: sem dono, OP mexe");
        igual(true, d.transferir("stand:a", "u2", false, "u2", "Kaue", 3L) != null, "dono: outro não transfere");
        igual(null, d.transferir("stand:a", "u1", false, "u2", "Kaue", 3L), "dono: dono transfere");
        igual("Kaue", d.de("stand:a").nome, "dono: novo dono");
        igual(null, d.transferir("stand:a", "u9", true, "u3", "Hugo", 4L), "dono: OP transfere");
        igual(true, d.transferir("stand:zz", "u1", false, "u1", "Davi", 4L) != null, "dono: sem dono, jogador não transfere");
        igual(null, d.transferir("stand:zz", "u1", true, "u1", "Davi", 4L), "dono: sem dono, OP define");
        igual(1, d.contar("u3"), "dono: contar");
        d.remover("stand:a");
        igual(null, d.de("stand:a"), "dono: remover");
        var muitos = new Donos();
        for (int i = 0; i < Donos.MAX + 5; i++) muitos.reivindicar("k" + i, "u", "U", i);
        igual(Donos.MAX, muitos.donos.size(), "dono: limite total");
        igual(null, muitos.de("k0"), "dono: sai o mais antigo");
        Path tmp = Files.createTempDirectory("melbydn");
        d.gravar(tmp.resolve("d.json"));
        igual("Davi", Donos.ler(tmp.resolve("d.json")).de("stand:zz").nome, "dono: disco");
        igual(0, Donos.ler(tmp.resolve("x.json")).donos.size(), "dono: arquivo ausente");
        // ângulos
        igual(10f, PosePreset.girar(0, 10), "pose: girar");
        igual(-170f, PosePreset.girar(170, 20), "pose: dá a volta no 180");
        igual(170f, PosePreset.girar(-170, -20), "pose: dá a volta no -180");
        igual(0f, PosePreset.girar(350, 10), "pose: 360 vira 0");
        igual(180f, PosePreset.girar(179, 1), "pose: 180 fica 180");
        igual(5, PosePreset.proximoPasso(1), "pose: passo 1 -> 5");
        igual(15, PosePreset.proximoPasso(5), "pose: passo 5 -> 15");
        igual(45, PosePreset.proximoPasso(15), "pose: passo 15 -> 45");
        igual(1, PosePreset.proximoPasso(45), "pose: passo 45 -> 1");
        igual(1, PosePreset.proximoPasso(7), "pose: passo desconhecido volta ao 1");
        // poses
        var pp = new PosePreset();
        var pose = new PosePreset.Pose();
        pose.angulos[0] = new float[] {10, 20, 30};
        pose.angulos[2] = new float[] {-90, 0, 15};
        pose.angulos[3] = new float[] {0, 45, -20};
        pose.bracos = true;
        pose.base = false;
        igual(null, pp.salvar("u1", "Saudacao", pose, 5L), "pose: salvar");
        igual(true, pp.salvar("u1", "a b", pose, 5L) != null, "pose: nome ruim");
        pose.angulos[0][0] = 99;
        igual(10f, pp.obter("u1", "saudacao").angulos[0][0], "pose: o preset é cópia");
        igual(true, pp.obter("u1", "saudacao").bracos, "pose: guarda braços");
        igual(false, pp.obter("u1", "saudacao").base, "pose: guarda base");
        igual(null, pp.obter("u2", "saudacao"), "pose: outro jogador não vê");
        igual(null, pp.salvar("u1", "SAUDACAO", pose, 6L), "pose: mesmo nome substitui");
        igual(1, pp.de("u1").size(), "pose: sem duplicar");
        igual(99f, pp.obter("u1", "saudacao").angulos[0][0], "pose: substituiu");
        for (int i = 0; i < PosePreset.MAX_PRESETS; i++) pp.salvar("u2", "p" + i, pose, 1L);
        igual(true, pp.salvar("u2", "extra", pose, 1L) != null, "pose: limite");
        igual(true, pp.apagar("u2", "P0"), "pose: apagar");
        igual(false, pp.apagar("u2", "p0"), "pose: apagar de novo");
        // espelho: esquerda <-> direita e eixos Y e Z invertidos
        var e = PosePreset.espelhar(pose);
        igual(0f, e.angulos[2][0], "pose: espelho, x do braço esquerdo");
        igual(-45f, e.angulos[2][1], "pose: espelho, y do braço esquerdo vem do direito, invertido");
        igual(20f, e.angulos[2][2], "pose: espelho, z do braço esquerdo");
        igual(-90f, e.angulos[3][0], "pose: espelho, x do braço direito vem do esquerdo");
        igual(0f, e.angulos[3][1], "pose: espelho, y zero continua zero");
        igual(-15f, e.angulos[3][2], "pose: espelho, z do braço direito");
        igual(-20f, e.angulos[0][1], "pose: espelho da cabeça inverte y");
        igual(-30f, e.angulos[0][2], "pose: espelho da cabeça inverte z");
        var ee = PosePreset.espelhar(e);
        boolean igualOriginal = true;
        for (int i = 0; i < PosePreset.PARTES; i++) for (int k = 0; k < 3; k++) if (Math.abs(ee.angulos[i][k] - pose.angulos[i][k]) > 0.001f) igualOriginal = false;
        igual(true, igualOriginal, "pose: espelhar duas vezes volta ao original");
        Path f = tmp.resolve("poses.json");
        pp.gravar(f);
        var l = PosePreset.ler(f);
        igual(true, l.obter("u1", "saudacao").bracos, "pose: disco guarda");
        igual(99f, l.obter("u1", "saudacao").angulos[0][0], "pose: disco guarda ângulos");
        igual(0, PosePreset.ler(tmp.resolve("x.json")).jogadores.size(), "pose: arquivo ausente");
    }

    static BufferedImage cor(int w, int h, int rgb) {
        var img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) img.setRGB(x, y, 0xFF000000 | rgb);
        return img;
    }

    static void fotoMoldura() throws Exception {
        // URLs
        igual(null, UrlSegura.validar("https://i.imgur.com/abc.png"), "url: https normal");
        igual(null, UrlSegura.validar("https://cdn.discordapp.com/attachments/1/2/foto.jpg?ex=1&hm=2"), "url: com parâmetros");
        igual(null, UrlSegura.validar("https://exemplo.com:443/a.png"), "url: porta 443");
        igual(true, UrlSegura.validar("http://i.imgur.com/abc.png") != null, "url: http recusado");
        igual(true, UrlSegura.validar("ftp://x.com/a.png") != null, "url: ftp recusado");
        igual(true, UrlSegura.validar("file:///etc/passwd") != null, "url: file recusado");
        igual(true, UrlSegura.validar("https://usuario:senha@x.com/a.png") != null, "url: usuário e senha");
        igual(true, UrlSegura.validar("https://x.com:8443/a.png") != null, "url: porta estranha");
        igual(true, UrlSegura.validar("https://localhost/a.png") != null, "url: localhost");
        igual(true, UrlSegura.validar("https://foo.localhost/a.png") != null, "url: subdomínio de localhost");
        igual(true, UrlSegura.validar("https://servidor.local/a.png") != null, "url: .local");
        igual(true, UrlSegura.validar("https://api.internal/a.png") != null, "url: .internal");
        igual(true, UrlSegura.validar("https://127.0.0.1/a.png") != null, "url: loopback");
        igual(true, UrlSegura.validar("https://10.0.0.5/a.png") != null, "url: rede 10");
        igual(true, UrlSegura.validar("https://192.168.1.1/a.png") != null, "url: rede 192.168");
        igual(true, UrlSegura.validar("https://172.16.0.1/a.png") != null, "url: rede 172.16");
        igual(true, UrlSegura.validar("https://169.254.169.254/latest/meta-data") != null, "url: metadados de nuvem");
        igual(true, UrlSegura.validar("https://100.64.0.1/a.png") != null, "url: CGNAT");
        igual(true, UrlSegura.validar("https://0.0.0.0/a.png") != null, "url: 0.0.0.0");
        igual(true, UrlSegura.validar("https://[::1]/a.png") != null, "url: IPv6 loopback");
        igual(true, UrlSegura.validar("https://[fd00::1]/a.png") != null, "url: IPv6 único local");
        igual(true, UrlSegura.validar("https://[fe80::1]/a.png") != null, "url: IPv6 link-local");
        igual(null, UrlSegura.validar("https://8.8.8.8/a.png"), "url: IP público passa na checagem de texto");
        igual(true, UrlSegura.validar("https://intranet/a.png") != null, "url: nome sem ponto");
        igual(true, UrlSegura.validar("") != null, "url: vazia");
        igual(true, UrlSegura.validar(null) != null, "url: nula");
        igual(true, UrlSegura.validar("https://x.com/" + "a".repeat(600)) != null, "url: grande demais");
        igual(true, UrlSegura.validar("não é url") != null, "url: texto solto");
        igual(true, UrlSegura.ipPermitido(java.net.InetAddress.getByAddress(new byte[] {8, 8, 8, 8})), "ip: público");
        igual(false, UrlSegura.ipPermitido(java.net.InetAddress.getByAddress(new byte[] {10, 1, 2, 3})), "ip: privado");
        igual(false, UrlSegura.ipPermitido(java.net.InetAddress.getByAddress(new byte[] {(byte) 192, (byte) 168, 0, 1})), "ip: 192.168");
        igual(false, UrlSegura.ipPermitido(java.net.InetAddress.getByAddress(new byte[] {(byte) 172, 31, 0, 1})), "ip: 172.31");
        igual(true, UrlSegura.ipPermitido(java.net.InetAddress.getByAddress(new byte[] {(byte) 172, 32, 0, 1})), "ip: 172.32 é público");
        igual(false, UrlSegura.ipPermitido(java.net.InetAddress.getByAddress(new byte[] {100, 100, 0, 1})), "ip: CGNAT");
        igual(true, UrlSegura.ipPermitido(java.net.InetAddress.getByAddress(new byte[] {100, (byte) 128, 0, 1})), "ip: fora do CGNAT");
        igual(false, UrlSegura.ipPermitido(java.net.InetAddress.getByAddress(new byte[] {(byte) 224, 0, 0, 1})), "ip: multicast");
        igual(false, UrlSegura.ipPermitido(java.net.InetAddress.getByName("::1")), "ip: ::1");
        igual(false, UrlSegura.ipPermitido(java.net.InetAddress.getByName("fc00::1")), "ip: fc00::");
        igual(true, UrlSegura.ipPermitido(java.net.InetAddress.getByName("2606:4700:4700::1111")), "ip: IPv6 público");
        igual("https://cdn.x.com/b/c.png", UrlSegura.redirecionar("https://cdn.x.com/a/x.png", "/b/c.png"), "url: redireção relativa");
        igual("https://outro.com/z.png", UrlSegura.redirecionar("https://cdn.x.com/a", "https://outro.com/z.png"), "url: redireção absoluta");
        igual(null, UrlSegura.redirecionar("https://cdn.x.com/a", "http://outro.com/z.png"), "url: redireção para http");
        igual(null, UrlSegura.redirecionar("https://cdn.x.com/a", "https://127.0.0.1/z.png"), "url: redireção para a rede interna");
        // paleta
        igual(248, PaletaMapa.paleta(PaletaMapa.BASE_RGB).length, "paleta: 248 posições");
        igual(62, PaletaMapa.BASE_RGB.length, "paleta: 62 cores-base");
        igual(0x7FB238, PaletaMapa.rgb(6, PaletaMapa.BASE_RGB), "paleta: grama, tom normal");
        igual(0x7FB238 * 0 + (0x7F * 180 / 255 << 16 | 0xB2 * 180 / 255 << 8 | 0x38 * 180 / 255), PaletaMapa.rgb(4, PaletaMapa.BASE_RGB), "paleta: grama, tom escuro");
        igual(0xFFFFFF, PaletaMapa.rgb(8 * 4 + 2, PaletaMapa.BASE_RGB), "paleta: neve branca");
        int[] verde = {0xFF000000 | 0x7FB238};
        igual(6, PaletaMapa.converter(verde, 1, 1, false)[0] & 0xFF, "paleta: cor exata da grama vira o id 6");
        igual(8 * 4 + 2, PaletaMapa.converter(new int[] {0xFFFFFFFF}, 1, 1, false)[0] & 0xFF, "paleta: branco vira neve");
        int preto = PaletaMapa.converter(new int[] {0xFF000000}, 1, 1, false)[0] & 0xFF;
        igual(true, preto >= 4 && PaletaMapa.rgb(preto, PaletaMapa.BASE_RGB) < 0x202020, "paleta: preto vira algo bem escuro");
        igual(0, PaletaMapa.converter(new int[] {0x00FFFFFF}, 1, 1, false)[0] & 0xFF, "paleta: transparente vira 0");
        igual(0, PaletaMapa.converter(new int[] {0x7FFFFFFF}, 1, 1, true)[0] & 0xFF, "paleta: quase transparente vira 0");
        boolean todosValidos = true;
        int[] varios = new int[64 * 64];
        for (int i = 0; i < varios.length; i++) varios[i] = 0xFF000000 | (i * 997 & 0xFFFFFF);
        for (boolean dif : new boolean[] {false, true}) for (byte b : PaletaMapa.converter(varios, 64, 64, dif)) if ((b & 0xFF) < 4 || (b & 0xFF) > 247) todosValidos = false;
        igual(true, todosValidos, "paleta: todo id gerado é uma cor de mapa válida");
        // a difusão mistura duas cores perto de um meio-termo
        int[] cinza = new int[32 * 32];
        java.util.Arrays.fill(cinza, 0xFF000000 | 0x808080);
        var dif = PaletaMapa.converter(cinza, 32, 32, true);
        var distintos = new java.util.HashSet<Integer>();
        for (byte b : dif) distintos.add(b & 0xFF);
        var seco = PaletaMapa.converter(cinza, 32, 32, false);
        var distintos2 = new java.util.HashSet<Integer>();
        for (byte b : seco) distintos2.add(b & 0xFF);
        igual(1, distintos2.size(), "paleta: sem difusão, cor lisa dá um id só");
        igual(true, distintos.size() >= 1, "paleta: com difusão funciona em cor lisa");
        // imagem -> mapas
        var vermelho = cor(100, 100, 0xFF0000);
        var mapas = ImagemMapa.preparar(vermelho, 2, 3, ImagemMapa.ESTICAR, false);
        igual(6, mapas.length, "imagem: 2x3 dá 6 mapas");
        igual(16384, mapas[0].length, "imagem: cada mapa tem 128x128");
        int idv = PaletaMapa.converter(new int[] {0xFFFF0000}, 1, 1, false)[0] & 0xFF;
        boolean tudoVermelho = true;
        for (byte[] m : mapas) for (byte b : m) if ((b & 0xFF) != idv) tudoVermelho = false;
        igual(true, tudoVermelho, "imagem: cor lisa dá o mesmo id em todos os mapas");
        // ordem: metade esquerda azul, metade direita vermelha, 2 mapas lado a lado
        var duas = new BufferedImage(256, 128, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 128; y++) for (int x = 0; x < 256; x++) duas.setRGB(x, y, x < 128 ? 0xFF0000FF : 0xFFFF0000);
        var m2 = ImagemMapa.preparar(duas, 2, 1, ImagemMapa.ESTICAR, false);
        int idAzul = PaletaMapa.converter(new int[] {0xFF0000FF}, 1, 1, false)[0] & 0xFF;
        igual(idAzul, m2[0][5000] & 0xFF, "imagem: mapa da esquerda é a parte esquerda");
        igual(idv, m2[1][5000] & 0xFF, "imagem: mapa da direita é a parte direita");
        // cima e baixo
        var cb = new BufferedImage(128, 256, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 256; y++) for (int x = 0; x < 128; x++) cb.setRGB(x, y, y < 128 ? 0xFF0000FF : 0xFFFF0000);
        var m3 = ImagemMapa.preparar(cb, 1, 2, ImagemMapa.ESTICAR, false);
        igual(idAzul, m3[0][100] & 0xFF, "imagem: o primeiro mapa é o de cima");
        igual(idv, m3[1][100] & 0xFF, "imagem: o segundo mapa é o de baixo");
        // modos: imagem larga num quadrado
        var larga = new BufferedImage(400, 100, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 100; y++) for (int x = 0; x < 400; x++) larga.setRGB(x, y, x < 150 ? 0xFF0000FF : x < 250 ? 0xFF00FF00 : 0xFFFF0000);
        var cortada = ImagemMapa.preparar(larga, 1, 1, ImagemMapa.CORTAR, false);
        int idVerde = PaletaMapa.converter(new int[] {0xFF00FF00}, 1, 1, false)[0] & 0xFF;
        igual(idVerde, cortada[0][64 * 128 + 10] & 0xFF, "imagem: cortar pega o centro (esquerda do meio)");
        igual(idVerde, cortada[0][64 * 128 + 117] & 0xFF, "imagem: cortar pega o centro (direita do meio)");
        var ajustada = ImagemMapa.preparar(larga, 1, 1, ImagemMapa.AJUSTAR, false);
        int idPreto = PaletaMapa.converter(new int[] {0xFF000000}, 1, 1, false)[0] & 0xFF;
        igual(idPreto, ajustada[0][2 * 128 + 64] & 0xFF, "imagem: ajustar deixa faixa preta em cima");
        igual(idPreto, ajustada[0][125 * 128 + 64] & 0xFF, "imagem: ajustar deixa faixa preta embaixo");
        igual(true, (ajustada[0][64 * 128 + 5] & 0xFF) != idPreto, "imagem: ajustar mostra a imagem no meio");
        // transparência: o buraco vira cor 0
        var furo = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) furo.setRGB(x, y, x < 64 ? 0x00000000 : 0xFF00FF00);
        var mf = ImagemMapa.preparar(furo, 1, 1, ImagemMapa.ESTICAR, false);
        igual(0, mf[0][10] & 0xFF, "imagem: parte transparente vira sem cor");
        igual(idVerde, mf[0][100] & 0xFF, "imagem: parte opaca fica");
        // decodificação
        var buf = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(cor(30, 20, 0x336699), "png", buf);
        var lida = ImagemMapa.ler(buf.toByteArray());
        igual(30, lida.getWidth(), "ler: largura do PNG");
        igual(20, lida.getHeight(), "ler: altura do PNG");
        var jpg = new java.io.ByteArrayOutputStream();
        var rgb = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        javax.imageio.ImageIO.write(rgb, "jpg", jpg);
        igual(40, ImagemMapa.ler(jpg.toByteArray()).getWidth(), "ler: JPG");
        boolean erro1 = false, erro2 = false, erro3 = false;
        try { ImagemMapa.ler("isto não é imagem".getBytes()); } catch (IllegalArgumentException e) { erro1 = true; }
        try { ImagemMapa.ler(new byte[0]); } catch (IllegalArgumentException e) { erro2 = true; }
        var enorme = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new BufferedImage(6000, 6000, BufferedImage.TYPE_BYTE_BINARY), "png", enorme);
        try { ImagemMapa.ler(enorme.toByteArray()); } catch (IllegalArgumentException e) { erro3 = e.getMessage().contains("grande demais"); }
        igual(true, erro1, "ler: lixo é recusado");
        igual(true, erro2, "ler: vazio é recusado");
        igual(true, erro3, "ler: 36 milhões de pixels é recusado antes de abrir");
        // tamanho e geometria
        igual(3, FotoDados.tamanho("3x2")[0], "foto: tamanho 3x2");
        igual(2, FotoDados.tamanho("3X2")[1], "foto: maiúscula");
        igual(6, FotoDados.tamanho("6x6")[0], "foto: 6x6");
        igual(null, FotoDados.tamanho("7x1"), "foto: 7 é demais");
        igual(null, FotoDados.tamanho("0x3"), "foto: zero");
        igual(null, FotoDados.tamanho("3"), "foto: sem x");
        igual(null, FotoDados.tamanho("ax2"), "foto: letra");
        igual(null, FotoDados.tamanho(null), "foto: nulo");
        igual(-1, FotoDados.direita(0, -1)[0], "foto: parede para o norte, a direita de quem olha é o oeste");
        igual(1, FotoDados.direita(0, 1)[0], "foto: parede para o sul, a direita é o leste");
        igual(1, FotoDados.direita(1, 0)[1] * -1, "foto: parede para o leste, a direita é o norte (z-)");
        igual(1, FotoDados.direita(-1, 0)[1], "foto: parede para o oeste, a direita é o sul (z+)");
        var c0 = FotoDados.celula(10, 64, 20, 0, -1, 3, 0, 0);
        igual(66, c0[1], "foto: linha de cima fica 2 acima da base");
        igual(10, c0[0], "foto: coluna 0 no x da base");
        var c1 = FotoDados.celula(10, 64, 20, 0, -1, 3, 2, 2);
        igual(64, c1[1], "foto: linha de baixo na base");
        igual(8, c1[0], "foto: coluna 2 vai para o oeste");
        igual(20, c1[2], "foto: z não muda na parede norte");
        // murais
        var d = new FotoDados();
        var g = new FotoDados.Grupo();
        g.dono = "u1";
        g.donoNome = "Davi";
        g.larg = 3;
        g.alt = 2;
        g.mapas = new java.util.ArrayList<>(java.util.List.of(1, 2, 3, 4, 5, 6));
        g.molduras = new java.util.ArrayList<>(java.util.List.of("a", "b"));
        d.adicionar(g, 9L);
        igual(1, g.id, "foto: primeiro id");
        igual(6, d.totalMapas(), "foto: total de mapas");
        igual(g, d.acharPorMoldura("b"), "foto: achar pela moldura");
        igual(null, d.acharPorMoldura("z"), "foto: moldura desconhecida");
        igual(g, d.achar(1), "foto: achar por id");
        igual(null, FotoDados.pode(g, "u1", false), "foto: dono mexe");
        igual(true, FotoDados.pode(g, "u2", false) != null, "foto: outro não mexe");
        igual(null, FotoDados.pode(g, "u2", true), "foto: OP mexe");
        for (int i = 1; i < FotoDados.MAX_POR_JOGADOR; i++) {
            var x = new FotoDados.Grupo();
            x.dono = "u1";
            d.adicionar(x, 1L);
        }
        igual(true, d.podeCriar("u1", false, 4) != null, "foto: limite por jogador");
        igual(null, d.podeCriar("u1", true, 4), "foto: OP sem limite por jogador");
        igual(null, d.podeCriar("u2", false, 4), "foto: outro jogador pode");
        igual(true, d.podeCriar("u2", false, FotoDados.MAX_MAPAS_TOTAL) != null, "foto: limite de mapas do servidor");
        igual(true, d.remover(1), "foto: remover");
        igual(false, d.remover(1), "foto: remover de novo");
        d.adicionar(g, 9L);
        Path tmp = Files.createTempDirectory("melbyft");
        d.gravar(tmp.resolve("f.json"));
        var l = FotoDados.ler(tmp.resolve("f.json"));
        igual(d.grupos.size(), l.grupos.size(), "foto: disco guarda os murais");
        igual(l.proximoId, d.proximoId, "foto: disco guarda o próximo id");
        igual(0, FotoDados.ler(tmp.resolve("x.json")).grupos.size(), "foto: arquivo ausente");
    }
}
