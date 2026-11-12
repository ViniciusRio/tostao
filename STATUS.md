# STATUS

**Feito:** release 0.5.0 (versionCode 5) instalada no A26 por cima da 0.4.0, com dados preservados (backup feito antes). MVP, gráficos de Relatórios, backup/restauração/CSV, correções da release, AGENTS.md, widget e atalhos do ícone, Transações da categoria, Relatórios por conta (release 0.4.0, versionCode 4, instalada no A26) (PRs #1–#14).
**Em revisão:** PR #15, `grupos-categorias-e-soma-no-valor` (soma no campo Valor, grupos de categorias, Room v2, backup formato 2, filtro por categoria/grupo, Relatórios "Por grupo"). Codex: 2 rodadas, sem bloqueante.
**Depois:** merge do PR #15 pelo Vinícius; arquivar o change. O próximo release usa `versionCode` 6.
**Bloqueado:** nada.
**Não mexer:** sem migração destrutiva no Room; o usuário tem dados reais na release. O debug agora é `com.viniciusrio.tostao.debug` (instala ao lado da release); testes no aparelho: `adb push` + `pm install -r` dos dois APKs e `am instrument -w com.viniciusrio.tostao.debug.test/androidx.test.runner.AndroidJUnitRunner`. Use `timeout` no `adb push`: no WSL ele às vezes trava.
