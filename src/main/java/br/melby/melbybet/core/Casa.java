package br.melby.melbybet.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estado da casa de apostas e todas as regras. Sem nada do Minecraft aqui:
 * os diamantes são tirados e entregues pela camada do jogo; esta classe só faz as contas.
 */
public class Casa {
    public static class Config {
        /** Parte do pote que a casa fica, em %. */
        /** Waypoints do cliente liberados neste servidor (o OP pode desligar). */
        public boolean waypoints = true;
        public int rakePct = 5;
        public int apostaMin = 1;
        /** Máximo que cada jogador pode pôr num mesmo bolão. */
        public int apostaMax = 64;
        /** A partir de quanto uma aposta vira aviso no chat. */
        public int avisoGrande = 32;
        /** Taxa de quem desiste da aposta (cash out), em %. */
        public int cashoutTaxaPct = 10;
        /** Os nicks da Gangue; viram as opções dos bolões de jogador. */
        public List<String> roster = new ArrayList<>(List.of("d4rk_4g10t4", "vitorcs201012", "saya_neku", "has_edu", "patricksmirnoff", "davigamer58GB"));
        /** Cor do nick no chat: nick (minúsculo) -> degradê. */
        public Map<String, CorNick> cores = new LinkedHashMap<>();
        /** Arena de vidro dos duelos (medidas EXTERNAS em blocos, material, contagem e limpeza). */
        public boolean arenaAtiva = true;
        public int arenaLarg = 15;
        public int arenaComp = 15;
        public int arenaAlt = 10;
        public String arenaMaterial = "minecraft:glass";
        public int arenaContagemSeg = 5;
        /** Distância mínima entre duas arenas ao mesmo tempo, em blocos. */
        public int arenaDistMin = 32;
        /** Segundos entre o fim do duelo e a volta dos jogadores / limpeza da arena. */
        public int arenaLimpezaSeg = 3;
        /** Por quantos minutos depois do duelo o mod registra o que o vencedor fez com o prêmio (0 = não registra). */
        public int rastroMin = 30;
        /** Distância do chat local, em blocos (0 = sem limite). */
        public int chatLocalRaio = 100;
        /** Segundos depois da morte em que só o dono (ou um OP) pega os itens do corpo; depois qualquer um pega (0 = sem proteção). */
        public int corpoProtecaoSeg = 30;
        /** Quanto o banco invisível do /sit fica abaixo dos pés, em blocos (maior = senta mais baixo). */
        public double bancoAltura = 1.6;
        /** De quantos em quantos minutos o servidor apaga os itens do chão (0 = desligado). */
        public int limpezaMinutos = 0;
        /** Quantos segundos antes da limpeza o servidor avisa (depois vem a contagem final). */
        public int limpezaAvisoSeg = 30;
        /** Itens que acabaram de cair (há menos de tantos segundos) não são apagados. */
        public int limpezaIdadeMinSeg = 30;
        /** Não apagar itens valiosos (diamante, netherite, elytra, totem, estrela do Nether), nem encantados ou com nome. */
        public boolean limpezaProtege = true;
        /** Avisar no chat (e na tela) antes de limpar. */
        public boolean limpezaAviso = true;
        /** Mensagem de cada jogador quando ele mata outro: usa {matador} e {vitima}. Chave: nick em minúsculas. */
        public Map<String, String> msgMatar = new LinkedHashMap<>(); // formato antigo (uma só), migrado para msgMatarLista
        /** Mensagens de kill: "matador" = padrão; "matador>vitima" = só quando mata aquela pessoa. Chaves em minúsculas. */
        public Map<String, List<String>> msgMatarLista = new LinkedHashMap<>();
        /** Jogadores que desligaram o aviso de durabilidade (nick em minúsculas). */
        public java.util.Set<String> semAvisoDurabilidade = new java.util.LinkedHashSet<>();
        /** Depois de quantas horas o corpo some e solta os itens no chão (0 = nunca). */
        public int corpoHoras = 0;
        /** Quantos backups diários do casa.json guardar. */
        public int backupDias = 7;
        /** Endereço do painel de cores (a pasta docs do repositório, via GitHub Pages). Vazio = não mostra o link. */
        public String painelCoresUrl = "https://d4rk0l4.github.io/melbybet/";
        /** Horários (HH:mm) em que o servidor reinicia: o mod avisa 30, 15, 10, 5, 3, 2 e 1 minuto antes, e a contagem final. */
        public List<String> reinicios = new ArrayList<>();
        /** Fuso dos horários de reinício. */
        public String fuso = "America/Sao_Paulo";
        /** Títulos dados por OP: nick (minúsculo) -> título. */
        public Map<String, Titulo> titulos = new LinkedHashMap<>();
        public List<String> msgEntrar = new ArrayList<>(List.of(
            "{nick} chegou na Gangue!", "Olha quem apareceu: {nick}", "{nick} entrou para perder diamantes",
            "{nick} voltou do além", "A casa agradece a visita de {nick}"));
        public List<String> msgSair = new ArrayList<>(List.of(
            "{nick} foi buscar pão", "{nick} desistiu (por enquanto)", "{nick} saiu com os diamantes... ou sem",
            "Tchau, {nick}! A casa sempre ganha", "{nick} sumiu no Void do mundo real"));
        /** Minutos parado até ficar AFK sozinho (0 = só pelo /afk). */
        public int afkAutoMinutos = 5;
        /** Nicks dos moderadores: usam /inv, /ec, /tells e /seen sem precisar ser OP. */
        public List<String> moderadores = new ArrayList<>();
        /** Opcional: URL de webhook do Discord para resultados. Vazio = desligado. */
        public String discordWebhook = "";
        /** Bônus da combinada, em % do total apostado, por perna além da primeira. Sai do cofre. */
        public int combinadaBonusPct = 25;
        /** Teto do bônus de uma combinada, em diamantes. */
        public int combinadaBonusMax = 64;
        /** Pasta do mundo. Vazio = lê o level-name do server.properties. */
        public String pastaMundo = "";
    }

