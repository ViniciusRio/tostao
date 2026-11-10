# Design

## Context

- O app tem uma única `MainActivity` com pilha de navegação própria (`MainViewModel.backStack`). O formulário `TransactionFormScreen(vm, data, type, editId)` grava com `vm.saveTransaction(...)`, que dispara uma coroutine no `viewModelScope`, e logo depois chama `vm.back()`.
- `vm.back()` não faz nada quando a pilha tem um só item.
- O ícone do launcher é adaptativo (`mipmap-anydpi-v26`). O minSdk é 26, então os atalhos estáticos (API 25+) e os widgets estão disponíveis em todos os aparelhos suportados.

## Goals / Non-Goals

**Goals:** abrir o formulário com um toque, de fora do app, e voltar à origem; reaproveitar o formulário existente sem duplicá-lo.

**Non-Goals:** o widget de saldo (decisão do usuário), tile de Configurações Rápidas, atalhos dinâmicos ou fixados pelo usuário e um formulário simplificado só para o widget.

## Decisions

### RemoteViews em vez de Glance
O widget é um único botão estático, sem estado e sem atualização periódica (`updatePeriodMillis = 0`). Com RemoteViews isso é um `AppWidgetProvider` com um layout XML e um `PendingIntent`. O Glance traria `androidx.glance:glance-appwidget` (e o WorkManager junto), mais tempo de inicialização e mais tamanho de APK, sem ganho para um botão. Se o widget de saldo entrar depois, com estado e atualização, o Glance volta a ser avaliado.

### `QuickAddActivity` separada, em tarefa própria
- `android:taskAffinity=""`, `excludeFromRecents="true"`, `launchMode="singleTask"` e `noHistory` desligado. Com isso, o atalho cria uma tarefa à parte, e ao fechar o sistema volta para quem a abriu (launcher ou outro app). A tarefa do app principal fica intocada, inclusive a tela em que ele estava.
- **Alternativa descartada:** abrir a `MainActivity` com um extra e empilhar `TxForm`. Isso mexeria na pilha do app principal (cenário "App aberto em outra tela") e, ao salvar, cairia na Início, que é o comportamento que o usuário não quer.
- A activity tem seu próprio `MainViewModel` (outro `ViewModelStore`), com `backStack = [TxForm(tipo)]`. Ela compartilha o `AppContainer`, portanto o mesmo banco. O `Flow` do Room atualiza o app principal sozinho.
- **Escolha do tipo:** vem do extra `type` (`EXPENSE`, `INCOME` ou `TRANSFER`) da intent, com a ação `com.viniciusrio.tostao.action.QUICK_ADD`. Um valor desconhecido vira `EXPENSE`.

### Sair pela raiz e esperar a gravação
- `MainViewModel` ganha `onRootBack: (() -> Unit)?`. `back()` com um item só chama esse callback. Ele é nulo na `MainActivity`, então nada muda lá. A `QuickAddActivity` também trata o voltar do sistema com `BackHandler`, que chama `vm.back()`.
- `saveTransaction` passa a guardar o `Job` em `pendingWrites`. O `onRootBack` da `QuickAddActivity` roda `lifecycleScope.launch { vm.awaitWrites(); finish() }`, então a gravação nunca é cancelada pelo fim da activity.
- **Confirmação:** a `QuickAddActivity` observa `vm.message` e mostra um `Toast` curto ("Despesa registrada"), que continua visível depois que a activity fecha. Não há "Desfazer" no toast. Para corrigir, o usuário edita ou exclui a transação no app. Fica registrado como trade-off.

### Sem conta
Quando os dados carregam sem nenhuma conta, a `QuickAddActivity` abre a `MainActivity` com o extra `open=account_form` (`FLAG_ACTIVITY_NEW_TASK`) e se fecha. A `MainActivity` lê o extra em `onCreate` e `onNewIntent`, mostra a mesma mensagem do botão "+" e navega para `AccountForm`.

### Atalhos estáticos
`res/xml/shortcuts.xml`, referenciado na `MainActivity` (a activity com `LAUNCHER`) por `<meta-data android:name="android.app.shortcuts">`. Cada atalho tem uma intent explícita para a `QuickAddActivity` (`targetPackage` e `targetClass`), um rótulo curto ("Despesa", "Receita", "Transferir"), um rótulo longo ("Nova despesa", "Nova receita", "Transferência") e um ícone vetorial próprio, em `drawable/`, com fundo circular na cor do launcher.

### Widget
- `res/xml/quick_expense_widget.xml`: `minWidth`/`minHeight` de 40dp, `targetCellWidth`/`targetCellHeight` igual a 1, `resizeMode="none"`, `widgetCategory="home_screen"`, `previewLayout` (API 31+) e `description`.
- O layout é um `LinearLayout` vertical com `ImageView` (o ícone de despesa) e `TextView` ("Despesa") sobre fundo arredondado. O `contentDescription` é "Nova despesa no Tostão".
- `PendingIntent.getActivity(..., FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)` para a `QuickAddActivity` com `type=EXPENSE`.

### Widget de saldo (futuro, não implementado)
Quando entrar, ele deve seguir a preferência "ocultar valores" do app, como o usuário decidiu, e ser atualizado quando essa preferência ou os dados mudarem.

## Risks / Trade-offs

- [Sem desfazer no toast] → Existe a edição e a exclusão no app. O registro rápido é o caso comum.
- [Dois `MainViewModel` vivos ao mesmo tempo] → Ambos leem o mesmo Room e só escrevem por operações atômicas do repositório. A `last_account` em `SharedPreferences` é compartilhada, que é o comportamento desejado.
- [Launchers que não mostram atalhos estáticos] → O widget e o botão "+" continuam disponíveis.
