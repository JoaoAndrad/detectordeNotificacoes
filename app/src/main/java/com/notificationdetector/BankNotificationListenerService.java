package com.notificationdetector;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

public class BankNotificationListenerService extends NotificationListenerService {
    // DEBUG: Ativar ou desativar duplicação de notificações para debug
    private static final boolean DEBUG_DUPLICATE_NOTIFICATION = true;

    @Override
    public void onCreate() {
        super.onCreate();
        // Iniciar serviço em foreground para robustez em background
        try {
            Intent serviceIntent = new Intent(this, ForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e("NotificationService", "Erro ao iniciar ForegroundService", e);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        // Ignorar notificações do WhatsApp para evitar duplicidade
        if ("com.whatsapp".equals(packageName)) {
            return;
        }
        String title = "";
        String text = "";
        if (sbn.getNotification().extras != null) {
            title = String.valueOf(sbn.getNotification().extras.getCharSequence("android.title"));
            text = String.valueOf(sbn.getNotification().extras.getCharSequence("android.text"));
        }
        // Ignorar notificações vazias
        if (title == null || text == null || title.equals("null") || text.equals("null")
                || (title.isEmpty() && text.isEmpty())) {
            return;
        }
        // Ignorar notificações geradas pelo próprio app
        Context appContext = getApplicationContext();
        if (appContext != null && packageName.equals(appContext.getPackageName())) {
            return;
        }

        boolean isC6Pix = false;
        boolean sent = false;
        // Nova lógica: só processa se for do pacote com.c6bank.app e tiver 'pix' no
        // título ou corpo
        if ("com.c6bank.app".equals(packageName)
                && (title.toLowerCase().contains("pix") || text.toLowerCase().contains("pix")
                        || title.toLowerCase().contains("compra no débito"))) {
            // Loga o conteúdo bruto recebido para debug
            Log.d("NotificationService",
                    "Notificação C6 recebida: package=" + packageName + ", title=" + title + ", text=" + text);
            // Tenta extrair tipo, valor, data e remetente/destinatário
            String tipo = null;
            Double valor = null;
            String data = null;
            String pessoa = null;
            String descricao = null;
            // Recebido Pix
            if (title.toLowerCase().contains("recebeu") && text.toLowerCase().contains("pix")) {
                tipo = "receita";
                // Regex robusto para valor Pix
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                        "R\\$[\\s\\u00A0]*([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2}|[0-9]+,[0-9]{2}|[0-9]{1,3}(?:\\.[0-9]{3})*|[0-9]+)")
                        .matcher(text);
                if (m.find()) {
                    String valorStr = m.group(1).replace(".", "").replace(",", ".");
                    try {
                        valor = Double.parseDouble(valorStr);
                    } catch (Exception e) {
                        valor = null;
                    }
                }
                m = java.util.regex.Pattern.compile("de ([^,]+), (CPF|CNPJ)").matcher(text);
                if (m.find())
                    pessoa = m.group(1).trim();
                m = java.util.regex.Pattern.compile("em (\\d{2}/\\d{2}/\\d{4})").matcher(text);
                if (m.find()) {
                    String[] partes = m.group(1).split("/");
                    data = partes[2] + "-" + partes[1] + "-" + partes[0];
                }
            }
            // Enviado Pix
            else if (title.toLowerCase().contains("enviou") && text.toLowerCase().contains("pix")) {
                tipo = "despesa";
                // Regex robusto para valor Pix
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                        "R\\$[\\s\\u00A0]*([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2}|[0-9]+,[0-9]{2}|[0-9]{1,3}(?:\\.[0-9]{3})*|[0-9]+)")
                        .matcher(text);
                if (m.find()) {
                    String valorStr = m.group(1).replace(".", "").replace(",", ".");
                    try {
                        valor = Double.parseDouble(valorStr);
                    } catch (Exception e) {
                        valor = null;
                    }
                }
                m = java.util.regex.Pattern.compile("para ([^,]+), (CPF|CNPJ)").matcher(text);
                if (m.find())
                    pessoa = m.group(1).trim();
                m = java.util.regex.Pattern.compile("em (\\d{2}/\\d{2}/\\d{4})").matcher(text);
                if (m.find()) {
                    String[] partes = m.group(1).split("/");
                    data = partes[2] + "-" + partes[1] + "-" + partes[0];
                }
            }
            // Compra no débito aprovada
            else if (title.toLowerCase().contains("compra no débito") && text.toLowerCase().contains("aprovada")) {
                tipo = "despesa";
                // Valor
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                        "R\\$[\\s\\u00A0]*([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2}|[0-9]+,[0-9]{2}|[0-9]{1,3}(?:\\.[0-9]{3})*|[0-9]+)")
                        .matcher(text);
                if (m.find()) {
                    String valorStr = m.group(1).replace(".", "").replace(",", ".");
                    try {
                        valor = Double.parseDouble(valorStr);
                    } catch (Exception e) {
                        valor = null;
                    }
                }
                // Data e hora
                m = java.util.regex.Pattern.compile("dia (\\d{2}/\\d{2}/\\d{4}) \\u00e0s (\\d{2}:\\d{2})")
                        .matcher(text);
                if (m.find()) {
                    String[] partes = m.group(1).split("/");
                    data = partes[2] + "-" + partes[1] + "-" + partes[0] + "T" + m.group(2);
                }
                // Estabelecimento
                m = java.util.regex.Pattern.compile("em ([^,]+), foi aprovada").matcher(text);
                if (m.find()) {
                    pessoa = m.group(1).trim(); // Aqui pessoa = estabelecimento
                }
                // Final do cartão
                m = java.util.regex.Pattern.compile("cartão final (\\d{4})").matcher(text);
                String finalCartao = null;
                if (m.find()) {
                    finalCartao = m.group(1);
                }
                // Descrição detalhada
                descricao = "Compra no débito aprovada em " + (pessoa != null ? pessoa : "?") +
                        (finalCartao != null ? (" (final " + finalCartao + ")") : "") +
                        (data != null ? (" em " + data.replace("T", " às ")) : "");
            }
            // Só envia se conseguiu extrair tudo
            if (tipo != null && valor != null && data != null && pessoa != null) {
                isC6Pix = true;
                if (descricao == null) {
                    sent = sendToApiC6Pix(tipo, valor, data, pessoa, title, text);
                } else {
                    sent = sendToApiC6Debito(tipo, valor, data, pessoa, descricao, title, text);
                }
            } else {
                Log.d("NotificationService", "C6 notificação ignorada: campos obrigatórios não extraídos. Tipo=" + tipo
                        + ", valor=" + valor + ", data=" + data + ", pessoa=" + pessoa);
            }
        }
        saveNotification(this, packageName, title, text, isC6Pix, sent);
        sendBroadcast(this, packageName, title, text, isC6Pix, sent);

