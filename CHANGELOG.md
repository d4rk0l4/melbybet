# Versões do MelbyBet

## 2.22.3
- Mixin dos slots travados agora cobre as duas versões do método de clique do 26.3 (com botão int e com MouseButtonEvent); a que não existir na sua versão é ignorada

## 2.22.2
- **Corrigido crash ao abrir o Minecraft** (Lunar): o mixin dos slots travados mirava um método com duas versões no 26.3; agora aponta para a certa. Mixins do cliente que não casarem deixam de derrubar o jogo, só ficam desligados

## 2.22.1
- A luz dos monstros passou da tecla L (que no jogo é a tela de progressos) para a **Z**. Dá pra trocar em Controles

## 2.22.0
- **Telas no cliente (jar).** `/bet`, `/gestos`, `/duelo` (sem argumentos) e `/cor` agora abrem telas próprias em vez de menus de baú. `/bet`: navegue pelos bolões, veja o pote de cada opção, digite o valor e clique na opção para apostar. `/duelo`: lista de quem está online, valor em diamantes ou duelo de itens. `/cor`: duas cores com prévia ao vivo do degradê no seu nick. Sem o jar, os menus de baú continuam funcionando
- **Avisos no canto da tela.** Convite de duelo aparece num aviso no canto inferior direito
- **Painel no HUD** (canto superior direito): seus bolões apostados com o tempo que falta, duelo em andamento e a contagem da proteção do seu corpo
- Chat em abas ficou de fora: o servidor não marca mensagens locais/globais

## 2.21.0
- **Corpo deitado.** O corpo agora é um manequim com a skin de quem morreu, deitado, com a armadura e os itens das mãos que a pessoa usava. O nome fica em cima. Botão direito abre os itens. A pose é `corpoPose` no casa.json (`sleeping` deitado de costas, `swimming` de bruços, `crouching`, `standing`). Funciona sem o jar
- **Log de corpos** (OP): `/corpo log [nick]` mostra os últimos eventos: quem morreu e onde, quem pegou o quê do corpo de quem, expirações e recuperações
- **Recuperar itens.** Se o corpo sumir (expirou, foi removido, ou você morreu de novo tentando pegar), os itens que sobraram ficam guardados. Você recupera com `/corpo recuperar` ou pelo botão **Recuperar itens** no histórico de mortes (M). OP devolve com `/corpo recuperar <nick>` mesmo se o corpo ainda existir. Com `corpoHoras` ligado, o corpo que expira guarda os itens em vez de soltar no chão (`corpoGuardarAoExpirar`)
- **Jar do MelbyCraft exigido.** O cliente avisa a versão ao entrar. `/jar modo off|avisar|barrar` (padrão: avisar), `/jar quem` mostra quem está com o jar e a versão. No modo barrar, quem não tem o jar na versão do servidor é removido depois de 12 segundos, com a mensagem do que baixar

## 2.20.0
- **Tempo AFK não conta nas horas online.** O tempo em AFK (manual ou automático, incluindo os minutos parado antes de entrar em AFK) é descontado do tempo online no `/top`, no `/perfil`, na tela de estatísticas e nos marcos de horas
- Estatísticas novas no `/top` (agora 20 categorias, menu maior): pulos, dano causado, peixes pescados, trocas com villagers, diamantes minerados, animais criados, vezes que dormiu e itens encantados
- Tela de estatísticas (jar de cliente, tecla **F7**): suas estatísticas, sua posição e o ranking da Gangue por categoria
- **Travar slots** (jar de cliente): botão ao lado do Organizar. Ligue e clique nos slots do seu inventário (contorno dourado). Travado: o Organizar e o Guardar iguais não mexem, e o shift-clique e o drop não tiram o item
- **Reposição automática** (jar de cliente): quando a pilha na mão acaba (bloco, comida, flecha), puxa a próxima igual do inventário, sem usar slots travados. Dá para desligar em Perfil
- O GitHub avisa no Discord a cada versão nova, mandando o jar e as novidades

## 2.19.1
- O corpo agora abre com a disposição do inventário: armadura e mão secundária na fileira de cima, mochila no meio e hotbar embaixo, cada item no lugar onde estava. Item sem lugar de origem (corpos antigos) vai para o primeiro espaço vazio. O botão **Transferir itens** leva tudo que couber. Funciona em qualquer cliente, sem o jar

## 2.19.0
- Histórico de mortes (jar de cliente, tecla **M**): duas abas, **Corpos ativos** (os que ainda têm itens) e **Histórico** (todas as vezes que você morreu). Cada morte mostra data e hora, dimensão, coordenadas, distância, quantos itens ainda estão no corpo (ou "recuperados"), o tempo de proteção restante e o que você vestia e segurava (cabeça, peito, calça, bota e as duas mãos, com o nome ao passar o mouse). O botão **Itens** abre o corpo. Guarda as últimas 10 mortes

