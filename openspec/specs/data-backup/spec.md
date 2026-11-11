# data-backup Specification

## Purpose
Proteger os dados do usuário contra perda do aparelho ou desinstalação: o usuário gera uma cópia completa em arquivo, guarda onde quiser e restaura depois, sem nuvem nem conta.

## Requirements

### Requirement: Gerar backup completo
O app SHALL gerar, a pedido do usuário, um arquivo com todos os dados financeiros: contas, categorias, transações (inclusive ajustes e futuras), regras planejadas com o progresso de ocorrências e orçamentos com suas categorias. O usuário MUST escolher nome e local pelo seletor de arquivos do sistema, e o app MUST NOT pedir permissão de armazenamento.

#### Scenario: Salvar backup
- **WHEN** o usuário toca em "Fazer backup" e escolhe uma pasta
- **THEN** o app grava ali o arquivo, com nome sugerido "tostao-backup-AAAA-MM-DD.json", e confirma com "Backup salvo"

#### Scenario: Cancelar o seletor
- **WHEN** o usuário fecha o seletor de arquivos sem escolher local
- **THEN** nada é gravado e nenhuma mensagem de erro aparece

#### Scenario: Falha ao gravar
- **WHEN** o local escolhido não aceita a gravação ou ela é interrompida
- **THEN** o app informa que não foi possível salvar o backup, tenta remover o arquivo incompleto e não registra a data de último backup; o backup só conta como salvo depois que o arquivo foi gravado e fechado por inteiro

#### Scenario: Retrato consistente
- **WHEN** o backup é gerado enquanto outra alteração está sendo gravada
- **THEN** o arquivo reflete os dados de um único momento, nunca uma mistura de antes e depois da alteração

#### Scenario: Dados grandes demais
- **WHEN** o backup gerado passaria de 50 MB
- **THEN** o app não grava o arquivo e informa o motivo, para nunca produzir um backup que ele mesmo recusaria

### Requirement: Formato do arquivo de backup
O arquivo SHALL ser JSON em UTF-8 com identificação do app, versão de formato (inteiro, começando em 1), data e hora de criação e os dados. Valores monetários MUST ser centavos inteiros e datas de vencimento MUST ser datas civis. Os dados restaurados MUST ser idênticos aos de origem, inclusive identificadores, ordem e progresso das regras planejadas. Todo estado que o app consegue gravar MUST produzir um backup que ele mesmo aceita restaurar.

#### Scenario: Ida e volta
- **WHEN** o usuário faz backup e depois restaura esse mesmo arquivo
- **THEN** contas, saldos, transações, planejados (com a mesma próxima ocorrência) e orçamentos ficam iguais aos do momento do backup

#### Scenario: Ajuste grande ou editado
- **WHEN** existe um ajuste de saldo maior que R$ 999.999.999,99 ou um ajuste editado depois para outro tipo ou com categoria
- **THEN** o backup desses dados é aceito na restauração

### Requirement: Validar antes de restaurar
Antes de qualquer alteração, o app SHALL ler e validar o arquivo inteiro: identificação do app, versão de formato suportada, campos obrigatórios, tipos e enumerações, as regras de cadastro do app (limites de valor e de texto, nomes únicos, combinações válidas de recorrência) e referências entre registros. Se houver qualquer erro, inclusive falha de leitura, o app MUST mostrar o motivo e MUST NOT alterar nenhum dado.

#### Scenario: Arquivo que não é backup
- **WHEN** o usuário escolhe uma foto ou um JSON de outro app
- **THEN** o app informa "Este arquivo não é um backup do Tostão" e os dados atuais continuam intactos

#### Scenario: Versão mais nova
- **WHEN** o arquivo tem versão de formato maior que a suportada
- **THEN** o app informa que o backup foi feito por uma versão mais nova do Tostão e pede para atualizar o app

#### Scenario: Referência quebrada
- **WHEN** uma transação do arquivo aponta para uma conta que não existe no arquivo
- **THEN** o app recusa o arquivo, informando que ele está corrompido ou incompleto, e nada é alterado

