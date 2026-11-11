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
Nova tabela `category_groups (id, name, type, colorArgb, icon, sortOrder)` e a coluna `categories.groupId TEXT NULL` com índice, **sem chave estrangeira**. É o mesmo padrão de `transactions.categoryId` e `planned_rules.categoryId`, que também não têm FK: a integridade fica no repositório e na validação do backup.
- **Alternativa descartada**: `categories.parentId` apontando para outra categoria. Nesse caso a categoria pai também poderia receber transações e o total do "pai" ficaria ambíguo. Também seria preciso impedir ciclos e filhos de filhos.
- **Alternativa descartada**: `groupId` com FK `SET_NULL`. Declarar a FK por reconstrução de `categories` dispara o `ON DELETE CASCADE` de `budget_categories` (decisão 2). Seria possível declarar com `ADD COLUMN ... REFERENCES`, mas o projeto já não usa FK em `categoryId`, e a regra cabe numa única transação do repositório. Manter o padrão evita depender de `PRAGMA foreign_keys`.
- O repositório mantém a regra "excluir grupo deixa as categorias sem grupo": limpa `groupId` e apaga o grupo na mesma transação de banco.

### 2. Migração 1 → 2 sem reconstruir tabelas
`Migration(1, 2)` só acrescenta estrutura:
```sql
CREATE TABLE IF NOT EXISTS `category_groups` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL,
  `colorArgb` INTEGER NOT NULL, `icon` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`));
