# Design

## Context

Projeto novo, sem código. Referência funcional em `docs/engenharia-reversa.md`. Uso pessoal em um único aparelho (Galaxy A26, Android 15+), sem backend. O toolchain segue o do projeto Dengo (AGP 8.13, Kotlin 2.2, Compose BOM 2025.09, Room 2.8), já em cache local.

## Goals / Non-Goals

**Goals:**
- Domínio puro em Kotlin (sem Android) testável por testes unitários: dinheiro, saldos, recorrência, orçamentos, relatórios.
- Uma fonte de dados reativa (Room + Flow) para todas as telas.
- UI Material 3 com cor dinâmica desligada e paleta própria para manter identidade.

**Non-Goals:**
- Arquitetura multimódulo, injeção de dependência por framework, Navigation Compose. Um app deste tamanho não precisa.
- Migrações de banco: versão 1 do esquema; migrações começam no primeiro change que alterar o esquema publicado.

## Decisions

- **Dinheiro em `Long` de centavos** (`Money` value class). Alternativa `BigDecimal` foi descartada: mais cara e desnecessária para uma moeda com 2 casas. Resolve o problema 1 do relatório.
- **Data/hora de transação em `Long` epoch millis** (instante), agrupadas por dia/mês no fuso atual do aparelho. Uma viagem pode deslocar uma transação feita perto da meia-noite; aceitável para uso pessoal.
- **Datas de vencimento planejado em `LocalDate`** (epochDay no banco): data civil, imune a fuso.
- **Ajuste de saldo** é uma transação com `isAdjustment = true`: entra em saldos e extrato, sai de relatórios e orçamentos.
- **Orçamento "todas as despesas"** é um booleano explícito, nunca inferido de lista vazia.
- **Saldos calculados em memória** a partir das transações (milhares de linhas no uso pessoal), em vez de colunas de saldo desnormalizadas que podem divergir.
- **Pagamento planejado guarda `consumedOccurrences`** na regra; a próxima data é `occurrence(start, consumed)`. Pagar = inserir transação + `consumed+1` (numa transação de banco); pular = `consumed+1`. Desfazer só vale se `consumed` ainda for o valor gravado após a ação; então volta 1 e remove a transação criada (por id). Nunca existem ocorrências futuras materializadas (problema 7).
- **Recorrência mensal/anual ancorada no dia da primeira data** (`anchorDay`), para que 31/jan → 28/fev → 31/mar, em vez de deslizar para 28.
- **Orçamento ↔ categorias** em tabela de junção (`budget_category`), não em string serializada como o original.
- **Exclusão física com desfazer em memória**: a UI guarda a entidade excluída e a reinsere com o mesmo id. Sem `isDeleted`, pois não há sincronização.
- **Navegação**: pilha de destinos própria (`sealed interface Route` + `mutableStateListOf`) com `BackHandler`. Evita dependência extra.
- **Estado**: um único `ViewModel` de atividade expõe um `StateFlow<FinanceData>` (todas as tabelas combinadas) e as ações; cada tela deriva o que precisa com as funções puras do domínio. Formulários usam estado local da tela. Um `SnackbarHost` único mostra apenas a última ação com "Desfazer".
- **Relógio injetável** (`java.time.Clock`) para testar "hoje", atrasados e mês atual.
- **Preferência "ocultar valores"** em `SharedPreferences` (um booleano não justifica DataStore).
- **Pacote `com.viniciusrio.tostao`** e nome do app "Tostão" (lema "cada vintém no lugar") no recurso `app_name`.

## Risks / Trade-offs

- [Cálculo de saldo em memória cresce com o histórico] → Aceitável para uso pessoal; se passar de ~50 mil transações, trocar por agregações SQL.
- [Desfazer em memória se perde se o app for morto durante o Snackbar] → Janela curta (~5 s); aceitável.
- [Sem backup no MVP] → `allowBackup=false` para cumprir "somente local"; desinstalar o app apaga os dados. Exportação/backup manual fica para outro change.
- [Fuso horário] → Datas gravadas em UTC millis e agrupadas pelo fuso atual do aparelho.
