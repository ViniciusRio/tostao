# Design

## Context

Veja `proposal.md` (Why) e os specs deste change para o comportamento esperado.

Estado atual que molda o desenho:
- `Category` é plana (`id, name, type, colorArgb, icon, order`). O banco Room está na versão 1, com `exportSchema = true` e o schema em `app/schemas/.../1.json`, e não há migrações.
- `Reports.month` agrupa por `categoryId` e devolve `CategorySlice(category, totalCents, percent)`. `Charts.pie` transforma isso em `PieSlice` com a regra de "Outras". `ReportsScreen` controla a seleção por chaves `slice:<i>` e `cat:<id>`.
- `TransactionFilter(month, type, accountId)` e `TransactionQueries.filtered` atendem a lista. A rota `Route.Transactions(accountId)` já abre a lista com uma conta pré-filtrada.
- O backup (`BackupFormat`/`BackupCodec`) está no formato 1. Ele recusa versão maior que a suportada e valida as referências entre registros.
- `AmountField` filtra a entrada para dígitos, `,` e `.`, usa `KeyboardType.Decimal` e valida com `Money.parse`.

## Goals / Non-Goals

**Goals:**
- Os grupos não podem custar nada para quem não usa: sem grupos, telas e relatórios ficam idênticos aos de hoje.
- A agregação por grupo fica no domínio puro e testável, sem lógica na tela.
- A migração do banco preserva todos os dados e é coberta por teste.

**Non-Goals:**
- Orçamento por grupo. Hoje o orçamento escolhe categorias; ligar o orçamento a um grupo fica para depois.
- Grupos aninhados (grupo dentro de grupo) e categoria em mais de um grupo.
- Coluna de grupo no CSV.
- Soma de parcelas fora do formulário de transação (planejamento, orçamento, conta).
- Outras operações além de "+" no campo Valor (subtração, multiplicação, parênteses).

## Decisions

### 1. Grupo como entidade própria, não como "categoria pai"
Nova tabela `category_groups (id, name, type, colorArgb, icon, sortOrder)` e a coluna `categories.groupId TEXT NULL`, com `ForeignKey(onDelete = SET_NULL)` e índice.
- **Alternativa descartada**: `categories.parentId` apontando para outra categoria. Nesse caso a categoria pai também poderia receber transações e o total do "pai" ficaria ambíguo (as transações dele mais as dos filhos?). Também seria preciso impedir ciclos e filhos de filhos. Uma entidade separada deixa claro que o grupo só agrega e torna a regra de um nível óbvia.
- A FK com `SET_NULL` garante no banco a regra "excluir grupo deixa as categorias sem grupo". Mesmo assim, o repositório limpa `groupId` de forma explícita dentro da mesma transação, como já faz com `uncategorize*`, para não depender de `PRAGMA foreign_keys`.

### 2. Migração 1 → 2 manual
Uma `Migration(1, 2)` com `CREATE TABLE category_groups ...` e a reconstrução de `categories`. O SQLite não aceita adicionar FK com `ALTER TABLE`, então o caminho é criar `categories_new`, copiar, apagar a antiga e renomear, preservando o índice e as FKs de `budget_categories`. Exportar `2.json` e validar com `MigrationTestHelper` no androidTest.
- **Alternativa descartada**: `AutoMigration`. Ela não cobre a reconstrução com FK nova sem uma spec manual, e a migração manual fica explícita e testável.
- Sem `fallbackToDestructiveMigration`: perder dados nunca é aceitável aqui.

### 3. Domínio: `CategoryGroup` e agregação genérica
- `CategoryGroup(id, name, type, colorArgb, icon, order)`. `Category` ganha `groupId: String? = null`. `FinanceData` ganha `groups` e `groupById`.
- `Reports.month` mantém `expenseByCategory`/`incomeByCategory` e passa a calcular também `expenseByGroup`/`incomeByGroup: List<GroupSlice>`. Cada `GroupSlice` tem `group: CategoryGroup?` (null quando é uma categoria sem grupo), `category: Category?` (só nesse caso), `totalCents`, `percent` e `children: List<CategorySlice>`, com o percentual de cada filho calculado dentro do grupo.
- `Charts.pie` passa a receber uma lista neutra de itens (`label`, `cents`, `color`, `key`) em vez de `CategorySlice`. Assim a regra de "Outras" e os percentuais são os mesmos nas duas visões, sem código duplicado. Os testes de `ChartsTest` são adaptados.
- **Alternativa descartada**: calcular o agrupamento na tela. Ficaria sem teste e repetiria a lógica de percentuais.

### 4. Chave de seleção e filtro unificados
Um tipo `CategoryScope` no domínio, `sealed`, com os casos `All`, `None` ("Sem categoria"), `Category(id)` e `Group(id)`, com `matches(t, data)`. Ele é usado em três lugares:
- `TransactionFilter` ganha `scope: CategoryScope = All`.
- `ReportsScreen` usa o scope como chave de seleção (substitui `cat:<id>`). Se o scope for de grupo, ele já serve direto para "Ver transações".
- `Route.Transactions` ganha `month`, `type` e `scope` opcionais. Na rota, o scope é guardado como string estável (`all`, `none`, `cat:<id>`, `grp:<id>`) para continuar compatível com `rememberSaveable`.
- O resumo do filtro (total e detalhamento por categoria) sai de uma função de domínio `TransactionQueries.scopeSummary(list, scope, data)`.

