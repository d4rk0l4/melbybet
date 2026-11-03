# MelbyCraft

Mod de servidor (Fabric, Minecraft 26.3) da Gangue do Melby: casa de apostas satírica, visual, corpos, /find e vários recursos de servidor. **Só o servidor precisa dele**: os jogadores entram normalmente, sem instalar nada (quem quiser a barra de busca nos baús instala o mesmo jar no cliente, opcional).

## Como pegar o .jar (sem Java no PC)

1. Crie um repositório no GitHub e suba **todo o conteúdo desta pasta** (inclusive `.github`).
2. Aba **Actions** > workflow **build**. Ele roda sozinho a cada push; também dá pra clicar em *Run workflow*.
3. Quando ficar verde, abra a execução e baixe o artefato **melbybet-jar**. Dentro vem o `melbybet-1.0.0.jar` (o outro, `-sources`, não serve pro servidor).
4. Se ficar vermelho, copie o erro do passo **build** e me mande.

## Instalar no servidor (teste num servidor separado primeiro)

Na pasta `mods` do servidor: Fabric Loader, a **Fabric API** mais recente para 26.2 e o `melbybet-1.0.0.jar`. O sgui (menus) já vai dentro do jar.

Quem é OP (nível de admin) usa `/melbybet`. Os dados ficam em `config/melbybet/casa.json`.

## Jogadores

- `/bet`: lista de bolões. Clique no bolão, escolha o jogador (cabeça com a skin), escolha o valor. Os diamantes saem do inventário na hora e o prêmio volta em diamantes.
- `/top`: um item por categoria (distância total, andou, correu, voou, nadou, cavalgou, blocos quebrados, blocos colocados, mobs, mortes, tempo online, itens craftados). O ranking está na descrição e, ao clicar, sai no chat.
- No menu `/bet`: meu histórico, ranking de apostadores (com títulos de sátira) e desistir de uma aposta (com taxa).

Regras: não pode apostar em si mesmo, limite por bolão (64), all-in pede confirmação, a casa fica com 5% do pote, se ninguém acertar o pote vira **jackpot** e vai para o próximo bolão. Se o prêmio não couber no inventário ou a pessoa estiver offline, ele fica guardado e chega quando der.

## OP

| Comando | O que faz |
|---|---|
| `/melbybet criar VOID Quem cai no Void primeiro?` | cria (fechado) com os nicks do roster como opções |
| `/melbybet criar MANUAL Quem vence o PvP? \| Davi;Kaue` | bolão manual com opções suas |
| `/melbybet abrir <id>` / `fechar <id>` | abre ou fecha as apostas |
| `/melbybet prazo <id> <minutos>` | fecha sozinho (bolão relâmpago) |
| `/melbybet resolver <id> <opção>` | declara o vencedor (obrigatório nos MANUAL e nos ambíguos) |
| `/melbybet cancelar <id>` | devolve todas as apostas inteiras |
| `/melbybet lista`, `cofre` | situação dos bolões e do cofre |
| `/melbybet roster Nick1 Nick2 ...` | muda os nicks da Gangue |
| `/melbybet temporada nova` | zera o ranking de apostadores |

**Gatilhos automáticos:** `VOID` (morte no Void), `WITHER` e `DRAGAO` (quem matou), `DIAMANTE`, `NETHER`, `END`, `ELYTRA`, `NETHERITE` (primeira conquista, lida dos arquivos de advancements, vale só a partir da criação do bolão). Empate no mesmo segundo ou Wither/Dragão morto sem jogador claro: o bolão fica **aguardando** e um OP confirma com `/melbybet resolver`.

**Importante:** confira os nicks com `/melbybet roster` antes de criar bolões. O `/top` mostra quem já entrou no servidor depois de instalar o mod; os números vêm das stats do próprio Minecraft (acumuladas desde que o mundo existe).

## Inventário, ender chest e chat

