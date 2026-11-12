# monthly-reports Specification

## Purpose
Mostrar para onde o dinheiro foi e de onde veio em cada mês, sem configuração prévia.

## Requirements

### Requirement: Relatório mensal padrão
Ao abrir Relatórios, o app SHALL mostrar o mês atual sem exigir filtro: total de receitas, total de despesas, resultado (receitas menos despesas) e quantidade de transações. Transferências e ajustes de saldo MUST NOT entrar nesses totais nem na comparação mensal.

#### Scenario: Abrir relatório
- **WHEN** o usuário abre Relatórios
- **THEN** vê os totais do mês atual imediatamente

### Requirement: Despesas por categoria
Na visão "Por categoria" (a padrão; ver "Visão por grupo"), o relatório SHALL listar despesas (e, em aba separada, receitas) agrupadas por categoria em ordem decrescente de valor, com valor, percentual do total e barra proporcional; transações sem categoria MUST aparecer em "Sem categoria".

#### Scenario: Ranking de categorias
- **WHEN** o mês tem R$ 600,00 em Mercado e R$ 400,00 em Lazer
- **THEN** Mercado aparece primeiro com 60% e Lazer com 40%

### Requirement: Comparação com mês anterior
O relatório SHALL mostrar a variação das despesas totais em relação ao mês anterior, em valor e percentual.

#### Scenario: Gasto maior
- **WHEN** o mês anterior teve R$ 1.000,00 de despesas e o atual R$ 1.200,00
- **THEN** o app mostra "+R$ 200,00 (+20%) em relação ao mês anterior"

#### Scenario: Mês anterior sem despesas
- **WHEN** o mês anterior não teve despesas
- **THEN** o app informa que não há base de comparação, sem mostrar percentual

### Requirement: Gráfico de pizza por categoria
Na visão "Por categoria" (a padrão; ver "Visão por grupo"), a tela Relatórios SHALL mostrar, acima da lista por categoria, um gráfico de pizza em formato de rosca com uma fatia por categoria do tipo selecionado (despesas ou receitas), na cor da categoria e em ordem decrescente de valor ("Outras" entra na posição do seu total). O centro MUST mostrar o total do tipo no mês. Categorias abaixo de 3% do total MUST ser agrupadas numa única fatia "Outras" quando houver duas ou mais delas; a lista abaixo continua mostrando cada categoria separadamente.

#### Scenario: Pizza de despesas
- **WHEN** o mês tem R$ 600,00 em Mercado, R$ 380,00 em Lazer, R$ 10,00 em Saúde e R$ 10,00 em Educação
- **THEN** a pizza mostra Mercado (60%), Lazer (38%) e Outras (2%), com "R$ 1.000,00" no centro

#### Scenario: Uma só categoria pequena
- **WHEN** apenas uma categoria fica abaixo de 3%
- **THEN** ela aparece como fatia própria, sem criar "Outras"

#### Scenario: Mês sem dados
- **WHEN** o tipo selecionado não tem transações no mês
- **THEN** o gráfico não é exibido e a tela mostra o estado vazio

### Requirement: Seleção de fatia
Tocar numa fatia SHALL destacar essa fatia e mostrar no centro o nome, o valor e o percentual da categoria, e MUST destacar na lista a linha (ou as linhas, no caso de "Outras") correspondente. Tocar de novo na mesma fatia, ou fora do anel, MUST voltar a mostrar o total. Trocar de mês ou de tipo MUST limpar a seleção. Tocar numa linha da lista MUST NOT alterar a seleção: abre as transações da categoria (ver "Transações da categoria").

#### Scenario: Tocar em Mercado
- **WHEN** o usuário toca na fatia de Mercado
- **THEN** a fatia fica destacada, a linha de Mercado fica destacada e o centro mostra "Mercado", "R$ 600,00" e "60%"

#### Scenario: Tocar numa categoria agrupada na lista
- **WHEN** o usuário toca na linha "Saúde" (1%), que está dentro de "Outras"
- **THEN** a seleção da pizza não muda e abre a tela de transações de Saúde

#### Scenario: Tocar no espaço entre fatias ou no centro
- **WHEN** o usuário toca no vão entre duas fatias ou no buraco da rosca
- **THEN** a seleção é limpa e o centro volta ao total

