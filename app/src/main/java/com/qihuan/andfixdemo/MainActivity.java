package com.qihuan.andfixdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.qihuan.andfixdemo.base.ApkUtil;

public class MainActivity extends AppCompatActivity {

    private TextView mText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mText = (TextView) findViewById(R.id.act_text);

        mText.postDelayed(new Runnable() {
            @Override
            public void run() {
                mText.setText(new OnMain().changeButton());
            }
        }, 3000);

        mText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickText();
            }
        });
    }

    int count = 0;

    public void onClickText() {
        if (++count >= 3){
            throw new RuntimeException("make crash ");
        }
        mText.setText(newMethod());
    }

    public String newMethod(){
        return "clicked from newMethod";
    }
}
