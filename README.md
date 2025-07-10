# NotificationDetector (Android Nativo)

Este app Android nativo escuta notificações bancárias usando NotificationListenerService e envia os dados para uma API HTTP (Square Cloud).

## Funcionalidades

- Solicita permissão de acesso a notificações
- Exibe status do serviço
- Permite configurar a URL da API
- Envia notificações bancárias capturadas para a API

## Estrutura do Projeto

- `app/src/main/java/com/notificationdetector/` — Código Java principal
- `app/src/main/res/layout/` — Layouts XML
- `.github/copilot-instructions.md` — Instruções para Copilot

## Como rodar

Abra o projeto no Android Studio e execute normalmente em um dispositivo Android.

## Observação

Este diretório substitui o projeto React Native para builds nativos. O diretório anterior foi mantido como backup.
