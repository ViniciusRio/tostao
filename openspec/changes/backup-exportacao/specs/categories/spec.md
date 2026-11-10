# Spec Delta

## MODIFIED Requirements

### Requirement: Categorias padrão
Na primeira abertura depois da instalação o app SHALL criar categorias padrão em pt-BR. Despesa: Alimentação, Mercado, Moradia, Transporte, Saúde, Educação, Lazer, Compras, Contas e serviços, Outros. Receita: Salário, Freelance, Rendimentos, Outros. As categorias padrão MUST NOT ser recriadas depois, mesmo que o usuário exclua todas ou restaure um backup sem categorias.

#### Scenario: Primeira abertura
- **WHEN** o app é aberto pela primeira vez
- **THEN** as categorias padrão existem e podem ser editadas ou excluídas

#### Scenario: Backup sem categorias
- **WHEN** o usuário restaura um backup sem categorias e reabre o app
- **THEN** o app continua sem categorias
