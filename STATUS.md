# STATUS

**Feito:** MVP, gráficos de Relatórios, backup/restauração/CSV, correções da release, AGENTS.md, widget e atalhos do ícone, Transações da categoria (versão 0.3.0, instalada no A26) (PRs #1–#11). Specs aprovadas: `relatorio-por-conta`, `grupos-categorias-e-soma-no-valor`.
**Em revisão:** arquivamento de `transacoes-da-categoria` (delta de `monthly-reports` sincronizado).
**Depois:** implementar `relatorio-por-conta` (inclui a ligação da conta com `accountId` da tela da categoria e a tarefa 4.2 herdada); depois `grupos-categorias-e-soma-no-valor`. O próximo release usa `versionCode` 4.
**Bloqueado:** nada.
**Não mexer:** sem migração destrutiva no Room; o usuário tem dados reais na release. Instalar no A26 com `adb push` + `pm install -r` (o `adb install` trava no USB do WSL).
