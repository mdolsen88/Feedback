package com.example.mdol.feedback;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.mdol.mdollib.Action;
import com.example.mdol.mdollib.BluetoothScanner;
import com.example.mdol.mdollib.MoveSense.MovesenseSensor;
import com.example.mdol.mdollib.SensorActivator;
import com.example.mdol.mdollib.XML;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class BaseApplication extends Application {
    static String MY_PREFS_NAME = "FeedbackPrefs";
    static SharedPreferences sharedPreferences;
    public static SensorActivator[] sensorActivators = new SensorActivator[4];
    public static int currentProgram = 0;
    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);

        LoadConfig();
    }

    public static void SetDelay(long delay)
    {
        for(int i = 0; i < 4;i++)
            if(sensorActivators[i] != null)
                sensorActivators[i].mDelay = delay;
    }

    public void LoadConfig()
    {
        SharedPreferences sharedPreferences = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        for (int i = 0; i < 4;i++)
        {
            String str = sharedPreferences.getString("SensorActivator" + i,"");
            if(!str.equals(""))
                sensorActivators[i] = new SensorActivator(new XML(str));
        }
        currentProgram = sharedPreferences.getInt("currentProgram",0);
    }
}
