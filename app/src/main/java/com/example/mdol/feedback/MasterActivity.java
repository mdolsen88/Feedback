package com.example.mdol.feedback;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.mdol.feedback.MainActivity.SAVE;
import static com.example.mdol.feedback.MainActivity.friendlyNames;
import static com.example.mdol.feedback.MainActivity.friendlyNamesEng;
import static com.example.mdol.feedback.MainActivity.useDK;

import com.example.mdol.mdollib.Helper;
import com.example.mdol.mdollib.IO.DataSaver;
import com.example.mdol.mdollib.Sensor;

public abstract class MasterActivity extends AppCompatActivity{

    int stopCounter = 0;
    long stopPrev = 0;
    Context context;
    static byte WRITETYPE_SUCCESS = 2;
    static byte WRITETYPE_STOP = 3;

    abstract String FriendlyName();

    int[] USE;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getSupportActionBar().hide();
        oncreate(savedInstanceState);
        View tapView = getview();
        if(tapView != null)
            tapView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(System.currentTimeMillis()-stopPrev<500) {
                    stopCounter++;
                    if(stopCounter == 2){
                        stopCounter = 0;
                        AskForStop();
                    }
                }
                else
                    stopCounter = 0;
                stopPrev = System.currentTimeMillis();
            }
        });

        Start = System.currentTimeMillis();
        MainActivity.SetDelay(getIntent().getLongExtra("DELAY",1000));
        USE = getIntent().getIntArrayExtra("USE");
        if(USE == null)
            USE = new int[4];

        String[] strings = useDK ? friendlyNames : friendlyNamesEng;
        String str = "";
        for (int i = 0; i < 4; i++)
            if (USE[i] == 1)
                if (str.equals(""))
                    str = strings[i];
                else
                    str += ";" + strings[i];
        if(!str.equals(""))
            Toast.makeText(context, str, Toast.LENGTH_LONG).show();
        Helper.MDOL_DEBUG(this.FriendlyName());
        startRecording(this.FriendlyName(),new byte[]{(byte)(USE[0]+USE[1]*2+USE[2]*4+USE[3]*8)});

        IgnoreSensors = true;
        for (int use:USE)
            if(use!=0) {
                IgnoreSensors = false;
                break;
            }

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (!Closed) {
                    if(System.currentTimeMillis()-Start>60000*3) {
                        if(!SuccessSent) {
                            SuccessSent = true;
                            stopRecording(WRITETYPE_SUCCESS);
                            MainActivity.SUCCESS();
                            setResult(RESULT_OK);
                            finish();
                        }
                    }
                    feedback(Validate(USE));
                    for (int i = 0; i < MainActivity.sensorActivators.length; i++)
                        if(MainActivity.sensorActivators[i]!=null)
                            if(SAVE)
                                DataSaver.addPacket(new SensorPacket(i, MainActivity.sensorActivators[i].mSensor));
                }
            }
        },0,50);
    }

    static class SensorPacket extends DataSaver.DataPacket
    {
        public SensorPacket(int sensorID, Sensor sensor)
        {
            addData("sensorID",sensorID);
            int nSignals = sensor.getSignals().length;
            addData("nSignals", nSignals);
            double[] Signals = new double[nSignals];
            for (int j = 0; j < nSignals; j++)
                Signals[j] = sensor.getValue(j);
            addData("Signals", Signals);
        }
        @Override
        protected String PacketStr() {
            return "SensorPacket";
        }
    }
    static class StartPacket extends DataSaver.DataPacket
    {
        public StartPacket(String classStr,byte[] Use)
        {
            addData("classStr", classStr);
            addData("Use", Use);
        }
        @Override
        protected String PacketStr() {
            return "StartPacket";
        }
    }
    static class StopPacket extends DataSaver.DataPacket
    {
        public StopPacket(int Reason)
        {
            addData("Reason", Reason);
        }
        @Override
        protected String PacketStr() {
            return "StopPacket";
        }
    }

    private void startRecording(String classString, byte[] use)
    {
        if(SAVE) {
            Helper.MDOL_DEBUG("Creating datasaver - " + classString);
            DataSaver.NewFile(new File(context.getFilesDir(), "FeedbackLog"), MainActivity.ID, "");
            DataSaver.addPacket(new StartPacket(classString, use));
        }
    }

    void stopRecording(int reason)
    {
        if(SAVE) {
            DataSaver.addPacket(new StopPacket(reason));
        }
    }

    void AskForStop()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle((useDK ? "Er du sikker på du vil afbryde?" : "Do you want to stop?"));
        builder.setPositiveButton(useDK ? "Ja" : "Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                destroy();
                finish();
            }
        });
        builder.setNegativeButton(useDK ? "Nej" : "No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    boolean SuccessSent = false;
    boolean IgnoreSensors = false;
    long Start;

    private ArrayList<Integer> Validate(int[] USE)
    {
        ArrayList<Integer> active = new ArrayList<>();
        for (int i = 0; i < MainActivity.sensorActivators.length; i++) {
            if(IgnoreSensors) {
                active.add(i);
            }
            else if (USE[i]!=0) {
                if(MainActivity.sensorActivators[i] != null && MainActivity.sensorActivators[i].mSensor.getStatus() == 1 && MainActivity.sensorActivators[i].getLastValid()<100)
                    active.add(i);
                else if(USE[i] == 2)
                    return new ArrayList<>();
            }
        }
        return active;
    }
    boolean Closed = false;
    @Override
    public void onBackPressed() {
        if(System.currentTimeMillis()-stopPrev<500) {
            stopCounter++;
            if(stopCounter == 2){
                stopCounter = 0;
                AskForStop();
            }
        }
        else
            stopCounter = 0;
        stopPrev = System.currentTimeMillis();
    }

    @Override
    protected void onDestroy() {
        stopRecording(WRITETYPE_STOP);
        super.onDestroy();
        destroy();

        Closed = true;
        if(SAVE) {
            DataSaver.Close();
        }
    }

    abstract void destroy();
    abstract void feedback(ArrayList<Integer> active);
    abstract void oncreate(Bundle savedInstanceState);
    abstract View getview();
}