| Comando | Quem | O que faz |
|---|---|---|
| `/inv <jogador>` | OP | mostra o inventário (mochila, hotbar, armadura, mão secundária) de um jogador online, só para olhar |
| `/ec <jogador>` | OP | mostra o ender chest de um jogador online, só para olhar (não abre o seu, para não virar ender chest portátil) |
| `/melbybet mod add <nick>` | OP | cria um moderador: usa `/inv`, `/ec`, `/tells` e `/seen` sem ser OP (`remove` e `lista` também existem) |
| `/melbybet webhook <url>` | OP | liga os avisos no Discord (`off` desliga, `teste` manda uma mensagem de teste) |
| `/afk` | todos | liga/desliga o AFK: aparece **[AFK]** no tab, acima da cabeça e no chat. Sai sozinho quando a pessoa se mexe, e entra sozinho depois de 5 min parado |
| `/melbybet afk <minutos>` | OP | tempo parado até ficar AFK sozinho (`0` desliga o automático) |
| `/seen <jogador>` | todos | quando o jogador foi visto pela última vez (OP e moderador veem também a última posição) |
| `/melbybet titulo <nick> [#cor1 #cor2] <texto>` | OP | dá um título em degradê (`off` remove). Ex.: `/melbybet titulo saya_neku #ff8a24 #ffd23f Lenda` |
| `/topbet` | todos | ranking de quem mais ganhou nas apostas |
| `/bet ajuda` | todos | abre o livro de boas-vindas (na primeira vez que a pessoa usa `/bet`, ela recebe o livro e um aviso na tela) |

No chat, qualquer jogador pode escrever:
- `@inv`: vira **[Inventário de Nick]**; quem clicar vê uma foto do inventário daquele momento (só olhar).
- `@ec`: o mesmo para o ender chest.
- `@item`: mostra o item que está na mão, com balão de encantamentos e descrição.

## Menu /melby

`/melby` abre o menu central, separado por categorias. Tudo que pede texto (título, cor, motivo de ban, número) é digitado no chat, mas **não vai para o chat geral**: só vale como resposta do menu. Escrever `cancelar` desiste.

- **Para todos:** Apostas, Ranking de stats, Meu perfil (cor do nick, chat local ou global, AFK) e Ajuda.
- **Moderação** (admin e moderador): lista de jogadores online ou por nick (inclusive offline) com ver inventário, ver ender chest, mensagens privadas e visto por último.
- **Administração** (só OP): Bolões (criar com escolha do tipo, abrir, fechar, prazo, declarar vencedor, cancelar), mensagens de entrar e sair, configurações (alcance do chat, AFK, parte da casa, limites, webhook do Discord) e, dentro de cada jogador, título, cor, moderador, kick, ban (com motivo e confirmação) e desbanir.

Os comandos com `/melbybet ...` continuam funcionando.

Mencionar `@nick` no chat toca um som e mostra um aviso na tela de quem foi citado.

## Aposta combinada

No `/bet`, botão **Combinada**: escolha de 2 a 4 bolões abertos, uma opção em cada, e o valor por bolão (1, 5, 10, 32 ou 64). Cada perna é uma aposta normal. Se **todas** acertarem, a casa paga um bônus do cofre: 25% do total apostado por perna além da primeira, com teto de 64 diamantes e limitado ao que tem no cofre (`combinadaBonusPct` e `combinadaBonusMax` em `casa.json`). Se algum bolão for cancelado ou o jogador desistir de uma perna, a combinada é cancelada, sem bônus.

## Lembretes e avisos (`/capsula`)

- `/capsula "Arrumar baús" 2 horas`: lembrete só seu, que toca um som e aparece no chat e na tela na hora marcada. O tempo vai no fim e aceita `s`, `min`, `h` e `dias` (`30min`, `2h`, `1 dia`). De 10 segundos a 365 dias.
- `/capsula lista`: seus lembretes com [Cancelar] clicável. `/capsula cancelar <id>` também apaga.
- `/capsula todos "Reunião no spawn" 30 min`: só OP; manda um aviso com sino para todos os jogadores online na hora marcada.
- Fica salvo se o servidor reiniciar. Lembrete pessoal de quem estava offline espera a pessoa entrar. Até 20 pendentes por jogador.

