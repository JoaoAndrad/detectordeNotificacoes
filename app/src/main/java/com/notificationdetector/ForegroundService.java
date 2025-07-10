package com.notificationdetector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class ForegroundService extends Service {
    public static final String CHANNEL_ID = "bank_notification_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        android.util.Log.d("ForegroundService", "onCreate chamado");
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        android.util.Log.d("ForegroundService", "onStartCommand chamado");
        // Cria a notificação e chama startForeground() aqui para máxima compatibilidade
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("App ativo")
                .setContentText("") // Sem texto para máxima discrição
                .setSmallIcon(R.drawable.ic_transparent) // Ícone transparente
                .setPriority(NotificationCompat.PRIORITY_MIN) // Prioridade mínima
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET) // Não mostra conteúdo na tela de bloqueio
                .setShowWhen(false) // Não mostra horário
                .setOnlyAlertOnce(true) // Nunca alerta novamente
                .setAutoCancel(false) // Garante que não pode ser removida
                .setDefaults(0); // Sem som, vibração, luz
        Notification notification = builder.build();
        startForeground(1, notification);
        // Serviço em foreground, não faz nada além de manter o app ativo
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        android.util.Log.d("ForegroundService", "onDestroy chamado");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Notificações Bancárias",
                    NotificationManager.IMPORTANCE_MIN // IMPORTANCE_MIN para máxima discrição
            );
            channel.setDescription("Canal para serviço em primeiro plano de monitoramento de notificações bancárias");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
