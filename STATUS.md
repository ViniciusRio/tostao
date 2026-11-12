# STATUS

**Feito:** MVP, gráficos de Relatórios, backup/restauração/CSV, correções da release, AGENTS.md, widget e atalhos do ícone, Transações da categoria, Relatórios por conta (release 0.4.0, versionCode 4, instalada no A26) (PRs #1–#14).
**Em revisão:** `grupos-categorias-e-soma-no-valor` implementado na branch `feat/grupos-categorias-e-soma-no-valor` (soma no campo Valor, grupos de categorias, Room v2, backup formato 2, filtro por categoria/grupo, Relatórios "Por grupo"); versão 0.5.0, versionCode 5.
**Depois:** liberar e fazer a tarefa 7.2 (release 0.5.0 assinada por cima da 0.4.0 no A26, conferindo os dados reais depois da migração 1→2); arquivar o change após o merge.
**Bloqueado:** nada.
**Não mexer:** sem migração destrutiva no Room; o usuário tem dados reais na release. O debug agora é `com.viniciusrio.tostao.debug` (instala ao lado da release); testes no aparelho: `adb push` + `pm install -r` dos dois APKs e `am instrument -w com.viniciusrio.tostao.debug.test/androidx.test.runner.AndroidJUnitRunner`. Use `timeout` no `adb push`: no WSL ele às vezes trava.
