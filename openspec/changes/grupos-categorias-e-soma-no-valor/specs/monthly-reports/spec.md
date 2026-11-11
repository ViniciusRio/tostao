# Spec Delta

## ADDED Requirements

### Requirement: Visão por grupo
Quando existir ao menos um grupo do tipo selecionado, Relatórios SHALL oferecer "Por categoria" (como hoje) ou "Por grupo", mantendo a escolha ao trocar de mês ou de tipo. Em "Por grupo", cada grupo com movimento MUST virar um item com o total das suas categorias, e categorias sem grupo (inclusive "Sem categoria") continuam como itens próprios. Pizza e lista MUST usar esses itens com as mesmas regras da visão por categoria.

#### Scenario: Total de Saúde
- **WHEN** o mês tem R$ 500,00 em Plano de saúde, R$ 300,00 em Psicólogo (ambas no grupo Saúde) e R$ 200,00 em Lazer (sem grupo), e o usuário escolhe "Por grupo"
- **THEN** a pizza mostra Saúde (80%) e Lazer (20%), e a lista mostra Saúde R$ 800,00 e Lazer R$ 200,00

#### Scenario: Voltar a ver separado
- **WHEN** o usuário está em "Por grupo" e escolhe "Por categoria"
- **THEN** a pizza e a lista voltam a mostrar Plano de saúde, Psicólogo e Lazer separadamente

#### Scenario: Sem grupos
- **WHEN** não existe grupo de despesa
- **THEN** a escolha "Por categoria"/"Por grupo" não aparece e o relatório é o atual

#### Scenario: Grupo sem movimento
- **WHEN** o grupo "Contas básicas" não teve despesas no mês
- **THEN** ele não aparece na pizza nem na lista daquele mês

### Requirement: Detalhamento do grupo
Na visão "Por grupo", a linha de um grupo SHALL poder ser expandida para mostrar cada categoria do grupo com movimento no mês, com valor e percentual dentro do grupo, em ordem decrescente de valor. Expandir ou recolher MUST NOT mudar a seleção. Tocar numa categoria expandida MUST destacar a fatia do grupo e mostrar no centro o nome, o valor e o percentual dessa categoria sobre o total do tipo.

#### Scenario: Expandir Saúde
- **WHEN** o usuário expande o grupo Saúde do exemplo anterior
- **THEN** aparecem Plano de saúde R$ 500,00 (63%) e Psicólogo R$ 300,00 (37%)

#### Scenario: Tocar numa categoria do grupo
- **WHEN** o usuário toca em Psicólogo dentro de Saúde expandido
- **THEN** a fatia Saúde fica destacada e o centro mostra "Psicólogo", "R$ 300,00" e "30%"

### Requirement: Ver transações do item
Com um item selecionado (categoria, grupo ou "Sem categoria"), Relatórios SHALL oferecer a ação "Ver transações", que abre Transações no mesmo mês, filtrada por aquele item, pelo tipo selecionado e, se o relatório estiver filtrado por uma conta, por essa conta. A ação MUST NOT aparecer para a fatia "Outras".

#### Scenario: Abrir transações de Saúde
- **WHEN** o usuário seleciona o grupo Saúde em novembro e toca em "Ver transações"
- **THEN** Transações abre em novembro, filtrada por Despesas e pelo grupo Saúde, com o resumo do grupo

#### Scenario: Ver transações com conta
- **WHEN** o relatório está filtrado pela conta Pix e o usuário seleciona Saúde e toca em "Ver transações"
- **THEN** Transações abre filtrada por Despesas, Saúde e pela conta Pix
