# Tasks

## 1. Base

- [ ] 1.1 Adicionar o plugin `kotlin.plugin.serialization` e `kotlinx-serialization-json` ao catálogo e ao app, e ativar `buildConfig`. Verificar com `./gradlew :app:assembleDebug`.
- [ ] 1.2 Criar `domain/Limits` (nomes de conta e orçamento com 40, de categoria com 30, título com 60, observação com 500 e intervalo de 1 a 99) e trocar os números soltos nos formulários por ele. Verificar que os testes atuais e o build passam sem mudança de comportamento.

## 2. Formato e validação do backup (domínio)

- [ ] 2.1 Criar os DTOs do formato 1 (`domain/backup/BackupFormat.kt`) e `BackupCodec.encode(data, createdAt)` com saída canônica (listas ordenadas por id), verificando com um teste de ida e volta (`encode` e depois `decode` devolvem o mesmo `FinanceData`).
- [ ] 2.2 Implementar `BackupCodec.decode` com as três camadas (estrutura, decodificação e regras de negócio) e mensagens em pt-BR. Cobrir com testes: arquivo de outro app, JSON inválido, versão 2, enum desconhecido, ID repetido, nome de conta duplicado, valor fora do limite, transferência para a mesma conta, transferência com categoria, categoria de tipo errado, referência quebrada, orçamento sem categoria, regra com `interval` 0 e `plannedRuleId` órfão (aceito).
- [ ] 2.3 Implementar o fingerprint (hash da serialização canônica) e `BackupReminder.shouldShow`. Cobrir com testes os cenários do spec: backup antigo com mudança, backup antigo sem mudança, nunca houve backup com transação de 7 dias ou mais e com menos de 7 dias, adiamento ativo e sem transações.

## 3. CSV (domínio)

- [ ] 3.1 Implementar `ExportPeriod` e `CsvExport.build`, com BOM, `;`, CRLF, valor sem milhar e sinal só na despesa, escape de aspas, `;` e quebra de linha, e o prefixo `'` contra fórmulas. Cobrir com testes: a linha exata do cenário do spec, o cabeçalho, uma transferência com conta de destino, uma transação sem categoria, uma observação com quebra de linha e aspas, um título `=SOMA(...)`, um ajuste ("Sim"), a ordem cronológica e os filtros Tudo, Ano, Mês e Intervalo nas bordas do dia, no fuso.

## 4. Repositório

- [ ] 4.1 Adicionar ao DAO as exclusões de todas as tabelas e os inserts em lote, e `FinanceRepository.replaceAll(data)` dentro de `withTransaction`, mais `snapshot()` para ler tudo de uma vez. Cobrir no teste instrumentado: a substituição completa (planejado com `consumedOccurrences` e orçamento com categorias preservados) e uma falha forçada no meio, depois da qual os dados antigos continuam lá.
- [ ] 4.2 Mudar `seedIfEmpty` para usar a preferência `seeded`, testando no teste instrumentado (ou com um teste de unidade da regra) que um banco sem categorias, com `seeded` igual a true, continua vazio.

## 5. Serviço e ViewModel

- [ ] 5.1 Criar `data/DataTransferService`, com `writeBackup`, `readBackup` (teto de 20 MB), `restore` e `writeCsv`, usando `ContentResolver` em `Dispatchers.IO` e gravando `last_backup_at` e o fingerprint só depois do sucesso. Registrar o serviço no `AppContainer`. Verificar pelo build e pelo teste manual da seção 7.
- [ ] 5.2 Incluir no `MainViewModel` o `dataOp` (com bloqueio de operação simultânea), o `pendingRestore`, o fluxo do lembrete com `debounce` e o "Agora não". Depois de restaurar, voltar o `backStack` para `[Home]` e mostrar as mensagens do spec. Verificar pelo build e pelo teste manual.

## 6. Interface

- [ ] 6.1 Criar `Route.Settings` e `SettingsScreen` com as seções Organização, Seus dados (data do último backup e aviso de arquivo sem senha) e Sobre, com indicador de progresso. Trocar o ícone de Categorias pela engrenagem no topo da Início. Verificar no aparelho a navegação Início → Configurações → Categorias → voltar.
- [ ] 6.2 Ligar os launchers do SAF para backup e restauração, com o diálogo de confirmação que mostra a data e as contagens do arquivo e as atuais. Verificar no aparelho os cenários de cancelar o seletor, desistir e confirmar.
- [ ] 6.3 Criar a folha de exportação com chips de período, intervalo com validação e botão com a contagem. Verificar no aparelho a exportação do mês e o período vazio.
- [ ] 6.4 Criar o cartão de lembrete na Início, com "Fazer backup" abrindo o seletor e "Agora não". Verificar no aparelho, forçando as preferências por `adb shell run-as`, se necessário.
- [ ] 6.5 Atualizar o README: tirar "backup" e "exportação CSV" de "Fora do escopo" e acrescentar a seção de backup, explicando como restaurar num aparelho novo.

## 7. Verificação

- [ ] 7.1 Rodar `./gradlew -q :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` sem falhas.
- [ ] 7.2 No A26, instalar com `adb install --no-streaming -r` e rodar `am instrument`. Fazer backup para Downloads, criar uma transação, restaurar e conferir que ela sumiu e que os saldos batem. Tentar restaurar um `.json` qualquer e conferir que nada muda. Exportar o CSV e abri-lo no Google Planilhas para conferir as colunas, os valores numéricos e os acentos.
- [ ] 7.3 Pedir a revisão do código ao Codex (somente leitura), aplicar os achados válidos e registrá-los no PR.

## Workflow follow-up

- Abrir o PR da branch `feat/backup-exportacao` para o merge do usuário.
- Depois do merge, arquivar o change e sincronizar as specs.