#### Scenario: Tocar em Outras
- **WHEN** o usuário toca na fatia "Outras"
- **THEN** o centro mostra "Outras", a soma das categorias agrupadas e o percentual somado, e as linhas dessas categorias ficam destacadas

### Requirement: Evolução mensal
A tela Relatórios SHALL mostrar um gráfico de barras com receitas e despesas de cada um dos 6 meses que terminam no mês selecionado, com legenda e rótulo abreviado do mês. Tocar num mês MUST abrir o relatório daquele mês. Os valores seguem as mesmas regras dos totais (só transações efetivadas, sem transferências nem ajustes).

#### Scenario: Navegar pela barra
- **WHEN** o relatório está em novembro e o usuário toca na barra de agosto
- **THEN** o relatório passa a mostrar agosto

### Requirement: Gráficos com valores ocultos
Com "ocultar valores" ligado, os gráficos SHALL continuar mostrando proporções, mas valores em reais MUST aparecer como "R$ •••••".

#### Scenario: Centro oculto
- **WHEN** os valores estão ocultos e o usuário seleciona Mercado
- **THEN** o centro mostra "Mercado", "R$ •••••" e "60%"

### Requirement: Gráficos acessíveis
Cada gráfico SHALL ter uma descrição resumida para leitor de tela, cada mês da evolução MUST ter descrição própria, e a cor MUST NOT ser a única forma de identificar uma categoria.

#### Scenario: Leitor de tela na pizza
- **WHEN** o TalkBack foca o gráfico de despesas
- **THEN** anuncia algo como "Gráfico de despesas: Mercado 60%, Lazer 38%, Outras 2%"

#### Scenario: Leitor de tela na evolução
- **WHEN** o TalkBack foca a barra de agosto
- **THEN** anuncia o mês com os valores de receitas e despesas, ou "valores ocultos"

### Requirement: Transações da categoria
Tocar numa linha da lista por categoria em Relatórios SHALL abrir a tela "Transações da categoria" com as transações daquela categoria no mês e no tipo do relatório, seguindo as mesmas regras dos totais (só efetivadas, sem transferências nem ajustes de saldo). A tela MUST mostrar no topo o ícone, o nome, o total e a quantidade de transações, e abaixo a lista agrupada por dia, mais recentes primeiro. Tocar numa transação MUST abrir a edição dela.

#### Scenario: Abrir Mercado
- **WHEN** novembro tem 3 despesas em Mercado somando R$ 600,00 e o usuário toca na linha Mercado
- **THEN** abre "Mercado" com R$ 600,00, "3 transações" e as 3 despesas agrupadas por dia

#### Scenario: Sem categoria
- **WHEN** o usuário toca na linha "Sem categoria"
- **THEN** a tela lista as transações do tipo sem categoria no mês, incluindo as que apontam para uma categoria que não existe mais

#### Scenario: Categoria agrupada em Outras
- **WHEN** Saúde (1%) está dentro da fatia "Outras" e o usuário toca na linha Saúde
- **THEN** a tela abre só com as transações de Saúde

#### Scenario: Só o tipo do relatório
- **WHEN** a categoria tem despesas e receitas no mês e o relatório está em Receitas
- **THEN** a tela lista só as receitas

#### Scenario: Valores ocultos
- **WHEN** "ocultar valores" está ligado
- **THEN** o total e os valores das transações aparecem como "R$ •••••"

#### Scenario: Leitor de tela na linha
- **WHEN** o TalkBack foca a linha Mercado em Relatórios
- **THEN** anuncia o nome, o valor e o percentual, com a ação "Ver transações"

#### Scenario: Leitor de tela com valores ocultos
- **WHEN** "ocultar valores" está ligado e o TalkBack foca a linha Mercado
- **THEN** anuncia o nome, "valor oculto" e o percentual, com a ação "Ver transações"

#### Scenario: Voltar
- **WHEN** o relatório estava em agosto, em Receitas, e o usuário abre uma categoria e volta
- **THEN** Relatórios mostra agosto, em Receitas

#### Scenario: Categoria removida
- **WHEN** a categoria deixa de existir enquanto a tela está aberta
- **THEN** o app volta para Relatórios

#### Scenario: Sem categoria não é removida
- **WHEN** a tela "Sem categoria" está aberta e os dados mudam
- **THEN** a tela continua aberta, recalculada

