# DetectorDeNotificacoes (Android Nativo)

Este app Android nativo escuta notificações bancárias usando NotificationListenerService e envia os dados para uma API HTTP (Square Cloud).

## Funcionalidades

- Solicita permissão de acesso a notificações
- Exibe status do serviço
- Permite configurar a URL da API e o telefone do usuário
- Envia notificações bancárias capturadas para a API
- Histórico de notificações bancárias (apenas bancos)
- Filtro de busca no histórico
- Exportação do histórico
- Teste de envio para API
- Logs de ações principais
- Atualização em tempo real do histórico
- Interface acessível e responsiva

## Estrutura do Projeto

- `app/src/main/java/com/notificationdetector/` — Código Java principal
- `app/src/main/res/layout/` — Layouts XML

## Como rodar

Abra o projeto no Android Studio e execute normalmente em um dispositivo Android.

## Git e Windows

Se aparecerem avisos sobre "LF will be replaced by CRLF", são apenas avisos de final de linha e não afetam o funcionamento do projeto.

## Licença

MIT
