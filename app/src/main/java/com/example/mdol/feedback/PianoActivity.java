package com.example.mdol.feedback;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import com.example.mdol.mdollib.Helper;
import com.example.mdol.mdollib.IO.DataSaver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class PianoActivity extends MasterActivity {

    @Override
    String FriendlyName() {
        return "PianoActivity";
    }

    public static Intent createIntent(Context context)
    {
        return new Intent(context, PianoActivity.class);
    }

    Button[] keys = new Button[8];
    int[] colorInactive;
    int[] colorActive;
    static SoundPool SP;
    static int[] Sounds = new int[8];
    @SuppressLint("ClickableViewAccessibility")
    @Override
    void oncreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_piano);

        keys[0] = findViewById(R.id.cmd0);
        keys[1] = findViewById(R.id.cmd1);
        keys[2] = findViewById(R.id.cmd2);
        keys[3] = findViewById(R.id.cmd3);
        keys[4] = findViewById(R.id.cmd4);
        keys[5] = findViewById(R.id.cmd5);
        keys[6] = findViewById(R.id.cmd6);
        keys[7] = findViewById(R.id.cmd7);

        SP = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        Sounds[0] = SP.load(this,R.raw.piano_0,1);
        Sounds[1] = SP.load(this,R.raw.piano_1,1);
        Sounds[2] = SP.load(this,R.raw.piano_2,1);
        Sounds[3] = SP.load(this,R.raw.piano_3,1);
        Sounds[4] = SP.load(this,R.raw.piano_4,1);
        Sounds[5] = SP.load(this,R.raw.piano_5,1);
        Sounds[6] = SP.load(this,R.raw.piano_6,1);
        Sounds[7] = SP.load(this,R.raw.piano_7,1);

        colorInactive = new int[keys.length];
        colorActive = new int[keys.length];
        //float[] H = {0f,30f,60f,110f,180f,230f,280f,320f};
        //float[] H = {0f,180f,40f,230f,60f,280f,110f,320f};
        float[] H = {0f,180f,120f,300f,60f,240f,0f,180f};
        float[] HSV = {0,1,1};
        for(int i = 0; i < keys.length;i++)
        {
            HSV[0] = H[i];
            HSV[2] = 1;
            colorActive[i] = Color.HSVToColor(HSV);
            HSV[2] = 0.9f;
            colorInactive[i] = Color.HSVToColor(HSV);

            keys[i].setBackgroundColor(colorInactive[i]);
            keys[i].setTag(i);
            keys[i].setOnTouchListener((v, event) -> {
                DataSaver.addPacket(new TouchEventPacket(event));
                int iThis = (int)v.getTag();
                Helper.MDOL_DEBUG("TOUCH:" + event.getAction());
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    keys[iThis].setBackgroundColor(colorActive[iThis]);
                    SP.play(Sounds[iThis],1,1,0,0,1);
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) {
                    keys[iThis].setBackgroundColor(colorInactive[iThis]);
                }
                return true;
            });
        }
    }
    class TouchEventPacket extends DataSaver.DataPacket
    {
        @Override
        protected String PacketStr() {
            return "TouchEvent";
        }
        public TouchEventPacket(MotionEvent event)
        {
            addData("Action",event.getAction());
            addData("X",event.getX());
            addData("Y",event.getY());
        }
    }

    @Override
    View getview()
    {
        return null;
    }

    @Override
    void feedback(ArrayList<Integer> active) {}

    @Override
    protected void destroy() {
    }
}