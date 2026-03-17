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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Environment;
import android.provider.Settings;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.os.Build;
import android.webkit.MimeTypeMap;
import androidx.core.content.FileProvider;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pi.network.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchActivity extends AppCompatActivity {

    static {
        System.loadLibrary("pi");
    }

    private native String[] getSearchToken(String storagePath, String keyword);
    private native int[] performPostProcessing(String storagePath, int indexRange, String res1_0, String res1_1, String res2_0, String res2_1);
    private native boolean decryptResultFile(String storagePath, String encryptedPath, String decryptedPath);

    private AutoCompleteTextView actvSpaceSelector;
    private android.widget.EditText etParam1, etParam2;
    private Button btnConnect;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    private TextView tvResultsTitle, tvResultsSummary, tvCounter;
    private View svResults;
    private LinearLayout llResults;
    private Button btnDownload;
    private CheckBox cbSelectAll;
    private int[] currentMatchedIds;
    private final List<Integer> selectedIds = new ArrayList<>();
    private final List<CheckBox> idCheckboxes = new ArrayList<>();
    private final Map<Integer, View> fileItemViews = new HashMap<>();

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

        tvResultsTitle = findViewById(R.id.tvResultsTitle);
        tvResultsSummary = findViewById(R.id.tvResultsSummary);
        svResults = findViewById(R.id.svResults);
        llResults = findViewById(R.id.llResults);
        btnDownload = findViewById(R.id.btnDownload);
        tvCounter = findViewById(R.id.tvCounter);
        cbSelectAll = findViewById(R.id.cbSelectAll);
        
        View btnBack = findViewById(R.id.btnBack);
        btnBack.setVisibility(View.VISIBLE);
        btnBack.setOnClickListener(v -> finish());

        btnConnect.setOnClickListener(v -> performSearch());
        btnDownload.setOnClickListener(v -> performDownloadSelected());

        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (CheckBox cb : idCheckboxes) {
                cb.setChecked(isChecked);
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
        String ip = prefs.getString("last_ip", BuildConfig.SERVER_IP);
        String baseUrl = "http://" + ip + ":3000/api/get-index_value";

        Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show();

        // 0. Check for MANAGE_EXTERNAL_STORAGE permission on Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                Toast.makeText(this, "Please grant 'All Files Access' to save results to SD card", Toast.LENGTH_LONG).show();
                return;
            }
        }

        executor.execute(() -> {
            try {
                String storagePath = getDbStoragePath(dbName);
                
                // 0. Cleanup previous searches
                File publicDecryptedDir = new File(Environment.getExternalStorageDirectory(), "PI_SearchResults");
                clearDirectories(new File(getFilesDir(), "downloads"), publicDecryptedDir);
                
                // 1. Get tokens for param 1
                Log.i("PI_SEARCH", "Generating token for P1: " + p1 + " using DB storage: " + storagePath);
                String[] tokens1 = getSearchToken(storagePath, p1);
                Log.i("PI_SEARCH", "P1 Tokens: u=" + tokens1[0] + ", count=" + tokens1[2]);
                
                String url1 = baseUrl + "?dbName=" + dbName + "&keyword_token=" + Uri.encode(tokens1[0]) + "&state_token=" + Uri.encode(tokens1[1]) + "&count=" + tokens1[2];
                Log.i("PI_SEARCH", "Requesting P1 from server: " + url1);
                String response1 = NetworkUtils.performGetRequest(url1, token);
                JSONArray res1 = new JSONObject(response1).getJSONArray("results");
                Log.i("PI_SEARCH", "P1 Results received: " + res1.length());

                // 2. Get tokens for param 2
                Log.i("PI_SEARCH", "Generating token for P2: " + p2);
                String[] tokens2 = getSearchToken(storagePath, p2);
                Log.i("PI_SEARCH", "P2 Tokens: u=" + tokens2[0] + ", count=" + tokens2[2]);
                
                String url2 = baseUrl + "?dbName=" + dbName + "&keyword_token=" + Uri.encode(tokens2[0]) + "&state_token=" + Uri.encode(tokens2[1]) + "&count=" + tokens2[2];
                Log.i("PI_SEARCH", "Requesting P2 from server: " + url2);
                String response2 = NetworkUtils.performGetRequest(url2, token);
                JSONArray res2 = new JSONObject(response2).getJSONArray("results");
                Log.i("PI_SEARCH", "P2 Results received: " + res2.length());

                // 3. Post Process
                // resX is a list of results. In the benchmark queen.cpp, search_result1[1] and [0] were used.
                // Assuming res1 has at least 2 elements [equal_id, boundary_val]
                String r10 = res1.length() > 0 ? res1.getString(0) : "-1";
                String r11 = res1.length() > 1 ? res1.getString(1) : "-1";
                String r20 = res2.length() > 0 ? res2.getString(0) : "-1";
                String r21 = res2.length() > 1 ? res2.getString(1) : "-1";

                currentMatchedIds = performPostProcessing(storagePath, 100000, r10, r11, r20, r21);

                handler.post(() -> {
                    llResults.removeAllViews();
                    selectedIds.clear();
                    idCheckboxes.clear();
                    fileItemViews.clear();
                    cbSelectAll.setChecked(false);
                    updateSelectionCounter();

                    if (currentMatchedIds.length == 0) {
                        Toast.makeText(this, "No matching records found.", Toast.LENGTH_LONG).show();
                        tvResultsTitle.setVisibility(View.GONE);
                        svResults.setVisibility(View.GONE);
                        btnDownload.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(this, "Found " + currentMatchedIds.length + " matching IDs!", Toast.LENGTH_SHORT).show();
                        
                        LayoutInflater inflater = LayoutInflater.from(this);
                        for (int id : currentMatchedIds) {
                            View itemView = inflater.inflate(R.layout.item_file_download, llResults, false);
                            
                            CheckBox cb = itemView.findViewById(R.id.cbFile);
                            TextView tvName = itemView.findViewById(R.id.tvFileName);
                            TextView tvStatus = itemView.findViewById(R.id.tvStatus);
                            
                            tvName.setText("File ID: " + id);
                            tvStatus.setText("Ready to download");
                            
                            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                if (isChecked) {
                                    if (!selectedIds.contains(id)) selectedIds.add(id);
                                } else {
                                    selectedIds.remove(Integer.valueOf(id));
                                }
                                updateSelectionCounter();
                            });
                            
                            llResults.addView(itemView);
                            idCheckboxes.add(cb);
                            fileItemViews.put(id, itemView);
                        }
                        
                        tvResultsTitle.setVisibility(View.VISIBLE);
                        svResults.setVisibility(View.VISIBLE);
                        btnDownload.setVisibility(View.VISIBLE);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> Toast.makeText(this, "Search failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void performDownloadSelected() {
        if (selectedIds.isEmpty()) {
            Toast.makeText(this, "Please select at least one file to download", Toast.LENGTH_SHORT).show();
            return;
        }

        String dbName = actvSpaceSelector.getText().toString();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String token = prefs.getString("auth_token", null);
        String ip = prefs.getString("last_ip", BuildConfig.SERVER_IP);

        Toast.makeText(this, "Downloading " + selectedIds.size() + " files...", Toast.LENGTH_SHORT).show();
        btnDownload.setEnabled(false);
        btnDownload.setText("DOWNLOADING...");

        executor.execute(() -> {
            try {
                String storagePath = getDbStoragePath(dbName);
                File internalBase = getFilesDir();
                File downloadDir = new File(internalBase, "downloads");
                File decryptedDir = new File(Environment.getExternalStorageDirectory(), "PI_SearchResults");
                
                if (!downloadDir.exists()) downloadDir.mkdirs();
                if (!decryptedDir.exists()) decryptedDir.mkdirs();

                int successCount = 0;
                List<Integer> idsToDownload = new ArrayList<>(selectedIds);

                for (int id : idsToDownload) {
                    View itemView = fileItemViews.get(id);
                    if (itemView == null) continue;

                    ProgressBar pb = itemView.findViewById(R.id.pbDownload);
                    TextView tvStatus = itemView.findViewById(R.id.tvStatus);
                    ImageView ivIcon = itemView.findViewById(R.id.ivStatusIcon);

                    String fileId = "ID" + id;
                    String downloadUrl = "http://" + ip + ":3000/api/download-file?dbName=" + dbName + "&fileId=" + fileId;
                    
                    try {
                        handler.post(() -> tvStatus.setText("Phase 1: Downloading..."));
                        
                        String downloadedFileName = NetworkUtils.downloadFile(downloadUrl, downloadDir.getAbsolutePath(), token, progress -> {
                            // Phase 1: 0-80%
                            int scaledProgress = progress * 80 / 100;
                            handler.post(() -> pb.setProgress(scaledProgress));
                        });
                        
                        handler.post(() -> {
                            pb.setProgress(85);
                            tvStatus.setText("Phase 2: Decrypting & Saving...");
                        });

                        File encFile = new File(downloadDir, downloadedFileName);
                        File decFile = new File(decryptedDir, downloadedFileName);

                        boolean success = decryptResultFile(storagePath, encFile.getAbsolutePath(), decFile.getAbsolutePath());
                        if (success) {
                            successCount++;
                            encFile.delete(); // Delete encrypted file as requested
                            handler.post(() -> {
                                pb.setProgress(100);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    pb.setProgressTintList(ColorStateList.valueOf(0xFF4CAF50)); // Green
                                }
                                tvStatus.setText("Completed");
                                ivIcon.setVisibility(View.VISIBLE);
                                ivIcon.setOnClickListener(v -> openFile(decFile));
                            });
                        } else {
                            handler.post(() -> tvStatus.setText("Failed: Decryption error"));
                        }
                    } catch (Exception e) {
                        Log.e("PI_SEARCH", "Failed ID" + id + ": " + e.getMessage());
                        handler.post(() -> tvStatus.setText("Error: " + e.getMessage()));
                    }
                }

                int finalSuccessCount = successCount;
                handler.post(() -> {
                    btnDownload.setEnabled(true);
                    btnDownload.setText("DOWNLOAD SELECTED");
                    Toast.makeText(this, "Successfully downloaded " + finalSuccessCount + " files to PI_SearchResults", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    btnDownload.setEnabled(true);
                    btnDownload.setText("DOWNLOAD SELECTED");
                    Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updateSelectionCounter() {
        int total = currentMatchedIds != null ? currentMatchedIds.length : 0;
        tvCounter.setText("Selected: " + selectedIds.size() + " / " + total);
    }

    private String getDbStoragePath(String dbName) {
        File dbDir = new File(getFilesDir(), dbName);
        if (!dbDir.exists()) dbDir.mkdirs();
        return dbDir.getAbsolutePath();
    }

    private void clearDirectories(File... dirs) {
        for (File dir : dirs) {
            if (dir != null && dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) f.delete();
                }
            }
        }
    }

    private void fetchSpaces(String token) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String ip = prefs.getString("last_ip", BuildConfig.SERVER_IP);
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
    private void openFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType != null ? mimeType : "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open file with..."));
        } catch (Exception e) {
            Toast.makeText(this, "Could not open file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
