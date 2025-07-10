package com.notificationdetector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            try {
                // Reinicia o serviço de notificação após o boot
                Intent serviceIntent = new Intent(context, BankNotificationListenerService.class);
                context.startService(serviceIntent);
                Log.d("BootReceiver", "BankNotificationListenerService reiniciado após boot.");
            } catch (Exception e) {
                Log.e("BootReceiver", "Erro ao reiniciar serviço após boot", e);
            }
        }
    }
}
