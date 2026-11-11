# STATUS

**Feito:** MVP, gráficos de Relatórios, backup/restauração/CSV, correções da release, AGENTS.md, widget e atalhos do ícone (versão 0.2.0, instalada no A26) (PRs #1–#9). Specs aprovadas: `transacoes-da-categoria`, `relatorio-por-conta`, `grupos-categorias-e-soma-no-valor`.
**Em revisão:** PR #11 (`transacoes-da-categoria`), 3 rodadas do Codex sem bloqueante pendente. A 0.3.0 assinada com a chave da release está instalada no A26; falta a conferência no aparelho (3.2).
**Depois:** conferir no aparelho e arquivar `transacoes-da-categoria`; depois `relatorio-por-conta`; depois `grupos-categorias-e-soma-no-valor` (a parte de Relatórios exige `transacoes-da-categoria` arquivado). A 0.3.0 (`versionCode` 3, deste PR) está instalada no A26; o próximo release usa `versionCode` 4.
**Bloqueado:** nada.
**Não mexer:** sem migração destrutiva no Room; o usuário tem dados reais na release. Instalar no A26 com `adb push` + `pm install -r` (o `adb install` trava no USB do WSL).