ALTER TABLE `categories` ADD COLUMN `groupId` TEXT;
CREATE INDEX IF NOT EXISTS `index_categories_groupId` ON `categories` (`groupId`);
```
O SQL acima é um rascunho. O `CREATE TABLE` e o `CREATE INDEX` MUST ser copiados do `createSql` do `2.json` exportado pelo Room. O `ALTER TABLE ... ADD COLUMN` sai da diferença entre `1.json` e `2.json` (tipo e nulidade da coluna). O `MigrationTestHelper`, ao validar contra o `2.json`, prova que o resultado bate com o schema.
- **Por que não reconstruir `categories`**: a revisão reproduziu em SQLite que `DROP TABLE categories` com as FKs ativas apaga, por cascata, todas as linhas de `budget_categories`. Desligar as FKs com `PRAGMA foreign_keys=OFF` não funciona dentro da transação da migração. `ALTER TABLE ... ADD COLUMN` não toca nas linhas nem nas FKs existentes.
- **Alternativa descartada**: `AutoMigration`. Ela gera o mesmo `ADD COLUMN`, mas a migração manual fica explícita e é testada da mesma forma.
- Sem `fallbackToDestructiveMigration`: perder dados nunca é aceitável aqui.
- **Teste obrigatório, executado e não só compilado**: `MigrationTestHelper` em `connectedDebugAndroidTest` (no aparelho ou no emulador). Ele cria o v1 com contas, categorias, transações, planejamentos e orçamentos com categorias, migra com validação de schema contra `2.json` e confere as contagens de todas as tabelas, inclusive `budget_categories`, o `groupId` nulo e `PRAGMA foreign_key_check` vazio.

### 3. Domínio: `CategoryGroup`, agregação e identidade das fatias
- `CategoryGroup(id, name, type, colorArgb, icon, order)`, com `order` vindo de `nextGroupOrder()` (a ordem de criação). `Category` ganha `groupId: String? = null`. `FinanceData` ganha `groups` e `groupById`.
- `Reports.month` mantém `expenseByCategory`/`incomeByCategory` e passa a calcular também `expenseByGroup`/`incomeByGroup: List<GroupSlice>`. Cada `GroupSlice` tem `key: CategoryScope` (`Group(id)`, `Category(id)` para categoria sem grupo, ou `None`), `label`, `colorArgb`, `icon`, `totalCents`, `percent` e `children: List<CategorySlice>`, com o percentual de cada filho calculado dentro do grupo.
- `Charts.pie` passa a receber `List<ChartItem(key: CategoryScope, label, cents, colorArgb)>`. Ele devolve `PieSlice` com `keys: List<CategoryScope>` no lugar de `categories: List<Category?>`: uma chave por fatia comum e várias em "Outras". A visão por categoria monta os itens com `Category(id)`/`None`, e a visão por grupo, com as chaves acima.
- **Seleção** (alinhada a `transacoes-da-categoria`): só o toque numa **fatia** seleciona; a tela guarda o índice da fatia, e o centro mostra o nome, o valor e o percentual dela ("Outras" mostra a soma). As linhas destacadas são as das chaves da fatia (as categorias dentro de "Outras"; na visão por grupo, o grupo). Tocar numa **linha** não seleciona: abre "Transações da categoria" com o scope da linha (`Group(id)`, `Category(id)` ou `None`), e um filho expandido abre com `Category(id)`. O botão de expandir é separado do toque na linha. A correspondência fatia → linhas vira função pura (`Charts.keysOf(pie, index)`), com testes.
- **Alternativa descartada**: calcular o agrupamento na tela. Ficaria sem teste e repetiria a lógica de percentuais.

### 4. Chave de seleção e filtro unificados
Um tipo `CategoryScope` no domínio, `sealed`, com os casos `All`, `None` ("Sem categoria"), `Category(id)` e `Group(id)`, e com `matches(t, data)`.
- **Semântica do filtro**: com scope diferente de `All`, só entram receitas e despesas, sem transferências e sem ajustes de saldo. São as mesmas regras de Relatórios, então o resumo bate com o valor mostrado lá. `None` significa receita ou despesa sem categoria.
- **Resumo**: total de despesas e total de receitas do conjunto filtrado, cada um mostrado só quando for diferente de zero, em valor absoluto e rotulado ("Despesas"/"Receitas"), como nos cartões de Relatórios por categoria. Num grupo, o detalhamento por categoria lista só as que tiveram movimento. Tudo vem de `TransactionQueries.scopeSummary(list, scope, data)`.
- **Combinação**: o scope combina com o tipo e com a conta. Ao escolher o tipo "Transferências", o scope volta para `All` e o chip de categoria fica desabilitado enquanto esse tipo estiver escolhido.
- **Serialização**: o scope é guardado como string estável (`all`, `none`, `cat:<id>`, `grp:<id>`) para continuar compatível com `rememberSaveable` e com rotas. Se o id apontar para algo que não existe mais, ele volta para `all`.
- **Relatórios → transações do item**: em vez de abrir a aba Transações filtrada (versão anterior desta spec), Relatórios usa a tela de `transacoes-da-categoria`. A rota `Route.CategoryTransactions` troca `categoryId: String?` por `scope: String` (`none`, `cat:<id>` ou `grp:<id>`), mantendo `type`, `month` e `accountId`. Em modo grupo, a consulta é `CategoryScope.Group(id).matches` com as mesmas regras de `Reports.month`, o cabeçalho mostra o grupo e `TransactionQueries.scopeSummary` dá o detalhamento por categoria. O total MUST ser igual ao `GroupSlice.totalCents` correspondente. Se o grupo deixar de existir com a tela aberta, ela volta para Relatórios, como uma categoria removida.
- **Conta**: Relatórios passa a conta filtrada (de `relatorio-por-conta`) no `accountId` da rota. Quem for implementado por último faz a ligação.

### 5. UI do filtro de categoria em Transações
Um chip "Categoria: Todas ▾" numa terceira linha de filtros abre um `ModalBottomSheet` com "Todas", "Sem categoria", cada grupo do tipo atual (com as categorias dele recuadas embaixo) e depois as categorias sem grupo. Com o tipo "Todas", a lista mostra os dois tipos de categoria, separados por título. Ao escolher Transferências, o scope volta para `All` (decisão 4).
- **Alternativa descartada**: mais uma `LazyRow` de chips. Com 15 ou mais categorias e grupos, a rolagem horizontal fica difícil e não mostra a hierarquia.

### 6. Soma no campo Valor
- `Money.parseSum(input): Long?` remove os espaços, divide por `+` e faz o parse de cada parcela com `Money.parse`, aceitando os mesmos formatos de hoje (`1.234,56`, `45,9`, `45.90`, `1.234`). O resultado é inválido quando há `+` no início, no fim ou repetido (parcela vazia), mais de 50 parcelas, alguma parcela inválida, ou um total igual a zero ou acima de `MAX_CENTS`. Parcelas iguais a zero são aceitas desde que o total seja maior que zero. `Money.isSum(input)` diz se há mais de uma parcela.
- `AmountField` ganha `allowSum: Boolean = false`. Com ele ligado:
  - o filtro de entrada aceita `+`, o que também vale para texto colado, e o limite de caracteres passa de 18 para 800 (50 parcelas de até 15 caracteres mais os `+`);
  - um `trailingIcon` "Somar" (ícone Add, com descrição para leitor de tela) insere `+` na posição do cursor e deixa o cursor depois dele, por isso o campo passa a usar `TextFieldValue`;
  - o `supportingText` mostra "= R$ 35,80" quando há mais de uma parcela e a soma é válida.
- Só o formulário de transação liga `allowSum`. O valor salvo é `parseSum(amount)`. Ao editar, o campo começa com `Money.plain(total)`, como hoje.
- **Alternativa descartada**: uma calculadora própria em diálogo. Seria mais pesada e mudaria o fluxo rápido de lançamento, que o projeto escolheu de propósito (ver `docs/engenharia-reversa.md`, item 12).

### 7. Backup formato 2
- `FORMAT_VERSION = 2`. `BackupData.categoryGroups: List<CategoryGroupDto> = emptyList()` e `CategoryDto.groupId: String? = null`, os dois com `@EncodeDefault(EncodeDefault.Mode.NEVER)`. Assim, sem grupos, o JSON não ganha campos novos.
- **Impressão digital**: com os campos novos omitidos quando estão no padrão, a impressão digital de dados sem grupos é a mesma do formato 1. Atualizar o app não dispara o lembrete de backup sem que algo tenha mudado. Isso ganha um teste.
- **Leitura condicionada à versão**: num arquivo que declara `formatVersion: 1`, `categoryGroups` e `groupId` são ignorados, mesmo se estiverem presentes, e a restauração fica sem grupos. Num arquivo de versão 2 eles são lidos e validados (nomes únicos por tipo, grupo existente, mesmo tipo da categoria).
- O `toDto` ordena os grupos por id, como as demais listas. As transações mantêm a ordem de inserção (PR #4). A contagem da confirmação ganha "N grupos" quando N > 0.
- A restauração continua atômica no `replaceAll`, que agora também limpa e insere `category_groups` na mesma transação.

### 8. Categorias e formulários
**Pré-requisito**: o PR #4 (`fix/ajustes-release`), que cria o `NewCategoryDialog` no formulário de transação e de planejamento e mantém a categoria recém-criada como seleção pendente. Este change parte do `main` com o PR #4 já mesclado.
- `CategoriesScreen` renderiza seções: cada grupo (pela ordem de criação) como cabeçalho clicável que abre o formulário do grupo, as categorias dele (por `order`), e depois "Sem grupo". Um botão "Novo grupo" fica ao lado do "+" de categoria.
- `CategoryGroupFormScreen` (nova rota `Route.GroupForm(id, type)`) tem nome, tipo (travado depois de criado se o grupo tiver categorias), cor, ícone e a lista de categorias do tipo com checkbox. Ao salvar, `repo.saveGroup(group, memberIds)` grava numa transação de banco: o upsert do grupo, `groupId = null` em quem saiu e `groupId = id` em quem entrou (o que move a categoria de outro grupo).
- `CategoryFormScreen` ganha a seção "Grupo" com chips ("Sem grupo" e os grupos do tipo). Trocar o tipo limpa o grupo escolhido.
- O grupo é sempre o da categoria, e a transação não tem grupo próprio. Assim, ao agrupar uma categoria antiga, todas as transações dela passam a contar no grupo sem precisar editar uma por uma. Ter um grupo na transação foi descartado: abriria a possibilidade de transação e categoria discordarem.
- **Seletor agrupado nos formulários**: sem grupos, ele fica idêntico ao de hoje, com as usadas recentemente primeiro e depois por `order`. Com grupos, cada seção (grupo ou "Sem grupo") mantém essa mesma regra dentro dela. As seções de grupo são ordenadas pelo uso mais recente de qualquer categoria do grupo (e, no empate, pela ordem de criação), e "Sem grupo" vem por último.
- O diálogo "Nova categoria" do formulário ganha a escolha de grupo (padrão "Sem grupo").

## Risks / Trade-offs

- [Uma migração errada perde dados reais] → a migração só faz `CREATE TABLE`/`ADD COLUMN`/`CREATE INDEX` (sem reconstrução), e o teste de migração é executado no aparelho (`connectedDebugAndroidTest`) com contagens de todas as tabelas e `foreign_key_check`. O app só é publicado com esse teste passando.
- [`groupId` sem FK pode apontar para um grupo apagado] → `deleteGroup` limpa `groupId` na mesma transação. A leitura trata um grupo inexistente como "Sem grupo", e a validação do backup recusa esse caso no arquivo.
- [Um backup de formato 2 aberto numa versão antiga do app é recusado como "versão mais nova"] → é o comportamento esperado do spec atual. A mensagem já orienta a atualizar o app.
- [A tecla "+" no teclado `Decimal` varia por fabricante] → o botão "Somar" no próprio campo não depende do teclado.
- [Mudar `Charts.pie` para itens genéricos pode alterar a pizza por categoria] → os testes atuais de `ChartsTest` continuam valendo, adaptados à nova entrada sem mudar os resultados esperados.
- [Mais uma linha de filtros em Transações ocupa altura] → um único chip abre o seletor, então a linha é curta.

## Migration Plan

1. Antes de arquivar este change: arquivar `backup-exportacao`, para que `openspec/specs/data-backup` exista.
2. O app atualizado roda a migração 1 → 2 na primeira abertura. Não há passo manual.
3. Rollback: voltar para uma versão anterior do app não abre um banco v2 (o Room recusa um downgrade). A orientação é fazer backup antes, e um backup de formato 1 continua restaurável na versão nova.
