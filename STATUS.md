# STATUS

**Feito:** MVP, gráficos de Relatórios, backup/restauração/CSV, correções da release, AGENTS.md, widget e atalhos do ícone, Transações da categoria, Relatórios por conta, grupos de categorias e soma no campo Valor (release 0.5.0, versionCode 5, instalada no A26 por cima da 0.4.0 com os dados preservados) (PRs #1–#15).
**Em revisão:** arquivamento de `grupos-categorias-e-soma-no-valor` (specs `category-groups`, `data-backup`, `monthly-reports` e `transactions` sincronizadas).
**Depois:** nenhum change aberto. O próximo release usa `versionCode` 6.
**Bloqueado:** nada.
**Não mexer:** sem migração destrutiva no Room; o usuário tem dados reais na release. O debug agora é `com.viniciusrio.tostao.debug` (instala ao lado da release); testes no aparelho: `adb push` + `pm install -r` dos dois APKs e `am instrument -w com.viniciusrio.tostao.debug.test/androidx.test.runner.AndroidJUnitRunner`. Use `timeout` no `adb push`: no WSL ele às vezes trava.