## 2.18.1
- Corpo protegido nos primeiros 30 segundos depois da morte: só o dono (ou um OP) pega os itens; depois qualquer um pega, como antes. O tempo fica em `corpoProtecaoSeg` no casa.json (0 desliga)

## 2.18.0
- Contador de farm (jar de cliente, tecla **V**): liga zerando e mostra no canto o que entrou no inventário e os itens por minuto; aperte de novo para parar. Mexer em baú ou inventário não conta
- `/catalogo` agora conta o que você precisa levar: bandeiras e placas de parede viram a bandeira e a placa, planta em vaso vira vaso mais planta, caules e vinhas viram muda ou semente. A lista de materiais na tela usa a mesma conta. O padrão da bandeira não é contado, só a cor
- `/fotomoldura desfazer` apaga a última foto que você colocou, sem precisar olhar para ela

## 2.17.0
- Luz dos monstros (jar de cliente, tecla **Z**): quadrados no chão até 12 blocos de você. Vermelho = sem luz de bloco e sem céu (monstro nasce a qualquer hora); amarelo = sem luz de bloco mas com céu (nasce à noite)
- Zoom segurando **C** (volta ao normal ao soltar). O F1 do jogo já esconde o HUD
- Foto na parede por arquivo do seu computador (tecla **F8**): cole o caminho ou arraste o arquivo para a tela, escolha tamanho e modo. O jogo reduz a imagem e manda ao servidor em pedaços, então o servidor não precisa de internet. O `/fotomoldura <LxA> <endereço>` continua existindo

## 2.16.0
- Ferramentas de construção (jar de cliente), tecla **G** abre o painel: guia de forma só com contorno (círculo, esfera, elipse, cúpula; centro no bloco olhado ou na sua posição, camada dos pés, estimativa de blocos e pilhas), **régua** (R marca A e B e mostra dx x dy x dz e distância), **espelho** (Y define o plano de simetria no bloco olhado e mostra o bloco espelhado)
- Lista de materiais na tela (U liga/desliga): o `/catalogo hud [nome|limpar]` mostra o que falta de um projeto descontando o inventário
- `/mapart`: grade numerada dos mapas no mundo (`/mapart esconder`)
- Editor de armor stand (K olhando para ele): sliders de pose por parte, giro do corpo, braços, base e invisível, com efeito ao vivo
- Editor de placa (O olhando para ela): 4 linhas com prévia colorida e histórico de versões com Restaurar
- Todas as teclas são configuráveis em Controles > MelbyCraft: Construção

## 2.15.0
- Inventário e baús (jar de cliente): botão **Organizar** no inventário e nos baús, que arruma a mochila e a hotbar pelo seu perfil e junta pilhas iguais. O botão **Perfil...** abre uma tela onde você escolhe o tipo (espada, machado, picareta, pá, comida, blocos e outros 17) e clica nos slots onde ele deve ficar; também dá para "Aprender da hotbar" ou "Aprender de tudo" a partir do inventário atual. Armadura e mão secundária nunca são mexidas. Há opções para o Organizar mexer ou não na hotbar
- Nos baús e shulker boxes: **Guardar iguais** manda para o recipiente o que você tem e que já existe lá dentro (por padrão sem tocar na hotbar), e **Pegar iguais** traz tudo do recipiente de que você já tem pelo menos uma unidade (para quando o inventário encher e avisa)
- Shulker box com itens mostra o conteúdo em grade 9x3 ao passar o mouse, sem abrir
- /bau buscar: quem tem o jar de cliente recebe os baús achados como waypoints temporários amarelos (até 8, somem ao chegar perto ou em 10 minutos)

## 2.14.1
- Barra de busca do baú: com ela em foco, as teclas vão para a caixa. Apertar E não fecha mais o baú e os números 1 a 9 não trocam itens da hotbar enquanto você digita. Esc continua fechando

