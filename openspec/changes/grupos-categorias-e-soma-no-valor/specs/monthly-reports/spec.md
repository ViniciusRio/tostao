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
Na visão "Por grupo", a linha de um grupo SHALL ter um botão próprio de expandir/recolher que mostra cada categoria do grupo com movimento no mês, com valor e percentual dentro do grupo, em ordem decrescente de valor. Expandir ou recolher MUST NOT mudar a seleção da pizza nem abrir outra tela. Tocar numa categoria expandida MUST abrir "Transações da categoria" para aquela categoria.

#### Scenario: Expandir Saúde
- **WHEN** o usuário expande o grupo Saúde do exemplo anterior
- **THEN** aparecem Plano de saúde R$ 500,00 (63%) e Psicólogo R$ 300,00 (37%)

#### Scenario: Tocar numa categoria do grupo
- **WHEN** o usuário toca em Psicólogo dentro de Saúde expandido
- **THEN** abre "Transações da categoria" de Psicólogo com R$ 300,00, e a seleção da pizza não muda

### Requirement: Transações do grupo
Na visão "Por grupo", tocar na linha de um grupo SHALL abrir a tela "Transações da categoria" (change `transacoes-da-categoria`) em modo grupo: as transações de todas as categorias do grupo no mês e no tipo do relatório, com o nome, o ícone e o total do grupo no topo e o detalhamento por categoria com movimento. O total MUST ser igual ao da linha do grupo. Linhas de categoria sem grupo e de "Sem categoria" MUST se comportar como na visão por categoria.

#### Scenario: Abrir transações de Saúde
- **WHEN** o usuário está em "Por grupo" em novembro e toca na linha do grupo Saúde
- **THEN** abre "Saúde" com R$ 800,00, Plano de saúde R$ 500,00 e Psicólogo R$ 300,00 no detalhamento e as transações das duas categorias agrupadas por dia

#### Scenario: Grupo com conta
- **WHEN** o relatório está filtrado pela conta Pix e o usuário toca na linha do grupo Saúde
- **THEN** a tela mostra só as despesas de Saúde no Pix, com o total igual ao da linha

#### Scenario: Grupo excluído com a tela aberta
- **WHEN** a tela de transações do grupo Saúde está aberta e o grupo é excluído
- **THEN** o app volta para Relatórios, sem mostrar as transações de todas as categorias

#### Scenario: Outras na visão por grupo
- **WHEN** na visão "Por grupo" os itens Educação (sem grupo, 1%) e Pets (grupo, 2%) formam a fatia "Outras" e o usuário toca nela
- **THEN** o centro mostra "Outras" e a soma, e as linhas de Educação e Pets ficam destacadas; tocar depois na linha Pets abre só as transações do grupo Pets

#### Scenario: Fatia do grupo
- **WHEN** o usuário toca na fatia Saúde
- **THEN** a fatia fica destacada e o centro mostra "Saúde", o valor e o percentual, sem abrir outra tela