    public static class Jogador {
        public String nome = "";
        public int apostas;
        public int acertos;
        public long gasto;
        public long ganho;
        public int duelosGanhos;
        public int duelosPerdidos;
    }

    /** Título que um OP deu a um jogador (degradê entre duas cores). */
    public static class Titulo {
        public String texto = "";
        public int cor1 = 0xFFAA00;
        public int cor2 = 0xFFE066;
    }

    /** Cor (degradê) do nick no chat, escolhida pelo jogador. */
    public static class CorNick {
        public int cor1 = 0xFFFFFF;
        public int cor2 = 0xFFFFFF;
        /** Nome mostrado no lugar do nick (chat e tab); null = o nick de verdade. */
        public String apelido;
    }

    /** Uma mensagem privada, guardada para os admins conferirem. */
    public static class Tell {
        public long ts;
        public String de = "";
        public String para = "";
        public String texto = "";
    }

    /** Quando cada jogador entrou e saiu pela última vez. */
    public static class Visto {
        public String nome = "";
        public String uuid = "";
        public long primeira;
        public long entrou;
        public long saiu;
        public boolean online;
        public String dimensao = "";
        public int x, y, z;
    }

    public static class Registro {
        public long ts;
        public String texto = "";

        public Registro() {}

        public Registro(long ts, String texto) {
            this.ts = ts;
            this.texto = texto;
        }
    }

    public static class Pagamento {
        public final String uuid;
        public final String nome;
        public final int valor;

        public Pagamento(String uuid, String nome, int valor) {
            this.uuid = uuid;
            this.nome = nome;
            this.valor = valor;
        }
    }

    public Config config = new Config();
    public int proximoId = 1;
    public int temporada = 1;
    public long cofre;
    /** Pote acumulado de bolões em que ninguém acertou. Vai para o próximo bolão resolvido. */
    public long jackpot;
    public Loteria loteria = new Loteria();

    /** Corpo deixado por quem morreu: os itens ficam aqui até alguém pegar. Itens guardados como texto (JSON). */
    public static class Corpo {
        public int id;
        public String uuid = "", nome = "", dim = "overworld";
        public double x, y, z;
        public long quando;
        public List<String> itens = new ArrayList<>();
        /** UUID do boneco (armor stand) que marca o corpo no mundo. */
        public String boneco = "";
        /** Posição de cada item no inventário de quem morreu (paralela a itens; -1 = não sei). Corpos antigos não têm. */
        public List<Integer> slots = new ArrayList<>();

        public int slotDe(int i) {
            return slots != null && i >= 0 && i < slots.size() && slots.get(i) != null ? slots.get(i) : -1;
        }

        public void removerItem(int i) {
            itens.remove(i);
            if (slots != null && i < slots.size()) slots.remove(i);
        }

        /** O slot de origem serve se existir no inventário (0..tamanho-1) e estiver livre; senão -1 (usa o primeiro livre). */
        public static int destino(int slot, int tamanho, java.util.function.IntPredicate livre) {
            return slot >= 0 && slot < tamanho && livre.test(slot) ? slot : -1;
        }
    }

    /** Último marco de horas online já anunciado, por UUID. */
    public Map<String, Integer> marcos = new LinkedHashMap<>();

    // ---------- mensagens de kill ----------

    public void migrarMsgMatar() {
        if (config.msgMatar.isEmpty()) return;
        for (var e : config.msgMatar.entrySet()) config.msgMatarLista.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(e.getValue());
        config.msgMatar.clear();
    }

    /** Lista de mensagens: de qualquer vítima (alvo = null) ou só de uma. */
    public List<String> msgsMatar(String matador, String alvo) {
        migrarMsgMatar();
        String chave = matador.toLowerCase() + (alvo == null ? "" : ">" + alvo.toLowerCase());
        return config.msgMatarLista.computeIfAbsent(chave, k -> new ArrayList<>());
    }

