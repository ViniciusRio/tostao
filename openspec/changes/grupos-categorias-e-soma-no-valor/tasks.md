# Tasks

## 1. Soma no campo Valor

- [ ] 1.1 `Money.parseSum` e `Money.isSum`: até 50 parcelas separadas por "+", sem parcela vazia, total ≤ `MAX_CENTS`. Verificar com testes em `MoneyTest` cobrindo `12,50+8,30+15` = 3580, `12,50+` inválido, 51 parcelas inválido, total acima do limite inválido e valor simples igual a `Money.parse`.
- [ ] 1.2 `AmountField(allowSum)`: aceita "+", botão "Somar" com descrição para leitor de tela que acrescenta "+" e mantém o foco/cursor no fim, e "= R$ X" no texto de apoio quando há mais de uma parcela. Ligar só no formulário de transação, salvando `parseSum`. Verificar no app: digitar `12,50+8,30+15`, ver "= R$ 35,80", salvar e conferir R$ 35,80 na lista; `12,50+` deixa Salvar desabilitado.

## 2. Modelo e banco dos grupos

- [ ] 2.1 Domínio: `CategoryGroup`, `Category.groupId`, `FinanceData.groups`/`groupById` e o ajuste dos fixtures de teste. Verificar com `./gradlew :app:testDebugUnitTest` verde.
- [ ] 2.2 Room: `CategoryGroupEntity`, `CategoryEntity.groupId` com FK `SET_NULL` e índice, versão 2, `Migration(1, 2)` com reconstrução de `categories`, e `2.json` exportado. Verificar com o teste de migração no androidTest (dados v1 com categorias, orçamento com categorias e transações → v2 com tudo preservado e `groupId` nulo) e `./gradlew :app:compileDebugAndroidTestKotlin`.
- [ ] 2.3 DAO/repositório: fluxo de grupos em `data`, `saveGroup(group, memberIds)` atômico (move categorias de outro grupo), `deleteGroup` limpando `groupId`, troca de tipo da categoria limpando `groupId`, e a limpeza/inserção de grupos no `replaceAll` da restauração. Verificar com testes em `FinanceRepositoryTest` (androidTest): mover categoria, excluir grupo mantendo categorias e transações, grupo vazio após excluir a última categoria.

## 3. Backup formato 2

- [ ] 3.1 `BackupFormat`/`BackupCodec`: `FORMAT_VERSION = 2`, `categoryGroups` e `CategoryDto.groupId` com padrões, validação (nome repetido por tipo, grupo inexistente, tipo diferente), `toDto` ordenado e a contagem de grupos na confirmação. Verificar com testes em `BackupCodecTest`: ida e volta com grupos, arquivo v1 lido sem grupos, três casos de recusa e versão 3 recusada como mais nova.

## 4. Telas de categorias e grupos

- [ ] 4.1 `CategoriesScreen` por seções (grupos com suas categorias e depois "Sem grupo"), botão "Novo grupo", e a lista idêntica à atual quando não há grupos. Verificar no app com e sem grupos.
- [ ] 4.2 `Route.GroupForm` e `CategoryGroupFormScreen`: nome único por tipo, cor, ícone, categorias do tipo com checkbox, tipo travado quando tem categorias, e exclusão com confirmação que informa quantas categorias ficarão sem grupo. Verificar no app criando "Saúde" com 3 categorias, movendo uma categoria de outro grupo e excluindo o grupo.
- [ ] 4.3 `CategoryFormScreen`: seção "Grupo" com "Sem grupo" e os grupos do tipo, e troca de tipo limpando o grupo. Verificar no app.
- [ ] 4.4 `CategoryChips` agrupado (título por grupo, categorias sem grupo no fim, idêntico ao atual sem grupos) nos formulários de transação e de planejamento, e `NewCategoryDialog` com escolha de grupo (padrão "Sem grupo"). Verificar no app: nova despesa com grupos mostra os títulos; criar "Dentista" em Saúde pelo diálogo e ver a categoria selecionada.

## 5. Filtro por categoria/grupo e busca

- [ ] 5.1 Domínio: `CategoryScope` (`All`, `None`, `Category`, `Group`) com serialização em string estável, `TransactionFilter.scope`, `TransactionQueries.scopeSummary` (total e detalhamento por categoria com movimento) e a busca pelo nome do grupo. Verificar com testes em `TransactionQueriesTest` usando o cenário Saúde do spec (R$ 800,00 = 500 + 300; Seguro de vida ausente), filtro combinado com tipo/conta, "Sem categoria" e a busca "saude".
- [ ] 5.2 `TransactionsScreen`: chip "Categoria" com seletor em bottom sheet (grupos com categorias recuadas, sem grupo, "Sem categoria") e cartão de resumo do filtro. `Route.Transactions` ganha `month`, `type` e `scope` opcionais. Verificar no app filtrando por Saúde e por uma categoria, e navegando entre meses com o filtro mantido.

## 6. Relatórios por grupo

- [ ] 6.1 Domínio: `GroupSlice` e `expenseByGroup`/`incomeByGroup` em `Reports.month`, com `Charts.pie` passando a receber itens genéricos. Verificar com testes em `BudgetsAndReportsTest`/`ChartsTest`: cenário Saúde 80% / Lazer 20%, filhos 63% / 37%, grupo sem movimento ausente, "Sem categoria" como item próprio, e os testes atuais da pizza por categoria sem mudar os resultados esperados.
- [ ] 6.2 `ReportsScreen`: escolha "Por categoria"/"Por grupo" (só quando existe grupo do tipo, mantida ao trocar mês/tipo), linhas de grupo expansíveis, seleção por `CategoryScope` e ação "Ver transações" (fora de "Outras") abrindo a rota com mês, tipo e scope. Verificar no app e com o TalkBack anunciando o resumo da pizza por grupo.

## 7. Fechamento

- [ ] 7.1 Atualizar o README (grupos, soma no valor, backup formato 2) e rodar `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`, tudo verde.
- [ ] 7.2 Subir `versionCode` para 2. Verificação no aparelho com um banco v1 real: instalar por cima da versão release assinada com a mesma chave, confirmar que os dados continuam lá e que nenhuma categoria tem grupo, criar grupos, fazer backup, restaurar e conferir Relatórios e Transações.

## Workflow follow-up

- Arquivar `backup-exportacao` antes de arquivar este change (o delta `data-backup` depende do spec principal).
- Arquivar este change depois do PR aprovado e sincronizar os specs.
