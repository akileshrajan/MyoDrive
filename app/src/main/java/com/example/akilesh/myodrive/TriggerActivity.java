package com.example.akilesh.myodrive;

/**
 * Created by akilesh on 3/28/2016.
 */


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;


public class TriggerActivity extends Activity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start the BackgroundService to receive and handle Myo events.
        startService(new Intent(this, BackgroundActivity.class));

        // Close this activity since BackgroundService will run in the background.
        finish();
    }
}
