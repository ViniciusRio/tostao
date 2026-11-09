# Proposal

## Why

O Vinícius usa o Ivy Wallet no Galaxy A26 para controlar finanças, mas a engenharia reversa (`docs/engenharia-reversa.md`) mostrou problemas de precisão (valores em ponto flutuante), navegação escondida, textos inconsistentes, ações destrutivas sem desfazer e acessibilidade fraca. Este change cria um app Android próprio, local e escrito do zero, com as funções que ele usa no dia a dia e essas melhorias.

## What Changes

- Novo app Android em Kotlin + Jetpack Compose, somente local (sem conta, nuvem, internet nem backup automático do Android), moeda única BRL.
- Contas com saldo inicial, cor, ícone, inclusão no saldo total e ajuste de saldo.
- Categorias de receita/despesa com conjunto padrão em pt-BR.
- Transações de receita, despesa e transferência, com edição, exclusão com desfazer, busca e filtro por mês.
- Pagamentos planejados (únicos ou recorrentes) mostrando apenas a próxima ocorrência, com Pagar/Pular e desfazer.
- Orçamentos mensais por categoria com progresso e alerta de excesso.
- Relatório do mês por categoria, sem exigir filtro.
- Tela Início com saldo, resumo do mês, próximos/atrasados e transações recentes; opção de ocultar valores.
- Fora do escopo deste change: empréstimos, várias moedas e câmbio, tags, importação/exportação CSV, widgets, bloqueio biométrico, lembretes e sincronização.

## Capabilities

### New Capabilities
- `accounts`: contas financeiras, saldo calculado, ajuste de saldo e inclusão no total.
- `categories`: categorias de receita e despesa, padrão inicial e manutenção.
- `transactions`: registro, edição, exclusão com desfazer, busca e listagem por mês.
- `planned-payments`: regras de pagamento planejado, próxima ocorrência, pagar e pular.
- `budgets`: orçamentos mensais por categoria e progresso.
- `monthly-reports`: resumo mensal de receitas e despesas por categoria.
- `home-overview`: painel inicial, formatação monetária e ocultação de valores.

### Modified Capabilities

## Impact

Repositório novo: módulo Android `:app` (minSdk 26, targetSdk 36), banco Room local, testes unitários do domínio e instrumentados de UI no A26. Nenhum sistema existente é afetado.
