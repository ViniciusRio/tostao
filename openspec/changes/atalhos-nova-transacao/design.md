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
- **Rota inicial:** é definida só quando o VM é novo, ou seja, quando `backStack` ainda é `[Home]`. Na rotação, o VM sobrevive e os campos (`rememberSaveable`) também. Se o processo morrer, a intent é entregue de novo e a rota é recriada a partir dela (achado do Codex).
- **Novo toque com o formulário aberto:** os atalhos estáticos já chegam com `NEW_TASK | CLEAR_TASK`, o que cria um formulário novo. Para o widget o comportamento é o mesmo: `onNewIntent` troca a rota pelo novo tipo e incrementa uma chave de composição, para que o formulário recomece vazio. O rascunho anterior é descartado de propósito, para manter a regra simples e previsível (achado do Codex).
- O `exported` é `true`, com um filtro para a ação `QUICK_ADD` e a categoria `DEFAULT`. Assim os atalhos e o `adb` funcionam. A tela só abre um formulário, então expô-la não acrescenta risco.
- **Escolha do tipo:** vem do extra `type` (`EXPENSE`, `INCOME` ou `TRANSFER`) da intent, com a ação `com.viniciusrio.tostao.action.QUICK_ADD`. Um valor desconhecido vira `EXPENSE`.

### Sair pela raiz e esperar a gravação
- `MainViewModel` ganha `onRootBack: (() -> Unit)?`. `back()` com um item só chama esse callback. Ele é nulo na `MainActivity`, então nada muda lá. A `QuickAddActivity` também trata o voltar do sistema com `BackHandler`, que chama `vm.back()`.
- `saveTransaction` passa a guardar o `Job` em `pendingWrites` e a capturar exceções do repositório. Em caso de erro, mostra "Não foi possível salvar a transação" e marca a falha. O `onRootBack` da `QuickAddActivity` roda `lifecycleScope.launch { if (vm.awaitWrites()) finish() }`: só fecha se todas as gravações deram certo. Se alguma falhou, o formulário continua aberto com os dados (achado do Codex). A morte do processo no meio da gravação continua sendo um limite aceito.
- **Toque duplo em Salvar:** o formulário guarda o estado "salvando" e ignora novos toques. Isso vale também para o app principal (achado do Codex).
- **Categorias padrão com dois VMs:** os dois podem ler `seeded=false` no primeiro uso, mas `insertCategories` usa IDs fixos (`def-exp-*`, `def-inc-*`) com `OnConflictStrategy.IGNORE`, então não há duplicação. O VM do formulário rápido também roda o loop de minuto e o fingerprint, que são baratos. Um VM dedicado ficaria para outro change.
- **Confirmação:** a `QuickAddActivity` observa `vm.message` e mostra um `Toast` curto ("Despesa registrada"), que continua visível depois que a activity fecha. Não há "Desfazer" no toast. Para corrigir, o usuário edita ou exclui a transação no app. Fica registrado como trade-off.

### Sem conta
Quando os dados carregam sem nenhuma conta, a `QuickAddActivity` abre a `MainActivity` com o extra `open=account_form` e as flags `NEW_TASK | CLEAR_TOP | SINGLE_TOP`, que entregam a intent à instância existente por `onNewIntent` ou criam uma nova. Depois se fecha. A `MainActivity` mostra a mesma mensagem do botão "+" e navega para `AccountForm` só se a tela atual ainda não for o cadastro de conta nova, ou seja, a navegação é idempotente (achado do Codex).

### Atalhos estáticos
`res/xml/shortcuts.xml`, referenciado na `MainActivity` (a activity com `LAUNCHER`) por `<meta-data android:name="android.app.shortcuts">`. Cada atalho tem uma intent explícita para a `QuickAddActivity` (`targetPackage` e `targetClass`), um rótulo curto ("Despesa", "Receita", "Transferir"), um rótulo longo ("Nova despesa", "Nova receita", "Transferência") e um ícone vetorial próprio, em `drawable/`, com fundo circular na cor do launcher.

### Widget
- `res/xml/quick_expense_widget.xml`: `minWidth`/`minHeight` de 40dp, `targetCellWidth`/`targetCellHeight` igual a 1, `resizeMode="none"`, `widgetCategory="home_screen"`, `previewLayout` (API 31+) e `description`.
- O layout é um `LinearLayout` vertical com `ImageView` (o ícone de despesa) e `TextView` ("Despesa") sobre fundo arredondado. O `contentDescription` é "Nova despesa no Tostão".
- `PendingIntent.getActivity(..., REQUEST_EXPENSE (constante), ..., FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)` para a `QuickAddActivity`, com a ação `QUICK_ADD` e `type=EXPENSE`. O `contentDescription` fica na view raiz clicável, e o ícone e o texto ficam fora da acessibilidade, para não haver leitura duplicada.

### Widget de saldo (futuro, não implementado)
Quando entrar, ele deve seguir a preferência "ocultar valores" do app, como o usuário decidiu, e ser atualizado quando essa preferência ou os dados mudarem.

## Risks / Trade-offs

- [Sem desfazer no toast] → Existe a edição e a exclusão no app. O registro rápido é o caso comum.
- [Dois `MainViewModel` vivos ao mesmo tempo] → Ambos leem o mesmo Room e só escrevem por operações atômicas do repositório. A `last_account` em `SharedPreferences` é compartilhada, que é o comportamento desejado.
- [Launchers que não mostram atalhos estáticos] → O widget e o botão "+" continuam disponíveis.
