# Design

## Context

- Todo o estado financeiro está nas 6 tabelas do Room (`finance.db`, esquema versão 1). O `FinanceRepository` já expõe `FinanceData`, um retrato em domínio de contas, categorias, transações, regras e orçamentos (com `categoryIds`).
- Preferências (ocultar valores, última conta usada) ficam em `SharedPreferences` (`prefs`).
- `domain/` é Kotlin puro e testado com JUnit na JVM. Por isso o `org.json` do Android não serve ali: nos testes locais ele é só um stub.
- Não existe tela de Configurações. O topo da Início tem 4 ícones: olho, busca, categorias e contas.
- `seedIfEmpty()` recria as categorias padrão sempre que a tabela está vazia.

## Goals / Non-Goals

**Goals:**
- Montar o backup, validá-lo e gerar o CSV em funções puras, testadas sem Android.
- Fazer a restauração numa única transação do Room.
- Usar o Storage Access Framework via `ActivityResultContracts` e ler e gravar pelo `ContentResolver`.

**Non-Goals:**
- Criptografia do arquivo (fica para o formato 2).
- Backup automático ou agendado, notificações e integração com Drive.
- Importar CSV.
- Incluir preferências no backup. "Ocultar valores" e "última conta" são do aparelho, não dados financeiros.
- Mesclar backups.

## Decisions

### Formato JSON com kotlinx.serialization
Adiciono `org.jetbrains.kotlin.plugin.serialization` (mesma versão do Kotlin, 2.2.20) e `kotlinx-serialization-json`. Os DTOs ficam em `domain/backup/BackupFormat.kt`, separados das entidades do Room e das classes de domínio. Assim o formato não muda sem querer quando o esquema muda.

```json
{
  "app": "tostao",
  "formatVersion": 1,
  "createdAt": "2026-11-09T14:30:00-03:00",
  "data": {
    "accounts":   [{ "id", "name", "colorArgb", "icon", "initialBalanceCents", "includeInTotal", "order" }],
    "categories": [{ "id", "name", "type": "EXPENSE|INCOME", "colorArgb", "icon", "order" }],
    "transactions": [{ "id", "type", "amountCents", "accountId", "toAccountId", "categoryId", "title", "note",
                       "dateTimeMillis", "plannedRuleId", "isAdjustment" }],
    "plannedRules": [{ "id", "type", "amountCents", "accountId", "categoryId", "title", "startDate": "AAAA-MM-DD",
                       "unit": "DAY|WEEK|MONTH|YEAR|null", "interval", "endDate", "consumedOccurrences" }],
    "budgets": [{ "id", "name", "limitCents", "allExpenses", "categoryIds": [], "order" }]
  }
}
```

- Saída com `prettyPrint` e indentação de 2 espaços. Fica legível e o tamanho continua baixo: cerca de 300 bytes por transação, ou uns 3 MB para 10 mil transações.
- Na leitura, `ignoreUnknownKeys = true` dentro da mesma versão, para aceitar campos opcionais acrescentados depois. Uma versão maior que a suportada é recusada antes de decodificar `data`.
- As datas civis usam ISO (`LocalDate`), e `createdAt` usa ISO com offset. `dateTimeMillis` continua em epoch ms, que é o que está no banco e evita ambiguidade de fuso.
- **Alternativa descartada:** copiar o `.db` do SQLite, porque fica preso ao esquema do Room e não dá para validar sem abrir um banco temporário. Também descartei Moshi e Gson, que exigem reflexão ou kapt, e o `org.json`, que não funciona nos testes de JVM.

