# Versões do MelbyBet

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