### Requirement: Total da tela da categoria
O total da tela "Transações da categoria" SHALL ser igual ao valor da linha da mesma categoria em Relatórios para o mês, o tipo e os dados atuais, inclusive depois de trocar o mês na tela ou editar uma transação. O subtotal de cada dia MUST ser exibido sem sinal, como o total.

#### Scenario: Subtotal do dia
- **WHEN** em 10/11 há duas despesas de Mercado de R$ 50,00 e R$ 30,00
- **THEN** o subtotal do dia aparece como R$ 80,00, sem sinal de menos

#### Scenario: Transação futura
- **WHEN** existe uma despesa de Mercado agendada para mais tarde hoje ou para depois de hoje no mês atual
- **THEN** ela não aparece na tela nem entra no total, como no relatório

#### Scenario: Trocar de mês na tela
- **WHEN** o usuário está em "Mercado" de novembro e toca na seta do mês anterior
- **THEN** a tela passa a mostrar Mercado de outubro; sem despesas, mostra o estado vazio

#### Scenario: Editar e voltar
- **WHEN** o usuário abre uma transação da tela, muda a categoria dela e salva
- **THEN** ao voltar a transação não aparece mais na lista e o total é recalculado

### Requirement: Relatório por conta
A tela Relatórios SHALL oferecer um filtro de conta com "Todas as contas" (padrão) e uma opção por conta. Com uma conta escolhida, todo o relatório (totais, resultado, comparação, evolução, pizza e lista) MUST considerar só as receitas e despesas dela, com as demais regras inalteradas. A escolha MUST ser mantida ao trocar de mês ou de tipo.

#### Scenario: Ver só o Pix
- **WHEN** em novembro há R$ 300,00 de despesas no BTG e R$ 200,00 no Pix, e o usuário toca no chip "Pix"
- **THEN** o relatório mostra R$ 200,00 de despesas e a pizza e a lista mostram só as categorias das despesas do Pix

#### Scenario: Trocar de conta
- **WHEN** o relatório está filtrado por "Pix" e o usuário toca em "BTG"
- **THEN** o relatório passa a mostrar só o BTG

#### Scenario: Voltar para todas
- **WHEN** o usuário toca em "Todas as contas"
- **THEN** o relatório volta a somar todas as contas, como antes

#### Scenario: Evolução da conta
- **WHEN** o relatório está filtrado por "Pix"
- **THEN** as barras dos últimos 6 meses mostram só receitas e despesas do Pix, e tocar num mês mantém o filtro

#### Scenario: Transferência entre contas
- **WHEN** o usuário transferiu R$ 100,00 do BTG para o Pix e filtra por qualquer uma das duas
- **THEN** a transferência não entra como receita nem como despesa da conta

#### Scenario: Conta fora do saldo total
- **WHEN** existe a conta "Investimentos", marcada para não entrar no saldo total, com despesas no mês
- **THEN** "Todas as contas" inclui essas despesas, como hoje, e o chip "Investimentos" mostra só elas

#### Scenario: Ajuste de saldo na conta
- **WHEN** o Pix teve um ajuste de saldo de R$ 20,00 no mês e o relatório está filtrado por Pix
- **THEN** o ajuste não entra nas receitas nem nas despesas do Pix

#### Scenario: Conta excluída
- **WHEN** o relatório está filtrado por uma conta e ela é excluída
- **THEN** o filtro volta para "Todas as contas"

#### Scenario: Conta sem movimento no mês
- **WHEN** a conta escolhida não tem receitas nem despesas no mês
- **THEN** os totais aparecem zerados e a seção por categoria mostra o estado vazio

#### Scenario: Transações da categoria com conta
- **WHEN** o relatório está filtrado por "Pix" e o usuário toca na linha Mercado
- **THEN** a tela de transações de Mercado mostra só as despesas de Mercado no Pix, com o total igual ao da linha

#### Scenario: Voltar mantém a conta
- **WHEN** o relatório está filtrado por "Pix", o usuário abre uma categoria e volta
- **THEN** Relatórios continua filtrado por "Pix"

### Requirement: Visão por grupo
Quando existir ao menos um grupo do tipo selecionado, Relatórios SHALL oferecer "Por categoria" (como hoje) ou "Por grupo", mantendo a escolha ao trocar de mês ou de tipo. Em "Por grupo", cada grupo com movimento MUST virar um item com o total das suas categorias, e categorias sem grupo (inclusive "Sem categoria") continuam como itens próprios. Pizza e lista MUST usar esses itens com as mesmas regras da visão por categoria.