### Validação em duas camadas, tudo antes de escrever
`BackupCodec.decode(text): BackupResult` (domínio) devolve `Valid(FinanceData, createdAt, counts)` ou `Invalid(motivo)`. Os passos são:
1. Ler a estrutura: o JSON deve ser válido, `app == "tostao"` e `formatVersion` deve existir. Uma falha aqui gera "não é um backup do Tostão" ou "versão mais nova".
2. Decodificar o conteúdo. Erros de tipo ou de enumeração geram "arquivo corrompido".
3. Aplicar as regras de negócio, reaproveitando os limites do app (`Money.MAX_CENTS`, tamanhos de nome, título e observação). Os IDs não podem ser vazios nem repetidos por tabela. Os nomes de conta precisam ser únicos sem diferenciar maiúsculas, e os de categoria, únicos por tipo. Valores precisam ser maiores que 0, com exceção do saldo inicial, que só precisa estar dentro do módulo máximo. Uma transferência precisa de `toAccountId` existente e diferente da origem e não pode ter categoria. A categoria de uma receita ou despesa precisa existir e ser do mesmo tipo. Regras planejadas não podem ser do tipo transferência e precisam de `interval` entre 1 e 99 e de `consumedOccurrences` maior ou igual a 0. `plannedRuleId` pode apontar para uma regra que não existe mais, porque uma regra excluída mantém as transações pagas. Orçamentos precisam de limite maior que 0, e as categorias de um orçamento por categorias devem existir, ser de despesa e não estar vazias.

A primeira violação gera uma mensagem curta em pt-BR com o tipo de registro, por exemplo "Transação 37: conta inexistente". Não tento listar todos os erros.

### Limite de 20 MB e leitura fora da thread principal
O `ContentResolver.openInputStream` é lido com um contador que interrompe ao passar de 20 MB. Isso cobre 60 mil transações ou mais com folga. Todo I/O e a decodificação rodam em `Dispatchers.IO`.

### Substituição atômica no repositório
`FinanceRepository.replaceAll(data: FinanceData)` executa, dentro de `db.withTransaction`, `DELETE` em `budget_categories`, `budgets`, `transactions`, `planned_rules`, `categories` e `accounts` (nesta ordem, por causa das FKs) e depois os inserts em lote. Uma exceção desfaz a transação inteira. A validação acontece antes, então uma falha aqui é inesperada, mas mesmo assim é tratada com a mensagem "a restauração falhou".

### Arquitetura: `DataTransferService` no `AppContainer`
O `MainViewModel` não pode guardar `Context`. Por isso crio `data/DataTransferService`, que recebe `ContentResolver`, `FinanceRepository`, `Clock` e `prefs`. Ele expõe `writeBackup(uri)`, `readBackup(uri)`, `restore(valid)` e `writeCsv(uri, period)`. O ViewModel mantém um `dataOp: StateFlow<DataOp?>` (`Backup`, `Restore` ou `Export`) para o indicador e o bloqueio de toque duplo, e um `pendingRestore` com o resultado validado à espera da confirmação.

### Fluxo de interface
- Os launchers do SAF são `rememberLauncherForActivityResult` na tela de Configurações e no cartão da Início. Eles usam `CreateDocument("application/json")`, `CreateDocument("text/csv")` e `OpenDocument(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))`, porque alguns provedores, como o Drive, não informam o MIME de um `.json`.
- Restauração: o usuário escolhe o arquivo, o app valida mostrando o indicador e abre o diálogo "Substituir todos os dados?". O diálogo mostra a data do backup, as contagens do arquivo e as contagens atuais, e tem o botão destrutivo "Substituir dados" e o botão "Cancelar". Depois da troca, `backStack` volta para `[Home]`, e a mensagem "Backup restaurado" aparece.
- Exportação: uma folha modal pede o período (chips "Tudo", "Este ano", "Este mês", "Intervalo" e, no intervalo, os campos de data existentes em `DateFields`). O botão mostra a contagem ("Exportar 120 transações"). Com zero transações ele fica desabilitado e aparece a mensagem do spec. Ao tocar, abre o `CreateDocument`.
- Configurações é uma rota nova, `Route.Settings`, fora das abas e com `BackTopBar`. A engrenagem (`Icons.Outlined.Settings`) substitui o ícone de categorias no topo da Início, e Categorias passa a ser um item de Configurações.

