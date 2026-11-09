# Design

## Context

Relatórios já tem `Reports.month` com `expenseByCategory`/`incomeByCategory` ordenados. Não há biblioteca de gráficos no projeto.

## Goals / Non-Goals

**Goals:** pizza com seleção e barras mensais desenhadas com `Canvas`; cálculo de fatias e série mensal em funções puras testáveis.

**Non-Goals:** animações elaboradas, zoom, gráficos na tela Início, exportar imagem.

## Decisions

- **Canvas do Compose em vez de biblioteca** (Vico, MPAndroidChart): duas formas simples não justificam uma dependência; mantém o APK e o controle de acessibilidade.
- **Agrupamento "Outras" no domínio** (`Charts.pie`): regra de 3% e "duas ou mais" testável sem UI. Percentuais calculados sobre centavos com arredondamento por maior resto, para a soma dar 100%.
- **Toque na pizza**: converter o ponto em ângulo (0° no topo, sentido horário) e raio; dentro do anel seleciona a fatia, fora limpa.
- **Série mensal** (`Reports.trend`): reaproveita `Reports.month` para cada mês, garantindo as mesmas regras dos totais.
- **Seleção** guardada em estado da tela com chave (mês, tipo), limpa ao trocar qualquer um deles.
- **Fatias com espaçamento** de 1,5° entre elas e cor de superfície no centro; fatia selecionada desenhada com anel mais grosso.

## Risks / Trade-offs

- [Cores repetidas entre categorias personalizadas] → nome no centro e lista abaixo; cor nunca é o único identificador.
- [Fatias muito finas difíceis de tocar] → a lista abaixo também seleciona.