## 2.14.0
- Waypoints (só no jar de cliente, no mesmo arquivo do mod): lista com tecla B, criar um ponto aqui com N, criar um temporário com J e ligar/desligar todos com H. As teclas aparecem em Opções > Controles, na categoria "MelbyCraft: Waypoints", e podem ser trocadas. Cada waypoint tem nome, inicial, cor, coordenadas e dimensão, e pode ser ligado, editado, apagado e compartilhado no chat. Temporários somem ao chegar perto (raio configurável) ou depois de N minutos
- No mundo cada waypoint aparece com um marcador visível através das paredes, nome, distância e um feixe de luz opcional. No topo da tela há uma bússola com os waypoints (os que ficam atrás aparecem nas pontas). Não há minimapa
- O corpo ganha um waypoint automático para quem morreu, que some quando o corpo é esvaziado. Em /coords, os botões ganham [Waypoint], e quem compartilha um waypoint no chat dá um botão [Waypoint] para todos adicionarem
- OP: /waypoint servidor ligar ou desligar, para liberar ou bloquear os waypoints no servidor inteiro
- /marcador: quem tem o jar de cliente vê o contorno como linhas de verdade, através das paredes, em vez de partículas. Quem não tem continua vendo as partículas

## 2.13.1
- Corpo: quando o dono pega os itens do próprio corpo (um a um ou em "Pegar tudo"), cada item volta para o slot onde estava: hotbar no mesmo lugar, armadura já equipada e item da mão secundária. Se o slot já estiver ocupado, o item vai para o primeiro espaço livre. Outros jogadores que pegam itens do corpo continuam recebendo no primeiro espaço livre, sem bagunçar a hotbar deles. Corpos criados antes desta versão não guardam o slot e se comportam como antes

## 2.13.0
- /fotomoldura <LxA> <endereço> [modo]: coloca uma foto da internet na parede, feita de mapas do jogo em molduras invisíveis (até 6x6 molduras), então quem entra no servidor vê sem instalar nada. Só HTTPS, bloqueia endereços da rede interna, aceita PNG, JPG, GIF (primeiro quadro) e BMP até 8 MB. Modos: cortar, ajustar (faixas pretas) ou esticar. As cores são convertidas para a paleta dos mapas com difusão de erro. O download e a conversão rodam fora da thread principal
- Até 5 fotos por jogador (OP sem limite) e 1500 mapas no servidor. /fotomoldura lista, info, reparar (desgira molduras) e apagar. As molduras são invulneráveis: só o dono ou OP remove, pelo comando

## 2.12.0
- /armorstand: editor de armor stand num menu. Escolhe a parte (cabeça, corpo, braços, pernas) e ajusta X, Y e Z em passos de 1, 5, 15 ou 45 graus; mostrar braços, base, invisível, girar o stand e mover com ajuste fino (1/16 de bloco). Copiar e colar a pose entre stands (shift cola espelhada) e poses guardadas por jogador (até 20)
- Dono: quem coloca um armor stand passa a ser o dono, e só ele (ou OP) edita. Stands de antes do mod não têm dono: só OP edita, ou qualquer jogador com /armorstand reivindicar se o stand estiver vazio. /armorstand dono e transferir <jogador>

## 2.11.0
- /banner: catálogo com 30 estandartes (bandeiras e símbolos), cada um com pré-visualização e a receita passo a passo (base, camadas do tear, corantes e itens de padrão). Construtor para montar o seu camada por camada, com favoritos por jogador. Os estandartes do menu são só para ver: não dá para pegar

## 2.10.0
- /mapart: planejamento de mapas-arte. calc <largura> <altura> [escala] (mapas, blocos, pilhas e shulkers, plano ou com relevo), grade (onde começa e termina o mapa em que você está), marcar (marca esse mapa com o /marcador), info (lê o mapa que você segura) e projetos salvos (projeto, lista, ver, apagar) a partir da grade do jogo

## 2.9.0
- /bau: etiquetas para baús, barris e shulkers (nome, descrição, categoria). Ao abrir um baú etiquetado a etiqueta aparece na barra de ação. /bau info, limpar, lista [categoria] e buscar <texto> (acha baús de qualquer jogador, com coordenadas clicáveis). O primeiro a etiquetar vira dono; OP muda qualquer um. Baú duplo conta como um só; quebrar o baú apaga a etiqueta

## 2.8.0
- /placa historico: lista as últimas versões da placa para a qual você olha (quem escreveu, quando e o texto), com botão [restaurar]. /placa restaurar <id> volta a placa a uma versão: OP restaura qualquer uma, jogador só as que ele mesmo escreveu. Guarda até 20 versões por placa (config/melbybet/placas-historico.json); quebrar a placa apaga o histórico. /placa limpar agora respeita placa encerada e proteção