## Duelo

- `/duelo <nick> <valor>` manda um convite com [Aceitar] e [Recusar] clicáveis (vale 60 segundos). Ao aceitar, os dois põem o mesmo valor em diamantes.
- Vence quem matar o outro primeiro, ou quem receber um `/duelo desistir` do adversário. O vencedor leva o pote menos a parte da casa (5%).
- Depois de um golpe entre os dois, ficam 30 segundos em combate: `/duelo desistir` só funciona depois disso. Sair do jogo conta como derrota (a mensagem avisa se foi em combate).
- Se um dos dois sair do jogo, perde. Se alguém morrer por outro motivo, ou passarem 15 minutos sem vencedor, o duelo é cancelado e os diamantes voltam.
- O resultado vai para o ranking de apostadores e para o Discord.
- **Apostas no duelo dos outros:** quando um duelo começa, abre sozinho um bolão com os dois lutadores (em `/bet`), aberto por 60 segundos, com limite de 16 diamantes por apostador. Os dois duelistas não podem apostar. O resultado do duelo paga quem acertou (com a taxa normal da casa); se o duelo for cancelado, as apostas voltam.
- **Duelo de itens:** `/duelo <nick> inv`. Cada um vê o inventário do outro e escolhe até 5 itens dele; depois os dois aceitam os termos. Nada sai do inventário na hora: só quando o duelo acaba o perdedor entrega os itens apostados ao vencedor (no instante da morte, antes de caírem). Sem taxa da casa. O item é reconhecido pelo tipo, encantamentos e nome, e a durabilidade não conta (a espada pode gastar ou ser reparada pelo Remendo durante o duelo); o vencedor recebe o item no estado em que estiver. Se alguém tirar o item apostado do inventário durante o duelo, perde.

## Limpeza do chão (`/limpar`)

`/limpar` (só OP, também em `/melby` > Administração > Limpeza do chão) abre o menu: escolha de quanto em quanto tempo o servidor apaga os itens largados (5, 10, 15, 30, 60 min ou outro valor, ou desligado), quantos segundos antes avisa no chat (depois vem a contagem 10, 5, 3, 2, 1 na tela), por quanto tempo um item recém-largado fica protegido (padrão 30 s) e se itens valiosos ficam no chão (diamante, netherite, elytra, totem, estrela do Nether, encantados e com nome). Também tem "Limpar agora". `/limpar agora` faz o mesmo pelo comando. `/limpar aviso sim|nao` liga ou desliga todos os avisos da limpeza (a contagem e o resumo no chat; com ele desligado, só quem mandou limpar vê o resumo). Vem desligada por padrão.

## Morte e placar de duelos

- `/morte`: mostra onde foi a sua última morte, a que distância e em que direção (norte, sul..., e uma seta em relação a onde você olha), com [Copiar] e [Enviar]. Fica salvo se o servidor reiniciar.
- `/duelo placar [nick]`: vitórias e derrotas, os confrontos diretos ("Davi 3 x 1 Kaue") e os últimos 5 duelos, com o que foi apostado.

## Perfil e reinício

- `/perfil [nick]`: cartão do jogador (online ou offline): título, status, tempo na Gangue, tempo online, distância, blocos, mobs, mortes (com a posição no ranking), apostas, saldo e duelos.
- **Aviso de reinício:** `/melbybet reinicio add 04:30` (ou `/melby` > Administração > Reinício) cadastra os horários em que o servidor reinicia. O mod avisa 30, 15, 10, 5, 3, 2 e 1 minuto antes, e faz a contagem dos últimos 10 segundos na tela. O fuso padrão é America/Sao_Paulo (`fuso` em `casa.json`). `/melbybet reinicio lista` e `remover <hora>` também existem.
- `/melbybet reiniciar <minutos>`: contagem manual; no fim o mod salva e desliga o servidor (a hospedagem precisa religar). `/melbybet reiniciar cancelar` desiste.

## Gestos

