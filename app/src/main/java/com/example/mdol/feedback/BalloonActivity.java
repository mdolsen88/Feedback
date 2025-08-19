package com.example.mdol.feedback;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.VideoView;

import java.util.ArrayList;

public class BalloonActivity extends MasterActivity {

    @Override
    String FriendlyName() {
        return "BalloonActivity";
    }

    public static Intent createIntent(Context context)
    {
        return new Intent(context, BalloonActivity.class);
    }

    BalloonView ballonView;
    @Override
    void oncreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_balloon);

        ballonView = findViewById(R.id.ballonView);
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