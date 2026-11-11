# Proposal

## Why

Os dados do Tostão existem só no aparelho (`allowBackup=false`): desinstalar o app, trocar de celular ou perder o aparelho apaga tudo, sem volta. Também não há como levar as transações para uma planilha. Este change dá ao usuário uma cópia completa dos dados, que ele guarda onde quiser, e uma exportação legível, sem abrir mão do "somente local" (nada de nuvem nem conta).

## What Changes

- Nova tela **Configurações**, aberta por um ícone de engrenagem no topo da Início. A engrenagem ocupa o lugar do atalho de Categorias, que passa a ficar dentro de Configurações.
- **Fazer backup**: gera um arquivo JSON com todos os dados (contas, categorias, transações, planejados, orçamentos) e o salva onde o usuário escolher, pelo seletor de arquivos do sistema. Não pede permissão de armazenamento. O arquivo tem marcador do app e versão de formato.
- **Restaurar backup**: lê um arquivo escolhido pelo usuário e valida o conteúdo inteiro antes de mexer em qualquer dado. Depois mostra o que existe no arquivo e pede uma confirmação explícita de que os dados atuais serão substituídos. A troca é atômica: se falhar, nada muda.
- **Lembrete discreto**: Configurações mostra a data do último backup. A Início mostra um cartão dispensável quando já passou tempo demais sem backup e os dados mudaram. Não usa notificação.
- **Exportar transações (CSV)**: no padrão brasileiro (`;` como separador e vírgula decimal), com período escolhido (tudo, este ano, este mês ou um intervalo). Abre no Excel e no Google Planilhas.
- As categorias padrão passam a ser criadas só na primeira instalação, para que um backup restaurado sem categorias não as receba de novo.

## Capabilities

### New Capabilities
- `data-backup`: backup completo em arquivo, restauração com validação e substituição atômica, e lembrete de backup.
- `transaction-export`: exportação de transações em CSV no padrão brasileiro, com escolha de período.
- `settings`: tela de Configurações, que reúne atalhos de organização (Categorias), as ações de dados e as informações do app.

### Modified Capabilities
- `home-overview`: o painel inicial ganha o cartão de lembrete de backup, e o topo da Início troca o atalho de Categorias por Configurações.
- `categories`: as categorias padrão são criadas só na primeira instalação, e não sempre que a tabela estiver vazia.

## Impact

- Nova dependência: `kotlinx-serialization-json` e o plugin de serialização do Kotlin.
- `domain/`: codificação e validação do backup e geração do CSV, em Kotlin puro e com testes unitários.
- `data/`: `FinanceRepository` ganha a leitura completa e a substituição atômica de todos os dados (`replaceAll`), com teste instrumentado. O esquema do Room não muda.
- `ui/`: nova rota e tela de Configurações, diálogos de restauração e de exportação, cartão na Início e integração com o Storage Access Framework (`CreateDocument`/`OpenDocument`).
- Privacidade: o arquivo de backup não é criptografado. A interface avisa disso. Criptografia fica para um change futuro (formato 2).