    /** Escolhe uma mensagem: a da vítima, se houver, senão uma das padrão. Null se não tiver nenhuma. */
    public String escolherMsgMatar(String matador, String vitima, java.util.Random r) {
        migrarMsgMatar();
        List<String> especifica = config.msgMatarLista.get(matador.toLowerCase() + ">" + vitima.toLowerCase());
        List<String> l = especifica != null && !especifica.isEmpty() ? especifica : config.msgMatarLista.get(matador.toLowerCase());
        return l == null || l.isEmpty() ? null : l.get(r.nextInt(l.size()));
    }

    /** Todas as mensagens do jogador, numeradas: [chave, texto]. As padrão primeiro. */
    public List<String[]> todasMsgsMatar(String matador) {
        migrarMsgMatar();
        List<String[]> r = new ArrayList<>();
        String m = matador.toLowerCase();
        for (String t : config.msgMatarLista.getOrDefault(m, List.of())) r.add(new String[] {m, t});
        for (var e : config.msgMatarLista.entrySet())
            if (e.getKey().startsWith(m + ">")) for (String t : e.getValue()) r.add(new String[] {e.getKey(), t});
        return r;
    }

    public boolean removerMsgMatar(String matador, int n) {
        List<String[]> todas = todasMsgsMatar(matador);
        if (n < 1 || n > todas.size()) return false;
        String[] alvo = todas.get(n - 1);
        List<String> l = config.msgMatarLista.get(alvo[0]);
        l.remove(alvo[1]);
        if (l.isEmpty()) config.msgMatarLista.remove(alvo[0]);
        return true;
    }

    /** Uma morte no histórico (o corpo some quando é esvaziado, a morte fica). */
    public static class Morte {
        public int corpoId;
        public String uuid = "", dim = "overworld";
        public int x, y, z, itens;
        public long quando;
        public boolean recuperado;
        /** Cabeça, peito, calça, bota, mão principal e mão secundária (JSON de cada item, "" se vazio). */
        public List<String> equip = new ArrayList<>();
    }

    public List<Morte> historicoMortes = new ArrayList<>();
    public List<Corpo> corpos = new ArrayList<>();
    public int proximoCorpo = 1;

    public Corpo novoCorpo(String uuid, String nome, String dim, double x, double y, double z, List<String> itens, long agora) {
        return novoCorpo(uuid, nome, dim, x, y, z, itens, null, agora);
    }

    public Corpo novoCorpo(String uuid, String nome, String dim, double x, double y, double z, List<String> itens, List<Integer> slots, long agora) {
        Corpo c = new Corpo();
        if (slots != null) c.slots.addAll(slots);
        c.id = proximoCorpo++;
        c.uuid = uuid;
        c.nome = nome;
        c.dim = dim;
        c.x = x;
        c.y = y;
        c.z = z;
        c.quando = agora;
        c.itens.addAll(itens);
        corpos.add(c);
        return c;
    }

    public Corpo corpo(int id) {
        for (Corpo c : corpos) if (c.id == id) return c;
        return null;
    }

    public Corpo corpoDoBoneco(String boneco) {
        for (Corpo c : corpos) if (c.boneco.equals(boneco)) return c;
        return null;
    }

    public List<Corpo> corposDe(String uuid) {
        List<Corpo> r = new ArrayList<>();
        for (Corpo c : corpos) if (c.uuid.equals(uuid)) r.add(c);
        return r;
    }

    public void removerCorpo(int id) {
        corpos.removeIf(c -> c.id == id);
    }
    public List<Bolao> bolaos = new ArrayList<>();
    /** uuid -> estatísticas de aposta. */
    public Map<String, Jogador> jogadores = new LinkedHashMap<>();
    /** uuid -> diamantes que ainda faltam entregar (jogador offline ou inventário cheio). */
    public Map<String, Integer> creditos = new LinkedHashMap<>();
    public List<Registro> historico = new ArrayList<>();
    /** uuid -> nick de todo mundo que já entrou (usado para achar os arquivos de stats). */
    public Map<String, String> conhecidos = new LinkedHashMap<>();
    /** uuids de quem já recebeu o livro de boas-vindas. */
    public List<String> tutorial = new ArrayList<>();
    /** Últimas mensagens privadas (até 300). */
    public List<Tell> logTells = new ArrayList<>();
    /** nick (minúsculo) -> última vez visto. */
    public Map<String, Visto> vistos = new LinkedHashMap<>();

    public static class Local {
        public String dim = "overworld";
        public int x, y, z;
        public long quando;
    }

    /** Onde cada jogador morreu por último (uuid -> local). */
    public Map<String, Local> mortes = new LinkedHashMap<>();

    /** Locais salvos por jogador (uuid -> nome -> local). Só anotações: não teleporta. */
    public Map<String, Map<String, Local>> locais = new LinkedHashMap<>();

    // ---------- duelos ----------

    /** Duelo entre dois jogadores: cada um põe o mesmo valor e o vencedor leva o pote (menos a parte da casa). */
    public static class Duelo {
        public int id;
        public String uuidA = "", nomeA = "", uuidB = "", nomeB = "";
        public int valor;
        /** PROPOSTO, ESCOLHENDO (só duelo de itens), PREPARANDO (construindo a arena), ATIVO, RESOLVIDO ou CANCELADO. */
        public String estado = "PROPOSTO";
        /** true = a aposta são itens escolhidos do inventário do outro, e não diamantes. */
        public boolean itens;
        public long criadoEm;
        public long inicio;
        public String vencedor = "";
        /** O que foi pago de fato quando resolveu (para poder anular depois). */
        public int premio, taxa;
    }

