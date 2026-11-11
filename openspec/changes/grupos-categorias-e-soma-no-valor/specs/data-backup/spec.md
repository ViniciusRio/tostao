# Spec Delta

## ADDED Requirements

### Requirement: Grupos de categorias no backup
O backup SHALL incluir os grupos de categorias e o grupo de cada categoria, num formato de versão 2. A restauração MUST aceitar os formatos 1 e 2. Um arquivo de versão 1 é restaurado sem grupos, mesmo que traga campos de grupo. Num arquivo de versão 2, a validação MUST recusar grupo com nome repetido no mesmo tipo e categoria que aponte para um grupo inexistente ou de outro tipo. A confirmação MUST contar os grupos quando houver.

#### Scenario: Ida e volta com grupos
- **WHEN** o usuário tem o grupo Saúde com 3 categorias, faz backup e restaura o mesmo arquivo
- **THEN** o grupo Saúde volta com as mesmas 3 categorias

#### Scenario: Backup antigo
- **WHEN** o usuário restaura um backup de formato 1, feito antes dos grupos
- **THEN** a restauração funciona e todas as categorias ficam sem grupo

#### Scenario: Versão 1 com campos de grupo
- **WHEN** um arquivo declara versão 1 mas contém grupos
- **THEN** a restauração ignora os grupos e todas as categorias ficam sem grupo

#### Scenario: Atualizar não pede backup à toa
- **WHEN** o usuário fez backup na versão anterior, atualiza o app e não altera nenhum dado
- **THEN** o lembrete de backup não aparece por causa da atualização

#### Scenario: Grupo de outro tipo
- **WHEN** no arquivo uma categoria de receita aponta para um grupo de despesa
- **THEN** o app recusa o arquivo como corrompido e nada é alterado
