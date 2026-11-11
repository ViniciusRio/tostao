# AGENTS.md — Tostão

App Android de finanças pessoais, offline, em pt-BR. Kotlin, Jetpack Compose (Material 3) e Room. Sem servidor e sem conta.

## Fluxo de trabalho
Segue `~/projects/personal/ai-docs/boas-praticas-ia.md`. Na prática:
- Uma mudança relevante começa por um change OpenSpec (`/openspec-propose`). O Codex faz a revisão adversarial da spec, somente leitura, **antes** do código, e o Vinícius aprova.
- Crie a branch (`feat/...`, `fix/...`, `docs/...`) **antes** da primeira edição. Nunca trabalhe na `main`.
- Marque o progresso no `tasks.md` do change.
- **Pronto** = lint, testes e build verdes, rodados pelo implementador.
- O handoff vai na descrição do PR: Objetivo, Branch e commit, Arquivos alterados, Decisões, Checagens rodadas, Dúvidas em aberto, Próxima ação.
- O Codex revisa o diff do PR em modo somente leitura e classifica cada achado como Bloqueante ou Sugestão, registrado em comentário no PR. São no máximo 2 rodadas.
- Só o Vinícius faz merge. A `main` é protegida e exige PR.
- Commits seguem Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`), com o trailer `Co-Authored-By`.
- Atualize o `STATUS.md` ao parar.

## Comandos
```bash
./gradlew :app:testDebugUnitTest            # testes de domínio (JVM)
./gradlew :app:assembleDebug                # build
./gradlew :app:lintDebug                    # lint (relatório em app/build/reports/)
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:connectedDebugAndroidTest    # Room/migrações; precisa de aparelho ou emulador
./gradlew :app:installDebug
openspec validate <change>
```

## Estrutura (`app/src/main/java/com/viniciusrio/tostao/`)
- `domain/`: regras puras e testáveis (saldos, relatórios, gráficos, recorrência, dinheiro, backup/CSV). A lógica nova vai aqui, com teste em `app/src/test`.
- `data/`: Room (`Entities`, `FinanceDao`, `FinanceDatabase`), `FinanceRepository`, `DataTransferService`. O schema é exportado em `app/schemas/`.
- `ui/`: `FinanceRoot` (navegação por `backStack` e `Route`), `MainViewModel`, `screens/`, `components/`, `theme/`.
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
- Dispare a revisão do Codex sem esperar o pedido, na spec e no diff (agente `codex:codex-rescue`, somente leitura).
- Migrações do Room: nunca use `fallbackToDestructiveMigration`. Não reconstrua uma tabela referenciada por FK com `ON DELETE CASCADE` (`DROP TABLE categories` apaga `budget_categories`). Prefira `ADD COLUMN` e execute o teste de migração, não basta compilar.
- Ordem da lista de transações: o empate de horário usa o `rowid`. O snapshot e o backup preservam a ordem de inserção.
- Um id criado de forma assíncrona (ex.: nova categoria) precisa valer como seleção antes de o Flow do Room emiti-lo.
- Há usuário com dados reais na versão release. A atualização tem de ser instalada por cima, com a mesma chave de assinatura, e com o `versionCode` incrementado.
