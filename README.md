# Tostão

*Cada vintém no lugar.*

App Android de finanças pessoais, **local e offline**, em Kotlin + Jetpack Compose. Inspirado nas funções do Ivy Wallet, mas escrito do zero, com melhorias de UI/UX levantadas por engenharia reversa do app original (veja [docs/engenharia-reversa.md](docs/engenharia-reversa.md)).

> Projeto independente, sem relação com o Ivy Wallet ou seus autores. Nenhum código, arte ou texto do app original foi copiado.

## O que tem

- **Contas** com saldo inicial (pode ser negativo), cor, ícone, inclusão no saldo total, reordenação e **ajuste de saldo** (vira transação marcada, fora de relatórios).
- **Transações**: receita, despesa e transferência; edição; exclusão com **Desfazer**; lista por mês agrupada por dia ("Hoje", "Ontem"); filtros por tipo e conta; **busca sem acento** em todos os meses.
- **Planejados**: contas fixas e recorrências (dias/semanas/meses/anos, com data final). Só a **próxima ocorrência** aparece, sem poluir o extrato. Pagar/Pular com Desfazer.
- **Orçamentos** mensais por categoria ou "todas as despesas", com estados normal/atenção/excedido em texto.
- **Relatórios** do mês sem precisar de filtro: totais, resultado, ranking por categoria e comparação com o mês anterior.
- **Início**: saldo total, **saldo previsto no fim do mês**, receitas/despesas do mês, atrasados e próximos 30 dias, recentes. Botão para **ocultar valores**.

## Principais melhorias em relação ao original

| Original | Aqui |
| --- | --- |
| Valores em ponto flutuante (`REAL`) | Centavos inteiros (`Long`), soma exata |
| "BRL -1.234,50" | "-R$ 1.234,50" com cor e sinal |
| Funções escondidas atrás de uma seta sem rótulo | Barra inferior com 4 destinos rotulados |
| "Rendimento", "Renda", "Receita" misturados | Receitas / Despesas / Transferências |
| Pagar/Pular sem desfazer | Snackbar com Desfazer em ações destrutivas |
| Recorrências geram transações até 2029 no extrato | Só a próxima ocorrência é mostrada |
| Relatório vazio até criar filtro | Mês atual aberto direto |
| Leitor de tela anuncia "icon", "circle button" | Descrições em todos os controles, alvos de 48 dp |

## Arquitetura

- `domain/`: Kotlin puro (dinheiro, saldos, recorrência, previsão, orçamentos, relatórios), coberto por testes unitários.
- `data/`: Room (6 tabelas) e repositório com operações atômicas (excluir conta em cascata, pagar planejado).
- `ui/`: Compose Material 3, um `MainViewModel`, pilha de navegação própria.

Specs e decisões em [`openspec/`](openspec/) (SDD com [OpenSpec](https://github.com/Fission-AI/OpenSpec)).

## Comandos

```sh
./gradlew :app:testDebugUnitTest          # testes de domínio
./gradlew :app:lintDebug :app:assembleDebug
adb install --no-streaming -r app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleDebugAndroidTest   # depois: adb shell am instrument -w com.viniciusrio.tostao.test/androidx.test.runner.AndroidJUnitRunner
```

Requer JDK 17+ e Android SDK 36. minSdk 26.

## Fora do escopo do MVP

Empréstimos, várias moedas, tags, importação/exportação CSV, widgets, bloqueio biométrico, lembretes e backup/sincronização. Os dados ficam só no aparelho (`allowBackup=false`): desinstalar apaga tudo.
