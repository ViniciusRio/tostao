# Spec Delta

## ADDED Requirements

### Requirement: Grupos de categorias no backup
O backup SHALL incluir os grupos de categorias (identificador, nome, tipo, cor, ícone e ordem) e o grupo de cada categoria, num formato de versão 2. A restauração MUST aceitar os formatos 1 e 2: um arquivo de versão 1 é restaurado sem grupos. A validação MUST recusar um arquivo cujo grupo tenha nome repetido no mesmo tipo, cuja categoria aponte para um grupo inexistente ou para um grupo de outro tipo, e a contagem mostrada antes de confirmar MUST incluir os grupos quando houver.

#### Scenario: Ida e volta com grupos
- **WHEN** o usuário tem o grupo Saúde com 3 categorias, faz backup e restaura o mesmo arquivo
- **THEN** o grupo Saúde volta com as mesmas 3 categorias

#### Scenario: Backup antigo
- **WHEN** o usuário restaura um backup de formato 1, feito antes dos grupos
- **THEN** a restauração funciona e todas as categorias ficam sem grupo

#### Scenario: Grupo de outro tipo
- **WHEN** no arquivo uma categoria de receita aponta para um grupo de despesa
- **THEN** o app recusa o arquivo como corrompido e nada é alterado
