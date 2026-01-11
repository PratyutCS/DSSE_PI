package com.example.pi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
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

public class SearchActivity extends AppCompatActivity {

    private AutoCompleteTextView actvSpaceSelector;
    private Button btnConnect;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final String PREFS_NAME = "pi_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        actvSpaceSelector = findViewById(R.id.actvSpaceSelector);
        btnConnect = findViewById(R.id.btnConnect);
        
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("SELECT SPACE");
        
        View btnBack = findViewById(R.id.btnBack);
        btnBack.setVisibility(View.VISIBLE);
        btnBack.setOnClickListener(v -> finish());

        btnConnect.setOnClickListener(v -> {
            String selected = actvSpaceSelector.getText().toString();
            if (selected.isEmpty() || selected.equals("Select Space")) {
                Toast.makeText(this, "Please select a space", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Connecting to: " + selected, Toast.LENGTH_SHORT).show();
                // Future navigation logic
            }
        });

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String token = prefs.getString("auth_token", null);

        if (token != null) {
            fetchSpaces(token);
        } else {
            finish();
        }
    }

    private void fetchSpaces(String token) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String ip = prefs.getString("last_ip", "10.0.2.2");
        String url = "http://" + ip + ":3000/api/get-spaces";

        executor.execute(() -> {
            try {
                String response = NetworkUtils.performGetRequest(url, token);
                JSONObject jsonResponse = new JSONObject(response);
                JSONArray spaces = jsonResponse.optJSONArray("spaces");
                handler.post(() -> updateSpaceList(spaces));
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateSpaceList(JSONArray spaces) {
        if (spaces == null || spaces.length() == 0) {
            actvSpaceSelector.setText("No secure spaces found.", false);
            return;
        }

        List<String> spaceNames = new ArrayList<>();
        try {
            for (int i = 0; i < spaces.length(); i++) {
                spaceNames.add(spaces.getString(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.item_dropdown_luxury,
                spaceNames
        );
        actvSpaceSelector.setAdapter(adapter);
        
        if (!spaceNames.isEmpty()) {
            com.google.android.material.textfield.TextInputLayout til = findViewById(R.id.tilSpaceSelector);
            til.setHint("Select from " + spaceNames.size() + " spaces");
        }
    }
}
