package com.example.mdol.feedback;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;

public class MusicActivity extends MasterActivity {

    @Override
    String FriendlyName() {
        return "MusicActivity";
    }
    public static Intent createIntent(Context context, int[] use, int music)
    {
        Intent intent = new Intent(context, MusicActivity.class);
        intent.putExtra("DELAY",(long)-1000);
        music = com.example.mdol.mdollib.Helper.trueMod(music,8);
        music = context.getResources().getIdentifier("mp3_" + music, "raw", context.getPackageName());
        intent.putExtra("SRC",music);
        intent.putExtra("USE",use);
        return intent;
    }

    MediaPlayer mediaPlayer;
    @Override
    void oncreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_music);
        int src = getIntent().getIntExtra("SRC",0);
        mediaPlayer = MediaPlayer.create(this, src);
    }

    @Override
    View getview()
    {
        return findViewById(R.layout.activity_music);
    }

    @Override
    void feedback(ArrayList<Integer> active) {
        boolean res = active.size()>0;
        if (res && !mediaPlayer.isPlaying())
            mediaPlayer.start();
        else if(!res && mediaPlayer.isPlaying())
            mediaPlayer.stop();
    }

    @Override
    protected void destroy() {
        mediaPlayer.stop();
        mediaPlayer.release();
    }
}
