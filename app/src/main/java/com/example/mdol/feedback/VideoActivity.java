package com.example.mdol.feedback;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.VideoView;

import com.example.mdol.mdollib.Action;
import com.example.mdol.mdollib.Helper;
import com.example.mdol.mdollib.IO.DataSaver;
import com.example.mdol.mdollib.MoveSense.MovesenseSensor;
import com.example.mdol.mdollib.SensorActivator;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class VideoActivity extends MasterActivity {

    @Override
    String FriendlyName() {
        return "VideoActivity";
    }
    public static Intent createIntent(Context context,int[] use,int video)
    {
        Intent intent = new Intent(context, VideoActivity.class);
        intent.putExtra("DELAY",(long)-1000);
        video = com.example.mdol.mdollib.Helper.trueMod(video,8);
        video = context.getResources().getIdentifier("mp4_" + video, "raw", context.getPackageName());
        intent.putExtra("SRC",video);
        intent.putExtra("USE",use);
        return intent;
    }

    VideoView vid;
    @Override
    void oncreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_video);

        vid = findViewById(R.id.vid);

        int src = getIntent().getIntExtra("SRC",0);
        String uriPath = "android.resource://"+getPackageName() + "/" + src;
        vid.setVideoPath(uriPath);
        vid.start();
    }
    @Override
    View getview()
    {
        return vid;
    }

    boolean isPlaying = false;
    @Override
    void feedback(ArrayList<Integer> active) {
        boolean res = active.size()>0;
        if (res && !isPlaying) {
            isPlaying = true;
            vid.start();
        }
        else if (!res && isPlaying) {
            isPlaying = false;
            vid.pause();
        }
    }

    static class VideoPacket extends DataSaver.DataPacket
    {
        @Override
        protected String PacketStr() {
            return "VideoPacket";
        }
        public VideoPacket(int Duration,int Position)
        {
            addData("Duration",Duration);
            addData("Position",Position);
        }
    }

    @Override
    protected void destroy() {
        DataSaver.addPacket(new VideoPacket(vid.getDuration(),vid.getCurrentPosition()));
        vid.stopPlayback();
    }
}
