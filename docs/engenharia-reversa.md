# Engenharia reversa: Ivy Wallet 2026.10.22

Referência estudada: **Ivy Wallet** (`com.ivy.wallet`), versão `2026.10.22` (versionCode 207), instalada pela Play Store no Samsung Galaxy A26 (`SM-A266M`). O objetivo foi entender **funcionalidades, modelo de dados e fluxos** para construir um app próprio, escrito do zero, com melhorias de UI/UX. Nenhum código, ícone, ilustração, fonte ou texto do app original foi copiado para este repositório.

> O próprio app informa que é de código aberto (tela "Mais": "Ivy Wallet é de código aberto"). Este projeto **não** usa esse código-fonte; é uma reimplementação independente a partir do comportamento observado. "Ivy Wallet" é marca dos seus autores.

## Ferramentas e procedimento

| Etapa | Ferramenta | Resultado (local, fora do Git) |
| --- | --- | --- |
| Identidade do pacote | REA 6.8.0 `inspect-adb-package` | `re/evidence/adb-package.json` |
| Cópia do conjunto de APKs (base + 3 splits) com SHA-256 | REA `pull-adb-package` | `re/evidence/pull-package.json` |
| Assinatura do APK | REA `inspect-apk-signature` | `re/evidence/apk-signature.json` |
| Manifesto, permissões, componentes | `aapt2 dump badging/xmltree` (SDK 35.0.0) | `re/evidence/manifest.txt` |
| Inventário de classes | `dexdump` | `re/evidence/classes.txt` (8.390 classes) |
| Esquema do banco Room | leitura das strings `CREATE TABLE` do `classes.dex` | `re/evidence/dex-sql.txt` |
| Textos da interface | `aapt2 dump resources` | `re/evidence/strings-en.txt` |
| Observação em execução | REA `start-adb-app`, `capture-adb-screen`, `uiautomator dump` | `re/screens/*/screen.png` |

O `base.apk` tem SHA-256 `f68b911864094d94de66401d0f152eccb4cd625497ed7f88b9ba383278e60b30`.

Limitações: `inspect-android-package`, `search-android-classes` e `inspect-android-method` do REA exigem o JAR `jadx-headless-mcp` 0.7.1, que não foi instalado (download de binário externo não autorizado nesta sessão). Por isso **não houve decompilação de métodos**. O código está ofuscado por R8 (só ~30 classes `com.ivy.*` mantêm nome). As conclusões vêm do manifesto, do esquema SQL, dos textos e da navegação no aparelho.

A pasta `re/` (APKs, capturas com dados financeiros pessoais, evidências) está no `.gitignore` e não é publicada.

## Arquitetura observada

- Activity única `com.ivy.wallet.RootActivity` com Jetpack Compose; ação `ivy.wallet.intent.action.add_transaction` para atalhos.
- Widgets Glance: adicionar transação (normal e compacto) e saldo. Tile de Configurações Rápidas (`PaymentTileService`).
- Room + WorkManager (lembretes diários: "Você registrou suas transações hoje?").
- Permissões: Internet, notificações, biometria (bloqueio do app), boot, localização (declarada, sem uso visível).
- Firebase presente (chaves no `resources.arsc`), backup/importação CSV, cotações de câmbio remotas.

## Modelo de dados (Room)

| Tabela | Campos relevantes |
| --- | --- |
| `accounts` | name, currency, color, icon, orderNum, includeInBalance |
| `categories` | name, color, icon, orderNum |
| `transactions` | accountId, type (INCOME/EXPENSE/TRANSFER), **amount REAL**, toAccountId, toAmount, title, description, dateTime, categoryId, dueDate, recurringRuleId, paidForDateTime, loanId |
| `planned_payment_rules` | startDate, intervalN, intervalType, oneTime, type, accountId, amount, categoryId, title |
| `budgets` | name, amount, categoryIdsSerialized, accountIdsSerialized |
| `loans`, `loan_records` | empréstimos concedidos/tomados e pagamentos |
| `tags`, `tags_association` | etiquetas |
| `exchange_rates` | taxa por par de moedas, com sobrescrita manual |
| `settings` | theme, currency, bufferAmount (reserva), name |

Todas as tabelas têm `isSynced`/`isDeleted` (exclusão lógica, legado de sincronização em nuvem).

## Fluxos observados

1. **Início**: saudação + seletor de mês; saldo total em destaque; cartões "Rendimento"/"Despesas" do mês; seções "Próximos"/"Atrasado" de pagamentos planejados com botões **Pular** e **Pagar**; histórico do mês.
2. **Botão +** (FAB central): leque com Adicionar receita, Adicionar despesa, Transferência de conta e Adicionar pagamento planejado.
3. **Nova despesa**: abre primeiro um **teclado numérico próprio** em folha modal com escolha de conta; depois a tela com categoria, tags, descrição, data e "Pague com".
4. **Contas** (2ª aba): saldo total e saldo excluído; lista de contas; menu para reordenar (arrastar).
5. **"Mais"** (seta no topo da Início): busca, atalhos para Configurações, Categorias, Modo escuro, Pagamentos planejados, Relatórios, Orçamentos, Empréstimos; meta de economia ("reserva").
6. **Relatórios**: totais de receitas e despesas, mas nada aparece até o usuário **definir um filtro**.
7. **Orçamentos**, **Empréstimos**, **Pagamentos planejados**: telas próprias acessadas pelo "Mais".

## Problemas de UI/UX identificados

| # | Problema observado | Evidência | Melhoria proposta |
| --- | --- | --- | --- |
| 1 | Valores monetários em `REAL` (ponto flutuante): risco de erros de arredondamento em somas | esquema `transactions.amount REAL` | Guardar centavos em inteiro (`Long`) |
| 2 | Moeda exibida como código ("BRL -1.234,50", "0,00 BRL"), ordem inconsistente | telas Início/Contas | Formato local `R$ 1.234,50` com sinal e cor semântica |
| 3 | Tradução inconsistente: "Rendimento", "RENDA", "Receita" para *income* | Início, Relatórios, FAB | Vocabulário único: **Receitas / Despesas / Transferências** |
| 4 | Funções principais escondidas num menu acionado por uma seta sem rótulo | botão `icon` no topo | Barra inferior com 4 destinos rotulados + botão de adicionar |
| 5 | Leitores de tela recebem "icon", "item icon", "circle button" | `uiautomator dump` | Descrições de conteúdo em todos os controles |
| 6 | "Pagar"/"Pular" agem na hora, sem desfazer | cartão de atrasados | Ação com Snackbar **Desfazer** |
| 7 | Regras recorrentes geram transações futuras até 2029 que inundam o extrato da conta | tela da conta | Mostrar só a **próxima ocorrência** de cada regra |
| 8 | Relatório vazio até configurar filtro | tela Relatórios | Relatório do mês atual por padrão, com gráfico por categoria |
| 9 | Datas pouco legíveis: "set. 17. 2029" e vencimentos em caixa alta ("VENCIMENTO EM SÁB., ...") | contas, atrasados | Datas relativas ("Hoje", "Ontem", "em 3 dias") e formato `17 de set.` |
| 10 | Contas identificadas apenas por cor (texto colorido em fundo escuro, contraste baixo) | reordenar contas | Ícone + nome em cor de texto padrão; cor só como acento |
| 11 | Saldo negativo gigante sem contexto do que o compõe | Início | Saldo com detalhamento por conta e indicador de saldo previsto |
| 12 | Teclado numérico próprio antes da categoria (duas etapas modais) | nova despesa | Formulário único: valor em destaque com teclado do sistema, categorias como chips recentes |