#### Scenario: Total de Saúde
- **WHEN** o mês tem R$ 500,00 em Plano de saúde, R$ 300,00 em Psicólogo (ambas no grupo Saúde) e R$ 200,00 em Lazer (sem grupo), e o usuário escolhe "Por grupo"
- **THEN** a pizza mostra Saúde (80%) e Lazer (20%), e a lista mostra Saúde R$ 800,00 e Lazer R$ 200,00

#### Scenario: Voltar a ver separado
- **WHEN** o usuário está em "Por grupo" e escolhe "Por categoria"
- **THEN** a pizza e a lista voltam a mostrar Plano de saúde, Psicólogo e Lazer separadamente

#### Scenario: Sem grupos
- **WHEN** não existe grupo de despesa
- **THEN** a escolha "Por categoria"/"Por grupo" não aparece e o relatório é o atual

#### Scenario: Receita em categoria de despesa agrupada
- **WHEN** existe um grupo de receita (ex.: Renda), há uma receita de R$ 50,00 na categoria Psicólogo (despesa, grupo Saúde) e o usuário vê Receitas "Por grupo"
- **THEN** ela aparece como item próprio "Psicólogo", fora de qualquer grupo

#### Scenario: Grupo sem movimento
- **WHEN** o grupo "Contas básicas" não teve despesas no mês
- **THEN** ele não aparece na pizza nem na lista daquele mês

### Requirement: Detalhamento do grupo
Na visão "Por grupo", a linha de um grupo SHALL ter um botão próprio de expandir/recolher que mostra cada categoria do grupo com movimento no mês, com valor e percentual dentro do grupo, em ordem decrescente de valor. Expandir ou recolher MUST NOT mudar a seleção da pizza nem abrir outra tela. Tocar numa categoria expandida MUST abrir "Transações da categoria" para aquela categoria.

#### Scenario: Expandir Saúde
- **WHEN** o usuário expande o grupo Saúde do exemplo anterior
- **THEN** aparecem Plano de saúde R$ 500,00 (63%) e Psicólogo R$ 300,00 (37%)

#### Scenario: Tocar numa categoria do grupo
- **WHEN** o usuário toca em Psicólogo dentro de Saúde expandido
- **THEN** abre "Transações da categoria" de Psicólogo com R$ 300,00, e a seleção da pizza não muda

### Requirement: Transações do grupo
Na visão "Por grupo", tocar na linha de um grupo SHALL abrir a tela "Transações da categoria" (change `transacoes-da-categoria`) em modo grupo: as transações de todas as categorias do grupo no mês e no tipo do relatório, com o nome, o ícone e o total do grupo no topo e o detalhamento por categoria com movimento. O total MUST ser igual ao da linha do grupo. Linhas de categoria sem grupo e de "Sem categoria" MUST se comportar como na visão por categoria.

#### Scenario: Abrir transações de Saúde
- **WHEN** o usuário está em "Por grupo" em novembro e toca na linha do grupo Saúde
- **THEN** abre "Saúde" com R$ 800,00, Plano de saúde R$ 500,00 e Psicólogo R$ 300,00 no detalhamento e as transações das duas categorias agrupadas por dia

#### Scenario: Grupo com conta
- **WHEN** o relatório está filtrado pela conta Pix e o usuário toca na linha do grupo Saúde
- **THEN** a tela mostra só as despesas de Saúde no Pix, com o total igual ao da linha

#### Scenario: Grupo excluído com a tela aberta
- **WHEN** a tela de transações do grupo Saúde está aberta e o grupo é excluído
- **THEN** o app volta para Relatórios, sem mostrar as transações de todas as categorias

#### Scenario: Outras na visão por grupo
- **WHEN** na visão "Por grupo" os itens Educação (sem grupo, 1%) e Pets (grupo, 2%) formam a fatia "Outras" e o usuário toca nela
- **THEN** o centro mostra "Outras" e a soma, e as linhas de Educação e Pets ficam destacadas; tocar depois na linha Pets abre só as transações do grupo Pets

#### Scenario: Fatia do grupo
- **WHEN** o usuário toca na fatia Saúde
- **THEN** a fatia fica destacada e o centro mostra "Saúde", o valor e o percentual, sem abrir outra tela
