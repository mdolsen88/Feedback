package com.example.mdol.feedback;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class SoundActivity extends MasterActivity {

    @Override
    String FriendlyName() {
        return "SoundActivity";
    }
    public static Intent createIntent(Context context, int[] use, int sound)
    {
        Intent intent = new Intent(context, SoundActivity.class);
        intent.putExtra("DELAY",(long)-200);
        sound = com.example.mdol.mdollib.Helper.trueMod(sound,4);
        sound = context.getResources().getIdentifier("wav_" + sound, "raw", context.getPackageName());
        intent.putExtra("SRC",sound);
        intent.putExtra("USE",use);
        return intent;
    }

    private SoundPool soundpool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
    int sound;
    @Override
    void oncreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_sound);
        int src = getIntent().getIntExtra("SRC",0);
        sound = soundpool.load(this, src, 1);
    }
    @Override
    View getview()
    {
        return findViewById(R.layout.activity_sound);
    }

    @Override
    void feedback(ArrayList<Integer> active) {
        boolean res = active.size()>0;
        if (res)
            soundpool.play(sound, 1, 1, 0, 0, 1);
    }

    @Override
    protected void destroy() {
        soundpool.release();
    }
}
