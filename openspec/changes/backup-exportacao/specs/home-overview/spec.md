# Spec Delta

## MODIFIED Requirements

### Requirement: Painel inicial
A tela Início SHALL mostrar: saldo total; receitas e despesas do mês atual; pagamentos atrasados e próximos (até 30 dias) com Pagar/Pular; as 5 transações mais recentes com atalho para a lista completa; e, quando a condição de lembrete de backup valer, o cartão de lembrete de backup logo abaixo do saldo. O topo da Início MUST ter, nesta ordem, ocultar valores, busca, contas e Configurações.

#### Scenario: Primeiro uso
- **WHEN** não existe nenhuma conta
- **THEN** a Início mostra um estado vazio explicando o primeiro passo com o botão "Criar conta"

#### Scenario: Lembrete de backup na Início
- **WHEN** a condição de lembrete de backup vale
- **THEN** o cartão aparece abaixo do saldo, e "Fazer backup" abre direto o seletor de arquivos para salvar o backup

#### Scenario: Atalho de Configurações
- **WHEN** o TalkBack foca a engrenagem no topo da Início
- **THEN** anuncia "Configurações"
