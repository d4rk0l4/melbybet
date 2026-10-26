# MelbyBet

Mod de servidor (Fabric, Minecraft 26.2) com a casa de apostas satírica da Gangue do Melby. **Só o servidor precisa dele**: os jogadores entram normalmente, sem instalar nada.

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

## Inventário, ender chest, freeze e chat

| Comando | Quem | O que faz |
|---|---|---|
| `/inv <jogador>` | OP | mostra o inventário (mochila, hotbar, armadura, mão secundária) de um jogador online, só para olhar |
| `/ec <jogador>` | OP | mostra o ender chest de um jogador online, só para olhar (não abre o seu, para não virar ender chest portátil) |
| `/freeze <jogador>` | OP | liga/desliga: o jogador não anda nem pula |
| `/melbybet mod add <nick>` | OP | cria um moderador: usa `/inv`, `/ec` e `/freeze` sem ser OP (`remove` e `lista` também existem) |
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

## Chat

- Por padrão o chat é **local**: só quem está perto (100 blocos, na mesma dimensão) lê. OP muda com `/melbybet raio <blocos>` (`0` = sem limite).
- `/g <mensagem>` manda uma vez no **global**. `/g` sozinho trava o chat no global; `/g` de novo ou `/l` volta ao local. `/l <mensagem>` manda uma vez no local.
- `/tell <nick>` (ou `/msg`, `/w`) sozinho trava o chat naquela conversa privada; `/tell <nick> <mensagem>` manda uma só. `/r` responde ao último. `/g` ou `/l` sai da conversa travada.
- `/cor <#cor1> [#cor2]` ou `/cor fogo` (oceano, neon, floresta, rosa, ouro, gelo, por-do-sol) muda a cor do nick no chat, em degradê. `/cor off` tira. OP: `/melbybet cor <nick> ...`.
- Admin e moderador: `/spy` mostra as mensagens privadas dos outros em tempo real e `/tells <nick>` mostra as últimas 15 (ficam guardadas as últimas 300).
- Frases de entrar e sair: `/melbybet msg entrar add {nick} chegou!`, `... remover <n>`, `... lista` (o mesmo com `sair`). Uma é sorteada a cada vez.

O degradê do nick aparece só no chat; no tab e acima da cabeça o Minecraft não permite.

`/inv` e `/ec` funcionam também com jogador offline (precisa já ter entrado com o mod instalado).

## Títulos e mensagens

Quem lidera ou perde nas apostas ganha um título automático em degradê antes do nick (tab, cabeça e chat): Rei da Banca, Pé-Frio Oficial, Doador da Casa, Profeta. Um título dado por OP vale mais que o automático.

As mensagens de entrar e sair do servidor são trocadas por frases da Gangue, sorteadas. Para mudar as frases, edite `msgEntrar` e `msgSair` no `casa.json` (use `{nick}` onde vai o nome), com o servidor parado.

## Config (`config/melbybet/casa.json`, bloco `config`)

`rakePct`, `apostaMin`, `apostaMax`, `avisoGrande` (aposta que vira aviso no chat), `cashoutTaxaPct`, `roster`, `discordWebhook` (URL: resultados vão pro Discord), `pastaMundo` (vazio = usa o `level-name`). Pare o servidor antes de editar.

## O que não foi testado

A lógica de apostas, pagamento, ranking e leitura de stats tem testes (`tests/`, rodam no workflow). **A parte que fala com o Minecraft 26.2 (menus, comandos, eventos) nunca foi compilada nem rodada**: foi escrita lendo o código-fonte do sgui e da Fabric API do 26.2. É esperado que o primeiro build acuse algum nome de método. Mande o erro que eu corrijo.

Também aproximado: "blocos colocados" conta itens usados que não são ferramenta (o vanilla não separa). O caminho das stats mudou entre versões, então o mod procura em `world/players/stats` e `world/stats`.

Ainda não feito: empréstimo do Mineiro, apostas combinadas, bolões automáticos semanais, boss bar e fogos.
