package com.bodhi.daily;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;

public class MainActivity extends Activity {
    public void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout layout=new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40,40,40,40);

        TextView title=new TextView(this);
        title.setText("菩提日课\n\n今日修行");
        title.setTextSize(24);
        layout.addView(title);

        CheckBox jie=new CheckBox(this); jie.setText("戒");
        CheckBox ding=new CheckBox(this); ding.setText("定");
        CheckBox hui=new CheckBox(this); hui.setText("慧");
        layout.addView(jie);
        layout.addView(ding);
        layout.addView(hui);

        Button nian=new Button(this);
        nian.setText("念佛 +1");
        layout.addView(nian);

        Button save=new Button(this);
        save.setText("保存今日记录");
        layout.addView(save);

        setContentView(layout);
    }
}