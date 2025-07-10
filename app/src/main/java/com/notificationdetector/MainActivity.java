package com.notificationdetector;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private TextView statusText;
    private EditText searchEdit;
    private RecyclerView historyRecyclerView;
    private HistoryAdapter historyAdapter;

    private BroadcastReceiver notificationReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        searchEdit = findViewById(R.id.searchEdit);
        historyRecyclerView = findViewById(R.id.historyRecyclerView);

        updateStatus();
        statusText.setText("Serviço sempre ativo (requer permissão de notificação)");

        // Atualização do histórico em tempo real
        notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null
                        && intent.getAction().equals("com.notificationdetector.NEW_NOTIFICATION")) {
                    loadHistory();
                }
            }
        };
        registerReceiver(notificationReceiver, new IntentFilter("com.notificationdetector.NEW_NOTIFICATION"),
                Context.RECEIVER_NOT_EXPORTED);

        checkNotificationPermission();

        Button testApiButton = findViewById(R.id.testApiButton);
        TextView testApiResult = findViewById(R.id.testApiResult);
        testApiResult.setText("");
        testApiButton.setOnClickListener(v -> {
            android.util.Log.d("MainActivity", "Botão Testar Envio para API pressionado");
            // Exemplo de payload de teste
            try {
                SharedPreferences prefs = getSharedPreferences("notifs", MODE_PRIVATE);
                String userPhone = prefs.getString("userPhone", "558182132346");
                org.json.JSONObject data = new org.json.JSONObject();
                data.put("bank", "Teste");
                data.put("type", "despesa");
                data.put("value", 0.05);
                data.put("from", "Usuário Teste");
                data.put("date", "2025-07-08");
                data.put("rawText", "Notificação de teste enviada pelo app");

                org.json.JSONObject json = new org.json.JSONObject();
                json.put("phone", userPhone); // Telefone salvo do usuário ou padrão
                json.put("type", "transaction");
                json.put("data", data);

                runOnUiThread(() -> testApiResult.setText("Enviando teste..."));
                new Thread(() -> {
                    try {
                        java.net.URL url = new java.net.URL(
                                "https://assistentevirtual-financeiro-bot.squareweb.app/api/send-message");
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);
                        java.io.OutputStream os = conn.getOutputStream();
                        os.write(json.toString().getBytes());
                        os.flush();
                        os.close();
                        int responseCode = conn.getResponseCode();
                        java.io.InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream()
                                : conn.getErrorStream();
                        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                        String responseBody = s.hasNext() ? s.next() : "";
                        runOnUiThread(() -> testApiResult.setText("Resposta: " + responseCode + "\n" + responseBody));
                        conn.disconnect();
                    } catch (Exception ex) {
                        runOnUiThread(() -> testApiResult.setText("Erro ao enviar teste: " + ex.getMessage()));
                    }
                }).start();
            } catch (Exception ex) {
                testApiResult.setText("Erro ao montar JSON de teste: " + ex.getMessage());
            }
        });

        searchEdit.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadHistory();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        historyRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        Button settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        Button clearHistoryButton = findViewById(R.id.clearHistoryButton);
        Button refreshHistoryButton = findViewById(R.id.refreshHistoryButton);

        clearHistoryButton.setOnClickListener(v -> {
            android.util.Log.d("MainActivity", "Botão Limpar Histórico pressionado");
            SharedPreferences prefs = getSharedPreferences("notifs", MODE_PRIVATE);
            prefs.edit().remove("history").apply();
            loadHistory();
            Toast.makeText(this, "Histórico limpo!", Toast.LENGTH_SHORT).show();
        });

        refreshHistoryButton.setOnClickListener(v -> {
            android.util.Log.d("MainActivity", "Botão Forçar Atualização pressionado");
            loadHistory();
            Toast.makeText(this, "Histórico atualizado!", Toast.LENGTH_SHORT).show();
        });

        loadHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        // Exibe orientação sobre bateria ao usuário
        showBatteryOptimizationWarning();
    }

    private void showBatteryOptimizationWarning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
            String packageName = getPackageName();
            if (pm != null && pm.isIgnoringBatteryOptimizations(packageName)) {
                // Já está fora das otimizações
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Atenção: Otimização de Bateria")
                    .setMessage(
                            "Para garantir que o app funcione sempre em segundo plano, remova o app das otimizações de bateria nas configurações do Android.\n\nVá em: Configurações > Bateria > Otimização de bateria > Apps não otimizados > selecione este app e marque como 'Não otimizar'.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void updateStatus() {
        String enabledListeners = android.provider.Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        String packageName = getPackageName();
        if (enabledListeners != null && enabledListeners.contains(packageName)) {
            statusText.setText("Serviço ativo");
        } else {
            statusText.setText("Permissão necessária: clique em 'Permissões'");
        }
    }

    private void loadHistory() {
        try {
            SharedPreferences prefs = getSharedPreferences("notifs", MODE_PRIVATE);
            String old = prefs.getString("history", "[]");
            JSONArray arr = new JSONArray(old);
            String search = searchEdit != null ? searchEdit.getText().toString().toLowerCase() : "";
            JSONArray filtered = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                boolean isBank = obj.optBoolean("isBank", false);
                if (!isBank)
                    continue; // Exibe apenas notificações de banco
                String bank = obj.optString("bank");
                String title = obj.optString("title");
                String message = obj.optString("message");
                boolean sent = obj.optBoolean("sent", false);
                String status = "";
                if (isBank && !sent) {
                    status = "Falha";
                } else if (isBank && sent) {
                    status = "Enviado";
                }
                String full = ("[" + bank + "] " + title + ": " + message
                        + (status.isEmpty() ? "" : ("\nStatus: " + status)) + "\n---\n").toLowerCase();
                if (search.isEmpty() || full.contains(search)) {
                    filtered.put(obj);
                }
            }
            if (historyAdapter == null) {
                historyAdapter = new HistoryAdapter(this, filtered);
                historyRecyclerView.setAdapter(historyAdapter);
            } else {
                historyAdapter.updateData(filtered);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao carregar histórico", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkNotificationPermission() {
        String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        String packageName = getPackageName();
        if (enabledListeners == null || !enabledListeners.contains(packageName)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permissão necessária")
                    .setMessage(
                            "O app precisa de acesso às notificações para funcionar. Clique em 'Permissões de Notificação' para ativar.")
                    .setPositiveButton("OK", (dialog, which) -> {
                    })
                    .show();
        }
    }

    private void requestAllPermissions() {
        String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        boolean needRequest = false;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }
        if (needRequest) {
            ActivityCompat.requestPermissions(this, permissions, 1001);
        }
    }

    // Solicita permissão para POST_NOTIFICATIONS no Android 13+
    private void requestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.POST_NOTIFICATIONS }, 2001);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            boolean allGranted = true;
            for (int res : grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED)
                    allGranted = false;
            }
            if (!allGranted) {
                Toast.makeText(this, "Permissões de armazenamento são necessárias para salvar notificações.",
                        Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 2001) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "Permissão para enviar notificações negada. O app pode não funcionar corretamente.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationReceiver != null) {
            unregisterReceiver(notificationReceiver);
        }
    }
}
