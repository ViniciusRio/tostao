@AGENTS.md

## Notas do Claude Code
- Revisão cruzada: use o agente `codex:codex-rescue` em modo somente leitura (os comandos `/codex:review` e `/codex:adversarial-review` só o usuário dispara). Registre os achados classificados como comentário no PR.
- Dispare a revisão do Codex sem esperar o pedido, na spec e no diff.
- Use as skills OpenSpec do projeto (`/openspec-propose`, `/openspec-apply-change`, `/openspec-archive-change`).
