# Tasks

## 1. Navegação de saída

- [x] 1.1 Adicionar ao `MainViewModel` `onRootBack`, o rastreio de `pendingWrites` em `saveTransaction` (com captura de erro e mensagem) e `awaitWrites(): Boolean`, e bloquear o toque duplo em Salvar no formulário. Verificar que na `MainActivity` o voltar na raiz continua igual e que os testes atuais passam.
- [x] 1.2 Fazer a `MainActivity` aceitar o extra `open=account_form` (em `onCreate` e `onNewIntent`), com a mensagem e a navegação idempotente para `AccountForm`. Verificar com `adb shell am start -n com.viniciusrio.tostao/.MainActivity --es open account_form` duas vezes seguidas: só um formulário de conta deve abrir.

## 2. Formulário rápido

- [x] 2.1 Criar a `QuickAddActivity` (tarefa própria, fora dos recentes), com o tipo vindo do extra, `BackHandler`, toast da mensagem, espera da gravação antes do `finish()` e o desvio para o cadastro de conta quando não houver contas. Registrar no manifesto. Incluir a rota inicial só com VM novo, `onNewIntent` com reinício do formulário e fechamento só depois de gravar com sucesso. Verificar com `adb shell am start -n com.viniciusrio.tostao/.QuickAddActivity -a com.viniciusrio.tostao.action.QUICK_ADD --es type INCOME`: salvar, cancelar, girar a tela, tocar duas vezes em Salvar e conferir os recentes.

## 3. Atalhos e widget

- [x] 3.1 Criar os ícones vetoriais, as strings e `res/xml/shortcuts.xml` e ligá-los à `MainActivity`. Verificar no A26 pressionando o ícone, com os três atalhos abrindo o tipo certo. **Verificação parcial, aceita pelo Vinícius em 2026-11-11: gesto real no ícone não testado (Olauncher); intents verificadas via adb.**
- [x] 3.2 Criar o `QuickExpenseWidget` (RemoteViews), o layout, o info XML e o receiver no manifesto. Verificar no A26 adicionando o widget à tela inicial: um toque abre a despesa e salvar volta para a tela inicial. **Verificação parcial, aceita pelo Vinícius em 2026-11-11: widget não colocado na tela inicial (Olauncher); registro e intent verificados.**
- [x] 3.3 Atualizar o README (o que tem e o que fica fora do escopo). Verificar lendo o texto.

## 4. Verificação

- [x] 4.1 Rodar `./gradlew -q :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` sem falhas, incluindo lint limpo para os atalhos e o widget.
- [x] 4.2 No A26: instalar, rodar `am instrument` e testar o cenário "App aberto em outra tela" (o app em Relatórios continua em Relatórios com o número atualizado) e o primeiro uso sem conta, com o app realmente vazio (`pm clear` no debug, liberado pelo usuário), restaurando o backup depois. **Verificação parcial, aceita pelo Vinícius em 2026-11-11: primeiro uso sem conta não testado (exigiria limpar os dados reais).**
- [x] 4.3 Pedir a revisão de código ao Codex (somente leitura), aplicar os achados válidos e registrá-los no PR.

## Workflow follow-up

- Abrir o PR para o merge do usuário e arquivar o change depois do merge.

## Como foi verificado (A26, release 0.2.0, 2026-11-11)

- Atalhos: os 3 estão registrados no sistema (`dumpsys shortcut`) e cada intent abre o tipo certo. O gesto real de segurar o ícone não foi feito, porque o launcher do aparelho é o Olauncher.
- Widget: está registrado (`dumpsys appwidget`), o XML no APK está correto e a intent dele abre a despesa. Não foi colocado na tela inicial, porque o Olauncher não mostra widgets.
- Formulário rápido: voltar cancela e volta para onde o usuário estava, e o formulário não aparece nos recentes. O toque duplo em Salvar grava uma transação só. A rotação mantém o valor. Uma categoria criada no formulário e salva logo em seguida fica gravada na transação. Os dados de teste foram apagados.
- Atualização por cima da 0.1.0 com a mesma chave: os dados foram preservados.
- O primeiro uso sem conta não foi testado no aparelho, porque exigiria limpar os dados reais.
- Revisão do Codex: 3 rodadas no PR #8; o bloqueante da 3ª foi corrigido em `b1230f3`.