#### Scenario: Arquivo grande demais
- **WHEN** o arquivo tem mais de 50 MB
- **THEN** o app recusa a leitura sem carregá-lo inteiro na memória

#### Scenario: Falha de leitura
- **WHEN** o provedor de arquivos nega acesso ou a leitura é interrompida
- **THEN** o app informa que não conseguiu ler o arquivo e nada é alterado

#### Scenario: Recorrência impossível
- **WHEN** uma regra planejada tem intervalo 0, data final anterior à inicial ou um progresso cuja próxima data não pode ser calculada
- **THEN** o app recusa o arquivo e nada é alterado

### Requirement: Restaurar substituindo os dados
Depois de validar, o app SHALL mostrar a data do backup e quantas contas, transações, planejados e orçamentos ele contém, e MUST pedir confirmação explícita de que todos os dados atuais serão apagados e substituídos. Confirmada, a substituição MUST ser atômica: ou todos os dados passam a ser os do arquivo, ou nada muda.

#### Scenario: Confirmar restauração
- **WHEN** o usuário escolhe um backup válido de 01/11/2026 com 3 contas e 250 transações e toca em "Substituir dados"
- **THEN** os dados atuais são apagados, os do arquivo passam a valer e o app mostra "Backup restaurado"

#### Scenario: Desistir na confirmação
- **WHEN** o usuário toca em "Cancelar" na confirmação
- **THEN** nenhum dado é alterado

#### Scenario: Falha durante a gravação
- **WHEN** ocorre um erro do banco no meio da substituição
- **THEN** os dados anteriores continuam todos lá e o app informa que a restauração falhou

#### Scenario: Navegação depois de restaurar
- **WHEN** a restauração termina
- **THEN** o app volta para a Início e nenhuma tela aberta continua apontando para um registro que deixou de existir

#### Scenario: Desfazer pendente
- **WHEN** havia uma mensagem com "Desfazer" na tela no momento em que o usuário restaurou um backup
- **THEN** a mensagem some e nenhum desfazer anterior pode mais alterar os dados restaurados

#### Scenario: Última conta usada
- **WHEN** a última conta usada no formulário não existe no backup restaurado
- **THEN** o app esquece essa conta e o formulário passa a sugerir a primeira conta da ordem

### Requirement: Aviso de arquivo sem senha
As telas de backup SHALL avisar que o arquivo contém todos os dados financeiros sem criptografia e deve ser guardado em local de confiança.

#### Scenario: Ver aviso
- **WHEN** o usuário abre a seção "Seus dados" em Configurações
- **THEN** vê o aviso de que o backup não tem senha

### Requirement: Data do último backup
O app SHALL guardar a data do último backup bem-sucedido e mostrá-la em Configurações, ou "Nunca" quando não houver.

#### Scenario: Sem backup
- **WHEN** o usuário nunca fez backup
- **THEN** Configurações mostra "Último backup: Nunca"

### Requirement: Lembrete de backup
A Início SHALL mostrar um cartão de lembrete quando houver transações, os dados tiverem mudado desde o último backup e: o último backup tiver 30 dias ou mais; ou nunca tiver havido backup e a transação efetivada mais antiga tiver data de 7 dias atrás ou mais. A condição MUST ser reavaliada com a passagem do tempo, e o lembrete MUST NOT usar notificações.

#### Scenario: Backup antigo
- **WHEN** o último backup foi há 35 dias e o usuário registrou uma despesa depois dele
- **THEN** a Início mostra o cartão "Faça um backup dos seus dados" com os botões "Fazer backup" e "Agora não"

#### Scenario: Nada mudou
- **WHEN** o último backup foi há 40 dias, mas nenhum dado mudou desde então
- **THEN** o cartão não aparece

#### Scenario: Adiar
- **WHEN** o usuário toca em "Agora não"
- **THEN** o cartão some e só volta depois de 7 dias, se a condição continuar valendo

#### Scenario: Depois de restaurar
- **WHEN** o usuário restaura um backup
- **THEN** a data do último backup passa a ser a do arquivo restaurado, e o cartão só volta quando os dados mudarem
