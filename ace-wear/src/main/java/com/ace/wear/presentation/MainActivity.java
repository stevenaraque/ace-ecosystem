package com.ace.wear.presentation;

import android.os.Bundle;
import androidx.activity.ComponentActivity;
import androidx.wear.activity.EmbeddedScrollingActivity;

public class MainActivity extends ComponentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: Set Wear Compose content
        setContentView(R.layout.activity_main);
    }
}
