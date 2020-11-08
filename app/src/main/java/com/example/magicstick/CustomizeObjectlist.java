package com.example.magicstick;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class CustomizeObjectlist extends AppCompatActivity {

    static int i=0;
    static String [] objectArray;

    TextView textView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_customize_objectlist);
        textView = findViewById(R.id.textView2);
        TextToSpeech tts = new android.speech.tts.TextToSpeech(getApplicationContext(), new android.speech.tts.TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
            }
        });
        tts.setLanguage(Locale.KOREAN);
        tts.setPitch(1.0f);
        tts.setSpeechRate(1.0f);
        tts.speak("지금부터 장애물 설정을 하겠습니다.",TextToSpeech.QUEUE_FLUSH,null);

        objectArray = getResources().getStringArray(R.array.array_object);
        textView.setText(objectArray[0]);
        tts.speak(objectArray[0],TextToSpeech.QUEUE_FLUSH,null);

        View view = findViewById(R.id.background_view);
        view.setOnTouchListener(new OnSwipeTouchListener(this){
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = sharedPreferences.edit();

            //Yes
            public void onSwipeTop(){

                editor.putBoolean(objectArray[i],true);
                i++;
                if(i<25){
                    textView.setText(objectArray[i]);
                    editor.apply();
                    tts.speak(objectArray[i],TextToSpeech.QUEUE_FLUSH,null);
                }
            }

            //No
            public void onSwipeBottom(){
                editor.putBoolean(objectArray[i],false);
                i++;
                if(i<25){
                    textView.setText(objectArray[i]);
                    editor.apply();
                    tts.speak(objectArray[i],TextToSpeech.QUEUE_FLUSH,null);
                }
            }

        });

        if(i==25){
            Log.d("", "stop");
            onDestroy();
        }
    }
}
