# Proposal

## Why

O Vinícius sentiu falta de gráficos ao abrir Relatórios, principalmente de pizza. Hoje a tela só tem barras horizontais por categoria, que não mostram de relance a proporção do todo nem a evolução entre meses.

## What Changes

- Gráfico de pizza (rosca) no topo da lista por categoria em Relatórios, para despesas e para receitas, com o total no centro.
- Tocar numa fatia ou numa linha da lista destaca a categoria no gráfico e mostra nome, valor e percentual no centro; tocar de novo volta ao total.
- Categorias com menos de 3% são agrupadas numa fatia "Outras" no gráfico; a lista abaixo continua mostrando cada uma.
- Gráfico de barras com receitas e despesas dos últimos 6 meses até o mês selecionado.
- Tocar numa barra de mês leva o relatório para aquele mês.
- Respeita "ocultar valores" e tem descrição textual para leitor de tela.

## Capabilities

### New Capabilities

### Modified Capabilities
- `monthly-reports`: adiciona gráfico de pizza por categoria com seleção e gráfico de evolução mensal.

## Impact

Somente a tela Relatórios e o domínio de relatórios (`Reports`). Sem mudança de banco e sem dependências novas: os gráficos são desenhados com Canvas do Compose.
