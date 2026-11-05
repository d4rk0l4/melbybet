# Versões do MelbyBet

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
