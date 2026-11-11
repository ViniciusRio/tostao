# STATUS

**Feito:** MVP, gráficos de Relatórios, backup/restauração/CSV, correções da release (PRs #1–#4).
**Em revisão:**
- PR #5 `docs/propostas-relatorio-conta-e-grupos`: specs de relatório por conta e de grupos de categorias + soma no valor.
- PR #6 `docs/agents-md`: este AGENTS.md, CLAUDE.md e STATUS.md.
- PR #7 `docs/transacoes-da-categoria`: spec da tela de transações da categoria em Relatórios.
- `feat/atalhos-nova-transacao`: widget e atalhos do ícone; já arquiva `backup-exportacao`.
**Depois:** implementar `transacoes-da-categoria`; ajustar as specs do PR #5 (tarefas 4.x do #7) e implementar `relatorio-por-conta` e `grupos-categorias-e-soma-no-valor`; arquivar `backup-exportacao` se a branch dos atalhos não for mesclada.
**Bloqueado:** nada.
**Não mexer:** sem migração destrutiva no Room; o usuário tem dados reais na release.