    /** Um duelo terminado, para o placar. */
    public static class DueloReg {
        public long ts;
        public String uuidVenc = "", nomeVenc = "", uuidPerd = "", nomePerd = "";
        public boolean itens;
        public String resumo = "";
        /** Id do duelo (0 nos registros antigos). */
        public int id;
    }

    public List<DueloReg> placarDuelos = new ArrayList<>();

    private void registrarDuelo(Duelo d, String uuidVencedor, boolean itens, String resumo, long agora) {
        boolean aGanhou = uuidVencedor.equals(d.uuidA);
        DueloReg r = new DueloReg();
        r.ts = agora;
        r.uuidVenc = aGanhou ? d.uuidA : d.uuidB;
        r.nomeVenc = aGanhou ? d.nomeA : d.nomeB;
        r.uuidPerd = aGanhou ? d.uuidB : d.uuidA;
        r.nomePerd = aGanhou ? d.nomeB : d.nomeA;
        r.itens = itens;
        r.resumo = resumo;
        r.id = d.id;
        placarDuelos.add(r);
        while (placarDuelos.size() > 300) placarDuelos.remove(0);
    }

    /** Os duelos do jogador (nick), do mais novo para o mais antigo. */
    public List<DueloReg> duelosDe(String nick, int max) {
        List<DueloReg> r = new ArrayList<>();
        for (int i = placarDuelos.size() - 1; i >= 0 && r.size() < max; i--) {
            DueloReg x = placarDuelos.get(i);
            if (x.nomeVenc.equalsIgnoreCase(nick) || x.nomePerd.equalsIgnoreCase(nick)) r.add(x);
        }
        return r;
    }

    /** Vitórias de a contra b e de b contra a. */
    public int[] confronto(String a, String b) {
        int[] v = new int[2];
        for (DueloReg x : placarDuelos) {
            if (x.nomeVenc.equalsIgnoreCase(a) && x.nomePerd.equalsIgnoreCase(b)) v[0]++;
            else if (x.nomeVenc.equalsIgnoreCase(b) && x.nomePerd.equalsIgnoreCase(a)) v[1]++;
        }
        return v;
    }

    public int proximoDuelo = 1;
    public List<Duelo> duelos = new ArrayList<>();

    public Duelo achaDuelo(int id) {
        for (Duelo d : duelos) if (d.id == id) return d;
        return null;
    }

    /** Já está em um duelo ativo ou escolhendo itens para um. */
    public boolean ocupado(String uuid) {
        for (Duelo d : duelos) {
            boolean vivo = d.estado.equals("ATIVO") || d.estado.equals("ESCOLHENDO") || d.estado.equals("PREPARANDO");
            if (vivo && (d.uuidA.equals(uuid) || d.uuidB.equals(uuid))) return true;
        }
        return false;
    }

    /** O duelo ativo em que o jogador está, ou null. */
    public Duelo duelAtivoDe(String uuid) {
        for (Duelo d : duelos) if (d.estado.equals("ATIVO") && (d.uuidA.equals(uuid) || d.uuidB.equals(uuid))) return d;
        return null;
    }

    public boolean temDueloPendente(String uuid) {
        for (Duelo d : duelos) {
            if (d.estado.equals("PROPOSTO") && d.uuidA.equals(uuid)) return true;
        }
        return false;
    }

    /** Valida o convite. null = pode; senão o motivo. */
    public String validarDuelo(String uuidA, String uuidB, int valor) {
        if (uuidA.equals(uuidB)) return "Você não pode duelar contra você mesmo.";
        if (valor < config.apostaMin) return "Valor mínimo: " + config.apostaMin + " diamante(s).";
        if (valor > config.apostaMax) return "Valor máximo: " + config.apostaMax + " diamantes.";
        if (ocupado(uuidA)) return "Você já está em um duelo.";
        if (ocupado(uuidB)) return "O outro jogador já está em um duelo.";
        if (temDueloPendente(uuidA)) return "Você já tem um convite pendente.";
        return null;
    }

    public Duelo proporDuelo(String uuidA, String nomeA, String uuidB, String nomeB, int valor, long agora) {
        Duelo d = new Duelo();
        d.id = proximoDuelo++;
        d.uuidA = uuidA;
        d.nomeA = nomeA;
        d.uuidB = uuidB;
        d.nomeB = nomeB;
        d.valor = valor;
        d.criadoEm = agora;
        duelos.add(d);
        return d;
    }

    /** Marca como ativo. Chame só depois de tirar os diamantes dos dois. */
    public void ativarDuelo(Duelo d, long agora) {
        d.estado = "ATIVO";
        d.inicio = agora;
    }

