package com.example.mdol.feedback;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SizeF;
import android.view.MotionEvent;
import android.view.View;

import com.example.mdol.mdollib.IO.DataSaver;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class BalloonView extends View {

    float w = 0.2f;
    float speed = 0.2f;
    float BallonsPerSecond = 2;
    static int[] raw = {
            R.drawable.balloon00,R.drawable.balloon01,R.drawable.balloon02,R.drawable.balloon03,
            R.drawable.balloon10,R.drawable.balloon11,R.drawable.balloon12,R.drawable.balloon13,
            R.drawable.balloon20,R.drawable.balloon21,R.drawable.balloon22,R.drawable.balloon23,
            R.drawable.balloon30,R.drawable.balloon31,R.drawable.balloon32,R.drawable.balloon33,
            R.drawable.balloon40,R.drawable.balloon41,R.drawable.balloon42,R.drawable.balloon43,
            R.drawable.balloon50,R.drawable.balloon51,R.drawable.balloon52,R.drawable.balloon53
    };
    static Bitmap[] bmp;
    static Random rnd = new Random();
    class Balloon
    {
        PointF start;
        int Width,Height;
        float speed;
        int mode;
        long time = System.currentTimeMillis();

        int bmpI;
        public Balloon(float x,float y,float speed,float width)
        {
            start = new PointF(x,y);
            this.speed = speed;
            mode = 0;
            bmpI = rnd.nextInt(bmp.length/4);
            Width = (int)width;
            Height = (int)(width/bmp[bmpI*4].getWidth()*bmp[bmpI*4].getHeight());
        }

        PointF getXY()
        {
            return new PointF(start.x,start.y-speed*(System.currentTimeMillis()-time)/1000f);
        }
        public boolean Draw(Canvas canvas)
        {
            PointF XY = getXY();
            if(XY.y<-Height) {
                return false;
            }
            canvas.drawBitmap(bmp[bmpI*4+mode],
                    null,
                    new Rect((int)(XY.x-Width/2),(int)(XY.y-Height/2),(int)(XY.x+Width/2),(int)(XY.y+Height/2)),
                    null);
            if(mode != 0) {
                mode++;
                if(mode == 4) {
                    return false;
                }
            }

            return true;
        }
        public void Pop(int X,int Y)
        {
            if(mode != 0)
                return;
            PointF XY = getXY();
            if(Math.abs(XY.x-X)<=Width*0.5 && Math.abs(XY.y-Y)<=Height*0.5)
            {
                SP.play(POP,1,1,0,0,1);
                mode = 1;
                DataSaver.addPacket(new PopPacket());
            }
        }
        class PopPacket extends DataSaver.DataPacket
        {
            @Override
            protected String PacketStr() {
                return "BalloonPop";
            }
        }
    }

    ArrayList<Balloon> balloons = new ArrayList<>();
    static SoundPool SP;
    static int POP;
    public BalloonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setFocusableInTouchMode(true);

        SP = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        POP = SP.load(getContext(),R.raw.pop,1);
        bmp = new Bitmap[raw.length];
        for(int i = 0; i < raw.length;i++)
            bmp[i] = BitmapFactory.decodeResource(getResources(), raw[i]);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(!SizeSaved)
                {
                    DataSaver.addPacket(new DrawStartedPacket(getWidth(),getHeight()));
                    SizeSaved = true;
                }
                postInvalidate();
            }
        },0,50);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                balloons.add(new Balloon((rnd.nextFloat()*(1-w)+w/2)*getWidth(),1.5f*getHeight(),speed*getHeight(),getWidth()*w));
            }
        },2000,(int)(1000/BallonsPerSecond));
    }
    boolean SizeSaved = false;
    class DrawStartedPacket extends DataSaver.DataPacket
    {
        @Override
        protected String PacketStr() {
            return "DrawStarted";
        }
        public DrawStartedPacket(int Width, int Height)
        {
            addData("Width",Width);
            addData("Height",Height);
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
    public boolean onTouchEvent(MotionEvent event) {
        DataSaver.addPacket(new TouchEventPacket(event));
        if(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE)
        {
            int X = (int)(event.getX());
            int Y = (int)(event.getY());
            for(int i = 0; i < balloons.size();i++){
                balloons.get(i).Pop(X,Y);
            }
        }
        return true;
    }

    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        for(int i = 0; i < balloons.size();i++){
            if(!balloons.get(i).Draw(canvas)) {
                balloons.remove(i--);
            }
        }
    }
}
