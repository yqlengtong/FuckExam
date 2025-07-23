package com.lengtong.fuckexam;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.content.Context;
import android.media.projection.MediaProjectionManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.SharedPreferences;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1;
    private static final int REQUEST_CODE_SYSTEM_ALERT_WINDOW = 2;
    private static final int REQUEST_CODE_MEDIA_PROJECTION = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        checkAndRequestPermissions();

        Spinner aiSpinner = findViewById(R.id.ai_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.ai_models, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        aiSpinner.setAdapter(adapter);

        EditText apiKeyInput = findViewById(R.id.api_key_input);

        SharedPreferences prefs = getSharedPreferences("AIConfig", MODE_PRIVATE);
        String savedModel = prefs.getString("selected_ai", null);
        if (savedModel != null) {
            aiSpinner.setSelection(adapter.getPosition(savedModel));
        }
        apiKeyInput.setText(prefs.getString("api_key", ""));

        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> {
            String selectedAI = aiSpinner.getSelectedItem().toString();
            String apiKey = apiKeyInput.getText().toString();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("selected_ai", selectedAI);
            editor.putString("api_key", apiKey);
            editor.apply();
            Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
        });

        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> {
            startScreenCapture();
        });
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
            } else {
                checkOverlayPermission();
            }
        } else {
            checkOverlayPermission();
        }
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_SYSTEM_ALERT_WINDOW);
            } else {
                // Both permissions are granted
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkOverlayPermission();
            } else {
                Toast.makeText(this, "Notification permission is required.", Toast.LENGTH_SHORT).show();
                // You might want to close the app or disable features
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SYSTEM_ALERT_WINDOW) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // Permission granted
                } else {
                    Toast.makeText(this, "Overlay permission is required.", Toast.LENGTH_SHORT).show();
                    // You might want to close the app or disable features
                }
            }
        } else if (requestCode == REQUEST_CODE_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                ScreenCaptureService.data = data;
                startService(new Intent(this, FloatingBallService.class));
                moveTaskToBack(true);
            }
        }
    }

    private void startScreenCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_MEDIA_PROJECTION);
    }
}