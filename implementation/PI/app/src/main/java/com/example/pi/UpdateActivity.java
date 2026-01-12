package com.example.pi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pi.network.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateActivity extends AppCompatActivity {

    private Spinner spinnerSpaces;
    private Button btnPerformUpdate;
    private TextView tvEmptyMessage;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final String PREFS_NAME = "pi_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        spinnerSpaces = findViewById(R.id.spinnerSpaces);
        btnPerformUpdate = findViewById(R.id.btnPerformUpdate);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("UPDATE DASHBOARD");

        View btnBack = findViewById(R.id.btnBack);
        btnBack.setVisibility(View.VISIBLE);
        btnBack.setOnClickListener(v -> finish());

        btnPerformUpdate.setOnClickListener(v -> performUpdate());

        fetchEmptySpaces();
    }

    private void fetchEmptySpaces() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String token = prefs.getString("auth_token", null);
        String ip = prefs.getString("last_ip", "10.0.2.2");
        String url = "http://" + ip + ":3000/api/get-spaces?uninitialized=true";

        if (token == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        executor.execute(() -> {
            try {
                String response = NetworkUtils.performGetRequest(url, token);
                JSONObject jsonResponse = new JSONObject(response);
                JSONArray spacesJson = jsonResponse.optJSONArray("spaces");
                
                List<String> spaceList = new ArrayList<>();
                if (spacesJson != null) {
                    for (int i = 0; i < spacesJson.length(); i++) {
                        spaceList.add(spacesJson.getString(i));
                    }
                }

                handler.post(() -> updateSpinner(spaceList));
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> Toast.makeText(this, "Fetch failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void updateSpinner(List<String> spaceList) {
        if (spaceList.isEmpty()) {
            tvEmptyMessage.setVisibility(View.VISIBLE);
            btnPerformUpdate.setEnabled(false);
            btnPerformUpdate.setAlpha(0.5f);
            spinnerSpaces.setEnabled(false);
        } else {
            tvEmptyMessage.setVisibility(View.GONE);
            btnPerformUpdate.setEnabled(true);
            btnPerformUpdate.setAlpha(1.0f);
            spinnerSpaces.setEnabled(true);
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                    R.layout.item_dropdown_luxury, android.R.id.text1, spaceList);
            spinnerSpaces.setAdapter(adapter);
        }
    }

    private void performUpdate() {
        String selectedSpace = (String) spinnerSpaces.getSelectedItem();
        if (selectedSpace != null) {
            Toast.makeText(this, "Updating space: " + selectedSpace, Toast.LENGTH_SHORT).show();
            // Implement further update logic here as needed
        }
    }
}