`/gestos` abre um menu com um item para cada gesto (também em `/melby` > Meu perfil > Gestos). Cada um também tem o seu comando:

- `/spin`: o boneco gira em pé, como um pião, para quem está por perto, até você andar. A sua câmera não gira. `/spin` de novo também para.
- `/sit`: senta no chão (num banco invisível); shift levanta. Se ficar alto ou baixo, o OP ajusta com `/melbybet sit <altura>` (padrão 1.6; maior senta mais baixo).
- `/wave`: acena.
- `/point`: aponta para onde você olha (até 120 blocos): sobe uma coluna de brilho por 6 segundos e quem está perto recebe as coordenadas com [Copiar].
- `/crawl`: rasteja onde der, mesmo em espaço aberto, e todos veem você rastejando. `/crawl` de novo levanta.

## Coordenadas

Ao morrer, só quem morreu recebe a mensagem "Você morreu em x y z (dimensão)" com [Copiar] e [Enviar].

- `/coords`: mostra onde você está, com botões clicáveis **[Copiar]** (copia `x y z`) e **[Enviar]** (manda no chat para todos, já com [Copiar]).
- `/coords salvar <nome>`, `/coords lista` (cada local com Copiar, Enviar e Apagar) e `/coords apagar <nome>`. Até 30 locais por jogador. É só anotação, não teleporta.

## Chat

- Por padrão o chat é **local**: só quem está perto (100 blocos, na mesma dimensão) lê. OP muda com `/melbybet raio <blocos>` (`0` = sem limite).
- `/g <mensagem>` manda uma vez no **global**. `/g` sozinho trava o chat no global; `/g` de novo ou `/l` volta ao local. `/l <mensagem>` manda uma vez no local.
- `/tell <nick>` (ou `/msg`, `/w`) sozinho trava o chat naquela conversa privada; `/tell <nick> <mensagem>` manda uma só. `/r` responde ao último. `/g` ou `/l` sai da conversa travada.
- `/cor <#cor1> [#cor2]` ou `/cor fogo` (oceano, neon, floresta, rosa, ouro, gelo, por-do-sol) muda a cor do nick no chat, em degradê. `/cor off` tira. OP: `/melbybet cor <nick> ...`.
- Admin e moderador: `/tells <nick>` mostra as últimas 15 (ficam guardadas as últimas 300).
- Frases de entrar e sair: `/melbybet msg entrar add {nick} chegou!`, `... remover <n>`, `... lista` (o mesmo com `sair`). Uma é sorteada a cada vez.

O degradê do nick aparece só no chat; no tab e acima da cabeça o Minecraft não permite.

`/inv` e `/ec` funcionam também com jogador offline (precisa já ter entrado com o mod instalado).

## Títulos e mensagens

Quem lidera ou perde nas apostas ganha um título automático em degradê antes do nick (tab, cabeça e chat): Rei da Banca, Pé-Frio Oficial, Doador da Casa, Profeta. Um título dado por OP vale mais que o automático.

As mensagens de entrar e sair do servidor são trocadas por frases da Gangue, sorteadas. Para mudar as frases, edite `msgEntrar` e `msgSair` no `casa.json` (use `{nick}` onde vai o nome), com o servidor parado.

## Config (`config/melbybet/casa.json`, bloco `config`)

`rakePct`, `apostaMin`, `apostaMax`, `avisoGrande` (aposta que vira aviso no chat), `cashoutTaxaPct`, `roster`, `discordWebhook` (URL: resultados vão pro Discord), `pastaMundo` (vazio = usa o `level-name`). Pare o servidor antes de editar.

## O que não foi testado

A lógica de apostas, pagamento, ranking e leitura de stats tem testes (`tests/`, rodam no workflow). **A parte que fala com o Minecraft 26.3 (menus, comandos, eventos) nunca foi compilada nem rodada**: foi escrita lendo o código-fonte do sgui e da Fabric API do 26.2. É esperado que o primeiro build acuse algum nome de método. Mande o erro que eu corrijo.

