package com.notificationdetector;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;

public class SettingsActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> createFileLauncher;
    private Uri fileUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button permissionButton = findViewById(R.id.permissionButton);
        Button saveButton = findViewById(R.id.saveButton);
        Button exportHistoryButton = findViewById(R.id.exportHistoryButton);
        Button requestStorageButton = findViewById(R.id.requestStorageButton);
        EditText apiUrlEdit = findViewById(R.id.apiUrlEdit);
        EditText phoneEdit = findViewById(R.id.phoneEdit);

        // Definir valores padrão se estiverem vazios
        if (apiUrlEdit.getText().toString().isEmpty()) {
            apiUrlEdit.setText("https://assistentevirtual-financeiro-bot.squareweb.app/api/send-message");
        }
        if (phoneEdit.getText().toString().isEmpty()) {
            phoneEdit.setText("558182132346");
        }

        permissionButton.setOnClickListener(v -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        });

        saveButton.setOnClickListener(v -> {
            String apiUrl = apiUrlEdit.getText().toString();
            String userPhone = phoneEdit.getText().toString();
            SharedPreferences.Editor editor = getSharedPreferences("notifs", MODE_PRIVATE).edit();
            editor.putString("apiUrl", apiUrl);
            editor.putString("userPhone", userPhone);
            editor.apply();
            Toast.makeText(this, "Configurações salvas!", Toast.LENGTH_SHORT).show();
        });

        exportHistoryButton.setOnClickListener(v -> {
            exportHistoryToTxt();
        });

        // SAF para Android 11+
        createFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        fileUri = result.getData().getData();
                        Toast.makeText(this, "Arquivo selecionado para salvar notificações!", Toast.LENGTH_LONG).show();
                        // Salvar o URI em SharedPreferences para uso pelo serviço
                        getSharedPreferences("notifs", MODE_PRIVATE).edit().putString("fileUri", fileUri.toString())
                                .apply();
                    }
                });

        requestStorageButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TITLE, "notifications_log.txt");
                    createFileLauncher.launch(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "Não foi possível abrir o seletor de arquivos.", Toast.LENGTH_LONG).show();
                }
            } else {
                requestAllPermissions();
            }
        });
    }

    private void exportHistoryToTxt() {
        try {
            SharedPreferences prefs = getSharedPreferences("notifs", MODE_PRIVATE);
            String old = prefs.getString("history", "[]");
            JSONArray arr = new JSONArray(old);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                sb.append("[" + obj.optString("bank") + "] ");
                sb.append(obj.optString("title") + ": ");
                sb.append(obj.optString("message") + "\n");
                sb.append("Status: " + (obj.optBoolean("sent") ? "Enviado" : "Falha") + "\n");
                sb.append("---\n");
            }
            String exportText = sb.toString();
            File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS);
            if (dir == null)
                dir = getExternalFilesDir(null);
            if (dir == null)
                throw new Exception("Diretório não encontrado");
            File file = new File(dir, "notifications_export.txt");
            FileWriter fw = new FileWriter(file, false);
            fw.write(exportText);
            fw.close();
            // Compartilhar arquivo
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_STREAM,
                    androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".provider", file));
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Compartilhar histórico"));
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao exportar histórico: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
}
