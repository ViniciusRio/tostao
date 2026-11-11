# Spec Delta

## Purpose

Permitir agrupar, opcionalmente, categorias afins sob um "guarda-chuva" (ex.: Saúde, Contas básicas) para ver o total do grupo e o detalhamento por categoria, sem perder a visão por categoria.

## ADDED Requirements

### Requirement: Cadastro de grupos
O app SHALL permitir criar, editar e excluir grupos de categorias com nome (1 a 30 caracteres), tipo (receita ou despesa), cor e ícone. O nome do grupo MUST ser único entre os grupos do mesmo tipo, ignorando maiúsculas, e pode repetir o nome de uma categoria. Grupos são opcionais: sem nenhum grupo, o app MUST se comportar como antes.

#### Scenario: Criar grupo Saúde
- **WHEN** o usuário cria o grupo de despesa "Saúde" e marca Plano de saúde, Psicólogo e Seguro de vida
- **THEN** o grupo aparece em Categorias com essas três categorias dentro dele

#### Scenario: Nome repetido
- **WHEN** já existe o grupo de despesa "Saúde" e o usuário tenta criar outro grupo de despesa "saúde"
- **THEN** o app impede salvar e explica que já existe um grupo com esse nome

#### Scenario: Grupo com nome de categoria
- **WHEN** existe a categoria "Saúde" e o usuário cria o grupo "Saúde"
- **THEN** o grupo é criado e a categoria "Saúde" pode ficar dentro dele

### Requirement: Categorias no grupo
Cada categoria SHALL pertencer a no máximo um grupo, e só a um grupo do mesmo tipo. O usuário MUST poder escolher o grupo no formulário da categoria (inclusive "Sem grupo") e marcar as categorias no formulário do grupo; marcar no grupo uma categoria que estava em outro grupo MUST movê-la. Um grupo pode ficar sem categorias.

#### Scenario: Mover categoria de grupo
- **WHEN** "Internet" está no grupo "Lazer" e o usuário a marca no grupo "Contas básicas"
- **THEN** "Internet" passa a estar só em "Contas básicas"

#### Scenario: Seletor do formulário de categoria
- **WHEN** o usuário edita uma categoria de despesa
- **THEN** o seletor de grupo mostra "Sem grupo" e apenas os grupos de despesa

#### Scenario: Categoria muda de tipo
- **WHEN** uma categoria sem uso que está num grupo de despesa é alterada para receita
- **THEN** ela sai do grupo e fica "Sem grupo"

### Requirement: Lista de categorias por grupo
Em Configurações > Categorias, o app SHALL mostrar, para o tipo selecionado, cada grupo (na ordem em que foram criados, com nome e quantidade de categorias) seguido das suas categorias na ordem atual das categorias, e depois as categorias sem grupo. Tocar no grupo MUST abrir a edição do grupo; tocar numa categoria MUST abrir a edição da categoria.

#### Scenario: Sem grupos
- **WHEN** o usuário não criou nenhum grupo
- **THEN** a lista de categorias aparece como antes, sem seção de grupos

### Requirement: Excluir grupo
Excluir um grupo SHALL manter suas categorias, que passam a "Sem grupo"; transações, planejamentos e orçamentos MUST NOT ser alterados. O app MUST pedir confirmação informando quantas categorias ficarão sem grupo.

#### Scenario: Excluir Saúde
- **WHEN** o usuário exclui o grupo "Saúde", que tem 3 categorias, e confirma
- **THEN** o grupo some e as 3 categorias continuam existindo, sem grupo, com todas as suas transações

### Requirement: Excluir categoria agrupada
Excluir uma categoria que está num grupo SHALL apenas removê-la do grupo, com as mesmas regras de exclusão de categoria; o grupo MUST continuar existindo mesmo que fique vazio.

#### Scenario: Grupo fica vazio
- **WHEN** o grupo "Pets" tem só a categoria "Veterinário" e ela é excluída
- **THEN** o grupo "Pets" continua em Categorias, com zero categorias

### Requirement: Dados existentes ao atualizar
Ao atualizar o app para a versão com grupos, todas as contas, categorias, transações, planejamentos e orçamentos existentes SHALL ser preservados, e todas as categorias MUST começar "Sem grupo". Sem grupo, as categorias e as transações continuam funcionando e aparecendo como antes.

#### Scenario: Primeira abertura depois da atualização
- **WHEN** o usuário com 3 contas e 250 transações atualiza o app instalando por cima
- **THEN** os dados continuam todos lá, nenhuma categoria tem grupo e Relatórios e Transações mostram o mesmo que antes

#### Scenario: Agrupar dados antigos
- **WHEN** depois de atualizar o usuário coloca a categoria antiga "Psicólogo" no grupo "Saúde"
- **THEN** todas as transações já existentes de Psicólogo passam a contar no grupo Saúde, sem editar uma por uma

### Requirement: Grupo da transação vem da categoria
O grupo de uma transação SHALL ser sempre o grupo da sua categoria. Para mudar o grupo de uma transação, o usuário escolhe outra categoria ou muda o grupo da categoria.

#### Scenario: Trocar a categoria
- **WHEN** uma despesa em "Psicólogo" (grupo Saúde) é editada para a categoria "Lazer" (sem grupo)
- **THEN** ela deixa de contar em Saúde e passa a contar como Lazer

### Requirement: Grupo a partir do formulário de transação
No formulário de transação (e de planejamento), o seletor de categoria SHALL mostrar as categorias organizadas por grupo, com o nome de cada grupo como título e as categorias sem grupo no final. Dentro de cada seção, as usadas recentemente vêm primeiro; os grupos são ordenados pelo uso mais recente de qualquer categoria sua. O diálogo "Nova categoria" desse formulário MUST permitir escolher um grupo existente do mesmo tipo ou "Sem grupo" (padrão).

#### Scenario: Escolher categoria por grupo
- **WHEN** existem os grupos Saúde e Contas básicas e o usuário abre uma nova despesa
- **THEN** o seletor mostra "Saúde" com suas categorias, "Contas básicas" com as suas e, por último, as categorias sem grupo

#### Scenario: Sem grupos no seletor
- **WHEN** não existe nenhum grupo
- **THEN** o seletor de categoria aparece como antes, sem títulos

#### Scenario: Criar categoria já no grupo
- **WHEN** ao lançar uma despesa o usuário cria a categoria "Dentista" no diálogo e escolhe o grupo "Saúde"
- **THEN** "Dentista" é criada dentro de Saúde e já fica selecionada na transação
