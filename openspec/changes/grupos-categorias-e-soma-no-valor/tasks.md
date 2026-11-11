# Tasks

## 0. Pré-requisito

- [x] 0.1 `transacoes-da-categoria` mesclado (PR #11) e arquivado (PR #12), com o delta de "Seleção de fatia" já sincronizado em `openspec/specs/monthly-reports/spec.md` (a linha da lista abre a tela e não seleciona). Concluída com o merge do PR #12.

## 1. Soma no campo Valor

- [ ] 1.1 `Money.parseSum` e `Money.isSum` conforme o design (decisão 6). Verificar com testes em `MoneyTest`: `12,50+8,30+15` = 3580; `1.234,56+10` = 124456; `45.90+0,10` = 4600; espaços ignorados (`12 + 3`); `+12`, `12+`, `12++3` inválidos; `0+5` = 500 e `0+0` inválido; 50 parcelas válido e 51 inválido; total acima de `MAX_CENTS` inválido; valor simples igual a `Money.parse`.
- [ ] 1.2 `AmountField(allowSum)` com `TextFieldValue`: aceita "+" (inclusive em texto colado), limite de 800 caracteres, botão "Somar" com descrição para leitor de tela que insere "+" na posição do cursor, e "= R$ X" no texto de apoio quando há mais de uma parcela válida. Ligar só no formulário de transação, salvando `parseSum`. Verificar no app: digitar `12,50+8,30+15`, ver "= R$ 35,80", salvar e conferir R$ 35,80 na lista; `12,50+` deixa Salvar desabilitado.

## 2. Modelo e banco dos grupos

- [ ] 2.1 Domínio: `CategoryGroup`, `Category.groupId`, `FinanceData.groups`/`groupById` e o ajuste dos fixtures de teste. Verificar com `./gradlew :app:testDebugUnitTest` verde.
- [ ] 2.2 Room: `CategoryGroupEntity`, `CategoryEntity.groupId` (sem FK) com índice, versão 2, `2.json` exportado e `Migration(1, 2)` só com `CREATE TABLE`/`ADD COLUMN`/`CREATE INDEX` (`CREATE` copiados do `createSql` do `2.json`, `ADD COLUMN` derivado da diferença entre `1.json` e `2.json`). Verificar **executando** `./gradlew :app:connectedDebugAndroidTest` no aparelho ou no emulador: o teste de migração (`MigrationTestHelper`, validação contra `2.json`) cria o v1 com contas, categorias, transações, planejamentos e orçamento com categorias, migra e confere as contagens de todas as tabelas (inclusive `budget_categories`), o `groupId` nulo e `PRAGMA foreign_key_check` vazio.
- [ ] 2.3 DAO/repositório: fluxo de grupos em `data` (grupo inexistente lido como "Sem grupo"), `nextGroupOrder`, `saveGroup(group, memberIds)` atômico (move categorias de outro grupo), `deleteGroup` limpando `groupId` na mesma transação, troca de tipo da categoria limpando `groupId`, e a limpeza/inserção de grupos no `replaceAll` da restauração. Verificar com testes em `FinanceRepositoryTest` (androidTest): mover categoria, excluir grupo mantendo categorias e transações, grupo vazio após excluir a última categoria.

## 3. Backup formato 2

- [ ] 3.1 `BackupFormat`/`BackupCodec`: `FORMAT_VERSION = 2`, `categoryGroups` e `CategoryDto.groupId` com `@EncodeDefault(NEVER)`, leitura condicionada à versão (v1 ignora os campos de grupo), validação (nome repetido por tipo, grupo inexistente, tipo diferente) e a contagem de grupos na confirmação. Verificar com testes em `BackupCodecTest`: ida e volta com grupos; v1 lido sem grupos; v1 com campos de grupo ignorados; ciclo v1 → dados → backup v2 → restauração com os mesmos ids e vínculos; impressão digital de dados sem grupos igual à do formato 1; três casos de recusa; versão 3 recusada como mais nova. No androidTest, `replaceAll` com grupos é atômico (falha no meio mantém os dados anteriores).

## 4. Telas de categorias e grupos

- [ ] 4.1 `CategoriesScreen` por seções (grupos pela ordem de criação, com suas categorias por `order`, e depois "Sem grupo"), botão "Novo grupo", e a lista idêntica à atual quando não há grupos. Verificar no app com e sem grupos.
- [ ] 4.2 `Route.GroupForm` e `CategoryGroupFormScreen`: nome único por tipo, cor, ícone, categorias do tipo com checkbox, tipo travado quando tem categorias, e exclusão com confirmação que informa quantas categorias ficarão sem grupo. Verificar no app criando "Saúde" com 3 categorias, movendo uma categoria de outro grupo e excluindo o grupo.
- [ ] 4.3 `CategoryFormScreen`: seção "Grupo" com "Sem grupo" e os grupos do tipo, e troca de tipo limpando o grupo. Verificar no app.
- [ ] 4.4 Pré-requisito: PR #4 mesclado (`NewCategoryDialog` e seleção pendente). `CategoryChips` agrupado com a ordenação da decisão 8 (seções de grupo pelo uso mais recente, recentes primeiro dentro de cada seção, "Sem grupo" por último, idêntico ao atual sem grupos), com a ordenação em função pura testada em `TransactionQueriesTest`, nos formulários de transação e de planejamento, e `NewCategoryDialog` com escolha de grupo (padrão "Sem grupo"). Verificar no app: nova despesa com grupos mostra os títulos; criar "Dentista" em Saúde pelo diálogo e ver a categoria selecionada.

## 5. Filtro por categoria/grupo e busca

- [ ] 5.1 Domínio: `CategoryScope` (`All`, `None`, `Category`, `Group`) com serialização em string estável (id inexistente vira `All`), `TransactionFilter.scope` (fora de `All`, sem transferências nem ajustes), `TransactionQueries.scopeSummary` (despesas e receitas separadas, detalhamento por categoria com movimento) e a busca pelo nome do grupo. Verificar com testes em `TransactionQueriesTest`: cenário Saúde do spec (R$ 800,00 = 500 + 300; Seguro de vida ausente); "Sem categoria" sem transferências nem ajustes; resumo igual ao total de Relatórios para o mesmo item; combinação com tipo e conta; e a busca "saude".
- [ ] 5.2 `TransactionsScreen`: chip "Categoria" com seletor em bottom sheet (grupos com categorias recuadas, sem grupo, "Sem categoria"), que volta para "Todas" e fica desabilitado ao escolher o tipo "Transferências", e cartão de resumo do filtro. Verificar no app filtrando por Saúde e por uma categoria, e navegando entre meses com o filtro mantido.

## 6. Relatórios por grupo

- [ ] 6.1 Domínio: `GroupSlice` e `expenseByGroup`/`incomeByGroup` em `Reports.month`; `Charts.pie` recebendo `ChartItem` com `CategoryScope` e devolvendo `PieSlice.keys`; `Charts.keysOf`. Verificar com testes em `BudgetsAndReportsTest`/`ChartsTest`: cenário Saúde 80% / Lazer 20%, filhos 63% / 37%, grupo sem movimento ausente, "Sem categoria" como item próprio; linhas destacadas pela fatia do grupo e pela fatia "Outras"; e os testes atuais da pizza por categoria com os mesmos resultados esperados.
- [ ] 6.2 Pré-requisito: `transacoes-da-categoria` implementado. `Route.CategoryTransactions` com `scope` no lugar de `categoryId` (lido por `CategoryScope.parseRoute`, nunca `parseFilter`) e a tela em modo grupo (cabeçalho do grupo, detalhamento por `scopeSummary`, total igual ao `GroupSlice`, volta para Relatórios se o grupo sumir). Verificar com testes: total da tela igual a `GroupSlice.totalCents` (que é a soma dos subtotais) e cada subtotal do detalhamento igual ao `CategorySlice` da mesma categoria, para o mesmo mês, tipo e `accountId` (com e sem conta), também depois de trocar o mês na tela; `parseRoute` de grupo inexistente devolve `null`; e no app abrindo Saúde.
- [ ] 6.3 `ReportsScreen`: escolha "Por categoria"/"Por grupo" (só quando existe grupo do tipo, mantida ao trocar mês/tipo e na volta da tela de transações, como mês e tipo em `transacoes-da-categoria`), botão de expandir separado do toque na linha, seleção só por fatia, e toque na linha de grupo, categoria, "Sem categoria" ou filho expandido abrindo `Route.CategoryTransactions` com o scope e a conta do filtro de Relatórios, se houver. Verificar no app e com o TalkBack anunciando o resumo da pizza por grupo e a ação "Ver transações" nas linhas.

## 7. Fechamento

- [ ] 7.1 Atualizar o README (grupos, soma no valor, backup formato 2) e rodar `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`, tudo verde.
- [ ] 7.2 Subir o `versionCode` para o próximo número (o PR #8 já usa 2). Verificação no aparelho com um banco v1 real: instalar por cima da versão release assinada com a mesma chave, confirmar que os dados continuam lá e que nenhuma categoria tem grupo, criar grupos, fazer backup, restaurar e conferir Relatórios e Transações.

## Workflow follow-up

- Arquivar `backup-exportacao` antes de arquivar este change (o delta `data-backup` depende do spec principal).
- Arquivar este change depois do PR aprovado e sincronizar os specs.