        // DUPLICAR NOTIFICAÇÃO PARA DEBUG
        // (Removido conforme solicitado)
    }

    private boolean isBankNotification(String packageName) {
        return packageName.contains("bradesco") || packageName.contains("itau") ||
                packageName.contains("santander") || packageName.contains("caixa") ||
                packageName.contains("bb.com.br") || packageName.contains("nubank") ||
                packageName.contains("inter") || packageName.contains("original") ||
                packageName.contains("c6bank") || packageName.contains("picpay") ||
                packageName.contains("pagseguro") || packageName.contains("mercadopago");
    }

    private void saveNotification(Context context, String packageName, String title, String text, boolean isBank,
            boolean sent) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("notifs", MODE_PRIVATE);
            String old = prefs.getString("history", "[]");
            org.json.JSONArray arr = new org.json.JSONArray(old);
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("bank", packageName);
            obj.put("title", title);
            obj.put("message", text);
            obj.put("isBank", isBank);
            obj.put("sent", sent);
            obj.put("timestamp", System.currentTimeMillis());
            arr.put(obj);
            prefs.edit().putString("history", arr.toString()).apply();
            // Salvar em arquivo txt na pasta Download
            saveToTxtFile(packageName, title, text, isBank, sent);
        } catch (Exception e) {
            Log.e("NotificationService", "save error", e);
        }
    }

    private void saveToTxtFile(String packageName, String title, String text, boolean isBank, boolean sent) {
        try {
            // Salva sempre no diretório privado do app, compatível com Android 11+
            java.io.File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS);
            if (dir == null)
                dir = getExternalFilesDir(null);
            if (dir == null)
                return;
            java.io.File file = new java.io.File(dir, "notifications_log.txt");
            java.io.FileWriter fw = new java.io.FileWriter(file, true);
            fw.write("Notificação detectada:\n");
            fw.write("Hora: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())
                    + "\n");
            fw.write("App: " + packageName + (isBank ? " (Banco)" : "") + "\n");
            fw.write("Título: " + title + "\n");
            fw.write("Conteúdo: " + text + "\n");
            fw.write("Status envio: " + (sent ? "Enviado para API" : "Falha ao enviar") + "\n");
            fw.write("---\n");
            fw.close();
        } catch (Exception e) {
            Log.e("NotificationService", "file write error", e);
        }
    }

    private void sendBroadcast(Context context, String packageName, String title, String text, boolean isBank,
            boolean sent) {
        Intent intent = new Intent("com.notificationdetector.NEW_NOTIFICATION");
        intent.putExtra("bank", packageName);
        intent.putExtra("title", title);
        intent.putExtra("message", text);
        intent.putExtra("isBank", isBank);
        intent.putExtra("sent", sent);
        context.sendBroadcast(intent);
    }

    private boolean sendToApi(String packageName, String title, String text) {
        try {
            // Ignorar notificações vazias
            if (title == null || text == null) {
                Log.w("NotificationService", "Notificação ignorada: title ou text é null");
                return false;
            }
            String apiUrl = "https://assistentevirtual-financeiro-bot.squareweb.app/api/send-message";
            org.json.JSONObject data = new org.json.JSONObject();
            // C6 Bank Pix Recebido (Receita)
            if (packageName.contains("c6bank") && title.toLowerCase().contains("recebeu")
                    && text.toLowerCase().contains("pix recebido")) {
                data.put("bank", "C6 Bank");
                data.put("type", "receita");
                // Valor
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("R\\$ ([0-9.,]+)").matcher(text);
                if (m.find()) {
                    String valorStr = m.group(1).replace(".", "").replace(",", ".");
                    data.put("value", Double.parseDouble(valorStr));
                }
                // Remetente
                m = java.util.regex.Pattern.compile("de ([^,]+), (CPF|CNPJ)").matcher(text);
                if (m.find()) {
                    data.put("from", m.group(1).trim());
                }
                // Data
                m = java.util.regex.Pattern.compile("em (\\d{2}/\\d{2}/\\d{4})").matcher(text);
                if (m.find()) {
                    String[] partes = m.group(1).split("/");
                    data.put("date", partes[2] + "-" + partes[1] + "-" + partes[0]);
                }
                data.put("rawText", text);
            }
            // C6 Bank Pix Enviado (Despesa)
            else if (packageName.contains("c6bank") && title.toLowerCase().contains("enviou")
                    && text.toLowerCase().contains("pix enviado")) {
                data.put("bank", "C6 Bank");
                data.put("type", "despesa");
                // Valor
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("R\\$([0-9.,]+)").matcher(text);
                if (m.find()) {
                    String valorStr = m.group(1).replace(".", "").replace(",", ".");
                    data.put("value", Double.parseDouble(valorStr));
                }
                // Destinatário
                m = java.util.regex.Pattern.compile("para ([^,]+), (CPF|CNPJ)").matcher(text);
                if (m.find()) {
                    data.put("to", m.group(1).trim());
                }
                // Data
                m = java.util.regex.Pattern.compile("em (\\d{2}/\\d{2}/\\d{4})").matcher(text);
                if (m.find()) {
                    String[] partes = m.group(1).split("/");
                    data.put("date", partes[2] + "-" + partes[1] + "-" + partes[0]);
                }
                data.put("rawText", text);
            } else {
                // Caso não reconheça, envie apenas o texto bruto
                data.put("bank", packageName);
                data.put("type", "desconhecido");
                data.put("rawText", text);
            }

            org.json.JSONObject json = new org.json.JSONObject();
            json.put("phone", getUserPhone());
            json.put("type", "transaction");
            json.put("data", data);

            Log.d("NotificationService", "Enviando JSON para API: " + json.toString());

            java.net.URL url = new java.net.URL(apiUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            java.io.OutputStream os = conn.getOutputStream();
            os.write(json.toString().getBytes());
            os.flush();
            os.close();
            int responseCode = conn.getResponseCode();
            Log.d("NotificationService", "API response code: " + responseCode);
            // Ler resposta da API
            java.io.InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream()
                    : conn.getErrorStream();
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            String responseBody = s.hasNext() ? s.next() : "";
            Log.d("NotificationService", "API response body: " + responseBody);
            conn.disconnect();
            return responseCode >= 200 && responseCode < 300;
        } catch (Exception e) {
            Log.e("NotificationService", "API error", e);
            return false;
        }
    }

    // Novo método para envio de C6 Pix
    private boolean sendToApiC6Pix(String tipo, Double valor, String data, String pessoa, String title, String text) {
        try {
            final String apiUrl = "https://assistentevirtual-financeiro-bot.squareweb.app/api/send-message";
            final JSONObject dataObj = new JSONObject();
            dataObj.put("bank", "C6 Bank");
            dataObj.put("type", tipo); // "despesa" ou "receita"
            dataObj.put("amount", valor); // campo correto
            dataObj.put("date", data);
            if ("receita".equals(tipo)) {
                dataObj.put("from", pessoa);
                dataObj.put("description", "PIX recebido de " + pessoa);
            } else {
                dataObj.put("to", pessoa);
                dataObj.put("description", "PIX enviado para " + pessoa);
            }
            dataObj.put("rawText", text);
            // Tenta usar o id da notificação se disponível
            try {
                dataObj.put("id", String.valueOf(System.currentTimeMillis()));
            } catch (Exception e) {
            }
            final JSONObject json = new JSONObject();
            json.put("phone", getUserPhone());
            json.put("type", "transaction");
            json.put("data", dataObj);
            Log.d("NotificationService", "JSON FINAL PARA API (C6 Pix): " + json.toString());
            // Executa envio em thread separada
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(apiUrl);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    java.io.OutputStream os = conn.getOutputStream();
                    os.write(json.toString().getBytes());
                    os.flush();
                    os.close();
                    int responseCode = conn.getResponseCode();
                    Log.d("NotificationService", "API response code: " + responseCode);
                    java.io.InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream()
                            : conn.getErrorStream();
                    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                    String responseBody = s.hasNext() ? s.next() : "";
                    Log.d("NotificationService", "API response body: " + responseBody);
                    conn.disconnect();
                } catch (Exception e) {
                    Log.e("NotificationService", "API error (C6 Pix)", e);
                }
            }).start();
            return true; // Retorna true para indicar que iniciou o envio
        } catch (Exception e) {
            Log.e("NotificationService", "API error (C6 Pix)", e);
            return false;
        }
    }

    // Novo método para envio de compra no débito C6
    private boolean sendToApiC6Debito(String tipo, Double valor, String data, String estabelecimento, String descricao,
            String title, String text) {
        try {
            final String apiUrl = "https://assistentevirtual-financeiro-bot.squareweb.app/api/send-message";
            final JSONObject dataObj = new JSONObject();
            dataObj.put("bank", "C6 Bank");
            dataObj.put("type", tipo); // "despesa"
            dataObj.put("amount", valor);
            dataObj.put("date", data);
            dataObj.put("to", estabelecimento);
            dataObj.put("description", descricao);
            dataObj.put("rawText", text);
            try {
                dataObj.put("id", String.valueOf(System.currentTimeMillis()));
            } catch (Exception e) {
            }
            final JSONObject json = new JSONObject();
            json.put("phone", getUserPhone());
            json.put("type", "transaction");
            json.put("data", dataObj);
            Log.d("NotificationService", "JSON FINAL PARA API (C6 Débito): " + json.toString());
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(apiUrl);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    java.io.OutputStream os = conn.getOutputStream();
                    os.write(json.toString().getBytes());
                    os.flush();
                    os.close();
                    int responseCode = conn.getResponseCode();
                    Log.d("NotificationService", "API response code: " + responseCode);
                    java.io.InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream()
                            : conn.getErrorStream();
                    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                    String responseBody = s.hasNext() ? s.next() : "";
                    Log.d("NotificationService", "API response body: " + responseBody);
                    conn.disconnect();
                } catch (Exception e) {
                    Log.e("NotificationService", "API error (C6 Débito)", e);
                }
            }).start();
            return true;
        } catch (Exception e) {
            Log.e("NotificationService", "API error (C6 Débito)", e);
            return false;
        }
    }

    // Notificação local ao detectar notificação de banco
    private void showBankDetectedNotification(String title, String text) {
        String channelId = "bank_detected_channel";
        String channelName = "Banco Detectado";
        android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId, channelName, android.app.NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notificações de banco detectadas");
            nm.createNotificationChannel(channel);
        }
        String content = "Detectada notificação de banco (C6). Veja o log para detalhes do envio à API.";
        android.app.Notification notification = new android.app.Notification.Builder(this, channelId)
                .setContentTitle("Banco detectado: C6 Bank")
                .setContentText(content)
                .setStyle(new android.app.Notification.BigTextStyle()
                        .bigText(content + "\nTítulo: " + title + "\nConteúdo: " + text))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build();
        nm.notify((int) System.currentTimeMillis(), notification);
    }

    private String getUserPhone() {
        try {
            SharedPreferences prefs = getSharedPreferences("notifs", MODE_PRIVATE);
            return prefs.getString("userPhone", "558182132346");
        } catch (Exception e) {
            return "558182132346";
        }
    }
}
