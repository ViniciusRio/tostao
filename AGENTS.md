# AGENTS.md — Tostão

App Android de finanças pessoais, offline, em pt-BR. Kotlin, Jetpack Compose (Material 3) e Room. Sem servidor e sem conta.

## Fluxo de trabalho
As regras abaixo valem por si (o documento de origem fica fora do repositório):
- Uma mudança relevante começa por um change OpenSpec (`proposal.md`, `design.md`, `tasks.md`, specs). O Codex faz a revisão adversarial da spec, somente leitura, **antes** do código, e o Vinícius aprova. Tarefas triviais (typo, ajuste de uma linha) pulam a spec.
- Crie a branch (`feat/...`, `fix/...`, `docs/...`) **antes** da primeira edição. Nunca trabalhe na `main`.
- Marque o progresso no `tasks.md` do change.
- **Pronto** = lint, testes e build verdes, rodados pelo implementador.
- O handoff vai na descrição do PR: Objetivo, Branch e commit, Arquivos alterados, Decisões, Checagens rodadas, Dúvidas em aberto, Próxima ação.
- O Codex revisa o diff do PR em modo somente leitura e classifica cada achado como Bloqueante (volta para correção) ou Sugestão (não bloqueia), registrado em comentário no PR. A 2ª rodada confere cada achado original contra o diff corrigido. São no máximo 2 rodadas; depois disso, o Vinícius decide.
- Só o Vinícius faz merge. A `main` é protegida e exige PR.
- Commits seguem Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`), com o trailer `Co-Authored-By`.
- Atualize o `STATUS.md` ao parar.

## Comandos
```bash
./gradlew :app:testDebugUnitTest            # testes de domínio (JVM)
./gradlew :app:assembleDebug                # build
./gradlew :app:lintDebug                    # lint (relatório em app/build/reports/)
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:connectedDebugAndroidTest    # testes instrumentados (hoje: repositório com banco em memória); precisa de aparelho ou emulador
./gradlew :app:installDebug
openspec validate <change>
```

## Estrutura (`app/src/main/java/com/viniciusrio/tostao/`)
- `domain/`: regras puras e testáveis. A lógica nova vai aqui, com teste em `app/src/test`.
- `data/`: Room e repositório. O schema é exportado em `app/schemas/`.
- `ui/`: Compose; a navegação é uma pilha própria (`MainViewModel.backStack` + `Route`), sem Navigation Compose e sem `SaveableStateHolder`.
- `openspec/specs/`: comportamento vigente. `openspec/changes/`: mudanças em andamento.

## Convenções
- Texto de UI, specs e commits em pt-BR. Nas specs, os cabeçalhos estruturais e os termos SHALL/MUST ficam em inglês.
- Dinheiro sempre em centavos (`Long`). Use `Money.parse` e `Money.format`, nunca `Double`.
- Datas e horas no fuso do aparelho (`vm.zone`). Transações efetivadas = `dateTimeMillis <= agora`.
- Relatórios e orçamentos ignoram transferências e ajustes de saldo.
- O backup é JSON versionado (`BackupCodec.FORMAT_VERSION`): versões antigas continuam restaurando, e o arquivo fica separado das entidades do Room.
- Acessibilidade: área de toque mínima de 48 dp, descrição para leitor de tela, e a cor nunca é a única informação.

## Correções recorrentes
- Não implemente na `main` "só para testar": crie a branch antes.
- Ao abrir qualquer PR (spec ou código), o implementador dispara a revisão do Codex na hora, sem esperar o pedido, sempre somente leitura, e registra o resultado no PR antes de reportar ao Vinícius. Vale também para PR de documentação e para PR de branch antiga reaberta.
- Migrações do Room: nunca use `fallbackToDestructiveMigration`. Não reconstrua uma tabela referenciada por FK com `ON DELETE CASCADE` (`DROP TABLE categories` apaga `budget_categories`). Prefira `ADD COLUMN`. Toda migração exige um teste de migração próprio (`MigrationTestHelper` a partir do schema exportado), rodado no aparelho; hoje não existe nenhum, e `connectedDebugAndroidTest` sozinho não cobre migração.
- Ordem da lista de transações: o empate de horário usa o `rowid`. O snapshot e o backup preservam a ordem de inserção.
- Um id criado de forma assíncrona (ex.: nova categoria) precisa valer como seleção antes de o Flow do Room emiti-lo.
- Há usuário com dados reais na versão release. A atualização tem de ser instalada por cima, com a mesma chave de assinatura, e com o `versionCode` incrementado.
