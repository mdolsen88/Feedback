package com.example.mdol.feedback;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.SweepGradient;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;

public class ColorActivity extends MasterActivity {

    @Override
    String FriendlyName() {
        return "ColorActivity";
    }
    public static Intent createIntent(Context context, int[] use)
    {
        Intent intent = new Intent(context, ColorActivity.class);
        intent.putExtra("DELAY",(long)500);
        intent.putExtra("USE",use);
        return intent;
    }

    ImageView iv;
    @Override
    void oncreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_color);
        iv = findViewById(R.id.color_layout_img);
        iv.setImageDrawable(getGradient(0));
    }

    int[] colors = new int[]{Color.RED,Color.YELLOW,Color.GREEN,Color.MAGENTA,Color.BLUE,Color.CYAN};
    GradientDrawable getGradient(double weight)
    {
        int id = (int)Math.floor(weight);
        int start = colors[com.example.mdol.mdollib.Helper.trueMod(id,colors.length)];
        int end = colors[com.example.mdol.mdollib.Helper.trueMod(id+1,colors.length)];
        GradientDrawable gd = new GradientDrawable();
        // Set the color array to draw gradient

        int split =(int)((weight-id)*255);
        int[] gradient = new int[255];
        for(int i = 0; i<255;i++)
            gradient[i] = i > split ? start : end;
        gd.setColors(gradient);
        gd.setGradientType(GradientDrawable.SWEEP_GRADIENT);
        return gd;
    }

    @Override
    void destroy() {
    }
    @Override
    View getview()
    {
        return iv;
    }

    double we = 0;
    @Override
    void feedback(ArrayList<Integer> active) {
        if(active.size()>0)
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    iv.setImageDrawable(getGradient(we+=0.05));
                }
            });
    }
}