    /** Paga o vencedor. Devolve quanto ele recebeu. */
    public int resolverDuelo(Duelo d, String uuidVencedor, long agora) {
        d.estado = "RESOLVIDO";
        d.vencedor = uuidVencedor;
        int pote = d.valor * 2;
        int rake = pote * config.rakePct / 100;
        int premio = pote - rake;
        d.premio = premio;
        d.taxa = rake;
        cofre += rake;
        credito(uuidVencedor, premio);
        boolean aGanhou = uuidVencedor.equals(d.uuidA);
        Jogador ja = jogador(d.uuidA, d.nomeA);
        Jogador jb = jogador(d.uuidB, d.nomeB);
        ja.apostas++;
        jb.apostas++;
        ja.gasto += d.valor;
        jb.gasto += d.valor;
        (aGanhou ? ja : jb).acertos++;
        (aGanhou ? ja : jb).ganho += premio;
        (aGanhou ? ja : jb).duelosGanhos++;
        (aGanhou ? jb : ja).duelosPerdidos++;
        historico.add(new Registro(agora, "Duelo: " + (aGanhou ? d.nomeA : d.nomeB) + " venceu " + (aGanhou ? d.nomeB : d.nomeA) + ", levou " + premio + " diamantes"));
        registrarDuelo(d, uuidVencedor, false, premio + " diamantes", agora);
        return premio;
    }

    /**
     * Desfaz um duelo resolvido: tira dos números e do placar e, no duelo de diamantes, devolve a aposta (v) a cada um.
     * O cofre cobre o que não foi recolhido do vencedor: sai do cofre 2v menos o que se recuperou dele.
     * Quem chama recolhe o prêmio do vencedor antes e diz quanto veio em {@code recuperado}.
     */
    public void anularDuelo(int id, String uuidA, String nomeA, String uuidB, String nomeB, String uuidVencedor, int valor, boolean itens, int premio, int recuperado, long agora) {
        boolean aGanhou = uuidVencedor.equals(uuidA);
        Jogador ja = jogador(uuidA, nomeA);
        Jogador jb = jogador(uuidB, nomeB);
        Jogador venc = aGanhou ? ja : jb, perd = aGanhou ? jb : ja;
        venc.apostas = Math.max(0, venc.apostas - 1);
        perd.apostas = Math.max(0, perd.apostas - 1);
        venc.acertos = Math.max(0, venc.acertos - 1);
        venc.duelosGanhos = Math.max(0, venc.duelosGanhos - 1);
        perd.duelosPerdidos = Math.max(0, perd.duelosPerdidos - 1);
        if (!itens) {
            venc.gasto = Math.max(0, venc.gasto - valor);
            perd.gasto = Math.max(0, perd.gasto - valor);
            venc.ganho = Math.max(0, venc.ganho - premio);
            credito(uuidA, valor);
            credito(uuidB, valor);
            cofre -= (2L * valor - recuperado);
        }
        placarDuelos.removeIf(r -> r.id == id && id > 0);
        Duelo d = achaDuelo(id);
        if (d != null) d.estado = "ANULADO";
        historico.add(new Registro(agora, "Duelo #" + id + " " + nomeA + " x " + nomeB + " anulado pela staff"));
    }

    /** Duelo de itens: quem perde entrega os itens apostados (feito pela camada do jogo). Sem taxa da casa. */
    public void resolverDueloItens(Duelo d, String uuidVencedor, long agora) {
        resolverDueloItens(d, uuidVencedor, agora, "");
    }

    public void resolverDueloItens(Duelo d, String uuidVencedor, long agora, String resumo) {
        d.estado = "RESOLVIDO";
        d.vencedor = uuidVencedor;
        boolean aGanhou = uuidVencedor.equals(d.uuidA);
        Jogador ja = jogador(d.uuidA, d.nomeA);
        Jogador jb = jogador(d.uuidB, d.nomeB);
        ja.apostas++;
        jb.apostas++;
        (aGanhou ? ja : jb).acertos++;
        (aGanhou ? ja : jb).duelosGanhos++;
        (aGanhou ? jb : ja).duelosPerdidos++;
        historico.add(new Registro(agora, "Duelo de itens: " + (aGanhou ? d.nomeA : d.nomeB) + " venceu " + (aGanhou ? d.nomeB : d.nomeA)));
        registrarDuelo(d, uuidVencedor, true, resumo, agora);
    }

    /** Cancela e devolve o valor aos dois (só se já estava ativo, pois só aí os diamantes foram tirados). */
    public void cancelarDuelo(Duelo d, long agora) {
        if (d.estado.equals("ATIVO") && !d.itens) {
            credito(d.uuidA, d.valor);
            credito(d.uuidB, d.valor);
            historico.add(new Registro(agora, "Duelo " + d.nomeA + " x " + d.nomeB + " cancelado, valores devolvidos"));
        }
        d.estado = "CANCELADO";
    }

    // ---------- cápsulas do tempo e lembretes ----------

    /** Lembrete pessoal (uuid do dono) ou aviso para todos (uuid "*"), entregue quando chegar a hora. */
    public static class Capsula {
        public int id;
        public String uuid = "";
        public String nome = "";
        public String texto = "";
        public long quando;
        public long criadoEm;
    }

    public int proximaCapsula = 1;
    public List<Capsula> capsulas = new ArrayList<>();

