# MelbyCraft

Mod de servidor (Fabric, Minecraft 26.3) da Gangue do Melby: casa de apostas satírica, visual, corpos, /find e vários recursos de servidor. **Só o servidor precisa dele**: os jogadores entram normalmente, sem instalar nada (quem quiser a barra de busca nos baús instala o mesmo jar no cliente, opcional).

## Como pegar o .jar (sem Java no PC)

1. Crie um repositório no GitHub e suba **todo o conteúdo desta pasta** (inclusive `.github`).
2. Aba **Actions** > workflow **build**. Ele roda sozinho a cada push; também dá pra clicar em *Run workflow*.
3. Quando ficar verde, abra a execução e baixe o artefato **melbybet-jar**. Dentro vem o `melbybet-1.0.0.jar` (o outro, `-sources`, não serve pro servidor).
4. Se ficar vermelho, copie o erro do passo **build** e me mande.

## Instalar no servidor (teste num servidor separado primeiro)

Na pasta `mods` do servidor: Fabric Loader, a **Fabric API** mais recente para 26.3 e o `melbybet-2.0.0.jar`. O sgui (menus) já vai dentro do jar.

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

## Copiar texto de placas (`/placa copiar`, `/placa colar`)

Olhando para uma placa, `/placa copiar` mostra as 4 linhas no formato de códigos (`&c`, `&#ff8a24`, degradês `<#ff0000-#0000ff>texto</>`). Clicar na linha copia o comando pronto (`/placa 1 ...`) para a área de transferência, e clicar no código cinza copia só o texto. As 4 linhas também ficam guardadas: olhe para outra placa e use `/placa colar`. Não precisa de nada no cliente. Placas encerradas não recebem colagem.

## Histórico de placas (`/placa historico`, `/placa restaurar <id>`)

Toda edição de placa (escrevendo na placa ou pelos comandos `/placa`) é registrada: quem, quando e como ficou o texto. Olhando para a placa, `/placa historico` mostra as 10 últimas versões (de até 20 guardadas), cada uma com o botão **[restaurar]**. `/placa restaurar <id>` devolve o texto daquela versão, olhando para a mesma placa (e o mesmo lado). **OP** restaura qualquer versão, inclusive a original (de antes do registro começar); **jogador comum** só restaura versões que ele mesmo escreveu. A restauração também entra no histórico. Quebrar a placa apaga o histórico dela. Só vale para edições feitas depois desta versão. Dados em `config/melbybet/placas-historico.json`.

## Etiquetas de baú (`/bau`)

Olhando para um baú, barril ou shulker box (a até 6 blocos):

- `/bau nome <texto>`, `/bau descricao <texto>`, `/bau categoria <texto>` etiquetam (texto vazio não existe: use `/bau limpar` para apagar tudo). Limites: nome 32, descrição 100, categoria 20 letras.
- Ao abrir um baú etiquetado, a etiqueta aparece na barra de ação. O Minecraft não deixa mostrar nada ao passar o mouse sobre o bloco, então é só ao abrir, ou com `/bau info`.
- `/bau lista [categoria]` mostra os seus baús etiquetados, com coordenadas que copiam ao clicar. `/bau buscar <texto>` procura por nome, descrição ou categoria em todos os baús etiquetados, de qualquer jogador (sem diferenciar acento).
- O **primeiro a etiquetar vira o dono**: só ele (ou um OP) muda ou apaga a etiqueta. Até 200 baús por jogador.
- Baú duplo conta como um só. Quebrar o baú apaga a etiqueta (se era um duplo, ela passa para a metade que sobrou). Dados em `config/melbybet/baus.json`.

## Marcador de áreas (`/marcador`)

Escolhe uma região (dois cantos) para usar nos recursos de construção. Não precisa de nada no cliente.

- **Varinha:** `/marcador varinha` entrega uma vareta chamada "Varinha de Marcação". Clique esquerdo num bloco marca o canto 1, clique direito marca o canto 2 (não quebra nem abre nada enquanto você segura a varinha).
- **Comandos:** `/marcador pos1` e `/marcador pos2` marcam o bloco para o qual você olha (ou onde você está). Também aceitam coordenadas: `/marcador pos1 100 64 -20`.
- **Partículas:** os limites aparecem em verde (os cantos em vermelho e azul), só para você, e só se estiver a até 48 blocos. `/marcador ver` liga ou desliga.
- **Ajustes:** `/marcador info` (tamanho e volume), `expandir <n>`, `contrair <n>`, `altura` (do fundo ao topo do mundo), `limpar`.
- **Áreas salvas:** `/marcador salvar <nome>`, `lista`, `carregar <nome>`, `apagar <nome>` (até 30 por jogador). `compartilhar <nome> <jogador>` deixa outra pessoa ver e carregar a área (ela usa `carregar <seudono>:<nome>`); `descompartilhar` tira o acesso.

A seleção e as áreas ficam em `config/melbybet/marcadores.json` e sobrevivem a reinícios. Em outra dimensão a seleção não vale: trocar de dimensão e marcar um canto descarta o outro.

## Catálogo de blocos (`/catalogo`)

Conta os blocos da área marcada com `/marcador`. Não precisa de nada no cliente.

