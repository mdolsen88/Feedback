package com.example.mdol.feedback;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.example.mdol.mdollib.Action;
import com.example.mdol.mdollib.Dialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class VideoChooserActivity extends AppCompatActivity {

    ImageButton cmdPlaying = null;
    boolean updateSeek = true;
    VideoView vid;
    SeekBar seek;
    BaseAdapter videoAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chooser);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(updateSeek && vid != null)
                    try {
                        seek.setProgress(vid.getCurrentPosition() * 100 / vid.getDuration());
                    }
                    catch (Exception ignored)
                    {}
            }
        },0,50);

        seek = findViewById(R.id.seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                updateSeek = false;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(vid != null)
                    vid.seekTo(seek.getProgress()*vid.getDuration()/100);
                updateSeek = true;
            }
        });

        ListView lstVideos = findViewById(R.id.lstVideos);
        vid = findViewById(R.id.vid);
        vid.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(cmdPlaying != null) {
                    cmdPlaying.setImageResource(R.drawable.play);
                    cmdPlaying = null;
                }
            }
        });

        videoAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return MainActivity.videos.size();
            }

            @Override
            public Object getItem(int position) {
                return MainActivity.videos.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(VideoChooserActivity.this).
                            inflate(R.layout.item_video, parent, false);
                }
                Video video = (Video)getItem(position);
                CheckBox chkEnabled = convertView.findViewById(R.id.chkEnabled);
                chkEnabled.setChecked(video.isEnabled);
                chkEnabled.setOnCheckedChangeListener((buttonView, isChecked) ->
                {
                    video.isEnabled = isChecked;
                    setResult(RESULT_OK);
                });

                TextView txtName = convertView.findViewById(R.id.txtName);
                txtName.setText(video.name);
                ImageButton cmdPlay = convertView.findViewById(R.id.cmdPlay);
                cmdPlay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(cmdPlaying != null)
                            cmdPlaying.setImageResource(R.drawable.play);
                        if(cmdPlaying == cmdPlay)
                        {
                            cmdPlaying = null;
                            vid.pause();
                        }
                        else {
                            cmdPlay.setImageResource(R.drawable.stop);
                            cmdPlaying = cmdPlay;
                            vid.setVideoPath(video.uriPath);
                            vid.start();
                        }
                    }
                });
                return convertView;
            }
        };
        lstVideos.setAdapter(videoAdapter);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_videochooser, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        //respond to menu item selection
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog;
        switch (item.getItemId()) {
            case R.id.addVideo:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/mp4");
                intent = Intent.createChooser(intent, "Choose a file");
                startActivityForResult(intent, 1234);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1234 && resultCode == RESULT_OK)
        {
            Uri uri = data.getData();

            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            String name = cursor.getString(nameIndex);

            File root = new File(getFilesDir(),"UserVideos");
            if(!root.exists())
                root.mkdir();
            OutputStream out = null;
            Log.d("MDOL_DEBUG","Trying to copy: " + new File(root, name));
            try {
                // open the user-picked file for reading:
                InputStream in = getContentResolver().openInputStream(uri);
                // open the output-file:
                out = new FileOutputStream(new File(root, name));
                // copy the content:
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                // Contents are copied!
                if (in != null) {
                    in.close();
                }
                if (out != null){
                    out.close();
                }
                Log.d("MDOL_DEBUG","Copy finished");
                videoAdapter.notifyDataSetChanged();
            }
            catch(IOException ex)
            {
                Log.d("MDOL_DEBUG","FileCopyError: " + ex.getMessage());
            }
        }
    }
}