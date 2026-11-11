# STATUS

**Feito:** MVP, gráficos de Relatórios, backup/restauração/CSV, correções da release, AGENTS.md, widget e atalhos do ícone, Transações da categoria, Relatórios por conta (versão 0.4.0, instalada no A26) (PRs #1–#13). Spec aprovada: `grupos-categorias-e-soma-no-valor`.
**Em revisão:** arquivamento de `relatorio-por-conta` (delta de `monthly-reports` sincronizado).
**Depois:** implementar `grupos-categorias-e-soma-no-valor` (a ligação da conta com a tela da categoria já foi feita aqui; o grupo precisa repassar o `accountId` também). O próximo release usa `versionCode` 5.
**Bloqueado:** nada.
**Não mexer:** sem migração destrutiva no Room; o usuário tem dados reais na release. Instalar no A26 com `adb push` + `pm install -r` (o `adb install` trava no USB do WSL).
