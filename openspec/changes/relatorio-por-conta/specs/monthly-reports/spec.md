# Spec Delta

## ADDED Requirements

### Requirement: Relatório por conta
A tela Relatórios SHALL oferecer um filtro de conta com "Todas as contas" (padrão) e uma opção por conta. Com uma conta escolhida, todo o relatório (totais, resultado, comparação, evolução, pizza e lista) MUST considerar só as receitas e despesas dela, com as demais regras inalteradas. A escolha MUST ser mantida ao trocar de mês ou de tipo.

#### Scenario: Ver só o Pix
- **WHEN** em novembro há R$ 300,00 de despesas no BTG e R$ 200,00 no Pix, e o usuário toca no chip "Pix"
- **THEN** o relatório mostra R$ 200,00 de despesas e a pizza e a lista mostram só as categorias das despesas do Pix

#### Scenario: Trocar de conta
- **WHEN** o relatório está filtrado por "Pix" e o usuário toca em "BTG"
- **THEN** o relatório passa a mostrar só o BTG

#### Scenario: Voltar para todas
- **WHEN** o usuário toca em "Todas as contas"
- **THEN** o relatório volta a somar todas as contas, como antes

#### Scenario: Evolução da conta
- **WHEN** o relatório está filtrado por "Pix"
- **THEN** as barras dos últimos 6 meses mostram só receitas e despesas do Pix, e tocar num mês mantém o filtro

#### Scenario: Transferência entre contas
- **WHEN** o usuário transferiu R$ 100,00 do BTG para o Pix e filtra por qualquer uma das duas
- **THEN** a transferência não entra como receita nem como despesa da conta

#### Scenario: Conta fora do saldo total
- **WHEN** existe a conta "Investimentos", marcada para não entrar no saldo total, com despesas no mês
- **THEN** "Todas as contas" inclui essas despesas, como hoje, e o chip "Investimentos" mostra só elas

#### Scenario: Ajuste de saldo na conta
- **WHEN** o Pix teve um ajuste de saldo de R$ 20,00 no mês e o relatório está filtrado por Pix
- **THEN** o ajuste não entra nas receitas nem nas despesas do Pix

#### Scenario: Conta excluída
- **WHEN** o relatório está filtrado por uma conta e ela é excluída
- **THEN** o filtro volta para "Todas as contas"

#### Scenario: Conta sem movimento no mês
- **WHEN** a conta escolhida não tem receitas nem despesas no mês
- **THEN** os totais aparecem zerados e a seção por categoria mostra o estado vazio
