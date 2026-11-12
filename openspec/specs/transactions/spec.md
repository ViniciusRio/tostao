# transactions Specification

## Purpose
Registrar e consultar movimentações de dinheiro: receitas, despesas e transferências entre contas.

## Requirements

### Requirement: Registrar transação
O app SHALL registrar receita ou despesa com valor maior que zero (até R$ 999.999.999,99), conta, data e hora, categoria opcional, título opcional (até 60 caracteres) e observação opcional (até 500 caracteres). O valor MUST ser armazenado em centavos inteiros.

O campo Valor do formulário de transação SHALL aceitar a soma de até 50 parcelas separadas por "+", cada uma no mesmo formato de um valor digitado sozinho (ex.: `12,50+8,30+15` ou `1.234,56+10`), ignorando espaços. Com mais de uma parcela, o app MUST mostrar o total junto do campo e, ao salvar, MUST registrar uma única transação com o total. Um botão ao lado do campo MUST inserir "+" na posição do cursor. Parcela vazia ("+" no início, no fim ou repetido) ou inválida, total zero ou acima do limite tornam o valor inválido. Ao editar, o campo mostra o total.

#### Scenario: Despesa rápida
- **WHEN** o usuário toca em "Despesa", digita 45,90, escolhe "Alimentação" e salva
- **THEN** a despesa é registrada na conta selecionada com data e hora atuais

#### Scenario: Valor inválido
- **WHEN** o valor está vazio ou é zero
- **THEN** o botão Salvar fica desabilitado

#### Scenario: Somar corridas
- **WHEN** o usuário digita `12,50+8,30+15` no valor de uma despesa
- **THEN** o app mostra "= R$ 35,80" junto do campo e, ao salvar, registra uma despesa de R$ 35,80

#### Scenario: Parcela incompleta
- **WHEN** o valor termina em "+" (ex.: `12,50+`)
- **THEN** o campo indica valor inválido e o botão Salvar fica desabilitado até a parcela ser completada ou o "+" removido

#### Scenario: Botão de somar
- **WHEN** o usuário digitou `12,50` e toca no botão "+" ao lado do campo
- **THEN** o campo passa a `12,50+` e o teclado continua aberto para a próxima parcela

#### Scenario: Formatos misturados
- **WHEN** o usuário digita `1.234,56 + 10` ou cola `45.90+0,10`
- **THEN** o total mostrado é R$ 1.244,56 ou R$ 46,00, respectivamente

#### Scenario: Sinal repetido ou no início
- **WHEN** o valor é `+12`, `12++3` ou `0+0`
- **THEN** o campo indica valor inválido e o botão Salvar fica desabilitado

#### Scenario: Total acima do limite
- **WHEN** a soma das parcelas passa de R$ 999.999.999,99
- **THEN** o campo indica valor inválido e não é possível salvar

### Requirement: Transferência entre contas
O app SHALL registrar transferência com conta de origem, conta de destino diferente e valor maior que zero (até R$ 999.999.999,99), sem categoria.

#### Scenario: Mesma conta
- **WHEN** origem e destino são a mesma conta
- **THEN** o app impede salvar e explica o motivo

### Requirement: Conta padrão
O formulário SHALL pré-selecionar a última conta usada; sem histórico, a primeira conta da ordem. Sem nenhuma conta, o app MUST levar o usuário a criar uma antes de registrar.

#### Scenario: Sem contas
- **WHEN** não existe conta e o usuário toca em adicionar transação
- **THEN** o app explica que é preciso uma conta e abre o cadastro de conta

### Requirement: Editar e excluir com desfazer
O app SHALL permitir editar todos os campos de uma transação. Ao excluir, MUST mostrar uma mensagem com a ação "Desfazer" por alguns segundos, que restaura a transação idêntica.

#### Scenario: Ações sucessivas
- **WHEN** o usuário exclui uma transação e, antes de tocar em "Desfazer", exclui outra
- **THEN** a mensagem passa a se referir apenas à última exclusão; a primeira não pode mais ser desfeita

#### Scenario: Desfazer exclusão
- **WHEN** o usuário exclui uma despesa e toca em "Desfazer"
- **THEN** a despesa volta com os mesmos dados e os saldos voltam ao valor anterior

### Requirement: Lista de transações
Datas e horas de transações SHALL ser interpretadas no fuso atual do aparelho para agrupar por dia e por mês. O app SHALL listar transações efetivadas do mês selecionado, da mais recente para a mais antiga, agrupadas por dia com cabeçalho relativo ("Hoje", "Ontem" ou data como "17 de set."), mostrando o total do dia. Transações com a mesma data e hora MUST aparecer da lançada por último para a lançada primeiro. O usuário MUST poder navegar entre meses e filtrar por tipo, por conta e por categoria ou grupo de categorias.

Com filtro de categoria, de grupo ou "Sem categoria", a lista SHALL mostrar só receitas e despesas daquele item, sem transferências nem ajustes de saldo (as mesmas regras de Relatórios), e um resumo com o total de despesas e o de receitas do mês, cada um só quando houver. Para grupo, o resumo MUST detalhar cada categoria do grupo com movimento. O filtro combina com tipo e conta. Ao escolher o tipo "Transferências", o filtro volta para "Todas" e fica indisponível.

#### Scenario: Navegar meses
- **WHEN** o usuário toca na seta de mês anterior
- **THEN** a lista mostra as transações do mês anterior e o título do mês muda

#### Scenario: Filtrar pelo grupo Saúde
- **WHEN** o mês tem R$ 500,00 em Plano de saúde, R$ 300,00 em Psicólogo e R$ 0,00 em Seguro de vida, e o usuário filtra pelo grupo "Saúde"
- **THEN** a lista mostra só essas transações e o resumo mostra "Saúde · Despesas R$ 800,00", com Plano de saúde R$ 500,00 e Psicólogo R$ 300,00

#### Scenario: Filtrar por uma categoria
- **WHEN** o usuário filtra pela categoria "Psicólogo"
- **THEN** a lista mostra só as transações de Psicólogo e o resumo mostra o total delas no mês

#### Scenario: Sem categoria não inclui transferências
- **WHEN** o mês tem uma despesa sem categoria, uma transferência e um ajuste de saldo, e o usuário filtra por "Sem categoria"
- **THEN** a lista mostra só a despesa sem categoria

#### Scenario: Resumo igual ao relatório
- **WHEN** Relatórios mostra Saúde com R$ 800,00 em novembro
- **THEN** filtrar Transações de novembro por Saúde mostra o mesmo R$ 800,00 no resumo

#### Scenario: Mesmo horário
- **WHEN** o usuário lança duas despesas no mesmo minuto
- **THEN** a lançada por último aparece acima da outra

### Requirement: Busca
O app SHALL buscar transações por título, observação, nome da categoria, nome do grupo da categoria ou valor, ignorando maiúsculas e acentos, em todos os meses.

#### Scenario: Buscar sem acento
- **WHEN** o usuário busca "farmacia"
- **THEN** aparecem transações com título "Farmácia"

#### Scenario: Buscar pelo grupo
- **WHEN** o usuário busca "saude" e "Psicólogo" está no grupo "Saúde"
- **THEN** aparecem as transações de Psicólogo
