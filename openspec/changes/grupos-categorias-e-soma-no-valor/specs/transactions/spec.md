# Spec Delta

## MODIFIED Requirements

### Requirement: Registrar transação
O app SHALL registrar receita ou despesa com valor maior que zero (até R$ 999.999.999,99), conta, data e hora, categoria opcional, título opcional (até 60 caracteres) e observação opcional (até 500 caracteres). O valor MUST ser armazenado em centavos inteiros.

O campo Valor do formulário de transação SHALL aceitar a soma de até 50 parcelas separadas por "+" (cada parcela no mesmo formato de um valor, ex.: `12,50+8,30+15`). Com mais de uma parcela, o app MUST mostrar o total junto do campo enquanto o usuário digita, e ao salvar MUST registrar uma única transação com o total. Um botão ao lado do campo MUST inserir "+" sem depender do teclado. Parcela vazia ou inválida torna o valor inválido, e o total MUST respeitar o mesmo limite de um valor digitado diretamente. Ao reabrir a transação para editar, o campo mostra o total, não as parcelas.

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

#### Scenario: Total acima do limite
- **WHEN** a soma das parcelas passa de R$ 999.999.999,99
- **THEN** o campo indica valor inválido e não é possível salvar

### Requirement: Lista de transações
Datas e horas de transações SHALL ser interpretadas no fuso atual do aparelho para agrupar por dia e por mês. O app SHALL listar transações efetivadas do mês selecionado, da mais recente para a mais antiga, agrupadas por dia com cabeçalho relativo ("Hoje", "Ontem" ou data como "17 de set."), mostrando o total do dia. Transações com a mesma data e hora MUST aparecer da lançada por último para a lançada primeiro. O usuário MUST poder navegar entre meses e filtrar por tipo, por conta e por categoria ou grupo de categorias.

Com filtro de categoria ou de grupo, a lista SHALL mostrar só as transações daquela categoria, ou de qualquer categoria do grupo, e um resumo com o total filtrado do mês. Para grupo, o resumo MUST detalhar o total de cada categoria do grupo que teve movimento no mês. Os filtros combinam entre si (tipo, conta e categoria/grupo), e o filtro de categoria/grupo MUST oferecer "Sem categoria".

#### Scenario: Navegar meses
- **WHEN** o usuário toca na seta de mês anterior
- **THEN** a lista mostra as transações do mês anterior e o título do mês muda

#### Scenario: Filtrar pelo grupo Saúde
- **WHEN** o mês tem R$ 500,00 em Plano de saúde, R$ 300,00 em Psicólogo e R$ 0,00 em Seguro de vida, e o usuário filtra pelo grupo "Saúde"
- **THEN** a lista mostra só essas transações e o resumo mostra "Saúde: R$ 800,00", com Plano de saúde R$ 500,00 e Psicólogo R$ 300,00

#### Scenario: Filtrar por uma categoria
- **WHEN** o usuário filtra pela categoria "Psicólogo"
- **THEN** a lista mostra só as transações de Psicólogo e o resumo mostra o total delas no mês

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
