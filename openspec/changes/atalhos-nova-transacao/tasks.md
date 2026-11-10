# Tasks

## 1. Navegação de saída

- [ ] 1.1 Adicionar ao `MainViewModel` `onRootBack`, o rastreio de `pendingWrites` em `saveTransaction` e `awaitWrites()`. Verificar que na `MainActivity` o voltar na raiz continua igual e que os testes atuais passam.
- [ ] 1.2 Fazer a `MainActivity` aceitar o extra `open=account_form` (em `onCreate` e `onNewIntent`), com a mensagem e a navegação para `AccountForm`. Verificar com `adb shell am start -n .../.MainActivity --es open account_form`.

## 2. Formulário rápido

- [ ] 2.1 Criar a `QuickAddActivity` (tarefa própria, fora dos recentes), com o tipo vindo do extra, `BackHandler`, toast da mensagem, espera da gravação antes do `finish()` e o desvio para o cadastro de conta quando não houver contas. Registrar no manifesto. Verificar com `adb shell am start -a com.viniciusrio.tostao.action.QUICK_ADD --es type INCOME`: salvar, cancelar e conferir os recentes.

## 3. Atalhos e widget

- [ ] 3.1 Criar os ícones vetoriais, as strings e `res/xml/shortcuts.xml` e ligá-los à `MainActivity`. Verificar no A26 pressionando o ícone, com os três atalhos abrindo o tipo certo.
- [ ] 3.2 Criar o `QuickExpenseWidget` (RemoteViews), o layout, o info XML e o receiver no manifesto. Verificar no A26 adicionando o widget à tela inicial: um toque abre a despesa e salvar volta para a tela inicial.
- [ ] 3.3 Atualizar o README (o que tem e o que fica fora do escopo). Verificar lendo o texto.

## 4. Verificação

- [ ] 4.1 Rodar `./gradlew -q :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` sem falhas, incluindo lint limpo para os atalhos e o widget.
- [ ] 4.2 No A26: instalar, rodar `am instrument` e testar o cenário "App aberto em outra tela" (o app em Relatórios continua em Relatórios com o número atualizado) e o primeiro uso sem conta (em um app limpo, via `pm clear` só se o usuário permitir; senão, simular com o extra).
- [ ] 4.3 Pedir a revisão de código ao Codex (somente leitura), aplicar os achados válidos e registrá-los no PR.

## Workflow follow-up

- Abrir o PR para o merge do usuário e arquivar o change depois do merge.