    public Capsula novaCapsula(String uuid, String nome, String texto, long quando, long agora) {
        Capsula c = new Capsula();
        c.id = proximaCapsula++;
        c.uuid = uuid;
        c.nome = nome;
        c.texto = texto;
        c.quando = quando;
        c.criadoEm = agora;
        capsulas.add(c);
        return c;
    }

    public int capsulasPendentesDe(String uuid) {
        int n = 0;
        for (Capsula c : capsulas) if (c.uuid.equals(uuid)) n++;
        return n;
    }

    public boolean removerCapsula(int id) {
        return capsulas.removeIf(c -> c.id == id);
    }

    // ---------- combinadas ----------

    public static class Perna {
        public int bolao;
        public String opcao = "";

        public Perna() {}

        public Perna(int bolao, String opcao) {
            this.bolao = bolao;
            this.opcao = opcao;
        }
    }

    /** Várias apostas feitas de uma vez; se todas acertarem, a casa paga um bônus. */
    public static class Combinada {
        public int id;
        public String uuid = "";
        public String nome = "";
        public int valorPorPerna;
        public List<Perna> pernas = new ArrayList<>();
        /** PENDENTE, GANHA, PERDIDA ou CANCELADA. */
        public String estado = "PENDENTE";
        public int bonus;
    }

    public int proximaCombinada = 1;
    public List<Combinada> combinadas = new ArrayList<>();
    /** Bônus pagos pela última resolução (só para avisar; não é salvo). */
    public transient List<Pagamento> bonusRecentes = new ArrayList<>();

    /** Bônus se todas as pernas acertarem (antes do limite do cofre). */
    public int bonusDe(int pernas, int totalApostado) {
        long b = (long) totalApostado * config.combinadaBonusPct * Math.max(0, pernas - 1) / 100;
        return (int) Math.min(b, config.combinadaBonusMax);
    }

    /** null = pode; senão o motivo. */
    public String validarCombinada(String uuid, String nome, List<Perna> pernas, int valor) {
        if (pernas.size() < 2) return "Escolha pelo menos 2 bolões.";
        if (pernas.size() > 4) return "No máximo 4 bolões por combinada.";
        List<Integer> vistos = new ArrayList<>();
        for (Perna pn : pernas) {
            if (vistos.contains(pn.bolao)) return "Não repita o mesmo bolão.";
            vistos.add(pn.bolao);
            Bolao b = achar(pn.bolao);
            if (b == null) return "Bolão #" + pn.bolao + " não existe.";
            String e = validarAposta(b, uuid, nome, pn.opcao, valor);
            if (e != null) return "#" + b.id + ": " + e;
        }
        return null;
    }

    /** Registra as apostas de cada perna e a combinada. Chame só depois de tirar valor * pernas diamantes. */
    public Combinada criarCombinada(String uuid, String nome, List<Perna> pernas, int valor) {
        Combinada c = new Combinada();
        c.id = proximaCombinada++;
        c.uuid = uuid;
        c.nome = nome;
        c.valorPorPerna = valor;
        for (Perna pn : pernas) {
            Bolao b = achar(pn.bolao);
            String exata = b.opcaoExata(pn.opcao);
            apostar(b, uuid, nome, exata, valor);
            c.pernas.add(new Perna(pn.bolao, exata));
        }
        combinadas.add(c);
        return c;
    }

    public List<Combinada> combinadasDe(String uuid) {
        List<Combinada> r = new ArrayList<>();
        for (Combinada c : combinadas) if (c.uuid.equals(uuid)) r.add(c);
        return r;
    }

    private static boolean temAposta(Bolao b, String uuid, String opcao) {
        for (Bolao.Aposta a : b.apostas) if (a.uuid.equals(uuid) && a.opcao.equalsIgnoreCase(opcao)) return true;
        return false;
    }

    /** Confere as combinadas pendentes depois que um bolão mudou. Devolve os bônus pagos (já lançados nos créditos). */
    public List<Pagamento> avaliarCombinadas(long agora) {
        List<Pagamento> pagos = new ArrayList<>();
        for (Combinada c : combinadas) {
            if (!c.estado.equals("PENDENTE")) continue;
            boolean todasGanhas = true;
            String novo = null;
            for (Perna pn : c.pernas) {
                Bolao b = achar(pn.bolao);
                if (b == null || b.status == Bolao.Status.CANCELADO || !temAposta(b, c.uuid, pn.opcao)) {
                    novo = "CANCELADA";
                    break;
                }
                if (b.status == Bolao.Status.RESOLVIDO) {
                    if (!pn.opcao.equalsIgnoreCase(b.vencedor)) {
                        novo = "PERDIDA";
                        break;
                    }
                } else {
                    todasGanhas = false;
                }
            }
            if (novo == null && todasGanhas) novo = "GANHA";
            if (novo == null) continue;
            c.estado = novo;
            if (novo.equals("GANHA")) {
                int total = c.valorPorPerna * c.pernas.size();
                int bonus = (int) Math.min(bonusDe(c.pernas.size(), total), Math.max(0, cofre));
                c.bonus = bonus;
                if (bonus > 0) {
                    cofre -= bonus;
                    credito(c.uuid, bonus);
                    jogador(c.uuid, c.nome).ganho += bonus;
                    pagos.add(new Pagamento(c.uuid, c.nome, bonus));
                }
                historico.add(new Registro(agora, "Combinada de " + c.nome + " (" + c.pernas.size() + " bolões) acertou tudo, bônus " + bonus));
            }
        }
        return pagos;
    }