### 5. UI do filtro de categoria em Transações
Um chip "Categoria: Todas ▾" numa terceira linha de filtros abre um `ModalBottomSheet` com "Todas", "Sem categoria", cada grupo do tipo atual (com as categorias dele recuadas embaixo) e depois as categorias sem grupo. Com o tipo "Todas" ou "Transferências", a lista mostra os dois tipos de categoria, separados por título. Escolher Transferências com scope de categoria é permitido, e o resultado é a lista vazia (transferência não tem categoria).
- **Alternativa descartada**: mais uma `LazyRow` de chips. Com 15 ou mais categorias e grupos, a rolagem horizontal fica difícil e não mostra a hierarquia.

### 6. Soma no campo Valor
- `Money.parseSum(input): Long?` divide por `+`, aceita até 50 parcelas, faz o parse de cada uma com `Money.parse` e recusa parcela vazia ou total acima de `MAX_CENTS`. Também há `Money.isSum(input)` para decidir se mostra o "= total".
- `AmountField` ganha `allowSum: Boolean = false`. Com ele ligado, o filtro de entrada aceita `+`, o limite de caracteres sobe de 18 para 200, aparece um `trailingIcon` "Somar" (ícone Add, com descrição para leitor de tela) que acrescenta `+` e move o cursor para o fim, e o `supportingText` mostra "= R$ 35,80" quando há mais de uma parcela válida. O campo passa a usar `TextFieldValue` para controlar o cursor.
- Só o formulário de transação liga `allowSum`. O valor salvo é `parseSum(amount)`. Ao editar, o campo começa com `Money.plain(total)`, como hoje.
- **Alternativa descartada**: uma calculadora própria em diálogo. Seria mais pesada e mudaria o fluxo rápido de lançamento, que o projeto escolheu de propósito (ver `docs/engenharia-reversa.md`, item 12).

### 7. Backup formato 2
`FORMAT_VERSION = 2`. `BackupData.categoryGroups: List<CategoryGroupDto> = emptyList()` e `CategoryDto.groupId: String? = null`. Os padrões fazem um arquivo de formato 1 ser lido como "sem grupos". A validação ganha nomes únicos por tipo, referência existente e tipo igual. O `toDto` ordena os grupos por id, como as demais listas. A contagem da confirmação ganha "N grupos" quando N > 0.

### 8. Categorias e formulários
- `CategoriesScreen` renderiza seções: cada grupo como cabeçalho clicável (abre o formulário do grupo), as categorias dele, e depois "Sem grupo". Um botão "Novo grupo" fica ao lado do "+" de categoria existente.
- `CategoryGroupFormScreen` (nova rota `Route.GroupForm(id, type)`) tem nome, tipo (travado depois de criado se o grupo tiver categorias), cor, ícone e a lista de categorias do tipo com checkbox. Ao salvar, `repo.saveGroup(group, memberIds)` grava numa transação de banco: o upsert do grupo, `groupId = null` em quem saiu e `groupId = id` em quem entrou (o que move a categoria de outro grupo).
- `CategoryFormScreen` ganha a seção "Grupo" com chips ("Sem grupo" e os grupos do tipo). Trocar o tipo limpa o grupo escolhido.
- O grupo é sempre o da categoria, e a transação não tem grupo próprio. Assim, ao agrupar uma categoria antiga, todas as transações dela passam a contar no grupo sem precisar editar uma por uma. Ter um grupo na transação foi descartado: abriria a possibilidade de transação e categoria discordarem, e cada lançamento antigo precisaria ser editado.
- `CategoryChips` ganha a versão agrupada (título por grupo e as categorias sem grupo no fim), usada nos formulários de transação e de planejamento. Sem grupos, renderiza exatamente como hoje.
- O diálogo "Nova categoria" do formulário ganha a escolha de grupo (padrão "Sem grupo").

## Risks / Trade-offs

- [A migração reconstrói `categories`, e um erro pode apagar categorias ou quebrar as FKs de `budget_categories`] → teste de migração com dados reais (categorias, orçamento com categorias, transações) verificando contagens e junções depois de migrar. O app só é publicado com esse teste passando.
- [Um backup de formato 2 aberto numa versão antiga do app é recusado como "versão mais nova"] → é o comportamento esperado do spec atual. A mensagem já orienta a atualizar o app.
- [A tecla "+" no teclado `Decimal` varia por fabricante] → o botão "Somar" no próprio campo não depende do teclado.
- [Mudar `Charts.pie` para itens genéricos pode alterar a pizza por categoria] → os testes atuais de `ChartsTest` continuam valendo, adaptados à nova entrada sem mudar os resultados esperados.
- [Mais uma linha de filtros em Transações ocupa altura] → um único chip abre o seletor, então a linha é curta.

## Migration Plan

1. Antes de arquivar este change: arquivar `backup-exportacao`, para que `openspec/specs/data-backup` exista.
2. O app atualizado roda a migração 1 → 2 na primeira abertura. Não há passo manual.
3. Rollback: voltar para uma versão anterior do app não abre um banco v2 (o Room recusa um downgrade). A orientação é fazer backup antes, e um backup de formato 1 continua restaurável na versão nova.
