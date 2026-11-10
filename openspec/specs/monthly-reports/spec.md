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
O relatório SHALL listar despesas (e, em aba separada, receitas) agrupadas por categoria em ordem decrescente de valor, com valor, percentual do total e barra proporcional; transações sem categoria MUST aparecer em "Sem categoria".

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
A tela Relatórios SHALL mostrar, acima da lista por categoria, um gráfico de pizza em formato de rosca com uma fatia por categoria do tipo selecionado (despesas ou receitas), na cor da categoria e em ordem decrescente de valor ("Outras" entra na posição do seu total). O centro MUST mostrar o total do tipo no mês. Categorias abaixo de 3% do total MUST ser agrupadas numa única fatia "Outras" quando houver duas ou mais delas; a lista abaixo continua mostrando cada categoria separadamente.

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
Tocar numa fatia ou na linha correspondente da lista SHALL destacar essa fatia e mostrar no centro o nome, o valor e o percentual da categoria. Tocar de novo na mesma fatia, ou fora do anel, MUST voltar a mostrar o total. Trocar de mês ou de tipo MUST limpar a seleção.

#### Scenario: Tocar em Mercado
- **WHEN** o usuário toca na fatia de Mercado
- **THEN** a fatia fica destacada e o centro mostra "Mercado", "R$ 600,00" e "60%"

#### Scenario: Tocar numa categoria agrupada na lista
- **WHEN** o usuário toca na linha "Saúde" (1%), que está dentro de "Outras"
- **THEN** a fatia "Outras" fica destacada e o centro mostra "Saúde", o valor e o percentual de Saúde

#### Scenario: Tocar no espaço entre fatias ou no centro
- **WHEN** o usuário toca no vão entre duas fatias ou no buraco da rosca
- **THEN** a seleção é limpa e o centro volta ao total

#### Scenario: Tocar em Outras
- **WHEN** o usuário toca na fatia "Outras"
- **THEN** o centro mostra "Outras", a soma das categorias agrupadas e o percentual somado

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
