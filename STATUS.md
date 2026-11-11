# STATUS

**Feito:** MVP, gráficos de Relatórios, backup/restauração/CSV, correções da release, AGENTS.md, widget e atalhos do ícone, Transações da categoria (versão 0.3.0, instalada no A26) (PRs #1–#12). Specs aprovadas: `relatorio-por-conta`, `grupos-categorias-e-soma-no-valor`.
**Em revisão:** implementação de `relatorio-por-conta` (branch `feat/relatorio-por-conta`); falta a verificação no aparelho da tarefa 2.1.
**Depois:** verificar `relatorio-por-conta` no aparelho e arquivar; depois `grupos-categorias-e-soma-no-valor` (a ligação da conta com a tela da categoria já foi feita aqui; o grupo precisa repassar o `accountId` também). O próximo release usa `versionCode` 4.
**Bloqueado:** nada.
**Não mexer:** sem migração destrutiva no Room; o usuário tem dados reais na release. Instalar no A26 com `adb push` + `pm install -r` (o `adb install` trava no USB do WSL).
