# Proposal

## Why

Usando a versão release, o Vinícius sentiu falta de duas coisas. Primeiro, quando junta várias corridas de Uber num lançamento só, precisa somar os valores numa calculadora fora do app. Segundo, categorias afins (Plano de saúde, Psicólogo, Seguro de vida; ou Aluguel, Luz, Água) ficam soltas, e não há como ver quanto foi gasto com "Saúde" ou com "Contas básicas" como um todo sem somar à mão.

## What Changes

- **Soma no campo Valor do formulário de transação**: aceitar parcelas separadas por "+" (ex.: `12,50+8,30+15`), mostrar o total enquanto digita e salvar uma única transação com o total. Um botão "+" ao lado do campo insere o sinal, porque o teclado numérico do sistema nem sempre tem essa tecla.
- **Grupos de categorias (opcional)**: o usuário cria grupos ("guarda-chuvas") de despesa ou de receita, com nome, cor e ícone, e coloca neles as categorias do mesmo tipo. Cada categoria fica em no máximo um grupo. Quem não usar grupos não vê diferença.
- **Categorias em Configurações** passam a mostrar as categorias organizadas por grupo, com criação e edição de grupos e escolha do grupo no formulário da categoria.
- **Relatórios**: nova opção "Por grupo" ao lado de "Por categoria". Por grupo, a pizza e a lista mostram cada grupo como um item (categorias sem grupo continuam aparecendo sozinhas). Ao expandir um grupo, a lista mostra o detalhamento por categoria. "Por categoria" continua como hoje.
- **Transações**: novo filtro por categoria ou por grupo, que mostra o total filtrado do mês e, para grupo, o detalhamento por categoria. A partir de Relatórios, um item selecionado pode abrir Transações já filtradas por ele.
- **Busca** passa a encontrar transações também pelo nome do grupo da categoria.
- **Backup** passa a incluir os grupos (formato 2). Arquivos no formato 1 continuam sendo aceitos.

## Capabilities

### New Capabilities
- `category-groups`: grupos opcionais de categorias. Cobre o cadastro do grupo, quais categorias pertencem a ele, as regras de tipo e o que acontece ao excluir.

### Modified Capabilities
- `transactions`: soma de parcelas no campo Valor, filtro por categoria ou grupo na lista com total filtrado, e busca pelo nome do grupo.
- `monthly-reports`: visão por grupo (pizza e lista com detalhamento) e atalho para Transações filtradas.
- `data-backup`: o backup inclui os grupos e a restauração aceita os formatos 1 e 2. Este capability vem do change `backup-exportacao`, que ainda não foi arquivado.

## Impact

- **Banco**: Room passa da versão 1 para a 2. Cria a tabela de grupos e adiciona a coluna `groupId` (que pode ficar vazia) em `categories`. A migração não perde dados; o schema 2 é exportado.
- **Domínio**: `Money` (somar parcelas), `Reports`/`Charts` (agregação por grupo), `TransactionQueries` (filtro por categoria/grupo, busca) e `FinanceData` (grupos).
- **Backup**: `BackupFormat` e `BackupCodec` passam para o formato 2.
- **UI**: `AmountField`, formulário de transação, Categorias, formulário de categoria, novo formulário de grupo, Relatórios e Transações.
- **Fora do escopo**: orçamentos por grupo, coluna de grupo no CSV, soma no valor de planejamentos/orçamentos e grupos dentro de grupos.
- **Pré-requisito para arquivar**: arquivar `backup-exportacao` antes, para que o spec `data-backup` exista em `openspec/specs/`.