    // ---------- mensagens privadas ----------

    public void registrarTell(String de, String para, String texto, long agora) {
        Tell t = new Tell();
        t.ts = agora;
        t.de = de;
        t.para = para;
        t.texto = texto;
        logTells.add(t);
        while (logTells.size() > 300) logTells.remove(0);
    }

    /** As últimas mensagens em que o jogador falou ou recebeu, da mais antiga para a mais nova. */
    public List<Tell> tellsDe(String nick, int max) {
        List<Tell> r = new ArrayList<>();
        for (int i = logTells.size() - 1; i >= 0 && r.size() < max; i--) {
            Tell t = logTells.get(i);
            if (t.de.equalsIgnoreCase(nick) || t.para.equalsIgnoreCase(nick)) r.add(0, t);
        }
        return r;
    }

    // ---------- consultas ----------

    public Bolao achar(int id) {
        for (Bolao b : bolaos) if (b.id == id) return b;
        return null;
    }

    public List<Bolao> ativos() {
        List<Bolao> r = new ArrayList<>();
        for (Bolao b : bolaos) if (b.emAndamento()) r.add(b);
        return r;
    }

    public List<Bolao> comGatilho(Bolao.Gatilho g) {
        List<Bolao> r = new ArrayList<>();
        for (Bolao b : bolaos) if (b.emAndamento() && b.gatilho == g) r.add(b);
        return r;
    }

    /** Multiplicador estimado se a opção ganhar agora (ex.: 2.4 = recebe 2,4x). 0 se ninguém apostou nela. */
    public double odd(Bolao b, String opcao) {
        int naOpcao = b.poteDa(opcao);
        if (naOpcao <= 0) return 0;
        double liquido = b.pote() * (100 - config.rakePct) / 100.0 + jackpot;
        return liquido / naOpcao;
    }

    // ---------- administração ----------

    public Bolao criar(String titulo, Bolao.Gatilho gatilho, List<String> opcoes, long agora) {
        Bolao b = new Bolao();
        b.id = proximoId++;
        b.titulo = titulo;
        b.gatilho = gatilho == null ? Bolao.Gatilho.MANUAL : gatilho;
        b.opcoes = new ArrayList<>(opcoes);
        b.criadoEm = agora;
        b.status = Bolao.Status.FECHADO;
        bolaos.add(b);
        return b;
    }

    /** Devolve null se deu certo, ou o motivo. */
    public String abrir(Bolao b) {
        if (b.status != Bolao.Status.FECHADO) return "Só dá para abrir um bolão fechado.";
        if (b.opcoes.size() < 2) return "O bolão precisa de pelo menos 2 opções.";
        b.status = Bolao.Status.ABERTO;
        return null;
    }

    public String fechar(Bolao b) {
        if (b.status != Bolao.Status.ABERTO) return "O bolão não está aberto.";
        b.status = Bolao.Status.FECHADO;
        return null;
    }

    // ---------- apostas ----------

    /** Valida a aposta. null = pode apostar; senão, o motivo. */
    public String validarAposta(Bolao b, String uuid, String nome, String opcao, int valor) {
        if (!b.aberto()) return "Esse bolão não está aberto.";
        if (b.opcaoExata(opcao) == null) return "Opção inválida.";
        if (b.excluidos.contains(uuid)) return "Você está nesse duelo: não pode apostar nele.";
        if (b.opcaoExata(opcao).equalsIgnoreCase(nome)) return "Não pode apostar em você mesmo.";
        if (valor < config.apostaMin) return "Aposta mínima: " + config.apostaMin + " diamante(s).";
        int teto = b.maxAposta > 0 ? b.maxAposta : config.apostaMax;
        if (b.apostadoPor(uuid) + valor > teto)
            return "Limite por bolão: " + teto + " diamantes (você já pôs " + b.apostadoPor(uuid) + ").";
        return null;
    }

    /** Registra a aposta. Chame só depois de tirar os diamantes. */
    public void apostar(Bolao b, String uuid, String nome, String opcao, int valor) {
        String exata = b.opcaoExata(opcao);
        b.apostas.add(new Bolao.Aposta(uuid, nome, exata, valor));
        Jogador j = jogador(uuid, nome);
        j.apostas++;
        j.gasto += valor;
    }

    /** Desiste das apostas do jogador no bolão aberto. Devolve quanto volta (já sem a taxa), ou -1. */
    public int cashout(Bolao b, String uuid) {
        if (!b.aberto()) return -1;
        int total = b.apostadoPor(uuid);
        if (total <= 0) return -1;
        int volta = total * (100 - config.cashoutTaxaPct) / 100;
        cofre += total - volta;
        b.apostas.removeIf(a -> a.uuid.equals(uuid));
        Jogador j = jogadores.get(uuid);
        if (j != null) {
            j.gasto -= total;
            j.apostas = Math.max(0, j.apostas - 1);
        }
        credito(uuid, volta);
        return volta;
    }