### CSV
`CsvExport.build(data, range, zone): String` fica no domínio. O separador é `;` e as linhas terminam em CRLF (RFC 4180, que o Excel prefere). O texto começa com o BOM `﻿`. O valor é escrito sem milhar (`1234,50`) para que Excel e Sheets em pt-BR o leiam como número. Com "1.234,50", o Sheets em outra localidade leria texto. O escape coloca o campo entre aspas se ele contiver `;`, `"`, CR ou LF, duplicando as aspas internas. Para evitar injeção de fórmula, os campos de texto (conta, categoria, título e observação) que começam com `= + - @ \t \r` recebem o prefixo `'`. Data, hora, valor e tipo não passam por esse escape, já que são gerados pelo app. As transações são ordenadas por `dateTimeMillis` crescente e, em caso de empate, por id, para que a saída seja estável.

O período é representado por `ExportPeriod` (`All`, `Year`, `Month`, `Range(start, end)`) e convertido em intervalo de millis com `Periods`, no fuso do aparelho.

### Lembrete
Em `prefs` ficam `last_backup_at` (epoch ms), `last_backup_fingerprint` e `backup_snooze_until`. O fingerprint é um hash (SHA-256 truncado) da serialização canônica de `data` no formato 1, sem `createdAt`. O ViewModel recalcula o fingerprint num `Flow` com `debounce(500)` e `flowOn(Dispatchers.Default)` a partir de `repo.data`, e a função pura `BackupReminder.shouldShow(...)` decide se o cartão aparece. Recalcular a cada mudança custa alguns ms para milhares de transações. Comparar o fingerprint, em vez de guardar um "modificado em" em cada escrita, evita mexer em todos os métodos do repositório e trata "editei e desfiz" como sem mudança. Na restauração, `last_backup_at` passa a ser o `createdAt` do arquivo, e o fingerprint passa a ser o dos dados restaurados. "Agora não" grava `snooze_until = agora + 7 dias`.

### Categorias padrão só na primeira instalação
`seedIfEmpty()` passa a consultar a preferência `seeded`. Se ela for falsa e não houver categorias, o app cria as padrão. Em seguida, em qualquer caso, grava `seeded = true`. Numa instalação existente, que já tem categorias, a preferência é gravada sem criar nada, então ninguém perde nada. A restauração também grava `seeded = true`.

### Nome sugerido e "Sobre"
Os nomes sugeridos são `tostao-backup-AAAA-MM-DD.json` e `tostao-transacoes-AAAA-MM-DD.csv`, com a data local. A seção "Sobre" mostra `BuildConfig.VERSION_NAME`, o que exige `buildFeatures.buildConfig = true`.

## Risks / Trade-offs

- [Arquivo sem senha em pasta compartilhada ou na nuvem] → Aviso fixo em "Seus dados". A criptografia fica prevista no formato 2, e a versão de formato já permite isso.
- [Usuário restaura sem querer por cima de dados mais novos] → O diálogo mostra a data do backup e as contagens atuais contra as do arquivo, e o botão é destrutivo e explícito. Não existe desfazer, porque isso exigiria um snapshot dos dados atuais. É aceitável, já que o usuário pode fazer backup antes, e o diálogo sugere isso.
- [Um provedor de SAF grava parcialmente e falha] → A exceção é informada e a data de último backup não é registrada. O arquivo parcial pode ficar no provedor, mas como é inválido, a restauração o recusa.
- [O fingerprint deixa de bater depois de mudanças inócuas de ordem] → A serialização canônica ordena cada lista por id.
- [`ignoreUnknownKeys` pode esconder um erro de digitação num arquivo editado à mão] → Os campos obrigatórios continuam sendo exigidos. Um campo a mais é ignorado de propósito.
- [A memória sobe com o arquivo de 20 MB inteiro em `String`] → O teto de 20 MB mantém isso abaixo de cerca de 80 MB de pico. O aparelho-alvo (A26) tem folga.

## Migration Plan

O esquema do banco não muda. A atualização só acrescenta a preferência `seeded`, como descrito acima. Para reverter, basta voltar a versão do app: os arquivos de backup continuam legíveis por qualquer versão futura que suporte o formato 1.