## 2.7.0
- /placa copiar: mostra o texto da placa para a qual você olha como código (&c, &#hex, degradê), com clique para copiar para a área de transferência (a linha inteira como comando /placa N ... ou só o texto). /placa colar passa as 4 linhas copiadas para outra placa

## 2.6.0
- /catalogo: conta os blocos da área marcada com /marcador, tipo por tipo, com porcentagem e pilhas/shulkers. Lê aos poucos (sem travar o servidor), só chunks carregados, até 2 milhões de blocos. Salvar, listar, ver e apagar projetos

## 2.5.0
- /marcador: escolha uma área com dois cantos (comandos ou Varinha de Marcação), veja os limites em partículas só para você, ajuste (expandir, contrair, altura) e salve áreas com nome, que dá para compartilhar com outros jogadores. Base para os próximos recursos de construção

## 2.4.0
- Log de duelos (config/melbybet/duelos-log.jsonl): id, quem duelou, data e hora, aposta, resultado, eventos da arena e origem dos jogadores. /duelo log [id] para a staff
- Aba Duelos no /melby (staff): lista, detalhes, rastro do prêmio e anulação (só OP anula) com motivo e confirmação; /melbybet duelo anular <id> <motivo>
- Anulação devolve a aposta; recolhe o prêmio do vencedor quando acha (crédito pendente, inventário, ender chest) e o cofre cobre o resto
- Rastro do prêmio: por 30 min (rastroMin) o mod registra o que o vencedor faz com o que ganhou (baús, ender chest, drops)

## 2.3.0
- Arena de vidro nos duelos: todo duelo aceito acontece numa jaula de vidro no céu do Overworld, construída só em lugar totalmente vazio, com contagem de 5 s, proteção do vidro, sem pérola/fruta do coro, e limpeza total no fim (inclusive blocos colocados). O corpo do perdedor aparece onde ele estava antes. Recuperação depois de reinício em config/melbybet/arenas.json

## 2.2.0
- Botão "Novas trocas" na tela do villager (precisa do mod no cliente): refaz as ofertas, como quebrar e repor a bancada, mas só enquanto o villager não foi usado (nível 1, sem experiência e sem troca feita)

## 2.1.0
- /bussola <x> <z> (ou <x> <y> <z>): com uma bússola na mão, a agulha passa a apontar para as coordenadas (o Y é ignorado). /bussola reset volta ao normal

## 2.0.0
- Atualizado para Minecraft 26.3 (Fabric API 0.165.2+26.3, sgui 2.2.1+26.3). Não roda mais na 26.2: para ficar na 26.2, use a 1.6.1

## 1.6.1
- O mod agora se chama MelbyCraft (nome, prefixo do chat, livro de boas-vindas, README e painel). IDs internos, pasta de config e dados salvos continuam os mesmos; o menu de bolões segue como MelbyBet

## 1.6.0
- Barra de busca no topo de baús, barris e shulker boxes (opcional, só no cliente: quem instalar o mesmo jar vê a barra). Digite o nome do item: os que combinam ganham borda dourada e os outros escurecem; o botão "Pegar" leva todos os que combinam para o inventário
- O servidor continua funcionando normalmente sem o mod no cliente; /find segue como alternativa

## 1.5.0
- /find: olhando para um baú, pega o item pelo nome (português ou inglês) direto para o inventário

## 1.4.2
- Todo jogador escreve o próprio apelido (títulos continuam só do OP); apelido não pode ser nick de outro jogador nem repetido
- Painel de cores com quadrado de cor e barra de matiz

## 1.4.1
- Painel web de cores (docs/index.html) e link dele ao escolher "Cor exata"; cores prontas de volta no menu de visual

## 1.4.0
- Novo menu de visual: placa para escrever apelido e título, cor única ou degradê escolhida pelos corantes (com o código da cor e brilho no escolhido)

## 1.3.1
- Backup diário do casa.json (7 dias), /backup manual e cópia de segurança quando o arquivo não abre

## 1.3.0
- /msgmatar com várias mensagens (sorteadas) e uma específica por vítima
- Aviso de horas online (5, 10, 25, 50, 100... horas) e aviso de durabilidade na action bar (/durabilidade)
- Vender tudo no villager: a tela agora mostra a troca esgotada

## 1.2.0
- Corpo: itens da morte ficam num corpo que qualquer um abre e escolhe o que pegar (substitui o mod Gravestones); morte no void deixa o corpo no último lugar seguro
- Mensagem de kill, loteria e placas bonitas (já na 1.1.0 abaixo)

## 1.1.0
- Duelos (diamantes ou itens), apostas nos duelos dos outros e placar de duelos
- Gestos (/gestos, /spin, /point, /wave, /crawl, /sit), /morte, /perfil, /coords, /capsula
- Aposta combinada, avisos automáticos de reinício
- /limpar (limpeza dos itens do chão), /nick (apelido), degradê do nick na tab
- Placas bonitas (/placa), vender tudo no villager (shift+clique)
- Loteria da Gangue (/loteria) e mensagem de kill (/msgmatar)

## 1.0.0
- Versão inicial: bolões, /bet, /top, chat local/global, títulos, /inv /ec