Também aproximado: "blocos colocados" conta itens usados que não são ferramenta (o vanilla não separa). O caminho das stats mudou entre versões, então o mod procura em `world/players/stats` e `world/stats`.

Ainda não feito: empréstimo do Mineiro, apostas combinadas, bolões automáticos semanais, boss bar e fogos.


O degradê de cor do nick (`/melby` > Perfil) também aparece na lista de jogadores (tab), junto com o título e o [AFK].

## Apelido (`/nick`)

`/nick <jogador> <apelido>` (só OP) troca o nome mostrado no chat e na tab, sem mudar o nick de verdade. Códigos de cor viram degradê: `&6Rei&eDoMel` vai do laranja ao amarelo (também vale `&#rrggbb`); a primeira e a última cor definem o degradê. `/nick <jogador> reset` volta ao normal.

## Vender tudo no villager

Na tela do villager, escolha a troca e dê shift+clique no resultado: o servidor repete a troca até acabarem os itens (ou o villager esgotar a troca). Funciona com a tela normal do jogo, nada para instalar.

## Placas bonitas

Ao escrever numa placa (ou com `/placa <linha 1-4> <texto>` olhando para ela, sem o limite de largura do teclado), os códigos viram cor: `&0`-`&f` (cores), `&l` negrito, `&o` itálico, `&n` sublinhado, `&m` riscado, `&k` embaralhado, `&r` limpa, `&#ff8a24` (cor exata) e degradê com `<#ff0000-#0000ff>texto</>`. `/placa limpar` apaga a placa. Placa encerada não muda. Vale também para placa pendurada e para o verso. Ao editar de novo uma placa já colorida, o campo mostra só o texto: os códigos precisam ser escritos outra vez.

## Loteria da Gangue (`/loteria`)

O OP abre com `/loteria criar <minutos> <preço> [limite]`: tempo até o sorteio (1 min a 7 dias), preço do bilhete em diamantes (1 a 64) e limite de bilhetes por pessoa (padrão 10). Os jogadores usam `/loteria` (status, com botões clicáveis) e `/loteria comprar [qtd]`. Na hora marcada o servidor sorteia, com chance proporcional aos bilhetes, e o vencedor leva o pote menos a taxa da casa (a mesma dos bolões); o prêmio chega como os outros (crédito entregue ao entrar ou com espaço no inventário). Entrou menos de 2 pessoas: todos os bilhetes são devolvidos. Avisos no chat quando faltam 10 min, 5 min, 1 min e 10 s. `/loteria sortear` sorteia já e `/loteria cancelar` devolve tudo. Sobrevive a reinício do servidor.

## Mensagem de kill (`/msgmatar`)

Cada jogador tem as suas frases, que aparecem no chat quando ele mata outro jogador, no lugar da mensagem padrão do jogo. `/msgmatar add <texto>` adiciona uma (até 10; a cada kill uma é sorteada), `/msgmatar contra <jogador> <texto>` adiciona uma só para quando matar aquela pessoa (essa vence a padrão), `/msgmatar` lista com números, `/msgmatar remover <n>` tira uma e `/msgmatar off` apaga todas. Use `{vitima}` (obrigatório) e `{matador}`, e os códigos de cor das placas (`&c`, `&l`, `&#ff8a24`). Ex.: `/msgmatar add &c{matador} &7mandou {vitima} pro espaço`.

## Corpo (itens da morte)

Quando um jogador morre (sem keepInventory), todos os itens ficam num corpo: a cabeça dele flutuando com o nome "Corpo de Fulano". Qualquer jogador abre com o botão direito e vê os itens numa tela; clicar num item pega só ele, e "Pegar tudo" leva o que couber. Quando esvazia, o corpo some; o dono é avisado quando outro mexe. Morte no void deixa o corpo no último lugar seguro em que o jogador esteve. `/corpo` lista os seus corpos com coordenadas; OPs têm `/corpo lista` e `/corpo abrir <id>`. Os itens ficam salvos no `casa.json`, então sobrevivem a reinício. Opcional: `corpoHoras` na config (padrão 0 = nunca) faz o corpo soltar os itens no chão depois de N horas. **Não use junto com o mod Gravestones**: os dois disputam os itens da morte.

