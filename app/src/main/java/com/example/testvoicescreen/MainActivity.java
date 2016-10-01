package com.example.testvoicescreen;

import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.Time;
import android.text.style.ReplacementSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.testvoicescreen.elvis.ElvisBasedRecognizer;
import com.example.testvoicescreen.utils.PackageControl;


public class MainActivity extends Activity {
    private static final String TAG = "AMonitor";
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        context = this;
        Intent i = new Intent(MainActivity.this, UIService.class);
        startService(i);
        
        PackageControl.getPackageList(context);
        
        finish();
        ElvisBasedRecognizer.initFileManagerAndInstanceNames(getApplicationContext());

    }
    
    /*
    void testpattern(){
        //String regex = "\\sf..";
        String regex = "[1-2]";
        
        //
        // Compiles the pattern and obtains the matcher object.
        //
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(
                "2T1he q3uick 2brown 1 f2oxd jumps3 ov4er t1he la2zy dog");
 
        //
        // find every match and print it
        //
        while (matcher.find()) {
            Log.d("sss", "found");
            System.out.format("Text \"%s\" found at %d to %d.%n",
                    matcher.group(), matcher.start(), matcher.end());
        }
    }*/
}