- `/catalogo` conta tudo que não é ar nem fluido; `/catalogo tudo` inclui ar e fluidos. Portas, plantas altas e camas contam como um só, laje dupla conta dois.
- Mostra, por tipo, a quantidade, a porcentagem e quantas pilhas ou shulkers dá. Os nomes saem no idioma do seu jogo. `/catalogo pagina <n>` passa as páginas.
- A leitura é feita aos poucos, sem travar o servidor, e só dos chunks carregados. Se algum pedaço estava descarregado, o aviso diz quantas posições ficaram de fora: chegue perto e repita. Máximo de 2 milhões de blocos. `/catalogo cancelar` interrompe.
- `/catalogo salvar <nome>`, `lista`, `ver <nome>`, `apagar <nome>` guardam resultados (até 20 por jogador) em `config/melbybet/catalogo.json`.

## Bússola para coordenadas (`/bussola`)

Segurando uma bússola, `/bussola <x> <z>` (também aceita `<x> <y> <z>`, o Y é ignorado, então dá para colar direto o que o `/coords` copia) faz a agulha apontar para aquele ponto. Usa o rastreador de pedra-ímã do próprio jogo: não precisa de nada no cliente e a agulha funciona como a de uma bússola de pedra-ímã comum. Só aponta na dimensão em que o comando foi usado (em outra dimensão a agulha gira). `/bussola reset` devolve a bússola ao normal.

## Botão de novas trocas no villager (opcional, cliente)

Quem tem o mod no cliente vê o botão **Novas trocas** acima da tela de troca do villager. Ele refaz as ofertas, como quebrar e repor a bancada de trabalho, mas só funciona enquanto o villager ainda não foi usado (nível 1, sem experiência e nenhuma troca feita), igual ao jogo normal. O servidor confere tudo, então o botão não dá vantagem depois da primeira troca. Sem o mod no cliente, ou em servidor sem o mod, o botão não aparece.

## Arena de vidro nos duelos

Todo duelo aceito (`/duelo`, de diamantes ou de itens) acontece numa **jaula de vidro no céu do Overworld**, sem comando extra:

1. Ao aceitar, o mod procura um lugar livre no céu (acima do terreno, abaixo do teto do mundo, só em chunks já carregados). A arena só é construída se **todos** os blocos da caixa e de 1 bloco de margem forem ar; qualquer outro bloco descarta o lugar, então nada existente é tocado. Duelos simultâneos respeitam a distância mínima entre arenas. Sem lugar, o duelo não começa e ninguém perde nada.
2. A caixa (15x15x10 **por fora**, ou seja 13x13x8 livres) é construída em poucos ticks. Só depois disso os diamantes saem e os dois são teleportados para lados opostos.
3. Contagem de 5 segundos com títulos na tela e sem dano. Depois o combate é liberado (os 15 minutos máximos contam daí).
4. Durante o duelo: ninguém quebra o vidro (se uma explosão abrir um buraco, o mod repõe), blocos só podem ser colocados dentro da jaula e só pelos duelistas, pérola do Ender e fruta do coro ficam bloqueadas (o único teleporte é o do começo e o do fim), bichos que aparecerem lá dentro são removidos e quem for achado fora da caixa por mais de 2 segundos perde o duelo.
5. No fim: resultado no chat, vencedor (e o perdedor, se ainda vivo) voltam para onde estavam uns 3 segundos depois, o **corpo do perdedor** aparece onde ele estava antes do duelo, e a caixa inteira vira ar (inclusive blocos, água, itens e flechas deixados lá). 
6. Se um jogador sai do jogo no meio, perde o duelo e, ao voltar, é levado para onde estava. Se o servidor reiniciar, `config/melbybet/arenas.json` guarda as arenas pendentes: ao ligar o mod as limpa, cancela os duelos ativos (diamantes devolvidos) e leva de volta quem estava lá dentro assim que entrar.

Configuração (bloco `config` do `casa.json`, sem interface): `arenaAtiva`, `arenaLarg`, `arenaComp`, `arenaAlt`, `arenaMaterial`, `arenaContagemSeg`, `arenaDistMin`, `arenaLimpezaSeg`. Com `arenaAtiva` falso o duelo volta a ser no lugar onde os dois estão.

## Log de duelos, anulação e rastro (staff)

- **Log:** `config/melbybet/duelos-log.jsonl`, uma linha por duelo: id, quem duelou, data e hora, aposta, resultado, eventos (arena, teleporte, contagem, fim) e a origem de cada jogador. `/duelo log` mostra os 10 últimos e `/duelo log <id>` o detalhe (admin e moderador).
- **Aba Duelos no `/melby`** (staff): lista paginada, detalhes, rastro do prêmio e, só para OP, **Anular duelo** (pede o motivo e uma confirmação). Também `/melbybet duelo anular <id> <motivo>` e `/melbybet duelo rastro <id>`.
- **Anulação:** no duelo de diamantes, cada um recebe a aposta de volta; o mod recolhe o prêmio do vencedor (primeiro o crédito pendente, depois inventário e ender chest, se ele estiver online) e o que não achar sai do cofre; sai do placar e do ranking. No duelo de itens, os itens voltam ao perdedor (que precisa estar online): o mod recolhe os equivalentes do vencedor e recria o que não achar. Apostas de terceiros no duelo não são desfeitas. Tudo fica registrado com quem anulou, quando e por quê.
- **Rastro do prêmio:** por `rastroMin` minutos (padrão 30; 0 desliga) depois do duelo, o log registra o que o vencedor faz com o que ganhou: que baú, barril, shulker ou funil abriu e quanto guardou ou pegou (com coordenadas), ender chest, quanto sumiu do inventário e onde, desconexão e morte. É comparação de contagens: não enxerga itens dentro de shulker no inventário, troca direta no chão nem separa o prêmio de itens iguais que o jogador já tinha (nos diamantes, minerar também aparece).
