package com.example.pi;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class UpdateActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_generic); 

        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("UPDATE DASHBOARD");
        
        View btnBack = findViewById(R.id.btnBack);
        btnBack.setVisibility(View.VISIBLE);
        btnBack.setOnClickListener(v -> finish());

        TextView info = new TextView(this);
        info.setText("Update feature coming soon.\nThis will allow renaming spaces or updating security parameters.");
        info.setTextColor(0xFFFFFFFF);
        info.setGravity(android.view.Gravity.CENTER);
        info.setPadding(32, 100, 32, 32);
        ((android.widget.LinearLayout)findViewById(R.id.llSpaceList)).addView(info);
    }
}