    // ---------- resultado ----------

    /** Resolve o bolão. Devolve os pagamentos (já lançados nos créditos). */
    public List<Pagamento> resolver(Bolao b, String vencedor, long agora) {
        String venc = b.opcaoExata(vencedor);
        if (venc == null) venc = vencedor;
        b.vencedor = venc;
        b.status = Bolao.Status.RESOLVIDO;
        b.candidatos.clear();

        int pote = b.pote();
        int rake = pote * config.rakePct / 100;
        long distribuir = (long) pote - rake + jackpot;
        cofre += rake;

        int vencedoresTotal = b.poteDa(venc);
        List<Pagamento> pag = new ArrayList<>();
        if (vencedoresTotal <= 0) {
            jackpot = distribuir;
            historico.add(new Registro(agora, "#" + b.id + " " + b.titulo + ": " + venc + " ganhou, ninguém acertou (jackpot " + jackpot + ")"));
            bonusRecentes = avaliarCombinadas(agora);
            return pag;
        }
        jackpot = 0;
        long pago = 0;
        for (Bolao.Aposta a : b.apostas) {
            if (!a.opcao.equalsIgnoreCase(venc)) continue;
            int v = (int) (distribuir * a.valor / vencedoresTotal);
            pago += v;
            pag.add(new Pagamento(a.uuid, a.nome, v));
            credito(a.uuid, v);
            Jogador j = jogador(a.uuid, a.nome);
            j.acertos++;
            j.ganho += v;
        }
        cofre += distribuir - pago;
        b.pagoTotal = (int) pago;
        historico.add(new Registro(agora, "#" + b.id + " " + b.titulo + ": " + venc + " ganhou, pagou " + pago + " diamantes"));
        bonusRecentes = avaliarCombinadas(agora);
        return pag;
    }

    /** Marca como aguardando confirmação de um OP, quando o resultado é ambíguo. */
    public void aguardar(Bolao b, List<String> candidatos) {
        b.status = Bolao.Status.AGUARDANDO;
        b.candidatos = new ArrayList<>(candidatos);
    }

    /** Cancela e devolve todas as apostas inteiras. */
    public void cancelar(Bolao b, long agora) {
        for (Bolao.Aposta a : b.apostas) {
            credito(a.uuid, a.valor);
            Jogador j = jogador(a.uuid, a.nome);
            j.gasto -= a.valor;
            j.apostas = Math.max(0, j.apostas - 1);
        }
        b.status = Bolao.Status.CANCELADO;
        historico.add(new Registro(agora, "#" + b.id + " " + b.titulo + ": cancelado, apostas devolvidas"));
        avaliarCombinadas(agora);
    }

    // ---------- créditos, jogadores, ranking ----------

    public void credito(String uuid, int valor) {
        if (valor <= 0) return;
        creditos.merge(uuid, valor, Integer::sum);
    }

    public int creditoDe(String uuid) {
        return creditos.getOrDefault(uuid, 0);
    }

    public void baixarCredito(String uuid, int entregue) {
        int resto = creditoDe(uuid) - entregue;
        if (resto <= 0) creditos.remove(uuid);
        else creditos.put(uuid, resto);
    }

    public Jogador jogador(String uuid, String nome) {
        Jogador j = jogadores.computeIfAbsent(uuid, k -> new Jogador());
        if (nome != null && !nome.isEmpty()) j.nome = nome;
        return j;
    }

    /** Ranking por lucro (ganho - gasto), depois acertos. */
    public List<Jogador> rankingApostadores() {
        List<Jogador> l = new ArrayList<>(jogadores.values());
        l.removeIf(j -> j.apostas == 0 && j.ganho == 0);
        l.sort((a, b) -> {
            int c = Long.compare(b.ganho - b.gasto, a.ganho - a.gasto);
            return c != 0 ? c : Integer.compare(b.acertos, a.acertos);
        });
        return l;
    }

    /** Título de sátira conforme a posição e o histórico. */
    public static String titulo(int posicao, Jogador j, int totalJogadores) {
        if (posicao == 1 && j.ganho > j.gasto) return "Rei da Banca";
        if (j.apostas >= 3 && j.acertos == 0) return "Pé-Frio Oficial";
        if (posicao == totalJogadores && j.ganho < j.gasto) return "Doador da Casa";
        if (j.apostas >= 5 && j.acertos * 2 >= j.apostas) return "Profeta";
        return "Apostador";
    }

    /** Nova temporada: zera as estatísticas de apostas (bolões e créditos pendentes ficam). */
    public void novaTemporada(long agora) {
        List<Jogador> r = rankingApostadores();
        String campeao = r.isEmpty() ? "ninguém" : r.get(0).nome;
        historico.add(new Registro(agora, "Fim da temporada " + temporada + ". Campeão: " + campeao));
        temporada++;
        jogadores.clear();
    }
}
