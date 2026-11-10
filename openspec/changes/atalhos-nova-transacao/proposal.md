# Proposal

## Why

Registrar uma despesa na hora em que ela acontece é o que mantém o Tostão em dia, mas hoje são quatro toques: abrir o app, esperar a Início, tocar em "+" e depois em "Despesa". Um atalho direto da tela inicial do celular reduz isso a um toque, com o formulário já aberto.

## What Changes

- **Widget "Nova despesa"** (1x1) para a tela inicial: um toque abre o formulário de despesa.
- **Atalhos do ícone do app** (pressionar e segurar): "Nova despesa", "Nova receita" e "Transferência".
- O formulário aberto por um atalho roda numa **tela própria e leve**. Ao salvar ou cancelar, ela fecha e o usuário volta para onde estava (tela inicial ou outro app), com uma confirmação rápida ("Despesa registrada"). Ela não mexe na pilha de telas do app principal.
- Sem nenhuma conta cadastrada, o atalho abre o app no cadastro de conta, como faz hoje o botão "+".
- O widget de saldo fica de fora deste change, por decisão do usuário. A regra para quando ele entrar, "segue o ocultar valores do app", fica registrada no design.

## Capabilities

### New Capabilities
- `quick-add`: entradas rápidas de fora do app (widget e atalhos do ícone) para registrar transações sem passar pela Início.

### Modified Capabilities

## Impact

- `AndroidManifest.xml`: nova `QuickAddActivity` (tarefa própria, fora dos recentes), o receiver do widget e os metadados de atalhos (`res/xml/shortcuts.xml`).
- Recursos novos: layout e info do widget, ícones vetoriais e strings.
- `ui/`: `MainViewModel` ganha um callback para quando o usuário volta da tela raiz, e uma espera pelas gravações pendentes antes de a tela fechar. O formulário de transação é reaproveitado sem mudar.
- Nenhuma dependência nova: o widget usa RemoteViews, não Glance.
