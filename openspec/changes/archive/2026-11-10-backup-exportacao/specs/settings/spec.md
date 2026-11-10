# Spec Delta

## Purpose

Reunir numa única tela de Configurações o que não é uso diário: a organização (categorias), a gestão dos dados (backup, restauração, exportação) e as informações do app.

## ADDED Requirements

### Requirement: Tela de Configurações
O app SHALL ter uma tela "Configurações" aberta pelo ícone de engrenagem no topo da Início, com as seções "Organização" (Categorias), "Seus dados" (Fazer backup, com a data do último backup; Restaurar backup; Exportar transações em CSV; e o aviso de arquivo sem senha) e "Sobre" (nome, versão e lema do app). Cada item MUST ter rótulo em texto e área de toque mínima de 48 dp.

#### Scenario: Abrir Configurações
- **WHEN** o usuário toca na engrenagem no topo da Início
- **THEN** a tela Configurações abre, com botão de voltar para a Início

#### Scenario: Ir para Categorias
- **WHEN** o usuário toca em "Categorias" em Configurações
- **THEN** a lista de categorias abre e o botão voltar retorna para Configurações

### Requirement: Operação de dados em andamento
Enquanto um backup, restauração ou exportação estiver em andamento, a tela SHALL mostrar um indicador de progresso e MUST NOT permitir iniciar outra dessas operações.

#### Scenario: Toque duplo
- **WHEN** o usuário toca em "Fazer backup" e, durante a gravação, toca em "Restaurar backup"
- **THEN** o segundo toque não faz nada até a primeira operação terminar
