# planned-payments Specification

## Purpose
Planejar contas e receitas futuras (únicas ou recorrentes) e acompanhar o que vence ou atrasou sem poluir o extrato.

## Requirements

### Requirement: Regras planejadas
O app SHALL permitir criar, editar e excluir regras de pagamento planejado com tipo (receita ou despesa), valor (até R$ 999.999.999,99), conta, categoria opcional, título obrigatório, data da primeira ocorrência e recorrência: única, ou a cada N dias, semanas, meses ou anos (N de 1 a 99), com data final opcional.

#### Scenario: Recorrência mensal no dia 31
- **WHEN** uma regra mensal começa em 31 de janeiro
- **THEN** as ocorrências seguintes são 28 (ou 29) de fevereiro, 31 de março, 30 de abril

#### Scenario: Anual em 29 de fevereiro
- **WHEN** uma regra anual começa em 29/02/2028
- **THEN** as ocorrências são 28/02/2029, 28/02/2030, 28/02/2031 e 29/02/2032

#### Scenario: Data final
- **WHEN** uma regra a cada 2 meses começa em 10/01 e termina em 15/05
- **THEN** as ocorrências são 10/01, 10/03 e 10/05, e a regra fica concluída depois de 10/05

#### Scenario: Editar a recorrência
- **WHEN** o usuário altera data inicial, unidade ou intervalo de uma regra
- **THEN** a próxima ocorrência passa a ser a nova data inicial; alterar só valor, conta, categoria ou título mantém a próxima ocorrência

### Requirement: Próxima ocorrência
Datas de vencimento SHALL ser datas civis (sem hora nem fuso). Cada regra SHALL ter uma única ocorrência pendente por vez: a mais antiga ainda não paga nem pulada. Ocorrências futuras MUST NOT aparecer no extrato nem afetar saldos.

#### Scenario: Extrato limpo
- **WHEN** existe uma regra mensal de R$ 300,00 até 2029
- **THEN** o extrato da conta não mostra ocorrências futuras e a regra aparece apenas em "Próximos" com sua próxima data

### Requirement: Pagar e pular
Ao pagar uma ocorrência, o app SHALL criar uma transação efetivada com os dados da regra e data atual e avançar para a próxima ocorrência. Ao pular, MUST apenas avançar. Ambas as ações MUST oferecer "Desfazer", válido apenas para a última ação: se a regra tiver avançado de novo depois dela, o desfazer é recusado com aviso.

#### Scenario: Pagar atrasado
- **WHEN** a ocorrência de 17 de outubro está atrasada e o usuário toca em "Pagar"
- **THEN** é criada a despesa com data de hoje, a próxima ocorrência vira 17 de novembro, e "Desfazer" remove a despesa e volta para 17 de outubro

#### Scenario: Regra única concluída
- **WHEN** a única ocorrência de uma regra única é paga ou pulada
- **THEN** a regra sai de Próximos/Atrasados e aparece como concluída na lista de planejados

### Requirement: Atrasados e próximos
O app SHALL classificar como "Atrasado" a ocorrência com data anterior a hoje e como "Próximo" a que vence de hoje até 30 dias, mostrando data relativa ("vence hoje", "em 3 dias", "há 5 dias") e totais de cada grupo.

#### Scenario: Vence hoje
- **WHEN** a próxima ocorrência é hoje
- **THEN** ela aparece em Próximos com o texto "vence hoje"