## Horas online e durabilidade

Quando alguém completa 5, 10, 25, 50, 100, 150, 200, 250, 300, 400, 500, 600, 750, 1000, 1500 ou 2000 horas de servidor, o chat comemora (e o Discord, se o webhook estiver ligado). Só os marcos novos são anunciados; quem já tinha horas quando o mod viu pela primeira vez só é registrado. Além disso, quando uma ferramenta ou peça de armadura que você está usando chega a 10% e depois a 5% de durabilidade, aparece um aviso na action bar (no 5% também toca um som). `/durabilidade` liga e desliga o aviso para você.

## Backup do casa.json

Uma vez por dia o mod copia o `casa.json` para `config/melbybet/backups/casa-AAAA-MM-DD.json` e mantém os 7 mais recentes (`backupDias` na config; 0 desliga). Se o `casa.json` não abrir ao ligar o servidor, o mod guarda uma cópia dele como `corrompido-...json` antes de começar uma casa nova (antes, esse arquivo era sobrescrito). `/backup` (OP) cria um backup manual na hora (guarda os 10 últimos). Para restaurar: pare o servidor, copie o backup para `config/melbybet/casa.json` e ligue de novo.

## Visual: apelido, título e cor com corantes

`/melby` > Meu perfil > Visual do nick abre um menu: a prévia de como você aparece no chat, o modo **Cor única** ou **Degradê** e os 16 corantes do jogo, cada um com o nome e o código exato da cor (`#3c44aa` para o azul, por exemplo). O corante escolhido fica brilhando como encantado; no degradê, clique um corante para o começo e outro para o fim. Também tem "Cor exata" (digite `#cor1 #cor2` no chat). Todo jogador vê a **placa** no centro, que abre o chat para escrever o próprio apelido e volta ao menu (não vale o nick de outro jogador nem um apelido já em uso). Só OPs escrevem **títulos** (aba Título) (as cores do título também vêm dos corantes). OPs editam apelido, título e cor de outros jogadores em `/melby` > Moderação > jogador > Visual.

## Painel de cores (página web)

A pasta `docs/` tem uma página (`docs/index.html`) para escolher a cor: cor única ou degradê, um quadrado de cor (arrastar para escolher) com barra de matiz, os 16 corantes do jogo, prévia do nick e o código pronto para copiar (`#cor1 #cor2`, e também o formato de placa). Ao clicar em **Cor exata** no menu de visual, o jogador recebe o link dela no chat. Para o link funcionar, ative o GitHub Pages do repositório (Settings > Pages > Deploy from a branch > `main` > `/docs`). O endereço é `painelCoresUrl` na config (padrão `https://d4rk0l4.github.io/melbybet/`; vazio = não mostra o link).

## /find (pegar item do baú)

Olhando para um baú, barril ou outro recipiente (a até 6 blocos), `/find <item>` tira o item de lá e coloca direto no seu inventário. O nome pode ser em português ou em inglês, sem acento e aceita só o começo das palavras (`/find cortador de pedra`, `/find stonecutter`, `/find diam`). Se achar mais de um item parecido, pega o que combina melhor com o que você digitou. Por padrão leva tudo daquele item; `/find 16 tocha` leva só 16. Respeita baú trancado e áreas protegidas, e cada uso fica no console do servidor (quem pegou o quê, e onde). Não funciona no ender chest.

## Barra de busca no baú (opcional, cliente)

Quem colocar o mesmo jar na pasta mods do próprio jogo ganha uma barra de busca acima de baús, barris e shulker boxes. Digite o nome (em português ou inglês, conforme o idioma do jogo): os itens que combinam ficam com borda dourada e os outros escurecem. O botão "Pegar" move todos os que combinam para o inventário, como um shift+clique em cada um. Nada é necessário no servidor além do jar de sempre, e quem não instalar o mod no cliente joga normalmente.
