package com.example.mdol.feedback;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.example.mdol.mdollib.Action;
import com.example.mdol.mdollib.BluetoothScanner;
import com.example.mdol.mdollib.Dialog;
import com.example.mdol.mdollib.Func1;
import com.example.mdol.mdollib.Func2;
import com.example.mdol.mdollib.Helper;
import com.example.mdol.mdollib.MACDevice;
import com.example.mdol.mdollib.Func;
import com.example.mdol.mdollib.MoveSense.MovesenseSensor;
import com.example.mdol.mdollib.Sensor;
import com.example.mdol.mdollib.SensorActivator;
import com.example.mdol.mdollib.XML;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public static SensorActivator[] sensorActivators = new SensorActivator[4];
    public static String[] friendlyNames = {"Højre Hånd","Venstre Hånd","Højre Fod","Venstre Fod"};
    public static String[] friendlyNamesEng = {"Right Hand","Left Hand","Right Foot","Left Foot"};
    public static ArrayList<Video> videos = new ArrayList<>();
    public static ArrayList<Video> userVideos = new ArrayList<>();

    public static String ID = "";
    public static boolean SAVE = true;
    public static boolean AUTOPLAY = false;
    public static boolean useDK = true;
    public static void SetDelay(long delay)
    {
        for(int i = 0; i < sensorActivators.length;i++)
            if(MainActivity.sensorActivators[i] != null)
                MainActivity.sensorActivators[i].mDelay = delay;
    }
    ImageButton[] imgS = new ImageButton[4];
    BluetoothScanner mBluetoothScanner;
    boolean isAdmin = false;
    int[] adminSequence = new int[]{0,1,3,2,3,1,0};
    int adminPosition = 0;

    class Site
    {
        int[] SensorOrTouch;
        String DK;
        String ENG;
        Func1<Object,Intent> Create = new Func1<Object, Intent>() {
            @Override
            public Intent Run(Object video) {
                return VideoActivity.createIntent(MainActivity.this,SensorOrTouch,(int)video);
            }
        };
        String CreateCode(Object video)
        {
            String code = "" + SensorOrTouch[0];
            for(int i = 1; i < SensorOrTouch.length;i++)
                code += ";" + SensorOrTouch[i];
            if(SensorOrTouch.length == 4)
                code += ";" + (int)video;
            return code;
        }
        public Site(String DK,String ENG, int... SensorOrTouch){
            this.DK = DK;
            this.ENG = ENG;
            this.SensorOrTouch = SensorOrTouch;
        }
        public Site(Func1<Object,Intent> Create,String DK,String ENG, int... SensorOrTouch){
            this.Create = Create;
            this.DK = DK;
            this.ENG = ENG;
            this.SensorOrTouch = SensorOrTouch;
        }
    }
    Site[] Sites = {
            new Site("Generelt", "Generel", 1, 1, 1, 1),
            new Site("Højre side","Right Site",1,0,1,0),
            new Site("Venstre side","Left site",0,1,0,1),
            new Site("Hænder","Hands",1,1,0,0),
            new Site("Fødder","Feet",0,0,1,1),
            new Site("Højre Hånd","Right Hand",1,0,0,0),
            new Site("Venstre Hånd","Left Hand",0,1,0,0),
            new Site("Højre Fod","Right Foot",0,0,1,0),
            new Site("Venstre Fod","Left Foot",0,0,0,1),
            new Site(new Func1<Object, Intent>() {
                @Override
                public Intent Run(Object o) {
                    return DrawActivity.createIntent(MainActivity.this);
                }
            },"Tegner", "Painter", 0),
            new Site(new Func1<Object, Intent>() {
                @Override
                public Intent Run(Object o) {
                    return BalloonActivity.createIntent(MainActivity.this);
                }
            },"Balloner","Balloons",1),
            new Site(new Func1<Object, Intent>() {
                @Override
                public Intent Run(Object o) {
                    return PianoActivity.createIntent(MainActivity.this);
                }
            },"Klavér","Piano",2),
    };
    boolean[] libraries;

    ActivityResultLauncher activityResultLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for(int i = 0; i < 8;i++)
        {
            String name = "mp4_" + i;
            int src = getResources().getIdentifier(name, "raw", getPackageName());
            String uriPath = "android.resource://"+getPackageName() + "/" + src;
            videos.add(new Video(uriPath,name,true));
        }

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::onActivityResult);

        libraries = new boolean[Sites.length];
        sharedPreferences = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);

        mBluetoothScanner = new BluetoothScanner(this);

        imgS[0] = findViewById(R.id.imgRH);
        imgS[1] = findViewById(R.id.imgLH);
        imgS[2] = findViewById(R.id.imgRF);
        imgS[3] = findViewById(R.id.imgLF);

        for(int i = 0; i < sensorActivators.length;i++) {
            final int final_i = i;
            imgS[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(isAdmin)
                        mBluetoothScanner.selectDevice(new Action<MACDevice>() {
                            @Override
                            public void Run(MACDevice macDevice) {
                                sensorActivators[final_i] = PrepareActivator(macDevice.mMac,macDevice.mName);
                                SaveConfig();
                            }
                        },null, new Func1<MACDevice, Boolean>() {
                            @Override
                            public Boolean Run(MACDevice macDevice) {
                                return macDevice.toString().contains("Movesense");
                            }
                        });
                    else
                    {
                        if(final_i==adminSequence[adminPosition]) {
                            adminPosition++;
                            if(adminPosition == adminSequence.length) {
                                isAdmin = true;
                                invalidateOptionsMenu();
                                Toast.makeText(MainActivity.this, "Admin enabled", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                            adminPosition = 0;
                    }
                }
            });
            imgS[i].setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if(isAdmin) {
                        sensorActivators[final_i].Edit(MainActivity.this,null);
                    }
                    return true;
                }
            });
        }

        LoadConfig();

        mediaPlayer = MediaPlayer.create(this,R.raw.adjust);
        mediaPlayer.setLooping(true);
    }

    @Override
    public void onBackPressed() {
    }

    MediaPlayer mediaPlayer;
    boolean adjustingSound = false;
    long lastVolumeAdjust = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP){
            lastVolumeAdjust = System.currentTimeMillis();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int savedDay = sharedPreferences.getInt("savedDay",-1);

        Calendar cal = Calendar.getInstance();
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);

        if(savedDay != currentDay) {
            currentProgram = 0;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("savedDay",currentDay);
            editor.putInt("currentProgram",currentProgram);
            editor.apply();

            CreateProgram();
        }
        else {
            currentProgram = sharedPreferences.getInt("currentProgram", 0);

            Helper.MDOL_DEBUG("Preparing to load " + programs.length + " programs");
            for(int i = 0; i < programs.length;i++)
            {
                String strProgram = sharedPreferences.getString("program" + i,"1;1;1;1;0");
                Helper.MDOL_DEBUG("Program loaded " + i + ":" + strProgram);
                String[] split = strProgram.split(";");
                if(split.length == 1) {
                    if (strProgram.equals("0"))
                        programs[i] = DrawActivity.createIntent(this);
                    else if (strProgram.equals("1"))
                        programs[i] = BalloonActivity.createIntent(this);
                    else if (strProgram.equals("2"))
                        programs[i] = PianoActivity.createIntent(this);
                }
                else
                {
                    int[] use = new int[4];
                    for(int j = 0; j < split.length-1;j++)
                        use[j] = Integer.parseInt(split[j]);
                    int video = Integer.parseInt(split[4]);
                    programs[i] = VideoActivity.createIntent(this,use,video);
                }
            }
        }

        int memory = sharedPreferences.getInt("ThresholdMemory",20);
        int automax = sharedPreferences.getInt("ThresholdAutoMax",50);
        for(int i = 0; i < sensorActivators.length;i++) {
            int final_i = i;
            if (sensorActivators[i] != null && sensorActivators[i].mSensor.getStatus() == 1) {
                sensorActivators[i].mSensor.setMemory((double)memory/10);
                sensorActivators[i].arrSignals[0].mAutoMax = (float) automax / 100;
                ((MovesenseSensor) sensorActivators[final_i].mSensor).UpdateBattery(new Action<Integer>() {
                    @Override
                    public void Run(Integer pct) {
                        if (pct < 20)
                            new AlertDialog.Builder(MainActivity.this).setTitle((useDK ? "Batteriet er lavt på " + friendlyNames[final_i] + "!" : "The battery is low on " + friendlyNamesEng[final_i] + "!")).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
                    }
                });
            }
        }
        upload();

        tmrActive = new Timer();
        tmrActive.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(System.currentTimeMillis()-lastVolumeAdjust<2000) {
                            if (!adjustingSound) {
                                adjustingSound = true;
                                mediaPlayer.start();
                            }
                        }
                        else
                        {
                            if(adjustingSound) {
                                adjustingSound = false;
                                mediaPlayer.pause();
                            }
                        }

                        for(int i = 0; i < sensorActivators.length;i++) {
                            if (sensorActivators[i] != null) {
                                int status = sensorActivators[i].mSensor.getStatus();
                                if (status != 1) {
                                    imgS[i].setImageResource(imgDisconnected[i]);
                                    sensorActivators[i].mSensor.connect(MainActivity.this);
                                }
                                else {
                                    if(sensorActivators[i].getLastValid()<200)
                                        imgS[i].setImageResource(imgMoving[i]);
                                    else
                                        imgS[i].setImageResource(imgConnected[i]);
                                }
                            }
                        }
                    }
                });
            }
        },0,500);
    }
    Timer tmrActive;
    boolean OnlyTouch = true;
    Random rnd = new Random();
    void CreateProgram()
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        for(int i = 0; i < Sites.length;i++)
            if(Sites[i].SensorOrTouch.length==4 && libraries[i])
                OnlyTouch = false;

        ArrayList<Integer> candidates = new ArrayList<>();
        Helper.MDOL_DEBUG("Preparing to create " + programs.length + " programs");
        for(int i = 0; i < programs.length;i++)
        {
            if(candidates.size()==0){
                if(!OnlyTouch)
                    candidates.add(-1);
                for(int j = 0; j < libraries.length;j++)
                {
                    if(libraries[j])
                        candidates.add(j);
                }
            }
            int candidate = rnd.nextInt(candidates.size());
            int[] use;
            int library = candidates.get(candidate);
            candidates.remove(candidate);
            String strProgram;

            ArrayList<Integer> videoIds = new ArrayList<>();
            for(int j = 0; j < videos.size();j++)
                if(videos.get(j).isEnabled)
                    videoIds.add(j);
            int video = videoIds.get(rnd.nextInt(videoIds.size()));
            if(library == -1) { //Play video without feedback
                use = new int[4];
                strProgram = use[0] + ";" + use[1] + ";" + use[2] + ";" + use[3] + ";" + video;
                programs[i] = VideoActivity.createIntent(this, use, video);
            }
            else
            {
                programs[i] = Sites[library].Create.Run(video);
                strProgram = Sites[library].CreateCode(video);
            }
            editor.putString("program" + i, strProgram);
            Helper.MDOL_DEBUG("Created Program " + i + ": " + strProgram);
        }
        editor.apply();
    }

    boolean CloseConnection = true;
    @Override
    protected void onPause() {
        super.onPause();
        tmrActive.cancel();
        if(CloseConnection)
        {
            for (SensorActivator sensorActivator:sensorActivators) {
                if(sensorActivator != null)
                    sensorActivator.mSensor.disconnect();
            }
        }
    }

    void upload()
    {
        File path= new File(getFilesDir(), "FeedbackLog");
        File[] logFiles = path.listFiles();

        if(logFiles != null) {
            Helper.MDOL_DEBUG("Preparing to upload files: " + logFiles.length);
            if (logFiles != null && logFiles.length > 0)
                new UploadFileAsync().execute(logFiles);
        }
    }

    Integer[] imgMoving = {R.drawable.moving_rh,R.drawable.moving_lh,R.drawable.moving_rf,R.drawable.moving_lf};
    Integer[] imgConnected = {R.drawable.connected_rh,R.drawable.connected_lh,R.drawable.connected_rf,R.drawable.connected_lf};
    Integer[] imgConnecting = {R.drawable.connecting_rh,R.drawable.connecting_lh,R.drawable.connecting_rf,R.drawable.connecting_lf};
    Integer[] imgDisconnected = {R.drawable.disconnected_rh,R.drawable.disconnected_lh,R.drawable.disconnected_rf,R.drawable.disconnected_lf};
    Integer[] imgUnset = {R.drawable.unset_rh,R.drawable.unset_lh,R.drawable.unset_rf,R.drawable.unset_lf};

    public void cmdClick(View view)
    {
        Next();
    }

    static SharedPreferences sharedPreferences;
    public static void SUCCESS()
    {
        currentProgram++;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("currentProgram",currentProgram);
        editor.apply();
    }

    Intent[] programs = new Intent[5];
    public static int currentProgram = 0;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 123 && resultCode == RESULT_OK)
        {
            CloseConnection = true;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(() -> Success());
                }
            },200);
        }
    }

    void Success() {
        if(currentProgram<programs.length){
            if(AUTOPLAY)
                Next();
            else
                new AlertDialog.Builder(this)
                        .setTitle(useDK ? "Vil du fortsætte med dagens program?":"Do you want to continue with todays program?")
                        .setPositiveButton(useDK ? "Ja" : "Yes", (dialog, which) -> {
                            Next();
                            dialog.dismiss();
                        })
                        .setNegativeButton(useDK ? "Nej" : "No", (dialog, which) -> dialog.dismiss())
                        .show();
        }
        else
        {
            new AlertDialog.Builder(this)
                    .setTitle(useDK ? "Der er ikke flere scenarier på programmet i dag!" : "There are no more scenarios today!")
                    .setPositiveButton("Ok", (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }
    void Next()
    {
        com.example.mdol.mdollib.Helper.MDOL_DEBUG("CurrentProgram: " + currentProgram);
        if(currentProgram>=programs.length){
            new AlertDialog.Builder(this)
                    .setTitle(useDK ? "Der er ikke flere scenarier på programmet i dag!" : "There are no more scenarios today!")
                    .setPositiveButton("Ok", (dialog, which) -> dialog.dismiss())
                    .show();
        }
        else{
            boolean allConnected = true;
            for (SensorActivator sensorActivator:sensorActivators) {
                if(sensorActivator == null || sensorActivator.mSensor.getStatus()!=1)
                    allConnected = false;
            }
            if(allConnected) {
                CloseConnection = false;
                startActivityForResult(programs[currentProgram], 123);
            }
            else
            {
                new AlertDialog.Builder(this)
                        .setTitle(useDK ? "En eller flere sensorer er ikke forbundet! Vil du fortsætte?" : "One or more sensors are not connected! Do you want to continue?")
                        .setPositiveButton(useDK ? "Ja" : "Yes", (dialog, which) -> {
                            CloseConnection = false;
                            startActivityForResult(programs[currentProgram], 123);
                            dialog.dismiss();
                        })
                        .setNegativeButton(useDK ? "Nej" : "No", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        }
    }

    static String MY_PREFS_NAME = "FeedbackPrefs";
    public void SaveConfig()
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (int i = 0; i< sensorActivators.length;i++)
        {
            SensorActivator sensorActivator = sensorActivators[i];
            if(sensorActivator != null) {
                editor.putString("SensorActivator" + i, sensorActivator.toString());
            }
            else {
                editor.putString("SensorActivator" + i, "");
            }
        }
        editor.putInt("nLibrary",libraries.length);
        for(int i = 0; i < libraries.length;i++) {
            editor.putBoolean("Library_" + i,libraries[i]);
        }

        editor.putInt("nVideos",videos.size());
        for(int i = 0; i < videos.size();i++) {
            editor.putBoolean("Videos_" + i,videos.get(i).isEnabled);
        }
        editor.apply();
    }

    public void LoadConfig()
    {
        SharedPreferences sharedPreferences = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        for (int i = 0; i < sensorActivators.length;i++)
        {
            String str = sharedPreferences.getString("SensorActivator" + i,"");
            if(!str.equals(""))
            {
                sensorActivators[i] = new SensorActivator(new XML(str));
                imgS[i].setImageResource(imgDisconnected[i]);
            }
        }
        int nLibraries = sharedPreferences.getInt("nLibrary",5);
        for(int i = 0; i < nLibraries & i < libraries.length;i++)
            libraries[i] = sharedPreferences.getBoolean("Library_" + i,true);

        ID = sharedPreferences.getString("ID","");
        SAVE = sharedPreferences.getBoolean("SAVE",true);
        Helper.MDOL_DEBUG("Current ID:" + ID + (SAVE ? "" : "NOT SAVING!"));
        AUTOPLAY = sharedPreferences.getBoolean("AUTOPLAY",false);

        int nVideos = sharedPreferences.getInt("nVideos",0);
        for(int i = 0; i < nVideos & i < videos.size();i++)
            videos.get(i).isEnabled = sharedPreferences.getBoolean("Videos_" + i,true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(isAdmin)
        {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.options_admin, menu);
        }
        else {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.options, menu);
        }
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        //respond to menu item selection
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog;
        switch (item.getItemId()) {
            case R.id.site:
                String[] librariesStr = new String[Sites.length];
                for(int i = 0;i < Sites.length;i++) {
                    librariesStr[i] = useDK ? Sites[i].DK : Sites[i].ENG;
                }
                Dialog.CheckList(this,
                        useDK ? "Vælg side(r)" : "Choose site(s)",
                        useDK ? "Færdig" : "Done",
                        librariesStr, libraries, new Action<boolean[]>() {
                            @Override
                            public void Run(boolean[] booleans) {
                                libraries = booleans;
                                SaveConfig();
                                CreateProgram();
                            }
                        });
                return true;
            case R.id.admin_setID:
                builder.setTitle(useDK ? "Vælg ID" : "Set ID");
                final EditText input = new EditText(this);
                builder.setView(input);

                builder.setPositiveButton(useDK ? "Færdig" : "Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ID = input.getText().toString();
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("ID",ID);
                        editor.apply();
                    }
                });
                builder.setNegativeButton("Stop", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                dialog = builder.create();
                dialog.show();
                return true;
            case R.id.admin_save:
                builder.setTitle(useDK ? "Gem data" : "Save data");
                final CheckBox checkBox = new CheckBox(this);
                builder.setView(checkBox);

                builder.setPositiveButton(useDK ? "Færdig" : "Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SAVE = checkBox.isChecked();
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("SAVE",SAVE);
                        editor.apply();
                    }
                });
                builder.setNegativeButton("Stop", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                dialog = builder.create();
                dialog.show();
                return true;
            case R.id.admin_autoplay:
                builder.setTitle("Autoplay");
                final CheckBox chkAutoplay = new CheckBox(this);
                builder.setView(chkAutoplay);

                builder.setPositiveButton(useDK ? "Færdig" : "Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AUTOPLAY = chkAutoplay.isChecked();
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("AUTOPLAY",AUTOPLAY);
                        editor.apply();
                    }
                });
                builder.setNegativeButton("Stop", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                dialog = builder.create();
                dialog.show();
                return true;
            case R.id.admin_upload:
                upload();
                return true;
            case R.id.admin_videos:
                activityResultLauncher.launch(new Intent(MainActivity.this, VideoChooserActivity.class));
                //startActivityForResult(new Intent(MainActivity.this, VideoChooserActivity.class),123);
                return true;
            case R.id.admin_settings:
                int memory = sharedPreferences.getInt("ThresholdMemory",20);
                int automax = sharedPreferences.getInt("ThresholdAutoMax",50);
                com.example.mdol.mdollib.Dialog.MultiInput(MainActivity.this, "Threshold Settings", "OK",
                        new String[]{"Seconds","Max"},
                        new int[]{memory,automax},
                        new Action<int[]>() {
                    @Override
                    public void Run(int[] ints) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("ThresholdMemory",ints[0]);
                        editor.putInt("ThresholdAutoMax",ints[1]);
                        editor.apply();
                        for(int i = 0; i < sensorActivators.length;i++) {
                            sensorActivators[i].mSensor.setMemory((double)ints[0]/10);
                            sensorActivators[i].arrSignals[0].mAutoMax = (float) ints[1] / 100;
                        }
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    void onActivityResult(ActivityResult result)
    {
        if(result.getResultCode() == RESULT_OK)
        {
            SaveConfig();
        }
    }

    SensorActivator PrepareActivator(String Mac, String Name)
    {
        SensorActivator sensorActivator = new SensorActivator(MovesenseSensor.CREATE(Mac,Name));
        sensorActivator.mDelay = -1000;
        sensorActivator.mSensor.setMemory(2);
        sensorActivator.arrSignals[0].mEnabled = true;
        sensorActivator.arrSignals[0].mMode = SensorActivator.THRESHOLDMODES.AUTO;
        return sensorActivator;
    }
}
