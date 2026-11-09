# Spec Delta

## Purpose

Permitir que o usuário leve suas transações para uma planilha (Excel, Google Planilhas) num arquivo CSV no padrão brasileiro, para análises que o app não faz.

## ADDED Requirements

### Requirement: Escolher período da exportação
O app SHALL exportar as transações de um período escolhido pelo usuário: "Tudo", "Este ano", "Este mês" ou "Intervalo" (data inicial e final, inclusivas, com a final não anterior à inicial). Transações registradas com data futura dentro do período MUST entrar, e ocorrências planejadas ainda não pagas MUST NOT entrar. Datas MUST ser interpretadas no fuso atual do aparelho.

#### Scenario: Exportar o mês
- **WHEN** o usuário escolhe "Este mês" em novembro de 2026
- **THEN** o arquivo traz só as transações de 01/11/2026 a 30/11/2026

#### Scenario: Período sem transações
- **WHEN** o período escolhido não tem transações
- **THEN** o app informa "Nenhuma transação nesse período" e não abre o seletor de arquivos

#### Scenario: Intervalo invertido
- **WHEN** a data final é anterior à inicial
- **THEN** o botão "Exportar" fica desabilitado e o app explica o motivo

### Requirement: Salvar o CSV
O app SHALL salvar o CSV onde o usuário escolher pelo seletor de arquivos do sistema, sem pedir permissão de armazenamento, com nome sugerido "tostao-transacoes-AAAA-MM-DD.csv", e confirmar com a quantidade exportada.

#### Scenario: Exportação concluída
- **WHEN** o usuário exporta 120 transações e escolhe uma pasta
- **THEN** o arquivo é gravado e o app mostra "120 transações exportadas"

### Requirement: Formato do CSV
O CSV SHALL ter uma linha de cabeçalho e uma linha por transação, em ordem cronológica, com as colunas: Data (DD/MM/AAAA), Hora (HH:MM), Tipo (Receita, Despesa ou Transferência), Valor, Conta, Conta de destino, Categoria, Título, Observação e Ajuste (Sim ou Não). O separador MUST ser ponto e vírgula, o arquivo MUST estar em UTF-8 com BOM, e o valor MUST ter vírgula decimal, sem separador de milhar nem símbolo de moeda.

#### Scenario: Linha de despesa
- **WHEN** existe uma despesa de R$ 1.234,50 em 05/11/2026 às 14:30 na conta "Nubank", categoria "Mercado", título "Feira"
- **THEN** a linha é `05/11/2026;14:30;Despesa;-1234,50;Nubank;;Mercado;Feira;;Não`

#### Scenario: Sinal do valor
- **WHEN** a transação é receita ou transferência
- **THEN** o valor sai positivo; só despesas saem com sinal de menos

#### Scenario: Sem categoria
- **WHEN** a transação não tem categoria ou é transferência
- **THEN** a coluna Categoria fica vazia

### Requirement: Textos seguros no CSV
Campos de texto SHALL ser escapados para que ponto e vírgula, aspas e quebras de linha não quebrem as colunas. Textos que começam com `=`, `+`, `-`, `@`, tabulação ou retorno de carro MUST ser prefixados com apóstrofo, para que a planilha não os execute como fórmula.

#### Scenario: Observação com quebra de linha
- **WHEN** a observação é `Pago em 2x; "à vista" não` com uma quebra de linha
- **THEN** o campo sai entre aspas, com as aspas internas duplicadas, e a planilha mostra o texto numa única célula

#### Scenario: Título parecido com fórmula
- **WHEN** o título é `=SOMA(A1:A9)`
- **THEN** o campo sai como `'=SOMA(A1:A9)`
