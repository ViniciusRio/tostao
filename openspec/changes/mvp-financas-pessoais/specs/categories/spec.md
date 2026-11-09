# Spec Delta

## Purpose
Organizar receitas e despesas em categorias para orçamentos e relatórios.

## ADDED Requirements

### Requirement: Categorias padrão
Na primeira abertura o app SHALL criar categorias padrão em pt-BR. Despesa: Alimentação, Mercado, Moradia, Transporte, Saúde, Educação, Lazer, Compras, Contas e serviços, Outros. Receita: Salário, Freelance, Rendimentos, Outros.

#### Scenario: Primeira abertura
- **WHEN** o app é aberto pela primeira vez
- **THEN** as categorias padrão existem e podem ser editadas ou excluídas

### Requirement: Manutenção de categorias
O app SHALL permitir criar, editar e excluir categorias com nome (1 a 30 caracteres), tipo (receita ou despesa), cor e ícone. Nome MUST ser único dentro do mesmo tipo, ignorando maiúsculas.

#### Scenario: Excluir categoria em uso
- **WHEN** o usuário exclui uma categoria usada por transações
- **THEN** o app confirma, as transações e regras planejadas ficam "Sem categoria", a categoria sai dos orçamentos e orçamentos que ficariam sem categorias são excluídos (com aviso na confirmação)

#### Scenario: Mudar tipo de categoria em uso
- **WHEN** o usuário tenta mudar de despesa para receita uma categoria usada por transações, regras ou orçamentos
- **THEN** o app recusa e explica que a categoria está em uso

### Requirement: Categoria por tipo
Ao registrar receita, o seletor SHALL mostrar apenas categorias de receita; ao registrar despesa, apenas de despesa. Transferências MUST NOT ter categoria.

#### Scenario: Seletor de despesa
- **WHEN** o usuário abre o formulário de despesa
- **THEN** só aparecem categorias de despesa, com as usadas recentemente primeiro
