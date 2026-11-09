# Tasks

## 1. Base do projeto

- [x] 1.1 Configurar Gradle (catálogo de versões, módulo `:app`, Compose, Room/KSP, desugaring), `.gitignore` excluindo `re/`
- [x] 1.2 Tema Material 3 próprio (claro/escuro do sistema), tipografia e tokens de cor semânticos (receita, despesa, alerta)

## 2. Domínio (Kotlin puro, com testes unitários)

- [x] 2.1 `Money` em centavos: parsing de entrada pt-BR e formatação `R$ 1.234,56` com sinal
- [x] 2.2 Cálculo de saldos por conta, total incluído e total excluído, ignorando transações futuras
- [x] 2.3 Recorrência: próxima data para dias/semanas/meses/anos com dia âncora e data final
- [x] 2.4 Classificação de planejados (atrasado, próximo em 30 dias) e saldo previsto do mês
- [x] 2.5 Progresso de orçamento (normal, atenção, excedido)
- [x] 2.6 Relatório mensal: totais, agrupamento por categoria, variação vs mês anterior
- [x] 2.7 Agrupamento por dia com rótulo relativo e busca sem acento

## 3. Dados

- [x] 3.1 Entidades e DAOs Room: contas, categorias, transações, regras planejadas, orçamentos e junção orçamento-categoria
- [x] 3.2 Repositório com Flows, transações atômicas (excluir conta em cascata, pagar planejado) e categorias padrão na criação do banco
- [x] 3.3 Teste instrumentado do repositório (cascata, pagar/desfazer)

## 4. Interface

- [x] 4.1 Navegação por pilha própria, barra inferior com 4 destinos e botão Adicionar com Despesa/Receita/Transferência
- [x] 4.2 Início: saldo, ocultar valores, resumo do mês, saldo previsto, atrasados/próximos com Pagar/Pular/Desfazer, recentes, estado vazio
- [x] 4.3 Formulário de transação (receita, despesa, transferência), edição, exclusão com Desfazer, conta padrão
- [x] 4.4 Transações: mês, filtros por tipo/conta, agrupamento por dia, busca
- [x] 4.5 Contas: lista com totais, cadastro/edição, reordenar, ajuste de saldo, exclusão com confirmação
- [x] 4.6 Categorias: lista por tipo, cadastro/edição/exclusão
- [x] 4.7 Planejamento: planejados (formulário com recorrência) e orçamentos (formulário e progresso)
- [x] 4.8 Relatórios: totais, abas despesas/receitas por categoria, variação mensal
- [x] 4.9 Acessibilidade: descrições, 48 dp, estados com texto

## 5. Verificação

- [x] 5.1 Testes unitários, lint e build de debug passando
- [ ] 5.2 Instalar no A26, percorrer os fluxos principais e capturar telas
- [x] 5.3 Revisão do Codex (somente leitura) e correções
