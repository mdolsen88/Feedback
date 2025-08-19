package com.example.mdol.feedback;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.example.mdol.mdollib.IO.DataSaver;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class PaintView extends View {

    private class DrawEvent
    {
        long time = System.currentTimeMillis();
        float x,y;
        int mode;
        public DrawEvent(float x,float y,int mode)
        {
            this.x = x;
            this.y = y;
            this.mode = mode;
        }
    }
    ArrayList<DrawEvent> DrawEvents = new ArrayList<>();
    Paint paint;
    SoundPool SP;
    int FAIRY;
    boolean SizeSaved = false;
    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setFocusableInTouchMode(true);
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(35);
        paint.setStrokeCap(Paint.Cap.ROUND);

        SP = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        FAIRY = SP.load(getContext(),R.raw.fairy,1);

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
    }
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

    int timeHist = 5000;
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        float[] HSV = {0,1,1};
        for(int i = 1; i< DrawEvents.size();i++) {
            DrawEvent drawEvent = DrawEvents.get(i);
            DrawEvent drawEventPrev = DrawEvents.get(i-1);
            if(System.currentTimeMillis()-drawEventPrev.time>timeHist) {
                DrawEvents.remove(i - 1);
                i--;
            }
            else if(drawEventPrev.mode != MotionEvent.ACTION_UP) {
                HSV[0] = (drawEvent.time/10) % 360;
                paint.setColor(Color.HSVToColor(HSV));
                canvas.drawLine(drawEventPrev.x, drawEventPrev.y, drawEvent.x, drawEvent.y, paint);
            }
        }
    }

    long lastFairy = 0;
    public boolean onTouchEvent(MotionEvent event) {
        DataSaver.addPacket(new TouchEventPacket(event));
        float pointX = event.getX();
        float pointY = event.getY();
        int action = event.getAction();
        if(action<=2)
            DrawEvents.add(new DrawEvent(pointX, pointY, event.getAction()));
        if(action == MotionEvent.ACTION_MOVE && (System.currentTimeMillis()-lastFairy)>500) {
            SP.play(FAIRY, 1, 1, 0, 0, 1);
            lastFairy = System.currentTimeMillis();
        }
        return true;
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
}
