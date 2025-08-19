package com.example.mdol.feedback;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.VideoView;

import java.util.ArrayList;

public class DrawActivity extends MasterActivity {

    @Override
    String FriendlyName() {
        return "DrawActivity";
    }
    public static Intent createIntent(Context context)
    {
        return new Intent(context, DrawActivity.class);
    }

    PaintView paintView;
    @Override
    void oncreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_draw);

        paintView = findViewById(R.id.paintView);
    }
    @Override
    View getview()
    {
        return paintView;
    }

    @Override
    void feedback(ArrayList<Integer> active) {
    }

    @Override
    protected void destroy() {
    }
}