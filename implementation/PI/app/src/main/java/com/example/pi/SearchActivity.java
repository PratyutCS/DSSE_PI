package com.example.pi;

import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
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

    static {
        System.loadLibrary("pi");
    }

    private native String[] getSearchToken(String keyword);
    private native int[] performPostProcessing(int indexRange, String res1_0, String res1_1, String res2_0, String res2_1);

    private AutoCompleteTextView actvSpaceSelector;
    private android.widget.EditText etParam1, etParam2;
    private Button btnConnect;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final String PREFS_NAME = "pi_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        actvSpaceSelector = findViewById(R.id.actvSpaceSelector);
        etParam1 = findViewById(R.id.etParam1);
        etParam2 = findViewById(R.id.etParam2);
        btnConnect = findViewById(R.id.btnConnect);
        
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("PRIVATE SEARCH");
        
        View btnBack = findViewById(R.id.btnBack);
        btnBack.setVisibility(View.VISIBLE);
        btnBack.setOnClickListener(v -> finish());

        btnConnect.setOnClickListener(v -> performSearch());

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String token = prefs.getString("auth_token", null);

        if (token != null) {
            fetchSpaces(token);
        } else {
            finish();
        }
    }

    private void performSearch() {
        String dbName = actvSpaceSelector.getText().toString();
        String p1 = etParam1.getText().toString();
        String p2 = etParam2.getText().toString();

        if (dbName.isEmpty() || dbName.equals("Select Space")) {
            Toast.makeText(this, "Please select a space", Toast.LENGTH_SHORT).show();
            return;
        }
        if (p1.isEmpty() || p2.isEmpty()) {
            Toast.makeText(this, "Please enter both search parameters", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String token = prefs.getString("auth_token", null);
        String ip = prefs.getString("last_ip", "10.0.2.2");
        String baseUrl = "http://" + ip + ":3000/api/get-index_value";

        Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                // 1. Get tokens for param 1
                String[] tokens1 = getSearchToken(p1);
                String url1 = baseUrl + "?dbName=" + dbName + "&keyword_token=" + Uri.encode(tokens1[0]) + "&state_token=" + Uri.encode(tokens1[1]) + "&count=" + tokens1[2];
                String response1 = NetworkUtils.performGetRequest(url1, token);
                JSONArray res1 = new JSONObject(response1).getJSONArray("results");

                // 2. Get tokens for param 2
                String[] tokens2 = getSearchToken(p2);
                String url2 = baseUrl + "?dbName=" + dbName + "&keyword_token=" + Uri.encode(tokens2[0]) + "&state_token=" + Uri.encode(tokens2[1]) + "&count=" + tokens2[2];
                String response2 = NetworkUtils.performGetRequest(url2, token);
                JSONArray res2 = new JSONObject(response2).getJSONArray("results");

                // 3. Post Process
                // resX is a list of results. In the benchmark queen.cpp, search_result1[1] and [0] were used.
                // Assuming res1 has at least 2 elements [equal_id, boundary_val]
                String r10 = res1.length() > 0 ? res1.getString(0) : "-1";
                String r11 = res1.length() > 1 ? res1.getString(1) : "-1";
                String r20 = res2.length() > 0 ? res2.getString(0) : "-1";
                String r21 = res2.length() > 1 ? res2.getString(1) : "-1";

                int[] ids = performPostProcessing(100000, r10, r11, r20, r21);

                handler.post(() -> {
                    if (ids.length == 0) {
                        Toast.makeText(this, "No matching records found.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Found " + ids.length + " matching IDs!", Toast.LENGTH_LONG).show();
                        Log.i("PI_SEARCH", "Found IDs: " + java.util.Arrays.toString(ids));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> Toast.makeText(this, "Search failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
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
